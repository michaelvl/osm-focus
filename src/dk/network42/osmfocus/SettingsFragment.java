package dk.network42.osmfocus;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;

public class SettingsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }
    
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    	if (getActivity() != null)
    		updateSummary(findPreference(key));
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	for (int ii = 0; ii < getPreferenceScreen().getPreferenceCount(); ii++) {
    		Preference preference = getPreferenceScreen().getPreference(ii);
    		if (preference instanceof PreferenceGroup) {
    			PreferenceGroup preferenceGroup = (PreferenceGroup) preference;
    			for (int jj = 0; jj < preferenceGroup.getPreferenceCount(); jj++) {
    				updateSummary(preferenceGroup.getPreference(jj));
    			}
    		} else {
    			updateSummary(preference);
    		}
    	}
    }
    
    private void updateSummary(Preference p) {
        if (p instanceof ListPreference) {
            ListPreference listPref = (ListPreference) p;
            p.setSummary(listPref.getEntry());
        }
        if (p instanceof EditTextPreference) {
            EditTextPreference editTextPref = (EditTextPreference) p;
            p.setSummary(editTextPref.getText());
        }
        if (p instanceof MultiSelectListPreference) {
            EditTextPreference editTextPref = (EditTextPreference) p;
            p.setSummary(editTextPref.getText());
        }
        if (p instanceof CheckBoxPreference) {
            CheckBoxPreference chkPref = (CheckBoxPreference) p;
            if (chkPref.isChecked()) {
            	p.setSummary(this.getString(R.string.info_enabled));
            } else {
            	p.setSummary(this.getString(R.string.info_disabled));
            }
        }
    }
}
