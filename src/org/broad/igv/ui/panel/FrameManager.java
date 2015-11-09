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

package org.broad.igv.ui.panel;

import org.broad.igv.PreferenceManager;
import org.broad.igv.feature.Locus;
import org.broad.igv.feature.exome.ExomeReferenceFrame;
import org.broad.igv.lists.GeneList;
import org.broad.igv.track.FeatureTrack;
import org.broad.igv.track.RegionScoreType;
import org.broad.igv.track.Track;
import org.broad.igv.ui.IGV;
import org.broad.igv.ui.action.SearchCommand;
import org.broad.igv.ui.util.MessageUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

/**
 * @author jrobinso
 * @date Sep 10, 2010
 */
/**
 * Aout 2015
 * @author mlfranchinard
 * Com redmine:FrameManager.java
 */
public class FrameManager {

   
    private static HashMap<Integer,List<ReferenceFrame>> frames = new HashMap<Integer,List<ReferenceFrame>>();  
    private static HashMap<Integer,ReferenceFrame> defaultFrames = new HashMap<Integer,ReferenceFrame>();
   
    private static HashMap <Integer, Boolean> exomeMode = new HashMap<Integer, Boolean>();

    public static final String DEFAULT_FRAME_NAME = "genome";

   /* static {
        defaultFrames.add(getDefaultFrame());
    }*/

    public synchronized static ReferenceFrame getDefaultFrame(int id) {
        if (defaultFrames.containsKey(id)) {
            return defaultFrames.get(id);
        }else if(IGV.getIGVs().containsKey(id)){
            ReferenceFrame ref = new ReferenceFrame(DEFAULT_FRAME_NAME, id);
            List lframe = new ArrayList();
            lframe.add(ref);
            defaultFrames.put(id, ref);
            frames.put(id, lframe);
            exomeMode.put(id, false);
            exomeTracks.put(id, null);
            return ref;
        }
        throw new RuntimeException("No IGV with this ID: "+String.valueOf(id));
    }
    
    public synchronized static ReferenceFrame getDefaultFrame() {
        return getDefaultFrame(IGV.getCID());
    }
    
    public synchronized static void destroyFrame(int id){
        if (defaultFrames.containsKey(id)){
            frames.remove(id);
            exomeMode.remove(id);
            exomeTracks.remove(id);
            defaultFrames.remove(id);
        }
    }

    /**
     * Set exome mode.
     *
     * @param b
     * @param showTrackMenu
     * @return true if a change was made,
     *         false if not.
     */
    public static boolean setExomeMode(boolean b, boolean showTrackMenu, int id) {
        if (exomeMode.containsKey(id)){
            if (b == exomeMode.get(id)) return false;  // No change
            if (b) {
                return switchToExomeMode(showTrackMenu, id);
            } else {
                return switchToGenomeMode(id);
            }
        }
        throw new RuntimeException("No IGV with this ID: "+String.valueOf(id));
    }

    public static boolean setExomeMode(boolean b, boolean showTrackMenu) {
        return setExomeMode(b,showTrackMenu,IGV.getCID());
    }

    public static boolean isExomeMode(int id) {
        return (exomeMode.containsKey(id) && exomeMode.get(id));
    }

    public static boolean isExomeMode() {
        return isExomeMode(IGV.getCID());
    }
   
    static HashMap<Integer,FeatureTrack> exomeTracks = new HashMap<Integer,FeatureTrack>();

    private static boolean switchToExomeMode(boolean showTrackMenu, int id) {

        Frame parent = IGV.hasInstance() ? IGV.getInstance().getMainFrame() : null;
        List<FeatureTrack> featureTracks = IGV.getIGV(id).getFeatureTracks();
        if (featureTracks.size() == 1) {
            exomeTracks.put(id, featureTracks.get(0));
        } else {
            if (exomeTracks.get(id) == null || showTrackMenu) {
                FeatureTrackSelectionDialog dlg = new FeatureTrackSelectionDialog(parent);
                dlg.setVisible(true);
                if (dlg.getIsCancelled()) return false;
                exomeTracks.put(id, dlg.getSelectedTrack());
            }
        }

        if (exomeTracks.get(id) == null) return false;

        ExomeReferenceFrame exomeFrame = new ExomeReferenceFrame(defaultFrames.get(id), exomeTracks.get(id));

        Locus locus = new Locus(defaultFrames.get(id).getChrName(), (int) defaultFrames.get(id).getOrigin(), (int) defaultFrames.get(id).getEnd());
        exomeFrame.jumpTo(locus);
        defaultFrames.put(id,exomeFrame);
        List lframe = new ArrayList();
        lframe.add(defaultFrames.get(id));
        frames.put(id,lframe);
        exomeMode.put(id, true);
        return true;
    }
    private static boolean switchToExomeMode(boolean showTrackMenu) {
        return switchToExomeMode(showTrackMenu,IGV.getCID());
    }

