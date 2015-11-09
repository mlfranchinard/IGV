/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2015 Broad Institute
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.broad.igv.feature.tribble;

import org.broad.igv.Globals;
import org.broad.igv.cli_plugin.Argument;
import org.broad.igv.cli_plugin.LineFeatureDecoder;
import org.broad.igv.cli_plugin.LineFeatureEncoder;
import org.broad.igv.feature.*;
import org.broad.igv.feature.genome.Genome;
import org.broad.igv.ui.color.ColorUtilities;
import org.broad.igv.util.StringUtils;
import org.broad.igv.util.collections.MultiMap;
import htsjdk.tribble.Feature;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.broad.igv.feature.genome.GenomeManager;
import org.broad.igv.ui.IGV;


/**
 * Created by IntelliJ IDEA.
 * User: jrobinso
 * Date: Dec 20, 2009
 * Time: 10:15:49 PM
 */
public class IGVBEDCodec extends UCSCCodec<BasicFeature> implements LineFeatureEncoder<Feature>, LineFeatureDecoder<BasicFeature> {

    static final Pattern BR_PATTERN = Pattern.compile("<br>");
    static final Pattern EQ_PATTERN = Pattern.compile("=");

    enum FeatureType {BED, GAPPED_PEAK};

    Genome genome;
    FeatureType featureType;

    public IGVBEDCodec() {
        this(null);
    }

    public IGVBEDCodec(Genome genome) {
        this(genome, FeatureType.BED);
    }

    public IGVBEDCodec(Genome genome, FeatureType featureType) {
        super(BasicFeature.class);
        this.featureType = featureType;
        this.genome = genome;
    }

    //@Override
    public BasicFeature decode(String[] tokens) {

        // The first 3 columns are non optional for BED.  We will relax this
        // and only require 2.
        int tokenCount = tokens.length;

        if (tokenCount < 2) {
            return null;
        }

        String c = tokens[0];
        String chr = genome == null ? c : genome.getChromosomeAlias(c);
        //find IGV from genome
        IGV igv = GenomeManager.getIGV(genome);

        //BED format, and IGV, use starting element as 0.
        int start = Integer.parseInt(tokens[1]);

        int end = start + 1;
        if (tokenCount > 2) {
            end = Integer.parseInt(tokens[2]);
        }

        BasicFeature feature = spliceJunctions ?
                new SpliceJunctionFeature(chr, start, end) :
                new BasicFeature(chr, start, end);

        // The rest of the columns are optional.  Stop parsing upon encountering
        // a non-expected value

        // Name
        if (tokenCount > 3) {
            if (isGffTags()) {
                MultiMap<String, String> atts = new MultiMap<String, String>();
                tagHelper.parseAttributes(tokens[3], atts);
                String name = tagHelper.getName(atts);
                feature.setName(name);

                String id = atts.get("ID");
                if (id != null) {
                    FeatureDB.addFeature(id, feature, genome, igv);
                    feature.setIdentifier(id);
                } else {
                    feature.setIdentifier(name);
                }
                String alias = atts.get("Alias");
                if (alias != null) {
                    FeatureDB.addFeature(alias, feature, genome, igv);
                }
                String geneSymbols = atts.get("Symbol");
                if (geneSymbols != null) {
                    String[] symbols = geneSymbols.split(",");
                    for (String sym : symbols) {
                        FeatureDB.addFeature(sym.trim(), feature, genome, igv);
                    }
                }

                feature.setAttributes(atts);


            } else {
                String name = tokens[3].replaceAll("\"", "");
                if(name.equals(".")) name = "";   // Convention
                feature.setName(name);
                feature.setIdentifier(name);
            }
        }

        // Bed files are not always to-spec after the name field.  Stop parsing when we find an unexpected column.
        // Score

        if (tokenCount > 4) {
            try {
                float score = Float.parseFloat(tokens[4]);
                feature.setScore(score);
                if (spliceJunctions) {
                    ((SpliceJunctionFeature) feature).setJunctionDepth((int) score);
                }
            } catch (NumberFormatException numberFormatException) {

                // Unexpected, but does not invalidate the previous values.
                // Stop parsing the line here but keep the feature
                // Don't log, would just slow parsing down.
                return feature;
            }
        }

        // Strand
        if (tokenCount > 5) {
            String strandString = tokens[5].trim();
            char strand = (strandString.length() == 0)
                    ? ' ' : strandString.charAt(0);

            if (strand == '-') {
                feature.setStrand(Strand.NEGATIVE);
            } else if (strand == '+') {
                feature.setStrand(Strand.POSITIVE);
            } else {
                feature.setStrand(Strand.NONE);
            }
        }

        // Thick ends
        if (tokenCount > 7) {
            try {
                int thickStart = Integer.parseInt(tokens[6]);
                int thickEnd = Integer.parseInt(tokens[7]);
                if(thickStart < start || thickStart > end || thickEnd < start || thickEnd > end) {
                    return feature;
                }
                feature.setThickStart(Integer.parseInt(tokens[6]));
                feature.setThickEnd(Integer.parseInt(tokens[7]));
            } catch (NumberFormatException e) {
                return feature;
            }
        }


        // Color
        if (tokenCount > 8 && featureType != FeatureType.GAPPED_PEAK) {
            String colorString = tokens[8];
            if (colorString.trim().length() > 0 && !colorString.equals(".")) {
                feature.setColor(ColorUtilities.stringToColor(colorString));
            }
        }

        // Exons
        if (tokenCount > 11) {
            createExons(start, tokens, feature, chr, feature.getStrand());
            //todo: some refactoring that allows this hack to be removed
            if (spliceJunctions) {
                SpliceJunctionFeature junctionFeature = (SpliceJunctionFeature) feature;

                List<Exon> exons = feature.getExons();

                junctionFeature.setJunctionStart(start + exons.get(0).getLength());
                junctionFeature.setJunctionEnd(end - exons.get(1).getLength());

            }
        }

        if(tokenCount > 14 && featureType == FeatureType.GAPPED_PEAK) {
            MultiMap<String, String> attributes = new MultiMap<String, String>();
            attributes.put("Signal Value", tokens[12]);
            attributes.put("pValue (-log10)", tokens[13]);
            attributes.put("qValue (-log10)", tokens[14]);
            feature.setAttributes(attributes);
        }

        return feature;
    }

