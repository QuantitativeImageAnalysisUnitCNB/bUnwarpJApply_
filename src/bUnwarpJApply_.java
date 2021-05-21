import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.Image;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import bunwarpj.BSplineModel;
import bunwarpj.MiscTools;
import bunwarpj.Transformation;
import bunwarpj.bUnwarpJ_;
import ij.CompositeImage;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.io.FileInfo;
import ij.measure.Calibration;
import ij.measure.Measurements;
import ij.plugin.ChannelSplitter;
import ij.plugin.ImagesToStack;
import ij.plugin.PlugIn;
import ij.plugin.RGBStackMerge;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import ij.process.LUT;
import loci.common.services.ServiceFactory;
import loci.formats.ImageReader;
import loci.formats.meta.IMetadata;
import loci.formats.meta.MetadataStore;
import loci.formats.out.OMETiffWriter;
import loci.formats.services.OMEXMLService;
import loci.plugins.in.DisplayHandler;
import loci.plugins.in.ImportProcess;
import loci.plugins.in.ImporterOptions;

public class bUnwarpJApply_ implements PlugIn, Measurements {

	private JFrame frameMain;
	static ImagePlus imp, sourceImp, targetImp, impSize, impFinalSource, impFinalTarget, impMerged;
	private JPanel panelOkCancel, panelImages, panelSave, panelTransf;
	private String BUNWARPJAPPLY_IMAGES_DEFAULT_PATH, BUNWARPJAPPLY_SAVE_DEFAULT_PATH,
			BUNWARPJAPPLY_TRANSF_DEFAULT_PATH, pathImages, pathSave, pathTransf;
	private Preferences prefImages, prefSave, prefTransf;
	private TextField textImages, textSave, textTransf;
	private JButton okButton1, cancelButton, buttonTransf, buttonCancel, buttonOk, buttonSelectAll, buttonDeselectAll;
	private ImagePlus[] channels, impsSelected;
	private JRadioButton dispRadioButton, nDispRadioButton, expRadioButton, nExpRadioButton;
	private String[] imageTitles, extension;
	private JComboBox comboChSource, comboChTarget;
	private int widthResize, heightResize, widthImp, heightImp;
	private JCheckBox checkSeries;
	private int intervals;
	private double[][] cx, cy;
	private JLabel directLabelImages, directLabelTransf;
	private Transformation transf;
	private File directImagesOriginal; // directImages;
	static File directImagesTotal;
	static List<String> pathMergeds;
	final JFrame frame = new JFrame("Aligning..."), frameLif = new JFrame("From .lif to .tiff...");
	JTextArea taskOutput;
	JTable tableImages;
	DefaultTableModel modelImages;
	JScrollPane jScrollPaneImages;
	Object[] columnNames = new Object[] { "", "", "" };
	String okProcess = "";
	JFrame frameSeries;

	@SuppressWarnings("deprecation")
	public void run(String arg) {

		try {
			for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
				if ("Nimbus".equals(info.getName())) {
					UIManager.setLookAndFeel(info.getClassName());
					break;
				}
			}
		} catch (Exception e) {
			// If Nimbus is not available, you can set the GUI to another look and feel.
		}
		BUNWARPJAPPLY_IMAGES_DEFAULT_PATH = "images_path";
		BUNWARPJAPPLY_SAVE_DEFAULT_PATH = "save_path";
		BUNWARPJAPPLY_TRANSF_DEFAULT_PATH = "transf_path";

		prefImages = Preferences.userRoot();
		prefSave = Preferences.userRoot();
		prefTransf = Preferences.userRoot();

		imp = WindowManager.getCurrentImage();

		if (imp == null)
			createAndShowGUI();
		if (imp != null)
			IJ.error("You should not have images opened, bUnwarpJApply is asking for the path.");

		// if (imp == null)

