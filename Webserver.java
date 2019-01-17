/**
 *  Webserver.java
 *  A simple server that responds to GET and HEAD requests.
 *  @author: Gabriel Brolo, 15105. Universidad del Valle de Guatemala. Redes.
 *  1/9/2019
 */
import java.io.*;
import java.net.*;
import java.util.*;
import java.net.InetAddress.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

final class HttpRequest implements Runnable {
    Socket socket;

    public HttpRequest(Socket socket) throws Exception { this.socket = socket; }

    public void run() {
        try {
            makeRequest();
        } catch (Exception e) { 
            //System.out.println(e); 
        }
    }

    private void makeRequest() throws Exception {
        InputStream in = socket.getInputStream();
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
        BufferedReader br = new BufferedReader(new InputStreamReader(in));

        // get request
        String request = br.readLine();
        System.out.println("\nProcessing new request: \n" + request);

        // METHOD TYPE
        String [] requestSplit = request.split(" ");
        String methodType = requestSplit[0];

        String documentRequested = requestSplit[1].substring(1);

        System.out.println("\nDocument requested: \n" + documentRequested);

        processFile(documentRequested, out, methodType);

        out.close();
        br.close();
        socket.close();
    }

    private void processFile(String documentRequested, DataOutputStream out, String methodType) {
        File f = new File(documentRequested);
        if(f.exists() && !f.isDirectory()) {            
            String contentType = contentType(documentRequested);

            try {
                byte[] documentRead = readFile(documentRequested, contentType);
                DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
	            Date date = new Date();

                String response = "HTTP/1.1 200 OK\n" +
                "Connection close\n" +
                "Date: " + dateFormat.format(date) + "\n" +
                "Server: brolius\n" +
                "Content-Length: " + documentRead.length + "\n" +
                "Content-Type: " + contentType + "\n" +
                "\n";

                if (methodType.equals("GET")) {
                    for (Byte b : response.getBytes()) {
                        out.write(b);
                    }
                }

                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                buffer.write(documentRead);
                buffer.writeTo(out);

                //return response;
            } catch(Exception e) { 
                //System.out.println(e); 
                try {
                    String response = getErrorPage();

                    for (Byte b : response.getBytes()) {
                        out.write(b);
                    }
                    
                } catch(Exception ex) {}             
            }

        } else {                     
            try {
                String response = getErrorPage();

                for (Byte b : response.getBytes()) {
                    out.write(b);
                }
                
            } catch(Exception ex) {}
        }

    }

    // for content-type header
    private static String contentType(String file) {
        if (file.toLowerCase().endsWith(".html")) {
            return "text/html";
        } else if (file.toLowerCase().endsWith(".gif")) {
            return "image/gif";
        } else if (file.toLowerCase().endsWith(".jpg")) {
            return "image/jpeg";
        } else if (file.toLowerCase().endsWith(".css")) {
            return "text/css";
        } else return "application/octet-stream";
    }

    // returns 404 error page
    private String getErrorPage() throws Exception {
        String documentRead = new String(readFile("not-found.html", contentType("not-found.html")));
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();

        return "HTTP/1.1 404 Not Found\n" +
                "Connection close\n" +
                "Date: " + dateFormat.format(date) + "\n" +
                "Server: miServidor\n" +                    
                "Content-Length: " + documentRead.length() + "\n" +
                "Content-Type: text/html\n" +
                "\n"+
                documentRead;
    }

    private static byte[] readFile(String path, String contentType) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));        

        return encoded;
    }

}

public final class Webserver {
    public static void main(String args[]) throws Exception {        
        // port is 2407
        // server socket
        ServerSocket socket = new ServerSocket(2407);

        // wait for HTTP requests
        while(true) {
            // request socket
            Socket requestSocket = socket.accept();
            HttpRequest request = new HttpRequest(requestSocket); //HTTP object to handle request
            request.run();

            // thread
            Thread requestThread = new Thread(request);
            requestThread.start();
        }
    }
}