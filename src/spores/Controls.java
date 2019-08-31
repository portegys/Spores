/*
 * This software is provided under the terms of the GNU General
 * Public License as published by the Free Software Foundation.
 * Copyright (c) 2003 by Tom Portegys, All Rights Reserved.
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.
 */
package spores;

import java.awt.*;
import java.awt.event.*;

import java.io.*;

import java.util.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import javax.swing.text.*;


/**
 * Spores Controls.
 */
public class Controls extends JFrame implements Parameters {
   JPanel      contentPane;
   JTabbedPane tabbedPane = new JTabbedPane();

   // Private files.
   JPanel            privateFilesTab      = new JPanel();
   JScrollPane       privateFilesScroll   = new JScrollPane();
   PrivateTableModel privateTableModel    = new PrivateTableModel();
   JTable            privateFilesTable    = new JTable(privateTableModel);
   JButton           privateFilesAdd      = new JButton();
   JButton           privateFilesDelete   = new JButton();
   JButton           privateFilesRefresh  = new JButton();
   JButton           privateFilesFolder   = new JButton();
   JFileChooser      privateFolderChooser = new JFileChooser();

   // Private folder.
   String privateFolder = DEFAULT_PRIVATE_FOLDER;

   // Shared files.
   JPanel           sharedFilesTab      = new JPanel();
   JScrollPane      sharedFilesScroll   = new JScrollPane();
   SharedTableModel sharedTableModel    = new SharedTableModel();
   JTable           sharedFilesTable    = new JTable(sharedTableModel);
   JButton          sharedFilesAdd      = new JButton();
   JButton          sharedFilesDelete   = new JButton();
   JButton          sharedFilesRefresh  = new JButton();
   JButton          sharedFilesFolder   = new JButton();
   JFileChooser     sharedFolderChooser = new JFileChooser();

   // Shared folder.
   String sharedFolder = DEFAULT_SHARED_FOLDER;

   // Transfer.
   JPanel        transferTab             = new JPanel();
   JProgressBar  transferProgressBar     = new JProgressBar();
   JLabel        transferProgressLabel   = new JLabel();
   JComboBox     transferOperationChoice = new JComboBox();
   JLabel        transferOperationLabel  = new JLabel();
   JToggleButton transferOperationSwitch = new JToggleButton();
   JLabel        transferCopiesLabel     = new JLabel();
   JTextField    transferCopiesText      = new JTextField();
   JLabel        transferResultsLabel    = new JLabel();
   JScrollPane   transferResultsScroll   = new JScrollPane();
   JTextArea     transferResultsText     = new JTextArea();

   // Properties.
   JPanel       propertiesTab                  = new JPanel();
   JLabel       propertiesFilesLabel           = new JLabel();
   JTextField   propertiesFilesText            = new JTextField();
   JLabel       propertiesKbytesLabel          = new JLabel();
   JTextField   propertiesKbytesText           = new JTextField();
   JLabel       propertiesRestrictionsLabel    = new JLabel();
   JLabel       propertiesStatisticsLabel      = new JLabel();
   JLabel       propertiesMaxFilesLabel        = new JLabel();
   JTextField   propertiesMaxFilesText         = new JTextField();
   JLabel       propertiesMaxKbytesLabel       = new JLabel();
   JTextField   propertiesMaxKbytesText        = new JTextField();
   JLabel       propertiesExtensionLabel       = new JLabel();
   JTextField   propertiesExtensionText        = new JTextField();
   ButtonGroup  propertiesFileOrderButtonGroup = new ButtonGroup();
   JRadioButton propertiesFileByNameButton     = new JRadioButton();
   JLabel       propertiesFileDisplayLabel     = new JLabel();
   JRadioButton propertiesFileByModifiedButton = new JRadioButton();
   JRadioButton propertiesFileBySizeButton     = new JRadioButton();

   // Connections.
   JPanel      connectionsTab     = new JPanel();
   JLabel      peersLabel         = new JLabel();
   JScrollPane peersScroll        = new JScrollPane();
   JTextArea   peerText           = new JTextArea();
   JLabel      webCacheLabel      = new JLabel();
   JScrollPane webCacheScroll     = new JScrollPane();
   JTextArea   webCacheText       = new JTextArea();
   JLabel      webCacheDiscLabel  = new JLabel();
   JScrollPane webCacheDiscScroll = new JScrollPane();
   JTextArea   webCacheDiscText   = new JTextArea();
   JLabel      connectedLabel     = new JLabel();
   JTextField  connectedText      = new JTextField();

   // Read me.
   JScrollPane readMeScroll = new JScrollPane();
   JEditorPane readMe       = new JEditorPane();

