package com.babyguard.babyone;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.mbientlab.metawear.Route;
import com.mbientlab.metawear.UnsupportedModuleException;
import com.babyguard.babyone.help.HelpOptionAdapter;
import com.mbientlab.metawear.module.Led;
import com.mbientlab.metawear.module.Led.Color;
import com.mbientlab.metawear.module.Switch;

import java.util.Locale;

import bolts.Task;

public class HomeFragment extends ModuleFragmentBase {
    private Led ledModule;
    private int switchRouteId = -1;

    public static class MetaBootWarningFragment extends DialogFragment {
        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity()).setTitle(R.string.title_warning)
                    .setPositiveButton(R.string.label_ok, null)
                    .setCancelable(false)
                    .setMessage(R.string.message_metaboot)
                    .create();
        }
    }

    public HomeFragment() {
        super(R.string.navigation_fragment_home);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {
        setRetainInstance(true);
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.led_red_on).setOnClickListener(view1 -> {
            configureChannel(ledModule.editPattern(Color.RED));
            ledModule.play();
        });
        view.findViewById(R.id.led_green_on).setOnClickListener(view12 -> {
            configureChannel(ledModule.editPattern(Color.GREEN));
            ledModule.play();
        });
        view.findViewById(R.id.led_blue_on).setOnClickListener(view13 -> {
            configureChannel(ledModule.editPattern(Color.BLUE));
            ledModule.play();
        });
        view.findViewById(R.id.led_stop).setOnClickListener(view14 -> ledModule.stop(true));
        /*view.findViewById(R.id.board_rssi_text).setOnClickListener(v -> mwBoard.readRssiAsync()
                .continueWith(task -> {
                    ((TextView) view.findViewById(R.id.board_rssi_value)).setText(String.format(Locale.US, "%d dBm", task.getResult()));
                    return null;
                }, Task.UI_THREAD_EXECUTOR)
        ); */
        view.findViewById(R.id.board_battery_level_text).setOnClickListener(v -> mwBoard.readBatteryLevelAsync()
                .continueWith(task -> {
                    ((TextView) view.findViewById(R.id.board_battery_level_value)).setText(String.format(Locale.US, "%d", task.getResult()));
                    return null;
                }, Task.UI_THREAD_EXECUTOR)
        );
       /* view.findViewById(R.id.update_firmware).setOnClickListener(view15 -> mwBoard.checkForFirmwareUpdateAsync()
                .continueWith(task -> {
                    if (task.isFaulted()) {
                        Snackbar.make(getActivity().findViewById(R.id.drawer_layout), task.getError().getLocalizedMessage(), Snackbar.LENGTH_LONG).show();
                    } else {
                        setupDfuDialog(new AlertDialog.Builder(getActivity()), task.getResult() ? R.string.message_dfu_accept : R.string.message_dfu_latest);
                    }
                    return null;
                }, Task.UI_THREAD_EXECUTOR)
        ); */
    }

    private void setupDfuDialog(AlertDialog.Builder builder, int msgResId) {
        builder.setTitle(R.string.title_firmware_update)
                .setPositiveButton(R.string.label_yes, (dialogInterface, i) -> fragBus.initiateDfu(null))
                .setNegativeButton(R.string.label_no, null)
                .setCancelable(false)
                .setMessage(msgResId)
                .show();
    }

    @Override
    protected void boardReady() throws UnsupportedModuleException {
        setupFragment(getView());
    }

    @Override
    protected void fillHelpOptionAdapter(HelpOptionAdapter adapter) {

    }

    @Override
    public void reconnected() {
        setupFragment(getView());
    }

    private void configureChannel(Led.PatternEditor editor) {
        final short PULSE_WIDTH= 1000;
        editor.highIntensity((byte) 31).lowIntensity((byte) 31)
                .highTime((short) (PULSE_WIDTH >> 1)).pulseDuration(PULSE_WIDTH)
                .repeatCount((byte) -1).commit();
    }

    private void setupFragment(final View v) {
        final String METABOOT_WARNING_TAG= "metaboot_warning_tag";

        if (!mwBoard.isConnected()) {
            return;
        }

        if (mwBoard.inMetaBootMode()) {
            if (getFragmentManager().findFragmentByTag(METABOOT_WARNING_TAG) == null) {
                new MetaBootWarningFragment().show(getFragmentManager(), METABOOT_WARNING_TAG);
            }
        } else {
            DialogFragment metabootWarning= (DialogFragment) getFragmentManager().findFragmentByTag(METABOOT_WARNING_TAG);
            if (metabootWarning != null) {
                metabootWarning.dismiss();
            }
        }

        mwBoard.readDeviceInformationAsync().continueWith(task -> {
            if (task.getResult() != null) {
               /* ((TextView) v.findViewById(R.id.manufacturer_value)).setText(task.getResult().manufacturer);
                ((TextView) v.findViewById(R.id.model_number_value)).setText(task.getResult().modelNumber);
                ((TextView) v.findViewById(R.id.serial_number_value)).setText(task.getResult().serialNumber);
                ((TextView) v.findViewById(R.id.firmware_revision_value)).setText(task.getResult().firmwareRevision);
                ((TextView) v.findViewById(R.id.hardware_revision_value)).setText(task.getResult().hardwareRevision);
                ((TextView) v.findViewById(R.id.device_mac_address_value)).setText(mwBoard.getMacAddress()); */

                ((TextView) v.findViewById(R.id.manufacturer_value)).setText("BoduGuard Team");
                ((TextView) v.findViewById(R.id.model_number_value)).setText("1");
                ((TextView) v.findViewById(R.id.serial_number_value)).setText("000000");
                ((TextView) v.findViewById(R.id.firmware_revision_value)).setText("1.0.0");
                ((TextView) v.findViewById(R.id.hardware_revision_value)).setText("0.1");
                ((TextView) v.findViewById(R.id.device_mac_address_value)).setText(mwBoard.getMacAddress());
            }

            return null;
        }, Task.UI_THREAD_EXECUTOR);

        /*Switch switchModule;
        if ((switchModule= mwBoard.getModule(Switch.class)) != null) {
            Route oldSwitchRoute;
            if ((oldSwitchRoute = mwBoard.lookupRoute(switchRouteId)) != null) {
                oldSwitchRoute.remove();
            }

           switchModule.state().addRouteAsync(source ->
                    source.stream((data, env) -> getActivity().runOnUiThread(() -> {
                        RadioGroup radioGroup = (RadioGroup) v.findViewById(R.id.switch_radio_group);

                        if (data.value(Boolean.class)) {
                            radioGroup.check(R.id.switch_radio_pressed);
                            v.findViewById(R.id.switch_radio_pressed).setEnabled(true);
                            v.findViewById(R.id.switch_radio_released).setEnabled(false);
                        } else {
                            radioGroup.check(R.id.switch_radio_released);
                            v.findViewById(R.id.switch_radio_released).setEnabled(true);
                            v.findViewById(R.id.switch_radio_pressed).setEnabled(false);
                        }
                    }))
            ).continueWith(task -> switchRouteId = task.getResult().id());
        }*/

        int[] ledResIds= new int[] {R.id.led_stop, R.id.led_red_on, R.id.led_green_on, R.id.led_blue_on};
        if ((ledModule = mwBoard.getModule(Led.class)) != null) {
            for(int id: ledResIds) {
                v.findViewById(id).setEnabled(true);
            }
        } else {
            for(int id: ledResIds) {
                v.findViewById(id).setEnabled(false);
            }
        }
    }
}
