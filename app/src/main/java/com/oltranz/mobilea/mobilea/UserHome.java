package com.oltranz.mobilea.mobilea;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.IntentCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.orm.SugarRecord;
import com.vistrav.ask.Ask;
import com.vistrav.ask.annotations.AskDenied;
import com.vistrav.ask.annotations.AskGranted;

import java.util.List;

import config.BaseUrl;
import entity.NotificationTable;
import fragments.About;
import fragments.CheckBalance;
import fragments.ContactUs;
import fragments.Favorite;
import fragments.MultipleSell;
import fragments.Notifications;
import fragments.Offers;
import fragments.RechargeWallet;
import fragments.SingleSell;
import fragments.TransactionHistory;
import fragments.WalletHistory;
import simplebeans.balancebeans.BalanceResponse;
import simplebeans.topupbeans.TopUpResponse;
import simplebeans.topupbeans.TopUpResponseBean;
import utilities.Extra;
import utilities.GetCurrentDate;
import utilities.utilitiesbeans.MySessionData;

/**
 * Created by Eng. ISHIMWE Aubain Consolateur email: iaubain@yahoo.fr / aubain.c.ishimwe@oltranz.com Tel: (250) 785 534 672.
 * Application Logged user's portal
 */
public class UserHome extends AppCompatActivity implements CheckBalance.CheckBalanceInteraction, SingleSell.SingleSellInteractionListener, MultipleSell.MultipleSellInteraction, RechargeWallet.RechargeWalletListener, WalletHistory.WalletHistoryInteraction, Favorite.FavoriteInteraction {
    private static final String sessionData = "sessionData";
    private String tag = "AirTime: " + getClass().getSimpleName();
    private FragmentManager fragmentManager;
    private Typeface font;
    private TextView titleBar;
    private MySessionData mSession;
    private NavigationView navigationView;
    private DrawerLayout drawerLayout;
    private Toolbar toolbar;
    private TextView countBadge;
    private LinearLayout mainTabHolder;
    private View accountTab;
    private View walletTab;
    private TextView welcomeUser;
    private TextView lblWelcome;
    private int notificationCount;
    private Menu mainMenuBagdge, sideMenuBagdge;
    private double walletBalance;
    private ProgressBar progress;
    private String loginBalance, loginWalletDate;

    /**
     *
     * @param savedInstanceState application saved instance
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        setContentView(R.layout.userhome_layout);
        String date = new GetCurrentDate(this).getServerDate();
        //request Permission
        rightManager();

        font = Typeface.createFromAsset(this.getAssets(), "font/ubuntu.ttf");
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("");
        titleBar = (TextView) toolbar.findViewById(R.id.toolbar_title);
        titleBar.setTypeface(font, Typeface.BOLD);
        setSupportActionBar(toolbar);

        //adding a menu
        menuHandle();

        // Check that the activity is using the layout version with
        // the fragment_container FrameLayout
        if (findViewById(R.id.checkBalanceFrame) != null &&
                findViewById(R.id.mainButtonFrame) != null &&
                findViewById(R.id.mainTabFrame) != null &&
                findViewById(R.id.salesFrame) != null) {

            // However, if we're being restored from a previous state,
            // then we don't need to do anything and should return or else
            // we could end up with overlapping fragments.
            if (savedInstanceState != null) {

                // Restore value of members from saved state
                Log.e(tag, "Activity Create Restoring session data");
                mSession = savedInstanceState.getParcelable(sessionData);
                //mCurrentLevel = savedInstanceState.getInt(STATE_LEVEL);
                return;
            }

            //initiate my session data;
            Bundle bundle = getIntent().getExtras();
            loginBalance=bundle.getString("balance");
            loginWalletDate=bundle.getString("walletDate");
            mSession = new MySessionData(bundle.getString("token"), bundle.getString("msisdn"), bundle.getString("userName"));
//            TextView wlcmLabel=(TextView) findViewById(R.id.welcomeLabel);
//            wlcmLabel.setTypeface(font);
//            Log.d(tag,"Logged user "+mSession.getUserName());
//            wlcmLabel.append(mSession.getUserName());

            //Activity tab holder initialisation
            mainTabHolder = (LinearLayout) findViewById(R.id.mainTabFrame);

            //Activity tabs initiation
            accountTab = View.inflate(this, R.layout.maintabs, null);

            walletTab = View.inflate(this, R.layout.wallettabs, null);

            fragmentManager = getSupportFragmentManager();

            /* *populating fragment holder with their Fragment**/


                //Adding Check balance Fragment
                checkBalanceFrag();

                //By default set Single topup
                singleTopUpFrag();

