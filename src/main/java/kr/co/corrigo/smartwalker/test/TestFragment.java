/*
 * Copyright (c) 2015, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package kr.co.corrigo.smartwalker.test;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import kr.co.corrigo.smartwalker.common.BoardHelpFragment;
import kr.co.corrigo.smartwalker.setting.SettingsActivity;
import no.nordicsemi.android.beacon.Beacon;
import no.nordicsemi.android.beacon.BeaconRegion;
import no.nordicsemi.android.beacon.BeaconServiceConnection;
import no.nordicsemi.android.beacon.ServiceProxy;
import kr.co.corrigo.smartwalker.MainActivity;
import kr.co.corrigo.smartwalker.R;
import kr.co.corrigo.smartwalker.common.BaseFragment;
import kr.co.corrigo.smartwalker.database.BeaconContract;
import kr.co.corrigo.smartwalker.database.DatabaseHelper;
import kr.co.corrigo.smartwalker.beacon.BeaconScannerFragment;
import kr.co.corrigo.smartwalker.beacon.BeaconsDetailsActivity;

public class TestFragment extends BaseFragment {
    private static final String BEACONS_FRAGMENT = "testFragment";
    private static final String SCANNER_FRAGMENT = "scannerFragment";

    private static final int BASE_RESOLUSION_WIDTH = 1440 ;
    private static final int BASE_RESOLUSION_HEIGHT = 2530 ;

    /** Nordic Semiconductor ASA company ID. This parameter may be skipped as it's default. */
    public static final int BEACON_COMPANY_ID = 0x0059;

    private DatabaseHelper mDatabaseHelper;
    private TestListFragment mBeaconsListFragment;
    private BeaconScannerFragment mScannerFragment;

    private boolean mServiceConnected;
    private boolean mFragmentResumed;

    private int fragmentWidth;
    private int fragmentHeight;

    private BeaconServiceConnection mServiceConnection = new BeaconServiceConnection() {
        @Override
        public void onServiceConnected() {
            mServiceConnected = true;

            startScanning();
            final BeaconScannerFragment scannerFragment = mScannerFragment;
            if (scannerFragment != null) {
                startRangingBeaconsInRegion(BEACON_COMPANY_ID, BeaconRegion.ANY_UUID, scannerFragment);
            } else {
                final FragmentManager fm = getChildFragmentManager();
                if (fm.getBackStackEntryCount() == 0) {
                    // Start scan only if there is no any other fragment (Mona Lisa) open
                    startScanning();
                }
            }
        }

        @Override
        public void onServiceDisconnected() {
            mServiceConnected = false;
        }
    };

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Log.i("LifeCycyle", "Test Fragment onActivityCreated");
        // Check if beacons list fragment already exists

        TestListFragment fragment = (TestListFragment) getChildFragmentManager().findFragmentByTag(BEACONS_FRAGMENT);
        if (fragment == null) {
            fragment = new TestListFragment();
            final FragmentTransaction ft = getChildFragmentManager().beginTransaction();
            ft.add(R.id.content, fragment, BEACONS_FRAGMENT);
            ft.commit();
        }
        mBeaconsListFragment = fragment;

        // Restore the scanner fragment if it was opened
        mScannerFragment = (BeaconScannerFragment) getChildFragmentManager().findFragmentByTag(SCANNER_FRAGMENT);

    }

    @Override
    public void onAttach(final Context context) {
        super.onAttach(context);
        Log.i("LifeCycyle", "Test Fragment onAttach");
        final MainActivity parent = (MainActivity) context;
        parent.setTestFragment(this);
    }

    /**
     * As the fragment is in the ViewPager the onResume() and onPause() methods are called when it is added to the cache, even if it is not visible.
     * By using {@link #onFragmentResumed()} and {@link #onFragmentPaused()}, which are called by the {@link ViewPager.OnPageChangeListener} we ensure
     * that the fragment is visible.
     */
    public void onFragmentResumed() {
        if (mFragmentResumed)
            return;
        Log.i("LifeCycyle", "Test Fragment resumed" );
        mFragmentResumed = true;
        bindService();
    }

    /**
     * @see #onFragmentResumed()
     */
    public void onFragmentPaused() {
        if (!mFragmentResumed)
            return;
        Log.i("LifeCycyle", "Test Fragment puased");
        mFragmentResumed = false;
        unbindService();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        final MainActivity parent = (MainActivity) getActivity();
        parent.setTestFragment(null);
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        Log.i("LifeCycyle", "Test Fragment onCreate");
        mDatabaseHelper = new DatabaseHelper(getActivity());
        fragmentWidth = BASE_RESOLUSION_WIDTH;
        fragmentHeight = BASE_RESOLUSION_HEIGHT;
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {

        final View view = inflater.inflate(R.layout.fragment_container, container, false);
        Log.i("LifeCycyle", "Test Fragment onCreateView");
        view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {

                //뷰의 생성된 후 크기와 위치 구하기
                fragmentWidth = view.getWidth();
                fragmentHeight = view.getHeight();
                Log.i("Container_onCreateView", "width = " + fragmentWidth + "height = "+ fragmentHeight );

                //리스너 해제
                view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

        return view;
    }

    public int getFragmentWidth(){
        return fragmentWidth;
    }

    public int getFragmentHeight(){
        return fragmentHeight;
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        inflater.inflate(R.menu.settings, menu);
      //  inflater.inflate(R.menu.about, menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        final int id = item.getItemId();
        switch (id) {
            case R.id.action_settings:
                final Intent intent = new Intent(getActivity(), SettingsActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);

                final Cursor cursor = mDatabaseHelper.getAllRegions();
                int i =0;
                int size = cursor.getCount();
                CharSequence[] beaconsList = new  CharSequence[size+1];
                beaconsList[i++] = "미지정";
                while (cursor.moveToNext()) {
                    final CharSequence name;
                    name = cursor.getString(1 /* NAME */);
                    beaconsList[i++] = name;
                }

                intent.putExtra("beaconsList", beaconsList);
                startActivity(intent);
                return true;
           /* case R.id.action_about:
                final BoardHelpFragment helpFragment = BoardHelpFragment.getInstance(BoardHelpFragment.MODE_DFU);
                helpFragment.show(getChildFragmentManager(), null);
                return true;*/
        }
        return false;
    }

    @Override
    protected void onPermissionGranted() {
        // Now, when the permission is granted, we may start scanning for beacons.
        // We bind even if the FAB was clicked.
        bindService();
    }

    /**
     * Opens the configuration screen for the region with given id.
     *
     * @param id
     *            the id of the region in the DB
     */
    public void onEditRegion(final long id) {
        final Intent intent = new Intent(getActivity(), BeaconsDetailsActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        intent.putExtra(BeaconsDetailsActivity.ID, id);
        startActivity(intent);
    }

    /**
     * Opens the scanned fragment. Starts ranging for any beacon in immediate position.
     */
    public void onAddOrEditRegion() {
        if (!ensurePermission())
            return;

        stopScanning();

        final BeaconScannerFragment fragment = mScannerFragment = new BeaconScannerFragment();
        fragment.show(getChildFragmentManager(), SCANNER_FRAGMENT);

        //mServiceConnection.startRangingBeaconsInRegion(BeaconRegion.ANY_UUID, fragment);
        mServiceConnection.startRangingBeaconsInRegion(BEACON_COMPANY_ID, BeaconRegion.ANY_UUID, fragment);
    }

    /**
     * Stops ranging for any beacon in immediate position. Opens configuration for selected beacon. It's being added if didn't exist before.
     *
     * @param beacon
     *            the beacon that was tapped
     */
    public void onScannerClosedWithResult(final Beacon beacon) {
        mServiceConnection.stopRangingBeaconsInRegion(mScannerFragment);
        mScannerFragment = null;

        final Cursor cursor = mDatabaseHelper.findRegionByBeacon(beacon);
        try {
            long id=0;
            if (cursor.moveToNext()) {
                // Update beacon
                id = cursor.getLong(0);
            } else {
                // Add new beacon
                //id = mDatabaseHelper.addRegion(beacon, getString(R.string.default_beacon_name), BeaconContract.EVENT_GET_NEAR, BeaconContract.ACTION_MONA_LISA, null);
                id = mDatabaseHelper.addRegion(beacon, beacon.getDeviceAddress(), BeaconContract.EVENT_GET_NEAR, BeaconContract.ACTION_MONA_LISA, null);
            }
            onEditRegion(id);
        } finally {
            cursor.close();
        }
    }

    /**
     * Stops ranging for any beacons in immediate position and starts for added beacons.
     */
    public void onScannerClosed() {
        mServiceConnection.stopRangingBeaconsInRegion(mScannerFragment);
        mScannerFragment = null;

        startScanning();
    }

    /**
     * Starts scanning for added beacons if the service is connected.
     */
    public void startScanning() {
        if (mServiceConnected) {
            mBeaconsListFragment.startScanning(mServiceConnection);
        }
    }

    /**
     * Stops scanning for added beacons if the service is connected.
     */
    public void stopScanning() {
        if (mServiceConnected) {
            mBeaconsListFragment.stopScanning(mServiceConnection);
        }
    }

    /**
     * Binds the app with the beacons service. If it's not installed on Android 4.3 or 4.4 it asks for download. On Android 5+ the service is built into the beacon application.
     */
    private void bindService() {
        if (!ensurePermission())
            return;

        Log.i("BIND_CHK", "TestFrag bind begin" );

        final boolean success = ServiceProxy.bindService(getActivity(), mServiceConnection);

        Log.i("BIND_CHK", "TestFrag bind end" );
        if (!success) {
            new AlertDialog.Builder(getActivity()).setTitle(R.string.service_required_title).setMessage(R.string.service_required_message)
                    .setPositiveButton(R.string.service_required_store, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(final DialogInterface dialog, final int which) {
                            final Intent playIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(MainActivity.NRF_BEACON_SERVICE_URL));
                            startActivity(playIntent);
                        }
                    }).setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(final DialogInterface dialog) {
                    dialog.dismiss();
                    getActivity().finish();
                }
            }).show();
        }
    }

    private void unbindService() {
        Log.i("BIND_CHK", "TestFrag unbind begin" );

        if (mServiceConnected) {
            // Unbinding service will stop all active scanning listeners
            ServiceProxy.unbindService(getActivity(), mServiceConnection);
            mDatabaseHelper.resetSignalStrength();
        }
        Log.i("BIND_CHK", "TestFrag unbind end" );
    }

    /**
     * Returns the database helper object.
     *
     * @return the database helper which may be used to access DB
     */
    public DatabaseHelper getDatabaseHelper() {
        return mDatabaseHelper;
    }
}
