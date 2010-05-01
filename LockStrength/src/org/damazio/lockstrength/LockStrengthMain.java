package org.damazio.lockstrength;

import java.util.List;

import org.damazio.lockstrength.LockPatternView.Cell;
import org.damazio.lockstrength.LockStrengthMeter.Strength;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;

/**
 * Main activity.
 *
 * @author rdamazio
 */
public class LockStrengthMain extends Activity implements LockPatternView.OnPatternListener {
  private static final String DISPLAY_PATTERN_KEY = "displayPattern";
  private static final String TACTILE_FEEDBACK_KEY = "tactileFeedback";

  private LockStrengthMeter meter;
  private TextView resultView;
  private LockPatternView patternView;
  private SharedPreferences preferences;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);

    patternView = (LockPatternView) findViewById(R.id.pattern);
    resultView = (TextView) findViewById(R.id.result);
    meter = new LockStrengthMeter();

    patternView.setOnPatternListener(this);
  }
  
  @Override
  protected void onResume() {
    super.onResume();

    preferences = PreferenceManager.getDefaultSharedPreferences(this);
    setDisplayPatternEnabled(preferences.getBoolean(DISPLAY_PATTERN_KEY, true));
    setTactileFeedbackEnabled(preferences.getBoolean(TACTILE_FEEDBACK_KEY, true));
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.main_options, menu);
    return true;
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    // Load the checked state from the preferences
    setMenuItemChecked(menu.findItem(R.id.toggle_display),
        preferences.getBoolean(DISPLAY_PATTERN_KEY, true));
    setMenuItemChecked(menu.findItem(R.id.toggle_tactile),
        preferences.getBoolean(TACTILE_FEEDBACK_KEY, true));
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    boolean checked = item.isChecked();
    if (item.isCheckable()) {
      checked = !checked;
      setMenuItemChecked(item, checked);
    }

    switch (item.getItemId()) {
      case R.id.toggle_display:
        setDisplayPatternEnabled(checked);
        break;
      case R.id.toggle_tactile:
        setTactileFeedbackEnabled(checked);
        break;
      case R.id.about:
        showAboutScreen();
        break;
    }
    return true;
  }

  /**
   * Sets the checked and icon properties for a checkable menu item.
   *
   * @param item the item to set properties for
   * @param checked whether it's checked or not
   */
  private void setMenuItemChecked(MenuItem item, boolean checked) {
    item.setChecked(checked);
    item.setIcon(checked
        ? android.R.drawable.button_onoff_indicator_on
        : android.R.drawable.button_onoff_indicator_off);
  }

  /**
   * Displays the about screen.
   */
  private void showAboutScreen() {
    startActivity(new Intent(this, AboutScreen.class));
  }

  /**
   * Sets whether tactile feedback is enabled or not.
   */
  private void setTactileFeedbackEnabled(boolean checked) {
    patternView.setTactileFeedbackEnabled(checked);
    preferences.edit().putBoolean(TACTILE_FEEDBACK_KEY, checked).commit();
  }

  /**
   * Sets whether to display the drawn patterns or not.
   */
  private void setDisplayPatternEnabled(boolean checked) {
    patternView.setInStealthMode(!checked);
    preferences.edit().putBoolean(DISPLAY_PATTERN_KEY, checked).commit();
  }

  /* (non-Javadoc)
   * @see org.damazio.lockstrength.LockPatternView.OnPatternListener#onPatternStart
   */
  public void onPatternStart() {
    // Do nothing
  }

  /* (non-Javadoc)
   * @see org.damazio.lockstrength.LockPatternView.OnPatternListener#onPatternCellAdded
   */
  public void onPatternCellAdded(List<Cell> pattern) {
    updateStrength(pattern);
  }

  /* (non-Javadoc)
   * @see org.damazio.lockstrength.LockPatternView.OnPatternListener#onPatternDetected
   */
  public void onPatternDetected(List<Cell> pattern) {
    updateStrength(pattern);
  }

  /* (non-Javadoc)
   * @see org.damazio.lockstrength.LockPatternView.OnPatternListener#onPatternCleared
   */
  public void onPatternCleared() {
    // Do nothing
  }

  /**
   * Calculates and displays the strength of the given pattern.
   */
  private void updateStrength(List<Cell> pattern) {
    Strength strength = meter.calculateStrength(pattern);
    resultView.setText(getTextForStrength(strength));
    patternView.setDrawingColor(getColorForStrength(strength));
    resultView.setBackgroundResource(getBackgroundForStrength(strength));
  }

  /**
   * Returns the lock pattern drawing color for the given strength.
   */
  private LockPatternView.DrawingColor getColorForStrength(Strength strength) {
    switch (strength) {
      case WEAK:
        return LockPatternView.DrawingColor.RED;
      case AVERAGE:
        return LockPatternView.DrawingColor.ORANGE;
      case GOOD:
        return LockPatternView.DrawingColor.YELLOW;
      case EXCELLENT:
        return LockPatternView.DrawingColor.GREEN;
    }
    return null;
  }

  /**
   * Returns the background drawable resource to use for the given strength.
   */
  private int getBackgroundForStrength(Strength strength) {
    switch (strength) {
      case WEAK:
        return R.drawable.bg_red;
      case AVERAGE:
        return R.drawable.bg_orange;
      case GOOD:
        return R.drawable.bg_yellow;
      case EXCELLENT:
        return R.drawable.bg_green;
    }
    return 0;
  }

  /**
   * Returns the text resource to use for the given strength.
   */
  private int getTextForStrength(Strength strength) {
    switch (strength) {
      case WEAK:
        return R.string.strength_weak;
      case AVERAGE:
        return R.string.strength_average;
      case GOOD:
        return R.string.strength_good;
      case EXCELLENT:
        return R.string.strength_excellent;
    }
    return 0;
  }
}