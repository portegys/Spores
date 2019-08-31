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

import java.applet.Applet;
import java.applet.AudioClip;

import java.awt.*;

import java.io.*;

import java.net.*;

import java.util.*;

import javax.swing.*;


/**
 * Properties.
 */
public class Properties implements Parameters {
   // GUI controls.
   private Controls controls;

   // Constructor.
   public Properties(Controls controls)
   {
      this.controls = controls;

      // Load properties.
      load();
   }


   // Dummy constructor.
   public Properties()
   {
   }


   // Check file/folder against restrictions.
   // Return empty string if file OK, otherwise error message.
   String checkFile(String fileName)
   {
      int    value;
      String text;

      // Empty string?
      fileName = fileName.trim();

      if (fileName.equals(""))
      {
         return("");
      }

      // Max files reached?
      try {
         text  = controls.propertiesMaxFilesText.getText().trim();
         value = Integer.parseInt(text);

         if (text.equals("") || (value < 0))
         {
            value = DEFAULT_MAX_SHARED_FILES;
            controls.propertiesMaxFilesText.setText(Integer.toString(
                                                       DEFAULT_MAX_SHARED_FILES));
         }
      }
      catch (NumberFormatException e) {
         value = DEFAULT_MAX_SHARED_FILES;
         controls.propertiesMaxFilesText.setText(Integer.toString(
                                                    DEFAULT_MAX_SHARED_FILES));
      }

      if ((Utils.getFileCount(controls.sharedFolder) - 1 +
           Utils.getFileCount(fileName)) >= value)
      {
         return("Max number of shared files reached");
      }

      // Max size reached?
      try {
         text  = controls.propertiesMaxKbytesText.getText().trim();
         value = Integer.parseInt(text);

         if (text.equals("") || (value < 0))
         {
            value = DEFAULT_MAX_SHARED_SIZE;
            controls.propertiesMaxKbytesText.setText(Integer.toString(
                                                        DEFAULT_MAX_SHARED_FILES));
         }
      }
      catch (NumberFormatException e) {
         value = DEFAULT_MAX_SHARED_SIZE;
         controls.propertiesMaxKbytesText.setText(Integer.toString(
                                                     DEFAULT_MAX_SHARED_SIZE));
      }

      if ((Utils.getFileSize(controls.sharedFolder) +
           Utils.getFileSize(fileName)) > (value * 1000))
      {
         return("Max shared files total size reached");
      }

      // Check file extensions.
      return(checkFileExt(fileName));
   }


   // Check file/folder against extension restrictions.
   // Return empty string if file OK, otherwise error message.
   String checkFileExt(String fileName)
   {
      int    index;
      String exlist;
      String ext;
      String ex;

      File file = new File(fileName);

      if (file.exists() && file.isDirectory())
      {
         String retMsg = "";
         String path   = file.getAbsolutePath();
         String[] fileNames = file.list();

         for (int i = 0; i < fileNames.length; i++)
         {
            retMsg = checkFileExt(path + File.separator + fileNames[i]);

            if (!retMsg.equals(""))
            {
               return(retMsg);
            }
         }

         return(retMsg);
      }

      index = fileName.lastIndexOf(".");

      if (index == -1)
      {
         return("");
      }

      if (fileName.endsWith("."))
      {
         return("");
      }

      ext = fileName.substring(index + 1).trim();

      if (ext.equals(""))
      {
         return("");
      }

      exlist = controls.propertiesExtensionText.getText().trim();

      if (exlist.equals(""))
      {
         return("");
      }

      int             posCount  = 0;
      boolean         posMatch  = false;
      StringTokenizer tokenizer = new StringTokenizer(exlist, ",");

      while (tokenizer.hasMoreTokens())
      {
         ex = tokenizer.nextToken().trim();

         if (ex.equals("") || ex.equals(".") || ex.equals("-") ||
             ex.equals("-."))
         {
            continue;
         }

         if (ex.startsWith("-"))
         {
            ex = ex.substring(1);

            if (ex.startsWith("."))
            {
               ex = ex.substring(1);
            }

            if (ex.equals(ext))
            {
               return("Excluded extension");
            }
         }
         else
         {
            posCount++;

            if (ex.startsWith("."))
            {
               ex = ex.substring(1);
            }

            if (ex.equals(ext))
            {
               posMatch = true;
            }
         }
      }

      if ((posCount == 0) || posMatch)
      {
         return("");
      }

      return("Excluded extension");
   }


   // Refresh statistics.
   void refresh()
   {
      controls.propertiesFilesText.setText(Long.toString(Utils.getFileCount(
                                                            controls.sharedFolder) - 1));

      long size = Utils.getFileSize(controls.sharedFolder) / 1000;
      controls.propertiesKbytesText.setText(Long.toString(size));
   }


