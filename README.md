# Change-Detection
Java based "Change Detection Application" for video where user must have to pick background frame from video. After that change detection shall be calculated by absoulute difference between selected backgrund and current frame of video. At the case of "Camera Tampering" selected background frame by user shall be changed with new background of camera (in case of light sensitivity chamged). Build the uploaded project in following environment before executing it:

Platform OS: Linux

Screen Resolution: 1920X1080

Language: java version "1.8.0_91"

Environment: opencv-3.4.0 or any similar versions must installed before running this application

Lib: jcommon.jar, log4j-1.2.17.jar, opencv-340.jar

IDE: NetBeans IDE 8.2

-Djava.library.path="<path_of_opencv>/opencv-3.4.0/build/lib/" add as VM option in netbeans to compile and run the application from netbeans program

Configuratuion: playList.txt is used to play the list of videos by media player

export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:<path_of_opencv>/opencv-3.4.0/build/lib/

Run Command: java -jar dist/VideoChangeDetection.jar
