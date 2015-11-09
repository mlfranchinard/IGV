/*
 * Copyright (c) 2007-2012 The Broad Institute, Inc.
 * SOFTWARE COPYRIGHT NOTICE
 * This software and its documentation are the copyright of the Broad Institute, Inc. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support whatsoever. The Broad Institute is not responsible for its use, misuse, or functionality.
 *
 * This software is licensed under the terms of the GNU Lesser General Public License (LGPL),
 * Version 2.1 which is available at http://www.opensource.org/licenses/lgpl-2.1.php.
 */

/*
 * ZoomSliderPanel.java
 *
 * Created on September 25, 2007, 12:03 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package org.broad.igv.ui.panel;

import org.broad.igv.ui.event.ViewChange;
import org.broad.igv.ui.util.IconFactory;

import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import java.awt.*;
import java.awt.event.MouseEvent;
import org.broad.igv.agv.panel.AGVZoomSliderPanel;
import org.broad.igv.ui.IGV;

/**
 * @author jrobinso
 */
public class ZoomAGVSliderPanel extends JPanel {

    static Color TICK_GRAY = new Color(90, 90, 90);
    static Color TICK_BLUE = new Color(25, 50, 200);

    //double imageScaleFactor = 0.8;
    Image slider;
    Image zoomPlus;
    Image zoomMinus;
    Rectangle zoomPlusRect;
    Rectangle zoomMinusRect;
    Rectangle[] zoomLevelRects;
    /**
     * Should correspond to "maxZoomLevel" in class referenceFrame.
     */
    int numZoomLevels = 25;


    private int minZoomLevel = 0;

    /**
     * Set the allowed zoom level, user cannot zoom out past this level
     *
     * @param minZoomLevel
     */
    public void setMinZoomLevel(int minZoomLevel){
        this.minZoomLevel = minZoomLevel;
    }

    private static final Color TRANSPARENT_GRAY = new Color(200, 200, 200, 150);
    private ReferenceFrame referenceFrame;

    public ZoomAGVSliderPanel(){
        this(null);
    }

    /**
     * @param referenceFrame The ReferenceFrame whose zoom level this panel will control
     */
    public ZoomAGVSliderPanel(ReferenceFrame referenceFrame) {
        this.referenceFrame = referenceFrame;
        slider = IconFactory.getInstance().getIcon(IconFactory.IconID.SLIDER).getImage();
        zoomPlus = IconFactory.getInstance().getIcon(IconFactory.IconID.ZOOM_SYNC_PLUS).getImage();
        zoomMinus = IconFactory.getInstance().getIcon(IconFactory.IconID.ZOOM_SYNC_MINUS).getImage();
        zoomLevelRects = new Rectangle[numZoomLevels];

        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        init();
    }

