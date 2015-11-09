/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.broad.igv.agv;

import apple.dts.samplecode.osxadapter.OSXAdapter;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JSeparator;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.plaf.basic.BasicBorders;
import org.apache.log4j.Logger;
import org.broad.igv.DirectoryManager;
import org.broad.igv.Globals;
import org.broad.igv.PreferenceManager;
import org.broad.igv.annotations.ForTesting;
import org.broad.igv.charts.ScatterPlotUtils;
import org.broad.igv.cli_plugin.PluginSpecReader;
import org.broad.igv.cli_plugin.ui.RunPlugin;
import org.broad.igv.cli_plugin.ui.SetPluginPathDialog;
import org.broad.igv.dev.db.DBProfileEditor;
import org.broad.igv.feature.genome.GenomeManager;
import org.broad.igv.ga4gh.Ga4ghAPIHelper;
import org.broad.igv.ga4gh.OAuthUtils;
import org.broad.igv.gs.GSOpenSessionMenuAction;
import org.broad.igv.gs.GSSaveSessionMenuAction;
import org.broad.igv.gs.GSUtils;
import org.broad.igv.lists.GeneListManagerUI;
import org.broad.igv.lists.VariantListManager;
import org.broad.igv.tools.IgvToolsGui;
import org.broad.igv.track.CombinedDataSourceDialog;
import org.broad.igv.ui.AboutDialog;
import org.broad.igv.ui.IGV;
import org.broad.igv.ui.Main;
import org.broad.igv.ui.ShutdownThread;
import org.broad.igv.ui.UIConstants;
import static org.broad.igv.ui.UIConstants.ABOUT_TOOLTIP;
import static org.broad.igv.ui.UIConstants.EXIT_TOOLTIP;
import static org.broad.igv.ui.UIConstants.HELP_TOOLTIP;
import static org.broad.igv.ui.UIConstants.IMPORT_REGION_TOOLTIP;
import static org.broad.igv.ui.UIConstants.PREFERENCE_TOOLTIP;
import static org.broad.igv.ui.UIConstants.SAVE_IMAGE_TOOLTIP;
import static org.broad.igv.ui.UIConstants.SELECT_DISPLAYABLE_ATTRIBUTES_TOOLTIP;
import static org.broad.igv.ui.UIConstants.SERVER_BASE_URL;
import static org.broad.igv.ui.UIConstants.SHOW_HEATMAP_LEGEND_TOOLTIP;
import static org.broad.igv.ui.UIConstants.SORT_TRACKS_TOOLTIP;
import org.broad.igv.ui.VersionUpdateDialog;
import org.broad.igv.ui.action.BrowseEncodeAction;
import org.broad.igv.ui.action.BrowseGa4ghAction;
import org.broad.igv.ui.action.ExportRegionsMenuAction;
import org.broad.igv.ui.action.ExportTrackNamesMenuAction;
import org.broad.igv.ui.action.FilterTracksMenuAction;
import org.broad.igv.ui.action.FitDataToWindowMenuAction;
import org.broad.igv.ui.action.GroupTracksMenuAction;
import org.broad.igv.ui.action.ImportRegionsMenuAction;
import org.broad.igv.ui.action.LoadFilesMenuAction;
import org.broad.igv.ui.action.LoadFromDatabaseAction;
import org.broad.igv.ui.action.LoadFromGSMenuAction;
import org.broad.igv.ui.action.LoadFromServerAction;
import org.broad.igv.ui.action.LoadFromURLMenuAction;
import org.broad.igv.ui.action.LoadGenomeFromGSMenuAction;
import org.broad.igv.ui.action.MenuAction;
import org.broad.igv.ui.action.NavigateRegionsMenuAction;
import org.broad.igv.ui.action.OpenSessionMenuAction;
import org.broad.igv.ui.action.ResetPreferencesAction;
import org.broad.igv.ui.action.RunScriptMenuAction;
import org.broad.igv.ui.action.SelectTracksMenuAction;
import org.broad.igv.ui.action.SetTrackHeightMenuAction;
import org.broad.igv.ui.action.SortTracksMenuAction;
import org.broad.igv.ui.legend.LegendDialog;
import org.broad.igv.ui.panel.FrameManager;
import org.broad.igv.ui.panel.MainPanel;
import org.broad.igv.ui.panel.ReferenceFrame;
import org.broad.igv.ui.panel.ReorderPanelsDialog;
import org.broad.igv.ui.util.FileDialogUtils;
import org.broad.igv.ui.util.HistoryMenu;
import org.broad.igv.ui.util.MenuAndToolbarUtils;
import org.broad.igv.ui.util.MessageUtils;
import org.broad.igv.ui.util.UIUtilities;
import org.broad.igv.util.BrowserLauncher;
import org.broad.igv.util.HttpUtils;
import org.broad.igv.util.LongRunningTask;
import org.broad.igv.util.ResourceLocator;
import org.broad.igv.util.encode.EncodeFileBrowser;

/**
 * 
 * @author mlfranchinard
 */
public class AGVMenuBar extends JMenuBar {
    
    private static Logger log = Logger.getLogger(AGVMenuBar.class);
    public static final String GENOMESPACE_REG_TOOLTIP = "Register for GenomeSpace";
    public static final String GENOMESPACE_REG_PAGE = "http://www.genomespace.org/register";

    private JMenu fileMenu;
    private JMenu extrasMenu;
    private FilterTracksMenuAction filterTracksAction;
    private SelectTracksMenuAction selectTracksAction;
    private JMenu viewMenu;
    AGV agv;

    private JMenu toolsMenu;

    /**
     * We store this as a field because we alter it if
     * we can't access genome server list
     */
    private JMenuItem loadFromServerMenuItem;

    private static final String LOAD_GENOME_SERVER_TOOLTIP = "Select genomes available on the server to appear in menu.";
    private static final String CANNOT_LOAD_GENOME_SERVER_TOOLTIP = "Could not reach genome server";