   // Current file.
   JPanel       currentFilePanel       = new JPanel();
   JLabel       currentFileLabel       = new JLabel();
   JTextField   currentFileText        = new JTextField();
   JLabel       currentUniquenessLabel = new JLabel();
   JTextField   currentUniquenessText  = new JTextField();
   JLabel       currentFileTitle       = new JLabel();
   JFileChooser currentFileChooser     = new JFileChooser();
   JButton      currentBrowseButton    = new JButton();
   JLabel       currentTypeLabel       = new JLabel();
   JComboBox    currentTypeChoice      = new JComboBox();
   JLabel       currentSizeLabel       = new JLabel();
   JTextField   currentSizeText        = new JTextField();
   JLabel       currentSizeUnitsLabel  = new JLabel();

   // Status.
   JPanel     statusPanel = new JPanel();
   JLabel     statusLabel = new JLabel();
   JTextField statusText  = new JTextField();

   // Components.
   Utils        utils;
   P2P          p2p;
   Server       server;
   PrivateFiles privateFiles;
   SharedFiles  sharedFiles;
   Transfer     transfer;
   Properties   properties;

   // Master sharing switch.
   boolean sharingEnabled = true;

   // Random numbers.
   Random random;

   // Search request cache.
   Hashtable searchCache;

   /**Construct the controls*/
   public Controls()
   {
      init(SPORES_PORT);
   }


   public Controls(int port)
   {
      init(port);
   }


   void init(int port)
   {
      enableEvents(AWTEvent.WINDOW_EVENT_MASK);

      // Initialize random number generator.
      random = new Random();

      // Acquire search request cache.
      searchCache = new Hashtable();

      // Get folder names.
      File folder = new File(privateFolder);

      if (folder.exists())
      {
         privateFolder = folder.getAbsolutePath();
      }

      folder = new File(sharedFolder);

      if (folder.exists())
      {
         sharedFolder = folder.getAbsolutePath();
      }

      // Initialize GUI.
      try {
         jbInit();
      }
      catch (Exception e) {
         e.printStackTrace();
      }

      // Create the components.
      utils = new Utils();
      p2p   = null;

      try {
         p2p = new P2P(this, port);
      }
      catch (Exception e) {
         String msgString = new String("Failed to initialize P2P: " +
                                       e.toString());
         Log.getLog().logWarning(msgString);
         statusText.setText(msgString);
      }

      properties   = new Properties(this);
      privateFiles = new PrivateFiles(this);
      sharedFiles  = new SharedFiles(this);
      transfer     = new Transfer(this);
      server       = new Server(this);

      // Start server and peer-searching threads.
      server.setDaemon(true);
      server.start();
      p2p.setDaemon(true);
      p2p.start();
   }