            counter();
        }
    }
    /**
     * Permission access manager for different android version
     */
    private void rightManager() {
        int currentVersion = 0;
        try {
            currentVersion = android.os.Build.VERSION.SDK_INT;
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (currentVersion > 0 && currentVersion >= 23) {
            Ask.on(this)
                    .forPermissions(Manifest.permission.READ_CONTACTS,
                            Manifest.permission.SEND_SMS)
                    .withRationales("In Order to make your life easy for contact pick up application needs Read Contact permission",
                            "For some sophisticated data validation in the app Send SMS permission is needed") //optional
                    .go();
        } else {
            return;
        }
    }

    /**
     * Handle menu drawer and menu actions
     */
    private void menuHandle() {
        //Initializing NavigationView
        navigationView = (NavigationView) findViewById(R.id.navigation_view);
        Menu menu = null;
        MenuItem item = null;
        try {
            menu = navigationView.getMenu();
            this.sideMenuBagdge = menu;
            item = menu.findItem(R.id.notifications);
        } catch (Exception e) {
            e.printStackTrace();
        }
        MenuItemCompat.setActionView(item, R.layout.menucounter);
        RelativeLayout notifCount = (RelativeLayout) MenuItemCompat.getActionView(item);

//        sideBadge = (TextView) notifCount.findViewById(R.id.actionbar_notifcation_textview);
        counter();

        //setting user name for the header
        View header = navigationView.getHeaderView(0);
        try {
            lblWelcome = (TextView) header.findViewById(R.id.lblWelcome);
            lblWelcome.setTypeface(font);

            welcomeUser = (TextView) header.findViewById(R.id.histMsisdn);
            welcomeUser.setTypeface(font);
            welcomeUser.setText(mSession.getUserName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        //Setting Navigation View Item Selected Listener to handle the item click of the navigation menu
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {

            // This method will trigger on item Click of navigation menu
            @Override
            public boolean onNavigationItemSelected(MenuItem menuItem) {

                //Checking if the item is in checked state or not, if not make it in checked state
                if (menuItem.isChecked()) menuItem.setChecked(false);
                else menuItem.setChecked(true);

                //Closing drawer on item click
                drawerLayout.closeDrawers();
                return menuSelected(menuItem.getItemId());
            }
        });

        // Initializing Drawer Layout and ActionBarToggle
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer);
        ActionBarDrawerToggle actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.app_name, R.string.app_name) {

            @Override
            public void onDrawerClosed(View drawerView) {
                // Code here will be triggered once the drawer closes as we dont want anything to happen so we leave this blank
                super.onDrawerClosed(drawerView);
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                // Code here will be triggered once the drawer open as we dont want anything to happen so we leave this blank

                super.onDrawerOpened(drawerView);
            }
        };

        //Setting the actionbarToggle to drawer layout
        drawerLayout.addDrawerListener(actionBarDrawerToggle);

        //calling sync state is necessay or else your hamburger icon wont show up
        actionBarDrawerToggle.syncState();

    }

    /**
     * Add amount to wallet
     * @param v View where this method is being called from
     */
    public void walletAddAmount(View v) {
        Fragment currentFrag = fragmentManager.findFragmentById(R.id.salesFrame);
        if (currentFrag.getClass().getSimpleName().equals(SingleSell.class.getSimpleName()) ||
                currentFrag.getClass().getSimpleName().equals(MultipleSell.class.getSimpleName()) ||
                currentFrag.getClass().getSimpleName().equals(Favorite.class.getSimpleName())) {
            walletTopUpFrag();
        }
        if (currentFrag.getClass().getSimpleName().equals(RechargeWallet.class.getSimpleName()) ||
                currentFrag.getClass().getSimpleName().equals(WalletHistory.class.getSimpleName())) {
            singleTopUpFrag();
        }
    }

    /**
     * Request wallet history
     * @param v View where this method is being called from
     */
    public void walletViewHistory(View v) {
        Fragment currentFrag = fragmentManager.findFragmentById(R.id.salesFrame);
        if (currentFrag.getClass().getSimpleName().equals(RechargeWallet.class.getSimpleName()) ||
                currentFrag.getClass().getSimpleName().equals(WalletHistory.class.getSimpleName())) {
            walletHistoryFrag();
        }
        if (currentFrag.getClass().getSimpleName().equals(SingleSell.class.getSimpleName()) ||
                currentFrag.getClass().getSimpleName().equals(MultipleSell.class.getSimpleName()) ||
                currentFrag.getClass().getSimpleName().equals(Favorite.class.getSimpleName())) {
            //Transaction History
            menuTransaction();
        }
    }

    /**
     * handle application main tab
     * @param v View where this method is being called from
     */
    public void onAccountTabClick(View v) {
        if (v.getId() == R.id.sendOne) {
            singleTopUpFrag();
        }
        if (v.getId() == R.id.sendMany) {
            multipleTopUpFrag();
        }
        if (v.getId() == R.id.favorites) {
            favoriteFrag();
        }
    }

    /**
     * Handle wallet tab click and touch
     * @param v View, on which this method is being called from
     */
    public void onWalletTabClick(View v) {
        if (v.getId() == R.id.addAirtime) {
            walletTopUpFrag();
        }
        if (v.getId() == R.id.viewHistory) {
            walletHistoryFrag();
        }
    }

    /**
     * RechargeWalletListener callback
     * @param statusCode link status code
     * @param message notification message
     * @param msisdn user msisdn
     * @param object recharger wallet result object
     */
    @Override
    public void onRechargeWalletInteraction(int statusCode, String message, String msisdn, Object object) {
        String date = new GetCurrentDate(this).getServerDate();
        int notifCount = 0;
        if (!TextUtils.isEmpty(message)) {
            NotificationTable notificationTable = new NotificationTable(date, message, mSession.getMsisdn(), "0");
            notificationTable.save();
            notifCount++;
        }
        counter();
        refreshBalance();

        if (statusCode == 100)
            singleTopUpFrag();
    }

    /**
     * SingleSellListener callback
     * @param statusCode link status
     * @param message notification message
     * @param object singleSellResponse object
     */
    @Override
    public void onSingleSell(int statusCode, String message, Object object) {
        if(statusCode == 403){
            onHomeActivity();
            return;
        }
        String date = new GetCurrentDate(this).getServerDate();
        int notifCount = 0;
        if (!TextUtils.isEmpty(message)) {
            NotificationTable notificationTable = new NotificationTable(date, "AirTime TopUp " + message, mSession.getMsisdn(), "0");
            notificationTable.save();
            notifCount++;
        }

        if (object != null) {
            TopUpResponse topUpResponse = (TopUpResponse) object;
            for (TopUpResponseBean topUpResponseBean : topUpResponse.getTopUpResponseBean()) {
                NotificationTable notificationTable = new NotificationTable(date, "AirTime TopUp on " + topUpResponseBean.getMsisdn() + ":N " + topUpResponseBean.getAmount() + " Status " + topUpResponseBean.getStatus().getMessage(), mSession.getMsisdn(), "0");
                notificationTable.save();
                notifCount++;
            }
        }
        counter();
        refreshBalance();

        if (statusCode == 100)
            singleTopUpFrag();
    }

    /**
     * MultipleSellListener callback
     * @param statusCode link status
     * @param message notification message
     * @param object MultipleSellResponse object
     */
    @Override
    public void onMultipleSellListener(int statusCode, String message, Object object) {
        if(statusCode == 403){
            onHomeActivity();
            return;
        }
        String date = new GetCurrentDate(this).getServerDate();
        int notifCount = 0;
        if (!TextUtils.isEmpty(message)) {
            NotificationTable notificationTable = new NotificationTable(date, "AirTime TopUp " + message, mSession.getMsisdn(), "0");
            notificationTable.save();
            notifCount++;
        }

        if (object != null) {
            TopUpResponse topUpResponse = (TopUpResponse) object;
            for (TopUpResponseBean topUpResponseBean : topUpResponse.getTopUpResponseBean()) {
                NotificationTable notificationTable = new NotificationTable(date, "AirTime TopUp on " + topUpResponseBean.getMsisdn() + ":N " + topUpResponseBean.getAmount() + " Status " + topUpResponseBean.getStatus().getMessage(), mSession.getMsisdn(), "0");
                notificationTable.save();
                notifCount++;
            }
        }
        counter();
        refreshBalance();

        if (statusCode == 1000)
            multipleTopUpFrag();
    }

    /**
     * CheckBalanceListener callback
     * @param statusCode link status
     * @param message notification message
     * @param object CheckBalanceResponse object
     */
    @Override
    public void onCheckBalanceInteraction(int statusCode, String message, Object object) {
        if(statusCode == 403){
            onHomeActivity();
            return;
        }
        if (object != null) {
            BalanceResponse balanceResponse = (BalanceResponse) object;
            walletBalance = Double.valueOf(balanceResponse.getBalance());
        }
    }

    /**
     * FavoriteListener callback
     * @param statusCode link status
     * @param message notification message
     * @param object FavoriteResponse object
     */
    @Override
    public void onFavoriteInteraction(int statusCode, String message, Object object) {
        if(statusCode == 403){
            onHomeActivity();
            return;
        }
        refreshBalance();
    }


    @Override
    public void onResume() {
        super.onResume();
        Log.d(tag, "Resumed the app check");
        try {
            //Activity tab holder initialisation
            mainTabHolder = (LinearLayout) findViewById(R.id.mainTabFrame);

            //Activity tabs initiation
            accountTab = View.inflate(this, R.layout.maintabs, null);

            walletTab = View.inflate(this, R.layout.wallettabs, null);

            fragmentManager = getSupportFragmentManager();

            //Initializing NavigationView
            navigationView = (NavigationView) findViewById(R.id.navigation_view);
            //setting user name for the header
            View header = navigationView.getHeaderView(0);
            try {
                lblWelcome = (TextView) header.findViewById(R.id.lblWelcome);
                lblWelcome.setTypeface(font);

                welcomeUser = (TextView) header.findViewById(R.id.histMsisdn);
                welcomeUser.setTypeface(font);
                welcomeUser.setText(mSession.getUserName());
            } catch (Exception e) {
                e.printStackTrace();
            }

            Log.d(tag, "current tab ");
            currentFrag();

            //updating the counter badge
            counter();
        } catch (Exception e) {
            Log.e(tag, Log.getStackTraceString(e));
            e.printStackTrace();
            onHomeActivity();
        }
        refreshBalance();
    }

    //optional
    @AskGranted(Manifest.permission.READ_CONTACTS)
    public void readContactAllowed() {
        Log.i(tag, "READ CONTACTS GRANTED");
    }

    //optional
    @AskDenied(Manifest.permission.READ_CONTACTS)
    public void readContactDenied() {
        Log.i(tag, "READ CONTACTS DENIED");
        Toast.makeText(this, "Sorry, Without READ_CONTACTS Permission the application workflow can be easily compromised", Toast.LENGTH_LONG).show();
        onHomeActivity();
    }

    //optional
    @AskGranted(Manifest.permission.SEND_SMS)
    public void sendMessageAllowed() {
        Log.i(tag, "SEND SMS GRANTED");
    }

    //optional
    @AskDenied(Manifest.permission.SEND_SMS)
    public void sendMessageDenied() {
        Log.i(tag, "SEND SMS DENIED");
        //Toast.makeText(this, "Sorry, Without SEND_SMS Permission the application workflow can be easily compromised", Toast.LENGTH_LONG).show();
        //onHomeActivity();
    }
    /**
     * Handle soft back key processes
     */
    @Override
    public void onBackPressed() {
        try {

            if (fragmentManager.getBackStackEntryCount() == 2) {
                try {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setMessage("Do you really want to logout?")
                            .setTitle(R.string.dialog_title);
                    // Add the buttons
                    builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            singleTopUpFrag();
                            dialog.dismiss();
                        }
                    });
                    builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            onHomeActivity();
                            dialog.dismiss();
                        }
                    });
                    AlertDialog dialog = builder.create();
                    dialog.show();
                } catch (Exception e) {
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    onHomeActivity();
                }

            } else {
                super.onBackPressed();
            }

        } catch (Exception e) {
            Log.e(tag, Log.getStackTraceString(e));
            onHomeActivity();
        }

        currentFrag();
    }

    /**
     * Activity's Fragment manager
     */
    private void currentFrag() {
        try {

            Fragment currentFrag = fragmentManager.findFragmentById(R.id.salesFrame);
            if (currentFrag.getClass().getSimpleName().equals(SingleSell.class.getSimpleName())) {
                onTabChanged(R.id.mainTabGroup, SingleSell.class.getSimpleName());
            }

            if (currentFrag.getClass().getSimpleName().equals(MultipleSell.class.getSimpleName())) {
                onTabChanged(R.id.mainTabGroup, MultipleSell.class.getSimpleName());
            }


            if (currentFrag.getClass().getSimpleName().equals(Favorite.class.getSimpleName())) {
                onTabChanged(R.id.mainTabGroup, Favorite.class.getSimpleName());
            }

            if (currentFrag.getClass().getSimpleName().equals(RechargeWallet.class.getSimpleName())) {
                onTabChanged(R.id.walletTabGroup, RechargeWallet.class.getSimpleName());
            }
            if (currentFrag.getClass().getSimpleName().equals(WalletHistory.class.getSimpleName())) {
                onTabChanged(R.id.walletTabGroup, WalletHistory.class.getSimpleName());
            }
        } catch (Exception e) {
            Log.e(tag, Log.getStackTraceString(e));
            onHomeActivity();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        Log.d(tag, "Activity Storing session data");
        // Save the user's current game state
        savedInstanceState.putParcelable(sessionData, mSession);

        super.onSaveInstanceState(savedInstanceState);
    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        Log.d(tag, "Activity Recreate Restoring session data");
        // Restore state members from saved instance
        mSession = savedInstanceState.getParcelable(sessionData);
    }

    /**
     * Soft key handler
     * @param keyCode Key pressed
     * @param event key event
     * @return
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            View drawerView = findViewById(R.id.navigation_view); // child drawer view
            if (!drawerLayout.isDrawerOpen(drawerView)) {
                drawerLayout.openDrawer(drawerView);
            } else if (drawerLayout.isDrawerOpen(drawerView)) {
                drawerLayout.closeDrawer(drawerView);
            }
            return true;
        }

        if (keyCode == KeyEvent.KEYCODE_BACK) {
            View drawerView = findViewById(R.id.navigation_view); // child drawer view
            if (drawerLayout.isDrawerOpen(drawerView)) {
                drawerLayout.closeDrawer(drawerView);
                return false;
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    /**
     * Activity tab manager
     * @param tabLabel requested tab
     * @param currentTab current tab
     */
    private void onTabChanged(int tabLabel, String currentTab) {
        if (tabLabel == R.id.mainTabGroup) {
            //check the tab holder
            try {

                mainTabHolder.removeAllViews();
                mainTabHolder.addView(accountTab);

                ImageView loadWalletButton, viewWalletHistory;
                try {
                    loadWalletButton = (ImageView) findViewById(R.id.addAmount);
                    loadWalletButton.setImageResource(R.drawable.ic_wallet_recharge);

                    viewWalletHistory = (ImageView) findViewById(R.id.viewHistoryBtn);
                    viewWalletHistory.setImageResource(R.drawable.ic_wallet_history);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                TextView sendOne = (TextView) findViewById(R.id.sendOne);
                sendOne.setTypeface(font, Typeface.BOLD);
                TextView sendMany = (TextView) findViewById(R.id.sendMany);
                sendMany.setTypeface(font, Typeface.BOLD);
                TextView favorites = (TextView) findViewById(R.id.favorites);
                favorites.setTypeface(font, Typeface.BOLD);
                TextView label = (TextView) findViewById(R.id.label);
                label.setTypeface(font, Typeface.BOLD);

                if (SingleSell.class.getSimpleName().equals(currentTab)) {

                    //Single sell is active
                    sendOne.setBackgroundResource(R.drawable.buttonorange);
                    sendOne.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.appWhite));

                    //mark others as no active
                    sendMany.setBackgroundResource(R.drawable.border_orange);
                    sendMany.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.appBlue));

                    favorites.setBackgroundResource(R.drawable.border_orange);
                    favorites.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.appBlue));
                } else if (MultipleSell.class.getSimpleName().equals(currentTab)) {

                    //Multiple sell is active
                    sendMany.setBackgroundResource(R.drawable.buttonorange);
                    sendMany.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.appWhite));

                    //mark others as no active
                    sendOne.setBackgroundResource(R.drawable.border_orange);
                    sendOne.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.appBlue));

                    favorites.setBackgroundResource(R.drawable.border_orange);
                    favorites.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.appBlue));
                } else if (Favorite.class.getSimpleName().equals(currentTab)) {

                    //Multiple sell is active
                    favorites.setBackgroundResource(R.drawable.buttonorange);
                    favorites.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.appWhite));

                    //mark others as no active
                    sendOne.setBackgroundResource(R.drawable.border_orange);
                    sendOne.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.appBlue));

                    sendMany.setBackgroundResource(R.drawable.border_orange);
                    sendMany.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.appBlue));
                }

