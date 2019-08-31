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

import java.io.*;

import javax.swing.UIManager;


/**
 * Spores test driver.
 * <p>
 * Usage:<br>
 * java spores.SporesTest &ltport&gt &ltrun file full path name&gt<br>
 *   &ltdownload file full directory path name&gt<br>
 *   &ltupload file full directory path name&gt<br>
 *   &ltfile suffix> &ltmaximum file suffix&gt<br>
 * <p>
 * Procedure:<br>
 * 1. Create uniquely named files and folders in uploadfiles
 *    and downloadfiles directories.<br>
 * 2. Create <maximum file suffix> test directories for test
 *    instances and copy webcache.txt, and peer.txt files into them.<br>
 * 3. In each test directory, start an instance of spores
 *    with a unique port and suffix.<br>
 * 4. To start file transfer test, create the run file.<br>
 * 5. When done, remove run file to kill spores instances.<br>
 * <p>
 * Note: Uses UNIX native commands.
 */
public class SporesTest implements Parameters {
   static int    myPort;
   static String downloadDirectory;
   static String uploadDirectory;
   static int    myFileSuffix;
   static int    maxFileSuffix;
   static String runFileName;
   boolean       packFrame = false;

   /**Construct the application*/
   public SporesTest()
   {
      // Logging.
      Log.LOGGING_FLAG = new String("Spores.Log");
      Log.LOG_FILE     = new String("spores.log");
      System.setProperty("Spores.Log", "true");

      // Copy suffixed downloadable files to shared-files directory.
      System.out.print("Copying downloadable files from " +
                       downloadDirectory + " to shared-files directory suffixed by " +
                       myFileSuffix + "...");

      File dir = new File("shared-files");

      if (!dir.exists())
      {
         dir.mkdir();
      }

      dir = new File(downloadDirectory);

      if (!dir.exists() || !dir.isDirectory())
      {
         System.err.println("Cannot access directory " + downloadDirectory);
         System.exit(1);
      }

      String[] downloadNames = dir.list();

      for (int i = 0; i < downloadNames.length; i++)
      {
         try {
            Runtime.getRuntime().exec("cp -r " + downloadDirectory + "/" +
                                      downloadNames[i] + " " + "shared-files/" +
                                      downloadNames[i] + myFileSuffix);
         }
         catch (IOException e) {
            System.err.println("Cannot copy file " + downloadNames[i] +
                               ": " + e.toString());
         }
      }

      System.out.println("done");

      // Copy suffixed uploadable files to private-files directory.
      System.out.print("Copying uploadable files from " + uploadDirectory +
                       " to private-files directory suffixed by " + myFileSuffix + "...");
      dir = new File("private-files");

      if (!dir.exists())
      {
         dir.mkdir();
      }

      dir = new File(uploadDirectory);

      if (!dir.exists() || !dir.isDirectory())
      {
         System.err.println("Cannot access directory " + uploadDirectory);
         System.exit(1);
      }

      String[] uploadNames = dir.list();

      for (int i = 0; i < uploadNames.length; i++)
      {
         try {
            Runtime.getRuntime().exec("cp -r " + uploadDirectory + "/" +
                                      uploadNames[i] + " " + "private-files/" + uploadNames[i] +
                                      myFileSuffix);
         }
         catch (IOException e) {
            System.err.println("Cannot copy file " + uploadNames[i] + ": " +
                               e.toString());
         }
      }

      System.out.println("done");

      // Create application GUI and components.
      Controls controls = new Controls(myPort);

      // Validate frames that have preset sizes
      // Pack frames that have useful preferred size info, e.g. from their layout
      if (packFrame)
      {
         controls.pack();
      }
      else
      {
         controls.validate();
      }

      // Size and center the window
      Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
      controls.setSize(WINDOW_SIZE);
      controls.setLocation((screenSize.width - WINDOW_SIZE.width) / 2,
                           (screenSize.height - WINDOW_SIZE.height) / 2);
      controls.setVisible(true);

      // Wait for run file creation.
      System.out.println("Waiting for " + runFileName +
                         " creation to start test...");

      while (true)
      {
         File run = new File(runFileName);

         if (run.exists())
         {
            break;
         }

         try {
            System.out.println("Connections=" +
                               controls.p2p.connectedPeers.size());
            Thread.sleep(5000);
         }
         catch (InterruptedException e) {
         }
      }

      // Download files suffixed by other peers.
      System.out.print("Downloading files from peers...");

      for (int i = 0; i < downloadNames.length; i++)
      {
         for (int j = 0; j <= maxFileSuffix; j++)
         {
            if (j == myFileSuffix)
            {
               continue;
            }

            controls.currentFileText.setText(downloadNames[i] + j);
            controls.currentUniquenessText.setText("");
            controls.transferOperationChoice.setSelectedIndex(DOWNLOAD);
            controls.transfer.operation = DOWNLOAD;

            File file = new File(downloadDirectory + "/" +
                                 downloadNames[i]);

            if (file.isDirectory())
            {
               controls.currentTypeChoice.setSelectedIndex(FOLDER);
            }
            else
            {
               controls.currentTypeChoice.setSelectedIndex(FILE);
            }

            controls.transfer.operate(true);

            try {
               while (true)
               {
                  if (controls.transfer.operation == NOP)
                  {
                     break;
                  }

                  Thread.sleep(100);
               }
            }
            catch (InterruptedException e) {
            }
         }
      }

      System.out.println("done");

      // Upload my files to peers.
      System.out.print("Uploading files to peers...");

      for (int i = 0; i < uploadNames.length; i++)
      {
         controls.currentFileText.setText("private-files/" + uploadNames[i] +
                                          myFileSuffix);
         controls.transferCopiesText.setText(controls.p2p.connectedPeers.size() +
                                             "");
         controls.transferOperationChoice.setSelectedIndex(UPLOAD);
         controls.transfer.operation = UPLOAD;
         controls.transfer.operate(true);

         try {
            while (true)
            {
               if (controls.transfer.operation == NOP)
               {
                  break;
               }

               Thread.sleep(100);
            }
         }
         catch (InterruptedException e) {
         }
      }

      System.out.println("done");

      // Wait for run file removal.
      System.out.print("Waiting for " + runFileName + " removal to exit...");

      while (true)
      {
         File run = new File(runFileName);

         if (!run.exists())
         {
            break;
         }

         try {
            Thread.sleep(100);
         }
         catch (InterruptedException e) {
         }
      }

      System.out.println("exiting");
      System.exit(0);
   }


   /**Main method*/
   public static void main(String[] args)
   {
      try {
         UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
      }
      catch (Exception e) {
         e.printStackTrace();
      }

      // Get args.
      if (args.length != 6)
      {
         System.out.println("Usage:");
         System.out.println(
            "java spores.SporesTest <port> <run file full path name>");
         System.out.println("   <download file full directory path name>");
         System.out.println("   <upload file full directory path name>");
         System.out.println("   <file suffix> <maximum file suffix>");
         System.exit(1);
      }

      myPort            = Integer.parseInt(args[0]);
      runFileName       = args[1];
      downloadDirectory = args[2];
      uploadDirectory   = args[3];
      myFileSuffix      = Integer.parseInt(args[4]);
      maxFileSuffix     = Integer.parseInt(args[5]);

      // Start test.
      new SporesTest();
   }
}