   /**Component initialization*/
   private void jbInit() throws Exception
   {
      setIconImage(Toolkit.getDefaultToolkit().createImage(Controls.class .getResource(
                                                              "spores.jpg")));
      contentPane = (JPanel) this.getContentPane();
      contentPane.setLayout(null);
      this.setSize(new Dimension(472, 522));
      this.setTitle("Spores");
      tabbedPane.setBounds(new Rectangle(10, 10, 457, 294));
      tabbedPane.addChangeListener(new TabbedPaneChangeListener());

      // Private files.
      privateFilesTab.setLayout(null);
      privateFilesAdd.setToolTipText("Add file");
      privateFilesAdd.setText("Add");
      privateFilesAdd.setBounds(new Rectangle(57, 242, 79, 22));
      privateFilesAdd.addActionListener(new PrivateFilesAddListener());
      privateFilesDelete.setBounds(new Rectangle(144, 242, 79, 22));
      privateFilesDelete.setText("Delete");
      privateFilesDelete.setToolTipText("Delete file");
      privateFilesDelete.addActionListener(new PrivateFilesDeleteListener());
      privateFilesRefresh.setBounds(new Rectangle(231, 242, 79, 22));
      privateFilesRefresh.setText("Refresh");
      privateFilesRefresh.setToolTipText("Refresh list");
      privateFilesRefresh.addActionListener(new PrivateFilesRefreshListener());
      privateFilesFolder.setBounds(new Rectangle(318, 242, 79, 22));
      privateFilesFolder.setText("Folder");
      privateFilesFolder.setToolTipText("Private folder: " + privateFolder);
      privateFilesFolder.addActionListener(new PrivateFilesFolderListener());
      privateFilesScroll.setBounds(new Rectangle(1, 0, 450, 239));
      privateFilesTable.setToolTipText("Private files");
      privateFilesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

      ListSelectionModel rowSM = privateFilesTable.getSelectionModel();
      rowSM.addListSelectionListener(new PrivateFilesListener());
      privateFilesTab.add(privateFilesAdd, null);
      privateFilesTab.add(privateFilesDelete, null);
      privateFilesTab.add(privateFilesRefresh, null);
      privateFilesTab.add(privateFilesFolder, null);
      privateFilesTab.add(privateFilesScroll, null);
      privateFilesScroll.getViewport().add(privateFilesTable, null);
      privateFolderChooser.setDialogType(JFileChooser.OPEN_DIALOG);

      // Shared files.
      sharedFilesTab.setLayout(null);
      sharedFilesAdd.setToolTipText("Add file");
      sharedFilesAdd.setText("Add");
      sharedFilesAdd.setBounds(new Rectangle(57, 242, 79, 22));
      sharedFilesAdd.addActionListener(new SharedFilesAddListener());
      sharedFilesDelete.setBounds(new Rectangle(144, 242, 79, 22));
      sharedFilesDelete.setText("Delete");
      sharedFilesDelete.setToolTipText("Delete file");
      sharedFilesDelete.addActionListener(new SharedFilesDeleteListener());
      sharedFilesRefresh.setBounds(new Rectangle(231, 242, 79, 22));
      sharedFilesRefresh.setText("Refresh");
      sharedFilesRefresh.setToolTipText("Refresh list");
      sharedFilesRefresh.addActionListener(new SharedFilesRefreshListener());
      sharedFilesFolder.setBounds(new Rectangle(318, 242, 79, 22));
      sharedFilesFolder.setText("Folder");
      sharedFilesFolder.setToolTipText("Shared folder: " + sharedFolder);
      sharedFilesFolder.addActionListener(new SharedFilesFolderListener());
      sharedFilesScroll.setBounds(new Rectangle(1, 0, 450, 239));
      sharedFilesTable.setToolTipText("Shared files");
      sharedFilesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      rowSM = sharedFilesTable.getSelectionModel();
      rowSM.addListSelectionListener(new SharedFilesListener());
      sharedFilesTab.add(sharedFilesAdd, null);
      sharedFilesTab.add(sharedFilesDelete, null);
      sharedFilesTab.add(sharedFilesRefresh, null);
      sharedFilesTab.add(sharedFilesFolder, null);
      sharedFilesTab.add(sharedFilesScroll, null);
      sharedFilesScroll.getViewport().add(sharedFilesTable, null);

      // Transfer.
      transferTab.setLayout(null);
      transferProgressBar.setToolTipText("Operation progress");
      transferProgressBar.setBounds(new Rectangle(68, 57, 376, 16));
      transferProgressLabel.setToolTipText("Operation progress");
      transferProgressLabel.setText("Progress:");
      transferProgressLabel.setBounds(new Rectangle(9, 56, 54, 17));
      transferOperationChoice.setToolTipText("Select operation");
      transferOperationChoice.setBounds(new Rectangle(70, 9, 125, 21));
      transferOperationChoice.addItem("Search");
      transferOperationChoice.addItem("Download");
      transferOperationChoice.addItem("Upload");
      transferOperationLabel.setText("Operation:");
      transferOperationLabel.setToolTipText("Select operation");
      transferOperationLabel.setBounds(new Rectangle(9, 11, 57, 17));
      transferOperationSwitch.setToolTipText("Start/stop operation");
      transferOperationSwitch.setText("Start");
      transferOperationSwitch.setBounds(new Rectangle(332, 8, 57, 25));
      transferOperationSwitch.addActionListener(new TransferOperationSwitchListener());
      transferCopiesLabel.setToolTipText("Number of copies to search/upload");
      transferCopiesLabel.setText("Copies:");
      transferCopiesLabel.setBounds(new Rectangle(203, 11, 43, 17));
      transferCopiesText.setToolTipText("Number of copies to search/upload");
      transferCopiesText.setBounds(new Rectangle(250, 10, 61, 21));
      transferCopiesText.addActionListener(new TransferCopiesTextListener());
      transferResultsText.setEditable(false);
      transferResultsLabel.setText("Results:");
      transferResultsLabel.setBounds(new Rectangle(10, 93, 46, 17));
      transferResultsScroll.setBounds(new Rectangle(9, 109, 437, 153));
      transferTab.add(transferOperationChoice, null);
      transferTab.add(transferCopiesLabel, null);
      transferTab.add(transferCopiesText, null);
      transferTab.add(transferOperationSwitch, null);
      transferTab.add(transferOperationLabel, null);
      transferTab.add(transferProgressLabel, null);
      transferTab.add(transferProgressBar, null);
      transferTab.add(transferResultsLabel, null);
      transferTab.add(transferResultsScroll, null);
      transferResultsScroll.getViewport().add(transferResultsText, null);

      // Properties.
      propertiesTab.setLayout(null);
      propertiesTab.setLayout(null);
      propertiesFilesLabel.setText("Shared files:");
      propertiesFilesLabel.setBounds(new Rectangle(31, 154, 69, 17));
      propertiesFilesText.setEditable(false);
      propertiesFilesText.setBounds(new Rectangle(104, 153, 77, 21));
      propertiesKbytesLabel.setText("Shared Kbytes:");
      propertiesKbytesLabel.setBounds(new Rectangle(256, 152, 83, 17));
      propertiesKbytesText.setEditable(false);
      propertiesKbytesText.setBounds(new Rectangle(343, 151, 83, 21));
      propertiesRestrictionsLabel.setText("Restrictions");
      propertiesRestrictionsLabel.setBounds(new Rectangle(185, 6, 66, 17));
      propertiesStatisticsLabel.setText("Statistics");
      propertiesStatisticsLabel.setBounds(new Rectangle(192, 129, 50, 17));
      propertiesMaxFilesLabel.setText("Max shared files:");
      propertiesMaxFilesLabel.setBounds(new Rectangle(7, 35, 92, 17));
      propertiesMaxFilesText.setBounds(new Rectangle(106, 34, 76, 21));
      propertiesMaxKbytesLabel.setText("Max shared Kbytes:");
      propertiesMaxKbytesLabel.setBounds(new Rectangle(234, 35, 106, 17));
      propertiesMaxKbytesText.setBounds(new Rectangle(346, 33, 78, 21));
      propertiesExtensionLabel.setToolTipText(
         "Comma-separated list of accepted/excluded file extensions");
      propertiesExtensionLabel.setText(
         "Accepted/excluded file extensions (see Read me):");
      propertiesExtensionLabel.setBounds(new Rectangle(6, 73, 300, 17));
      propertiesExtensionText.setToolTipText(
         "Comma-separated list of accepted/excluded file extensions");
      propertiesExtensionText.setBounds(new Rectangle(7, 94, 420, 21));
      propertiesFileOrderButtonGroup.add(propertiesFileByNameButton);
      propertiesFileOrderButtonGroup.add(propertiesFileByModifiedButton);
      propertiesFileOrderButtonGroup.add(propertiesFileBySizeButton);
      propertiesFileByNameButton.setText("by name");
      propertiesFileByNameButton.setBounds(new Rectangle(19, 220, 103, 25));
      propertiesFileByNameButton.addActionListener(new PropertiesFileOrderButtonListener());
      propertiesFileDisplayLabel.setText("File ordering");
      propertiesFileDisplayLabel.setBounds(new Rectangle(186, 195, 71, 17));
      propertiesFileByModifiedButton.setText("by modification time");
      propertiesFileByModifiedButton.setBounds(new Rectangle(145, 220, 137, 25));
      propertiesFileByModifiedButton.addActionListener(new PropertiesFileOrderButtonListener());
      propertiesFileBySizeButton.setText("by size");
      propertiesFileBySizeButton.setBounds(new Rectangle(312, 221, 103, 25));
      propertiesFileBySizeButton.addActionListener(new PropertiesFileOrderButtonListener());
      propertiesTab.add(propertiesRestrictionsLabel, null);
      propertiesTab.add(propertiesMaxFilesLabel, null);
      propertiesTab.add(propertiesMaxFilesText, null);
      propertiesTab.add(propertiesMaxKbytesLabel, null);
      propertiesTab.add(propertiesMaxKbytesText, null);
      propertiesTab.add(propertiesExtensionLabel, null);
      propertiesTab.add(propertiesExtensionText, null);
      propertiesTab.add(propertiesStatisticsLabel, null);

      // Connections.
      connectionsTab.setLayout(null);
      peersLabel.setToolTipText(
         "Peer addresses to initially connect to, e.g. 302.4.44.17 or somehost.com");
      peersLabel.setText("Initial peers");
      peersLabel.setBounds(new Rectangle(190, 5, 70, 17));
      peersScroll.setBounds(new Rectangle(4, 20, 441, 61));
      webCacheLabel.setToolTipText(
         "Web caches to connect to, e.g. somehost.com/gwebcache/gcache.php");
      webCacheLabel.setText("Webcaches");
      webCacheLabel.setBounds(new Rectangle(197, 80, 65, 17));
      webCacheScroll.setBounds(new Rectangle(4, 97, 441, 61));
      webCacheDiscLabel.setToolTipText(
         "Discovered web caches - these may change!");
      webCacheDiscLabel.setText("Webcaches discovered");
      webCacheDiscLabel.setBounds(new Rectangle(168, 159, 128, 17));
      webCacheDiscScroll.setBounds(new Rectangle(4, 176, 441, 61));
      connectedLabel.setToolTipText("Active connections");
      connectedLabel.setText("Connected peers:");
      connectedLabel.setBounds(new Rectangle(5, 242, 106, 17));
      connectedText.setEditable(false);
      connectedText.setBounds(new Rectangle(108, 242, 100, 21));
      connectionsTab.add(peersScroll, null);
      connectionsTab.add(peersLabel, null);
      connectionsTab.add(webCacheLabel, null);
      connectionsTab.add(webCacheScroll, null);
      connectionsTab.add(webCacheDiscLabel, null);
      connectionsTab.add(webCacheDiscScroll, null);
      connectionsTab.add(connectedLabel, null);
      webCacheDiscScroll.getViewport().add(webCacheDiscText, null);
      webCacheScroll.getViewport().add(webCacheText, null);
      peersScroll.getViewport().add(peerText, null);

      // Read me.
      readMe.setEditable(false);
      readMe.setContentType("text/html");
      readMe.addHyperlinkListener(new ReadmeHyperlinkListener());
      createReadmeText();
      readMeScroll.getViewport().add(readMe, null);

      // Current file.
      currentFilePanel.setBorder(BorderFactory.createRaisedBevelBorder());
      currentFilePanel.setBounds(new Rectangle(9, 307, 459, 150));
      currentFilePanel.setLayout(null);
      currentFileLabel.setText("File:");
      currentFileLabel.setBounds(new Rectangle(15, 45, 23, 17));
      currentFileText.setBounds(new Rectangle(42, 44, 405, 21));
      currentUniquenessLabel.setText("Uniqueness code:");
      currentUniquenessLabel.setToolTipText("Unique file identifier");
      currentUniquenessLabel.setBounds(new Rectangle(12, 80, 101, 17));
      currentUniquenessText.setBounds(new Rectangle(117, 78, 330, 21));
      currentUniquenessText.setToolTipText("Unique file identifier");
      currentFileTitle.setText("Current File");
      currentFileTitle.setBounds(new Rectangle(197, 12, 64, 17));
      currentBrowseButton.setText("Browse...");
      currentBrowseButton.setBounds(new Rectangle(340, 112, 107, 27));
      currentBrowseButton.addActionListener(new CurrentFileBrowseListener());
      currentTypeLabel.setText("Type:");
      currentTypeLabel.setBounds(new Rectangle(13, 118, 41, 17));
      currentTypeChoice.addItem("File");
      currentTypeChoice.addItem("Folder");
      currentTypeChoice.setBounds(new Rectangle(44, 117, 80, 21));
      currentSizeLabel.setText("Size:");
      currentSizeLabel.setBounds(new Rectangle(143, 118, 41, 17));
      currentSizeText.setEditable(false);
      currentSizeText.setBounds(new Rectangle(174, 117, 100, 21));
      currentSizeUnitsLabel.setText("Kbytes");
      currentSizeUnitsLabel.setBounds(new Rectangle(280, 119, 41, 17));
      currentFilePanel.add(currentFileLabel, null);
      currentFilePanel.add(currentFileTitle, null);
      currentFilePanel.add(currentFileText, null);
      currentFilePanel.add(currentUniquenessLabel, null);
      currentFilePanel.add(currentUniquenessText, null);
      currentFilePanel.add(currentTypeLabel, null);
      currentFilePanel.add(currentTypeChoice, null);
      currentFilePanel.add(currentSizeLabel, null);
      currentFilePanel.add(currentSizeText, null);
      currentFilePanel.add(currentSizeUnitsLabel, null);
      currentFilePanel.add(currentBrowseButton, null);
      currentFileChooser.setDialogType(JFileChooser.OPEN_DIALOG);

      // Status.
      statusPanel.setBorder(BorderFactory.createRaisedBevelBorder());
      statusPanel.setBounds(new Rectangle(8, 460, 459, 30));
      statusPanel.setLayout(null);
      statusLabel.setToolTipText("Status message");
      statusLabel.setText("Status:");
      statusLabel.setBounds(new Rectangle(6, 6, 41, 17));
      statusText.setToolTipText("Status message");
      statusText.addActionListener(new StatusTextListener());
      statusText.setBounds(new Rectangle(48, 5, 407, 21));
      statusPanel.add(statusLabel, null);
      statusPanel.add(statusText, null);

      // Assemble the GUI.
      contentPane.add(tabbedPane, null);
      tabbedPane.add(privateFilesTab, "Private files");
      tabbedPane.add(sharedFilesTab, "Shared files");
      tabbedPane.add(transferTab, "Transfer");
      tabbedPane.add(propertiesTab, "Properties");
      tabbedPane.add(connectionsTab, "Connections");
      tabbedPane.add(readMeScroll, "Read me");
      contentPane.add(currentFilePanel, null);
      contentPane.add(statusPanel, null);
      connectionsTab.add(connectedText, null);
      propertiesTab.add(propertiesFileByNameButton, null);
      propertiesTab.add(propertiesFileByModifiedButton, null);
      propertiesTab.add(propertiesFileDisplayLabel, null);
      propertiesTab.add(propertiesFileBySizeButton, null);
      propertiesTab.add(propertiesFilesLabel, null);
      propertiesTab.add(propertiesFilesText, null);
      propertiesTab.add(propertiesKbytesText, null);
      propertiesTab.add(propertiesKbytesLabel, null);
   }


