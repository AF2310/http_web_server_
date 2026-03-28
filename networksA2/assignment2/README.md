# HTTP server guide

### Compile the server

```bash
cd c:\Users\moner\networksA2\assignment2
javac WebServer.java
```

### Run the server

```bash
java WebServer port public-directory
```

Example:
```bash
java WebServer 8008 public
```

Result should be:
```
Server started on port 8008
```

### Navogate the server

Open browser and naviagte to localhost page with the selected port:
```
http://localhost:8008/
```

### Pages

You can navigate HTML pages by clicking  http://localhost:8008/ , http://localhost:8008/index.html for  main page, http://localhost:8008/named.html for named page, http://localhost:8008/login.html for login page, and http://localhost:8008/upload.html for upload page.

### Images

Image pages can be navigated by lciking locahost:port/name.png for example  http://localhost:8008/clown.png, http://localhost:8008/world.png,http://localhost:8008/a/b/bee.png they are stored in the root and other directories.

### Accessing directories

When you type a directory path that has at the end a slash, the server gives you the index.html.You can see http://localhost:8008/a/ for public/a/index.html, http://localhost:8008/a/b/ for public/a/b/index.html, and http://localhost:8008/a/b/c/ for public/a/b/c/index.html.Also you can check subdirectories or nested directories  http://localhost:8008/a/a.html, http://localhost:8008/a/b/b.html, and http://localhost:8008/a/b/c/c.html.



### Testing through browsing

Just navigate the urls .Try to get error code responces see if you get the correct error code responces.
See if you can navigate outside the public directory or folder you shouldnt be able to.
Try to redirects as well.
```
http://localhost:8008/redirect
should take you to lnu page for example
```



### Python script testing

Run the Python test script:

```bash
python testa2u1.py
```

Output should be okay for each page:
```
OK:MAIN INDEX PAGE
ECT
```



## File types

The server detects and supports files such as : HTML files, CSS , JS files, JPEG images , PNG images .