    private static boolean switchToGenomeMode(int id) {
        ReferenceFrame def = defaultFrames.get(id);
        ReferenceFrame refFrame = new ReferenceFrame(def);

        Locus locus = new Locus(def.getChrName(), (int) def.getOrigin(), (int) def.getEnd());
        refFrame.jumpTo(locus);
        defaultFrames.put(id, refFrame);
        List lframe = new ArrayList();
        lframe.add(defaultFrames.get(id));
        frames.put(id,lframe);
        exomeMode.put(id, false);
        return true;
    }

    private static boolean switchToGenomeMode() {
        return switchToGenomeMode(IGV.getCID());
    }
    
    public static List<ReferenceFrame> getFrames(int id) {
        if (frames.get(id)==null){
            List lframe = new ArrayList();
            lframe.add(defaultFrames.get(id));
            frames.put(id,lframe);
        }
        return frames.get(id);
    }

    public static List<ReferenceFrame> getFrames() {
        return getFrames(IGV.getCID());
    }
    
    public static ReferenceFrame getFrame(String frameName, int id){
        for(ReferenceFrame frame: frames.get(id)){
            if(frame.getName().equals(frameName)){
                return frame;
            }
        }
        return null;
    }
    
    public static ReferenceFrame getFrame(String frameName){
        return getFrame(frameName,IGV.getCID());
    }

    public static void setFrames(List<ReferenceFrame> f, int id) {
        frames.put(id, f);
    }

    public static void setFrames(List<ReferenceFrame> f) {
        setFrames(f,IGV.getCID());
    }
    
    public static boolean isGeneListMode(int id) {
        return frames.containsKey(id) && frames.get(id).size() > 1;
    }

    public static boolean isGeneListMode() {
        return isGeneListMode(IGV.getCID());
    }

    public static void setToDefaultFrame(String searchString, int id) {
        //frames.clear();
        if (searchString != null) {
            Locus locus = getLocus(searchString, 0);
            if (locus != null) {
                getDefaultFrame(id).jumpTo(locus);
            }
        }
        List lframe = new ArrayList();
        lframe.add(defaultFrames.get(id));
        frames.put(id,lframe);
        getDefaultFrame(id).recordHistory();
    }
    
    public static void setToDefaultFrame(String searchString) {
        setToDefaultFrame(searchString,IGV.getCID());
    }

    private static boolean addNewFrame(String searchString, int id){
        boolean locusAdded = false;
        Locus locus = getLocus(searchString,id);
        if (locus != null) {
            ReferenceFrame referenceFrame = new ReferenceFrame(searchString, id);
            referenceFrame.jumpTo(locus);
            //List lframe = new ArrayList();
            //lframe.add(referenceFrame);
            //frames.put(id,lframe);
            frames.get(id).add(referenceFrame);
            locusAdded = frames.get(id).contains(referenceFrame);
        }
        return locusAdded;
    }

    private static boolean addNewFrame(String searchString){
        return addNewFrame(searchString,IGV.getCID());
    }