    private void updateTickCount() {
        int tmp = getViewContext().getMaxZoom() + 1;
        if (tmp != numZoomLevels) {
            numZoomLevels = tmp;
            zoomLevelRects = new Rectangle[numZoomLevels];
        }

    }


    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        updateTickCount();
        //if (this.isEnabled()) {
        paintHorizontal(g);
        //}

    }

    protected void paintHorizontal(Graphics g) {

        Graphics2D transGraphics = (Graphics2D) g.create();
        transGraphics.setColor(TRANSPARENT_GRAY);

        int buttonWidth = zoomPlus.getWidth(null);
        int buttonHeight = zoomPlus.getHeight(null);

        Insets insets = getInsets();
        int panelWidth = getWidth() - insets.left - insets.right;
        int panelHeight = getHeight() - insets.top - insets.bottom;


        final boolean enabled = isEnabled();
        g.setColor(enabled ? Color.BLACK : Color.LIGHT_GRAY);
        double x = insets.left;

        double xStep = ((double) (panelWidth - 2 * buttonWidth - 10)) / (numZoomLevels);

        int y = insets.top + (panelHeight - buttonHeight) / 2;
        g.drawImage(zoomMinus, (int) x, y, null);
        zoomMinusRect = new Rectangle((int) x, y, buttonWidth, buttonHeight);

        if (!isEnabled()) {
            transGraphics.fill(zoomMinusRect);
        }

        x += 5 + buttonWidth;

        int lastX = (int) (x - xStep);
        for (int i = 0; i < numZoomLevels; i++) {
            Rectangle zoomRect = new Rectangle((int) x, y, (int) (x - lastX), buttonHeight);
            int xLine = (int) (x + xStep / 2);
            g.drawLine(xLine, y + 3, xLine, y + buttonHeight - 4);
            zoomLevelRects[i] = zoomRect;
            lastX = (int) x;
            x += xStep;
        }

        x += 5;

        y = insets.top + panelHeight / 2 - 1;
        //g.drawLine(xTop, y, xBottom, y);

        y = insets.top + (panelHeight - buttonHeight) / 2;
        //if (isEnabled()) {
        g.drawImage(zoomPlus, (int) x, y, null);
        //}
        zoomPlusRect = new Rectangle((int) x, y, buttonWidth, buttonWidth);

        if (!isEnabled()) {
            transGraphics.fill(zoomPlusRect);
        }

        // Draw current level -- zoomIndex is the zoom level + 1. 

        int zoom = (toolZoom >= 0 ? toolZoom : getViewContext().getAdjustedZoom());

        if (enabled) {
            if (zoom >= 0 && zoom < zoomLevelRects.length) {
                Rectangle rect = zoomLevelRects[zoom];

                g.setColor(TICK_BLUE);
                g.fill3DRect(
                        (int) (rect.getX() + rect.getWidth() / 2) - 3,
                        (int) rect.getY(),
                        6,
                        (int) rect.getHeight(),
                        true);

                //y =  (int) (rect.getY() + (rect.getHeight() - slider.getHeight(null)) / 2);
                // temporary hack
                //if(zoomIndex == 12) y += 15;
                //g.drawImage(slider, x + 1, y, null);
            }
        }
        transGraphics.dispose();
    }

    int setZoom(MouseEvent e) {

        if (zoomPlusRect.contains(e.getX(), e.getY())) {
            toolZoom++;
        } else if (zoomMinusRect.contains(e.getX(), e.getY()) && toolZoom > minZoomLevel) {
            toolZoom--;
        } else {
            for (int i = 0; i < zoomLevelRects.length; i++) {
                Rectangle rect = zoomLevelRects[i];
                if (rect.contains(e.getX(), e.getY()) && i >= minZoomLevel) {
                    toolZoom = i;
                }
            }
        }
        return toolZoom;
    }


    private ReferenceFrame getViewContext() {
        if(referenceFrame == null) return FrameManager.getDefaultFrame(0);
        return referenceFrame;
    }

    int toolZoom = -1;

    private void init() {

        MouseInputAdapter mouseAdapter = new MouseInputAdapter() {

            int lastMousePressX = 0;

            @Override
            public void mouseExited(MouseEvent e) {

            }

            @Override
            public void mouseClicked(MouseEvent e) {

            }

            @Override
            public void mouseMoved(MouseEvent e) {
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (!isEnabled()) {
                    return;
                }
                toolZoom = Math.max(0, getViewContext().getAdjustedZoom());
                if (IGV.hasAGVInstance()){
                    for(AGVZoomSliderPanel zpanel: AGVZoomSliderPanel.zpanels.values()){
                        int toolZoomS = Math.max(0, zpanel.getViewContext().getAdjustedZoom());
                        zpanel.setToolZoom(toolZoomS);
                    }
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (!isEnabled()) {
                    return;
                }
                //Sometimes the zoom doesn't change, don't need to do anything in that case
                int oldToolZoom = toolZoom;
                int diff = setZoom(e) - oldToolZoom;
                if(diff == 0) {
                    toolZoom = -1;
                    
                    return;
                }

                repaint();

                int effectiveZoom = toolZoom + getViewContext().getMinZoom();

                ViewChange.ZoomCause event = new ViewChange.ZoomCause(effectiveZoom);
                getViewContext().getEventBus().post(event);
                toolZoom = -1;
                
                
                if (IGV.hasAGVInstance()){
                    for(AGVZoomSliderPanel zpanel: AGVZoomSliderPanel.zpanels.values()){
                        int oldToolZoomS = zpanel.toolZoom;
                        int diffS = zpanel.setZoom(e) - oldToolZoomS;
                        if(diffS == 0) {
                            zpanel.setToolZoom(-1);
                            continue;
                        }
                          
                        zpanel.repaint();
                        int effectiveZoomS = zpanel.toolZoom + getViewContext().getMinZoom();
                        ViewChange.ZoomCause eventS = new ViewChange.ZoomCause(effectiveZoomS);
                        zpanel.getViewContext().getEventBus().post(eventS);
                        zpanel.setToolZoom(-1);
                    }
                }
//                NamedRunnable runnable = new NamedRunnable() {
//                    public void run() {
//                        int effectiveZoom = toolZoom + getViewContext().getMinZoom();
//                        getViewContext().doSetZoom(effectiveZoom);
//                        toolZoom = -1;
//                    }
//
//                    public String getName() {
//                        return "Zoom to: " + toolZoom;
//                    }
//                };
//
//                LongRunningTask.submit(runnable);
            }


            @Override
            public void mouseDragged(MouseEvent e) {
                // Dragging zoom tool is disable.  Generates too many
                // repaint events.
                setZoom(e);
                repaint();
                
                if (IGV.hasAGVInstance()){
                    for(AGVZoomSliderPanel zpanel: AGVZoomSliderPanel.zpanels.values()){
                        zpanel.setZoom(e);
                        zpanel.repaint();
                    }
                }
            }
        };

        addMouseMotionListener(mouseAdapter);
        addMouseListener(mouseAdapter);
    }
}
