# To-Do App with Login (Android)

**OIBSIP · Android App Development · Task 2**

A task manager where each user registers and logs in before seeing their own private to-do list. All data is stored locally in SQLite.

## Features
- **Sign-up** (name, email, password) and **Login** (email + password)
- Passwords are **never stored in plain text**: salted SHA-256 hash in SQLite
- **Session handling**: stays logged in across restarts; **Logout** clears the session and returns to Login
- **Task list** per logged-in user (tasks are linked to the user by `user_id`)
- **Add Task** dialog (task name + optional notes, with validation)
- **Mark complete** via checkbox (strikethrough, moved below pending tasks)
- **Delete** a task permanently (with confirmation)
- **Empty state** message when there are no tasks

## Tech stack
Android Studio · Java · XML · SQLite (`SQLiteOpenHelper`) · Material Components

## Project structure
```
app/src/main/java/com/oibsip/todoapp/
  LoginActivity.java     login screen + auto-redirect if session exists
  SignupActivity.java    registration with validation
  MainActivity.java      task list, add dialog, logout
  TaskAdapter.java       list rows (checkbox, strikethrough, delete)
  DatabaseHelper.java    users/tasks tables, hashing, CRUD
  SessionManager.java    SharedPreferences-based session
  Task.java              model
app/src/main/res/layout/ activity_login, activity_signup, activity_main, item_task, dialog_add_task
```

## Database schema
- `users(id, name, email UNIQUE, salt, password_hash)`
- `tasks(id, user_id → users.id, title, notes, completed)`

## Run it
1. Open this folder in **Android Studio** (Hedgehog or newer) and let Gradle sync.
2. Pick an emulator or a device (API 24+) and press **Run**.
3. Sign up, log in, add / complete / delete tasks, then log out and try a second account to see that tasks are user-specific.

## Screenshots
Add your screenshots to a `screenshots/` folder (login, sign-up, task list, add dialog, empty state).