   /**Overridden so we can exit when window is closed*/
   protected void processWindowEvent(WindowEvent e)
   {
      super.processWindowEvent(e);

      if (e.getID() == WindowEvent.WINDOW_CLOSING)
      {
         properties.save();
         saveConnections(peerText, PEER_FILE);
         saveConnections(webCacheText, WEB_CACHE_FILE);
         saveConnections(webCacheDiscText, WEB_CACHE_DISCOVERY_FILE);
         System.exit(0);
      }
   }


   // Create text for Read me.
   void createReadmeText()
   {
      StringWriter writer = new StringWriter();

      writer.write("<HTML>\n");
      writer.write("<HEAD>\n");
      writer.write("<TITLE>Spores Read me</TITLE>\n");
      writer.write("</HEAD>\n");
      writer.write("<BODY>\n");
      writer.write("<H1>Welcome to Spores " + VERSION + "</H1>\n");
      writer.write("<HR>\n");
      writer.write("<H3>Purpose</H3>\n");
      writer.write(
         "Spores is a push and pull peer-to-peer method of file sharing and<br>\n");
      writer.write(
         "storage, making use of publicly available space on the network.<br>\n");
      writer.write(
         "A user stores a file by pushing it to a set of peers. The<br>\n");
      writer.write(
         "file then becomes visible and available to remote peers that<br>\n");
      writer.write(
         "search for it. Spores allows the exchange of folders as well as<br>\n");
      writer.write("individual files.<br>\n");
      writer.write("<H3>Private and shared files and folders</H3>\n");
      writer.write(
         "Private files/folders are visible only to the local peer. Shared<br>\n");
      writer.write(
         "files/folders are stored by remote peers and are visible and available<br>\n");
      writer.write(
         "for download by peers that search for them. Shared and private files<br>\n");
      writer.write(
         "may be copied between private and shared space at will. You may<br>\n");
      writer.write(
         "also choose where to store private and shared files.<br>\n");
      writer.write("<H3>Uniqueness code</H3>\n");
      writer.write(
         "A uniqueness code is derived from the content of a file, not its name.<br>\n");
      writer.write(
         "To ensure a search or download references the desired file or folder<br>\n");
      writer.write(
         "content, you may alternatively provide a uniqueness code as a criterion.<br>\n");
      writer.write(
         "If both code and file name are given, the target must match both.<br>\n");
      writer.write("<H3>Properties</H3>\n");
      writer.write(
         "You can specify how many shared files can be stored as well as how<br>\n");
      writer.write(
         "much space is available for them. You can also specify shared file<br>\n");
      writer.write(
         "extensions that will be accepted or excluded. For example, if you<br>\n");
      writer.write(
         "want to only accept .mp3 and .wav files, you would enter <i>.mp3,.wav</i><br>\n");
      writer.write(
         " in the text input field. Conversely if you want to accept all files<br>\n");
      writer.write(
         "except .jpg and .mpeg files, you would enter <i>-.jpg,-.mpeg</i><br>\n");
      writer.write("<H3>Connections</H3>\n");
      writer.write(
         "You can edit the initial connections that Spores makes<br>\n");
      writer.write("to find other Spores on the web.\n");
      writer.write("<UL>\n");
      writer.write(
         "<LI>Initial peers: a list of peer addresses, for example, 302.4.44.17<br>\n");
      writer.write("or somehost.com\n");
      writer.write("<LI>Webcaches: a list of gwebcache URLs\n");
      writer.write(
         "(see <A href=\"www.gnucleus.com/gwebcache/\">www.gnucleus.com/gwebcache</A>)\n");
      writer.write(
         "<LI>Webcaches discovered: a list of discovered gwebcache URLs.\n");
      writer.write("(these change as Spores discovers new ones)\n");
      writer.write("</LI>\n");
      writer.write("<H3>Files and folders</H3>\n");
      writer.write("(In " + System.getProperty("user.dir") + ")<br>\n");
      writer.write(PROPERTIES_FILE + " - properties file.<br>\n");
      writer.write(PEER_FILE +
                   " - list of peer addresses (format=IP address:port).<br>\n");
      writer.write(WEB_CACHE_FILE + " - list of gwebcache URLs<br>\n");
      writer.write(WEB_CACHE_DISCOVERY_FILE +
                   " - list of discovered gwebcache URLs.<br>\n");
      writer.write(privateFolder + " - private file folder.<br>\n");
      writer.write(sharedFolder + " - shared file folder.<br>\n");
      writer.write(TRANSFER_FOLDER + " - transfer file folder.<br>\n");
      writer.write("</BODY>\n");
      writer.write("</HTML>\n");
      readMe.setText(writer.toString());
   }


