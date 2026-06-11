const defaultStudents = [
    { id: 1, regNo: "REG101", name: "Rahul Sharma", phone: "9876543210", department: "Computer", semester: 3, email: "rahul@example.com", photo: "", courseStartYear: 2024, passoutYear: 2027, studentStatus: "Studying", backPapers: 0, cgpa: 8.2, ogpa: "", grade: "A", status: "Pass" },
    { id: 2, regNo: "REG102", name: "Priya Kumari", phone: "9876501234", department: "Electrical", semester: 4, email: "priya@example.com", photo: "", courseStartYear: 2023, passoutYear: 2026, studentStatus: "Studying", backPapers: 0, cgpa: 7.8, ogpa: "", grade: "B", status: "Pass" },
    { id: 3, regNo: "REG103", name: "Amit Verma", phone: "9876512345", department: "Mechanical", semester: 2, email: "amit@example.com", photo: "", courseStartYear: 2025, passoutYear: 2028, studentStatus: "Studying", backPapers: 0, cgpa: "", ogpa: "", grade: "Not Added", status: "Pending" }
];

const usesServer = window.location.protocol !== "file:";
let students = usesServer ? [] : loadStudents();
let nextStudentId = usesServer ? 4 : loadNextStudentId();
let users = usesServer ? [] : loadUsers();
let currentRole = "";
let selectedGradeStatus = "Studying";

function loadStudents() {
    const savedStudents = localStorage.getItem("students");

    if (savedStudents) {
        return JSON.parse(savedStudents);
    }

    return defaultStudents;
}

function loadNextStudentId() {
    const savedNextStudentId = localStorage.getItem("nextStudentId");

    if (savedNextStudentId) {
        return Number(savedNextStudentId);
    }

    return 4;
}

function saveData() {
    if (usesServer) {
        return;
    }

    localStorage.setItem("students", JSON.stringify(students));
    localStorage.setItem("nextStudentId", String(nextStudentId));
}

function loadUsers() {
    const savedUsers = localStorage.getItem("users");

    if (savedUsers) {
        const storedUsers = JSON.parse(savedUsers)
            .filter(user => user.username !== "admin" && user.username !== "Admin" && user.role !== "admin");
        storedUsers.unshift({ username: "Admin", password: "Admin123", role: "admin" });
        return storedUsers;
    }

    return [
        { username: "Admin", password: "Admin123", role: "admin" },
        { username: "user", password: "user123", role: "user" }
    ];
}

function saveUsers() {
    if (!usesServer) {
        localStorage.setItem("users", JSON.stringify(users));
    }
}

async function login() {
    const username = document.getElementById("username").value.trim();
    const password = document.getElementById("password").value.trim();
    const message = document.getElementById("loginMessage");

    if (usesServer) {
        const response = await fetch("/api/login", {
            method: "POST",
            body: new URLSearchParams({ username: username, password: password })
        });
        const result = await response.json();

        if (result.success) {
            currentRole = result.role;
            await showDashboard();
        } else {
            message.textContent = "Invalid username or password.";
        }
        return;
    }

    const user = users.find(item => item.username === username && item.password === password);

    if (user) {
        currentRole = user.role;
        await showDashboard();
    } else {
        message.textContent = "Invalid username or password.";
    }
}

function showCreateAccount() {
    document.getElementById("loginPanel").classList.add("hidden");
    document.getElementById("createPanel").classList.remove("hidden");
    document.getElementById("forgotPanel").classList.add("hidden");
    document.getElementById("loginMessage").textContent = "";
}

function showLoginPanel() {
    document.getElementById("createPanel").classList.add("hidden");
    document.getElementById("forgotPanel").classList.add("hidden");
    document.getElementById("loginPanel").classList.remove("hidden");
    document.getElementById("createMessage").textContent = "";
    document.getElementById("forgotMessage").textContent = "";
}

function showForgotPassword() {
    document.getElementById("loginPanel").classList.add("hidden");
    document.getElementById("createPanel").classList.add("hidden");
    document.getElementById("forgotPanel").classList.remove("hidden");
    document.getElementById("loginMessage").textContent = "";
}

