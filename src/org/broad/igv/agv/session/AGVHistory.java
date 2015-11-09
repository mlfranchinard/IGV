/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.broad.igv.agv.session;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.apache.log4j.Logger;
import org.broad.igv.Globals;
import org.broad.igv.lists.GeneListManager;
import org.broad.igv.session.History;
import org.broad.igv.ui.IGV;
import org.broad.igv.ui.action.SearchCommand;
import org.broad.igv.ui.event.ViewChange;
import org.broad.igv.ui.panel.FrameManager;

/**
 *
 * @author mlfranchinard
 */
public class AGVHistory extends History{
    
    private static Logger log = Logger.getLogger(AGVHistory.class);

    int maxEntries = 100;
    int currPos = 0;
    private int id;
    
    LinkedList<Entry> activeStack;
    List<Entry> allHistory;

    public AGVHistory(int maxEntries, int id) {
        super();
        this.id = id;
        this.maxEntries = maxEntries;
        activeStack = new LinkedList();
        allHistory = new ArrayList();
    }


    public void push(String s, int zoom) {

        if (s == null || s.length() == 0) {
            return;
        }
        // If in gene list mode disable history,  with the exception of "List" items
        if (FrameManager.isGeneListMode(0) && !s.startsWith("List")) {
            return;
        }

       if(activeStack.size() > 0 && s.equals(activeStack.peek().locus)) {
            return;
        }


        log.debug("History: " + s);
        allHistory.add(new Entry(s, zoom));

        while (currPos > 0) {
            activeStack.removeFirst();
            currPos--;
        }
        activeStack.addFirst(new Entry(s, zoom));
    }


    public void back() {
        if (canGoBack()) {
            currPos++;
            Entry item = activeStack.get(currPos);
            processItem(item);
        }
    }

    public void forward() {

        if (canGoForward()) {
            currPos--;
            Entry item = activeStack.get(currPos);
            processItem(item);
        }
    }

    public boolean canGoBack() {
        return activeStack.size() > 0 && currPos < (activeStack.size() - 1);
    }

    public boolean canGoForward() {
        return activeStack.size() > 0 && currPos > 0;
    }

    public void processItem(Entry entry) {

        if (entry != null) {
            String locus = entry.getLocus();
            if (locus.equals(Globals.CHR_ALL)){
                ViewChange.Cause event = new ViewChange.ChromosomeChangeCause(this, Globals.CHR_ALL);
                event.setRecordHistory(false);
                FrameManager.getDefaultFrame(id).getEventBus().post(event);
            }else if(locus.startsWith("List: ")) {
                String listName = locus.substring(6);
                IGV.getAGVInstance(id).setGeneList(GeneListManager.getInstance().getGeneList(listName), false);
            } else {
                if (FrameManager.isGeneListMode(id)) {
                    IGV.getAGVInstance(id).setGeneList(null, false);
                }
                (new SearchCommand(FrameManager.getDefaultFrame(id), locus, false)).execute();
                //Zoom should be implicit in the locus
                //FrameManager.getDefaultFrame().setZoom(entry.getZoom());
            }

        }
    }


    public Entry peekBack() {
        if (activeStack.size() == 0 || (currPos + 1) >= activeStack.size()) {
            return null;
        }
        return activeStack.get(currPos + 1);
    }

    public Entry peekForward() {
        if (activeStack.size() == 0 || (currPos - 1) < 0) {
            return null;
        }
        return activeStack.get(currPos - 1);
    }

    public void clear() {
        activeStack.clear();
        allHistory.clear();
        currPos = 0;
    }

    public void printStack() {
        System.out.println("curr pos=" + currPos);
        for (Entry s : activeStack) {
            System.out.println(s.getLocus());
        }
        System.out.println();
    }

    public List<Entry> getAllHistory() {
        return allHistory;
    }

    /*public static class Entry {
        private String locus;
        private int zoom;

        public Entry(String s, int zoom) {
            this.locus = s;
            this.zoom = zoom;
        }

        public String getLocus() {
            return locus;
        }

        public int getZoom() {
            return zoom;
        }

        public String toString() {
            return locus;
        }
    }*/
}