   // Set private folder.
   void setPrivateFolder(String folderName)
   {
      File folder = new File(folderName);

      if (folder.exists() && folder.isDirectory())
      {
         privateFolder = folder.getAbsolutePath();
      }
      else
      {
         privateFolder = DEFAULT_PRIVATE_FOLDER;
      }

      privateFilesFolder.setToolTipText("Private folder: " + privateFolder);
      createReadmeText();
   }


   // Set shared folder.
   void setSharedFolder(String folderName)
   {
      File folder = new File(folderName);

      if (folder.exists() && folder.isDirectory())
      {
         sharedFolder = folder.getAbsolutePath();
      }
      else
      {
         sharedFolder = DEFAULT_SHARED_FOLDER;
      }

      sharedFilesFolder.setToolTipText("Shared folder: " + sharedFolder);
      createReadmeText();
   }


   /**
    * Save connections to file.
    */
   void saveConnections(JTextArea text, String filename)
   {
      String address;

      Log.getLog().logInformation("Saving connections to " + filename);

      try {
         PrintWriter out = new PrintWriter(new BufferedWriter(
                                              new FileWriter(filename)));
         PlainDocument  doc = (PlainDocument)text.getDocument();
         BufferedReader in  = new BufferedReader(new StringReader(doc.getText(
                                                                     0, doc.getLength())));

         while (true)
         {
            address = in.readLine();

            if (address == null)
            {
               break;
            }

            out.println(address);
         }

         in.close();
         out.close();
      }
      catch (BadLocationException e) {
         String msgString = new String("Cannot save connections to file " +
                                       filename + ": " + e.toString());
         Log.getLog().logError(msgString);
         statusText.setText(msgString);
      }
      catch (IOException e) {
         String msgString = new String("Cannot save connections to file " +
                                       filename + ": " + e.toString());
         Log.getLog().logError(msgString);
         statusText.setText(msgString);
      }
   }


