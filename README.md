# 🎓 Tutrnav
> **Discover teachers who teach what you love, and manage your tuition business with absolute perfection.**

![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Language](https://img.shields.io/badge/Language-Java-007396?style=for-the-badge&logo=java&logoColor=white)
![Database](https://img.shields.io/badge/Database-Firebase-FFCA28?style=for-the-badge&logo=firebase&logoColor=white)
![Maps](https://img.shields.io/badge/Maps-OSMDroid-7E57C2?style=for-the-badge&logo=openstreetmap&logoColor=white)

**Tutrnav** is a premium, edge-to-edge Android application built to bridge the gap between local tutors and students. With a stunning **Glassmorphism UI**, real-time messaging, and smart geolocation, it serves as a complete ecosystem for the modern education landscape.

---

## 📸 App Showcase

### 👨‍🏫 Teacher Experience
| Dashboard & Analytics | Class Management | Student Hub (Smart Select) | Broadcast & Fee Reminders |
| :---: | :---: | :---: | :---: |
| <img src="https://via.placeholder.com/250x500.png?text=Dashboard+Screenshot" width="200"/> | <img src="https://via.placeholder.com/250x500.png?text=Add+Class+Screenshot" width="200"/> | <img src="https://via.placeholder.com/250x500.png?text=Student+Hub+Screenshot" width="200"/> | <img src="https://via.placeholder.com/250x500.png?text=Broadcast+BottomSheet" width="200"/> |
| *Real-time income, active students, and swipe-to-act enrollment requests.* | *Create classes with Map Geocoding, Image Uploads, and Aesthetic Time Pickers.* | *View approved students, multi-select, and trigger contextual actions.* | *Send targeted class updates, private messages, or Fee Reminders.* |

### 👨‍🎓 Student Experience
| Discover & Explore | Map & Class Details | Today's Schedule | Real-Time Notifications |
| :---: | :---: | :---: | :---: |
| <img src="https://via.placeholder.com/250x500.png?text=Discover+Screenshot" width="200"/> | <img src="https://via.placeholder.com/250x500.png?text=Map+Bottom+Sheet" width="200"/> | <img src="https://via.placeholder.com/250x500.png?text=Schedule+Screenshot" width="200"/> | <img src="https://via.placeholder.com/250x500.png?text=Notifications+Screenshot" width="200"/> |
| *Find tuitions by category (Math, Code, Music) with interactive swipe stacks.* | *Tap map pins for quick-info bubbles, expand for Teacher PFP and Enroll.* | *Beautifully styled daily agenda with dynamic teacher data and favoriting.* | *Receive instant updates from teachers with custom badges (Normal/Important/Fee).* |

### 🔐 Authentication & Profile
| Smart OTP Auth | Edge-to-Edge Sign Up | Interactive Profile |
| :---: | :---: | :---: |
| <img src="https://via.placeholder.com/250x500.png?text=Login+OTP+Screenshot" width="200"/> | <img src="https://via.placeholder.com/250x500.png?text=Sign+Up+Screenshot" width="200"/> | <img src="https://via.placeholder.com/250x500.png?text=Profile+Screenshot" width="200"/> |
| *Phone & Email support, auto-focus OTP boxes, and Google Sign-in.* | *Cross-device role syncing ensures users land on the correct dashboard.* | *Edit Name, Phone, and Experience. Features Cloudinary PFP uploads.* |

---

## ✨ Key Features

### 🍎 For Teachers
*   **Swipe-to-Act Dashboard:** Swipe right to approve a student, swipe left to reject. UI updates optimistically with a seamless 35dp rounded-corner paint effect.
*   **Automated Income Tracking:** The app dynamically calculates monthly revenue based on approved student enrollments and class fees.
*   **Deep Delete:** Deleting a class automatically cleans up the database, wiping associated messages and enrollments to prevent ghost data.
*   **Smart Broadcast System:** Select one student for a private DM, select multiple for a group message, or click "Broadcast" to instantly alert an entire class.

### 🎒 For Students
*   **Interactive Geolocation:** Uses OSMDroid. Tap a map pin to pop open a sleek info window. Tap the window to slide up a Glassmorphism bottom sheet containing the Teacher's verified details.
*   **One-Tap Enrollment & Calling:** Request to join a class with one tap. If the teacher hasn't hidden their number, launch the phone dialer instantly.
*   **Live Schedule:** See exactly what classes are happening today, complete with the teacher's profile picture and specific address.

### 🛠 System & UI
*   **Premium Glassmorphism Design:** Deep `#44000000` transparent overlays, strictly maintained `35dp` corner radii, and zero elevations for a flat, modern aesthetic.
*   **Dual Authentication:** Support for Google Sign-in, Email/Password (with email verification links), and Phone Number (with 6-digit SMS OTP).
*   **Denormalized NoSQL Architecture:** Highly optimized Firestore structure fetches embedded Teacher details directly within class documents for lightning-fast reads.

---

## 💻 Tech Stack

*   **Language:** Java (Android Native)
*   **UI Toolkit:** XML / ConstraintLayout / Material Design Components (MDC)
*   **Database:** Firebase Firestore (Real-time NoSQL)
*   **Authentication:** Firebase Auth (Google, Email, Phone/OTP)
*   **Image Storage:** Cloudinary SDK (Unsigned Upload Presets)
*   **Maps:** OSMDroid (OpenStreetMap) - *100% Free, No Credit Card Required*
*   **Image Loading:** Glide (with DiskCacheStrategy and custom transformations)
*   **Architecture:** MVVM (Shared ViewModels for real-time fragment communication)

---

## 🚀 Installation & Setup

Want to run Tutrnav locally? Follow these exact steps:

### 1. Clone the Repository
```bash
git clone https://github.com/YourUsername/Tutrnav.git
cd Tutrnav
```

### 2. Configure Firebase (100% Free Spark Plan)
1. Go to the [Firebase Console](https://console.firebase.google.com/) and create a new project.
2. Add an Android App using your package name (`com.onrender.tutrnav`).
3. Download the `google-services.json` file and place it in the `app/` directory of this project.
4. **Enable Auth:** Enable Email/Password, Google, and Phone Authentication.
5. **Add SHA-1:** In Firebase Project Settings, add your Android Studio SHA-1 fingerprint (Required for Google Sign-in and Phone OTP).
6. **Enable Firestore:** Create a Firestore database in Test Mode. Update your rules to:
   ```javascript
   rules_version = '2';
   service cloud.firestore {
     match /databases/{database}/documents {
       match /{document=**} {
         allow read, write: if request.auth != null; 
       }
     }
   }
   ```

### 3. Configure Cloudinary
1. Create a free account on [Cloudinary](https://cloudinary.com/).
2. Go to **Settings > Upload** and create an **Unsigned Upload Preset** named `tutornav_preset`.
3. Open `MyApplication.java` in the project and replace `"drukt0qau"` with your actual Cloudinary `cloud_name`.

### 4. Build and Run
* Open the project in **Android Studio (Giraffe or newer)**.
* Sync Gradle files.
* Ensure you have an active internet connection on your emulator/physical device.
* Hit **Run**! 🏃‍♂️💨

---

## 📂 Project Structure Snapshot
```text
com.onrender.tutrnav
│
├── activities/           # AuthActivity, SplashActivity, Onboarding, Profiles
├── teacher/              # TeacherHomeActivity, DashFragment, Schedule, Tuition (CRUD)
├── student/              # StudentHomeActivity, Discover, MapsFragment, Schedule
├── models/               # TuitionModel, EnrollmentModel, MessageModel
├── adapters/             # Custom Recycler Adapters (StudentList, Classes, Notifications)
└── viewmodels/           # SharedTuitionViewModel, TeacherViewModel (Real-time sync)
```

---

## 🔮 Roadmap / Future Enhancements
- [ ] Add specific Search Filters on the Map (Filter by Price, Subject).
- [ ] Integrate an in-app Video Player for recorded class highlights.
- [ ] Implement Razorpay/Stripe for direct fee processing.
- [ ] Add a native dark/light mode theme toggle (currently forced dark-glass aesthetic).

---

## 🤝 Contributing
Contributions, issues, and feature requests are highly welcome! Feel free to check the [issues page](../../issues). 

## 📜 License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---
*Designed with ❤️ and built for perfection.*