   /**
    * Load properties.
    */
   void load()
   {
      BufferedReader in = null;
      String         inputLine;

      // Clear constraints.
      controls.propertiesMaxFilesText.setText("");
      controls.propertiesMaxKbytesText.setText("");

      // Default to file ordering by name.
      controls.propertiesFileByNameButton.setSelected(true);

      // Set default constraints?
      File file = new File(PROPERTIES_FILE);

      if (!file.exists())
      {
         Log.getLog().logInformation("Setting default properties");
         controls.propertiesMaxFilesText.setText(Integer.toString(
                                                    DEFAULT_MAX_SHARED_FILES));
         controls.propertiesMaxKbytesText.setText(Integer.toString(
                                                     DEFAULT_MAX_SHARED_SIZE));

         return;
      }

      Log.getLog().logInformation("Loading properties from file " +
                                  PROPERTIES_FILE);

      try {
         in        = new BufferedReader(new FileReader(PROPERTIES_FILE));
         inputLine = in.readLine();

         if (inputLine != null)
         {
            controls.propertiesMaxFilesText.setText(inputLine);
            inputLine = in.readLine();

            if (inputLine != null)
            {
               controls.propertiesMaxKbytesText.setText(inputLine);
               inputLine = in.readLine();

               if (inputLine != null)
               {
                  controls.propertiesExtensionText.setText(inputLine);
                  inputLine = in.readLine();

                  if (inputLine != null)
                  {
                     if (inputLine.equals("Name"))
                     {
                        controls.propertiesFileByNameButton.setSelected(true);
                     }
                     else if (inputLine.equals("Modified"))
                     {
                        controls.propertiesFileByModifiedButton.setSelected(true);
                     }
                     else
                     {
                        controls.propertiesFileBySizeButton.setSelected(true);
                     }

                     inputLine = in.readLine();

                     if (inputLine != null)
                     {
                        controls.setPrivateFolder(inputLine);
                        inputLine = in.readLine();

                        if (inputLine != null)
                        {
                           controls.setSharedFolder(inputLine);
                        }
                     }
                  }
               }
            }
         }
      }
      catch (IOException e) {
         String msgString = new String("Error reading properties from file " +
                                       PROPERTIES_FILE + ": " + e.toString());
         Log.getLog().logError(msgString);
         controls.statusText.setText(msgString);
      }
      finally {
         if (in != null)
         {
            try {
               in.close();
            }
            catch (Exception e) {
            }
         }
      }

      // Validate contraint values.
      int    value;
      String text;

      try {
         text  = controls.propertiesMaxFilesText.getText().trim();
         value = Integer.parseInt(text);

         if (text.equals("") || (value < 0))
         {
            controls.propertiesMaxFilesText.setText(Integer.toString(
                                                       DEFAULT_MAX_SHARED_FILES));
         }
      }
      catch (NumberFormatException e) {
         controls.propertiesMaxFilesText.setText(Integer.toString(
                                                    DEFAULT_MAX_SHARED_FILES));
      }

      try {
         text  = controls.propertiesMaxKbytesText.getText().trim();
         value = Integer.parseInt(text);

         if (text.equals("") || (value < 0))
         {
            controls.propertiesMaxKbytesText.setText(Integer.toString(
                                                        DEFAULT_MAX_SHARED_SIZE));
         }
      }
      catch (NumberFormatException e) {
         controls.propertiesMaxKbytesText.setText(Integer.toString(
                                                     DEFAULT_MAX_SHARED_SIZE));
      }
   }


   /**
    * Save properties.
    */
   void save()
   {
      Log.getLog().logInformation("Saving properties to " + PROPERTIES_FILE);

      PrintWriter out = null;

      try {
         out = new PrintWriter(new BufferedWriter(
                                  new FileWriter(PROPERTIES_FILE)));
         out.println(controls.propertiesMaxFilesText.getText());
         out.println(controls.propertiesMaxKbytesText.getText());
         out.println(controls.propertiesExtensionText.getText());

         if (controls.propertiesFileByNameButton.isSelected())
         {
            out.println("Name");
         }
         else if (controls.propertiesFileByModifiedButton.isSelected())
         {
            out.println("Modified");
         }
         else
         {
            out.println("Size");
         }

         out.println(controls.privateFolder);
         out.println(controls.sharedFolder);
      }
      catch (IOException e) {
         String msgString = new String("Cannot save properties to file " +
                                       PROPERTIES_FILE + ": " + e.toString());
         Log.getLog().logError(msgString);
         controls.statusText.setText(msgString);
      }
      finally {
         if (out != null)
         {
            try {
               out.close();
            }
            catch (Exception e) {
            }
         }
      }
   }
}
