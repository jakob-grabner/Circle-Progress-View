package at.grabner.circleview;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;

import at.grabner.circleprogress.CircleProgressView;


public class MainActivity extends ActionBarActivity {


    CircleProgressView mCircleView;
    Switch mSwitch;
    SeekBar mSeekBar;
    SeekBar mSeekBarSpinnerLength;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mCircleView = (CircleProgressView) findViewById(R.id.circlview);
        mCircleView.setMaxValue(100);



        mSwitch = (Switch) findViewById(R.id.switch1);
        mSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mCircleView.spin();
                }else{
                    mCircleView.stopSpinning();
                }
            }
        });


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
                mCircleView.setValueAnimated(seekBar.getProgress(),1500);
                mSwitch.setChecked(false);
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
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
//        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }

        return super.onOptionsItemSelected(item);
    }
}
