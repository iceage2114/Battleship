import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Starts a local HTTP server so you can play Battleship in any browser.
 *
 * Compile:  javac *.java
 * Run:      java GameServer
 * Open:     http://localhost:8080
 */
public class GameServer {

    private static volatile GameController game;
    private static volatile Direction currentDir = Direction.EAST;
    private static final List<String> gameLog = Collections.synchronizedList(new ArrayList<>());
    private static final int PORT = 8080;

    // ── Entry point ──────────────────────────────────────────────────────────

    public static void main(String[] args) throws IOException {
        resetGame();
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/", GameServer::handle);
        server.setExecutor(null);
        server.start();
        System.out.println("Battleship is running at http://localhost:" + PORT);
        System.out.println("Open your browser to that address to play.");
        System.out.println("Press Ctrl+C to stop the server.");
    }

    // ── Game state ───────────────────────────────────────────────────────────

    private static synchronized void resetGame() {
        gameLog.clear();
        currentDir = Direction.EAST;

        // This observer captures messages from ships (hit/sunk) via ShipFactory,
        // and from GameController itself (attack results) once addObserver() is called.
        GameObserver logger = msg -> {
            gameLog.add(0, msg);
            if (gameLog.size() > 40) gameLog.remove(gameLog.size() - 1);
        };

        game = new GameController(logger);
        game.addObserver(logger);                     // attach to GameController's own list
        game.setGameOverHandler((title, msg) -> {});  // suppress the Swing dialog

        // Seed the log with the opening prompt (constructor fires it before our
        // observer is attached, so we add it manually here).
        if (game.isPlacementPhase()) {
            ShipFactory.ShipType first = game.getCurrentShipTypeToPlace();
            gameLog.add(0, "New game! Place your " + first.name().charAt(0)
                    + first.name().substring(1).toLowerCase()
                    + " (length " + first.getLength() + ")");
        }
    }

    // ── HTTP handler ─────────────────────────────────────────────────────────

