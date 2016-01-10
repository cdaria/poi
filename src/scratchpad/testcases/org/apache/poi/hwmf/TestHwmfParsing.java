/* ====================================================================
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
==================================================================== */

package org.apache.poi.hwmf;

import static org.junit.Assert.assertEquals;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.imageio.ImageIO;

import org.apache.poi.POIDataSamples;
import org.apache.poi.hwmf.record.HwmfFill.HwmfImageRecord;
import org.apache.poi.hwmf.record.HwmfRecord;
import org.apache.poi.hwmf.usermodel.HwmfPicture;
import org.apache.poi.sl.usermodel.PictureData;
import org.apache.poi.sl.usermodel.PictureData.PictureType;
import org.apache.poi.sl.usermodel.SlideShow;
import org.apache.poi.sl.usermodel.SlideShowFactory;
import org.apache.poi.util.Units;
import org.junit.Ignore;
import org.junit.Test;

public class TestHwmfParsing {
    @Test
    public void parse() throws IOException {
        File f = POIDataSamples.getSlideShowInstance().getFile("santa.wmf");
        FileInputStream fis = new FileInputStream(f);
        HwmfPicture wmf = new HwmfPicture(fis);
        fis.close();
        List<HwmfRecord> records = wmf.getRecords();
        assertEquals(581, records.size());
    }
    
    @Test
    @Ignore("This is work-in-progress and not a real unit test ...")
    public void paint() throws IOException {
        File f = POIDataSamples.getSlideShowInstance().getFile("santa.wmf");
//        File f = new File("E:\\project\\poi\\misc\\govdocs-ppt", "000133-0001.wmf");
        FileInputStream fis = new FileInputStream(f);
        HwmfPicture wmf = new HwmfPicture(fis);
        fis.close();
        
        Dimension dim = wmf.getSize();
        int width = Units.pointsToPixel(dim.getWidth());
        // keep aspect ratio for height
        int height = Units.pointsToPixel(dim.getHeight());
        
        BufferedImage bufImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = bufImg.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        
        wmf.draw(g);

        g.dispose();
        
        ImageIO.write(bufImg, "PNG", new File("bla.png"));
    }

    @Test
    @Ignore("This is work-in-progress and not a real unit test ...")
    public void fetchWmfFromGovdocs() throws IOException {
        URL url = new URL("http://digitalcorpora.org/corpora/files/govdocs1/by_type/ppt.zip");
        File outdir = new File("build/ppt");
        outdir.mkdirs();
        ZipInputStream zis = new ZipInputStream(url.openStream());
        ZipEntry ze;
        while ((ze = zis.getNextEntry()) != null) {
            String basename = ze.getName().replaceAll(".*?([^/]+)\\.wmf", "$1");
            FilterInputStream fis = new FilterInputStream(zis){
                public void close() throws IOException {}
            };
            try {
                SlideShow<?,?> ss = SlideShowFactory.create(fis);
                int wmfIdx = 1;
                for (PictureData pd : ss.getPictureData()) {
                    if (pd.getType() != PictureType.WMF) continue;
                    byte wmfData[] = pd.getData();
                    String filename = String.format(Locale.ROOT, "%s-%04d.wmf", basename, wmfIdx);
                    FileOutputStream fos = new FileOutputStream(new File(outdir, filename));
                    fos.write(wmfData);
                    fos.close();
                    wmfIdx++;
                }
                ss.close();
            } catch (Exception e) {
                System.out.println(ze.getName()+" ignored.");
            }
        }
    }

    @Test
    @Ignore("This is work-in-progress and not a real unit test ...")
    public void parseWmfs() throws IOException {
        // parse and render the extracted wmfs from the fetchWmfFromGovdocs step
        boolean outputFiles = false;
        boolean renderWmf = true;
        File indir = new File("E:\\project\\poi\\misc\\govdocs-ppt");
        File outdir = new File("build/wmf");
        outdir.mkdirs();
        final String startFile = "";
        File files[] = indir.listFiles(new FileFilter() {
            boolean foundStartFile = false;
            public boolean accept(File pathname) {
                foundStartFile |= startFile.isEmpty() || pathname.getName().contains(startFile);
                return foundStartFile && pathname.getName().matches("(?i).*\\.wmf?$");
            }
        });
        for (File f : files) {
            try {
                String basename = f.getName().replaceAll(".*?([^/]+)\\.wmf", "$1");
                FileInputStream fis = new FileInputStream(f);
                HwmfPicture wmf = new HwmfPicture(fis);
                fis.close();
                
                int bmpIndex = 1;
                for (HwmfRecord r : wmf.getRecords()) {
                    if (r instanceof HwmfImageRecord) {
                        BufferedImage bi = ((HwmfImageRecord)r).getImage();
                        if (bi != null && outputFiles) {
                            String filename = String.format(Locale.ROOT, "%s-%04d.png", basename, bmpIndex);
                            ImageIO.write(bi, "PNG", new File(outdir, filename));
                        }
                        bmpIndex++;
                    }
                }

                if (renderWmf) {
                    Dimension dim = wmf.getSize();
                    int width = Units.pointsToPixel(dim.getWidth());
                    // keep aspect ratio for height
                    int height = Units.pointsToPixel(dim.getHeight());
                    
                    BufferedImage bufImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g = bufImg.createGraphics();
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                    g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
                    
                    wmf.draw(g);

                    g.dispose();
                    
                    ImageIO.write(bufImg, "PNG", new File(outdir, basename+".png"));
                }
            } catch (Exception e) {
                System.out.println(f.getName()+" ignored.");                
            }
        }
    }
}