//                if(mainTabHolder.getChildCount() > 0){
//                    if(mainTabHolder.getChildAt(0).getId()!= R.id.accountTabs){
//                        mainTabHolder.removeAllViews();
//                        mainTabHolder.addView(accountTab);
//                    }
//                }else{
//                    mainTabHolder.removeAllViews();
//                    mainTabHolder.addView(accountTab);
//                }
            } catch (Exception e) {
                Log.e(tag, " " + e.getMessage());
                onHomeActivity();
            }

        } else if (tabLabel == R.id.walletTabGroup) {
            //check the tab holder
            try {
                mainTabHolder.removeAllViews();
                mainTabHolder.addView(walletTab);

                ImageView loadWalletButton, viewWalletHistory;
                try {
                    loadWalletButton = (ImageView) findViewById(R.id.addAmount);
                    loadWalletButton.setImageResource(R.drawable.ic_send_airtime);

                    viewWalletHistory = (ImageView) findViewById(R.id.viewHistoryBtn);
                    viewWalletHistory.setImageResource(R.drawable.ic_wallet_history);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                TextView addAirtime = (TextView) findViewById(R.id.addAirtime);
                addAirtime.setTypeface(font);
                TextView viewHistory = (TextView) findViewById(R.id.viewHistory);
                viewHistory.setTypeface(font);

                if (RechargeWallet.class.getSimpleName().equals(currentTab)) {

                    //Recharge wallet is active
                    addAirtime.setBackgroundResource(R.drawable.buttonorange);
                    addAirtime.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.appWhite));

                    //mark others as no active
                    viewHistory.setBackgroundResource(R.drawable.border_orange);
                    viewHistory.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.appBlue));
                } else if (WalletHistory.class.getSimpleName().equals(currentTab)) {

                    //Recharge wallet is active
                    viewHistory.setBackgroundResource(R.drawable.buttonorange);
                    viewHistory.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.appWhite));

                    //mark others as no active
                    addAirtime.setBackgroundResource(R.drawable.border_orange);
                    addAirtime.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.appBlue));
                }