		okButton1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Thread mainProcess = new Thread(new Runnable() {

					public void run() {

						pathImages = textImages.getText();
						prefImages.put(BUNWARPJAPPLY_IMAGES_DEFAULT_PATH, textImages.getText());
						pathSave = textSave.getText();
						prefSave.put(BUNWARPJAPPLY_SAVE_DEFAULT_PATH, textSave.getText());
						pathTransf = textTransf.getText();
						prefTransf.put(BUNWARPJAPPLY_TRANSF_DEFAULT_PATH, textTransf.getText());
						frameMain.dispatchEvent(new WindowEvent(frameMain, WindowEvent.WINDOW_CLOSING));

						File imageFolder = new File(pathImages);
						File[] listOfFiles = imageFolder.listFiles();
						imageTitles = new String[listOfFiles.length];
						extension = new String[listOfFiles.length];

						for (int i = 0; i < listOfFiles.length; i++) {
							imageTitles[i] = listOfFiles[i].getName();
							extension[i] = imageTitles[i].substring(imageTitles[i].lastIndexOf("."));

						}
						String[] paths = new String[listOfFiles.length];
						for (int i = 0; i < paths.length; i++)
							paths[i] = pathImages + File.separator + imageTitles[i];
						String initialName = new File(textImages.getText()).getName();
						for (int i = 0; i < listOfFiles.length; i++) {
							if (listOfFiles[i].getName().contains(".tif") == false
									&& listOfFiles[i].getName().contains(".lif") == false) {
								IJ.error("It is need .lif o .tif format to analyze.");
								return;
							}
							if (listOfFiles[i].getName().contains(".tif")
									|| listOfFiles[i].getName().contains(".lif")) {
								if (listOfFiles[i].getName().contains(".tif") == true) {
									imp = new ImagePlus(pathImages + File.separator + imageTitles[i]);
									ImagePlus[] impSplits = null;
									pathMergeds = new ArrayList<String>();

									directImagesOriginal = new File(new File(textImages.getText()).getParentFile()
											+ File.separator + initialName);

									final int MAX = listOfFiles.length;

									// creates progress bar
									final JProgressBar pb = new JProgressBar();
									pb.setMinimum(0);
									pb.setMaximum(MAX);
									pb.setStringPainted(true);
									taskOutput = new JTextArea(5, 20);
									taskOutput.setMargin(new Insets(5, 5, 5, 5));
									taskOutput.setEditable(false);
									JPanel panel = new JPanel();
									panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
									panel.add(pb);
									panel.add(Box.createVerticalStrut(5));
									panel.add(new JScrollPane(taskOutput), BorderLayout.CENTER);
									panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

									frame.getContentPane().add(panel);
									frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
									frame.setSize(300, 200);
									frame.setVisible(true);
									final int currentValue = i + 1;
									try {
										SwingUtilities.invokeLater(new Runnable() {
											public void run() {
												pb.setValue(currentValue);
												taskOutput.append(String.format(
														"Processing %s -- Completed %f of task.\n", imp.getTitle(),
														(double) Math.round(
																(currentValue * 100.0) / listOfFiles.length * 100.0)
																/ 100.0));

											}
										});
										java.lang.Thread.sleep(100);
									} catch (InterruptedException e) {
										JOptionPane.showMessageDialog(frameLif, e.getMessage());
									}

									Calibration cal = imp.getCalibration();
									channels = ChannelSplitter.split(imp);
									if (comboChSource.getSelectedIndex() == 0
											&& comboChTarget.getSelectedIndex() == 1) {
										sourceImp = channels[1];
										targetImp = channels[0];
									}
									if (comboChSource.getSelectedIndex() == 1
											&& comboChTarget.getSelectedIndex() == 0) {
										sourceImp = channels[0];
										targetImp = channels[1];
									}

									// title = imp.getShortTitle();
									imp.setTitle("Original-" + imp.getTitle());

									/*
									 * if (checkTransf.isSelected() == Boolean.TRUE) { transf =
									 * bUnwarpJ_.computeTransformationBatch(targetImp, sourceImp,
									 * targetImp.getProcessor(), sourceImp.getProcessor(), 1, 0, 0, 2, 0.0, 0.0,
									 * 0.0, 1.0, 10.0, 0.01); intervals = transf.getIntervals(); cx =
									 * transf.getDirectDeformationCoefficientsX(); cy =
									 * transf.getDirectDeformationCoefficientsY(); }
									 */
									// if (checkTransf.isSelected() == Boolean.FALSE) {
									intervals = MiscTools.numberOfIntervalsOfTransformation(textTransf.getText());
									cx = new double[intervals + 3][intervals + 3];
									cy = new double[intervals + 3][intervals + 3];
									MiscTools.loadTransformation(textTransf.getText(), cx, cy);
									// }
									ImageStack outputStackSource = new ImageStack(sourceImp.getWidth(),
											sourceImp.getHeight());
									ImageStack outputStackTarget = new ImageStack(targetImp.getWidth(),
											targetImp.getHeight());

									// apply transform to each slice of the stack

									for (int i1 = 1; i1 <= sourceImp.getImageStackSize(); i1++) {
										ImageProcessor ip = sourceImp.getImageStack().getProcessor(i1);

										BSplineModel source = new BSplineModel(ip, false, 1);

										ImagePlus movingImage = new ImagePlus("", ip);

										ImageProcessor result = MiscTools.applyTransformationMT(movingImage, targetImp,
												source, intervals, cx, cy);

										outputStackSource.addSlice("", result);
									}
									if (WindowManager.getFrame("Log") != null) {
										IJ.selectWindow("Log");
										IJ.run("Close");
									}
									for (int i1 = 1; i1 <= targetImp.getImageStackSize(); i1++) {
										ImageProcessor ip = targetImp.getImageStack().getProcessor(i1);
										outputStackTarget.addSlice("", ip);
									}

									// return results
									impFinalSource = new ImagePlus("Fixed_Image-" + imp.getTitle(), outputStackSource);
									impFinalTarget = new ImagePlus("Moving_Image-" + imp.getTitle(), outputStackTarget);

									if (targetImp.getBitDepth() == 8) {
										IJ.run(impFinalSource, "8-bit", "");
									}
									if (targetImp.getBitDepth() == 16) {
										IJ.run(impFinalSource, "16-bit", "");
									}
									if (targetImp.getBitDepth() == 24) {
										IJ.run(impFinalSource, "RGB Color", "");
									}
									if (targetImp.getBitDepth() == 32) {
										IJ.run(impFinalSource, "32-bit", "");
									}

									imp.setCalibration(cal);
									impFinalSource.setCalibration(cal);
									impFinalTarget.setCalibration(cal);
									ImageStack stackMerged = null;
									if (comboChSource.getSelectedIndex() == 0)
										stackMerged = RGBStackMerge.mergeStacks(impFinalTarget.duplicate().getStack(),
												impFinalSource.duplicate().getStack(), null, false);
									if (comboChSource.getSelectedIndex() == 1)
										stackMerged = RGBStackMerge.mergeStacks(impFinalSource.duplicate().getStack(),
												impFinalTarget.duplicate().getStack(), null, false);
									impMerged = new ImagePlus("RGB_aligned-" + imp.getTitle(), stackMerged);

									impMerged.setCalibration(cal);

									if (dispRadioButton.isSelected() == Boolean.TRUE) {
										imp.show();
										impFinalSource.show();
										impFinalTarget.show();
										impMerged.show();
									}
									if (expRadioButton.isSelected() == Boolean.TRUE) {

										File directImagesParent = new File(textSave.getText() + File.separator
												+ imageTitles[i].substring(0, imageTitles[i].lastIndexOf(".")));
										directImagesTotal = new File(
												directImagesParent.getAbsolutePath() + File.separator + imp.getTitle());

										if (!directImagesParent.exists()) {
											boolean results = false;

											try {
												directImagesParent.mkdir();
												results = true;
											} catch (SecurityException se) {
												// handle it
											}
										}
										if (!directImagesTotal.exists()) {
											boolean results = false;

											try {
												directImagesTotal.mkdir();
												results = true;
											} catch (SecurityException se) {
												// handle it
											}
										}
										// if (checkTransf.isSelected() == Boolean.TRUE)
										// transf.saveDirectTransformation(directImagesTotal.getAbsolutePath()
										// + File.separator + imp.getTitle() + "_direct_transf.txt");
										IJ.saveAs(imp, "Tiff", directImagesTotal.getAbsolutePath() + File.separator
												+ "Original-" + imp.getTitle() + extension[i]);
										IJ.saveAs(impFinalSource, "Tiff", directImagesTotal.getAbsolutePath()
												+ File.separator + impFinalSource.getTitle() + extension[i]);
										IJ.saveAs(impFinalTarget, "Tiff", directImagesTotal.getAbsolutePath()
												+ File.separator + impFinalTarget.getTitle() + extension[i]);
										IJ.saveAs(impMerged, "Tiff", directImagesTotal.getAbsolutePath()
												+ File.separator + impMerged.getTitle() + extension[i]);
										pathMergeds.add(directImagesTotal.getAbsolutePath() + File.separator
												+ impMerged.getTitle());
									}

								}
								if (listOfFiles[i].getName().contains(".lif") == true) {
									ImagePlus[] imps = openBF((pathImages + File.separator + imageTitles[i]), false,
											false, false, false, false, true);

									if (checkSeries.isSelected() == Boolean.TRUE) {
										showSeriesDialog(imps);
										buttonOk.addActionListener(new ActionListener() {
											public void actionPerformed(ActionEvent ae) {
												frameSeries.dispatchEvent(
														new WindowEvent(frameSeries, WindowEvent.WINDOW_CLOSING));
												synchronized (buttonOk) {
													buttonOk.notify();
													List<Integer> seriesSelectedList = new ArrayList<Integer>();
													for (int j = 0; j < tableImages.getRowCount(); j++)
														if (((Boolean) tableImages.getModel().getValueAt(
																tableImages.convertRowIndexToModel(j),
																tableImages.convertColumnIndexToModel(0))) == true)
															seriesSelectedList.add(j);

													impsSelected = new ImagePlus[seriesSelectedList.size()];
													for (int j = 0; j < seriesSelectedList.size(); j++)
														impsSelected[j] = imps[seriesSelectedList.get(j)];
												}

											}
										});
										synchronized (buttonOk) {

											try {
												buttonOk.wait();
											} catch (InterruptedException ex) {
												ex.printStackTrace();
											}
										}

									}

									if (checkSeries.isSelected() == Boolean.FALSE) {
										impsSelected = new ImagePlus[imps.length];
										for (int j = 0; j < imps.length; j++)
											impsSelected[j] = imps[j];
									}

									ImagePlus[] impSplits = null;
									pathMergeds = new ArrayList<String>();
									directImagesOriginal = new File(new File(textImages.getText()).getParentFile()
											+ File.separator + initialName);
									final int MAX = impsSelected.length;

									// creates progress bar
									final JProgressBar pb = new JProgressBar();
									pb.setMinimum(0);
									pb.setMaximum(MAX);
									pb.setStringPainted(true);
									taskOutput = new JTextArea(5, 20);
									taskOutput.setMargin(new Insets(5, 5, 5, 5));
									taskOutput.setEditable(false);
									JPanel panel = new JPanel();
									panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
									panel.add(pb);
									panel.add(Box.createVerticalStrut(5));
									panel.add(new JScrollPane(taskOutput), BorderLayout.CENTER);
									panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

									frame.getContentPane().add(panel);
									frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
									frame.setSize(300, 200);
									frame.setVisible(true);

									for (int z = 0; z < impsSelected.length; z++) {

										final int currentValue = z + 1;
										try {
											SwingUtilities.invokeLater(new Runnable() {
												public void run() {
													pb.setValue(currentValue);
													taskOutput
															.append(String
																	.format("Processing %s -- Completed %f of task.\n",
																			impsSelected[currentValue - 1].getTitle(),
																			(double) Math.round((currentValue * 100.0)
																					/ impsSelected.length * 100.0)
																					/ 100.0));

												}
											});
											java.lang.Thread.sleep(100);
										} catch (InterruptedException e) {
											JOptionPane.showMessageDialog(frameLif, e.getMessage());
										}
										Calibration calibration = impsSelected[z].getCalibration();
										FileInfo fileInfo = impsSelected[z].getFileInfo();
										String titleTotal = impsSelected[z].getTitle();
										if (impsSelected[z].getNChannels() > 1) {
											impSplits = ChannelSplitter.split(impsSelected[z]);
											IJ.resetMinAndMax(impSplits[0]);
											IJ.resetMinAndMax(impSplits[1]);
											ImageStack stackMerged = RGBStackMerge.mergeStacks(
													impSplits[1].duplicate().getStack(),
													impSplits[0].duplicate().getStack(), null, false);
											impsSelected[z] = new ImagePlus(titleTotal, stackMerged);
											impsSelected[z].setCalibration(calibration);
											impsSelected[z].setFileInfo(fileInfo);
										}
										if (impsSelected[z].getNChannels() < 1) {
											IJ.resetMinAndMax(impsSelected[z]);
											impsSelected[z].setTitle(titleTotal);
											impsSelected[z].setCalibration(calibration);
											impsSelected[z].setFileInfo(fileInfo);
										}

										// IJ.saveAsTiff(imp, directImages.getPath() + File.separator + title);
										Calibration cal = impsSelected[z].getCalibration();
										channels = ChannelSplitter.split(impsSelected[z]);
										if (comboChSource.getSelectedIndex() == 0
												&& comboChTarget.getSelectedIndex() == 1) {
											sourceImp = channels[1];
											targetImp = channels[0];
										}
										if (comboChSource.getSelectedIndex() == 1
												&& comboChTarget.getSelectedIndex() == 0) {
											sourceImp = channels[0];
											targetImp = channels[1];

										}

										// title = imp.getShortTitle();
										impsSelected[z].setTitle("Original-" + titleTotal);

										/*
										 * if (checkTransf.isSelected() == Boolean.TRUE) { transf =
										 * bUnwarpJ_.computeTransformationBatch(targetImp, sourceImp,
										 * targetImp.getProcessor(), sourceImp.getProcessor(), 1, 0, 0, 2, 0.0, 0.0,
										 * 0.0, 1.0, 10.0, 0.01); intervals = transf.getIntervals(); cx =
										 * transf.getDirectDeformationCoefficientsX(); cy =
										 * transf.getDirectDeformationCoefficientsY(); }
										 */
										// if (checkTransf.isSelected() == Boolean.FALSE) {
										intervals = MiscTools.numberOfIntervalsOfTransformation(textTransf.getText());
										cx = new double[intervals + 3][intervals + 3];
										cy = new double[intervals + 3][intervals + 3];
										MiscTools.loadTransformation(textTransf.getText(), cx, cy);
										// }
										ImageStack outputStackSource = new ImageStack(sourceImp.getWidth(),
												sourceImp.getHeight());
										ImageStack outputStackTarget = new ImageStack(targetImp.getWidth(),
												targetImp.getHeight());

										// apply transform to each slice of the stack

										for (int i1 = 1; i1 <= sourceImp.getImageStackSize(); i1++) {
											ImageProcessor ip = sourceImp.getImageStack().getProcessor(i1);

											BSplineModel source = new BSplineModel(ip, false, 1);

											ImagePlus movingImage = new ImagePlus("", ip);

											ImageProcessor result = MiscTools.applyTransformationMT(movingImage,
													targetImp, source, intervals, cx, cy);

											outputStackSource.addSlice("", result);
										}
										if (WindowManager.getFrame("Log") != null) {
											IJ.selectWindow("Log");
											IJ.run("Close");
										}
										for (int i1 = 1; i1 <= targetImp.getImageStackSize(); i1++) {
											ImageProcessor ip = targetImp.getImageStack().getProcessor(i1);
											outputStackTarget.addSlice("", ip);
										}

										// return results
										impFinalSource = new ImagePlus("Fixed_Image-" + titleTotal, outputStackSource);
										impFinalTarget = new ImagePlus("Moving_Image-" + titleTotal, outputStackTarget);

										if (targetImp.getBitDepth() == 8) {
											IJ.run(impFinalSource, "8-bit", "");
										}
										if (targetImp.getBitDepth() == 16) {
											IJ.run(impFinalSource, "16-bit", "");
										}
										if (targetImp.getBitDepth() == 24) {
											IJ.run(impFinalSource, "RGB Color", "");
										}
										if (targetImp.getBitDepth() == 32) {
											IJ.run(impFinalSource, "32-bit", "");
										}
										impsSelected[z].setCalibration(cal);
										impFinalSource.setCalibration(cal);
										impFinalTarget.setCalibration(cal);
										ImageStack stackMerged = null;
										if (comboChSource.getSelectedIndex() == 0)
											stackMerged = RGBStackMerge.mergeStacks(
													impFinalTarget.duplicate().getStack(),
													impFinalSource.duplicate().getStack(), null, false);
										if (comboChSource.getSelectedIndex() == 1)
											stackMerged = RGBStackMerge.mergeStacks(
													impFinalSource.duplicate().getStack(),
													impFinalTarget.duplicate().getStack(), null, false);
										impMerged = new ImagePlus("RGB_aligned-" + titleTotal, stackMerged);

										impMerged.setCalibration(cal);

										if (dispRadioButton.isSelected() == Boolean.TRUE) {
											impsSelected[z].show();
											impFinalSource.show();
											impFinalTarget.show();
											impMerged.show();
										}
										if (expRadioButton.isSelected() == Boolean.TRUE) {

											File directImagesParent = new File(textSave.getText() + File.separator
													+ imageTitles[i].substring(0, imageTitles[i].lastIndexOf(".")));
											directImagesTotal = new File(
													directImagesParent.getAbsolutePath() + File.separator + titleTotal);

											if (!directImagesParent.exists()) {
												boolean results = false;

												try {
													directImagesParent.mkdir();
													results = true;
												} catch (SecurityException se) {
													// handle it
												}
											}
											if (!directImagesTotal.exists()) {
												boolean results = false;

												try {
													directImagesTotal.mkdir();
													results = true;
												} catch (SecurityException se) {
													// handle it
												}
											}
											// if (checkTransf.isSelected() == Boolean.TRUE)
											// transf.saveDirectTransformation(directImagesTotal.getAbsolutePath()
											// + File.separator + titleTotal + "_direct_transf.txt");
											IJ.saveAs(impsSelected[z], "Tiff", directImagesTotal.getAbsolutePath()
													+ File.separator + "Original-" + titleTotal + extension[i]);
											IJ.saveAs(impFinalSource, "Tiff", directImagesTotal.getAbsolutePath()
													+ File.separator + impFinalSource.getTitle() + extension[i]);
											IJ.saveAs(impFinalTarget, "Tiff", directImagesTotal.getAbsolutePath()
													+ File.separator + impFinalTarget.getTitle() + extension[i]);
											IJ.saveAs(impMerged, "Tiff", directImagesTotal.getAbsolutePath()
													+ File.separator + impMerged.getTitle() + extension[i]);
											pathMergeds.add(directImagesTotal.getAbsolutePath() + File.separator
													+ impMerged.getTitle());
										}

									}
								}
							}

							taskOutput.append("Done!");
							frame.dispatchEvent(new WindowEvent(frameLif, WindowEvent.WINDOW_CLOSING));

							taskOutput.append("Done!");
							frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
						}
					}
				});
				mainProcess.start();

			}
		});

		cancelButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				frameMain.dispatchEvent(new WindowEvent(frameMain, WindowEvent.WINDOW_CLOSING));
			}
		});

		expRadioButton.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent ev) {
				if (ev.getStateChange() == ItemEvent.SELECTED)
					panelSave.setEnabled(true);
				if (ev.getStateChange() == ItemEvent.DESELECTED)
					panelSave.setEnabled(false);
			}
		});
		/*
		 * checkTransf.addItemListener(new ItemListener() { public void
		 * itemStateChanged(ItemEvent ev) { if (ev.getStateChange() ==
		 * ItemEvent.SELECTED) { directLabelTransf.setEnabled(false);
		 * textTransf.setEnabled(false); buttonTransf.setEnabled(false); } if
		 * (ev.getStateChange() == ItemEvent.DESELECTED) {
		 * directLabelTransf.setEnabled(true); textTransf.setEnabled(true);
		 * buttonTransf.setEnabled(true); } } });
		 */
	}

	public void createAndShowGUI() {

		JLabel labelDisplay = new JLabel("     Display Options: ");
		labelDisplay.setFont(new Font("Verdana", Font.BOLD, 12));
		JPanel panelDisplay = new JPanel(new FlowLayout(FlowLayout.LEFT));
		panelDisplay.add(labelDisplay);
		JLabel labelExport = new JLabel("     Export Options: ");
		labelExport.setFont(new Font("Verdana", Font.BOLD, 12));
		JPanel panelExport = new JPanel(new FlowLayout(FlowLayout.LEFT));
		panelExport.add(labelExport);
		JLabel channelLabel = new JLabel("     Channel Settings : ");
		channelLabel.setFont(new Font("Verdana", Font.BOLD, 12));
		JPanel panelChannel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		panelChannel.add(channelLabel);
		JLabel labelChRed = new JLabel("               Source: ");
		JPanel panelChRed = new JPanel(new FlowLayout(FlowLayout.LEFT));
		comboChSource = new JComboBox();
		comboChSource.addItem("Ch-1");
		comboChSource.addItem("Ch-2");
		panelChRed.add(labelChRed);
		panelChRed.add(comboChSource);
		JLabel labelChGreen = new JLabel("               Target: ");
		JPanel panelChGreen = new JPanel(new FlowLayout(FlowLayout.LEFT));
		comboChTarget = new JComboBox();
		comboChTarget.addItem("Ch-1");
		comboChTarget.addItem("Ch-2");
		panelChGreen.add(labelChGreen);
		panelChGreen.add(comboChTarget);
		JPanel chPanelMain = new JPanel();
		chPanelMain.setLayout(new BoxLayout(chPanelMain, BoxLayout.Y_AXIS));
		chPanelMain.add(panelChannel);
		chPanelMain.add(panelChRed);
		chPanelMain.add(panelChGreen);
		dispRadioButton = new JRadioButton();
		dispRadioButton.setSelected(true);
		nDispRadioButton = new JRadioButton();
		ButtonGroup bgDisp = new ButtonGroup();
		bgDisp.add(dispRadioButton);
		bgDisp.add(nDispRadioButton);
		expRadioButton = new JRadioButton();
		expRadioButton.setSelected(true);
		nExpRadioButton = new JRadioButton();
		ButtonGroup bgExp = new ButtonGroup();
		bgExp.add(expRadioButton);
		bgExp.add(nExpRadioButton);

		JPanel optionDisp = new JPanel();
		optionDisp.setLayout(new FlowLayout(FlowLayout.LEFT));
		JPanel optionND = new JPanel();
		optionND.setLayout(new FlowLayout(FlowLayout.LEFT));
		optionDisp.add(new JLabel("         "));
		optionDisp.add(dispRadioButton, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
				GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
		// dispRadioButton.setSelected(true);
		optionDisp.add(new JLabel(" Display Results."), new GridBagConstraints(6, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
		optionND.add(new JLabel("         "));
		optionND.add(nDispRadioButton, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
				GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
		// expRadioButton.setSelected(true);
		optionND.add(new JLabel(" No Display."), new GridBagConstraints(6, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
				GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));

		JPanel panelDisp = new JPanel();
		panelDisp.setSize(300, 250);
		panelDisp.setLayout(new BoxLayout(panelDisp, BoxLayout.Y_AXIS));
		panelDisp.add(Box.createVerticalStrut(3));
		panelDisp.add(panelDisplay);
		panelDisp.add(Box.createVerticalStrut(3));
		panelDisp.add(optionDisp);
		panelDisp.add(Box.createVerticalStrut(3));
		panelDisp.add(optionND);

		JPanel optionExp = new JPanel();
		optionExp.setLayout(new FlowLayout(FlowLayout.LEFT));
		JPanel optionNE = new JPanel();
		optionNE.setLayout(new FlowLayout(FlowLayout.LEFT));

		optionExp.add(new JLabel("         "));
		optionExp.add(expRadioButton, new GridBagConstraints(6, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
				GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
		// dispRadioButton.setSelected(true);
		optionExp.add(new JLabel(" Export Results files."), new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
		optionNE.add(new JLabel("         "));
		optionNE.add(nExpRadioButton, new GridBagConstraints(6, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
				GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
		// expRadioButton.setSelected(true);
		optionNE.add(new JLabel(" No Export files."), new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));

		JPanel panelExp = new JPanel();
		panelExp.setSize(300, 250);
		panelExp.setLayout(new BoxLayout(panelExp, BoxLayout.Y_AXIS));
		panelExp.add(Box.createVerticalStrut(3));
		panelExp.add(panelExport);
		panelExp.add(Box.createVerticalStrut(3));
		panelExp.add(optionExp);
		panelExp.add(Box.createVerticalStrut(3));
		panelExp.add(optionNE);

		okButton1 = new JButton("");
		okButton1.setBounds(50, 100, 95, 30);
		ImageIcon iconOk = createImageIcon("images/okey.png");
		Icon iconOKCell = new ImageIcon(iconOk.getImage().getScaledInstance(17, 15, Image.SCALE_SMOOTH));
		okButton1.setIcon(iconOKCell);
		okButton1.setToolTipText("Click this button to import your file to table.");
		cancelButton = new JButton("");
		cancelButton.setBounds(50, 100, 95, 30);
		ImageIcon iconCancel = createImageIcon("images/delete.png");
		Icon iconCancelCell = new ImageIcon(iconCancel.getImage().getScaledInstance(17, 15, Image.SCALE_SMOOTH));
		cancelButton.setIcon(iconCancelCell);
		cancelButton.setToolTipText("Click this button to cancel.");
		panelOkCancel = new JPanel();
		panelOkCancel.setLayout(new FlowLayout());
		panelOkCancel.add(okButton1);
		panelOkCancel.add(cancelButton);

		ImageIcon iconBrowse = createImageIcon("images/browse.png");
		Icon iconBrowseCell = new ImageIcon(iconBrowse.getImage().getScaledInstance(15, 15, Image.SCALE_SMOOTH));

		JButton buttonImages = new JButton("");
		buttonImages.setIcon(iconBrowseCell);
		textImages = new TextField(20);
		textImages.setText(prefImages.get(BUNWARPJAPPLY_IMAGES_DEFAULT_PATH, ""));
		DirectoryListener listenerImages = new DirectoryListener("Browse for directory to collect images ", textImages,
				JFileChooser.FILES_AND_DIRECTORIES);
		directLabelImages = new JLabel("    ⊳   .lif Directory : ");
		directLabelImages.setFont(new Font("Helvetica", Font.BOLD, 12));
		buttonImages.addActionListener(listenerImages);
		panelImages = new JPanel(new FlowLayout(FlowLayout.LEFT));
		panelImages.add(directLabelImages);
		panelImages.add(textImages);
		panelImages.add(buttonImages);

		JButton buttonSave = new JButton("");
		buttonSave.setIcon(iconBrowseCell);
		panelSave = new JPanel(new FlowLayout(FlowLayout.LEFT));
		textSave = new TextField(20);
		textSave.setText(prefSave.get(BUNWARPJAPPLY_SAVE_DEFAULT_PATH, ""));
		DirectoryListener listenerSave = new DirectoryListener("Browse for directory to save files ", textSave,
				JFileChooser.FILES_AND_DIRECTORIES);
		JLabel directLabelSave = new JLabel("    ⊳   Results Directory : ");
		directLabelSave.setFont(new Font("Helvetica", Font.BOLD, 12));
		buttonSave.addActionListener(listenerSave);
		panelSave.add(directLabelSave);
		panelSave.add(textSave);
		panelSave.add(buttonSave);

		buttonTransf = new JButton("");
		buttonTransf.setIcon(iconBrowseCell);
		textTransf = new TextField(20);
		textTransf.setText(prefTransf.get(BUNWARPJAPPLY_TRANSF_DEFAULT_PATH, ""));
		DirectoryListener listenerTransf = new DirectoryListener("Browse for directory to collect images ", textTransf,
				JFileChooser.FILES_AND_DIRECTORIES);
		directLabelTransf = new JLabel("    ⊳   .txt Transformation file : ");
		directLabelTransf.setFont(new Font("Helvetica", Font.BOLD, 12));
		buttonTransf.addActionListener(listenerTransf);
		panelTransf = new JPanel(new FlowLayout(FlowLayout.LEFT));
		panelTransf.add(directLabelTransf);
		panelTransf.add(textTransf);
		panelTransf.add(buttonTransf);

		frameMain = new JFrame("bUnwarpJ Batch");
		frameMain.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		JSeparator separator1 = new JSeparator(SwingConstants.VERTICAL);
		JSeparator separator2 = new JSeparator(SwingConstants.VERTICAL);
		Dimension dime = separator1.getPreferredSize();
		dime.height = panelDisp.getPreferredSize().height;
		separator1.setPreferredSize(dime);
		separator2.setPreferredSize(dime);
		checkSeries = new JCheckBox("Check for series");
		JPanel checkSeriesPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		checkSeriesPanel.add(checkSeries);
		JPanel generalPanel = new JPanel();
		generalPanel.setLayout(new BoxLayout(generalPanel, BoxLayout.Y_AXIS));
		generalPanel.add(panelImages);
		generalPanel.add(panelSave);
		generalPanel.add(panelTransf);
		generalPanel.add(checkSeriesPanel);
		JPanel optionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		optionsPanel.add(panelDisp);
		optionsPanel.add(separator1);
		optionsPanel.add(panelExp);
		optionsPanel.add(separator2);
		optionsPanel.add(chPanelMain);
		generalPanel.add(optionsPanel);
		generalPanel.add(panelOkCancel);
		frameMain.add(generalPanel);
		frameMain.setSize(730, 430);
		frameMain.pack();
		try {
			for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
				if ("Nimbus".equals(info.getName())) {
					UIManager.setLookAndFeel(info.getClassName());
					break;
				}
			}
		} catch (Exception e) {
			// If Nimbus is not available, you can set the GUI to another look and feel.
		}
		frameMain.setVisible(true);

	}

	public static ImageIcon createImageIcon(String path) {
		java.net.URL imgURL = bUnwarpJApply_.class.getResource(path);
		if (imgURL != null) {
			return new ImageIcon(imgURL);
		} else {
			System.err.println("Couldn't find file: " + path);
			return null;
		}
	}

	public static ImagePlus[] openBF(String multiSeriesFileName, boolean splitC, boolean splitT, boolean splitZ,
			boolean autoScale, boolean crop, boolean allSeries) {
		ImporterOptions options;
		ImagePlus[] imps = null;
		try {
			options = new ImporterOptions();
			options.setId(multiSeriesFileName);
			options.setSplitChannels(splitC);
			options.setSplitTimepoints(splitT);
			options.setSplitFocalPlanes(splitZ);
			options.setAutoscale(autoScale);
			options.setStackFormat(ImporterOptions.VIEW_HYPERSTACK);
			options.setStackOrder(ImporterOptions.ORDER_XYCZT);
			options.setCrop(crop);
			options.setOpenAllSeries(allSeries);

			ImportProcess process = new ImportProcess(options);
			if (!process.execute())
				return null;
			DisplayHandler displayHandler = new DisplayHandler(process);
			if (options != null && options.isShowOMEXML()) {
				displayHandler.displayOMEXML();
			}
			List<ImagePlus> impsList = new ImagePlusReaderModified(process).readImages(false);
			imps = impsList.toArray(new ImagePlus[0]);
			if (options != null && options.showROIs()) {
				displayHandler.displayROIs(imps);
			}
			if (!options.isVirtual()) {
				process.getReader().close();
			}

		} catch (Exception e) {

			return null;
		}
		return imps;
	}

	public void showSeriesDialog(ImagePlus[] imps) {

		buttonCancel = new JButton("");
		ImageIcon iconCancel = createImageIcon("images/cancel.png");
		Icon iconCancelCell = new ImageIcon(iconCancel.getImage().getScaledInstance(15, 15, Image.SCALE_SMOOTH));
		buttonCancel.setIcon(iconCancelCell);
		buttonOk = new JButton("");
		ImageIcon iconOk = createImageIcon("images/ok.png");
		Icon iconOkCell = new ImageIcon(iconOk.getImage().getScaledInstance(15, 15, Image.SCALE_SMOOTH));
		buttonOk.setIcon(iconOkCell);
		buttonSelectAll = new JButton("Select All");
		buttonDeselectAll = new JButton("Deselect All");
		tableImages = new JTable();
		modelImages = new DefaultTableModel();
		modelImages.setColumnIdentifiers(columnNames);
		jScrollPaneImages = new JScrollPane(tableImages);
		jScrollPaneImages.setPreferredSize(new Dimension(650, 200));
		Object[][] dataTImages = new Object[imps.length][columnNames.length];
		for (int i = 0; i < dataTImages.length; i++)
			for (int j = 0; j < dataTImages[i].length; j++)
				dataTImages[i][j] = "";
		modelImages = new DefaultTableModel(dataTImages, columnNames) {

			@Override
			public Class<?> getColumnClass(int column) {
				if (getRowCount() >= 0) {
					Object value = getValueAt(0, column);
					if (value != null) {
						return getValueAt(0, column).getClass();
					}
				}

				return super.getColumnClass(column);
			}

			public boolean isCellEditable(int row, int col) {
				switch (col) {
				case 0:
					return true;
				default:
					return false;
				}
			}

		};

		tableImages.setModel(modelImages);
		ImagePlus[] lifs = imps;
		List<ImageIcon> iconsLif = new ArrayList<ImageIcon>();
		for (int i = 0; i < imps.length; i++)
			iconsLif.add(new ImageIcon(getScaledImage(stack2images(lifs[i])[0].getImage(), 90, 60)));

		for (int i = 0; i < modelImages.getRowCount(); i++) {
			modelImages.setValueAt(Boolean.TRUE, i, tableImages.convertColumnIndexToModel(0));
			modelImages.setValueAt(
					"Serie: " + (i + 1) + " Title: " + imps[i].getShortTitle() + " " + imps[i].getWidth() + " x "
							+ imps[i].getHeight() + " : " + imps[i].getNChannels() + "C" + " x " + imps[i].getNFrames(),
					i, tableImages.convertColumnIndexToModel(1));

			modelImages.setValueAt(iconsLif.get(i), i, tableImages.convertColumnIndexToModel(2));

		}
		tableImages.setModel(modelImages);
		tableImages.setSelectionBackground(new Color(229, 255, 204));
		tableImages.setSelectionForeground(new Color(0, 102, 0));
		DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
		centerRenderer.setHorizontalAlignment(JLabel.CENTER);
		tableImages.setDefaultRenderer(String.class, centerRenderer);
		tableImages.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		tableImages.setRowHeight(60);
		tableImages.setAutoCreateRowSorter(true);
		tableImages.getTableHeader().setDefaultRenderer(new SimpleHeaderRenderer());
		tableImages.getColumnModel().getColumn(0).setPreferredWidth(100);
		tableImages.getColumnModel().getColumn(1).setPreferredWidth(450);
		tableImages.getColumnModel().getColumn(2).setPreferredWidth(100);
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		JPanel imagePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		imagePanel.add(jScrollPaneImages);
		JPanel selectPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		selectPanel.add(buttonDeselectAll);
		selectPanel.add(buttonSelectAll);
		JPanel okCancelPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		okCancelPanel.add(buttonOk);
		okCancelPanel.add(buttonCancel);
		JPanel panelTotal = new JPanel(new FlowLayout(FlowLayout.CENTER));
		panelTotal.add(selectPanel);
		panelTotal.add(Box.createHorizontalStrut(20));
		panelTotal.add(okCancelPanel);
		mainPanel.add(imagePanel);
		mainPanel.add(panelTotal);
		frameSeries = new JFrame();
		frameSeries.setTitle("Series Option");
		frameSeries.setResizable(false);
		frameSeries.add(mainPanel);
		frameSeries.pack();
		frameSeries.setSize(660, 300);
		frameSeries.setLocationRelativeTo(null);
		frameSeries.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frameSeries.setVisible(true);
		buttonDeselectAll.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				for (int i = 0; i < modelImages.getRowCount(); i++)
					modelImages.setValueAt(Boolean.FALSE, i, tableImages.convertColumnIndexToModel(0));

			}
		});
		buttonSelectAll.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				for (int i = 0; i < modelImages.getRowCount(); i++)
					modelImages.setValueAt(Boolean.TRUE, i, tableImages.convertColumnIndexToModel(0));

			}
		});
		buttonCancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				frameSeries.dispatchEvent(new WindowEvent(frameSeries, WindowEvent.WINDOW_CLOSING));

			}
		});

	}

	public static Image getScaledImage(Image srcImg, int w, int h) {
		BufferedImage resizedImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2 = resizedImg.createGraphics();
		g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g2.drawImage(srcImg, 0, 0, w, h, null);
		g2.dispose();
		return resizedImg;
	}

	public static ImagePlus[] stack2images(ImagePlus imp) {
		String sLabel = imp.getTitle();
		String sImLabel = "";
		ImageStack stack = imp.getStack();

		int sz = stack.getSize();
		int currentSlice = imp.getCurrentSlice(); // to reset ***

		DecimalFormat df = new DecimalFormat("0000"); // for title
		ImagePlus[] arrayOfImages = new ImagePlus[imp.getStack().getSize()];
		for (int n = 1; n <= sz; ++n) {
			imp.setSlice(n); // activate next slice ***

			// Get current image processor from stack. What ever is
			// used here should do a COPY pixels from old processor to
			// new. For instance, ImageProcessor.crop() returns copy.
			ImageProcessor ip = imp.getProcessor(); // ***
			ImageProcessor newip = ip.createProcessor(ip.getWidth(), ip.getHeight());
			newip.setPixels(ip.getPixelsCopy());

			// Create a suitable label, using the slice label if possible
			sImLabel = imp.getStack().getSliceLabel(n);
			if (sImLabel == null || sImLabel.length() < 1) {
				sImLabel = "slice" + df.format(n) + "_" + sLabel;
			}
			// Create new image corresponding to this slice.
			ImagePlus im = new ImagePlus(sImLabel, newip);
			im.setCalibration(imp.getCalibration());
			arrayOfImages[n - 1] = im;

			// Show this image.
			// imp.show();
		}
		// Reset original stack state.
		imp.setSlice(currentSlice); // ***
		if (imp.isProcessor()) {
			ImageProcessor ip = imp.getProcessor();
			ip.setPixels(ip.getPixels()); // ***
		}
		imp.setSlice(currentSlice);
		return arrayOfImages;
	}

}