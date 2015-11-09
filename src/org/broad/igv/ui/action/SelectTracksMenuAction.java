/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.broad.igv.ui.action;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import org.apache.log4j.Logger;
import org.broad.igv.track.AttributeManager;
import org.broad.igv.track.Track;
import org.broad.igv.ui.IGV;
import org.broad.igv.ui.util.MessageUtils;

/**
 *
 * @author mlfranchinar
 */
public class SelectTracksMenuAction extends MenuAction {

    static Logger log = Logger.getLogger(SelectTracksMenuAction.class);
    IGV igv;

    private JCheckBox selectAllCheckBox = new JCheckBox();
    private JCheckBox unselectAllCheckBox = new JCheckBox();

    private TrackSelectPane trackSelectPane;
    private List<Track> tracks;
    Frame frame;

    public SelectTracksMenuAction(String label, int mnemonic, IGV igv) {
        super(label, null, mnemonic);
        this.igv = igv;
        this.frame = igv.getMainFrame();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        doFilterTracks();
    }

    private void doFilterTracks() {

        this.tracks = igv.getAllTracks(); 

        List<String> uniqueAttributeKeys = AttributeManager.getInstance().getAttributeNames();

        // Sort the attribute keys if we have any
        if (uniqueAttributeKeys != null) {
            //Collections.sort(uniqueAttributeKeys, AttributeManager.getInstance().getAttributeComparator());
        } else // If we have no attribute we can't display the
            // track filter dialog so say so and return
            if (uniqueAttributeKeys == null || uniqueAttributeKeys.isEmpty()) {

                MessageUtils.showMessage("No attributes found to use in a filter");
                return;
            }

        if (trackSelectPane == null) {
            trackSelectPane = new TrackSelectPane(tracks);

        } 

        trackSelectPane.clearTracks();
        trackSelectPane.addTracks(tracks);

        Integer response = showSelectTrackDialog(frame, trackSelectPane, "Select Tracks");

        if (response == null) {
            return;
        }

        if (response.intValue() == JOptionPane.CANCEL_OPTION) {

            return;
        } else if (response.intValue() == JOptionPane.OK_OPTION) {

            selectTracks(trackSelectPane);
            igv.doRefresh();
        }

    }