//                if(mainTabHolder.getChildCount() > 0){
//                    if(mainTabHolder.getChildAt(0).getId()!= R.id.walletTabs){
//                        mainTabHolder.removeAllViews();
//                        mainTabHolder.addView(walletTab);
//                    }
//                }else{
//                    mainTabHolder.removeAllViews();
//                    mainTabHolder.addView(walletTab);
//                }
            } catch (Exception e) {
                Log.e(tag, " " + e.getMessage());
                onHomeActivity();
            }


        }
    }

    /**
     * main fragment manager
     * @param object instance of fragment
     * @param fragId fragment container id
     */
    //_________Fragments manager___________\\
    private void fragmentHandler(Object object, int fragId) {
        Fragment fragment = (Fragment) object;
        String backStateName = fragment.getClass().getSimpleName();
        String fragmentTag = backStateName;

        boolean fragmentPopped = fragmentManager.popBackStackImmediate(backStateName, 0);

        if (!fragmentPopped && fragmentManager.findFragmentByTag(fragmentTag) == null) { //fragment not in back stack, create it.
            FragmentTransaction ft = fragmentManager.beginTransaction();
            ft.replace(fragId, fragment, fragmentTag);
            ft.addToBackStack(backStateName);
            ft.commit();
        } else {
            Log.d(tag, "Fragment already There");
        }
    }

    /**
     * Produce SingleSell instance
     */
    private void singleTopUpFrag() {
        onTabChanged(R.id.mainTabGroup, SingleSell.class.getSimpleName());
        SingleSell singleSell = new SingleSell();
        singleSell.setArguments(setArgs());
        fragmentHandler(singleSell, R.id.salesFrame);
    }

    /**
     * Produce MultipleSell instance
     */
    private void multipleTopUpFrag() {
        onTabChanged(R.id.mainTabGroup, MultipleSell.class.getSimpleName());
        MultipleSell multipleSell = new MultipleSell();
        multipleSell.setArguments(setArgs());
        fragmentHandler(multipleSell, R.id.salesFrame);

    }

    /**
     * Produce Favorite instance
     */
    private void favoriteFrag() {
        onTabChanged(R.id.mainTabGroup, Favorite.class.getSimpleName());
        Favorite favorite = new Favorite();
        favorite.setArguments(setArgs());
        fragmentHandler(favorite, R.id.salesFrame);
    }

    /**
     * Produce RechargeWallet instance
     */
    private void walletTopUpFrag() {
        onTabChanged(R.id.walletTabGroup, RechargeWallet.class.getSimpleName());
        RechargeWallet rechargeWallet = new RechargeWallet();
        rechargeWallet.setArguments(setArgs());
        fragmentHandler(rechargeWallet, R.id.salesFrame);
    }

    /**
     * Produce CheckBalance instance
     */
    private void checkBalanceFrag() {
        CheckBalance checkBalance = new CheckBalance();
        checkBalance.setArguments(setArgs());
        fragmentHandler(checkBalance, R.id.checkBalanceFrame);
    }

    /**
     * Refresh the wallet balance
     */
    private void refreshBalance() {

        try{
            Fragment fragment=getSupportFragmentManager().findFragmentById(R.id.checkBalanceFrame);
            if (fragment instanceof CheckBalance)
                ((CheckBalance) fragment).checkBalanceComponent();
        }catch (Exception e){
            e.printStackTrace();
        }
//        Fragment frg = null;
//        frg = getSupportFragmentManager().findFragmentByTag(CheckBalance.class.getSimpleName());
//        final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
//        ft.detach(frg);
//        ft.attach(frg);
//        ft.commit();
    }

    /**
     * Produce WalletHistory instance
     */
    private void walletHistoryFrag() {
        onTabChanged(R.id.walletTabGroup, WalletHistory.class.getSimpleName());
        WalletHistory walletHistory = new WalletHistory();
        walletHistory.setArguments(setArgs());
        fragmentHandler(walletHistory, R.id.salesFrame);
    }

    private void transactionHistoryFrag() {

    }

    /**
     * Setting of common fragments arguments
     * @return Bundle containing common params
     */
    private Bundle setArgs() {
        try {
            Bundle bundle = new Bundle();
            bundle.putString("token", mSession.getToken());
            bundle.putString("msisdn", mSession.getMsisdn());
            bundle.putString("walletBalance", loginBalance);
            bundle.putString("walletBalanceDate", loginWalletDate);

            return bundle;
        } catch (Exception e) {
            e.printStackTrace();
            onHomeActivity();
            return null;
        }
    }

    /**
     * Menu creation
     * @param menu menu
     * @return true if menu created else false
     */
    //__________Menu Item Click handling_____________\\
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        this.mainMenuBagdge = menu;
        getMenuInflater().inflate(R.menu.menu_home, menu);
        MenuItem item = menu.findItem(R.id.mainnotifications);
        MenuItemCompat.setActionView(item, R.layout.mainmenucounter);
        LinearLayout notifCount = (LinearLayout) MenuItemCompat.getActionView(item);

        TextView mainCount = (TextView) notifCount.findViewById(R.id.actionbar_notifcation_main);
        mainCount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                menuNotifications();
            }
        });
        counter();
