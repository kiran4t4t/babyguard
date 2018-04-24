package com.babyguard.babyone;

import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;

import com.mbientlab.metawear.UnsupportedModuleException;
import com.babyguard.babyone.help.HelpOption;
import com.babyguard.babyone.help.HelpOptionAdapter;
import com.mbientlab.metawear.module.BarometerBosch;
import com.mbientlab.metawear.module.Temperature;
import com.mbientlab.metawear.module.Temperature.SensorType;
import com.mbientlab.metawear.module.Timer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class TemperatureFragment extends SingleDataSensorFragment {
    private static final int TEMP_SAMPLE_PERIOD= 33, SINGLE_EXT_THERM_INDEX= 1;

    private byte gpioDataPin= 0, gpioPulldownPin= 1;
    private boolean activeHigh= false;

    private long startTime= -1;
    private Temperature tempModule;
    private Timer timerModule;
    private Timer.ScheduledTask scheduledTask;
    private List<String> spinnerEntries= null;
    private int selectedSourceIndex= 0;

    private Spinner sourceSelector;

    public TemperatureFragment() {
        super(R.string.navigation_fragment_temperature, "celsius", R.layout.fragment_temperature, TEMP_SAMPLE_PERIOD / 1000.f, 15, 45);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sourceSelector= (Spinner) view.findViewById(R.id.temperature_source);
        sourceSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View innerView, int position, long id) {
                if (tempModule.sensors()[position].type() == SensorType.BOSCH_ENV) {
                    try {
                        mwBoard.getModuleOrThrow(BarometerBosch.class).start();
                    } catch (UnsupportedModuleException e) {
                        view.findViewById(R.id.sample_control).setEnabled(false);

                        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                        builder.setTitle(R.string.title_error);
                        builder.setMessage(R.string.message_no_bosch_barometer);
                        builder.setPositiveButton(android.R.string.ok, null);
                        builder.show();
                    }
                } else {
                    try {
                        mwBoard.getModuleOrThrow(BarometerBosch.class).stop();
                    } catch (UnsupportedModuleException e) {
                        e.printStackTrace();
                    }
                    view.findViewById(R.id.sample_control).setEnabled(true);
                }

                int[] extThermResIds = new int[]{R.id.ext_thermistor_data_pin_wrapper, R.id.ext_thermistor_pulldown_pin_wrapper,
                        R.id.ext_thermistor_active_setting_title, R.id.ext_thermistor_active_setting
                };
                for (int resId : extThermResIds) {
                    view.findViewById(resId).setVisibility(tempModule.sensors()[position].type() == SensorType.EXT_THERMISTOR ? VISIBLE : GONE);
                }

                selectedSourceIndex = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        if (spinnerEntries != null) {
            final ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, spinnerEntries);
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            sourceSelector.setAdapter(spinnerAdapter);
            sourceSelector.setSelection(selectedSourceIndex);
        }

        final EditText extThermPinText= (EditText) view.findViewById(R.id.ext_thermistor_data_pin);
        extThermPinText.setText(String.format(Locale.US, "%d", gpioDataPin));
        extThermPinText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                TextInputLayout extThermWrapper = (TextInputLayout) view.findViewById(R.id.ext_thermistor_data_pin_wrapper);
                try {
                    gpioDataPin = Byte.valueOf(s.toString());
                    view.findViewById(R.id.sample_control).setEnabled(true);
                    extThermWrapper.setError(null);
                } catch (Exception e) {
                    view.findViewById(R.id.sample_control).setEnabled(false);
                    extThermWrapper.setError(e.getLocalizedMessage());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        final EditText pulldownPinText= (EditText) view.findViewById(R.id.ext_thermistor_pulldown_pin);
        pulldownPinText.setText(String.format(Locale.US, "%d", gpioPulldownPin));
        pulldownPinText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                TextInputLayout extThermWrapper = (TextInputLayout) view.findViewById(R.id.ext_thermistor_pulldown_pin_wrapper);

                try {
                    gpioPulldownPin = Byte.valueOf(s.toString());
                    view.findViewById(R.id.sample_control).setEnabled(true);
                    extThermWrapper.setError(null);
                } catch (Exception e) {
                    view.findViewById(R.id.sample_control).setEnabled(false);
                    extThermWrapper.setError(e.getLocalizedMessage());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        final Spinner activeSelections= (Spinner) view.findViewById(R.id.ext_thermistor_active_setting);
        activeSelections.setSelection(activeHigh ? 1 : 0);
        activeSelections.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                activeHigh = (position != 0);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    @Override
    protected void boardReady() throws UnsupportedModuleException {
        timerModule= mwBoard.getModuleOrThrow(Timer.class);
        tempModule= mwBoard.getModuleOrThrow(Temperature.class);

        spinnerEntries = new ArrayList<>();
        for (Temperature.Sensor it: tempModule.sensors()) {
            spinnerEntries.add(it.type().toString()
            );
        }

        final ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, spinnerEntries);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sourceSelector.setAdapter(spinnerAdapter);
        sourceSelector.setSelection(selectedSourceIndex);
    }

    @Override
    protected void fillHelpOptionAdapter(HelpOptionAdapter adapter) {
        adapter.add(new HelpOption(R.string.config_name_temp_source, R.string.config_desc_temp_source));
        adapter.add(new HelpOption(R.string.config_name_temp_active, R.string.config_desc_temp_active));
        adapter.add(new HelpOption(R.string.config_name_temp_data_pin, R.string.config_desc_temp_data_pin));
        adapter.add(new HelpOption(R.string.config_name_temp_pulldown_pin, R.string.config_desc_temp_pulldown_pin));
    }

    @Override
    protected void setup() {
        Temperature.Sensor tempSensor = tempModule.sensors()[selectedSourceIndex];
        if (tempSensor.type() == SensorType.EXT_THERMISTOR) {
            ((Temperature.ExternalThermistor) tempModule.sensors()[selectedSourceIndex]).configure(gpioDataPin, gpioPulldownPin, activeHigh);
        }
        tempSensor.addRouteAsync(source -> source.stream((data, env) -> {
            final Float celsius = data.value(Float.class);

            LineData chartData = chart.getData();

            if (startTime == -1) {
                chartData.addXValue("0");
                startTime = System.currentTimeMillis();
            } else {
                chartData.addXValue(String.format(Locale.US, "%.2f", sampleCount * samplingPeriod));
            }
            chartData.addEntry(new Entry(celsius, sampleCount), 0);

            sampleCount++;

            updateChart();
        })).continueWithTask(task -> {
            streamRoute = task.getResult();

            return timerModule.scheduleAsync(TEMP_SAMPLE_PERIOD, false, tempSensor::read);
        }).continueWithTask(task -> {
            scheduledTask = task.getResult();
            scheduledTask.start();

            return null;
        });
    }

    @Override
    protected void clean() {
        scheduledTask.remove();
    }

    @Override
    protected void resetData(boolean clearData) {
        super.resetData(clearData);

        if (clearData) {
            startTime= -1;
        }
    }
}