    private String[] tokens = new String[50];

    @Override
    public BasicFeature decode(String nextLine) {

        String trimLine = nextLine.trim();
        if (trimLine.length() == 0) {
            return null;
        }

        if (nextLine.startsWith("#") || nextLine.startsWith("track") || nextLine.startsWith("browser")) {
            return null;
        }

        tokens = Globals.whitespacePattern.split(trimLine);
        return decode(tokens);
    }


    /**
     * This function returns true iff the File potentialInput can be parsed by this
     * codec.
     * <p/>
     * There is an assumption that there's never a situation where two different Codecs
     * return true for the same file.  If this occurs, the recommendation would be to error out.
     * <p/>
     * Note this function must never throw an error.  All errors should be trapped
     * and false returned.
     *
     * @param path the file to test for parsability with this codec
     * @return true if potentialInput can be parsed, false otherwise
     */
    @Override
    public boolean canDecode(String path) {
        return path.toLowerCase().endsWith(".bed") || path.toLowerCase().endsWith(".bed.gz");
    }


    private void createExons(int start, String[] tokens, BasicFeature gene, String chr,
                             Strand strand) throws NumberFormatException {

        int cdStart = Integer.parseInt(tokens[6]);
        int cdEnd = Integer.parseInt(tokens[7]);

        int exonCount = Integer.parseInt(tokens[9]);
        String[] exonSizes = Globals.commaPattern.split(tokens[10]);
        String[] startsBuffer = Globals.commaPattern.split(tokens[11]);

        int exonNumber = (strand == Strand.NEGATIVE ? exonCount : 1);

        if (startsBuffer.length == exonSizes.length) {
            for (int i = 0; i < startsBuffer.length; i++) {
                int exonStart = start + Integer.parseInt(startsBuffer[i]);
                int exonEnd = exonStart + Integer.parseInt(exonSizes[i]);
                Exon exon = new Exon(chr, exonStart, exonEnd, strand);
                exon.setCodingStart(cdStart);
                exon.setCodingEnd(cdEnd);
                gene.addExon(exon);

                exon.setNumber(exonNumber);
                if (strand == Strand.NEGATIVE) {
                    exonNumber--;
                } else {
                    exonNumber++;
                }

            }
        }
    }

