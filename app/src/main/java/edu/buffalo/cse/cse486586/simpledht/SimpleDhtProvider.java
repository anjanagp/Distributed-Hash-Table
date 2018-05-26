package edu.buffalo.cse.cse486586.simpledht;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.File;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.io.*;
import java.util.*;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.telephony.TelephonyManager;
import android.util.Log;
import java.lang.StringBuilder;

public class SimpleDhtProvider extends ContentProvider {
    static final String TAG = SimpleDhtProvider.class.getSimpleName();
    Context myContext;
    static final String REMOTE_PORT0 = "11108";
    static final String REMOTE_PORT1 = "11112";
    static final String REMOTE_PORT2 = "11116";
    static final String REMOTE_PORT3 = "11120";
    static final String REMOTE_PORT4 = "11124";
    static final int SERVER_PORT = 10000;
    static String MY_PORT;
    static String predecessor;
    static String successor;
    static String SOURCE_PORT;
    public static boolean bool = true;
    ContentResolver mContentResolver;

    MatrixCursor myMatrixCursor;


    List<String> list = new ArrayList<String>();
    List<String> list1 = new ArrayList<String>(5);
    List<String> list2 = new ArrayList<String>(5);

    /*
                * list : active list
                * list1 : port ids
                * list2 : corresponding hash values
                * Hashed value to port : list1.get(list2.indexOf(hash))
     */

    private final Uri mUri = buildUri("content", "edu.buffalo.cse.cse486586.simpledht.provider");;

