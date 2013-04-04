// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library leap_server;

import 'dart:io';

class Conversation {
  HttpRequest request;
  HttpResponse response;

  static const String CONTENT_TYPE = HttpHeaders.CONTENT_TYPE;

  static const String LEAP_LANDING_PAGE =
      'sdk/lib/_internal/compiler/samples/leap/index.html';

  static String landingPage = LEAP_LANDING_PAGE;

  Conversation(this.request, this.response);

  onClosed(_) {
    if (response.statusCode == HttpStatus.OK) return;
    print('Request for ${request.uri} ${response.statusCode}');
  }

  setUpErrorHandler() {
    // TODO(ahe): When https://codereview.chromium.org/13467004/ lands
    // apply the following changes:
    // 1. Inline this method in [handle].
    // 2. Delete this method.
    // 3. Call response.close() instead of calling [done].
    // 4. Delete this [done].
    response.done
      ..then(onClosed)
      ..catchError(onError);
  }

  done() {
    setUpErrorHandler();
    response.close();
  }

  notFound(path) {
    response.statusCode = HttpStatus.NOT_FOUND;
    response.write(htmlInfo('Not Found',
                            'The file "$path" could not be found.'));
    done();
  }

  redirect(String location) {
    response.statusCode = HttpStatus.FOUND;
    response.headers.add(HttpHeaders.LOCATION, location);
    done();
  }

  handle() {
    String path = request.uri.path;
    if (path == '/') return redirect('/$landingPage');
    if (path == '/favicon.ico') {
      path = '/sdk/lib/_internal/dartdoc/static/favicon.ico';
    }
    if (path.contains('..') || path.contains('%')) return notFound(path);
    var f = new File("./$path");
    f.exists().then((bool exists) {
      if (!exists) return notFound(path);
      if (path.endsWith('.html')) {
        response.headers.set(CONTENT_TYPE, 'text/html');
      } else if (path.endsWith('.dart')) {
        response.headers.set(CONTENT_TYPE, 'application/dart');
      } else if (path.endsWith('.js')) {
        response.headers.set(CONTENT_TYPE, 'application/javascript');
      } else if (path.endsWith('.ico')) {
        response.headers.set(CONTENT_TYPE, 'image/x-icon');
      }
      setUpErrorHandler();
      f.openRead().pipe(response);
    });
  }

  static onRequest(HttpRequest request) {
    new Conversation(request, request.response).handle();
  }

  static onError(AsyncError e) {
    if (e.error is HttpParserException) {
      print('Error: ${e.error.message}');
    } else {
      print('Error: ${e.error}');
    }
  }

  String htmlInfo(String title, String text) {
    return """
<!DOCTYPE html>
<html lang='en'>
<head>
<title>$title</title>
</head>
<body>
<h1>$title</h1>
<p>$text</p>
</body>
</html>
""";
  }
}

main() {
  List<String> arguments = new Options().arguments;
  if (arguments.length > 0) {
    Conversation.landingPage = arguments[0];
  }
  var host = '127.0.0.1';
  HttpServer.bind(host, 0).then((HttpServer server) {
    print('HTTP server started on http://$host:${server.port}/');
    server.listen(Conversation.onRequest, onError: Conversation.onError);
  }).catchError((e) {
    print("HttpServer.bind error: ${e.error}");
    exit(1);
  });
}