    /**
     * Encode a feature as a BED string.
     *
     * @param feature - feature to encode
     * @return the encoded string
     */
    public String encode(Feature feature) {

        StringBuffer buffer = new StringBuffer();

        buffer.append(feature.getChr());
        buffer.append("\t");
        final int featureStart = feature.getStart();
        buffer.append(String.valueOf(featureStart));
        buffer.append("\t");
        buffer.append(String.valueOf(feature.getEnd()));

        BasicFeature basicFeature = null;

        if (!(feature instanceof BasicFeature)) {
            return buffer.toString();
        } else {
            basicFeature = (BasicFeature) feature;
        }

        if (basicFeature.getName() != null || (isGffTags() && basicFeature.getDescription() != null)) {

            buffer.append("\t");

            if (isGffTags() && basicFeature.getDescription() != null) {
                // mRNA<br>ID = LOC_Os01g01010.2<br>Name = LOC_Os01g01010.2<br>Parent = LOC_Os01g01010<br>
                //ID=LOC_Os01g01010.1:exon_1;Parent=LOC_Os01g01010.1
                String[] attrs = BR_PATTERN.split(basicFeature.getDescription());
                buffer.append("\"");
                for (String att : attrs) {
                    String[] kv = EQ_PATTERN.split(att, 2);
                    if (kv.length > 1) {
                        buffer.append(kv[0].trim());
                        buffer.append("=");
                        String value = kv[1].trim();
                        buffer.append(StringUtils.encodeURL(value));
                        buffer.append(";");
                    }
                }
                buffer.append("\"");
            } else {
                buffer.append(basicFeature.getName());
            }

            boolean more = !Float.isNaN(basicFeature.getScore()) || basicFeature.getStrand() != Strand.NONE ||
                    basicFeature.getColor() != null || basicFeature.getExonCount() > 0;

            if (more) {
                buffer.append("\t");
                // UCSC scores are integers between 0 and 1000, but
                float score = basicFeature.getScore();
                if (Float.isNaN(score)) {
                    buffer.append("1000");

                } else {
                    boolean isInt = (Math.floor(score) == score);
                    buffer.append(String.valueOf(isInt ? (int) score : score));
                }


                more = basicFeature.getStrand() != Strand.NONE || basicFeature.getColor() != null || basicFeature.getExonCount() > 0;
                if (more) {
                    buffer.append("\t");
                    Strand strand = basicFeature.getStrand();
                    if (strand == Strand.NONE) buffer.append(" ");
                    else if (strand == Strand.POSITIVE) buffer.append("+");
                    else if (strand == Strand.NEGATIVE) buffer.append("-");

                    more = basicFeature.getColor() != null || basicFeature.getExonCount() > 0;

                    if (more) {
                        // Must continue if basicFeature has color or exons
                        java.util.List<Exon> exons = basicFeature.getExons();
                        if (basicFeature.getColor() != null || exons != null) {
                            buffer.append("\t");
                            buffer.append(String.valueOf(basicFeature.getThickStart()));
                            buffer.append("\t");
                            buffer.append(String.valueOf(basicFeature.getThickEnd()));
                            buffer.append("\t");

                            java.awt.Color c = basicFeature.getColor();
                            buffer.append(c == null ? "." : ColorUtilities.colorToString(c));
                            buffer.append("\t");

                            if (exons != null && exons.size() > 0) {
                                buffer.append(String.valueOf(exons.size()));
                                buffer.append("\t");

                                for (Exon exon : exons) {
                                    buffer.append(String.valueOf(exon.getLength()));
                                    buffer.append(",");
                                }
                                buffer.append("\t");
                                for (Exon exon : exons) {
                                    int exonStart = exon.getStart() - featureStart;
                                    buffer.append(String.valueOf(exonStart));
                                    buffer.append(",");
                                }

                            }
                        }
                    }
                }
            }
        }

        return buffer.toString();
    }

    @Override
    public int getNumCols(String line) {
        return line.split("\t").length;
    }

    @Override
    public String getHeader() {
        return null;
    }

    @Override
    public void setInputs(List<String> commands, Map<Argument, Object> argumentMap, Argument argument) {
        //pass
    }
}


