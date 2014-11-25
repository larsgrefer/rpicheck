package de.eidottermihi.rpicheck.activity;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.base.Strings;

import de.eidottermihi.rpicheck.R;
import de.eidottermihi.rpicheck.activity.helper.KeyFileHelper;
import de.eidottermihi.rpicheck.activity.helper.Validation;
import de.eidottermihi.rpicheck.db.DeviceDbHelper;
import de.larsgrefer.android.library.injection.annotation.XmlLayout;
import de.larsgrefer.android.library.injection.annotation.XmlView;
import de.larsgrefer.android.library.ui.InjectionActionBarActivity;

@XmlLayout(id = R.layout.activity_raspi_new_auth, rClass = R.class)
public class NewRaspiAuthActivity extends InjectionActionBarActivity implements
		OnItemSelectedListener {
	private static final Logger LOGGER = LoggerFactory.getLogger(NewRaspiAuthActivity.class);

	public static final String[] SPINNER_AUTH_METHODS = { "password", "keys", "keysWithPassword" };

	public static final int REQUEST_LOAD = 0;

	private Validation validator = new Validation();

	@XmlView(id = R.id.spinnerAuthMethod)
	private Spinner spinnerAuth;

	private DeviceDbHelper deviceDb;

	@XmlView(id = R.id.rel_pw)
	private RelativeLayout relLaySshPass;

	@XmlView(id = R.id.rel_key)
	private RelativeLayout relLayKeyfile;

	@XmlView(id = R.id.rel_key_pw)
	private RelativeLayout relLayKeyPassphrase;

	@XmlView(id = R.id.editText_ssh_password)
	private EditText editTextSshPass;

	@XmlView(id = R.id.editTextKeyPw)
	private EditText editTextKeyfilePass;

	@XmlView(id = R.id.buttonKeyfile)
	private Button buttonKeyfile;

	@XmlView(id = R.id.text_key_pw)
	private TextView textKeyPass;

	@XmlView(id = R.id.edit_raspi_ssh_port_editText)
	private EditText editTextSshPort;

	@XmlView(id = R.id.edit_raspi_sudoPass_editText)
	private EditText editTextSudoPw;

	@XmlView(id = R.id.checkboxAsk)
	private CheckBox checkboxAskPassphrase;


	private String keyFileContent;

	private String host;
	private String desc;
	private String user;
	private String name;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Show the Up button in the action bar.
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		// show default option for auth method = ssh password
		this.switchAuthMethodsInView(SPINNER_AUTH_METHODS[0]);
		// init auth spinner
		final ArrayAdapter<CharSequence> adapter = ArrayAdapter
				.createFromResource(this, R.array.auth_methods,
						android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		// Apply the adapter to the spinner
		spinnerAuth.setAdapter(adapter);
		spinnerAuth.setOnItemSelectedListener(this);

		// init sql db
		deviceDb = new DeviceDbHelper(this);

		// get data from previous screen (name/host/user...), already validated
		final Bundle piData = this.getIntent().getExtras()
				.getBundle(NewRaspiActivity.PI_BUNDLE);
		host = piData.getString(NewRaspiActivity.PI_HOST);
		name = piData.getString(NewRaspiActivity.PI_NAME);
		user = piData.getString(NewRaspiActivity.PI_USER);
		desc = piData.getString(NewRaspiActivity.PI_DESC);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_raspi_new_auth, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// This ID represents the Home or Up button. In the case of this
			// activity, the Up button is shown. Use NavUtils to allow users
			// to navigate up one level in the application structure. For
			// more details, see the Navigation pattern on Android Design:
			//
			// http://developer.android.com/design/patterns/navigation.html#up-vs-back
			//
			NavUtils.navigateUpFromSameTask(this);
			return true;
		case R.id.menu_save:
			saveRaspi();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	public void onButtonClick(View view) {
		switch (view.getId()) {
		case R.id.buttonKeyfile:
			openKeyfile();
			break;
		default:
			break;
		}
	}

	public void onCheckboxClick(View view) {
		boolean checked = ((CheckBox) view).isChecked();
		switch (view.getId()) {
		case R.id.checkboxAsk:
			switchCheckbox(checked);
			break;
		default:
			break;
		}
	}

	private void switchCheckbox(boolean checked) {
		LOGGER.debug("Always ask for passphrase: {}", checked);
		if (checked) {
			// don't show textfield for passphrase
			relLayKeyPassphrase.setVisibility(View.GONE);
		} else {
			// show textfield for passphrase
			relLayKeyPassphrase.setVisibility(View.VISIBLE);
		}
	}

	private void openKeyfile() {
		KeyFileHelper.startOpenKeyFileIntent(this);
	}

	private void saveRaspi() {
		// get auth method
		final String selectedAuthMethod = SPINNER_AUTH_METHODS[spinnerAuth
				.getSelectedItemPosition()];
		final String sudoPass = editTextSudoPw.getText().toString().trim();
		final String sshPort = editTextSshPort.getText().toString().trim();
		boolean portOk = true;
		// validate ssh port (range 1 to 65535)
		if (!validator.validatePort(editTextSshPort)) {
			portOk = false;
		}
		if (portOk) {
			boolean saveSuccessful = false;
			if (selectedAuthMethod.equals(SPINNER_AUTH_METHODS[0])) {
				// ssh password (cannot be empty)
				if (validator.checkNonOptionalTextField(editTextSshPass,
						getString(R.string.validation_msg_password))) {
					final String sshPass = editTextSshPass.getText().toString()
							.trim();
					addRaspiToDb(name, host, user, selectedAuthMethod, sshPort,
							desc, sudoPass, sshPass, null, null);
					saveSuccessful = true;
				}
			} else if (selectedAuthMethod.equals(SPINNER_AUTH_METHODS[1])) {
				// keyfile must be selected
				if (keyFileContent != null && new File(keyFileContent).exists()) {
					addRaspiToDb(name, host, user, selectedAuthMethod, sshPort,
							desc, sudoPass, null, null, keyFileContent);
					saveSuccessful = true;
				} else {
					buttonKeyfile
							.setError(getString(R.string.validation_msg_keyfile));
				}
			} else if (selectedAuthMethod.equals(SPINNER_AUTH_METHODS[2])) {
				// keyfile must be selected
				if (keyFileContent != null && new File(keyFileContent).exists()) {
					if (checkboxAskPassphrase.isChecked()) {
						addRaspiToDb(name, host, user, selectedAuthMethod,
								sshPort, desc, sudoPass, null, null,
											keyFileContent);
						saveSuccessful = true;
					} else {
						// password must be set
						if (validator
								.checkNonOptionalTextField(
										editTextKeyfilePass,
										getString(R.string.validation_msg_key_passphrase))) {
							final String keyfilePass = editTextKeyfilePass
									.getText().toString().trim();
							addRaspiToDb(name, host, user, selectedAuthMethod,
									sshPort, desc, sudoPass, null, keyfilePass,
												keyFileContent);
							saveSuccessful = true;
						}
					}
				} else {
					buttonKeyfile
							.setError(getString(R.string.validation_msg_keyfile));
				}
			}
			if (saveSuccessful) {
				Toast.makeText(this, R.string.new_pi_created,
						Toast.LENGTH_SHORT).show();
				// finish
				this.setResult(RESULT_OK);
				this.finish();
			}
		}
	}

	private void addRaspiToDb(final String name, final String host, final String user,
			final String authMethod, String sshPort, final String description,
			String sudoPass, final String sshPass, final String keyPass, final String keyFileContent) {
		// if sshPort is empty, use default port (22)
		if (Strings.isNullOrEmpty(sshPort)) {
			sshPort = getText(R.string.default_ssh_port).toString();
		}
		if (Strings.isNullOrEmpty(sudoPass)) {
			sudoPass = "";
		}
		final String port = sshPort, pass = sudoPass;
		new Thread() {
			@Override
			public void run() {
				deviceDb.create(name, host, user, sshPass, Integer.parseInt(port),
						description, pass, authMethod, keyFileContent, keyPass);
			}
		}.start();
	}

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int pos,
			long id) {
		final String selectedAuthMethod = SPINNER_AUTH_METHODS[pos];
		LOGGER.debug("Auth method selected: {}", selectedAuthMethod);
		this.switchAuthMethodsInView(selectedAuthMethod);
	}

	@Override
	public void onNothingSelected(AdapterView<?> arg0) {

	}

	private void switchAuthMethodsInView(String method) {
		if (method.equals(SPINNER_AUTH_METHODS[0])) {
			// show only ssh password
			relLaySshPass.setVisibility(View.VISIBLE);
			relLayKeyfile.setVisibility(View.GONE);
		} else if (method.equals(SPINNER_AUTH_METHODS[1])) {
			// show key file button (no passphrase)
			relLaySshPass.setVisibility(View.GONE);
			relLayKeyfile.setVisibility(View.VISIBLE);
			checkboxAskPassphrase.setVisibility(View.GONE);
			textKeyPass.setVisibility(View.GONE);
			editTextKeyfilePass.setVisibility(View.GONE);
		} else {
			// show key file button and passphrase field
			relLaySshPass.setVisibility(View.GONE);
			relLayKeyfile.setVisibility(View.VISIBLE);
			checkboxAskPassphrase.setVisibility(View.VISIBLE);
			textKeyPass.setVisibility(View.VISIBLE);
			editTextKeyfilePass.setVisibility(View.VISIBLE);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d(this.getLocalClassName(), "onActivityResult");
		Log.d(this.getLocalClassName(), "requestCode: " + requestCode);
		if (resultCode == Activity.RESULT_OK) {
			Log.d(this.getLocalClassName(), "RESULT_OK");
			if (requestCode == KeyFileHelper.REQUEST_OPEN_KEY_FILE) {
				keyFileContent = KeyFileHelper.getKeyFileContentFromActivityResult(this, requestCode, resultCode, data);
			}
		} else if (resultCode == Activity.RESULT_CANCELED) {
			Log.d(this.getLocalClassName(), "RESULT_CANCELED");
		}
	}
}
