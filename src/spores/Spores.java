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

import javax.swing.UIManager;


/**
 * Spores peer-to-peer file sharing and storage.
 * <p>
 * Usage:<br>
 * java spores.Spores
 */
public class Spores implements Parameters {
   boolean packFrame = false;

   /**Construct the application*/
   public Spores()
   {
      // Logging.
      Log.LOGGING_FLAG = new String("Spores.Log");
      Log.LOG_FILE     = new String("spores.log");
      System.setProperty("Spores.Log", "false");   // "true" for logging.

      // Create application GUI and components.
      Controls controls = new Controls();

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

      // New version found?
      if (controls.p2p.version > VERSION)
      {
         String msgString = new String("Newer version " +
                                       controls.p2p.version + " can be downloaded at " +
                                       controls.p2p.updateAddress + "/download.html");
         Log.getLog().logInformation(msgString);
         controls.statusText.setText(msgString);

         try {
            // Let user see notice.
            Thread.sleep(VERSION_NOTICE_TIME);
         }
         catch (InterruptedException e) {
         }
      }
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

      new Spores();
   }
}
