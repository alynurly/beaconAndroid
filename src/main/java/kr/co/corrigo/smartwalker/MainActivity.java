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
package kr.co.corrigo.smartwalker;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Toast;


import kr.co.corrigo.smartwalker.analysis.AnalysisFragment;
import kr.co.corrigo.smartwalker.blog.BlogFragment;
import kr.co.corrigo.smartwalker.common.CustomViewPager;
import kr.co.corrigo.smartwalker.test.TestFragment;
import kr.co.corrigo.smartwalker.statistics.StatisticsFragment;
import kr.co.corrigo.smartwalker.beacon.BeaconsFragment;
import kr.co.corrigo.smartwalker.database.DatabaseHelper;


public class MainActivity extends AppCompatActivity {
	public static final String NRF_BEACON_SERVICE_URL = "market://details?id=no.nordicsemi.android.beacon.service";
	public static final String OPENED_FROM_LAUNCHER = "no.nordicsemi.android.nrfbeacon.extra.opened_from_launcher";
	public static final String EXTRA_OPEN_DFU = "no.nordicsemi.android.nrfbeacon.extra.open_dfu";

	private static final int REQUEST_ENABLE_BT = 1;

	private BeaconsFragment mBeaconsFragment;
	private TestFragment mTestFragment = null;
	private AnalysisFragment mAnalysisFragment = null;
	private DatabaseHelper mDatabaseHelper;

	// 뒤로가기 버튼 입력시간이 담길 long 객체
	private long pressedTime = 0;
    private CustomViewPager pager;

	// 리스너 생성
	public interface OnBackPressedListener {
		public void onBack();
	}

	// 리스너 객체 생성
	private OnBackPressedListener mBackListener;

	// 리스너 설정 메소드
	public void setOnBackPressedListener(OnBackPressedListener listener) {
		mBackListener = listener;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// Ensure that Bluetooth exists
		if (!ensureBleExists())
			finish();

		// Setup the custom toolbar
		final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

        // Setup the FloatingActionButton (FAB)
        final FloatingActionButton fabAdd = (FloatingActionButton) findViewById(R.id.fab_add);
        fabAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                //HSYOO add
                //	if (mBeaconsFragment != null)
                //		mBeaconsFragment.onAddOrEditRegion();
                if (mTestFragment != null)
                    mTestFragment.onAddOrEditRegion();
            }
        });

        // Prepare the sliding tab layout and the view pager
        final TabLayout tabLayout = (TabLayout) findViewById(R.id.sliding_tabs);
        //pager = (ViewPager) findViewById(R.id.view_pager);
		pager = (CustomViewPager) findViewById(R.id.view_pager);
		pager.setOffscreenPageLimit(1);

        pager.setAdapter(new FragmentAdapter(getSupportFragmentManager()));
        tabLayout.setupWithViewPager(pager);

        pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            @Override
            public void onPageSelected(final int position) {
				Log.i("LifeCycyle", "onPageSelected " + position );
                if (position == 1) { //HSYOO add
                    fabAdd.show();
                    if (mTestFragment != null)
                        mTestFragment.onFragmentResumed();
                    if (mAnalysisFragment != null)
                        mAnalysisFragment.onFragmentPaused();
                } else if(position == 2) { //HSYOO add
                    fabAdd.show();
                    if (mAnalysisFragment != null)
                        mAnalysisFragment.onFragmentResumed();
                    if (mTestFragment != null)
                        mTestFragment.onFragmentPaused();
                } else {
                    fabAdd.hide();
                    //HSYOO add
                    if (mTestFragment != null)
                        mTestFragment.onFragmentPaused();
                    //HSYOO add
                    if (mAnalysisFragment != null)
                        mAnalysisFragment.onFragmentPaused();
                }
            }

            @Override
            public void onPageScrolled(final int position, final float positionOffset, final int positionOffsetPixels) {
                // empty
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                //	if (state != ViewPager.SCROLL_STATE_IDLE)
                //		fabAdd.hide();
                //	else if (pager.getCurrentItem() == 0)
                //	fabAdd.show();
            }
        });

        pager.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Log.i("MainActivity", "pager height" + pager.getMeasuredHeight() + "pager width" +  pager.getMeasuredWidth());
                pager.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

		pager.setCurrentItem(3);

		pager.postDelayed(new Runnable() {

			@Override
			public void run() {
				pager.setCurrentItem(0);
			}
		}, 1);

	}

	/*@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		MenuInflater inflater=getMenuInflater();
		inflater.inflate(R.menu.settings, menu);
		return true;
	}*/

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			onBackPressed();
			return true;