    private final ContentValues mContentValues = new ContentValues();


    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }

    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {
        protected Void doInBackground(ServerSocket... sockets) {

            ServerSocket serverSocket = sockets[0];

                try {
                    Log.d("SERVER", "Entering ServerTask..");

                    while (true) {

                        Socket clientSocket = serverSocket.accept();

                        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                        PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

                        /*
                               Read the first message received from the ClientTask and split with ! delimiter
                               where requestMessage == nodeJoin if node join is requested
                               or requestMessage ==
                         */

                        String first_message_Received = in.readLine();

                        String[] first_message_received_split = first_message_Received.split("!");

                        Log.v("SERVER", "Port number sent is " + first_message_received_split[0]);

                        Log.v("SERVER", "The request sent is " + first_message_received_split[1]);

                        String portNumber = first_message_received_split[0];

                        String requestMessage = first_message_received_split[1];

                        /*
                            *requestMessage is 'nodeJoin'
                            * portNumber is the hashed port value sent = hashed value of MY_PORT/2

                            If the list is empty, add hashed value of first node ie 5554 to the list
                            If the list doesn't contain the portNumber sent then add it to the list and then sort it.
                            Gets sorted based on hashed values.

                            predecessorObtained and successorObtained are obtained from the sorted list and sent back to
                            my ClientTask.
                        */

                            //only if i'm connecting to 11108's server should i change the list (check)
                        if(requestMessage.equals("nodeJoin")) {

                            Log.v("SERVER", "Entering nodeJoin request..");

                            if(list.isEmpty()){

                                list.add(list2.get(0));
                            }
                            if (!(list.contains(portNumber))) {

                                list.add(portNumber);

                                Collections.sort(list);

                                String predecessorObtained;

                                String successorObtained;


                                int indexOfPortNumber = list.indexOf(portNumber);

                                /*
                                    Getting predecessorObtained
                                        where it is value in the list with index one less than my portNumber
                                         if previous index is -1 wrap around loop and initialise to last list value
                                 */

                                if((indexOfPortNumber - 1) < 0){

                                    Log.v("SERVER", "Entering nodeJoin obtain predecessor edge case..");

                                    predecessorObtained = list.get(list.size() - 1);

                                }

                                else {

                                    predecessorObtained = list.get(indexOfPortNumber - 1);

                                }

                                /*
                                    Obtaining successorObtained
                                        where it is value in the list with index one greater than my portNumber
                                        if next index is greater than list size wrap around loop and initialise to first list value

                                 */

                                if ((indexOfPortNumber + 1) > (list.size() - 1)) {

                                    Log.v("SERVER", "Entering nodeJoin obtain successor edge case..");

                                    successorObtained = list.get(0);

                                }

                                else {

                                    successorObtained = list.get(indexOfPortNumber + 1);
                                }

                                /*
                                    obtain the respective port numbers for the hashed values of predecessorObtained and successorObtained
                                     Send these port numbers back to the ClientTask of my port

                                 */

                                int ind_1 = list2.indexOf(predecessorObtained);

                                predecessorObtained = list1.get(ind_1);

                                int ind_2 = list2.indexOf(successorObtained);

                                successorObtained = list1.get(ind_2);

                                Log.v("SERVER", "predecessorObtained value being sent " + predecessorObtained + "\n" + "successorObtained value being sent " + successorObtained);

                                out.write(predecessorObtained + "\n");

                                out.flush();

                                out.write(successorObtained + "\n");

                                out.flush();
                            }
                        }

                        /*
                            *requestMessage is "updatePredOrSucc"
                            * portNumber is my predecessor's port which isn't used here
                            *
                            * ports[0] -> MY_PORT/2
                            * ports[1] -> pre or succ
                            *
                            * if ports[1] -> pre, change my predecessor's successor


                         */
                         else if (requestMessage.equals("updatePredOrSucc")) {

                            Log.v("SERVER", "Entering updatePredOrSucc request..");

                            String port = in.readLine();

                            String[] ports = port.split("#");

                            String my_Port = ports[0];

                            String nodeToChange = ports[1];

                            if (nodeToChange.equals("pre")) {

                                successor = my_Port;

                                Log.v("SERVER", "Changing my predecessor's successor value to " + my_Port);

                            } else if(nodeToChange.equals("success")){

                                predecessor = my_Port;

                                Log.v("SERVER", "Changing my successor's predecessor value to " + my_Port);

                            }

                            Log.d("SERVER", "****FINAL PREDECESSOR VALUE***** "+ predecessor);

                            Log.d("SERVER", "*****FINAL SUCCESSOR VALUE***** " + successor);
                         }

                        else if(requestMessage.equals("insertMessage")) {

                            Log.v("SERVER", "Entering insert part in Servertask");

                            String key_value_pair = in.readLine();

                            String[] key_value_pair_Array = key_value_pair.split("!");

                            String key = key_value_pair_Array[0];

                            String value = key_value_pair_Array[1];

                            //Call the insert function with key and value to check if it belongs to this node

                            Log.v("SERVER", "Calling insert function in Servertask");

                            mContentValues.put("key", key);

                            mContentValues.put("value", value);

                            mContentResolver.insert(mUri, mContentValues);

                        }

                        else if(requestMessage.equals("queryMessage")) {

                            Log.v("SERVER", "Entering query part in Servertask");

                            String key_to_find = in.readLine();

                            mContentResolver.query(mUri, null, key_to_find + "!" + portNumber, null, null);

                        }

                        else if(requestMessage.equals("querySuccess")){

                            Log.v("SERVER", "Entering querySuccess part in Servertask");

                            String key_value = in.readLine();

                            String[] key_value_array = key_value.split("!");

                            //add to matrixcursor

                            myMatrixCursor.addRow(new Object[]{key_value_array[0], key_value_array[1]});
                        }

                        else if(requestMessage.equals("queryStar")){

                            Log.v("SERVER", "Entering queryStar part in Servertask..");

                            //call my query function so it returns all its matrixcursor objects

                            MatrixCursor resultCursor = new MatrixCursor(new String[] {"key", "value"});

                            StringBuilder stringbuild = new StringBuilder();
                            stringbuild.append("querystarapp");

                            String path = getContext().getFilesDir().getPath();
                            File dir = new File(path);
                            File[] directoryListing = dir.listFiles();
                            if (directoryListing != null) {
                                for (File child : directoryListing) {
                                    BufferedReader inflow = new BufferedReader(new InputStreamReader(myContext.openFileInput(child.getName())));
                                    String string_readcontent = inflow.readLine();
                                    stringbuild.append("!");
                                    stringbuild.append(child.getName());
                                    stringbuild.append("#");
                                    stringbuild.append(string_readcontent);


                                }


                            }


                          /*  while (resultCursor.moveToNext()) {

                               stringbuild.append(resultCursor.getString(0));
                               stringbuild.append("#");
                               stringbuild.append((resultCursor.getString(1)));
                               stringbuild.append("!");

                            }*/
                            String finalString = stringbuild.toString();

                            Log.d("Query Star Response", finalString);

                            out.write(finalString + "\n");

                            out.flush();

                            Log.v("SERVER", "BEFORE SUCCESSOR");

                            out.write(successor + "\n");

                            Log.v("SERVER", "AFTER SUCCESSOR");

                            out.flush();

                        }
                    }
                } catch (IOException e) {

                    Log.e("Server", "ClientTask socket IOException");

                }


            return null;
        }

    }

    private class ClientTask extends AsyncTask<String, Void, Void> {

        protected Void doInBackground(String... msgs) {
            try {

                Log.v("CLIENT", "Entering client task..");

                String port_me = msgs[0];

                String[] port_me_delimit = port_me.split("!");

                String my_port = port_me_delimit[0];

                String successor_received = my_port;

                Log.v("CLIENT", "Successor_received after entering client task is " + successor_received);

                String message = port_me_delimit[1];

                String key = port_me_delimit[2];

                String value = port_me_delimit[3];

                if (message.equals("join")) {


                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt("11108"));


                    int port_me_int_by2 = Integer.valueOf(my_port) / 2;
                    String port_me_string_by2 = Integer.toString(port_me_int_by2);

                    //send hashed value of port_me_int_by2
                    int index1 = list1.indexOf(port_me_string_by2);
                    String port_me_to = list2.get(index1);


                    BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));

                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                    Log.e("CLIENT", "PORT_ME " + my_port);
                    out.write(port_me_to + "!" + "nodeJoin" + "\n");

                    out.flush();

                    //msg pred n change its successor and msg successor n change its predecessor
                    String pre_port = in.readLine();
                    Log.e("CLIENT", "THIS IS PRE_PORT " + pre_port);

                    String success_port = in.readLine();
                    Log.e("CLIENT", "THIS IS SUCCESS_PORT " + success_port);

                    if(pre_port == null || success_port == null)
                        return null;

                    predecessor = pre_port;
                    successor = success_port;


                    socket.close();

                    int pre_port_x2 = Integer.parseInt(pre_port) * 2;
                    String pre_port_2 = Integer.toString(pre_port_x2);
                    Log.e("CLIENT", "PRE_PORT_X2 " + pre_port_2);

                    int success_port_x2 = Integer.parseInt(success_port) * 2;
                    String success_port_2 = Integer.toString(success_port_x2);
                    Log.e("CLIENT", "SUCC_PORT_X2 " + success_port_2);

                    Socket socket_pre = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(pre_port_2));

                    BufferedWriter out_pre = new BufferedWriter(new OutputStreamWriter(socket_pre.getOutputStream(), "UTF-8"));

                    Log.e("CLIENT", "PRE_PORT " + pre_port);
                    out_pre.write(pre_port + "!" + "updatePredOrSucc" + "\n");
                    out_pre.flush();
                    Log.e("CLIENT", " Integer.valueOf(port_me)/2" + Integer.valueOf(my_port) / 2);
                    out_pre.write(port_me_int_by2 + "#" + "pre" + "\n");
                    out_pre.flush();

                    Log.e("CLIENT", "CLIENT CHECK");

                    socket_pre.close();


                    Socket socket_succ = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(success_port_2));

                    BufferedWriter out_succ = new BufferedWriter(new OutputStreamWriter(socket_succ.getOutputStream(), "UTF-8"));
                    BufferedReader in_succ = new BufferedReader(new InputStreamReader(socket_succ.getInputStream()));

                    Log.e("CLIENT", "SUCCESS_PORT" + success_port);
                    out_succ.write(success_port + "!" + "updatePredOrSucc" + "\n");
                    out_succ.flush();
                    Log.e("CLIENT", "Integer.valueOf(port_me)/2" + port_me_int_by2);
                    out_succ.write(port_me_int_by2 + "#" + "success" + "\n");
                    out_succ.flush();


                    socket_succ.close();

                    Log.e("CLIENT", "****FINAL PREDECESSOR VALUE " + predecessor);
                    Log.e("CLIENT", "*****FINAL SUCCESSOR VALUE " + successor);
                }

                else if(message.equals("insert")){

                    Log.v("CLIENT", "Entering insert part of ClientTask..");

                    Socket socket_insert = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(successor)*2);

                    BufferedWriter out_insert = new BufferedWriter(new OutputStreamWriter(socket_insert.getOutputStream(), "UTF-8"));

                    BufferedReader in_insert = new BufferedReader(new InputStreamReader(socket_insert.getInputStream()));

                    // Send the key, value and my port which is the source port

                    out_insert.write( MY_PORT + "!" + "insertMessage" + "\n" );

                    out_insert.flush();

                    Log.v("CLIENT", "Going to send key-value pairs in ClientTask to Servertask in insert..");

                    out_insert.write(key + "!" + value + "\n");

                    Log.v("CLIENT", "Key value is " + key + " and " + "value value is " + value);

                    out_insert.flush();

                    socket_insert.close();

                }

                else if (message.equals("query")) {

                    Log.v("CLIENT", "Entering query part in ClientTask..");

                    Socket socket_query = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(successor)*2);

                    BufferedWriter out_query = new BufferedWriter(new OutputStreamWriter(socket_query.getOutputStream(), "UTF-8"));

                    BufferedReader in_query = new BufferedReader(new InputStreamReader(socket_query.getInputStream()));

                    out_query.write( SOURCE_PORT + "!" + "queryMessage" + "\n" );

                    out_query.flush();

                    Log.v("CLIENT", "Going to send key-value pairs in ClientTask to Servertask in insert..");

                    out_query.write(key + "\n");

                    out_query.flush();

                    socket_query.close();

                }

                else if (message.equals("query_successful")){

                    Log.v("CLIENT", "Entering query_successful part in ClientTask..");

                    Socket socket_query_successful = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(my_port));

                    BufferedWriter out_query_successful = new BufferedWriter(new OutputStreamWriter(socket_query_successful.getOutputStream(), "UTF-8"));

                    BufferedReader in_query_successful = new BufferedReader(new InputStreamReader(socket_query_successful.getInputStream()));

                    out_query_successful.write( SOURCE_PORT + "!" + "querySuccess" + "\n" );

                    out_query_successful.flush();

                    out_query_successful.write(key + "!" + value + "\n");

                    out_query_successful.flush();

                    out_query_successful.close();

                }

                else if (message.equals("star_query")) {


                    // while successor_received*2 is not equal to myport
                    //setting successor_received to my_port initially above
                    while ((Integer.parseInt(successor_received) * 2) != Integer.parseInt(MY_PORT)) {
                     Log.v("CLIENT", "The successor_Received value in star_query part.." + successor_received);



                     Log.v("CLIENT", "Entering star_query part in ClientTask..");

                    Socket socket_query_star = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(successor_received) * 2);

                    BufferedWriter out_query_star = new BufferedWriter(new OutputStreamWriter(socket_query_star.getOutputStream(), "UTF-8"));

                    BufferedReader in_query_star = new BufferedReader(new InputStreamReader(socket_query_star.getInputStream()));

                    out_query_star.write(successor + "!" + "queryStar" + "\n");

                    out_query_star.flush();

                    //need to receive matrixcursor elements to add to the matrix cursor and the successor port and only then close socket

                    String received_matrix_objects = in_query_star.readLine();


                    Log.v("CLIENT", "RECEIVED_MATRIX_OBJECTS "  + received_matrix_objects);

                    //need to split
                    String[] matrix_each_row = received_matrix_objects.split("!");
                    String[] key_value_each_row;



                    for (int i = 1; i < matrix_each_row.length; i++) {

                        Log.v("CLIENT", "Entering loop to iterate matrx received..");

                        key_value_each_row = matrix_each_row[i].split("#");
                        String key_each_row = key_value_each_row[0];
                        String val_each_row = key_value_each_row[1];

                        myMatrixCursor.addRow(new Object[]{key_each_row, val_each_row});
                    }

                    successor_received = in_query_star.readLine();

                    Log.v("CLIENT", "The successor received is : " + successor_received);

                    socket_query_star.close();

                }


                    bool = false;






                }




            }catch (Exception e) {
                Log.e("Client", "ClientTask UnknownHostException ");
            }

            //wait for ack to return null or wait for a few ms

            return null;
        }

        }



    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub

        File dir = getContext().getFilesDir();
        File file = new File(dir, selection);
        file.delete();

        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // TODO Auto-generated method stub
        String filename = values.get("key").toString();

        //Log.e(TAG, "filename " + filename);

        String string = values.get("value").toString();

        //Log.e(TAG, "string " + string);

        try {

            Log.e("INSERT", "Entering insert function..");

            String filename_hashed = genHash(filename);

            String predecessor_hashed =  list2.get(list1.indexOf(predecessor)) ;

            int my_Port = (Integer.parseInt(MY_PORT)) /2;

            String my_Port_string = Integer.toString(my_Port);

            String my_Port_hashed = list2.get(list1.indexOf(my_Port_string));

            Log.v("INSERT", "Filename_hashed is " + filename_hashed + ":" + " Predecessor hashed is " + predecessor_hashed + " :" + " my_port_hashed is " + my_Port_hashed );
            Log.v("INSERT", "Filename is " + filename + " and " + "Predecessor is " + predecessor + " and " + "Successor is " + successor);

            if(genHash(my_Port_string).compareTo(genHash(predecessor)) <= 0 && genHash(my_Port_string).compareTo(genHash(successor)) <= 0) {

                if (filename_hashed.compareTo(predecessor_hashed) > 0 || filename_hashed.compareTo(my_Port_hashed) <= 0) {

                    Log.e("INSERT", "First of chord: BELONGS TO ME " + MY_PORT);

                    FileOutputStream outputStream;

                    outputStream =  myContext.openFileOutput(filename, Context.MODE_MULTI_PROCESS);//change mode

                    outputStream.write(string.getBytes());

                    outputStream.close();

                } else {

                    Log.d("INSERT", "First of chord: PASS TO SUCCESSOR ");
                    //pass it to my successor
                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, MY_PORT + "!" + "insert" + "!" + filename + "!" + string);



                }
            }

            else{

                if(filename_hashed.compareTo(predecessor_hashed) > 0 && filename_hashed.compareTo(my_Port_hashed) <= 0){
                    Log.d("INSERT", "Not first of chord: BELONGS");


                    FileOutputStream outputStream;

                    outputStream =  myContext.openFileOutput(filename, Context.MODE_MULTI_PROCESS);//change mode

                    outputStream.write(string.getBytes());

                    outputStream.close();
                }
                else{
                    Log.d("INSERT", "Not first of chord: DOESN'T BELONG");
                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, MY_PORT + "!" + "insert" + "!" + filename + "!" + string);

                }
            }



        } catch (IOException e) {
            Log.e(TAG, "File write failed");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        Log.v("insert", values.toString());
        return null;
    }

    @Override
    public boolean onCreate() {
        // TODO Auto-generated method stub
        TelephonyManager tel = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        MY_PORT = String.valueOf((Integer.parseInt(portStr) * 2));

        int my_port_int = (Integer.parseInt(MY_PORT)) /2;
        String my_port_int_to_string = Integer.toString(my_port_int);
        Log.e("MYPORT","myport: " + my_port_int);

        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);

            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);

        }catch(IOException e){

            Log.e(TAG, "Can't create a ServerSocket");
        }

        predecessor = my_port_int_to_string;

        successor = my_port_int_to_string;

        myContext = getContext();

        mContentResolver = myContext.getContentResolver();

        list1.add("5554");
        list1.add("5556");
        list1.add("5558");
        list1.add("5560");
        list1.add("5562");

        Log.v("ON CREATE", "LIST 1 COMPLETE");
        try{
            list2.add(genHash(list1.get(0)));
            list2.add(genHash(list1.get(1)));
            list2.add(genHash(list1.get(2)));
            list2.add(genHash(list1.get(3)));
            list2.add(genHash(list1.get(4)));
            Log.v("ON CREATE", "LIST 2 COMPLETE");
        }catch(NoSuchAlgorithmException e){

        }

            if(my_port_int_to_string.equals("5554")){
                predecessor = "5554";
                successor = "5554";

            }

            else{

                //call clienttask here n create a connection between 5554 and this port
                Log.e("ON_CREATE", "ENTER ELSE ON_CREATE");
                Log.e("ON CREATE", "PORTSTR " + MY_PORT);
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, MY_PORT + "!" + "join" + "!" + "0" + "!" + "1");
            }

        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        // TODO Auto-generated method stub
        String filename = selection;
        Log.e(TAG, filename);

        //split the filename, if the second part is null its original, otherwise i've called it
        String[] split_key = filename.split("!");

        if(split_key.length == 2){

            SOURCE_PORT = split_key[1];
        }

        else{

            SOURCE_PORT = MY_PORT;
        }

        filename = split_key[0];

        String string_readcontent;

        myMatrixCursor = null;
        myMatrixCursor = new MatrixCursor(new String[] {"key", "value"});

        try {

            if(selection.equals("*")) {

                Log.v("QUERY", "Enters * selection in query..");
                String path = getContext().getFilesDir().getPath();
                File dir = new File(path);
                File[] directoryListing = dir.listFiles();
                if (directoryListing != null) {
                    for (File child : directoryListing) {
                        BufferedReader in = new BufferedReader(new InputStreamReader(myContext.openFileInput(child.getName())));
                        string_readcontent = in.readLine();
                        myMatrixCursor.addRow(new Object[]{child.getName(), string_readcontent});
                    }


                }
                Log.v("QUERY", "Finished populating its matrixcursor in query..");

                //Call clienttask which has to connect to my successor
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, successor + "!" + "star_query" + "!" + "0" + "!" + "1");

                bool = true;

                while(bool){

                }

            }

            else if(selection.equals("@")) {
                String path = getContext().getFilesDir().getPath();
                File dir = new File(path);
                File[] directoryListing = dir.listFiles();
                if (directoryListing != null) {
                    for (File child : directoryListing) {
                        BufferedReader in = new BufferedReader(new InputStreamReader(myContext.openFileInput(child.getName())));

                        string_readcontent = in.readLine();
                        myMatrixCursor.addRow(new Object[]{child.getName(), string_readcontent});
                    }
                }
            }

            else {

                try {

                    Log.v("QUERY", "Entering when not * or @.. in query");

                    BufferedReader in = new BufferedReader(new InputStreamReader(myContext.openFileInput(filename)));

                    string_readcontent = in.readLine();

                    Log.v("QUERY", "value of string_Readcontent " + string_readcontent);
                    myMatrixCursor.addRow(new Object[]{filename, string_readcontent});

                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, SOURCE_PORT + "!" + "query_successful" + "!" + filename + "!" + string_readcontent);



                }catch (FileNotFoundException e){

                    Log.v("QUERY", "Entering catch block for file not found so passed to successor..");

                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, SOURCE_PORT + "!" + "query" + "!" + filename + "!" +"0" );


                }

            }

            if(SOURCE_PORT == MY_PORT){

                Thread.sleep(500);
            }

            Log.v("query", selection.toString());
            return myMatrixCursor;
        }
        catch (Exception e)  {
            Log.e(TAG, "File not found");
        }
        /*
         * TODO: You need to implement this method. Note that you need to return a Cursor object
         * with the right format. If the formatting is not correct, then it is not going to work.
         *
         * If you use SQLite, whatever is returned from SQLite is a Cursor object. However, you
         * still need to be careful because the formatting might still be incorrect.
         *
         * If you use a file storage option, then it is your job to build a Cursor * object. I
         * recommend building a MatrixCursor described at:
         * http://developer.android.com/reference/android/database/MatrixCursor.html
         */
        Log.v("query", selection);
        return null;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
        return 0;
    }

    private String genHash(String input) throws NoSuchAlgorithmException {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        byte[] sha1Hash = sha1.digest(input.getBytes());
        Formatter formatter = new Formatter();
        for (byte b : sha1Hash) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }
}
