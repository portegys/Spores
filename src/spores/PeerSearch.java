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

import java.io.*;

import java.net.*;

import java.util.*;


/**
 * Recursive/concurrent peer search.
 */
public class PeerSearch extends Thread implements Parameters {
   // Results.
   static final int SUCCESS = 0;
   static final int FAIL    = 1;
   static final int ERROR   = 2;
   static final int NONE    = 3;

   // Utililities.
   private Utils utils;

   // Search parameters.
   P2P.PeerList peerList;
   int          localPort;
   String       searchFile;
   String       searchType;
   String       searchCode;
   String       searchID;
   long         searchCopies;
   long         searchDepth;
   SearchThread[] searchThreads;

   // Constructor.
   PeerSearch(Utils utils, P2P.PeerList peerList, int localPort,
              String searchFile, String searchType, String searchCode,
              String searchID, long searchCopies, long searchDepth)
   {
      this.utils        = utils;
      this.peerList     = peerList;
      this.localPort    = localPort;
      this.searchFile   = searchFile;
      this.searchType   = searchType;
      this.searchCode   = searchCode;
      this.searchID     = searchID;
      this.searchCopies = searchCopies;
      this.searchDepth  = searchDepth;
      setDaemon(true);

      searchThreads = new SearchThread[peerList.size()];

      P2P.PeerAddress peerAddress;

      for (int i = 0; i < peerList.size(); i++)
      {
         peerAddress      = (P2P.PeerAddress)peerList.get(i);
         searchThreads[i] = new SearchThread(peerAddress);
      }
   }


   // Run search.
   public void run()
   {
      if (searchCopies == 0)
      {
         return;
      }

      // Start the search threads.
      for (int i = 0; i < searchThreads.length; i++)
      {
         searchThreads[i].start();
      }

      // Wait for thread completions or time-out.
      for (int i = 0; i < SEARCH_TIME_OUT; i += 100)
      {
         int j;

         for (j = 0; j < searchThreads.length; j++)
         {
            if (searchThreads[j].isAlive())
            {
               break;
            }
         }

         if (j == searchThreads.length)
         {
            break;
         }

         try {
            Thread.sleep(100);
         }
         catch (InterruptedException e) {
         }
      }

      // Interrupt incomplete threads.
      for (int i = 0; i < searchThreads.length; i++)
      {
         if (searchThreads[i].isAlive())
         {
            searchThreads[i].interrupt();
         }
      }
   }


   // Search a peer.
   class SearchThread extends Thread {
      // Parameters.
      P2P.PeerAddress peerAddress;

      // Results.
      int    result;
      String targetName;
      String targetPeer;
      long   targetPort;
      long   targetSize;
      long   copiesFound;

      // Constructor.
      SearchThread(P2P.PeerAddress peerAddress)
      {
         this.peerAddress = peerAddress;
         result           = NONE;
         targetPeer       = null;
         targetSize       = 0;
         copiesFound      = 0;
         setDaemon(true);
      }


      // Perform peer search.
      public void run()
      {
         Socket socket = null;

         try {
            socket = utils.createSocket(peerAddress.host, peerAddress.port);

            // Send search request.
            PrintWriter out = utils.getPrintWriter(socket);
            utils.printToStream(out,
                                Double.toString(VERSION) + "\n" +
                                Integer.toString(localPort) + "\n" + SEARCH_REQUEST + "\n" +
                                searchFile + "\n" + searchType + "\n" + searchCode + "\n" +
                                searchID + "\n" + Long.toString(searchCopies) + "\n" +
                                Long.toString(searchDepth));

            // Get response.
            DataInputStream in       = utils.getDataInputStream(socket);
            byte            response = utils.readByteFromStream(in, SEARCH_TIME_OUT);

            if (response == POSITIVE_RESPONSE)
            {
               result      = SUCCESS;
               targetName  = utils.readLineFromStream(in);
               targetPeer  = utils.readLineFromStream(in);
               targetPort  = utils.readLongFromStream(in);
               targetSize  = utils.readLongFromStream(in);
               copiesFound = utils.readLongFromStream(in);
            }
            else
            {
               result = FAIL;
            }
         }
         catch (Exception e) {
            result = ERROR;

            String msgString = new String("Error searching " + searchType +
                                          " " + searchFile + " from " + peerAddress.host + ":" +
                                          peerAddress.port + ": " + e.toString());
            Log.getLog().logWarning(msgString);
         }
         finally {
            try {
               if (socket != null)
               {
                  utils.closeSocket(socket);
               }
            }
            catch (Exception e) {
            }
         }
      }
   }
}