/*
		case R.id.action_settings:
			final Intent intent = new Intent(this, DfuSettingsFragment.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
			startActivity(intent);
			return true;*/
		}
		return false;
	}

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
	protected void onResume() {
		super.onResume();

		if (!isBleEnabled())
			enableBle();

		// we are in main fragment, show 'home up' if entered from Launcher (splash screen activity)
		final boolean openedFromLauncher = getIntent().getBooleanExtra(MainActivity.OPENED_FROM_LAUNCHER, false);
		getSupportActionBar().setDisplayHomeAsUpEnabled(!openedFromLauncher);
	}

	@Override
	protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
		switch (requestCode) {
		case REQUEST_ENABLE_BT:
			if (resultCode == RESULT_OK) {
				// empty?
			} else
				finish();
			break;
		default:
			super.onActivityResult(requestCode, resultCode, data);
		}
	}

	public void setBeaconsFragment(final BeaconsFragment fragment) {
		if (fragment == null && mBeaconsFragment != null)
			mBeaconsFragment.onFragmentPaused();
		if (fragment != null)
			fragment.onFragmentResumed();
		mBeaconsFragment = fragment;
	}

	//HSYOO add
	public void setTestFragment(final TestFragment fragment) {
		if (fragment == null && mTestFragment != null)
			mTestFragment.onFragmentPaused();
		if (fragment != null)
			fragment.onFragmentResumed();
		mTestFragment = fragment;
	}

	//HSYOO add
	public void setAnalysisFragment(final AnalysisFragment fragment) {
		if (fragment == null && mAnalysisFragment != null)
			mAnalysisFragment.onFragmentPaused();
		if (fragment != null)
			fragment.onFragmentResumed();
		mAnalysisFragment = fragment;
	}

	public void setDatabaseHelper(final DatabaseHelper helper) {
		if(mDatabaseHelper != null)
			mDatabaseHelper =helper;
	}

	public DatabaseHelper getDatabaseHelper() {
			return mDatabaseHelper;
	}
	/**
	 * Checks whether the device supports Bluetooth Low Energy communication
	 * 
	 * @return <code>true</code> if BLE is supported, <code>false</code> otherwise
	 */
	private boolean ensureBleExists() {
		if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
			Toast.makeText(this, R.string.no_ble, Toast.LENGTH_LONG).show();
			return false;
		}
		return true;
	}

	/**
	 * Checks whether the Bluetooth adapter is enabled.
	 */
	private boolean isBleEnabled() {
		final BluetoothManager bm = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
		final BluetoothAdapter ba = bm.getAdapter();
		return ba != null && ba.isEnabled();
	}

	/**
	 * Tries to start Bluetooth adapter.
	 */
	private void enableBle() {
		final Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
	}

	private class FragmentAdapter extends FragmentPagerAdapter {

	    public FragmentAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			switch (position) {
				case 0:
					return new TestFragment();
				case 1:
					return new AnalysisFragment();
				case 2:
					return new StatisticsFragment();
				default:
					return new TestFragment();
//				case 0:
//					return new BlogFragment();
			}
		}

		@Override
		public int getCount() {
			return 3;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			return getResources().getStringArray(R.array.tab_title)[position];
		}

	}

	@Override
	public void onBackPressed() {

		// 다른 Fragment 에서 리스너를 설정했을 때 처리됩니다.
		if(mBackListener != null) {
			mBackListener.onBack();
			Log.e("!!!", "Listener is not null");
			// 리스너가 설정되지 않은 상태(예를들어 메인Fragment)라면
			// 뒤로가기 버튼을 연속적으로 두번 눌렀을 때 앱이 종료됩니다.
		} else {
			Log.e("!!!", "Listener is null");
			if ( pressedTime == 0 ) {
				Snackbar.make(findViewById(R.id.container),
						"Press again to exit." , Snackbar.LENGTH_LONG).show();
				pressedTime = System.currentTimeMillis();
			}
			else {
				int seconds = (int) (System.currentTimeMillis() - pressedTime);

				if ( seconds > 2000 ) {
					Snackbar.make(findViewById(R.id.container),
							"Press again to exit." , Snackbar.LENGTH_LONG).show();
					pressedTime = 0 ;
				}
				else {
					super.onBackPressed();
					Log.e("!!!", "onBackPressed : finish, killProcess");
					finish();
					android.os.Process.killProcess(android.os.Process.myPid());
				}
			}
		}
	}
}
