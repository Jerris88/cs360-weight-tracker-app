# cs360-weight-tracker-app
Final Android mobile app project for SNHU CS360: Mobile Architect &amp; Progamming

## Overview  
This project is a weight tracking app I built in Android Studio for my CS360 Mobile Architect and Programming course. The goal was to build a practical everyday app that saves user data locally through SQLite and helps people keep track of their progress toward a goal weight. It lets users make an account, log in, record daily weights, set a goal, and get a text alert once that goal is reached.  

## Purpose and Goals  
I designed the app for people who want an easy way to stay on top of their weight goals without anything extra getting in the way. The idea was to keep it clean and straightforward so users can open the app, enter their info, and instantly see where they stand.  

## Screens and User Experience  
The app includes a login screen, registration form, forgot username and password dialogs, the main tracking screen, and an SMS permissions screen. I kept the layouts simple and consistent so everything feels familiar as users move through the app. Each button and field has a clear purpose, and the design focuses on making navigation smooth and frustration free.  

## Coding Approach  
Throughout the course, I started by focusing on the UI design in Android Studio, building each screen layout and testing how it looked and felt before connecting any background code. The goal was to create a UI that was both visually appealing and easy to use, with a modern design and color scheme. Once the design felt right, I began attaching the logic behind each screen so the layouts could actually function. I built the app step by step, starting with the login and SQLite database before adding registration, password recovery, and goal tracking through custom dialog XML layouts. These dialogs gave the app a clean and interactive feel while keeping users inside the same screen.  

For the dialog flow and intent handling, I wanted everything to feel natural so users could clearly see if something was entered incorrectly. One thing I have noticed when using other apps is that you often fill out an entire form only to find out at the end that one of the fields was wrong. My dialogs were built to prevent that. Each field gives instant feedback, showing exactly what needs to be fixed before moving on. It makes the experience smoother and more transparent for the user.  

The database stores everything locally including user info, weights, and goals so it all stays saved even if the app closes. I added plenty of inline comments and small helper functions to keep the logic easy to follow. Organizing things this way made the code easier to read and debug.  

## Testing and Debugging  
I tested each section inside the Android Emulator as I built it. Once login worked, I moved on to registration and made sure data was being saved correctly. Toast messages helped me confirm that the SQLite database updates were actually happening. Testing each feature one at a time helped me spot small issues early and understand how all the parts worked together.  

## Challenges and Problem Solving  
Getting the SMS permission system right was definitely one of the trickier parts. The SMS alerts were designed to notify users when their goal weight was achieved, so they had to work exactly when that condition was met. I had to make sure the app would still run normally if someone denied permission but still send alerts if they allowed it. That process helped me understand Android’s permission system better and how to handle user choices in a respectful way.  

## Reflection  
This course helped me see how design and functionality really connect. Early on, I was mostly focused on what the app looked like, but now I understand how each screen ties back to the logic behind it. Seeing my XML layouts actually interact with the SQLite data and Java code was the part that really brought everything together.  

I learned a lot about planning, writing clear comments, and testing my code properly. Most of all, I realized how much I enjoy building apps piece by piece and watching them come to life. Looking back, this project shows how far I have come since the start of the course both in skill and confidence.
