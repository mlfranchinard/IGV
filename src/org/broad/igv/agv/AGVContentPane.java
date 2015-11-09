/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.broad.igv.agv;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.plaf.basic.BasicBorders;
import org.apache.log4j.Logger;
import org.broad.igv.ui.IGV;
import org.broad.igv.ui.IGVCommandBar;
import org.broad.igv.ui.IGVContentPane;
import org.broad.igv.ui.UIConstants;
import org.broad.igv.ui.panel.MainPanel;
import org.broad.igv.ui.panel.TrackPanel;
import org.broad.igv.ui.util.ApplicationStatusBar;

/**
 * The content pane for the IGV main window.
 * 
 * @author mlfranchinard
 */
public class AGVContentPane extends IGVContentPane{
    
    private static Logger log = Logger.getLogger(AGVContentPane.class);

    private JPanel commandBarPanel;
    private AGVCommandBar agvCommandBar;
    private MainPanel mainPanel;
    private ApplicationStatusBar statusBar;

    private AGV agv;

    /**
     * Creates new form IGV
     */
    public AGVContentPane(AGV agv) {
        super();
        
        setOpaque(true);    // Required by Swing

        this.agv = agv;

        // Create components

        setLayout(new BorderLayout());

        commandBarPanel = new JPanel();
        BoxLayout layout = new BoxLayout(commandBarPanel, BoxLayout.PAGE_AXIS);

        commandBarPanel.setLayout(layout);
        add(commandBarPanel, BorderLayout.NORTH);

        agvCommandBar = new AGVCommandBar(agv);
        agvCommandBar.setMinimumSize(new Dimension(250, 33));
        agvCommandBar.setBorder(new BasicBorders.MenuBarBorder(Color.GRAY, Color.GRAY));
        agvCommandBar.setAlignmentX(Component.BOTTOM_ALIGNMENT);
        commandBarPanel.add(agvCommandBar);


        mainPanel = new MainPanel(agv);
        add(mainPanel, BorderLayout.CENTER);

        statusBar = new ApplicationStatusBar();
        statusBar.setDebugGraphicsOptions(javax.swing.DebugGraphics.NONE_OPTION);
        add(statusBar, BorderLayout.SOUTH);


    }

    public void addCommandBar(JComponent component) {
        component.setBorder(new BasicBorders.MenuBarBorder(Color.GRAY, Color.GRAY));
        component.setAlignmentX(Component.BOTTOM_ALIGNMENT);
        commandBarPanel.add(component);
        commandBarPanel.invalidate();
    }

    public void removeCommandBar(JComponent component) {
        commandBarPanel.remove(component);
        commandBarPanel.invalidate();
    }

    @Override
    public Dimension getPreferredSize() {
        return UIConstants.preferredSize;
    }


    public void repaintDataPanels() {
        for (TrackPanel tp : mainPanel.getTrackPanels()) {
            tp.getScrollPane().getDataPanel().repaint();
        }
    }

    public void revalidateDataPanels() {
        for (TrackPanel tp : mainPanel.getTrackPanels()) {
            tp.getScrollPane().getDataPanel().revalidate();
        }
    }


    public void doRefresh() {

        mainPanel.revalidate();
        repaint();
        //getContentPane().repaint();
    }

    /**
     * Reset the default status message, which is the number of tracks loaded.
     */
    public void resetStatusMessage() {
        statusBar.setMessage("" + agv.getVisibleTrackCount() + " tracks loaded");

    }

    public MainPanel getMainPanel() {
        return mainPanel;
    }

    public AGVCommandBar getAGVCommandBar() {
        return agvCommandBar;
    }

    public void chromosomeChanged(String chrName) {
        agvCommandBar.chromosomeChanged(chrName);
    }

    public void updateCurrentCoordinates() {
        agvCommandBar.updateCurrentCoordinates();
    }

    public ApplicationStatusBar getStatusBar() {

        return statusBar;
    }
    
}