    public static void resetFrames(GeneList gl, int id) {

        //frames.clear();

        if (gl == null) {
            List lframe = new ArrayList();
            lframe.add(getDefaultFrame(id));
            frames.put(id,lframe);
        } else {
            List<String> lociNotFound = new ArrayList();
            List<String> loci = gl.getLoci();
            if (loci.size() == 1) {
                Locus locus = getLocus(loci.get(0),id);
                if (locus == null) {
                    lociNotFound.add(loci.get(0));
                } else {
                    IGV.getIGV(id).getSession().setCurrentGeneList(null);
                    getDefaultFrame(id).jumpTo(locus.getChr(), locus.getStart(), locus.getEnd());
                }
            } else {
                for (String searchString : gl.getLoci()) {
                    if(!addNewFrame(searchString, id)){
                        lociNotFound.add(searchString);
                    }
                }
            }

            if (lociNotFound.size() > 1) {
                StringBuffer message = new StringBuffer();
                message.append("<html>The following loci could not be found in the currently loaded annotation sets: <br>");
                for (String s : lociNotFound) {
                    message.append(s + " ");
                }
                MessageUtils.showMessage(message.toString());

            }
        }
    }

    public static void resetFrames(GeneList gl) {
        resetFrames(gl,IGV.getCID());
    }
    
    /**
     * @return The minimum scale among all active frames
     *         TODO -- track this with "rescale" events, rather than compute on the fly
     */
    public static double getMinimumScale(int id) {
        double minScale = Double.MAX_VALUE;
        for (ReferenceFrame frame : frames.get(id)) {
            minScale = Math.min(minScale, frame.getScale());
        }
        return minScale;
    }

    public static double getMinimumScale() {
        return getMinimumScale(IGV.getCID());
    }

    /**
     * Uses default flanking region with
     * {@link #getLocus(String, int)}
     * @param searchString
     * @return
     */
    public static Locus getLocus(String searchString, int id) {
        int flankingRegion = PreferenceManager.getInstance().getAsInt(PreferenceManager.FLANKING_REGION);
        return getLocus(searchString, flankingRegion, id);
    }
    
    public static Locus getLocus(String searchString) {
        return getLocus(searchString,IGV.getCID());
    }

    /**
     * Runs a search for the specified string, and returns a locus
     * of the given region with additional space on each side.
     * Note: We DO NOT add the flanking region if the {@code searchString}
     * is a locus (e.g. chr1:50-100), only if it's a gene or feature name (or something else)
     *
     * @param searchString
     * @param flankingRegion
     * @return The found locus, null if not found
     */
    public static Locus getLocus(String searchString, int flankingRegion, int id) {
        SearchCommand cmd = new SearchCommand(getDefaultFrame(id), searchString);
        List<SearchCommand.SearchResult> results = cmd.runSearch(searchString);
        Locus locus = null;
        for (SearchCommand.SearchResult result : results) {
            if (result.getType() != SearchCommand.ResultType.ERROR) {
                int delta = 0;

                if (result.getType() != SearchCommand.ResultType.LOCUS) {
                    if (flankingRegion < 0) {
                        delta = (-flankingRegion * (result.getEnd() - result.getStart())) / 100;
                    } else {
                        delta = flankingRegion;
                    }
                }

                int start = result.getStart() - delta;
                //Don't allow flanking region to extend past origin
                //There are some circumstances in which we render before origin (e.g. soft-clips)
                //so we are conservative
                if (start < 0 && result.getStart() >= -1) {
                    start = 0;
                }
                locus = new Locus(
                        result.getChr(),
                        start,
                        result.getEnd() + delta);
                //We just take the first result
                break;
            }
        }
        return locus;
    }

    public static void removeFrame(ReferenceFrame frame) {
        frames.get(frame.id).remove(frame);
    }


    public static void sortFrames(final Track t, int id) {

        Collections.sort(frames.get(id), new Comparator<ReferenceFrame>() {
            @Override
            public int compare(ReferenceFrame o1, ReferenceFrame o2) {
                float s1 = t.getRegionScore(o1.getChromosome().getName(), (int) o1.getOrigin(), (int) o1.getEnd(),
                        o1.getZoom(), RegionScoreType.SCORE, o1.getName());
                float s2  = t.getRegionScore(o2.getChromosome().getName(), (int) o2.getOrigin(), (int) o2.getEnd(),
                        o2.getZoom(), RegionScoreType.SCORE, o2.getName());
                return (s1 == s2 ? 0 : (s1 > s2) ? -1 : 1);
            }
        });

    }
    
    public static void sortFrames(final Track t) {
        sortFrames(t,IGV.getCID());
    }

}

