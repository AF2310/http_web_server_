import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// HTTP server accepts connections and  each request to a thread.
public class WebServer {
  private static final int THREAD_SIZE = 10;
  private static Path publicDir;

  // starts server socket and thread pool, accepts incoming connections and submit them to the thread pool.
  // Each request is handled by the request handler class
  public static void main(String[] args){
    if (args.length != 2) {
      System.err.println(" java WebServer <port>");
      System.exit(1);

    }

    int port;
    try {
      port = Integer.parseInt(args[0]);
    } catch (NumberFormatException e) {
      System.err.println("Invalid port number: " + args[0]);
      System.exit(1);
      return;
    }

    publicDir = Paths.get(args[1]);
    try {
      publicDir = Paths.get(args[1]).toAbsolutePath().normalize();
      if (!Files.isDirectory(publicDir)) {
        System.err.println("Public directory does not exist or is not a directory: " + publicDir);
        return;
      }
    } catch (InvalidPathException e) {
      System.err.println("Invalid public directory path: " + args[1]);
      return;
    }

    ExecutorService threadPool = Executors.newFixedThreadPool(THREAD_SIZE);

    try (ServerSocket serverSocket = new ServerSocket(port)) {
      System.out.println("Server started on port " + port);
      while (true) {
        Socket clientSocket = serverSocket.accept();
        System.out.println("New connection from " + clientSocket.getInetAddress());
        threadPool.execute(new RequestHandler(clientSocket));

      }

    } catch (IOException e) {
      System.err.println("Could not start server: " + e.getMessage());
      e.printStackTrace();
    } catch (Exception e) {
      System.err.println("Unexpected error: " + e.getMessage());
      e.printStackTrace();
    } finally {
      threadPool.shutdown();
    }


  }

  // request handler reads requests parses them and sends responces
  // also handles errors creates redirections and sends back the correct error code responce
  private static class RequestHandler implements Runnable {
    private Socket clientSocket;

