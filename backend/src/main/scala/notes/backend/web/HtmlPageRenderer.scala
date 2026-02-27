package notes.backend.web

import notes.shared.NoteSnapshot

object HtmlPageRenderer:
  def notePage(snapshot: NoteSnapshot): String =
    val escapedContent = escapeHtml(snapshot.content)
    s"""<!DOCTYPE html>
       |<html lang="en">
       |<head>
       |  <meta charset="UTF-8" />
       |  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
       |  <title>Note.ms</title>
       |  <style>
       |    * { box-sizing: border-box; }
       |    html, body {
       |      margin: 0;
       |      width: 100%;
       |      height: 100%;
       |      background: #f5f7fa;
       |      font-family: "SFMono-Regular", Consolas, "Liberation Mono", Menlo, monospace;
       |    }
       |    .page {
       |      width: 100%;
       |      height: 100%;
       |      padding: 16px;
       |    }
       |    .content {
       |      width: 100%;
       |      height: 100%;
       |      resize: none;
       |      border: 1px solid #d0d7de;
       |      border-radius: 8px;
       |      background: #ffffff;
       |      color: #24292f;
       |      padding: 16px;
       |      font-size: 15px;
       |      line-height: 1.6;
       |      outline: none;
       |    }
       |    .content:focus {
       |      border-color: #0969da;
       |      box-shadow: 0 0 0 2px rgba(9, 105, 218, 0.15);
       |    }
       |  </style>
       |</head>
       |<body>
       |  <main class="page">
       |    <textarea class="content">$escapedContent</textarea>
       |  </main>
       |  <script>
       |    window.__NOTE__ = {
       |      id: "${snapshot.id}",
       |      version: ${snapshot.version}
       |    };
       |  </script>
       |</body>
       |</html>
       |""".stripMargin

  private def escapeHtml(value: String): String =
    value
      .replace("&", "&amp;")
      .replace("<", "&lt;")
      .replace(">", "&gt;")
      .replace("\"", "&quot;")
      .replace("'", "&#39;")
