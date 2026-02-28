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
       |    .status {
       |      position: fixed;
       |      right: 24px;
       |      bottom: 20px;
       |      padding: 6px 10px;
       |      border-radius: 6px;
       |      font-size: 12px;
       |      color: #57606a;
       |      background: rgba(255, 255, 255, 0.9);
       |      border: 1px solid #d0d7de;
       |    }
       |  </style>
       |</head>
       |<body>
       |  <main class="page">
       |    <textarea class="content">$escapedContent</textarea>
       |  </main>
       |  <div class="status" id="save-status">Saved</div>
       |  <script>
       |    window.__NOTE__ = {
       |      id: "${snapshot.id}",
       |      version: ${snapshot.version}
       |    };
       |
       |    (function() {
       |      const textarea = document.querySelector(".content");
       |      const status = document.getElementById("save-status");
       |      const noteId = window.__NOTE__.id;
       |      let version = window.__NOTE__.version || 0;
       |      let lastSavedContent = textarea.value;
       |      let debounceTimer = null;
       |      let saving = false;
       |      let pendingDirty = false;
       |      let conflict = false;
       |
       |      const setStatus = (text) => { status.textContent = text; };
       |
       |      const buildBody = () => {
       |        const params = new URLSearchParams();
       |        params.set("t", textarea.value);
       |        params.set("version", String(version));
       |        return params;
       |      };
       |
       |      const saveNow = async (force) => {
       |        if (conflict && !force) return;
       |        if (!force && textarea.value === lastSavedContent) return;
       |        if (saving) {
       |          pendingDirty = true;
       |          return;
       |        }
       |
       |        saving = true;
       |        setStatus("Saving...");
       |        try {
       |          const response = await fetch("/" + noteId, {
       |            method: "POST",
       |            headers: { "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8" },
       |            body: buildBody().toString(),
       |            credentials: "same-origin",
       |            keepalive: !!force
       |          });
       |
       |          if (response.status === 200) {
       |            const payload = await response.json();
       |            version = payload.version;
       |            lastSavedContent = textarea.value;
       |            conflict = false;
       |            setStatus("Saved");
       |          } else if (response.status === 409) {
       |            conflict = true;
       |            setStatus("Conflict. Refresh required");
       |          } else if (response.status === 413) {
       |            setStatus("Content too large");
       |          } else {
       |            setStatus("Save failed");
       |          }
       |        } catch (_) {
       |          setStatus("Save failed");
       |        } finally {
       |          saving = false;
       |          if (pendingDirty && !conflict) {
       |            pendingDirty = false;
       |            void saveNow(false);
       |          }
       |        }
       |      };
       |
       |      const scheduleSave = () => {
       |        if (conflict) return;
       |        if (debounceTimer) clearTimeout(debounceTimer);
       |        setStatus("Dirty");
       |        debounceTimer = setTimeout(() => { void saveNow(false); }, 800);
       |      };
       |
       |      textarea.addEventListener("input", scheduleSave);
       |      textarea.addEventListener("blur", () => { void saveNow(true); });
       |
       |      document.addEventListener("visibilitychange", () => {
       |        if (document.visibilityState === "hidden") void saveNow(true);
       |      });
       |
       |      window.addEventListener("beforeunload", () => {
       |        if (textarea.value === lastSavedContent || conflict) return;
       |        const params = buildBody();
       |        try {
       |          navigator.sendBeacon("/" + noteId, params);
       |        } catch (_) {}
       |      });
       |    })();
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