async function createAccount() {
    const username = document.getElementById("newUsername").value.trim();
    const password = document.getElementById("newPassword").value.trim();
    const message = document.getElementById("createMessage");

    if (!username || !password) {
        message.textContent = "Please enter username and password.";
        return;
    }

    if (usesServer) {
        const response = await fetch("/api/register", {
            method: "POST",
            body: new URLSearchParams({ username: username, password: password })
        });
        const result = await response.json();

        if (result.success) {
            message.style.color = "#067647";
            message.textContent = "Account created successfully. You can login now.";
            clearCreateForm();
        } else {
            message.style.color = "#b42318";
            message.textContent = result.message || "Account could not be created.";
        }
        return;
    }

    const alreadyExists = users.some(item => item.username === username);
    if (alreadyExists) {
        message.style.color = "#b42318";
        message.textContent = "Username already exists.";
        return;
    }

    users.push({ username: username, password: password, role: "user" });
    saveUsers();
    message.style.color = "#067647";
    message.textContent = "Account created successfully. You can login now.";
    clearCreateForm();
}

function clearCreateForm() {
    document.getElementById("newUsername").value = "";
    document.getElementById("newPassword").value = "";
}

async function resetPassword() {
    const username = document.getElementById("forgotUsername").value.trim();
    const newPassword = document.getElementById("forgotPassword").value.trim();
    const confirmPassword = document.getElementById("confirmForgotPassword").value.trim();
    const message = document.getElementById("forgotMessage");

    if (!username || !newPassword || !confirmPassword) {
        message.style.color = "#b42318";
        message.textContent = "Please fill all password reset details.";
        return;
    }

    if (newPassword !== confirmPassword) {
        message.style.color = "#b42318";
        message.textContent = "New password and confirm password do not match.";
        return;
    }

    if (username === "Admin") {
        message.style.color = "#b42318";
        message.textContent = "Admin password cannot be changed.";
        return;
    }

    if (usesServer) {
        const response = await fetch("/api/forgot-password", {
            method: "POST",
            body: new URLSearchParams({ username: username, password: newPassword })
        });
        const result = await response.json();

        if (result.success) {
            message.style.color = "#067647";
            message.textContent = "Password reset successfully. You can login now.";
            clearForgotPasswordForm();
        } else {
            message.style.color = "#b42318";
            message.textContent = result.message || "Password could not be reset.";
        }
        return;
    }

    const user = users.find(item => item.username === username);
    if (!user) {
        message.style.color = "#b42318";
        message.textContent = "Username not found.";
        return;
    }

    user.password = newPassword;
    saveUsers();
    message.style.color = "#067647";
    message.textContent = "Password reset successfully. You can login now.";
    clearForgotPasswordForm();
}

function clearForgotPasswordForm() {
    document.getElementById("forgotUsername").value = "";
    document.getElementById("forgotPassword").value = "";
    document.getElementById("confirmForgotPassword").value = "";
}

async function showDashboard() {
    document.getElementById("loginMessage").textContent = "";
    document.getElementById("loginPanel").classList.add("hidden");
    document.getElementById("createPanel").classList.add("hidden");
    document.getElementById("forgotPanel").classList.add("hidden");
    document.getElementById("dashboard").classList.remove("hidden");
    document.getElementById("dashboardTitle").textContent =
        currentRole === "admin" ? "Admin Dashboard" : "User Dashboard";
    document.getElementById("roleDescription").textContent =
        currentRole === "admin"
            ? "Manage student data and academic performance."
            : "View and search student information.";

    document.getElementById("addStudentMenuButton").classList.toggle("hidden", currentRole !== "admin");
    document.getElementById("modifyGradeMenuButton").classList.toggle("hidden", currentRole !== "admin");
    document.getElementById("infoActionHeader").classList.toggle("hidden", currentRole !== "admin");
    document.getElementById("passedInfoActionHeader").classList.toggle("hidden", currentRole !== "admin");
    hideForms();

    if (usesServer) {
        await fetchStudents();
    }

    renderStudents();
    showStudentInformation();
}

async function fetchStudents() {
    const response = await fetch("/api/students");
    students = await response.json();
}

function logout() {
    currentRole = "";
    document.getElementById("dashboard").classList.add("hidden");
    document.getElementById("loginPanel").classList.remove("hidden");
    document.getElementById("createPanel").classList.add("hidden");
    document.getElementById("forgotPanel").classList.add("hidden");
    document.getElementById("username").value = "";
    document.getElementById("password").value = "";
}