    //private static AGVMenuBar instance;
    private static HashMap<Integer,AGVMenuBar> instances = new HashMap<Integer,AGVMenuBar>();
    private JMenu googleMenu;

    /*public void notifyGenomeServerReachable(boolean reachable) {
        if (loadFromServerMenuItem != null) {
            loadFromServerMenuItem.setEnabled(reachable);
            String tooltip = reachable ? LOAD_GENOME_SERVER_TOOLTIP : CANNOT_LOAD_GENOME_SERVER_TOOLTIP;
            loadFromServerMenuItem.setToolTipText(tooltip);
        }
    }*/

    public void showAboutDialog() {
        (new AboutDialog(agv.getMainFrame(), true)).setVisible(true);
    }

    static AGVMenuBar createInstance(AGV agv) {
        if (instances.containsKey(agv.getID())) {
            AGVMenuBar instance = instances.get(agv.getID());
            return instance;
        }
        else {
            AGVMenuBar instance = new AGVMenuBar(agv);
            instances.put(agv.getID(), instance);
            return instance;
        }
    }

    public static AGVMenuBar getInstance(int id) {
        if (instances.containsKey(id)){
            return instances.get(id);
        }
        return null;
    }

    private AGVMenuBar(AGV agv) {
        this.agv = agv;
        setBorder(new BasicBorders.MenuBarBorder(Color.GRAY, Color.GRAY)); 
        setBackground(agv.getColor());
        setBorderPainted(true);

        for (AbstractButton menu : createMenus()) {
            add(menu);
        }

        //This is for Macs, so showing the about dialog
        //from the command bar does what we want.
        if (Globals.IS_MAC) {
            try {
                OSXAdapter.setAboutHandler(this, getClass().getDeclaredMethod("showAboutDialog", (Class[]) null));
                OSXAdapter.setQuitHandler(ShutdownThread.class, ShutdownThread.class.getDeclaredMethod("runS", (Class[]) null));
            } catch (Exception e) {
                log.error("Error setting apple-specific about and quit handlers", e);
            }

        }
    }

    private List<AbstractButton> createMenus() {

        List<AbstractButton> menus = new ArrayList<AbstractButton>();
        createFileMenu();
        menus.add(fileMenu);
        //menus.add(createGenomesMenu());
        menus.add(createViewMenu());
        menus.add(createTracksMenu());
        menus.add(createRegionsMenu());

        refreshToolsMenu();
        menus.add(toolsMenu);

        //menus.add(createGenomeSpaceMenu());
        extrasMenu = createExtrasMenu();
        //extrasMenu.setVisible(false);
        menus.add(extrasMenu);


        googleMenu = createGoogleMenu();
        googleMenu.setVisible(PreferenceManager.getInstance().get(PreferenceManager.GOOGLE_API_KEY) != null ||
                PreferenceManager.getInstance().getAsBoolean(PreferenceManager.ENABLE_GOOGLE_MENU));
        menus.add(googleMenu);

        //menus.add(createHelpMenu());

        // Experimental -- remove for production release

        return menus;
    }

