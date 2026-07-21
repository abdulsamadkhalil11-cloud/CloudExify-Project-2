# LibraSys — Library Management System

A desktop library management system built with Java 21, JavaFX, Maven and
SQLite. MVC architecture, DAO pattern, PBKDF2 password hashing, PDF reports,
light/dark themes.

## Status: what's in this build

Built out fully, end-to-end: **Login → Dashboard → Book Management →
Student Management → Issue/Return → Settings**, all backed by a real
SQLite database with seed data so it's usable the moment you run it.

Cut from the original wishlist, on purpose, given the timeline — see
**"Not included"** at the bottom for the full list and why (short version:
barcode *hardware*, CSV import/export, a full audit log, and .exe packaging
were the lowest-value items per hour of work this close to a deadline).
Everything else from the brief is here.

## Tech stack

| Layer | Choice |
|---|---|
| Language | Java 21 |
| UI | JavaFX 21 + FXML + CSS (hand-written, no Scene Builder-generated markup) |
| Build | Maven |
| Database | SQLite (default), MySQL-ready — see below |
| Patterns | MVC, DAO, Singleton (`DatabaseConnection`), Service layer |
| PDF reports | Apache PDFBox |
| Password hashing | PBKDF2WithHmacSHA256 (100,000 iterations, random salt per user) |

No icon-font library (Ikonli, FontAwesome, etc.) — every icon in the sidebar
and dashboard is a hand-drawn `javafx.scene.shape.SVGPath`. That was a
deliberate call: an icon-font dependency can't be verified without a live
Maven Central connection, and a single mistyped icon name throws at
runtime and takes the whole screen down. Plain SVGPath can't do that — worst
case it's a slightly odd shape, never a crash.

## Project structure

```
library-management-system/
├── pom.xml
├── database/
│   ├── schema_sqlite.sql      canonical DDL (also embedded in SchemaScript.java)
│   └── schema_mysql.sql       same design, MySQL syntax
├── src/main/java/com/library/
│   ├── Main.java               entry point (extends Application)
│   ├── Launcher.java           fat-jar entry point (see "Running" below)
│   ├── model/                  Book, Author, Category, Student, Staff, BorrowRecord
│   ├── dao/                    interfaces + DataAccessException
│   │   └── impl/                SQLite implementations
│   ├── service/                business rules: validation, issue/return logic, PDF reports
│   ├── controller/              one controller per FXML screen
│   └── util/                    DatabaseConnection, PasswordUtil, SceneManager, etc.
└── src/main/resources/
    ├── fxml/                    one FXML per screen
    ├── css/                     common.css + light-theme.css + dark-theme.css
    ├── images/                  app icon, default cover/avatar placeholders
    └── db.properties            db.type=sqlite|mysql
```

## Running it

**IntelliJ IDEA** (recommended)
1. `File → Open` → select the `library-management-system` folder (the one with `pom.xml`).
2. Let Maven finish importing (first import downloads JavaFX, SQLite, PDFBox — needs internet once).
3. Run configuration: Maven → `javafx:run`, or open `Main.java` and run it directly
   (IntelliJ will need the JavaFX VM options; the `javafx-maven-plugin` in `pom.xml`
   handles this automatically if you use `mvn javafx:run` instead).

**Command line**
```
mvn clean javafx:run
```

**NetBeans**
1. `File → Open Project` → select the folder.
2. Right-click the project → Run. NetBeans reads `pom.xml` directly; no extra config needed
   since the `javafx-maven-plugin` is already wired to the `javafx:run` goal.

**Scene Builder**
Every FXML file under `src/main/resources/fxml/` opens directly in Scene Builder —
right-click any `.fxml` → Open With → Scene Builder (or configure the association once
in IDE settings). `fx:controller` is already set on each file, so Scene Builder will
resolve the controller classes as long as the project has been built at least once.