function renderStudents(list = students) {
    const infoTable = document.getElementById("studentInfoTable");
    const passedOutInfoTable = document.getElementById("passedOutInfoTable");
    const resultTable = document.getElementById("studentTable");
    const passedOutAcademicTable = document.getElementById("passedOutAcademicTable");
    infoTable.innerHTML = "";
    passedOutInfoTable.innerHTML = "";
    resultTable.innerHTML = "";
    passedOutAcademicTable.innerHTML = "";

    list.forEach(student => {
        const photoCell = student.photo
            ? `<img class="student-photo" src="${student.photo}" alt="${student.name}">`
            : `<div class="photo-placeholder">No Photo</div>`;

        const infoRowHtml = `
            <td>${student.id}</td>
            <td>${photoCell}</td>
            <td>${student.name}</td>
            <td>${student.regNo}</td>
            <td>${student.phone || ""}</td>
            <td>${student.semester}</td>
            <td>${student.email || ""}</td>
            <td>${getCourseDuration(student)}</td>
            <td>${student.studentStatus || "Studying"}</td>
            <td class="${currentRole !== "admin" ? "hidden" : ""}">
                <div class="action-buttons">
                    <button onclick="editStudent(${student.id})">Edit</button>
                    <button class="danger" onclick="deleteStudent(${student.id})">Delete</button>
                </div>
            </td>
        `;

        const infoRow = document.createElement("tr");
        infoRow.innerHTML = infoRowHtml;

        if ((student.studentStatus || "Studying") === "Studying") {
            infoTable.appendChild(infoRow);
        } else {
            const passedInfoRow = document.createElement("tr");
            passedInfoRow.innerHTML = infoRowHtml;
            passedOutInfoTable.appendChild(passedInfoRow);
        }

        if ((student.studentStatus || "Studying") === "Studying") {
            const resultRow = document.createElement("tr");
            resultRow.innerHTML = `
                <td>${student.id}</td>
                <td>${student.regNo}</td>
                <td>${student.name}</td>
                <td>${student.department}</td>
                <td>${student.semester}</td>
                <td>${student.grade}</td>
                <td>${student.cgpa || ""}</td>
                <td>${student.backPapers || 0}</td>
                <td>${student.status}</td>
            `;
            resultTable.appendChild(resultRow);
        } else {
            const passedOutAcademicRow = document.createElement("tr");
            passedOutAcademicRow.innerHTML = `
                <td>${student.id}</td>
                <td>${student.regNo}</td>
                <td>${student.name}</td>
                <td>${student.department}</td>
                <td>${getCourseDuration(student)}</td>
                <td>${student.grade}</td>
                <td>${student.ogpa || ""}</td>
                <td>${student.backPapers || 0}</td>
                <td>${student.status}</td>
            `;
            passedOutAcademicTable.appendChild(passedOutAcademicRow);
        }
    });

    document.getElementById("studentForm").classList.add("hidden");
    document.getElementById("gradeForm").classList.add("hidden");
}

function showStudentInformation() {
    hideForms();
    document.getElementById("studentInfoSection").classList.remove("hidden");
    document.getElementById("academicResultSection").classList.add("hidden");
    document.getElementById("addStudentSection").classList.add("hidden");
    document.getElementById("modifyGradeSection").classList.add("hidden");
    showStudyingStudentInformation();
}

function showAcademicResults() {
    hideForms();
    document.getElementById("studentInfoSection").classList.add("hidden");
    document.getElementById("academicResultSection").classList.remove("hidden");
    document.getElementById("addStudentSection").classList.add("hidden");
    document.getElementById("modifyGradeSection").classList.add("hidden");
    showStudyingAcademicResults();
}

function showAddStudentOptions() {
    hideForms();
    document.getElementById("studentInfoSection").classList.add("hidden");
    document.getElementById("academicResultSection").classList.add("hidden");
    document.getElementById("modifyGradeSection").classList.add("hidden");
    document.getElementById("addStudentSection").classList.remove("hidden");
}

function showModifyGradeOptions() {
    hideForms();
    document.getElementById("studentInfoSection").classList.add("hidden");
    document.getElementById("academicResultSection").classList.add("hidden");
    document.getElementById("addStudentSection").classList.add("hidden");
    document.getElementById("modifyGradeSection").classList.remove("hidden");
}

