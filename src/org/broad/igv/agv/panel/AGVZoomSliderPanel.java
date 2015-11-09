/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.broad.igv.agv.panel;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import javax.swing.JPanel;
import javax.swing.event.MouseInputAdapter;
import org.broad.igv.agv.AGV;
import org.broad.igv.ui.event.ViewChange;
import org.broad.igv.ui.panel.FrameManager;
import org.broad.igv.ui.panel.ReferenceFrame;
import org.broad.igv.ui.util.IconFactory;

/**
 *
 * @author mlfranchinard
 */
public class AGVZoomSliderPanel extends JPanel {

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
    private AGV agv;
    public static HashMap<Integer,AGVZoomSliderPanel> zpanels = new HashMap<Integer,AGVZoomSliderPanel>();

    public AGVZoomSliderPanel(AGV agv){
        this(agv,FrameManager.getDefaultFrame(agv.getID()));
    }

    /**
     * @param referenceFrame The ReferenceFrame whose zoom level this panel will control
     */
    public AGVZoomSliderPanel(AGV agv, ReferenceFrame referenceFrame) {
        this.referenceFrame = referenceFrame;
        this.agv = agv;
        assert agv.getID()==referenceFrame.id;
        slider = IconFactory.getInstance().getIcon(IconFactory.IconID.SLIDER).getImage();
        zoomPlus = IconFactory.getInstance().getIcon(IconFactory.IconID.ZOOM_PLUS).getImage();
        zoomMinus = IconFactory.getInstance().getIcon(IconFactory.IconID.ZOOM_MINUS).getImage();
        zoomLevelRects = new Rectangle[numZoomLevels];

        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        init();
        zpanels.put(agv.getID(), this);
    }
    
    public AGVZoomSliderPanel getAGVZoomSliderPanel(int id){
        if (zpanels.containsKey(id)) return zpanels.get(id);
        throw new RuntimeException("Any AGVZoomSliderPanel with this id :"+id);
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

    public int setZoom(MouseEvent e) {

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


    public ReferenceFrame getViewContext() {
        if(referenceFrame == null) return FrameManager.getDefaultFrame(agv.getID());
        return referenceFrame;
    }

    public int toolZoom = -1;

    public void setToolZoom(int tz){
        toolZoom = tz ;
    }
    
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
            }
        };

        addMouseMotionListener(mouseAdapter);
        addMouseListener(mouseAdapter);
    }
}
