
# DroneSimulation-Part4 - Overview

A simulation of a multiple drones which will fly in a three dimensional virtual testbed. The main object of the drones is to bring packages from one airport to another as fast as possible. There will be multiple airports in the virtual testbed and multiple drones flying from one airport to the other to deliver packages.

<p align="center">
  <img src="https://github.com/RubenPants/DroneSimulation-Part4/blob/master/Virtual-testbed.gif"/>
</p>



# Run the Project

To run the project, go to 'Virtual Testbed' -> 'src' -> 'main' -> 'MainLoop' and run 'MainLoop' as a Java Application. Within the _testbed_ GUI, it is
possible to perform multiple types of actions (go to the section _Virtual Testbed_ for a detailed explenation) but if you want a quick demonstration of
the final product of the project go to the 'Packages' panel, make sure the program is running ('Run' -> 'Start Simulation') and press 'Automatic ON' from
within the _Packages_-panel.



# Virtual Testbed

The _virtual testbed_ will provide a clear representation of all the drones and their actions. Inside the GUI there are multiple windows which will add a 
corresponding functionality to the project. A quick overview of these windows are:  
* __File__ - Add a custom path to the testbed (depricated)  
* __Settings__ - Change the drone or the testbed settings (partly depricated)  
* __Run__ - Manage the flow of the program (start, reset, run tests, ...)  
* __Window__ - Change the drone-view  
* __Simulation__ - Handle the simulation-settings: change view and speed of the simulation  
* __Configuration__ - Change the drone you are currently watching and modify its configuration, it is possible to add new drones and airports to the testbed  
* __Packages__ - Handle the packaging-service: add new packages that must be delivered, get an overview of the logbook of delivered packages (and which drone
delivered them), or let the simulation run on itself by pressing the _Automatic_ button    



# Autopilot

In this version (the fourth one) multiple drones will work together as a distributed system to deliver packages at the corresponding airport. The drones 
will communicate with each other to make sure that they do not crash into each other. The drones will also communicate with the airports to know if they 
have the permission to land. The same autopilot is used as in _Part3_ of the project, which looks almost the same as the one in _Part2_, using a 
PD-controller.



# Airport

The airports all have two gates where they can have a drone. It is not possible to have two drones at the same airport. The airports have the control to 
grant permissions to drones and distrubute drones that are within the airport to other airports to make place for other drones within this airport. You can
consider an airport as a part within the distributed system.



# Package Delivery System

A package delivery system will control the whole simulation. The main objective of the distributed system of drones as a whole is to deliver to packages to
the requested airport as fast as possible. Packages will spawn (also visually, in comparison to _Part3_) at a given airport and must be brought to a
requested airport with the help of a drone. It is not possible for a drone to have more than one package.



# Changing the AutoPilot

When changes are made within the _AutoPilot_, you have to export the whole _AutoPilot_ file as a jar and place this jar in the _Virtual Testbed_ file on the
following location: 'Virtual Testbed' -> 'lib' -> 'jar'. You __must__ name this jar 'autopilot.jar' otherwise the testbed will not recognize the jar. At the
moment there is no functionality to toggle between multiple jars, and thus it will not be possible to test or compare two or more autopilots at the same time.



# History of the Project

This project is the seccond part of a larger whole:
* __Part1__ - Fly in the testbed.  
Link: https://github.com/RubenPants/DroneSimulation-Part1  
* __Part2__ - Take off, fly, land and taxi in the testbed. Control drone with phone.  
Link: https://github.com/RubenPants/DroneSimulation-Part2  
* __Part3__ - Example of a worst case of the package-distributing-system of _Part4_ where all the drones try to land at the same airport.  
Link: https://github.com/RubenPants/DroneSimulation-Part3  
* __Part4__ - A package-distributing-system where multiple drones must work together to distributed packages in a virtual environment.  
Link: https://github.com/RubenPants/DroneSimulation-Part4  
