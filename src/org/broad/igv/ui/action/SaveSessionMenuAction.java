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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.broad.igv.ui.action;

//~--- non-JDK imports --------------------------------------------------------

import org.apache.log4j.Logger;
import org.broad.igv.PreferenceManager;
import org.broad.igv.session.Session;
import org.broad.igv.session.SessionWriter;
import org.broad.igv.ui.IGV;
import org.broad.igv.ui.UIConstants;
import org.broad.igv.ui.WaitCursorManager;
import org.broad.igv.ui.util.FileDialogUtils;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import org.broad.igv.agv.AGV;

/**
 * @author jrobinso
 */
public class SaveSessionMenuAction extends MenuAction {

    static Logger log = Logger.getLogger(SaveSessionMenuAction.class);
    IGV igv;
    
    ///AGV sessionFiles list
    //Com redmine: AGVSessionFiles (SaveSessionMenuAction.java)
    public static HashMap<Integer,File> AGVSessionFiles;
    /**
     *
     *
     * @param label
     * @param mnemonic
     * @param igv
     */
    public SaveSessionMenuAction(String label, int mnemonic, IGV igv) {
        super(label, null, mnemonic);
        this.igv = igv;
        AGVSessionFiles = new HashMap<Integer,File>();
    }

    /**
     * Method description
     *
     * @param e
     */
    @Override
    public void actionPerformed(ActionEvent e) {


        File sessionFile = null;

        String currentSessionFilePath = igv.getSession().getPath();

        String initFile = currentSessionFilePath == null ? UIConstants.DEFAULT_SESSION_FILE : currentSessionFilePath;
        sessionFile = FileDialogUtils.chooseFile("Save Session",
                PreferenceManager.getInstance().getLastSessionDirectory(),
                new File(initFile),
                FileDialogUtils.SAVE);


        if (sessionFile == null) {
            igv.resetStatusMessage();
            return;
        }

        
        String filePath = sessionFile.getAbsolutePath();
        if (!filePath.toLowerCase().endsWith(".xml")) {
            sessionFile = new File(filePath + ".xml");
        }

        /////////AGV
        if (IGV.hasAGVInstance()){
            for(AGV agv : IGV.getAGVs().values()){
                final File agvSessionFile = new File(sessionFile.getAbsolutePath().replace(".xml", "_"+agv.getID()+ ".xml"));
                AGVSessionFiles.put(agv.getID(), agvSessionFile);
            }
        }
        
        igv.setStatusBarMessage("Saving session to " + sessionFile.getAbsolutePath());


        final File sf = sessionFile;
        WaitCursorManager.CursorToken token = WaitCursorManager.showWaitCursor();
        try {
            saveSession(igv, sf);
            
            //////save AGV sessions
            if (IGV.hasAGVInstance()){
                for(AGV agv : IGV.getAGVs().values()){
                    saveSession(agv,AGVSessionFiles.get(agv.getID()));
                }
            }
            // No errors so save last location
            PreferenceManager.getInstance().setLastSessionDirectory(sf.getParentFile());

        } catch (Exception e2) {
            JOptionPane.showMessageDialog(igv.getMainFrame(), "There was an error writing to " + sf.getName() + "(" + e2.getMessage() + ")");
            log.error("Failed to save session!", e2);
        } finally {
            WaitCursorManager.removeWaitCursor(token);
            igv.resetStatusMessage();


        }


    }

    /**
     * Saves current IGV session to {@code targetFile}. As a side effect,
     * sets the current sessions path (does NOT set the last session directory)
     * @param igv
     * @param targetFile
     * @throws IOException
     */
    public static void saveSession(IGV igv, File targetFile) throws IOException{
        Session currentSession = igv.getSession();
        currentSession.setPath(targetFile.getAbsolutePath());
        (new SessionWriter(igv)).saveSession(currentSession, targetFile);
    }
}