    /**
     * Generate the "tools" menu.
     * This is imperative, it is written to field {@code toolsMenu}.
     * Reason being, when we add (TODO remove)
     * a new tool, we need to refresh just this menu
     */
    void refreshToolsMenu() {
        List<JComponent> menuItems = new ArrayList<JComponent>(10);

        // batch script
        MenuAction menuAction = new RunScriptMenuAction("Run Batch Script...", KeyEvent.VK_X, agv);
        menuItems.add(MenuAndToolbarUtils.createMenuItem(menuAction));

        // igvtools
        //menuItems.add(new JSeparator());
        menuAction = new SortTracksMenuAction("Run igvtools...", KeyEvent.VK_T, agv) {
            @Override
            public void actionPerformed(ActionEvent e) {
                IgvToolsGui.launch(false, agv.getGenomeManager().getGenomeId());
            }
        };
        menuItems.add(MenuAndToolbarUtils.createMenuItem(menuAction));

        List<JComponent> otherToolMenus = agv.getOtherToolMenus();
        if (otherToolMenus.size() > 0) {
            for (JComponent entry : otherToolMenus) {
                menuItems.add(entry);
            }
        }
        //menuItems.add(new JSeparator());


        //-------------------------------------//
        //"Add tool" option, for loading cli_plugin from someplace else
        JMenuItem addTool = new JMenuItem("Add Tool...");
        addTool.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                File pluginFi = FileDialogUtils.chooseFile("Select cli_plugin .xml spec");
                if (pluginFi == null) return;

                try {
                    PluginSpecReader.addCustomPlugin(pluginFi.getAbsolutePath());
                    refreshToolsMenu();
                } catch (IOException e1) {
                    MessageUtils.showErrorMessage("Error loading custom cli_plugin", e1);
                }
            }
        });
        //menuItems.add(addTool);
        //menuItems.add(new JSeparator());

        //-------------------------------------//

        for (final PluginSpecReader pluginSpecReader : PluginSpecReader.getPlugins()) {
            for (final PluginSpecReader.Tool tool : pluginSpecReader.getTools()) {
                final String toolName = tool.name;
                boolean toolVisible = tool.visible;
                JMenuItem toolMenu;

                if (toolVisible) {

                    final String toolPath = pluginSpecReader.getToolPath(tool);
                    final String tool_url = tool.toolUrl;
                    boolean isValid = PluginSpecReader.isToolPathValid(toolPath);

                    ActionListener invalidActionListener = new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            String msg = String.format("%s executable not found at %s", toolName, toolPath);
                            if (tool_url != null) {
                                msg += "<br/>See " + tool_url + " to install";
                            }
                            MessageUtils.showMessage(msg);
                        }
                    };

                    toolMenu = new JMenu(toolName);
                    //Kind of overlaps with the side-pull menu, doesn't look great
                    //toolMenu.setToolTipText(tool.getAttribute("description"));
                    for (final PluginSpecReader.Command command : tool.commandList) {
                        final String cmdName = command.name;
                        JMenuItem cmdItem = new JMenuItem(cmdName);
                        toolMenu.add(cmdItem);
                        if (isValid || toolPath == null) {
                            cmdItem.addActionListener(new ActionListener() {
                                @Override
                                public void actionPerformed(ActionEvent e) {
                                    RunPlugin runPlugin = null;
                                    try {
                                        runPlugin = new RunPlugin(agv.getMainFrame(), pluginSpecReader, tool, command);
                                    } catch (IllegalStateException e1) {
                                        MessageUtils.showErrorMessage(e1.getMessage(), e1);
                                        return;
                                    }
                                    runPlugin.setVisible(true);
                                }
                            });
                            cmdItem.setEnabled(true);
                        } else {
                            cmdItem.setEnabled(false);
                        }
                    }
                    //Hack so we can have a tool which is just general command line stuff
                    //Don't let the user change the path in that case
                    if (tool.defaultPath != null) {
                        JMenuItem setPathItem = new JMenuItem(String.format("Set path to %s...", toolName));
                        setPathItem.addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                (new SetPluginPathDialog(agv.getMainFrame(), pluginSpecReader, tool)).setVisible(true);
                                refreshToolsMenu();
                            }
                        });
                        toolMenu.add(setPathItem);
                    }
                    menuItems.add(toolMenu);
                }
            }
        }
        //-------------------------------------//

        //-----------SQL DB Tools--------------//
        boolean showDBEditor = Globals.isDevelopment();
        if (showDBEditor) {
            JMenu sqlDBProfileEditor = new JMenu("SQL DB Profile Editor");
            JMenuItem createNewProfile = new JMenuItem("Create New Profile");
            createNewProfile.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    File file = FileDialogUtils.chooseFile("Save DB Profile", DirectoryManager.getUserDirectory(), FileDialogUtils.SAVE);
                    if (file != null) {
                        DBProfileEditor editor = new DBProfileEditor(agv.getMainFrame(), file.getAbsolutePath());
                        editor.setVisible(true);
                    }
                }
            });
            JMenuItem editExistingProfile = new JMenuItem("Edit Existing Profile");
            editExistingProfile.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    File file = FileDialogUtils.chooseFile("Select .dbxml database profile");
                    if (file != null) {
                        if (!file.exists()) {

                        }
                        DBProfileEditor editor = new DBProfileEditor(agv.getMainFrame(), file.getAbsolutePath());
                        editor.setVisible(true);
                    }
                }
            });
            sqlDBProfileEditor.add(createNewProfile);
            sqlDBProfileEditor.add(editExistingProfile);
            menuItems.add(sqlDBProfileEditor);
        }


        //-------------------------------------//


        //DataTrack Math------------------------//

        JMenuItem combineDataItem = new JMenuItem("Combine Data Tracks");
        combineDataItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                CombinedDataSourceDialog dialog = new CombinedDataSourceDialog(agv.getMainFrame());
                dialog.setVisible(true);
            }
        });
        menuItems.add(combineDataItem);


        //-------------------------------------//


        MenuAction toolsMenuAction = new MenuAction("Tools", null);
        if (toolsMenu == null) {
            toolsMenu = MenuAndToolbarUtils.createMenu(menuItems, toolsMenuAction);
            toolsMenu.setName("Tools");
        } else {
            toolsMenu.removeAll();
            for (JComponent item : menuItems) {
                toolsMenu.add(item);
            }
        }

    }

    public void enableExtrasMenu() {
        extrasMenu.setVisible(true);
    }


    void createFileMenu() {

        List<JComponent> menuItems = new ArrayList<JComponent>();
        MenuAction menuAction = null;
        //We disable certain load items when there is no genome.
        boolean genomeLoaded = agv.getGenomeManager().getCurrentGenome() != null;

        menuItems.add(new JSeparator());

        // Load menu items
        menuAction = new LoadFilesMenuAction("Load from File...", KeyEvent.VK_L, agv);
        menuAction.setToolTipText(UIConstants.LOAD_TRACKS_TOOLTIP);
        menuItems.add(MenuAndToolbarUtils.createMenuItem(menuAction));

        menuAction = new LoadFromURLMenuAction(LoadFromURLMenuAction.LOAD_FROM_URL, KeyEvent.VK_U, agv);
        menuAction.setToolTipText(UIConstants.LOAD_TRACKS_TOOLTIP);
        menuItems.add(MenuAndToolbarUtils.createMenuItem(menuAction));

        menuAction = new LoadFromServerAction("Load from Server...", KeyEvent.VK_S, agv);
        menuAction.setToolTipText(UIConstants.LOAD_SERVER_DATA_TOOLTIP);
        menuItems.add(MenuAndToolbarUtils.createMenuItem(menuAction));

        menuAction = new LoadFromURLMenuAction(LoadFromURLMenuAction.LOAD_FROM_DAS, KeyEvent.VK_D, agv);
        menuItems.add(MenuAndToolbarUtils.createMenuItem(menuAction));

        if (PreferenceManager.getInstance().getAsBoolean(PreferenceManager.DB_ENABLED)) {
            menuAction = new LoadFromDatabaseAction("Load from Database...", 0, agv);
            menuItems.add(MenuAndToolbarUtils.createMenuItem(menuAction));
        }

        String genomeId = agv.getGenomeManager().getGenomeId();
        if (EncodeFileBrowser.genomeSupported(genomeId)) {
            menuAction = new BrowseEncodeAction("Load from ENCODE...", KeyEvent.VK_E, agv);
            menuItems.add(MenuAndToolbarUtils.createMenuItem(menuAction));
        }

        if (PreferenceManager.getInstance().get(PreferenceManager.GOOGLE_API_KEY) != null ||
                PreferenceManager.getInstance().getAsBoolean(PreferenceManager.ENABLE_GOOGLE_MENU)) {
            menuAction = new BrowseGa4ghAction("Load from Ga4gh...", KeyEvent.VK_G, agv);
            menuItems.add(MenuAndToolbarUtils.createMenuItem(menuAction));
        }

        //Disable loading if no genome loaded. Something of an edge case
        if (!genomeLoaded) {
            for (JComponent menuItem : menuItems) {
                menuItem.setEnabled(false);
            }
        }

        menuItems.add(new JSeparator());

        // Session menu items
        /*menuAction = new NewSessionMenuAction("New Session...", KeyEvent.VK_N, agv);
        menuAction.setToolTipText(UIConstants.NEW_SESSION_TOOLTIP);
        menuItems.add(MenuAndToolbarUtils.createMenuItem(menuAction));

        menuAction = new OpenSessionMenuAction("Open Session...", KeyEvent.VK_O, agv);
        menuAction.setToolTipText(UIConstants.RESTORE_SESSION_TOOLTIP);
        menuItems.add(MenuAndToolbarUtils.createMenuItem(menuAction));

        menuAction = new SaveSessionMenuAction("Save Session...", KeyEvent.VK_V, agv);
        menuAction.setToolTipText(UIConstants.SAVE_SESSION_TOOLTIP);
        JMenuItem saveSessionItem = MenuAndToolbarUtils.createMenuItem(menuAction);
        menuItems.add(saveSessionItem);
        saveSessionItem.setEnabled(genomeLoaded);

        menuItems.add(new JSeparator());*/

        // ***** Snapshots
        // Snapshot Application
        menuAction =
                new MenuAction("Save Image ...", null, KeyEvent.VK_A) {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        agv.saveImage(agv.getMainPanel());

                    }
                };

        menuAction.setToolTipText(SAVE_IMAGE_TOOLTIP);
        menuItems.add(MenuAndToolbarUtils.createMenuItem(menuAction));

        // TODO -- change "Exit" to "Close" for BioClipse
        menuItems.add(new JSeparator());      // Exit
        menuAction =
                new MenuAction("Exit", null, KeyEvent.VK_X) {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        doExitApplication();
                    }
                };

        menuAction.setToolTipText(EXIT_TOOLTIP);
        menuItems.add(MenuAndToolbarUtils.createMenuItem(menuAction));


        // Empty the recent sessions list before we start to do
        // anything with it
        agv.getRecentSessionList().clear();

        // Retrieve the stored session paths
        String recentSessions = PreferenceManager.getInstance().getRecentSessions();
        if (recentSessions != null) {
            String[] sessions = recentSessions.split(";");
            for (String sessionPath : sessions) {
                if (!agv.getRecentSessionList().contains(sessionPath)) {
                    agv.getRecentSessionList().add(sessionPath);
                }

            }
        }

        if (!agv.getRecentSessionList().isEmpty()) {

            menuItems.add(new JSeparator());

            // Now add menu items
            for (final String session : agv.getRecentSessionList()) {
                OpenSessionMenuAction osMenuAction = new OpenSessionMenuAction(session, session, agv);
                menuItems.add(MenuAndToolbarUtils.createMenuItem(osMenuAction));
            }

        }

        MenuAction fileMenuAction = new MenuAction("File", null, KeyEvent.VK_F);
        if (fileMenu == null) {
            fileMenu = MenuAndToolbarUtils.createMenu(menuItems, fileMenuAction);
        } else {
            fileMenu.removeAll();
            for (JComponent item : menuItems) {
                fileMenu.add(item);
            }
        }
    }

    /*private void notifyGenomesAddedRemoved(List<GenomeListItem> selectedValues, boolean added) {
        if (selectedValues == null || selectedValues.size() == 0) return;
        int size = selectedValues.size();
        String msg = "";
        if (size == 1) {
            msg += selectedValues.get(0) + " genome";
        } else {
            msg += size + " genomes";
        }
        if (added) {
            msg += " added to";
        } else {
            msg += " removed from";
        }
        msg += " list";

        MessageUtils.setStatusBarMessage(msg);
    }*/

    /*private JMenu createGenomesMenu() {
        List<JComponent> menuItems = new ArrayList<JComponent>();
        MenuAction menuAction = null;

        // Load genome
        menuAction =
                new MenuAction("Load Genome from File...", null, KeyEvent.VK_I) {
                    @Override
                    public void actionPerformed(ActionEvent event) {
                        try {
                            org.broad.agv.ui.util.ProgressMonitor monitor = new org.broad.agv.ui.util.ProgressMonitor();
                            agv.doLoadGenome(monitor);
                        } catch (Exception e) {
                            MessageUtils.showErrorMessage(e.getMessage(), e);
                        }
                    }
                };

        menuAction.setToolTipText("Load a FASTA or .genome file...");
        menuItems.add(MenuAndToolbarUtils.createMenuItem(menuAction));

        // Load genome from URL
        menuAction = new LoadFromURLMenuAction(LoadFromURLMenuAction.LOAD_GENOME_FROM_URL, 0, agv);
        menuAction.setToolTipText("Load a FASTA or .genome file...");
        menuItems.add(MenuAndToolbarUtils.createMenuItem(menuAction));

        // Add genome to combo box from server
        menuAction = new MenuAction("Load Genome From Server...", null) {
            @Override
            public void actionPerformed(ActionEvent event) {
                agv.loadGenomeFromServerAction();
            }
        };
        menuAction.setToolTipText(LOAD_GENOME_SERVER_TOOLTIP);
        loadFromServerMenuItem = MenuAndToolbarUtils.createMenuItem(menuAction);
        menuItems.add(loadFromServerMenuItem);

        menuItems.add(new JSeparator());

        menuAction =
                new MenuAction("Create .genome File...", null, KeyEvent.VK_D) {
                    @Override
                    public void actionPerformed(ActionEvent event) {
                        org.broad.agv.ui.util.ProgressMonitor monitor = new org.broad.agv.ui.util.ProgressMonitor();
                        agv.doDefineGenome(monitor);
                    }
                };

        menuAction.setToolTipText(UIConstants.IMPORT_GENOME_TOOLTIP);
        menuItems.add(MenuAndToolbarUtils.createMenuItem(menuAction));

        menuItems.add(new JSeparator());

        // Add genome to combo box from server
        menuAction = new MenuAction("Manage Genome List...", null) {
            @Override
            public void actionPerformed(ActionEvent event) {
                ManageGenomesDialog dialog2 = new ManageGenomesDialog(agv.getMainFrame());
                dialog2.setVisible(true);
                boolean cancelled = dialog2.isCancelled();
                List<GenomeListItem> removedValuesList = dialog2.getRemovedValuesList();
                if (!cancelled) {
                    agv.getGenomeManager().buildGenomeItemList();
                    agv.getContentPane().getCommandBar().refreshGenomeListComboBox();
                    if (removedValuesList != null && !removedValuesList.isEmpty()) {
                        try {
                            agv.getGenomeManager().deleteDownloadedGenomes(removedValuesList);
                        } catch (IOException e) {
                            MessageUtils.showErrorMessage("Error deleting genome files", e);
                        }
                        agv.getGenomeManager().updateImportedGenomePropertyFile();
                        notifyGenomesAddedRemoved(removedValuesList, false);
                    }
                }
            }
        };
        menuAction.setToolTipText("Add, remove, or reorder genomes which appear in the dropdown list");
        menuItems.add(MenuAndToolbarUtils.createMenuItem(menuAction));

        MenuAction genomeMenuAction = new MenuAction("Genomes", null);
        return MenuAndToolbarUtils.createMenu(menuItems, genomeMenuAction);
    }*/


    private JMenu createTracksMenu() {

        List<JComponent> menuItems = new ArrayList<JComponent>();
        MenuAction menuAction = null;

        // Sort Context
        menuAction = new SortTracksMenuAction("Sort Tracks...", KeyEvent.VK_S, agv);
        menuAction.setToolTipText(SORT_TRACKS_TOOLTIP);
        menuItems.add(MenuAndToolbarUtils.createMenuItem(menuAction));

        menuAction = new GroupTracksMenuAction("Group Tracks... ", KeyEvent.VK_G, agv);
        menuAction.setToolTipText(UIConstants.GROUP_TRACKS_TOOLTIP);
        menuItems.add(MenuAndToolbarUtils.createMenuItem(menuAction));

        //mlfranchinard, Aout 2015
        //Com redmine:SelectTracksAction(AGV)
        
        // Manage Tracks
        JMenu filterTracksMenu = new JMenu("Manage Tracks");
        JMenuItem items;
        
        //Filter Tracks
        filterTracksAction = new FilterTracksMenuAction("Filter Tracks...", KeyEvent.VK_F,  agv);
        filterTracksAction.setToolTipText(UIConstants.FILTER_TRACKS_TOOLTIP);
        items = MenuAndToolbarUtils.createMenuItem(filterTracksAction);
        filterTracksMenu.add(items);
        
        //Select Tracks
        selectTracksAction = new SelectTracksMenuAction("Select Tracks...", KeyEvent.VK_S, agv);
        selectTracksAction.setToolTipText(UIConstants.SELECT_TRACKS_TOOLTIP);
        items = MenuAndToolbarUtils.createMenuItem(selectTracksAction);
        filterTracksMenu.add(items);
        
        menuItems.add(filterTracksMenu);
        menuItems.add(new JSeparator());

        // Reset Tracks
        menuAction = new FitDataToWindowMenuAction("Fit Data to Window", KeyEvent.VK_W, agv);
        menuAction.setToolTipText(UIConstants.FIT_DATA_TO_WINDOW_TOOLTIP);
        menuItems.add(MenuAndToolbarUtils.createMenuItem(menuAction));


        // Set track height
        menuAction = new SetTrackHeightMenuAction("Set Track Height...", KeyEvent.VK_H, agv);
        menuAction.setToolTipText(UIConstants.SET_DEFAULT_TRACK_HEIGHT_TOOLTIP);
        menuItems.add(MenuAndToolbarUtils.createMenuItem(menuAction));


        MenuAction dataMenuAction = new MenuAction("Tracks", null, KeyEvent.VK_K);

        //menuItems.add(exportData);

        return MenuAndToolbarUtils.createMenu(menuItems, dataMenuAction);
    }


    private JMenu createViewMenu() {

        List<JComponent> menuItems = new ArrayList<JComponent>();
        MenuAction menuAction = null;

        // Preferences
        menuAction =
                new MenuAction("Preferences...", null, KeyEvent.VK_P) {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        UIUtilities.invokeOnEventThread(new Runnable() {
                            public void run() {
                                agv.doViewPreferences();
                            }
                        });
                    }
                };
        menuAction.setToolTipText(PREFERENCE_TOOLTIP);
        menuItems.add(MenuAndToolbarUtils.createMenuItem(menuAction));

        menuAction =
                new MenuAction("Color Legends ...", null, KeyEvent.VK_H) {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        (new LegendDialog(agv.getMainFrame())).setVisible(true);
                    }
                };
        menuAction.setToolTipText(SHOW_HEATMAP_LEGEND_TOOLTIP);
        menuItems.add(MenuAndToolbarUtils.createMenuItem(menuAction));

        menuItems.add(new JSeparator());

        menuAction = new MenuAction("Show Name Panel", null, KeyEvent.VK_A) {
            @Override
            public void actionPerformed(ActionEvent e) {

                JCheckBoxMenuItem menuItem = (JCheckBoxMenuItem) e.getSource();
                if (menuItem.isSelected()) {
                    agv.getMainPanel().expandNamePanel();
                } else {
                    agv.getMainPanel().collapseNamePanel();
                }
                agv.doRefresh();
            }
        };
        boolean isShowing = agv.getMainPanel().isExpanded();
        JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem();
        menuItem.setSelected(isShowing);
        menuItem.setAction(menuAction);
        menuItems.add(menuItem);

        JMenuItem panelWidthmenuItem = new JMenuItem();
        menuAction = new MenuAction("Set Name Panel Width...", null, KeyEvent.VK_A) {
            @Override
            public void actionPerformed(ActionEvent e) {
                MainPanel mainPanel = agv.getMainPanel();
                String currentValue = String.valueOf(mainPanel.getNamePanelWidth());
                String newValue = MessageUtils.showInputDialog("Enter track name panel width: ", currentValue);
                if (newValue != null) {
                    try {
                        Integer w = Integer.parseInt(newValue);
                        if (w <= 0 || w == 1000) throw new NumberFormatException();
                        PreferenceManager.getInstance().put(PreferenceManager.NAME_PANEL_WIDTH, newValue);
                        mainPanel.setNamePanelWidth(w);
                    } catch (NumberFormatException ex) {
                        MessageUtils.showErrorMessage("Error: value must be a positive integer < 1000.", ex);
                    }
                }
            }
        };
        panelWidthmenuItem.setAction(menuAction);
        menuItems.add(panelWidthmenuItem);

        // Hide or Show the attribute panels
        boolean isShow = PreferenceManager.getInstance().getAsBoolean(PreferenceManager.SHOW_ATTRIBUTE_VIEWS_KEY);
        agv.doShowAttributeDisplay(isShow);  // <= WEIRD doing agv here!

        menuAction = new MenuAction("Show Attribute Display", null, KeyEvent.VK_A) {
            @Override
            public void actionPerformed(ActionEvent e) {

                JCheckBoxMenuItem menuItem = (JCheckBoxMenuItem) e.getSource();
                PreferenceManager.getInstance().setShowAttributeView(menuItem.getState());
                agv.getMainPanel().invalidate();
                agv.doRefresh();
            }
        };
        menuItem = MenuAndToolbarUtils.createMenuItem(menuAction, isShow);
        menuItems.add(menuItem);


        menuAction =
                new MenuAction("Select Attributes to Show...", null, KeyEvent.VK_S) {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        agv.doSelectDisplayableAttribute();
                    }
                };
        menuAction.setToolTipText(SELECT_DISPLAYABLE_ATTRIBUTES_TOOLTIP);
        menuItems.add(MenuAndToolbarUtils.createMenuItem(menuAction));

        menuAction = new MenuAction("Show Header Panel", null, KeyEvent.VK_A) {
            @Override
            public void actionPerformed(ActionEvent e) {

                JCheckBoxMenuItem menuItem = (JCheckBoxMenuItem) e.getSource();
                if (menuItem.isSelected()) {
                    agv.getMainPanel().restoreHeader();
                } else {
                    agv.getMainPanel().removeHeader();
                }
                agv.doRefresh();
            }
        };
        menuItems.add(MenuAndToolbarUtils.createMenuItem(menuAction, true));

        menuItems.add(new JSeparator());
        menuAction =
                new MenuAction("Reorder Panels...", null, KeyEvent.VK_S) {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        ReorderPanelsDialog dlg = new ReorderPanelsDialog(agv.getMainFrame());
                        dlg.setVisible(true);
                    }
                };
        menuItems.add(MenuAndToolbarUtils.createMenuItem(menuAction));

        menuItems.add(new JSeparator());
        menuItems.add(new HistoryMenu("Go to"));


        // Add to IGVPanel menu
        MenuAction dataMenuAction = new MenuAction("View", null, KeyEvent.VK_V);
        viewMenu = MenuAndToolbarUtils.createMenu(menuItems, dataMenuAction);
        return viewMenu;
    }

    private JMenu createRegionsMenu() {

        List<JComponent> menuItems = new ArrayList<JComponent>();
        MenuAction menuAction = null;


        menuAction = new NavigateRegionsMenuAction("Region Navigator ...", agv);
        menuAction.setToolTipText(UIConstants.REGION_NAVIGATOR_TOOLTIP);
        menuItems.add(MenuAndToolbarUtils.createMenuItem(menuAction));

        menuAction =
                new MenuAction("Gene Lists...", null, KeyEvent.VK_S) {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        (GeneListManagerUI.getInstance(agv.getMainFrame())).setVisible(true);
                    }
                };
        menuAction.setToolTipText("Open gene list manager");
        menuItems.add(MenuAndToolbarUtils.createMenuItem(menuAction));

        menuItems.add(new JSeparator());

        // Export Regions
        menuAction = new ExportRegionsMenuAction("Export Regions ...", KeyEvent.VK_E, agv);
        menuAction.setToolTipText(UIConstants.EXPORT_REGION_TOOLTIP);
        menuItems.add(MenuAndToolbarUtils.createMenuItem(menuAction));


        // Import Regions
        menuAction = new ImportRegionsMenuAction("Import Regions ...", KeyEvent.VK_I, agv);
        menuAction.setToolTipText(IMPORT_REGION_TOOLTIP);
        menuItems.add(MenuAndToolbarUtils.createMenuItem(menuAction));

        // Import Regions