    public RequestHandler(Socket clientSocket) {
      this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
      Socket socket = this.clientSocket;
      BufferedReader in = null;
      OutputStream out = null;
      try {
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = socket.getOutputStream();
        String requestLine = in.readLine();
        if (requestLine == null || requestLine.isEmpty()) {
          return;
        }

        System.out.println("Request: " + requestLine);
        

        
        String[] parts = requestLine.split(" ");

        if (parts.length != 3) {
          sendError(out, 400, "Bad Request");
          return;
        }


        String method = parts[0];
        String rawPath = parts[1];
        String httpVersion = parts[2];

        /*if (!method.equals("GET")) {
          String statusLine = "HTTP/1.1 200 OK\r\n";
          String contentTypeHeader = "Content-Type: text/html\r\n";
          String responseBody = "<html><body><h1>Hello from server!</h1></body></html>";
          String contentLengthHeader = "Content-Length: " + responseBody.length() + "\r\n";
          String blankLine = "\r\n";
          String response = statusLine + contentTypeHeader + contentLengthHeader + blankLine + responseBody;
          out.write(response.getBytes());
          out.flush();
          System.out.println("fixed response");

          
        }*/
       /* if the request is poorly constructed the server send back error 400 */
        if (!httpVersion.equals("HTTP/1.1")) {
            sendError(out, 400, "Bad Request");
            return;
        }

        /*accept only if it's a get or post method*/
        if (!method.equals("GET") && !method.equals("POST")) {
            sendError(out, 405, "Method Not Allowed");
            return;
        }

        if (method.equals("POST")) {
            handlePost(socket, in, out, rawPath);
            return;
        }

        String decodedPath;

        try {
          decodedPath = URLDecoder.decode(rawPath, "UTF-8");
        } catch (UnsupportedEncodingException e) {
          sendError(out, 400, "Bad Request");
          return;
        }
        /* redirect the page to an other one call sendRedirect */
        if (decodedPath.equals("/redirect")) {
            sendRedirect(out, "https://www.lnu.se");
            return;
        }

        int queryIndex = decodedPath.indexOf('?');

        if (queryIndex != -1) {
          decodedPath = decodedPath.substring(0, queryIndex);
        }

        Path requestedPath;
        try {
          requestedPath = publicDir.resolve(decodedPath.substring(1)).normalize();
        } catch (Exception e) {
          sendError(out, 400, "Bad Request");
          return;
        }

        if (!requestedPath.startsWith(publicDir)) {
          sendError(out, 403, "Forbidden");
          return;
        }

        if (Files.isDirectory(requestedPath)) {
          requestedPath = requestedPath.resolve("index.html");
        }

        if (!Files.exists(requestedPath) || Files.isHidden(requestedPath) || !Files.isReadable(requestedPath)) {
          sendError(out, 404, "Not Found");
          return;
        }

        String contentType = getContentType(requestedPath);
        long contentLength = Files.size(requestedPath);

        out.write("HTTP/1.1 200 OK\r\n".getBytes());
        out.write(("Content-Type: " + contentType + "\r\n").getBytes());
        out.write(("Content-Length: " + contentLength + "\r\n").getBytes());
        out.write("Connection: close\r\n".getBytes());
        out.write("\r\n".getBytes());

        Files.copy(requestedPath, out);
        out.flush();
        System.out.println("Served: " + requestedPath + " (" + contentType + ", " + contentLength + " bytes)");


    



      
      } catch (IOException e) {
        System.err.println("Error: " + e.getMessage());
      } finally {
        try {
          if (in != null) in.close();
          
        } catch (IOException e) {
        }

        try {
          if (out != null) out.close();
          
        } catch (IOException e) {
        }

        try {
          if (socket != null && !socket.isClosed()) socket.close();
          
        } catch (IOException e) {
        }
      }
    }
  }
  // function that returns  the content type of the file based on the extension
  private static String getContentType(Path path) {
    String fileName = path.getFileName().toString().toLowerCase();
    if (fileName.endsWith(".html") || fileName.endsWith(".htm")) {
      return "text/html";
    } else if (fileName.endsWith(".css")) {
      return "text/css";
    } else if (fileName.endsWith(".js")) {
      return "application/javascript";
    } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
      return "image/jpeg";
    } else if (fileName.endsWith(".png")) {
      return "image/png";
    } else if (fileName.endsWith(".gif")) {
      return "image/gif";
    } else if (fileName.endsWith(".txt")) {
      return "text/plain";
    } else {
      return "application/octet-stream";
    }
  }
  /* send error response with status code and message (400, 403, 404, 405, 500) */
  private static void sendError(OutputStream out, int statusCode, String message) throws IOException {
    String statusLine;
    String reasonPhrase;
    switch (statusCode) {
      case 400:
        statusLine = "HTTP/1.1 400 Bad Request\r\n";
        reasonPhrase = "400 Bad Request";
        break;
      case 403:
        statusLine = "HTTP/1.1 403 Forbidden\r\n";
        reasonPhrase = "403 Forbidden";
        break;
      case 404:
        statusLine = "HTTP/1.1 404 Not Found\r\n";
        reasonPhrase = "404 Not Found";
        break;
      case 405:
        statusLine = "HTTP/1.1 405 Method Not Allowed\r\n";
        reasonPhrase = "405 Method Not Allowed";
        break;
      default:
        statusLine = "HTTP/1.1 500 Internal Server Error\r\n";
        reasonPhrase = "500 Internal Server Error";
    }

    String responseBody = "<html><body><h1>" + reasonPhrase + "</h1></body></html>";

    String response = statusLine +
    "Content-Type: text/html\r\n" +
    "Content-Length: " + responseBody.length() + "\r\n" +
    "Connection: close\r\n" +
    "\r\n" +
    responseBody;
    out.write(response.getBytes());
    out.flush();
    System.err.println("Error " + statusCode + ": " + message);
  }


  /* redirect the page to an other one, call function sendRedirect, code 302 */    
  private static void sendRedirect(OutputStream out, String location) throws IOException {
      String response =
          "HTTP/1.1 302 Found\r\n" +
          "Location: " + location + "\r\n" +
          "Content-Length: 0\r\n" +
          "Connection: close\r\n" +
          "\r\n";
      out.write(response.getBytes());
      out.flush();
      System.out.println("Redirecting to: " + location);
  }

  /* read the request POST and select either the function login handling or upload handling */    
  private static void handlePost(Socket socket, BufferedReader in, OutputStream out, String path) throws IOException {

      /* Read headers to collect Content-Length */ 
      int contentLength = 0;
      String line;
      while (!(line = in.readLine()).isEmpty()) {
          if (line.startsWith("Content-Length:")) {
              contentLength = Integer.parseInt(line.split(":")[1].trim());
          }
      }
      /* check what's the request (login or upload) and send it to the right function */ 
      if (path.equals("/login")) {
          char[] body = new char[contentLength];
          in.read(body, 0, contentLength);
          String requestBody = new String(body);
          handleLogin(out, requestBody);

      } else if (path.equals("/upload")) {
          byte[] body = new byte[contentLength];
          socket.getInputStream().read(body, 0, contentLength);
          handleUpload(out, body);

      } else {
          sendError(out, 404, "Not Found");
      }
  }

  /* Process the login request, extract the username and password form the entry and compare with the stored credentials and return 200 ok or 401 unauthorized */
  private static void handleLogin(OutputStream out, String body) throws IOException {

    /* how looks the body = username=admin&password=1234, need to extract username and password */
    String[] params = body.split("&");
    String username = params[0].split("=")[1];
    String password = params[1].split("=")[1];

    username = URLDecoder.decode(username, "UTF-8");
    password = URLDecoder.decode(password, "UTF-8");
    /* Read credentials.json */
    String[] credentials = readCredentials();
    String storedUser = credentials[0];
    String storedPass = credentials[1];
    /* test if it's correct */
    if (username.equals(storedUser) && password.equals(storedPass)) {
        send200(out, "Login successful");
    } else {
        send401(out);
    }
  }
  /* function who extract the credentials from the json file and return them */
  private static String[] readCredentials() throws IOException {

      Path credPath = Paths.get("credentials.json");
      String json = Files.readString(credPath);

      String username = json.split("\"username\"")[1]
                            .split(":")[1]
                            .split(",")[0]
                            .replace("\"", "")
                            .trim();

      String password = json.split("\"password\"")[1]
                            .split(":")[1]
                            .split("}")[0]
                            .replace("\"", "")
                            .trim();

      return new String[]{username, password};
  }

  /* handle the upload request, extract only png file and store it in the repository pubilc/uploads */
  private static void handleUpload(OutputStream out, byte[] body) throws IOException {

      String bodyStr = new String(body);

      int fileStart = bodyStr.indexOf("\r\n\r\n") + 4;
      int fileEnd = bodyStr.lastIndexOf("\r\n------");

      if (fileStart < 0 || fileEnd < 0) {
          sendError(out, 400, "Bad Upload");
          return;
      }

      byte[] fileBytes = new byte[fileEnd - fileStart];
      System.arraycopy(body, fileStart, fileBytes, 0, fileBytes.length);

      Path uploadPath = publicDir.resolve("uploads/uploaded.png");

      Files.write(uploadPath, fileBytes);

      send200(out, "Image uploaded successfully!");
  }

  /* send a code 200 ok html response */
  private static void send200(OutputStream out, String message) throws IOException {
      String body = "<html><body><h1>" + message + "</h1></body></html>";

      String response =
          "HTTP/1.1 200 OK\r\n" +
          "Content-Type: text/html\r\n" +
          "Content-Length: " + body.length() + "\r\n" +
          "Connection: close\r\n\r\n" +
          body;

      out.write(response.getBytes());
      out.flush();
  }
  /* send a code 401 unauthorized html response if unauthorized */
  private static void send401(OutputStream out) throws IOException {
      String body = "<html><body><h1>401 Unauthorized</h1></body></html>";

      String response =
          "HTTP/1.1 401 Unauthorized\r\n" +
          "Content-Type: text/html\r\n" +
          "Content-Length: " + body.length() + "\r\n" +
          "Connection: close\r\n\r\n" +
          body;

      out.write(response.getBytes());
      out.flush();
  }
}
