# CMPUT 301 W25 - Team AN-Droids

## Team Members

| Name        | CCID   | GitHub Username |
| ----------- | ------ | --------------- |
| Jimi Lin  | jimi |   jimi-l      |
| Mekha George | mekha | mekha03    |
| Songrao Fang | songrao | HiguchiGenmei     |
| Vaibhav Jain | vjain3 | vaibh123540     |
|Gurkaranvir Kaur|gurkara4 | gurkaranvirr    |
| Divit Batra| dbatra | dbatra11     |

## Project Description

This project is an Android application that enables users to share and interact with mood updates, as well as follow and engage with other users. The app allows users to view profiles, manage relationships (follow, request follow, unfollow), and listen to audio notes. The main goal is to provide a simple and engaging social platform where users can express their moods and communicate.

## Key Features

- User Profiles: Users can view profiles, including username, followers, following stats, and a profile picture.
- Follow/Unfollow: Users can follow, unfollow, or send follow requests to other users. The follow state is dynamically managed based on the relationship between users.
- Mood Sharing: Users can post their moods, with privacy settings to control visibility. They can also view the most recent moods of others.
- Moods Feed: Display the latest mood updates from followed users with an option to view specific user posts.

## Backlog

See the Github project for a view of User Stories and the tasks associated with them. Issues marked with "story" tag have the requirements and acceptance criteria, while those marked as "task" reflect logical chunks of work that complete the user story. Tasks are linked to user stories,

Feel free to log bugs with the "bug" tag and these will be picked up ASAP.

## Setup Instructions

1. **Clone the repository**:

2. **Install dependencies**:
    - Open the project in Android Studio and let it sync with Gradle. Ensure that Firebase is set up and the necessary API keys are provided.

3. **Run the app**:
    - Once the project is set up and the dependencies are installed, you can run the app on an emulator or a physical device. Ensure that you have an internet connection for Firebase operations.

4. **Permissions**:
    - The app uses permissions to record audio. Make sure to add appropriate permissions in the `AndroidManifest.xml` file:
      ```xml
      <uses-permission android:name="android.permission.RECORD_AUDIO" />
      <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
      <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
      ```

## Documentation

- [Wiki Link](https://github.com/cmput301-w25/project-an-droids/wiki)
- [Scrum Board](https://github.com/orgs/cmput301-w25/projects/52)
- [UI Mockups](https://github.com/cmput301-w25/project-an-droids/wiki/Final-Version-%E2%80%90-Storyboard-Sequences-and-User-Interface-Mockups)
- [UML](https://github.com/cmput301-w25/project-an-droids/blob/main/project-an-droids%20UML.drawio.png)
