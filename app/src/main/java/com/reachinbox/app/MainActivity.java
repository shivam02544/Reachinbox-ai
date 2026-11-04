package com.reachinbox.app;

import android.os.Bundle;
import android.graphics.Color;
import android.os.Build;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.reachinbox.app.data.local.SessionManager;
import com.reachinbox.app.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        sessionManager = new SessionManager(this);
        applyTheme();
        
        EdgeToEdge.enable(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getWindow().setNavigationBarColor(Color.TRANSPARENT);
            getWindow().setNavigationBarContrastEnforced(false);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getWindow().getDecorView().setSystemUiVisibility(
                    getWindow().getDecorView().getSystemUiVisibility()
                            | (getResources().getBoolean(R.bool.window_light_status_bar)
                            ? View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR : 0));
        }
        
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);

            int navigationInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
            binding.bottomNavigation.setPadding(0, 0, 0, navigationInset);
            binding.bottomNavigation.getLayoutParams().height = getResources()
                    .getDimensionPixelSize(R.dimen.navigation_bar_height) + navigationInset;
            binding.bottomNavigation.requestLayout();
            return insets;
        });

        setupNavigation();
    }

    private void applyTheme() {
        int mode = sessionManager.getThemeMode();
        switch (mode) {
            case SessionManager.THEME_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case SessionManager.THEME_DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }

    private void setupNavigation() {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();
            
            // Set up Toolbar
            setSupportActionBar(binding.toolbar);
            NavigationUI.setupActionBarWithNavController(this, navController);
            
            // Set up Bottom Navigation
            NavigationUI.setupWithNavController(binding.bottomNavigation, navController);
            
            // Handle Bottom Navigation visibility
            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                int id = destination.getId();
                boolean showBottomNav = (id == R.id.dashboardFragment || 
                                       id == R.id.scheduledEmailsFragment ||
                                       id == R.id.campaignsFragment ||
                                       id == R.id.activityFragment ||
                                       id == R.id.moreFragment ||
                                       id == R.id.profileFragment);
                
                binding.bottomNavigation.setVisibility(showBottomNav ? View.VISIBLE : View.GONE);
                
                // Hide activity toolbar on Login and Register screens
                // Also hide if the fragment has its own toolbar
                boolean hideActivityToolbar = (id == R.id.loginFragment || 
                                              id == R.id.registerFragment ||
                                              id == R.id.composeEmailFragment ||
                                              id == R.id.createCampaignFragment ||
                                              id == R.id.campaignDetailsFragment);
                
                binding.appBarLayout.setVisibility(hideActivityToolbar ? View.GONE : View.VISIBLE);
            });
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();
            return navController.navigateUp() || super.onSupportNavigateUp();
        }
        return super.onSupportNavigateUp();
    }
}