function showStudyingStudentInformation() {
    document.getElementById("studyingInfoSection").classList.remove("hidden");
    document.getElementById("passedOutInfoSection").classList.add("hidden");
}

function showPassedOutStudentInformation() {
    document.getElementById("studyingInfoSection").classList.add("hidden");
    document.getElementById("passedOutInfoSection").classList.remove("hidden");
}

function showStudyingAcademicResults() {
    document.getElementById("studyingAcademicSection").classList.remove("hidden");
    document.getElementById("passedOutAcademicSection").classList.add("hidden");
}

function showPassedOutAcademicResults() {
    document.getElementById("studyingAcademicSection").classList.add("hidden");
    document.getElementById("passedOutAcademicSection").classList.remove("hidden");
}

function getCourseDuration(student) {
    if (!student.courseStartYear && !student.passoutYear) {
        return "";
    }

    return `${student.courseStartYear || ""} - ${student.passoutYear || ""}`;
}

function searchStudents() {
    const keyword = document.getElementById("searchInput").value.toLowerCase().trim();
    const results = students.filter(student =>
        student.regNo.toLowerCase().includes(keyword)
        || student.name.toLowerCase().includes(keyword)
        || (student.phone || "").toLowerCase().includes(keyword)
        || (student.email || "").toLowerCase().includes(keyword)
        || student.department.toLowerCase().includes(keyword)
        || student.status.toLowerCase().includes(keyword)
        || (student.studentStatus || "").toLowerCase().includes(keyword)
        || String(student.courseStartYear || "").includes(keyword)
        || String(student.passoutYear || "").includes(keyword)
        || String(student.backPapers || "").includes(keyword)
        || String(student.cgpa || "").includes(keyword)
        || String(student.ogpa || "").includes(keyword)
        || String(student.id).includes(keyword)
    );
    renderStudents(results);
}

function showAddStudentForm(status = "Studying") {
    hideForms();
    document.getElementById("addStudentSection").classList.remove("hidden");
    document.getElementById("studentForm").classList.remove("hidden");
    document.getElementById("editStudentId").value = "";
    document.getElementById("regNo").value = "";
    document.getElementById("studentName").value = "";
    document.getElementById("phone").value = "";
    document.getElementById("department").value = "";
    document.getElementById("semester").value = "";
    document.getElementById("email").value = "";
    document.getElementById("courseStartYear").value = "";
    document.getElementById("passoutYear").value = "";
    document.getElementById("backPapers").value = "0";
    document.getElementById("cgpa").value = "";
    document.getElementById("ogpa").value = "";
    document.getElementById("studentStatus").value = status;
    if (status === "Passed Out") {
        const currentYear = new Date().getFullYear();
        document.getElementById("semester").value = "6";
        document.getElementById("passoutYear").value = currentYear;
        document.getElementById("courseStartYear").value = currentYear - 3;
    }
    handleStudentStatusChange();
    document.getElementById("photo").value = "";
    document.getElementById("photoFile").value = "";
    setPhotoPreview("");
}