   // Tabs listener.
   class TabbedPaneChangeListener implements ChangeListener {
      public void stateChanged(ChangeEvent evt)
      {
         // Clear message when changing tabs.
         statusText.setText("");

         // Refresh panel?
         if (tabbedPane.getSelectedIndex() == SPORES_PROPERTIES)
         {
            properties.refresh();
         }

         if (tabbedPane.getSelectedIndex() == CONNECTIONS)
         {
            p2p.refreshConnections();
         }
      }
   }

   // Private files table model.
   class PrivateTableModel extends AbstractTableModel {
      // Column names.
      private String[] columnNames =
      {
         "File", "Type", "Size (Kbytes)", "Modified", "Uniqueness code"
      };

      // Row data.
      Object[][] rowData = new Object[0][5];

      public int getColumnCount()
      {
         return(columnNames.length);
      }


      public int getRowCount()
      {
         return(rowData.length);
      }


      public String getColumnName(int col)
      {
         return(columnNames[col]);
      }


      public Object getValueAt(int row, int col)
      {
         return(rowData[row][col]);
      }
   }

   // Private files row selection listener.
   class PrivateFilesListener implements ListSelectionListener {
      public void valueChanged(ListSelectionEvent e)
      {
         if (e.getValueIsAdjusting())
         {
            return;
         }

         ListSelectionModel lsm = (ListSelectionModel)e.getSource();

         if (!lsm.isSelectionEmpty())
         {
            privateFiles.select(lsm.getMinSelectionIndex());
            lsm.clearSelection();
         }
      }
   }

