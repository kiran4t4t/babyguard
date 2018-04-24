package com.babyguard.babyone;

import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.mbientlab.metawear.UnsupportedModuleException;
import com.babyguard.babyone.help.HelpOption;
import com.babyguard.babyone.help.HelpOptionAdapter;
import com.mbientlab.metawear.module.Debug;
import com.mbientlab.metawear.module.Settings;

import bolts.Task;

public class SettingsFragment extends ModuleFragmentBase {
    private final static int[] CONFIG_WRAPPERS;

    static {
        CONFIG_WRAPPERS = new int[] {
                R.id.settings_ad_name_wrapper, R.id.settings_ad_interval_wrapper, R.id.settings_ad_timeout_wrapper, R.id.settings_tx_power_wrapper
        };
    }

    private Settings settingsModule;
    private Debug debugModule;

    private String deviceName;
    private int adInterval;
    private short timeout;
    private byte txPower;

    public SettingsFragment() {
        super(R.string.navigation_fragment_settings);
    }

    @Override
    protected void boardReady() throws UnsupportedModuleException {
        debugModule= mwBoard.getModuleOrThrow(Debug.class);
        settingsModule= mwBoard.getModuleOrThrow(Settings.class);

        settingsModule.readBleAdConfigAsync()
                .continueWith(task -> {
                    final int[] configEditText= new int[] {
                            R.id.settings_ad_name_value, R.id.settings_ad_interval_value, R.id.settings_ad_timeout_value, R.id.settings_tx_power_value
                    };
                    Object[] values= new Object[] {task.getResult().deviceName, task.getResult().interval, task.getResult().timeout, task.getResult().txPower};

                    for (int i= 0; i < values.length; i++) {
                        ((EditText) getView().findViewById(configEditText[i])).setText(values[i].toString());
                    }

                    return null;
                }, Task.UI_THREAD_EXECUTOR);
    }

    @Override
    protected void fillHelpOptionAdapter(HelpOptionAdapter adapter) {
        adapter.add(new HelpOption(R.string.config_name_ad_device_name, R.string.config_desc_ad_device_name));
        adapter.add(new HelpOption(R.string.config_name_ad_timeout, R.string.config_desc_ad_timeout));
        adapter.add(new HelpOption(R.string.config_name_ad_tx, R.string.config_desc_ad_tx));
        adapter.add(new HelpOption(R.string.config_name_ad_interval, R.string.config_desc_ad_interval));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        setRetainInstance(true);
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        View settingsControl= view.findViewById(R.id.settings_control);

        Button enableBtn= (Button) settingsControl.findViewById(R.id.layout_two_button_left);
        enableBtn.setText(R.string.label_save);
        enableBtn.setOnClickListener(innerView -> {
            TextInputLayout[] inputLayouts = new TextInputLayout[CONFIG_WRAPPERS.length];
            for (int i = 0; i < CONFIG_WRAPPERS.length; i++) {
                inputLayouts[i] = (TextInputLayout) view.findViewById(CONFIG_WRAPPERS[i]);
            }

            boolean valid = true;
            deviceName = ((EditText) view.findViewById(R.id.settings_ad_name_value)).getText().toString();

            try {
                adInterval = Integer.valueOf(((EditText) view.findViewById(R.id.settings_ad_interval_value)).getText().toString());
                inputLayouts[1].setError(null);
            } catch (Exception e) {
                valid = false;
                inputLayouts[1].setError(e.getLocalizedMessage());
            }

            try {
                timeout = Short.valueOf(((EditText) view.findViewById(R.id.settings_ad_timeout_value)).getText().toString());
                inputLayouts[2].setError(null);
            } catch (Exception e) {
                valid = false;
                inputLayouts[2].setError(e.getLocalizedMessage());
            }

            try {
                txPower = Byte.valueOf(((EditText) view.findViewById(R.id.settings_tx_power_value)).getText().toString());
                inputLayouts[3].setError(null);
            } catch (Exception e) {
                valid = false;
                inputLayouts[3].setError(e.getLocalizedMessage());
            }

            if (valid) {
                settingsModule.editBleAdConfig()
                        .deviceName(deviceName)
                        .timeout((byte) (timeout & 0xff))
                        .interval((short) (adInterval & 0xffff))
                        .txPower(txPower)
                        .commit();
            }
        });

        Button disableBtn= (Button) settingsControl.findViewById(R.id.layout_two_button_right);
        disableBtn.setText(R.string.label_clear);
        disableBtn.setOnClickListener(view1 -> debugModule.resetAfterGc());
    }
}
