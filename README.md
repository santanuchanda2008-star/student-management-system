# Student Management System

This is a simple console-based Java project for a polytechnic minor project. It uses basic Java concepts like classes, objects, methods, `Scanner`, `ArrayList`, loops, and conditions.

## Project Roadmap

The project has two types of users:

1. User
   - Login
   - View student information
   - Search student information by Student ID or Registration Number

2. Admin
   - Login
   - Add student
   - View student
   - Update student
   - Delete student
   - Add grade
   - Update grade
   - View results
   - Search results

## Login Details

Admin:

```text
Username: Admin
Password: Admin123
```

User:

```text
Username: user
Password: user123
```

## File Explanation

`Main.java`
- Starts the program.
- Creates the `Scanner`, `StudentData`, and `Login` objects.
- Shows the project title and login details.

`Login.java`
- Takes username and password.
- Sends admin users to the admin menu.
- Sends normal users to the user menu.

`Student.java`
- Stores one student's details.
- Contains fields like Student ID, Registration Number, Student Name, Department Name, Semester, Grade, and Result Status.
- Has display methods to print student information.

`StudentData.java`
- Stores all student records in an `ArrayList`.
- Adds sample student records when the program starts.
- Contains methods to add, search, and delete students.

`Admin.java`
- Contains the admin menu.
- Handles student data operations.
- Handles academic performance operations.

`User.java`
- Contains the user menu.
- Allows viewing and searching student information only.

## How To Compile

Open terminal in this folder and run:

```bash
javac *.java
```

## How To Run

After compiling, run:

```bash
java Main
```

## How To Run The Website With Oracle Database

Easy method:

Double-click:

```text
run_website.bat
```

Then open:

```text
http://127.0.0.1:8081/
```

Manual method:

Make sure Oracle is running, then compile with the Oracle JDBC driver:

```bash
javac -cp ".;lib\ojdbc8.jar" *.java
```

Start the website server:

```bash
java -cp ".;lib\ojdbc8.jar" WebServer
```

Open this link in the browser:

```text
http://127.0.0.1:8081/
```

The website uses these Oracle tables:

- `sms_users`
- `sms_students`

The program creates these tables automatically if they do not already exist.

The website also has a `Create Account` option before login. New accounts are created as user accounts and saved in the `sms_users` table.

The admin dashboard shows four main options:

- Student Information: has `Students` and `Passed Out Students`
- Academic Result: has `Students` with CGPA and `Passed Out Students` with OGPA
- Add Student: has `Students` and `Passed Out Students`
- Modify Grades: has `Students` and `Passed Out Students`

Every student table shows `Student ID` as a serial number.

## Full Online Hosting

This project includes a `Dockerfile` and supports environment variables for online hosting.

See `DEPLOYMENT_GUIDE.md` for full hosting steps.

## Presentation Help

Use `PRESENTATION_GUIDE.md` to explain the project to teachers. The running program also has a `Project Information` option on the login page, which helps you explain the modules before showing the live demo.

## Important Note

This project stores data temporarily in memory using `ArrayList`. That means newly added data remains available only while the program is running. When the program is closed, it starts again with sample records.

This design is intentional because the project is written in simple basic Java for beginner-level understanding.