    private Integer showSelectTrackDialog(Frame parent,
                                          final TrackSelectPane trackSelectPane,
                                          String title) {

        JScrollPane scrollPane = new JScrollPane(trackSelectPane);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        int optionType = JOptionPane.OK_CANCEL_OPTION;
        int messageType = JOptionPane.PLAIN_MESSAGE;

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        JPanel selectHeaderPanel = new JPanel();
        //filterHeaderPanel.setBackground(Color.WHITE);
        selectHeaderPanel.setLayout(new GridLayout(0, 1));
        selectHeaderPanel.add(new JLabel("Select tracks to be viewed."));

        selectAllCheckBox.setText("Select All Tracks");
        selectAllCheckBox.setSelected(false);
        selectAllCheckBox.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                if (selectAllCheckBox.isSelected()){
                    trackSelectPane.selectAll();
                    unselectAllCheckBox.setSelected(false);
                }
                
            }
        });

        unselectAllCheckBox.setText("Uncheck All Tracks");
        unselectAllCheckBox.setSelected(false);
        unselectAllCheckBox.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                if (unselectAllCheckBox.isSelected()){
                    trackSelectPane.unselectAll();
                    selectAllCheckBox.setSelected(false);
                }
                
            }
        });
        
        JPanel controls = new JPanel();
        FlowLayout layoutManager = new FlowLayout();
        layoutManager.setAlignment(FlowLayout.LEFT);
        controls.setLayout(layoutManager);
        controls.add(selectAllCheckBox);
        controls.add(unselectAllCheckBox);
       
        //controls.setBackground(Color.WHITE);
        controls.setOpaque(true);
        selectHeaderPanel.add(controls);

        panel.setOpaque(true);
        panel.add(selectHeaderPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        final JOptionPane optionPane = new JOptionPane(panel, messageType, optionType);
        optionPane.setPreferredSize(new Dimension(350, 400));
        optionPane.setOpaque(true);
        //optionPane.setBackground(Color.WHITE);
        optionPane.addPropertyChangeListener(
                JOptionPane.VALUE_PROPERTY,
                new PropertyChangeListener() {

                    public void propertyChange(PropertyChangeEvent e) {

                        Object value = e.getNewValue();
                        if (value instanceof Integer) {

                            int option = (Integer) value;
                            if (option == JOptionPane.OK_OPTION) {

                            }
                        }
                    }
                });

        JDialog dialog = optionPane.createDialog(parent, title);
        dialog.setResizable(true);
        //dialog.setBackground(Color.WHITE);
        //dialog.getContentPane().setBackground(Color.WHITE);

        /*Component[] children = optionPane.getComponents();
        if (children != null) {
            for (Component child : children) {
                //child.setBackground(Color.WHITE);
            }

        }*/

        dialog.pack();
        dialog.setVisible(true);

        Object selectedValue = optionPane.getValue();
        if (selectedValue == null) {
            return JOptionPane.CANCEL_OPTION;
        } else if (((Integer) selectedValue).intValue() == JOptionPane.OK_OPTION) {
            
        }
        return ((Integer) selectedValue);
    }

    private void selectTracks(TrackSelectPane trackSelectPane) {

        boolean showAllTracks = selectAllCheckBox.isSelected();
        boolean showNeverTracks = unselectAllCheckBox.isSelected();
        if (showAllTracks) {

            //List<Track> tracks = IGV.getInstance().getAllTracks();
            for (Track track : tracks) {
                track.setVisible(true);
            }

        } else if (showNeverTracks) {

            //List<Track> tracks = IGV.getInstance().getAllTracks();
            for (Track track : tracks) {
                track.setVisible(false);
            }
        }else {
            HashMap<SelectCheckBox,Track>selectTracks = trackSelectPane.getSelectTrack();
            
            for (Iterator<SelectCheckBox> it = selectTracks.keySet().iterator() ; it.hasNext() ; ){
                SelectCheckBox cb = it.next();
                Track track = selectTracks.get(cb);
                if (cb.isSelected()){
                    track.setVisible(true);
                }else{
                    track.setVisible(false);
                }
            }
            
        }

    }


    public void setSelectShowAllTracks(boolean value) {
        if (selectAllCheckBox != null) {
            selectAllCheckBox.setSelected(value);
        }
    }

    public JCheckBox getShowAllTracksSelectCheckBox() {
        return selectAllCheckBox;
    }


    public class SelectCheckBox extends JCheckBox{
        public SelectCheckBox(String name){
            super(name);
            addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                        if (isSelected()){
                            unselectAllCheckBox.setSelected(false);
                        }
                        else{
                            selectAllCheckBox.setSelected(false);
                        }
                }
            });
        }
    }
    
    public class TrackSelectPane extends JPanel{
        
        HashMap<SelectCheckBox,Track> selectTrack;
        
        public TrackSelectPane(List<Track> tracks){
            setOpaque(true);
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            selectTrack = new HashMap<SelectCheckBox,Track>();
            for (Track track : tracks) {
                SelectCheckBox cb = new SelectCheckBox(track.getName());
                boolean isVisible = track.isVisible();
                cb.setSelected(isVisible);
                add(cb);
                selectTrack.put(cb, track);
            }
        }
        
        public void init(List<Track> tracks){
            for (Track track : tracks) {
                SelectCheckBox cb = new SelectCheckBox(track.getName());
                boolean isVisible = track.isVisible();
                cb.setSelected(isVisible);
                add(cb);
                selectTrack.put(cb, track);
            }
        }
        
        public void clearTracks(){
            this.removeAll();
            selectTrack.clear();
        }
        
        public void addTracks(List<Track> tracks){
            clearTracks();
            init(tracks);
        }
        
        public HashMap getSelectTrack(){
            return selectTrack;
        }
        
        
        public void selectAll(){
            for(Iterator<SelectCheckBox> it = getSelectTrack().keySet().iterator();it.hasNext();){
                SelectCheckBox cb = it.next();
                cb.setSelected(true);
            }
        }
        
        public void unselectAll(){
            for(Iterator<SelectCheckBox> it = getSelectTrack().keySet().iterator();it.hasNext();){
                SelectCheckBox cb = it.next();
                cb.setSelected(false);
            }
        }
    }
}
