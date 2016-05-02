# Open-Skies

Welcome! Outlier: Open Skies has been relicensed under MIT in order to maximize the utility people will get from the source code. The short version: You can do pretty much whatever you want with this code!

In short, Outlier is a 3D space combat and trading simulator with an X3 style dynamic universe and economy with a Freelancer style open world and an inertial flight model (face in a different direction than you fly and only thrust to change direction / velocity).

Dynamic Universe
----------------
The dynamic universe is quite sophisticated and is inspired by many hundreds of hours playing the X series (namely Reunion and Terran Conflict). No matter what you're doing, the NPCs of the universe are going about their daily lives. There is no instancing, and all solar systems, stations, and ships are simulated in real time at all times. This means that your actions, and the actions of the NPCs, have a ripple effect throughout the world making it emergent and alive.

Even missions don't go after instanced targets. When you're given a mission to destroy an NPC, that NPC existed and was going about its own business long before the mission was offered to you.

Scale Out
---------
Outlier allows the player to command as many ships and stations as they can buy. Ships can be instructed to patrol, trade, and more. This allows the player to eventually control an armada of automated fighters and traders, and manufacture their own goods.

Modding Friendly
----------------
When writing this game, a goal was to make it easy for modders to get into. The resource files are common formats like plain text, blender models, PNG files, and WAV audio. The game is written in Java and is extremely portable. I've tried to document the code, and kept it under an open source license friendly to forking.

Development Branch
------------------
Note that the master branch is currently stale and represents a much older release (the last stable release). The development branch ( https://github.com/masternerdguy/Open-Skies/tree/Development ) is where new development is happening and has many new features and improvements but is not ready for release yet. It may or may not be stable at any given time. Milestones in the development branch which are stable will be tagged.

Preparation 
-----------
Building requires JDK 8 and NetBeans IDE. You can get those as a free download. This game is cross platform and is developed on Linux, officially supported on Windows + Linux, and has been tested on OS X. This project does not require you to install the JMonkeyEngine IDE because the JME engine library files are bundled with the project.

Once you've downloaded those, and you've pulled the repository, you'll need to extract the 7zipped skybox textures in "assets/Textures/Skybox" which are required at runtime.

Now you can open the project in NetBeans and build / run the project.

Controls
--------
You should seriously read the controls in "src/resource/Controls.txt".

When you map your joystick, map throttle to a hat. You don't want to thrust constantly! This isn't Freelancer, you will run out of fuel fast if you don't use controlled bursts of thrust to manauver.