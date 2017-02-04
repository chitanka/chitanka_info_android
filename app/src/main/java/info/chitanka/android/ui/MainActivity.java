package info.chitanka.android.ui;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.folioreader.activity.FolioActivity;
import com.kobakei.ratethisapp.RateThisApp;

import org.parceler.Parcels;

import java.util.HashMap;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import info.chitanka.android.ChitankaApplication;
import info.chitanka.android.Constants;
import info.chitanka.android.R;
import info.chitanka.android.TrackingConstants;
import info.chitanka.android.components.AnalyticsService;
import info.chitanka.android.di.HasComponent;
import info.chitanka.android.di.presenters.DaggerPresenterComponent;
import info.chitanka.android.di.presenters.PresenterComponent;
import info.chitanka.android.mvp.models.Download;
import info.chitanka.android.ui.dialogs.NetworkRequiredDialog;
import info.chitanka.android.ui.fragments.AuthorsFragment;
import info.chitanka.android.ui.fragments.BaseFragment;
import info.chitanka.android.ui.fragments.CategoriesFragment;
import info.chitanka.android.ui.fragments.my.MyLibraryFragment;
import info.chitanka.android.ui.fragments.newest.NewBooksAndTextworksFragment;
import info.chitanka.android.utils.ConnectivityUtils;

public class MainActivity extends AppCompatActivity implements HasComponent<PresenterComponent>, NavigationView.OnNavigationItemSelectedListener {
    public static final String NETWORK_REQUIRED_DIALOG_FRAGMENT = "NetworkRequiredDialogFragment";
    private static final String KEY_SELECTED_ITEM = "selected_item";

    @Bind(R.id.nav_view)
    NavigationView navigationView;

    @Bind(R.id.app_bar)
    AppBarLayout appBarLayout;

    @Inject
    AnalyticsService analyticsService;

    private int selectedNavItemId = R.id.nav_new;

    private NetworkRequiredDialog networkRequiredDialog;
    private PresenterComponent presenterComponent;
    private BroadcastReceiver readBookReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(Constants.NOTIFICATION_ID_DOWNLOAD);
            Download download = Parcels.unwrap(intent.getParcelableExtra(Constants.EXTRA_DOWNLOAD));
            readFile(download.getFilePath());
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        LocalBroadcastManager.getInstance(this).registerReceiver(readBookReceiver, new IntentFilter(Constants.ACTION_MESSAGE_PROGRESS));

        presenterComponent = DaggerPresenterComponent.builder().applicationComponent(ChitankaApplication.getApplicationComponent()).build();
        getComponent().inject(this);
        ButterKnife.bind(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        if(!ConnectivityUtils.isNetworkAvailable(this)) {
            if (networkRequiredDialog == null && getSupportFragmentManager().findFragmentByTag(NETWORK_REQUIRED_DIALOG_FRAGMENT) == null) {
                networkRequiredDialog = new NetworkRequiredDialog();
                networkRequiredDialog.show(getSupportFragmentManager(), NETWORK_REQUIRED_DIALOG_FRAGMENT);
                analyticsService.logEvent(TrackingConstants.VIEW_NO_NETWORK);
            }
        }

        navigationView.setNavigationItemSelectedListener(this);

        if (savedInstanceState != null && savedInstanceState.containsKey(KEY_SELECTED_ITEM)) {
            selectedNavItemId = savedInstanceState.getInt(KEY_SELECTED_ITEM);
        }

        MenuItem item = navigationView.getMenu().findItem(selectedNavItemId);
        item.setChecked(true);
        onNavigationItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();
        RateThisApp.onStart(this);
        RateThisApp.showRateDialogIfNeeded(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_SELECTED_ITEM, selectedNavItemId);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        final SearchView searchView = (SearchView) MenuItemCompat.getActionView(menu.findItem(R.id.action_search));
        searchView.setQueryHint(getString(R.string.action_search));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                if (!isFinishing()) {
                    analyticsService.logEvent(TrackingConstants.SEARCHED, new HashMap<String, String>() {{
                        put("term", s);
                    }});
                    Intent intent = new Intent(MainActivity.this, SearchActivity.class);
                    intent.putExtra(Constants.EXTRA_SEARCH_TERM, s);
                    startActivity(intent);
                }

                searchView.clearFocus();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                return false;
            }
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if(id == android.R.id.home) {
            finish();
        }


        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ButterKnife.unbind(this);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(readBookReceiver);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        selectedNavItemId = item.getItemId();

        selectNavigationItem(selectedNavItemId);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);

        return true;
    }

    private void selectNavigationItem(int id) {
        BaseFragment fragment = null;
        if (id == R.id.nav_new) {
            fragment = NewBooksAndTextworksFragment.newInstance();
        } else if (id == R.id.nav_authors) {
            fragment = AuthorsFragment.newInstance("");
        } else if (id == R.id.nav_books) {
            fragment = CategoriesFragment.newInstance();
        } else if (id == R.id.nav_my_lib) {
            fragment = MyLibraryFragment.newInstance();
        } else if(id == R.id.nav_readers) {
            startActivity(new Intent(this, ReadersActivity.class));
            return;
        } else if (id == R.id.nav_site) {
            analyticsService.logEvent(TrackingConstants.CLICK_WEBSITE);
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.chitanka.info")));
            return;
        } else if (id == R.id.nav_email) {
            analyticsService.logEvent(TrackingConstants.CLICK_WRITE_US);
            final Intent emailIntent = new Intent(Intent.ACTION_SEND);
            emailIntent.setType("plain/text");
            emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"contact@chitanka.info"});
            startActivity(Intent.createChooser(emailIntent, "Изпрати имейл"));
            return;
        }

        updateAppBarShadow();

        if (fragment == null) {
            return;
        }

        Fragment fragmentByTag = getSupportFragmentManager().findFragmentByTag(fragment.getTitle());
        if(fragmentByTag != null) {
            return;
        }

        getSupportFragmentManager().beginTransaction().replace(R.id.container, fragment, fragment.getTitle()).commit();
    }

    private void updateAppBarShadow() {
        if (selectedNavItemId == R.id.nav_new) {
            appBarLayout.setEnabled(false);
        } else {
            appBarLayout.setEnabled(true);
        }
        appBarLayout.requestLayout();
    }

    private void readFile(String filePath) {
        Intent intent = new Intent(this, FolioActivity.class);
        intent.putExtra(FolioActivity.INTENT_EPUB_SOURCE_TYPE, FolioActivity.EpubSourceType.SD_CARD);
        intent.putExtra(FolioActivity.INTENT_EPUB_SOURCE_PATH, filePath);
        startActivity(intent);
    }

    @Override
    public PresenterComponent getComponent() {
        return presenterComponent;
    }
}