   // Private files add button listener.
   class PrivateFilesAddListener implements ActionListener {
      public void actionPerformed(ActionEvent e)
      {
         privateFiles.add();
      }
   }

   // Private files delete button listener.
   class PrivateFilesDeleteListener implements ActionListener {
      public void actionPerformed(ActionEvent e)
      {
         privateFiles.delete();
      }
   }

   // Private files refresh button listener.
   class PrivateFilesRefreshListener implements ActionListener {
      public void actionPerformed(ActionEvent e)
      {
         privateFiles.refresh();
      }
   }

   // Private files folder button listener.
   class PrivateFilesFolderListener implements ActionListener {
      public void actionPerformed(ActionEvent e)
      {
         // Open a folder chooser dialog.
         File folder = new File(privateFolder);

         privateFolderChooser.setCurrentDirectory(folder.getAbsoluteFile());
         privateFolderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

         int returnVal = privateFolderChooser.showOpenDialog(null);

         if (returnVal == JFileChooser.APPROVE_OPTION)
         {
            File file = privateFolderChooser.getSelectedFile();

            if (!file.exists() || !file.isDirectory())
            {
               statusText.setText("Invalid folder");

               return;
            }

            if (!file.canRead() || !file.canWrite())
            {
               statusText.setText("Cannot access folder");

               return;
            }

            String folderName = file.getAbsolutePath();

            if (folderName.equals(sharedFolder))
            {
               statusText.setText(
                  "Private folder must be different than shared folder");

               return;
            }

            privateFolder = folderName;
            privateFilesFolder.setToolTipText("Private folder: " +
                                              privateFolder);
            createReadmeText();
            privateFiles.refresh();
         }
      }
   }

   // Shared files folder button listener.
   class SharedFilesFolderListener implements ActionListener {
      public void actionPerformed(ActionEvent e)
      {
         // Open a folder chooser dialog.
         File folder = new File(sharedFolder);

         sharedFolderChooser.setCurrentDirectory(folder.getAbsoluteFile());
         sharedFolderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

         int returnVal = sharedFolderChooser.showOpenDialog(null);

         if (returnVal == JFileChooser.APPROVE_OPTION)
         {
            File file = sharedFolderChooser.getSelectedFile();

            if (!file.exists() || !file.isDirectory())
            {
               statusText.setText("Invalid folder");

               return;
            }

            if (!file.canRead() || !file.canWrite())
            {
               statusText.setText("Cannot access folder");

               return;
            }

            String folderName = file.getAbsolutePath();

            if (folderName.equals(privateFolder))
            {
               statusText.setText(
                  "Shared folder must be different than private folder");

               return;
            }

            sharedFolder = folderName;
            sharedFilesFolder.setToolTipText("Shared folder: " +
                                             sharedFolder);
            createReadmeText();
            sharedFiles.refresh();
         }
      }
   }

