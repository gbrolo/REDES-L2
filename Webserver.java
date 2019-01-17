import java.io.*;
import java.net.*;
import java.util.*;
import java.net.InetAddress.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

final class HttpRequest implements Runnable {
    final static class METHOD {
        final static String GET = "GET";
        final static String HEAD = "HEAD";
    }

    final static String VERSION = "HTTP/1.0";
    final static String CRLF = "\r\n";

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

        String [] requestSplit = request.split(" ");
        String documentRequested = requestSplit[1].substring(1);

        System.out.println("\nDocument requested: \n" + documentRequested);

        String response = processFile(documentRequested);
        System.out.println("\nServer response: \n" + response);

        out.writeBytes(
            response
        );
        out.close();
        br.close();
        socket.close();
    }

    private String processFile(String documentRequested) {
        File f = new File(documentRequested);
        if(f.exists() && !f.isDirectory()) {
            //System.out.println("File found");

            String contentType = contentType(documentRequested);

            try {
                String documentRead = readFile(documentRequested, contentType);

                String response = "HTTP/1.1 200 OK\n" +
                "Connection close\n" +
                "Date: Thu, 06 Aug 1998 12:00:15 GMT\n" +
                "Server: brolius\n" +
                "Last-Modified: Mon, 22 Jun 1998\n" +
                "Content-Length: " + documentRead.length() + "\n" +
                "Content-Type: " + contentType + "\n" +
                "\n"+
                documentRead;

                return response;
            } catch(Exception e) { 
                //System.out.println(e); 
                try {
                    return getErrorPage();
                } catch(Exception ex) {}             
            }

        } else {
            //System.out.println("File NOT found");            
            try {
                return getErrorPage();
            } catch(Exception ex) {}
        }

        return "HTTP/1.1 500 Internal Server Error\n" +
                "Connection close\n" +
                "Date: Thu, 06 Aug 1998 12:00:15 GMT\n" +
                "Server: miServidor\n" +                    
                "Content-Length: " + "38" + "\n" +
                "Content-Type: text/html\n" +
                "\n"+
                "<HTML>500 Internal Server Error</HTML>"; 
    }

    private static String contentType(String file) {
        if (file.toLowerCase().endsWith(".html")) {
            return "text/html";
        } else if (file.toLowerCase().endsWith(".gif")) {
            return "image/gif";
        } else if (file.toLowerCase().endsWith(".jpg")) {
            return "image/jpeg";
        } else return "application/octet-stream";
    }

    private String getErrorPage() throws Exception {
        String documentRead = readFile("not-found.html", contentType("not-found.html"));

        return "HTTP/1.1 404 Not Found\n" +
                "Connection close\n" +
                "Date: Thu, 06 Aug 1998 12:00:15 GMT\n" +
                "Server: miServidor\n" +                    
                "Content-Length: " + documentRead.length() + "\n" +
                "Content-Type: text/html\n" +
                "\n"+
                documentRead;
    }

    private static String readFile(String path, String contentType) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));        

        return new String(encoded);
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