    private static void handle(HttpExchange ex) throws IOException {
        try {
            if ("POST".equalsIgnoreCase(ex.getRequestMethod())) {
                String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                handleAction(parseParams(body));
                // Post-Redirect-Get: prevents duplicate actions on browser refresh
                ex.getResponseHeaders().add("Location", "/");
                ex.sendResponseHeaders(303, -1);
                ex.getResponseBody().close();
            } else {
                byte[] html = buildPage().getBytes(StandardCharsets.UTF_8);
                ex.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
                ex.sendResponseHeaders(200, html.length);
                ex.getResponseBody().write(html);
                ex.getResponseBody().close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            byte[] err = ("<h1>Server Error</h1><pre>" + e.getMessage() + "</pre>")
                    .getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
            ex.sendResponseHeaders(500, err.length);
            ex.getResponseBody().write(err);
            ex.getResponseBody().close();
        }
    }

    // ── Action dispatch ──────────────────────────────────────────────────────

    private static synchronized void handleAction(Map<String, String> p) {
        switch (p.getOrDefault("action", "")) {

            case "new" -> resetGame();

            case "dir" -> {
                try { currentDir = Direction.valueOf(p.get("dir")); }
                catch (Exception ignored) {}
            }

            case "place" -> {
                try {
                    int x = Integer.parseInt(p.get("x"));
                    int y = Integer.parseInt(p.get("y"));
                    game.tryPlacePlayerShip(new Position(x, y), currentDir);
                } catch (Exception e) {
                    gameLog.add(0, "Placement error: " + e.getMessage());
                }
            }

            case "attack" -> {
                try {
                    int x = Integer.parseInt(p.get("x"));
                    int y = Integer.parseInt(p.get("y"));
                    if (!game.isGameOver() && !game.isPlacementPhase()) {
                        game.playerAttack(new Position(x, y));
                        if (!game.isGameOver()) {
                            game.cpuAttack();
                        }
                    }
                } catch (Exception e) {
                    gameLog.add(0, "Attack error: " + e.getMessage());
                }
            }
        }
    }

    // ── Form parsing ─────────────────────────────────────────────────────────

    private static Map<String, String> parseParams(String raw) {
        Map<String, String> m = new LinkedHashMap<>();
        if (raw == null || raw.isEmpty()) return m;
        for (String pair : raw.split("&")) {
            String[] kv = pair.split("=", 2);
            try {
                String key = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
                String val = kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "";
                m.put(key, val);
            } catch (Exception ignored) {}
        }
        return m;
    }

    // ── HTML page builder ────────────────────────────────────────────────────

    private static synchronized String buildPage() {
        GameBoard pb  = game.getPlayerBoard();
        GameBoard eb  = game.getEnemyBoard();
        boolean place = game.isPlacementPhase();
        boolean over  = game.isGameOver();

        StringBuilder sb = new StringBuilder();

        sb.append("""
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="UTF-8">
              <title>Battleship</title>
              <style>
                *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }
                body {
                  font-family: 'Segoe UI', Arial, sans-serif;
                  background: #0a1628; color: #e0e8f0;
                  min-height: 100vh; padding: 20px;
                  display: flex; flex-direction: column; align-items: center;
                }
                h1 {
                  color: #4fc3f7; letter-spacing: 4px; text-transform: uppercase;
                  text-shadow: 0 0 20px rgba(79,195,247,0.4);
                  margin-bottom: 4px; font-size: 2rem;
                }
                .sub { color: #546e7a; letter-spacing: 2px; font-size: 0.85rem; margin-bottom: 16px; }
                #status {
                  background: #132035; border: 1px solid #1e3a5f; border-radius: 8px;
                  padding: 10px 24px; margin-bottom: 14px; color: #80cbc4;
                  text-align: center; max-width: 800px; width: 100%;
                }
                .controls {
                  display: flex; gap: 10px; align-items: center;
                  flex-wrap: wrap; justify-content: center; margin-bottom: 14px;
                }
                .dir-bar {
                  display: flex; gap: 6px; align-items: center;
                  background: #132035; border: 1px solid #1e3a5f;
                  border-radius: 8px; padding: 6px 12px;
                }
                .dir-bar span { color: #546e7a; font-size: 0.85rem; margin-right: 4px; }
                /* inline form so buttons flow naturally */
                form.inline { display: inline; }
                .dir-btn {
                  background: #263238; color: #cfd8dc; border: none;
                  border-radius: 5px; padding: 5px 12px; font-size: 0.85rem; cursor: pointer;
                }
                .dir-btn:hover   { background: #37474f; }
                .dir-btn.active  { background: #1565c0; color: #e3f2fd; }
                .dir-btn.active:hover { background: #1976d2; }
                .btn {
                  background: #1565c0; color: #e3f2fd; border: none;
                  border-radius: 6px; padding: 8px 20px; font-size: 0.9rem; cursor: pointer;
                }
                .btn:hover  { background: #1976d2; }
                .btn-new    { background: #1b5e20; }
                .btn-new:hover { background: #2e7d32; }
                .legend {
                  display: flex; gap: 14px; flex-wrap: wrap;
                  justify-content: center; margin-bottom: 12px;
                  font-size: 0.8rem; color: #78909c;
                }
                .legend span { display: flex; align-items: center; gap: 5px; }
                .dot { width: 13px; height: 13px; border-radius: 3px; display: inline-block; }
                .boards {
                  display: flex; gap: 40px; flex-wrap: wrap;
                  justify-content: center; margin-bottom: 16px;
                }
                .board-wrap { display: flex; flex-direction: column; align-items: center; gap: 8px; }
                .board-title { letter-spacing: 2px; text-transform: uppercase; color: #90a4ae; font-size: 0.9rem; }
                /* CSS Grid board — each cell is a direct child so forms are valid here */
                .board { display: grid; grid-template-columns: 28px repeat(10, 36px); gap: 2px; }
                .lbl {
                  width: 28px; height: 28px; display: flex;
                  align-items: center; justify-content: center;
                  color: #546e7a; font-size: 0.72rem; font-weight: bold;
                }
                /* shared size for divs and forms */
                .cell, .board > form {
                  width: 36px; height: 36px; border-radius: 4px;
                  display: flex; align-items: center; justify-content: center;
                }
                .cell { background: #0d2137; border: 1px solid #1a3a5c; font-size: 1rem; font-weight: bold; }
                .ship { background: #1e3d5c; border-color: #2a5280; }
                .hit  { background: #b71c1c; border-color: #e53935; color: #ffcdd2; }
                .miss { background: #1a3a5c; border-color: #2196f3; color: #4fc3f7; font-size: 1.4rem; }
                .sunk { background: #4a1010; border-color: #c62828; color: #ef9a9a; }
                /* Clickable cell = a <form> containing a <button> that fills the cell */
                .board > form { overflow: hidden; }
                .board > form button {
                  width: 100%; height: 100%; background: #0d2137; border: 1px solid #1a3a5c;
                  border-radius: 4px; cursor: crosshair;
                  /* hide the text visually while keeping it for screen readers */
                  color: transparent; font-size: 0;
                }
                .board > form.attack button:hover { background: #1a4a7a; border-color: #42a5f5; }
                .board > form.place  button:hover { background: #1b5e20; border-color: #43a047; }
                #log {
                  background: #0b1e2d; border: 1px solid #1e3a5f; border-radius: 8px;
                  padding: 10px 14px; width: 100%; max-width: 800px; max-height: 160px;
                  overflow-y: auto; font-size: 0.82rem; color: #78909c;
                  font-family: Consolas, monospace;
                }
                #log p { margin: 2px 0; padding: 2px 0; border-bottom: 1px solid #0f2236; }
              </style>
            </head>
            <body>
            <h1>&#9875; Battleship</h1>
            <p class="sub">Naval Combat Simulator</p>
            """);

        // ── Status bar ──
        sb.append("<div id='status'>");
        if (place) {
            ShipFactory.ShipType next = game.getCurrentShipTypeToPlace();
            String name = next.name().charAt(0) + next.name().substring(1).toLowerCase();
            sb.append("Placement Phase &mdash; Place your <strong>").append(name)
              .append("</strong> (length ").append(next.getLength())
              .append(") &nbsp;|&nbsp; Direction: <strong>").append(currentDir).append("</strong>");
        } else if (over) {
            sb.append("<strong>Game Over</strong> &mdash; Click &ldquo;New Game&rdquo; to play again.");
        } else {
            sb.append("Attack Phase &mdash; Click any cell on the <strong>Enemy Waters</strong> to fire!");
        }
        sb.append("</div>");

        // ── Controls bar ──
        sb.append("<div class='controls'>");

        if (place) {
            sb.append("<div class='dir-bar'><span>Direction:</span>");
            for (Direction d : Direction.values()) {
                String active = d == currentDir ? " active" : "";
                sb.append("<form method='post' action='/' class='inline'>")
                  .append("<input type='hidden' name='action' value='dir'>")
                  .append("<input type='hidden' name='dir' value='").append(d.name()).append("'>")
                  .append("<button type='submit' class='dir-btn").append(active).append("'>")
                  .append(d.name()).append("</button></form>");
            }
            sb.append("</div>");
        }

        sb.append("<form method='post' action='/' class='inline'>")
          .append("<input type='hidden' name='action' value='new'>")
          .append("<button type='submit' class='btn btn-new'>&#8635; New Game</button></form>");
        sb.append("</div>");

        // ── Legend ──
        sb.append("<div class='legend'>")
          .append("<span><span class='dot' style='background:#1e3d5c;border:1px solid #2a5280'></span>Your Ship</span>")
          .append("<span><span class='dot' style='background:#b71c1c;border:1px solid #e53935'></span>Hit</span>")
          .append("<span><span class='dot' style='background:#4a1010;border:1px solid #c62828'></span>Sunk</span>")
          .append("<span><span class='dot' style='background:#1a3a5c;border:1px solid #2196f3'></span>Miss</span>")
          .append("</div>");

        // ── Boards ──
        sb.append("<div class='boards'>")
          .append(renderBoard("Your Fleet",   pb, /*hideShips*/false, /*allowPlace*/place,  /*allowAttack*/false))
          .append(renderBoard("Enemy Waters", eb, /*hideShips*/true,  /*allowPlace*/false,  /*allowAttack*/!place && !over))
          .append("</div>");

        // ── Game log ──
        sb.append("<div id='log'>");
        synchronized (gameLog) {
            for (String msg : gameLog) {
                sb.append("<p>").append(escHtml(msg)).append("</p>");
            }
        }
        sb.append("</div></body></html>");

        return sb.toString();
    }

    // ── Board grid renderer ──────────────────────────────────────────────────

    /**
     * Renders a 10x10 board as a CSS Grid. Clickable cells are <form> elements
     * (direct grid children) so they are valid HTML without JS.
     *
     * cells[x][y] — x = column (0=left), y = row (0=top), matching GameBoard's layout.
     */
    private static String renderBoard(String title, GameBoard board,
                                      boolean hideShips, boolean allowPlace, boolean allowAttack) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div class='board-wrap'>")
          .append("<div class='board-title'>").append(title).append("</div>")
          .append("<div class='board'>");

        // Corner + column headers (A–J)
        sb.append("<div class='lbl'></div>");
        for (int x = 0; x < 10; x++) {
            sb.append("<div class='lbl'>").append((char)('A' + x)).append("</div>");
        }

        for (int y = 0; y < 10; y++) {
            // Row header
            sb.append("<div class='lbl'>").append(y + 1).append("</div>");

            for (int x = 0; x < 10; x++) {
                Cell cell = board.cells[x][y];
                boolean isHit  = cell.isHit();
                boolean hasShip = cell.hasShip();
                boolean isSunk  = hasShip && cell.getShip().isSunk();

                String coord = String.valueOf((char)('A' + x)) + (y + 1);

                if (isHit) {
                    if (hasShip) {
                        sb.append("<div class='cell ").append(isSunk ? "sunk" : "hit").append("'>&#x2715;</div>");
                    } else {
                        sb.append("<div class='cell miss'>&middot;</div>");
                    }
                } else if (!hideShips && hasShip) {
                    sb.append("<div class='cell ship'></div>");
                } else if (allowPlace) {
                    appendClickableCell(sb, "place", x, y, coord, "place");
                } else if (allowAttack) {
                    appendClickableCell(sb, "attack", x, y, coord, "attack");
                } else {
                    sb.append("<div class='cell'></div>");
                }
            }
        }

        sb.append("</div></div>");
        return sb.toString();
    }

    private static void appendClickableCell(StringBuilder sb, String action,
                                            int x, int y, String label, String cssClass) {
        sb.append("<form method='post' action='/' class='").append(cssClass).append("'>")
          .append("<input type='hidden' name='action' value='").append(action).append("'>")
          .append("<input type='hidden' name='x' value='").append(x).append("'>")
          .append("<input type='hidden' name='y' value='").append(y).append("'>")
          .append("<button type='submit' title='").append(label).append("'>")
          .append(label).append("</button>")
          .append("</form>");
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static String escHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
