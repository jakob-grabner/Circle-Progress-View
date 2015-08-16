package at.grabner.circleview;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;

import at.grabner.circleprogress.CircleProgressView;


public class MainActivity extends ActionBarActivity {


    CircleProgressView mCircleView;
    Switch mSwitchSpin;
    Switch mSwitchShowUnit;
    SeekBar mSeekBar;
    SeekBar mSeekBarSpinnerLength;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mCircleView = (CircleProgressView) findViewById(R.id.circleView);
//      value setting
        mCircleView.setMaxValue(100);
        mCircleView.setValue(0);
        mCircleView.setValueAnimated(24);

//        show unit
        mCircleView.setUnit("%");
        mCircleView.setShowUnit(true);

//        text sizes
        mCircleView.setTextSize(20); // text size set, auto text size off
        mCircleView.setUnitSize(15); // if i set the text size i also have to set the unit size

        mCircleView.setAutoTextSize(true); // enable auto text size, previous values are overwritten

//        spinning
        mCircleView.spin(); // start spinning
        mCircleView.stopSpinning(); // stops spinning. Spinner gets shorter until it disappears.
        mCircleView.setValueAnimated(24); // stops spinning. Spinner spinns until on top. Then fills to set value.



        //Setup Switch
        mSwitchSpin = (Switch) findViewById(R.id.switch1);
        mSwitchSpin.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mCircleView.spin();
                } else {
                    mCircleView.stopSpinning();
                }
            }
        });

        mSwitchShowUnit = (Switch) findViewById(R.id.switch2);
        mSwitchShowUnit.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    mCircleView.setShowUnit(isChecked);
            }
        });

        //Setup SeekBar
        mSeekBar = (SeekBar) findViewById(R.id.seekBar);
        mSeekBar.setMax(100);
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mCircleView.setValueAnimated(seekBar.getProgress(), 1500);
                mSwitchSpin.setChecked(false);
            }
        });

        mSeekBarSpinnerLength = (SeekBar) findViewById(R.id.seekBar2);
        mSeekBarSpinnerLength.setMax(360);
        mSeekBarSpinnerLength.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mCircleView.setSpinningBarLength(seekBar.getProgress());
            }
        });


    }



    @Override
    protected void onStart() {
        super.onStart();
        mCircleView.setValue(0);
        mCircleView.setValueAnimated(42);
    }

}

