package com.babyguard.babyone;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.github.mikephil.charting.components.YAxis;

import com.mbientlab.metawear.AsyncDataProducer;
import com.mbientlab.metawear.UnsupportedModuleException;
import com.babyguard.babyone.help.HelpOption;
import com.babyguard.babyone.help.HelpOptionAdapter;
import com.mbientlab.metawear.data.AngularVelocity;
import com.mbientlab.metawear.module.GyroBmi160;
import com.mbientlab.metawear.module.GyroBmi160.*;


public class GyroFragment extends ThreeAxisChartFragment {
    private static final float[] AVAILABLE_RANGES= {125.f, 250.f, 500.f, 1000.f, 2000.f};
    private static final float INITIAL_RANGE= 125.f, GYR_ODR= 25.f;

    private GyroBmi160 gyro = null;
    private int rangeIndex= 0;

    public GyroFragment() {
        super("rotation", R.layout.fragment_sensor_config_spinner,
                R.string.navigation_fragment_gyro, -INITIAL_RANGE, INITIAL_RANGE, GYR_ODR);
    }

    @Override
    protected void boardReady() throws UnsupportedModuleException {
        gyro = mwBoard.getModuleOrThrow(GyroBmi160.class);
    }

    @Override
    protected void fillHelpOptionAdapter(HelpOptionAdapter adapter) {
        adapter.add(new HelpOption(R.string.config_name_gyro_range, R.string.config_desc_gyro_range));
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final YAxis leftAxis = chart.getAxisLeft();

        ((TextView) view.findViewById(R.id.config_option_title)).setText(R.string.config_name_gyro_range);

        Spinner rotationRangeSelection= (Spinner) view.findViewById(R.id.config_option_spinner);
        rotationRangeSelection.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                rangeIndex = position;
                leftAxis.setAxisMaxValue(AVAILABLE_RANGES[rangeIndex]);
                leftAxis.setAxisMinValue(-AVAILABLE_RANGES[rangeIndex]);

                refreshChart(false);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        ArrayAdapter<CharSequence> spinnerAdapter= ArrayAdapter.createFromResource(getContext(), R.array.values_gyro_range, android.R.layout.simple_spinner_item);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        rotationRangeSelection.setAdapter(spinnerAdapter);
        rotationRangeSelection.setSelection(rangeIndex);
    }

    @Override
    protected void setup() {
        Range[] values = Range.values();
        gyro.configure()
                .odr(OutputDataRate.ODR_25_HZ)
                .range(values[values.length - rangeIndex - 1])
                .commit();

        final float period = 1 / GYR_ODR;
        final AsyncDataProducer producer = gyro.packedAngularVelocity() == null ?
                gyro.packedAngularVelocity() :
                gyro.angularVelocity();
        producer.addRouteAsync(source -> source.stream((data, env) -> {
            final AngularVelocity value = data.value(AngularVelocity.class);
            addChartData(value.x(), value.y(), value.z(), period);
            updateChart();
        })).continueWith(task -> {
            streamRoute = task.getResult();

            gyro.angularVelocity().start();
            gyro.start();

            return null;
        });
    }

    @Override
    protected void clean() {
        gyro.stop();

        (gyro.packedAngularVelocity() == null ?
                gyro.packedAngularVelocity() :
                gyro.angularVelocity()
        ).stop();
    }
}
