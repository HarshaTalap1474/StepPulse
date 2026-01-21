# 🏃‍♂️ StepPulse - Advanced Android Fitness Tracker

**StepPulse** is a feature-rich Android fitness application developed in **Java**. It utilizes the Android Sensor Framework to track physical activity in real-time and includes health utility tools like a BMI Calculator.

Designed for efficiency, it processes all data locally on the device, ensuring privacy and minimal battery consumption.

---

## 📱 Features

### 1. Activity Tracking (Real-Time)
* **Step Counter:** Utilizes `Sensor.TYPE_STEP_COUNTER` for accurate hardware-based stepping.
* **Distance & Calorie Logic:** Automatically converts steps into Kilometers and Kcal based on average stride algorithms.
* **Live Visualization:** Features a dynamic Line Chart (using *MPAndroidChart*) that updates with every step.

### 2. Health Utilities
* **BMI Calculator:** A dedicated tool to calculate Body Mass Index based on height and weight inputs, providing health category feedback (Underweight, Normal, Obese).

### 3. Social Integration
* **Share Stats:** Built-in sharing functionality allows users to send their daily progress summary to WhatsApp, Instagram, or SMS.

---

## 🛠️ Tech Stack

* **Language:** Java
* **Minimum SDK:** API 29 (Android 10)
* **IDE:** Android Studio
* **Dependencies:**
    * `MPAndroidChart` (Data Visualization)
    * `AndroidX` Libraries
    * `Material Design Components`

---

## 🚀 Getting Started

### Prerequisites
* Android Studio Ladybug (or newer)
* Physical Android Device (Simulators cannot mimic the Step Counter sensor)

### Installation
1.  **Clone the repo:**
    ```bash
    git clone https://github.com/YourUsername/StepPulse.git
    ```
2.  **Sync Gradle:**
    * Open the project in Android Studio.
    * Ensure `maven { url 'https://jitpack.io' }` is in your `settings.gradle`.
3.  **Run:**
    * Connect your phone via USB debugging.
    * Grant **Physical Activity** permissions upon launch.

---

## 📸 Screenshots

| Activity Dashboard | BMI Calculator |
|:---:|:---:|
|![ss_home](https://github.com/user-attachments/assets/5b56e085-6e60-44d6-bd60-c443dce3e8c8)
|![ss_bmi](https://github.com/user-attachments/assets/2b1e7a5a-3949-4385-b656-6454e236a3f4)
|

---

## 👤 Author

**Harshavardhan Talap**
* **Role:** Developer
* **Education:** BE EnTC, DPYCOE Akurdi

---

## 🤝 Contributing

Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.
