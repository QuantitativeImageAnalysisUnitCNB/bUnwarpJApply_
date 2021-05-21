

/*
 * #%L
 * OME Bio-Formats package for reading and converting biological file formats.
 * %%
 * Copyright (C) 2005 - 2015 Open Microscopy Environment:
 *   - Board of Regents of the University of Wisconsin-Madison
 *   - Glencoe Software, Inc.
 *   - University of Dundee
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;

import javax.imageio.ImageIO;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ByteProcessor;
import loci.common.services.DependencyException;
import loci.common.services.ServiceException;
import loci.common.services.ServiceFactory;
import loci.formats.FormatTools;
import loci.formats.ImageReader;
import loci.formats.meta.IMetadata;
import loci.formats.services.OMEXMLService;
import ome.xml.model.enums.DimensionOrder;
import ome.xml.model.enums.EnumerationException;
import ome.xml.model.enums.PixelType;
import ome.xml.model.primitives.PositiveInteger;
import loci.formats.out.OMETiffWriter;

/**
 * Converts the given files to OME-TIFF format.
 */
public class FileExport {

	public static void main(String[] args) throws Exception {
		if (args.length == 0) {
			System.out.println("Usage: java ConvertToOmeTiff file1 file2 ...");
			return;
		}

		ImageReader reader[] = new ImageReader[args.length];
		ImageReader readerFinal = new ImageReader();
		OMETiffWriter writer = new OMETiffWriter();
		for (int i = 0; i < args.length; i++) {
			String id = args[i];
			int dot = id.lastIndexOf(".");
			String outId = (dot >= 0 ? id.substring(0, dot) : id) + ".ome.tif";
			System.out.print("Converting " + id + " to " + outId + " ");

			// record metadata to OME-XML format
			ServiceFactory factory = new ServiceFactory();
			OMEXMLService service = factory.getInstance(OMEXMLService.class);
			IMetadata omexmlMeta = service.createOMEXMLMetadata();
			IMetadata omexmlMetaFinal = service.createOMEXMLMetadata();
			reader[i] = new ImageReader();
			reader[i].setMetadataStore(omexmlMeta);
			reader[i].setId(args[i]);
			// omexmlMeta.setPixelsSizeT(new PositiveInteger(1), 0);
			// configure OME-TIFF writer
			writer.setMetadataRetrieve(omexmlMeta);
			//writer.setInterleaved(reader[i].isInterleaved());
			writer.setId((args[i].lastIndexOf(".") >= 0 ? args[i].substring(0, args[i].lastIndexOf(".")) : args[i])
					+ ".ome.tif");

			// writer.setCompression("J2K");

			// write out image planes
			// readerFinal.setMetadataStore(omexmlMeta);
			// int seriesCount = reader.getSeriesCount();
			byte[] plane = new byte[FormatTools.getPlaneSize(reader[i])];
			for (int s = 0; s < reader[i].getSeriesCount(); s++) {
				reader[i].setSeries(s);
				writer.setSeries(s);
				reader[i].setId(bUnwarpJApply_.pathMergeds.get(s));
				// omexmlMetaFinal.setPixelsSizeT(new PositiveInteger(1), s);
				// omexmlMeta.setImageID(bUnwarpJApply_.pathMergeds.get(s), s);
				// omexmlMeta.setPixelsID(bUnwarpJApply_.pathMergeds.get(s), s);
				// omexmlMeta.setPixelsDimensionOrder(DimensionOrder.XYCZT, s);
				// omexmlMeta.setPixelsType(PixelType.UINT16, s);
				// readerFinal.setSeries(s);
				// IJ.log(reader.getSizeT()+"'");
				// omexmlMeta.setPixelsSizeT(new PositiveInteger(reader.getSizeT()), s);
				writer.setSeries(s);

				//int planeCount = readerFinal.getImageCount();
				for (int p = 0; p < reader[i].getImageCount(); p++) {
					// write plane to output file
					writer.saveBytes(s,reader[i].openBytes(p, reader[i].openBytes(p)));
					System.out.print(".");
				}
			}
			writer.close();
			reader[i].close();
			// readerFinal.close();
			System.out.println(" [done]");
			
		}

	}

}