//        ImageView bell = (ImageView) notifCount.findViewById(R.id.notificationBell);
//        bell.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                menuNotifications();
//            }
//        });
        return true;
    }

    /**
     * Handle menu selection
     * @param menuId requested menu ID
     * @return true if action is effected else false
     */
    public boolean menuSelected(int menuId) {
        //Check to see which item was being clicked and perform appropriate action
        switch (menuId) {
            case R.id.accountHome:
                menuAccount();
                return true;
            case R.id.transactionsHistory:
                menuTransaction();
                return true;
            case R.id.notifications:
                menuNotifications();
                return true;
            case R.id.help:
                menuHelp();
                return true;
            case R.id.invite:
                menuInvite();
                return true;
            case R.id.about:
                menuAbout();
                return true;
            case R.id.logout:
                menuLogout();
                return true;
            case R.id.contactUs:
                menuContact();
                return true;
            case R.id.offers:
                menuOffers();
                return true;
//            case R.id.settings:
//                menuSettings();
//                return true;
            default:
                Toast.makeText(getApplicationContext(), "Something is Wrong", Toast.LENGTH_SHORT).show();
                return true;
        }
    }

    private void menuAccount() {
        singleTopUpFrag();
    }

    private void menuTransaction() {
        Intent intent = new Intent(this, Extra.class);
        intent.putExtras(extraBundle(new TransactionHistory()));
        startActivity(intent);
    }

    private void menuContact() {
        Intent intent = new Intent(this, Extra.class);
        intent.putExtras(extraBundle(new ContactUs()));
        startActivity(intent);
    }

    private void menuOffers() {
        Intent intent = new Intent(this, Extra.class);
        intent.putExtras(extraBundle(new Offers()));
        startActivity(intent);
    }

    private void menuSettings() {
        final EditText currentPw, newPw, retypeNewPw;
        final TextView error, lbl1,lbl2,lbl3;
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.change_pin, (ViewGroup) findViewById(R.id.root));
        lbl1=(TextView) layout.findViewById(R.id.label);
        lbl1.setTypeface(font, Typeface.BOLD);

        lbl2=(TextView) layout.findViewById(R.id.lblpw1);
        lbl2.setTypeface(font, Typeface.BOLD);

        lbl3=(TextView) layout.findViewById(R.id.lblrepw);
        lbl3.setTypeface(font, Typeface.BOLD);

        currentPw=(EditText) layout.findViewById(R.id.currentPw);
        currentPw.setTypeface(font);

        newPw=(EditText) layout.findViewById(R.id.newPw);
        newPw.setTypeface(font);

        retypeNewPw=(EditText) layout.findViewById(R.id.retypePw);
        retypeNewPw.setTypeface(font);

        error = (TextView) layout.findViewById(R.id.TextView_PwdProblem);
        error.setTypeface(font);

        retypeNewPw.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                String strPass1 = newPw.getText().toString();
                String strPass2 = retypeNewPw.getText().toString();
                String currentPass= currentPw.getText().toString();
                if(!TextUtils.isEmpty(currentPass)) {
                    if (strPass1.equals(strPass2)) {
                        error.setText("Matching password");
                    } else {
                        retypeNewPw.requestFocus();
                        retypeNewPw.setError("Revise password");
                    }
                }else{
                    currentPw.requestFocus();
                    currentPw.setError("Revise your password");
                }
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(layout)
                .setIcon(android.R.drawable.ic_dialog_info)
                .setTitle(R.string.notification);

        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();
            }
        });

        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                String strPass1 = newPw.getText().toString();
                String strPass2 = retypeNewPw.getText().toString();
                String currentPass= currentPw.getText().toString();
                if(!TextUtils.isEmpty(currentPass) && currentPass.length()>3) {
                    if (strPass1.equals(strPass2) &&(strPass1.length()>3 && strPass2.length()>3)) {
                        error.setText("");
                        Toast.makeText(UserHome.this, "Sorry, this feature is underdevelopment ", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    } else {
                        newPw.requestFocus();
                        newPw.setError("Revise password");

                        retypeNewPw.requestFocus();
                        retypeNewPw.setError("Revise password");

                        Toast.makeText(UserHome.this, "Sorry, password mismatch. Try again!", Toast.LENGTH_SHORT).show();
                    }
                }else{
                    currentPw.requestFocus();
                    currentPw.setError("Revise your password");

                    Toast.makeText(UserHome.this, "Sorry, your current password is missing. Try again!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void menuHelp() {
        showDialog("HELP", BaseUrl.helpUrl);
    }

    private void menuInvite() {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, getResources().getString(R.string.invitationTitle) + getResources().getString(R.string.invitationMessage) + mSession.getUserName() + " is inviting you to join MobileA:\n" + getResources().getString(R.string.downloadUrl));
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent, getResources().getText(R.string.sendTo)));
        Toast.makeText(getApplicationContext(), "Inviting Friends", Toast.LENGTH_LONG).show();
    }

    private void menuAbout() {
        Intent intent = new Intent(this, Extra.class);
        intent.putExtras(extraBundle(new About()));
        startActivity(intent);
    }

    private void menuLogout() {
        mSession = null;
        if (getIntent().getExtras() != null) {
            setIntent(null);
        }
        try {
            SugarRecord.deleteAll(NotificationTable.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        onHomeActivity();
    }

    private void menuNotifications() {
        Intent intent = new Intent(this, Extra.class);
        intent.putExtras(extraBundle(new Notifications()));
        startActivity(intent);
    }

    private Bundle extraBundle(Object object) {
        try {
            Bundle bundle = new Bundle();
            bundle.putString("msisdn", mSession.getMsisdn());
            bundle.putString("token", mSession.getToken());
            bundle.putString("userName", mSession.getUserName());
            bundle.putString("what", object.getClass().getSimpleName());

            return bundle;
        } catch (Exception e) {
            e.printStackTrace();
            onHomeActivity();
            return null;
        }
    }

    public void mainMenuBell(View v) {
        menuNotifications();
    }

    private void counter() {
        try {
            MenuItem item = mainMenuBagdge.findItem(R.id.mainnotifications);
            MenuItemCompat.setActionView(item, R.layout.mainmenucounter);
            LinearLayout notifCountMain = (LinearLayout) MenuItemCompat.getActionView(item);

            countBadge = (TextView) notifCountMain.findViewById(R.id.actionbar_notifcation_main);
            if(notificationCountManager()<=0)
                countBadge.setVisibility(View.GONE);
            else{
                countBadge.setVisibility(View.VISIBLE);
                countBadge.setText(String.valueOf(notificationCountManager()));
            }

            item = sideMenuBagdge.findItem(R.id.notifications);
            MenuItemCompat.setActionView(item, R.layout.menucounter);
            RelativeLayout notifCountSide = (RelativeLayout) MenuItemCompat.getActionView(item);

            countBadge = (TextView) notifCountSide.findViewById(R.id.actionbar_notifcation_textview);
            if(notificationCountManager()<=0)
                countBadge.setVisibility(View.GONE);
            else{
                countBadge.setVisibility(View.VISIBLE);
                countBadge.setText(String.valueOf(notificationCountManager()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int notificationCountManager() {
        try {
            int numberOfNotification = (int) NotificationTable.count(NotificationTable.class, "seen = ?", new String[]{"0"});
            if (numberOfNotification > 0) {
                return notificationCount = numberOfNotification;
            }
            notificationCount = 0;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }

        return notificationCount;
    }

    /*
    int presentCount=Integer.parseInt(countBadge.getText().toString());
            int newCount=count+presentCount;
            countBadge.setText(""+newCount);
            sideBadge.setText(""+newCount);
     */
    private void onHomeActivity() {
//        Intent intent = new Intent(this, Home.class);
//        intent.setFlags(IntentCompat.FLAG_ACTIVITY_TASK_ON_HOME | IntentCompat.FLAG_ACTIVITY_CLEAR_TASK);
        this.finish();
//        startActivity(intent);
    }

    /**
     * Show popup dialog
     * @param mTitle popup title
     * @param url requesting url for webview
     */
    private void showDialog(String mTitle, String url) {
        TextView close;
        TextView title;
        WebView mWeb;

        final Dialog dialog = new Dialog(UserHome.this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setContentView(R.layout.web_dialog);

        close = (TextView) dialog.findViewById(R.id.close);
        close.setTypeface(font, Typeface.BOLD);
        title = (TextView) dialog.findViewById(R.id.title);
        title.setTypeface(font, Typeface.BOLD);
        mWeb = (WebView) dialog.findViewById(R.id.webView);
        progress = (ProgressBar) dialog.findViewById(R.id.progressBar);
        progress.setVisibility(View.GONE);

        title.setText(mTitle);

        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        mWeb.setWebViewClient(new MyWebViewClient());
        mWeb.loadUrl(url);

        dialog.show();
    }

    @Override
    public void onWalletHistoryInteraction(int statusCode, String message, Object object) {

    }

    /**
     * Web client for handling web pages
     */
    private class MyWebViewClient extends WebViewClient {

        @SuppressWarnings("deprecation")
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }

        @TargetApi(Build.VERSION_CODES.N)
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            view.loadUrl(String.valueOf(request.getUrl()));
            return true;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            progress.setVisibility(View.GONE);
            progress.setProgress(100);
            super.onPageFinished(view, url);
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            progress.setVisibility(View.VISIBLE);
            progress.setProgress(0);
            super.onPageStarted(view, url, favicon);
        }
    }



    public void setValue(int progress) {
        this.progress.setProgress(progress);
    }
}
