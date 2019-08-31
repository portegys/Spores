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

import spores.Utils.*;

import java.io.*;

import java.text.*;

import java.util.*;

import javax.swing.*;


/**
 * Shared files.
 */
public class SharedFiles implements Parameters {
   // GUI controls.
   Controls controls;

   // File list.
   LinkedList fileList;
   int        fileIndex;

   // File name comparator.
   Utils.NameCompare nameCompare;

   // File modified time comparator.
   Utils.ModifiedCompare modifiedCompare;

   // File size comparator.
   Utils.SizeCompare sizeCompare;

   // Constructor.
   public SharedFiles(Controls controls)
   {
      this.controls = controls;

      // Create folder.
      File fileFolder = new File(controls.sharedFolder);

      if (!fileFolder.exists())
      {
         fileFolder.mkdir();
      }

      // List files.
      nameCompare     = new Utils.NameCompare();
      modifiedCompare = new Utils.ModifiedCompare();
      sizeCompare     = new Utils.SizeCompare();
      refresh();
   }


   // Dummy constructor.
   public SharedFiles()
   {
   }


   /**
    * Select file.
    */
   void select(int index)
   {
      controls.statusText.setText("");

      if ((index >= 0) && (index <= fileList.size()))
      {
         fileIndex = index;

         Utils.FileElem fileElem = (Utils.FileElem)fileList.get(fileIndex);
         String         fileName = controls.sharedFolder + File.separator +
                                   fileElem.name;
         controls.currentFileText.setText(fileName);
         controls.currentUniquenessText.setText(fileElem.md5code);
         controls.currentTypeChoice.setSelectedIndex(fileElem.type);
         controls.currentSizeText.setText(Long.toString(fileElem.size / 1000));
      }
   }


   /**
    * Add file.
    */
   void add()
   {
      FileElem fileElem;
      String   fileName;
      String   currentFileName;

      controls.statusText.setText("");

      // Check for duplicate.
      currentFileName = controls.currentFileText.getText().trim();

      if (currentFileName.equals(""))
      {
         return;
      }

      if (currentFileName.startsWith(controls.sharedFolder))
      {
         controls.statusText.setText("File already in folder");

         return;
      }

      for (int i = 0; i < fileList.size(); i++)
      {
         fileElem = (FileElem)fileList.get(i);
         fileName = fileElem.name;

         if (currentFileName.equals(fileName) ||
             currentFileName.endsWith(File.separator + fileName))
         {
            controls.statusText.setText("Duplicate file entry " + i);

            return;
         }
      }

      // Check property restrictions.
      String errmsg = controls.properties.checkFile(currentFileName);

      if (!errmsg.equals(""))
      {
         controls.statusText.setText("Cannot add file " + currentFileName +
                                     ": " + errmsg);

         return;
      }

      // Copy file into shared folder.
      File currentFile = new File(currentFileName);

      if (!currentFile.canRead())
      {
         controls.statusText.setText("Cannot access file " +
                                     currentFileName);

         return;
      }

      fileName = currentFile.getName();

      try {
         Utils.copyFile(currentFileName,
                        controls.sharedFolder + File.separator + fileName);
      }
      catch (IOException e) {
         controls.statusText.setText("Cannot copy file " + currentFileName +
                                     ": " + e.toString());

         return;
      }

      refresh();
   }


   /**
    * Delete file.
    */
   void delete()
   {
      FileElem fileElem;
      String   fileName;
      String   currentFileName;

      controls.statusText.setText("");

      currentFileName = controls.currentFileText.getText().trim();

      if (!currentFileName.startsWith(controls.sharedFolder))
      {
         return;
      }

      File file = new File(currentFileName);

      if (!file.exists())
      {
         return;
      }

      currentFileName = file.getName();

      for (int i = 0; i < fileList.size(); i++)
      {
         fileElem = (FileElem)fileList.get(i);
         fileName = fileElem.name;

         if (currentFileName.equals(fileName))
         {
            file = new File(controls.sharedFolder, fileName);

            if (file.exists())
            {
               Utils.deleteFile(file.getAbsolutePath());
            }

            refresh();

            return;
         }
      }

      controls.statusText.setText("Cannot delete file " + currentFileName);
   }


   /**
    * Refresh file list.
    */
   void refresh()
   {
      controls.statusText.setText("");

      LinkedList     fileNames   = Utils.listFiles(controls.sharedFolder);
      LinkedList     newFileList = new LinkedList();
      Utils.FileElem fileElem;
      String         fileName;
      File           file;

      for (int i = 0; i < fileNames.size(); i++)
      {
         fileElem         = new Utils.FileElem();
         fileElem.name    = (String)fileNames.get(i);
         fileName         = controls.sharedFolder + File.separator + fileElem.name;
         fileElem.md5code = Utils.getMD5ForFile(fileName);
         file             = new File(fileName);

         if (file.exists() && file.canRead())
         {
            if (file.isDirectory())
            {
               fileElem.type = FOLDER;
            }
            else
            {
               fileElem.type = FILE;
            }

            fileElem.size     = Utils.getFileSize(file.getAbsolutePath());
            fileElem.modified = file.lastModified();
         }
         else
         {
            fileElem.type     = FILE;
            fileElem.size     = 0;
            fileElem.modified = 0;
         }

         newFileList.add(fileElem);
      }

      // Sort by user preference.
      if (controls.propertiesFileByNameButton.isSelected())
      {
         Collections.sort(newFileList, nameCompare);
      }
      else if (controls.propertiesFileByModifiedButton.isSelected())
      {
         Collections.sort(newFileList, modifiedCompare);
      }
      else
      {
         Collections.sort(newFileList, sizeCompare);
      }

      fileList = newFileList;
      display();
   }


   /**
    * Display file list.
    */
   private void display()
   {
      FileElem         fileElem;
      String           size;
      Date             modified;
      String           date;
      SimpleDateFormat formatter = new SimpleDateFormat(
         "MM/dd/yyyy hh:mm aaa");

      Object[][] rowData = new Object[fileList.size()][5];

      for (int i = 0; i < fileList.size(); i++)
      {
         fileElem      = (FileElem)fileList.get(i);
         rowData[i][0] = fileElem.name;

         if (fileElem.type == FOLDER)
         {
            rowData[i][1] = "folder";
         }
         else
         {
            rowData[i][1] = "file";
         }

         size          = Long.toString(fileElem.size / 1000);
         rowData[i][2] = size;
         modified      = new Date(fileElem.modified);
         rowData[i][3] = modified;
         date          = formatter.format(modified);
         rowData[i][4] = fileElem.md5code;
      }

      controls.sharedTableModel.rowData = rowData;
      controls.sharedTableModel.fireTableDataChanged();
   }
}
