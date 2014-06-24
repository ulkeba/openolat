/**
 * <a href="http://www.openolat.org">
 * OpenOLAT - Online Learning and Training</a><br>
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); <br>
 * you may not use this file except in compliance with the License.<br>
 * You may obtain a copy of the License at the
 * <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache homepage</a>
 * <p>
 * Unless required by applicable law or agreed to in writing,<br>
 * software distributed under the License is distributed on an "AS IS" BASIS, <br>
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
 * See the License for the specific language governing permissions and <br>
 * limitations under the License.
 * <p>
 * Initial code contributed and copyrighted by<br>
 * frentix GmbH, http://www.frentix.com
 * <p>
 */
package org.olat.core.commons.services.image.spi;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;

import org.apache.commons.io.IOUtils;
import org.olat.core.commons.services.image.ImageHelperSPI;
import org.olat.core.commons.services.image.Size;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.util.StringHelper;
import org.olat.core.util.vfs.VFSLeaf;

/**
 * 
 * Initial date: 04.04.2014<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public abstract class AbstractImageHelper implements ImageHelperSPI {
	
	private static final OLog log = Tracing.createLoggerFor(AbstractImageHelper.class);

	@Override
	public Size getSize(VFSLeaf image, String suffix) {
		Size size = null;
		if(StringHelper.containsNonWhitespace(suffix)) {
			size = getImageSize(image, suffix);
		}
		if(size == null) {
			size = getImageSizeFallback(image);
		}
		return size;
	}
	
	private Size getImageSizeFallback(VFSLeaf media) {
		BufferedInputStream fileStrean = null;
		BufferedImage imageSrc = null;
		try {
			fileStrean = new BufferedInputStream(media.getInputStream());
			imageSrc = ImageIO.read(fileStrean);
			if (imageSrc == null) {
				// happens with faulty Java implementation, e.g. on MacOSX
				return null;
			}
			double realWidth = imageSrc.getWidth();
			double realHeight = imageSrc.getHeight();
			return new Size((int)realWidth, (int)realHeight, 0, 0, false);
		} catch (IOException e) {
			// log error, don't do anything else
			log.error("Problem while setting image size to fit for resource::" + media, e);
			return null;
		} finally {
			IOUtils.closeQuietly(fileStrean);
			if (imageSrc != null) {
				imageSrc.flush();
			}
		}
	}
	
	private Size getImageSize(VFSLeaf media, String suffix) {
		Size result = null;
		Iterator<ImageReader> iter = ImageIO.getImageReadersBySuffix(suffix);
		if (iter.hasNext()) {
			ImageReader reader = iter.next();
			try {
				ImageInputStream stream = new MemoryCacheImageInputStream(media.getInputStream());
				reader.setInput(stream);
				int readerMinIndex = reader.getMinIndex();
				int width = reader.getWidth(readerMinIndex);
				int height = reader.getHeight(readerMinIndex);
				result = new Size(width, height, 0, 0, false);
			} catch (IOException e) {
				log.error(e.getMessage());
			} finally {
				reader.dispose();
			}
		} else {
			log.error("No reader found for given format: " + suffix);
		}
		return result;
	}

}