   // Shared files table model.
   class SharedTableModel extends AbstractTableModel {
      // Column names.
      private String[] columnNames =
      {
         "File", "Type", "Size (Kbytes)", "Modified", "Uniqueness code"
      };

      // Row data.
      Object[][] rowData = new Object[0][5];

      public int getColumnCount()
      {
         return(columnNames.length);
      }


      public int getRowCount()
      {
         return(rowData.length);
      }


      public String getColumnName(int col)
      {
         return(columnNames[col]);
      }


      public Object getValueAt(int row, int col)
      {
         return(rowData[row][col]);
      }
   }

   // Shared files row selection listener.
   class SharedFilesListener implements ListSelectionListener {
      public void valueChanged(ListSelectionEvent e)
      {
         if (e.getValueIsAdjusting())
         {
            return;
         }

         ListSelectionModel lsm = (ListSelectionModel)e.getSource();

         if (!lsm.isSelectionEmpty())
         {
            sharedFiles.select(lsm.getMinSelectionIndex());
            lsm.clearSelection();
         }
      }
   }

   // Shared files add button listener.
   class SharedFilesAddListener implements ActionListener {
      public void actionPerformed(ActionEvent e)
      {
         sharedFiles.add();
      }
   }

   // Shared files delete button listener.
   class SharedFilesDeleteListener implements ActionListener {
      public void actionPerformed(ActionEvent e)
      {
         sharedFiles.delete();
      }
   }

   // Shared files refresh button listener.
   class SharedFilesRefreshListener implements ActionListener {
      public void actionPerformed(ActionEvent e)
      {
         sharedFiles.refresh();
      }
   }

   // Transfer copies text listener.
   class TransferCopiesTextListener implements ActionListener {
      public void actionPerformed(ActionEvent e)
      {
         String s = statusText.getText();
      }
   }

   // Transfer operation toggle listener.
   class TransferOperationSwitchListener implements ActionListener {
      public void actionPerformed(ActionEvent e)
      {
         statusText.setText("");

         String s = statusText.getText();

         if (transferOperationSwitch.isSelected())
         {
            transferOperationSwitch.setText("Stop");
            transfer.operate(true);
         }
         else
         {
            transferOperationSwitch.setText("Start");
            transfer.operate(false);
         }
      }
   }

   // File ordering radio button listener.
   class PropertiesFileOrderButtonListener implements ActionListener {
      public void actionPerformed(ActionEvent e)
      {
         privateFiles.refresh();
         sharedFiles.refresh();
      }
   }

   // Current file browse button listener.
   class CurrentFileBrowseListener implements ActionListener {
      public void actionPerformed(ActionEvent e)
      {
         // Open a file chooser dialog.
         currentFileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

         int returnVal = currentFileChooser.showOpenDialog(null);

         if (returnVal == JFileChooser.APPROVE_OPTION)
         {
            File   file     = currentFileChooser.getSelectedFile();
            String fileName = file.getPath();
            currentFileText.setText(fileName);
            currentUniquenessText.setText(Utils.getMD5ForFile(fileName));

            if (file.isDirectory())
            {
               currentTypeChoice.setSelectedIndex(FOLDER);
            }
            else
            {
               currentTypeChoice.setSelectedIndex(FILE);
            }

            currentSizeText.setText(Long.toString(
                                       Utils.getFileSize(fileName) / 1000));
         }
      }
   }

   // Status text listener.
   class StatusTextListener implements ActionListener {
      public void actionPerformed(ActionEvent e)
      {
         String s = statusText.getText();

         if (s.equals("start logging"))
         {
            System.setProperty("Spores.Log", "true");
         }

         if (s.equals("stop logging"))
         {
            System.setProperty("Spores.Log", "false");
         }

         if (s.equals("enable sharing"))
         {
            sharingEnabled = true;
         }

         if (s.equals("disable sharing"))
         {
            sharingEnabled = false;
         }
      }
   }

   // Listen for Read me link clicks.
   class ReadmeHyperlinkListener implements HyperlinkListener {
      public void hyperlinkUpdate(HyperlinkEvent evt)
      {
         if (evt.getEventType() == HyperlinkEvent.EventType.ACTIVATED)
         {
            JEditorPane pane = (JEditorPane)evt.getSource();

            try {
               // Show the page in a new browser window
               Process browser = Runtime.getRuntime().exec("C:\\Program Files\\Internet Explorer\\IEXPLORE.EXE " +
                                                           evt.getDescription());
            }
            catch (IOException e) {
            }
         }
      }
   }
}
