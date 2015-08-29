# CircleView
A animated circle view, similar to Android's ProgressBar. Can be used in 'value mode' or 'spinning mode'. 

![MainImage](https://raw.githubusercontent.com/jakob-grabner/Circle-Progress-View/master/media/CircleProgressView.png)

## Fully animated:
![animation demo](https://raw.githubusercontent.com/jakob-grabner/Circle-Progress-View/master/media/demo.gif)

- Animated set value.
- spinning mode.
- Transition from spinning mode to value mode.

## Fully customizable:

![CircleParts](https://raw.githubusercontent.com/jakob-grabner/Circle-Progress-View/master/media/CircleParts.PNG)

All parts come with a customizeable color and thickness. Set the size of a part to 0 to hide it. 

# Text sizes
Per default the texts size is automatically calculated to fit in the circle. 

# Colors
The spin bar color can consist of a single color or a gradient from up to 4 colors.

![Gradient colors.](https://raw.githubusercontent.com/jakob-grabner/Circle-Progress-View/master/media/ColorGradient.jpg)

# Seek Mode
Set value on touch input. Enbale it:
- Via Code:
```
	circleview.setSeekModeEnabled(true);
```
- Via XML:
```
	CircleProgressView:seekMode="true"
```
## Add it to you project:

Get the latest release from https://jitpack.io/#jakob-grabner/Circle-Progress-View 

	allprojects {
	    repositories {
	        // ...
	        maven { url "https://jitpack.io" }
	    }
	}
	
	
	dependencies {
		// ...
	        compile 'com.github.jakob-grabner:Circle-Progress-View:v1.2.1'
	}
