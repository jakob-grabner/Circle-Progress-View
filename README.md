# CircleView
A animated circle view. Can be used in 'value mode' or 'spinning mode'. Nice transitions between spinning and value. Can be used as a loading indicator and to show progress or values in a circular manner. In seek mode, it can also be used to set a value.

![MainImage](https://raw.githubusercontent.com/jakob-grabner/Circle-Progress-View/master/media/big.png)

Try it out [here](https://play.google.com/store/apps/details?id=at.grabner.example.circleprogressview).

## Fully animated:
![animation demo](https://raw.githubusercontent.com/jakob-grabner/Circle-Progress-View/master/media/demo.gif)

- Animated set value.
- spinning mode.
- Transition from spinning mode to value mode.

## Fully customizable:

![CircleParts](https://raw.githubusercontent.com/jakob-grabner/Circle-Progress-View/master/media/CircleParts.PNG)

All parts come with a customizable color and thickness. Set the size of a part to 0 to hide it. 

## Text sizes
Per default, the texts size is automatically calculated to fit in the circle. 

## Colors
The spin bar color can consist of a single color or a gradient from up to 4 colors.

![Gradient colors.](https://raw.githubusercontent.com/jakob-grabner/Circle-Progress-View/master/media/ColorGradient.jpg)

## Block Mode
- Via XML
```
        CircleProgressView:cpv_blockCount="18"
        CircleProgressView:cpv_blockScale="0.9"
```

## Seek Mode
Set value on touch input. Enable it:
- Via Code:
```
	circleview.setSeekModeEnabled(true);
```
- Via XML:
```
	CircleProgressView:cpv_seekMode="true"
```


For more examples take a look at the example app.


## Add it to your project:

Get the latest release from https://jitpack.io/#jakob-grabner/Circle-Progress-View 

	allprojects {
	    repositories {
	        // ...
	        maven { url "https://jitpack.io" }
	    }
	}
	
	
	dependencies {
		// ...
	        compile 'com.github.jakob-grabner:Circle-Progress-View:1.2.8'
	}
	
## JavaDoc

Get it [here](https://jitpack.io/com/github/jakob-grabner/Circle-Progress-View/1.2.8/javadoc/).