async function saveStudent() {
    const editId = document.getElementById("editStudentId").value;
    const regNo = document.getElementById("regNo").value.trim();
    const name = document.getElementById("studentName").value.trim();
    const phone = document.getElementById("phone").value.trim();
    const department = document.getElementById("department").value.trim();
    const email = document.getElementById("email").value.trim();
    const courseStartYear = document.getElementById("courseStartYear").value.trim();
    const passoutYear = document.getElementById("passoutYear").value.trim();
    const backPapers = document.getElementById("backPapers").value.trim() || "0";
    const cgpa = document.getElementById("cgpa").value.trim();
    const ogpa = document.getElementById("ogpa").value.trim();
    const studentStatus = document.getElementById("studentStatus").value;
    const semester = studentStatus === "Passed Out" ? "6" : document.getElementById("semester").value.trim();
    const photo = document.getElementById("photo").value;

    if (!regNo || !name || !phone || !department || !semester || !email || !courseStartYear || !passoutYear) {
        alert("Please fill all student details.");
        return;
    }

    if (/[0-9]/.test(name)) {
        alert("Student name should not contain numbers.");
        return;
    }

    if (!/^[0-9]{1,10}$/.test(phone)) {
        alert("Phone number should contain only numbers and maximum 10 digits.");
        return;
    }

    if (!/^[1-6]$/.test(semester)) {
        alert("Semester should be a number between 1 and 6.");
        return;
    }

    if (!/^[A-Za-z0-9._%+-]+@(gmail|yahoo|outlook)\.com$/.test(email)) {
        alert("Invalid email. Use Gmail, Yahoo, or Outlook email.");
        return;
    }

    if (!/^[0-9]+$/.test(backPapers)) {
        alert("Back papers should be a whole number.");
        return;
    }

    if (cgpa && !isValidPointAverage(cgpa)) {
        alert("CGPA should be between 0 and 10.");
        return;
    }

    if (ogpa && !isValidPointAverage(ogpa)) {
        alert("OGPA should be between 0 and 10.");
        return;
    }

    if (usesServer) {
        const formData = new URLSearchParams({
            regNo: regNo,
            name: name,
            phone: phone,
            department: department,
            semester: semester,
            email: email,
            courseStartYear: courseStartYear,
            passoutYear: passoutYear,
            backPapers: backPapers,
            cgpa: cgpa,
            ogpa: ogpa,
            studentStatus: studentStatus,
            photo: photo
        });
        const url = editId ? "/api/students/" + editId : "/api/students";
        const method = editId ? "PUT" : "POST";

        await fetch(url, { method: method, body: formData });
        await fetchStudents();
        renderStudents();
        return;
    }

    if (editId) {
        const student = students.find(item => item.id === Number(editId));
        student.regNo = regNo;
        student.name = name;
        student.phone = phone;
        student.department = department;
        student.semester = Number(semester);
        student.email = email;
        student.courseStartYear = Number(courseStartYear);
        student.passoutYear = Number(passoutYear);
        student.backPapers = Number(backPapers);
        student.cgpa = cgpa;
        student.ogpa = ogpa;
        student.studentStatus = studentStatus;
        student.photo = photo;
    } else {
        students.push({
            id: nextStudentId,
            regNo: regNo,
            name: name,
            phone: phone,
            department: department,
            semester: Number(semester),
            email: email,
            courseStartYear: Number(courseStartYear),
            passoutYear: Number(passoutYear),
            backPapers: Number(backPapers),
            cgpa: cgpa,
            ogpa: ogpa,
            studentStatus: studentStatus,
            photo: photo,
            grade: "Not Added",
            status: "Pending"
        });
        nextStudentId++;
    }

    saveData();
    renderStudents();
}

function editStudent(id) {
    const student = students.find(item => item.id === id);
    hideForms();
    document.getElementById("studentForm").classList.remove("hidden");
    document.getElementById("editStudentId").value = student.id;
    document.getElementById("regNo").value = student.regNo;
    document.getElementById("studentName").value = student.name;
    document.getElementById("phone").value = student.phone || "";
    document.getElementById("department").value = student.department;
    document.getElementById("semester").value = student.semester;
    document.getElementById("email").value = student.email || "";
    document.getElementById("courseStartYear").value = student.courseStartYear || "";
    document.getElementById("passoutYear").value = student.passoutYear || "";
    document.getElementById("backPapers").value = student.backPapers || 0;
    document.getElementById("cgpa").value = student.cgpa || "";
    document.getElementById("ogpa").value = student.ogpa || "";
    document.getElementById("studentStatus").value = student.studentStatus || "Studying";
    handleStudentStatusChange();
    document.getElementById("photo").value = student.photo || "";
    document.getElementById("photoFile").value = "";
    setPhotoPreview(student.photo || "");
}

function handleStudentStatusChange() {
    const status = document.getElementById("studentStatus").value;
    const semesterInput = document.getElementById("semester");
    const cgpaInput = document.getElementById("cgpa");
    const ogpaInput = document.getElementById("ogpa");

    if (status === "Passed Out") {
        semesterInput.value = "6";
        semesterInput.classList.add("hidden");
        cgpaInput.classList.add("hidden");
        ogpaInput.classList.remove("hidden");
    } else {
        semesterInput.classList.remove("hidden");
        cgpaInput.classList.remove("hidden");
        ogpaInput.classList.add("hidden");
    }
}

function cleanPhoneInput() {
    const phoneInput = document.getElementById("phone");
    phoneInput.value = phoneInput.value.replace(/\D/g, "").slice(0, 10);
}

function cleanNameInput() {
    const nameInput = document.getElementById("studentName");
    nameInput.value = nameInput.value.replace(/[0-9]/g, "");
}

