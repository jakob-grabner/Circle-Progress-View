# CircleView
An circle view, similar to Android's ProgressBar. Can be used in 'value mode' or 'spinning mode'. 


Add it to you project:

	- Download the CircleProgressView.aar into your libs folder.
	- Add libs folder as a maven repo in your build.gradle
	
	repositories {
		mavenCentral()
		flatDir {
			dirs 'libs' //this way we can find the .aar file in libs folder
		}
	}
	
	- add dependency
	compile 'at.grabner.circleprogress:CircleProgressView:1.0@aar'
	