**Plain `java -jar`** (after `mvn clean package`)
```
java -jar target/library-management-system.jar
```
This runs through `Launcher.java` rather than `Main.java` directly — a class that
itself extends `Application` can't be the `Main-Class` of a plain runnable jar on
some JDK/OS combinations (it fails with a misleading "JavaFX runtime components
are missing" error even though they're bundled by the shade plugin). `Launcher`
just forwards to `Main.main()`.

## Default login

| Username | Password | Role |
|---|---|---|
| `admin` | `admin123` | Administrator |
| `librarian` | `librarian123` | Librarian |

Both accounts' security question is **"What city were you born in?"** with the
answer **"Islamabad"** — used by the "Forgot password?" link on the login screen.

Only Administrators see the Library Configuration and Data (backup/restore)
sections in Settings; Librarians can still change their own password and theme.

## Forgot password — why not email?

A real "email me a reset link" flow needs an SMTP server, which means asking
whoever grades or runs this to configure mail credentials before the app even
opens. Instead: every staff account has a security question set at creation,
and "Forgot password?" on the login screen verifies the answer (case-insensitive)
before allowing a new password. Works completely offline, which is what a
desktop app running on a library's front-desk PC actually needs.

## Barcode scanning

There's no barcode-scanner SDK integration — but the Issue/Return screen's
ISBN field works with any USB barcode scanner out of the box, because those
scanners just type the scanned code as keystrokes into whatever field has
focus, then send Enter. Click into the ISBN field once and start scanning.

## Switching to MySQL

1. Run `database/schema_mysql.sql` against your MySQL server.
2. In `src/main/resources/db.properties`, set:
   ```
   db.type=mysql
   db.mysql.host=localhost
   db.mysql.port=3306
   db.mysql.database=library_db
   db.mysql.user=root
   db.mysql.password=yourpassword
   ```
3. In `pom.xml`, uncomment the `mysql-connector-j` dependency.
4. Rebuild. No Java code changes — every DAO is written against plain `java.sql`.

Note: Settings → Data (backup/restore) only works against SQLite, since it's
a direct file copy of the database. It's automatically disabled when
`db.type=mysql`.

## Database schema

Seven tables, normalized to 3NF: `categories`, `authors`, `books`, `students`,
`staff`, `borrow_records`, `settings`. Foreign keys enforced (`PRAGMA
foreign_keys = ON`), plus CHECK constraints (e.g. `available_copies <=
total_copies`) — see `database/schema_sqlite.sql` for the full DDL with
comments. The app seeds a demo catalog (7 books, 4 students, standard
settings) on first run only; delete `library.db` to start fresh.

## Verification

Every layer was actually tested, not just written and hoped for:
- **Backend (models/DAOs/services):** compiled and run against a real
  SQLite database — schema creation, seeding, password hashing, foreign
  keys, CHECK constraints, and the full issue → renew → renew → return
  lifecycle all pass real assertions, not just "it compiled."
- **PDF reports:** actually generated with PDFBox and inspected — real
  files, correct page layout, no column overflow.
- **UI:** the whole app was launched headless (Xvfb) and driven through
  login, every screen, dark mode, and the Add Book modal with simulated
  clicks/keystrokes. Zero exceptions across the full run.

None of that guarantees zero bugs — only that the happy paths, the
constraints, and the money calculations (fines, availability) are real,
checked behavior, not guesses.

## Keyboard shortcuts

`Ctrl+D` Dashboard · `Ctrl+B` Books · `Ctrl+T` Students · `Ctrl+I` Issue/Return
· `Ctrl+,` Settings

## Packaging to a Windows .exe (not done yet)

Not included in this build — genuinely lower priority than a working app
before the deadline. When you're ready:
```
mvn clean package
jpackage --input target/ --name LibraSys --main-jar library-management-system.jar ^
  --main-class com.library.Launcher --type exe --icon src/main/resources/images/app-icon.png
```
`jpackage` ships with the JDK (11+) — no extra download. You'll want to test
this on an actual Windows machine; it's untested here.

## Not included (and why)

| Cut | Why |
|---|---|
| Barcode *scanner hardware* SDK | The ISBN text field already works with any USB scanner (see above) — a dedicated SDK integration adds real complexity for the same practical result. |
| CSV import/export | Real feature, low priority under a hard deadline; straightforward to add later with `PrintWriter`/`Scanner` over the existing DAOs. |
| Excel export, Print | One report format (PDF) covers the requirement; the other two are the same data, different renderer. |
| Full activity/audit log | Borrow history already tracks every issue/return/renew with who/when; a separate table logging every UI click was scoped out as low value for the effort. |
| Database backup scheduling | Manual backup/restore is in Settings; *automatic* scheduled backups would need a background job — cut for time. |
| `.exe` packaging | `jpackage` command is documented above and ships with the JDK; not run/tested here for lack of a Windows target to verify against. |

If any of these turn out to matter for grading, they're the right ones to
ask for next — each is additive, none require touching what already works.