function cleanSemesterInput() {
    const semesterInput = document.getElementById("semester");
    semesterInput.value = semesterInput.value.replace(/\D/g, "").slice(0, 1);
}

function isValidPointAverage(value) {
    const number = Number(value);
    return !Number.isNaN(number) && number >= 0 && number <= 10;
}

async function previewSelectedPhoto() {
    const fileInput = document.getElementById("photoFile");
    const file = fileInput.files[0];

    if (!file) {
        return;
    }

    const photoData = await resizePhoto(file);
    document.getElementById("photo").value = photoData;
    setPhotoPreview(photoData);
}

function clearPhoto() {
    document.getElementById("photo").value = "";
    document.getElementById("photoFile").value = "";
    setPhotoPreview("");
}

function setPhotoPreview(photo) {
    const preview = document.getElementById("photoPreview");

    if (photo) {
        preview.innerHTML = `<img class="student-photo" src="${photo}" alt="Selected student">`;
    } else {
        preview.textContent = "No Photo";
    }
}

function resizePhoto(file) {
    return new Promise((resolve, reject) => {
        const reader = new FileReader();

        reader.onload = function(event) {
            const image = new Image();
            image.onload = function() {
                const maxSize = 240;
                let width = image.width;
                let height = image.height;

                if (width > height && width > maxSize) {
                    height = Math.round((height * maxSize) / width);
                    width = maxSize;
                } else if (height > maxSize) {
                    width = Math.round((width * maxSize) / height);
                    height = maxSize;
                }

                const canvas = document.createElement("canvas");
                canvas.width = width;
                canvas.height = height;
                const context = canvas.getContext("2d");
                context.drawImage(image, 0, 0, width, height);
                resolve(canvas.toDataURL("image/jpeg", 0.75));
            };
            image.onerror = reject;
            image.src = event.target.result;
        };

        reader.onerror = reject;
        reader.readAsDataURL(file);
    });
}

async function deleteStudent(id) {
    if (usesServer) {
        await fetch("/api/students/" + id, { method: "DELETE" });
        await fetchStudents();
        renderStudents();
        return;
    }

    students = students.filter(student => student.id !== id);
    saveData();
    renderStudents();
}

function showGradeForm(status = "Studying") {
    hideForms();
    selectedGradeStatus = status;
    document.getElementById("modifyGradeSection").classList.remove("hidden");
    document.getElementById("gradeForm").classList.remove("hidden");
    document.getElementById("gradeStudentId").value = "";
    document.getElementById("grade").value = "";
    document.getElementById("gradeCgpa").value = "";
    document.getElementById("gradeOgpa").value = "";
    document.getElementById("gradeCgpa").classList.toggle("hidden", status !== "Studying");
    document.getElementById("gradeOgpa").classList.toggle("hidden", status !== "Passed Out");
}

async function saveGrade() {
    const studentId = Number(document.getElementById("gradeStudentId").value);
    const grade = document.getElementById("grade").value;
    const cgpa = document.getElementById("gradeCgpa").value.trim();
    const ogpa = document.getElementById("gradeOgpa").value.trim();
    const student = students.find(item => item.id === studentId);

    if (!student || !grade) {
        alert("Please enter a valid student ID and grade.");
        return;
    }

    if ((student.studentStatus || "Studying") !== selectedGradeStatus) {
        alert("Selected student does not belong to this category.");
        return;
    }

    if (cgpa && !isValidPointAverage(cgpa)) {
        alert("CGPA should be between 0 and 10.");
        return;
    }

    if (ogpa && !isValidPointAverage(ogpa)) {
        alert("OGPA should be between 0 and 10.");
        return;
    }

    if (usesServer) {
        await fetch("/api/students/" + studentId + "/grade", {
            method: "PUT",
            body: new URLSearchParams({ grade: grade, cgpa: cgpa, ogpa: ogpa })
        });
        await fetchStudents();
        renderStudents();
        return;
    }

    student.grade = grade;
    if ((student.studentStatus || "Studying") === "Studying") {
        student.cgpa = cgpa;
    } else {
        student.ogpa = ogpa;
    }
    student.status = grade === "F" ? "Fail" : "Pass";
    saveData();
    renderStudents();
}

function hideForms() {
    document.getElementById("studentForm").classList.add("hidden");
    document.getElementById("gradeForm").classList.add("hidden");
}
