// Any questions should be directed to Peter Darveau, P.Eng. - Hexagon Technology Inc.
// This file is intended for Proof of Concept only and should not be used in production environment
// Tested on Linux 4.14.79

import java.net.*;
import java.io.*;
import java.util.*;


class Main
{
  public static void main (String [] args)
  {
    Server s = new Server(3001);
    s.run();
  }
}


class Server
{
  HttpManager http;

  ServerSocket ss;
  BufferedReader in;
  PrintWriter out;

  public Server(int port) {
    try {
      ss = new ServerSocket(port);
    } catch(Exception e) {
      System.err.println("There was a problem listening on port " + port);
    }
  }

  public void run() {
    http = new HttpManager();

    while(true) {
      try {
        // Block until HTTP request
        Socket client = ss.accept();

        in = new BufferedReader(new InputStreamReader(client.getInputStream()));
        out = new PrintWriter(client.getOutputStream(), true);

	String request;
        String headers = "";
        String line;
        boolean hasContent = false;

        String content = "";
        int contentLen = 0;

        // Read request
        request = in.readLine();


        // Read the headers and body
        while((line = in.readLine()) != null) {
          if(line.length() == 0) {
            break;
          }
          if(line.startsWith("Content-Length:")) {
            StringTokenizer st = new StringTokenizer(line, ": ");
            st.nextToken();
            contentLen = Integer.parseInt(st.nextToken());
            hasContent = true;
          }
          headers += line + "\r\n";
        }

        if(hasContent) {
          StringBuilder ct = new StringBuilder();
          int c = 0;

          for(int i = 0; i < contentLen; i++) {
            c = in.read();
            ct.append((char) c);
          }
          content = ct.toString();
        }

        // Process the request appropriately
        processRequest(request, headers, content);

        // We clean up and then loop back to listen on the next iteration
        in.close();
        out.flush();
        out.close();
        client.close();

      } catch(NumberFormatException e) {
        System.err.println("Content length field number improper: " + e);
      } catch(Exception e) {
        System.err.println("Oops! Something went wrong: " + e);
      }
    }
  }

  public void processRequest(String request, String headers, String content) {
    StringTokenizer st = new StringTokenizer(request);
    // What type of request is it?
    if(st.countTokens() < 3) {
      System.err.println("ERROR: There was a problem with the format of this request");
      errorResponse(404, "Too many tokens");
      return;
    }

    String cmd = st.nextToken();
    String url = st.nextToken();

    String response = null;

    try {
      // Recognise http request and make api call
      if(cmd.equals("GET")) {
        response = http.get(url, content);
      } else if(cmd.equals("PUT")) {
        http.put(url, content);
        response = "";
      } else if(cmd.equals("POST")) {
        response = http.post(url, content);
      } else if(cmd.equals("DELETE")) {
        http.delete(url);
        response = "";
      }

      successResponse(response);
    } catch(Exception e) {
      errorResponse(404, e.toString());
    }

  }

  // HTTP success response for a request
  public void successResponse(String body) {
    out.print("HTTP/1.1 200\r\n"); // Version & status code
    out.print("Content-Type: text/plain\r\n"); // The type of data
    out.print("Connection: close\r\n\r\n"); // Will close stream
    out.println(body);
  }

  // HTTP error response for a request
  public void errorResponse(int code, String body) {
    out.print("HTTP/1.1 " + code + "\r\n"); // Version & status code
    out.print("Content-Type: text/plain\r\n"); // The type of data
    out.print("Connection: close\r\n\r\n"); // Will close stream
    out.println(body);
  }
}

