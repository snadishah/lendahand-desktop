# LendAHand — Desktop (JavaFX)

The original desktop version of **LendAHand**, a community task marketplace where **Task Posters** hire **Helpers** for small gigs. I built it as an Object-Oriented Programming project at NUST, then rebuilt it as a full-stack web app. That version is live at [lendahand-vzv6.onrender.com](https://lendahand-vzv6.onrender.com) ([source](https://github.com/snadishah/LendAHand)).

## Features

- **Post, bid, hire.** Posters list tasks with a budget and location. Helpers bid, and posters review the bids and accept one.
- **Wallet with escrow.** Accepting a bid holds the funds in escrow. They're released to the helper on completion or refunded if the task is cancelled.
- **Map view.** Tasks are plotted on an OpenStreetMap layer (Gluon Maps). Distances are computed with the Haversine formula to show nearby work.
- **AI assistant.** Google Gemini suggests a fair price in Rs from a task description and powers an in-app chat assistant.
- **Two-way reviews** and a light/dark theme.

## OOP design

| Concept | Where |
|---|---|
| Abstraction and inheritance | `User` is abstract, and `TaskPoster` and `Helper` extend it with role-specific behaviour |
| Encapsulation | Models expose state only through getters and setters, and the DAOs own all SQL |
| Separation of concerns | MVC: FXML views, `controllers/`, `models/`, a `dao/` data layer and `services/` |
| Singletons and utilities | `SessionManager`, `SceneManager`, `ThemeManager`, `DatabaseConnection` |

```
src/main/java/com/lendahand/
├── models/        User (abstract), TaskPoster, Helper, Task, Bid, Review
├── dao/           UserDAO, TaskDAO, BidDAO, ReviewDAO, all using prepared statements
├── controllers/   One controller per FXML screen
├── services/      EscrowManager, GeminiService, LocationService
└── utils/         Session, scene and theme management, DB connection, distance calculation
```

## Tech stack

Java 21 · JavaFX 21 (FXML + CSS) · Gluon Maps · SQLite / MySQL via JDBC · Google Gemini API · Maven

## Run it locally

Requirements: JDK 21+ and Maven.

```bash
# Optional: enables AI price estimates and the chat assistant
export GEMINI_API_KEY=your_key        # free at https://aistudio.google.com/app/apikey

mvn javafx:run
```

By default the app uses a local SQLite file (`lendahand.db`) and creates its tables on first run. To use MySQL instead:

```bash
export LENDAHAND_DB=mysql
export LENDAHAND_MYSQL_USER=root
export LENDAHAND_MYSQL_PASSWORD=your_password
```
