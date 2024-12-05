# Nexus Android App

Nexus is an Android social media application built using **Kotlin** and **Jetpack Compose**, inspired by platforms like Twitter and Threads. The app allows users to post updates, follow others, like and comment on posts, and receive real-time notifications. It integrates **Firebase** for backend services, including real-time data synchronization, user authentication, and push notifications.

---

## 📜 Table of Contents
- [Features](#-features)
- [Technologies Used](#-technologies-used)
- [Installation](#-installation)
- [Usage](#-usage)
- [Screenshots](#-screenshots)
- [Contributors](#-contributors)
- [License](#-license)

---

## 🚀 Features
- Post text-based updates to share with followers.
- Follow other users and see their posts on the feed.
- Like and comment on posts to interact with content.
- Receive real-time notifications with **Firebase Cloud Messaging**.
- Send and receive direct messages.
- Manage your profile, including display name and profile picture.

---

## 🛠 Technologies Used
- **Kotlin** & **Jetpack Compose**: For building a modern, responsive user interface.
- **Firebase**: For backend services including:
  - **Firebase Authentication**: Manage user sign-in/sign-up.
  - **Firebase Firestore**: Real-time data storage for posts, comments, and user interactions.
  - **Firebase Cloud Messaging (FCM)**: Push notifications.
  - **Firebase Storage**: Storing profile pictures and media.

---

## 📥 Installation

1. Clone the repository:
    ```bash
    git clone https://github.com/your-username/nexus-android.git
    ```

2. Navigate to the project directory:
    ```bash
    cd nexus-android
    ```

3. Open the project in Android Studio:
    ```bash
    open Nexus.AndroidStudioProject
    ```

4. Install project dependencies using **Gradle**:
    ```bash
    ./gradlew build
    ```

5. Set up **Firebase**:
   - Create a Firebase project and add an Android app.
   - Download the `google-services.json` from Firebase and add it to your project.
   - Enable **Firestore**, **Authentication**, and **Cloud Messaging** in the Firebase Console.

6. Build and run the project:
    ```bash
    ./gradlew assembleDebug
    ```

---

## 💻 Usage
- Sign up or log in using Firebase Authentication.
- Post updates to your timeline, follow users, and interact with posts.
- Check notifications for real-time updates on likes, comments, and new followers.
- Access your profile to manage account details and upload a profile picture.

---

## 📷 Screenshots
*(Include screenshots to showcase your app's UI and features)*

---

## 👨‍💻 Contributors
This project was developed by **Group 6**:
- **Binaya Thapa Magar**
- **Frank Venance**
- **Samskar Koirala**
- **Soundarya Ramesh**

---

## 📄 License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

### 📬 Contact
For inquiries or feedback, feel free to reach out to the contributors via GitHub.