//         menuAction = new ClearRegionsMenuAction("Clear Regions ...", agv);
//         menuAction.setToolTipText(IMPORT_REGION_TOOLTIP);
//         menuItems.add(MenuAndToolbarUtils.createMenuItem(menuAction));


        MenuAction dataMenuAction = new MenuAction("Regions", null, KeyEvent.VK_V);
        viewMenu = MenuAndToolbarUtils.createMenu(menuItems, dataMenuAction);
        return viewMenu;
    }

    /*private JMenu createHelpMenu() {

        List<JComponent> menuItems = new ArrayList<JComponent>();

        MenuAction menuAction = null;

        menuAction =
                new MenuAction("User Guide ... ") {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        try {
                            BrowserLauncher.openURL(SERVER_BASE_URL + "agv/UserGuide");
                        } catch (IOException ex) {
                            log.error("Error opening browser", ex);
                        }

                    }
                };
        menuAction.setToolTipText(HELP_TOOLTIP);
        menuItems.add(MenuAndToolbarUtils.createMenuItem(menuAction));


        if (Desktop.isDesktopSupported()) {
            final Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.MAIL)) {

                menuAction =
                        new MenuAction("Help Forum...") {

                            @Override
                            public void actionPerformed(ActionEvent e) {
                                try {
                                    URI uri = new URI("http://groups.google.com/forum/#!forum/agv-help");
                                    Desktop.getDesktop().browse(uri);
                                } catch (Exception ex) {
                                    log.error("Error opening agv-help uri", ex);
                                }

                            }
                        };
                menuAction.setToolTipText("Email support");
                menuItems.add(MenuAndToolbarUtils.createMenuItem(menuAction));
            }
        }

        menuAction =
                new MenuAction("Check for Updates...") {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        checkVersion();
                    }
                };
        menuItems.add(MenuAndToolbarUtils.createMenuItem(menuAction));

        menuAction =
                new MenuAction("About IGV ") {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        (new AboutDialog(agv.getMainFrame(), true)).setVisible(true);
                    }
                };
        menuAction.setToolTipText(ABOUT_TOOLTIP);
        menuItems.add(MenuAndToolbarUtils.createMenuItem(menuAction));

        MenuAction helpMenuAction = new MenuAction("Help");


        return MenuAndToolbarUtils.createMenu(menuItems, helpMenuAction);
    }

    private void checkVersion() {

        int readTimeout = Globals.READ_TIMEOUT;
        int connectTimeout = Globals.CONNECT_TIMEOUT;

        try {
            Main.Version thisVersion = Main.Version.getVersion(Globals.VERSION);
            if (thisVersion == null) return;  // Can't compare

            Globals.CONNECT_TIMEOUT = 5000;
            Globals.READ_TIMEOUT = 1000;
            final String serverVersionString = HttpUtils.getInstance().getContentsAsString(new URL(Globals.getVersionURL())).trim();
            // See if user has specified to skip this update

            final String skipString = PreferenceManager.getInstance().get(PreferenceManager.SKIP_VERSION);
            HashSet<String> skipVersion = new HashSet<String>(Arrays.asList(skipString.split(",")));
            if (skipVersion.contains(serverVersionString)) return;

            Main.Version serverVersion = Main.Version.getVersion(serverVersionString.trim());
            if (serverVersion == null) return;

            if (thisVersion.lessThan(serverVersion)) {

                log.info("A later version of IGV is available (" + serverVersionString + ")");
                final VersionUpdateDialog dlg = new VersionUpdateDialog(serverVersionString);

                dlg.setVisible(true);
                if (dlg.isSkipVersion()) {
                    String newSkipString = skipString + "," + serverVersionString;
                    PreferenceManager.getInstance().put(PreferenceManager.SKIP_VERSION, newSkipString);
                }

            } else {
                MessageUtils.showMessage("IGV is up to date");
            }

        } catch (Exception e) {
            log.error("Error checking version", e);
        } finally {
            Globals.CONNECT_TIMEOUT = connectTimeout;
            Globals.READ_TIMEOUT = readTimeout;
        }
    }*/

    /*private JMenu createGenomeSpaceMenu() {

        JMenu menu = new JMenu("GenomeSpace");

        MenuAction menuAction = null;
        menuAction = new LoadFromGSMenuAction("Load File from GenomeSpace...", KeyEvent.VK_U, agv);
        menu.add(MenuAndToolbarUtils.createMenuItem(menuAction));

        menu.addSeparator();
        menuAction = new LoadGenomeFromGSMenuAction("Load Genome from GenomeSpace...", KeyEvent.VK_Z, agv);
        menu.add(MenuAndToolbarUtils.createMenuItem(menuAction));

        menu.addSeparator();

        menuAction = new GSSaveSessionMenuAction("Save Session to GenomeSpace...", agv);
        menu.add(MenuAndToolbarUtils.createMenuItem(menuAction));

        menuAction = new GSOpenSessionMenuAction("Load Session from GenomeSpace...", agv);
        menu.add(MenuAndToolbarUtils.createMenuItem(menuAction));

        menu.add(new JSeparator());
        menuAction = new MenuAction("Logout") {
            @Override
            public void actionPerformed(ActionEvent e) {
                GSUtils.logout();
                if (MessageUtils.confirm("You must shutdown IGV to complete the GenomeSpace logout. Shutdown now?")) {
                    doExitApplication();
                }
            }
        };
        menu.add(MenuAndToolbarUtils.createMenuItem(menuAction));

        menu.add(new JSeparator());
        menuAction =
                new MenuAction("Register... ") {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        try {
                            BrowserLauncher.openURL(GENOMESPACE_REG_PAGE);
                        } catch (IOException ex) {
                            log.error("Error opening browser", ex);
                        }

                    }
                };
        menuAction.setToolTipText(GENOMESPACE_REG_TOOLTIP);
        menu.add(MenuAndToolbarUtils.createMenuItem(menuAction));


        menu.setVisible(PreferenceManager.getInstance().getAsBoolean(PreferenceManager.GENOME_SPACE_ENABLE));


        return menu;
    }*/

    private JMenu createExtrasMenu() {

        List<JComponent> menuItems = new ArrayList<JComponent>();

        MenuAction menuAction = null;

        final JCheckBoxMenuItem exomeModeItem = new JCheckBoxMenuItem("Exome mode");
        exomeModeItem.setSelected(FrameManager.isExomeMode(agv.getID()));
        exomeModeItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                FrameManager.setExomeMode(exomeModeItem.isSelected(), true, agv.getID());
                agv.resetFrames();
            }
        });
        menuItems.add(exomeModeItem);
        menuItems.add(new JSeparator());


        // Preferences reset
        menuAction = new ResetPreferencesAction("Reset Preferences", agv);
        menuItems.add(MenuAndToolbarUtils.createMenuItem(menuAction));

        menuItems.add(new JSeparator());

        menuAction = new MenuAction("Variant list ...  *EXPERIMENTAL*") {
            @Override
            public void actionPerformed(ActionEvent e) {
                VariantListManager.openNavigator(agv.getMainFrame());
            }
        };
        menuItems.add(MenuAndToolbarUtils.createMenuItem(menuAction));

        menuItems.add(new JSeparator());


        // Set frame dimensions
        menuAction =
                new MenuAction("Set window dimensions", null, KeyEvent.VK_C) {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        String value = JOptionPane.showInputDialog("Enter dimensions, e.g. 800x400");
                        if (value != null) {
                            String[] vals = value.split("x");
                            if (vals.length == 2) {
                                int w = Integer.parseInt(vals[0]);
                                int h = Integer.parseInt(vals[1]);
                                agv.getMainFrame().setSize(w, h);
                            }
                        }
                    }
                };
        menuItems.add(MenuAndToolbarUtils.createMenuItem(menuAction));

        // Save entire window
        menuAction =
                new MenuAction("Save Screenshot ...", null, KeyEvent.VK_A) {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        agv.saveImage(agv.getContentPane());

                    }
                };

        menuItems.add(MenuAndToolbarUtils.createMenuItem(menuAction));


        menuAction = new ExportTrackNamesMenuAction("Export track names...", agv);
        menuItems.add(MenuAndToolbarUtils.createMenuItem(menuAction));


        menuAction = new MenuAction("Scatter Plot ...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                final ReferenceFrame defaultFrame = FrameManager.getDefaultFrame(agv.getID());
                String chr = defaultFrame.getChrName();
                int start = (int) defaultFrame.getOrigin();
                int end = (int) defaultFrame.getEnd();
                int zoom = defaultFrame.getZoom();
                ScatterPlotUtils.openPlot(chr, start, end, zoom);
            }
        };
        menuItems.add(MenuAndToolbarUtils.createMenuItem(menuAction));

        MenuAction extrasMenuAction = new MenuAction("Extras");
        JMenu menu = MenuAndToolbarUtils.createMenu(menuItems, extrasMenuAction);


        //
        JMenu lfMenu = new JMenu("L&F");
        LookAndFeel lf = UIManager.getLookAndFeel();
        for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {

            final String lfName = info.getName();
            JMenuItem cb = new JMenuItem(lfName);
            //cb.setSelected(info.getClassName().equals(lf.getClass().getName());
            cb.addActionListener(new AbstractAction() {

                public void actionPerformed(ActionEvent actionEvent) {
                    for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {

                        if (lfName.equals(info.getName())) {
                            try {
                                UIManager.setLookAndFeel(info.getClassName());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            break;
                        }
                    }
                }
            });
            lfMenu.add(cb);
        }
        menu.add(lfMenu);

        menu.setVisible(false);


        return menu;
    }

    private JMenu createGoogleMenu() {

        final JMenu menu = new JMenu("Google");

        final JMenuItem login = new JMenuItem("Login ... ");
        login.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    OAuthUtils.getInstance().openAuthorizationPage();
                } catch (Exception ex) {
                    MessageUtils.showErrorMessage("Error fetching oAuth tokens.  See log for details", ex);
                    log.error("Error fetching oAuth tokens", ex);
                }

            }
        });
        login.setEnabled(false);
        menu.add(login);


        final JMenuItem logout = new JMenuItem("Logout ");
        logout.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                OAuthUtils.getInstance().logout();
            }
        });
        logout.setEnabled(false);
        menu.add(logout);

        final JMenuItem loadReadset = new JMenuItem("Load Genomics ReadGroupSet... ");
        loadReadset.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String readsetId = MessageUtils.showInputDialog("Enter ReadGroupSet ID (e.g. CMvnhpKTFhCjz9_25e_lCw): ");
                if (readsetId != null) {
                    ResourceLocator locator = new ResourceLocator(readsetId);
                    locator.setName(readsetId);
                    locator.setType(Ga4ghAPIHelper.RESOURCE_TYPE);
                    locator.setAttribute("provider", Ga4ghAPIHelper.GA4GH_GOOGLE_PROVIDER);
                    agv.loadTracks(Arrays.asList(locator));
                }
            }
        });
        loadReadset.setEnabled(false);
        menu.add(loadReadset);

        menu.addMenuListener(new MenuListener() {
            @Override
            public void menuSelected(MenuEvent e) {
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        boolean loggedIn = OAuthUtils.getInstance().isLoggedIn();
                        if (loggedIn) {
                            login.setText(OAuthUtils.getInstance().getCurrentUserName());
                        } else {
                            login.setText("Login ...");
                        }
                        login.setEnabled(!loggedIn);
                        logout.setEnabled(loggedIn);
                        loadReadset.setEnabled(loggedIn);
                    }
                };
                LongRunningTask.submit(runnable);
            }

            @Override
            public void menuDeselected(MenuEvent e) {

            }

            @Override
            public void menuCanceled(MenuEvent e) {

            }
        });


        return menu;
    }

//    public void enableRemoveGenomes() {
//        if (removeImportedGenomeAction != null) {
//            removeImportedGenomeAction.setEnabled(true);
//        }
//    }

    public void resetSessionActions() {
        if (filterTracksAction != null) {
            filterTracksAction.resetTrackFilter();
        }
    }


    public void setFilterMatchAll(boolean value) {
        if (filterTracksAction != null) {
            filterTracksAction.setFilterMatchAll(value);
        }

    }

    public boolean isFilterMatchAll() {
        if (filterTracksAction != null) {
            return filterTracksAction.isFilterMatchAll();
        }

        return false;
    }

    public void setFilterShowAllTracks(boolean value) {
        if (filterTracksAction != null) {
            filterTracksAction.setFilterShowAllTracks(value);
        }

    }

    public boolean isFilterShowAllTracks() {
        if (filterTracksAction != null) {
            return filterTracksAction.getShowAllTracksFilterCheckBox().isSelected();
        }

        return false;
    }

    public JMenu getViewMenu() {
        return viewMenu;
    }

    final public void doExitApplication() {

        try {
            agv.saveStateForExit();

            Frame mainFrame = agv.getMainFrame();
            // Hide and close the application
            mainFrame.setVisible(false);
            mainFrame.dispose();

        } finally {
            System.exit(0);
        }

    }

    @ForTesting
    static void destroyInstance(int id) {
        instances.remove(id);
    }

    public void enableGoogleMenu(boolean aBoolean) {
        googleMenu.setVisible(aBoolean);
    }
    
}
