package com.fsck.k9.activity.setup;


import java.net.URI;
import java.net.URISyntaxException;

import android.content.Context;
import android.content.Intent;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;

import com.fsck.k9.Account;
import com.fsck.k9.Core;
import com.fsck.k9.DI;
import com.fsck.k9.EmailAddressValidator;
import com.fsck.k9.Preferences;
import com.fsck.k9.account.AccountCreator;
import com.fsck.k9.activity.K9Activity;
import com.fsck.k9.activity.setup.AccountSetupCheckSettings.CheckDirection;
import com.fsck.k9.backend.BackendManager;
import com.fsck.k9.helper.UrlEncodingHelper;
import com.fsck.k9.helper.Utility;
import com.fsck.k9.mail.AuthType;
import com.fsck.k9.mail.ConnectionSecurity;
import com.fsck.k9.mail.ServerSettings;
import com.fsck.k9.preferences.Protocols;
import com.fsck.k9.ui.R;
import com.fsck.k9.view.ClientCertificateSpinner;
import com.fsck.k9.view.ClientCertificateSpinner.OnClientCertificateChangedListener;
import timber.log.Timber;

/**
 * Prompts the user for the email address and password.
 * Attempts to lookup default settings for the domain the user specified. If the
 * domain is known the settings are handed off to the AccountSetupCheckSettings
 * activity. If no settings are found the settings are handed off to the
 * AccountSetupAccountType activity.
 */
public class AccountSetupBasics extends K9Activity
    implements OnClickListener, TextWatcher, OnCheckedChangeListener, OnClientCertificateChangedListener {
    private final static String EXTRA_ACCOUNT = "com.fsck.k9.AccountSetupBasics.account";
    private final static String STATE_KEY_CHECKED_INCOMING = "com.fsck.k9.AccountSetupBasics.checkedIncoming";


    private final BackendManager backendManager = DI.get(BackendManager.class);

    private EditText mEmailView;
    private EditText mPasswordView;
    private CheckBox mClientCertificateCheckBox;
    private ClientCertificateSpinner mClientCertificateSpinner;
    private Button mNextButton;
    private Button mManualSetupButton;
    private Account mAccount;

    private EmailAddressValidator mEmailValidator = new EmailAddressValidator();
    private boolean mCheckedIncoming = false;
    private CheckBox mShowPasswordCheckBox;

    public static void actionNewAccount(Context context) {
        Intent i = new Intent(context, AccountSetupBasics.class);
        context.startActivity(i);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setLayout(R.layout.account_setup_basics);
        mEmailView = findViewById(R.id.account_email);
        mPasswordView = findViewById(R.id.account_password);
        mClientCertificateCheckBox = findViewById(R.id.account_client_certificate);
        mClientCertificateSpinner = findViewById(R.id.account_client_certificate_spinner);
        mNextButton = findViewById(R.id.next);
        mManualSetupButton = findViewById(R.id.manual_setup);
        mShowPasswordCheckBox = findViewById(R.id.show_password);
        mNextButton.setOnClickListener(this);
        mManualSetupButton.setOnClickListener(this);
    }

    private void initializeViewListeners() {
        mEmailView.addTextChangedListener(this);
        mPasswordView.addTextChangedListener(this);
        mClientCertificateCheckBox.setOnCheckedChangeListener(this);
        mClientCertificateSpinner.setOnClientCertificateChangedListener(this);
        mShowPasswordCheckBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                showPassword(isChecked);
            }
        });

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mAccount != null) {
            outState.putString(EXTRA_ACCOUNT, mAccount.getUuid());
        }
        outState.putBoolean(STATE_KEY_CHECKED_INCOMING, mCheckedIncoming);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        if (savedInstanceState.containsKey(EXTRA_ACCOUNT)) {
            String accountUuid = savedInstanceState.getString(EXTRA_ACCOUNT);
            mAccount = Preferences.getPreferences(this).getAccount(accountUuid);
        }

        mCheckedIncoming = savedInstanceState.getBoolean(STATE_KEY_CHECKED_INCOMING);

        updateViewVisibility(mClientCertificateCheckBox.isChecked());

        showPassword(mShowPasswordCheckBox.isChecked());
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        /*
         * We wait until now to initialize the listeners because we didn't want
         * the OnCheckedChangeListener active while the
         * mClientCertificateCheckBox state was being restored because it could
         * trigger the pop-up of a ClientCertificateSpinner.chooseCertificate()
         * dialog.
         */
        initializeViewListeners();
        validateFields();
    }

    public void afterTextChanged(Editable s) {
        validateFields();
    }

    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void onClientCertificateChanged(String alias) {
        validateFields();
    }

    /**
     * Called when checking the client certificate CheckBox
     */
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        updateViewVisibility(isChecked);
        validateFields();

        // Have the user select (or confirm) the client certificate
        if (isChecked) {
            mClientCertificateSpinner.chooseCertificate();
        }
    }

    private void updateViewVisibility(boolean usingCertificates) {
        if (usingCertificates) {
            // hide password fields, show client certificate spinner
            mPasswordView.setVisibility(View.GONE);
            mShowPasswordCheckBox.setVisibility(View.GONE);
            mClientCertificateSpinner.setVisibility(View.VISIBLE);
        } else {
            // show password fields, hide client certificate spinner
            mPasswordView.setVisibility(View.VISIBLE);
            mShowPasswordCheckBox.setVisibility(View.VISIBLE);
            mClientCertificateSpinner.setVisibility(View.GONE);
        }
    }

    private void showPassword(boolean show) {
        int cursorPosition = mPasswordView.getSelectionStart();
        if (show) {
            mPasswordView.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
        } else {
            mPasswordView.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }
        mPasswordView.setSelection(cursorPosition);
    }

    private void validateFields() {
        boolean clientCertificateChecked = mClientCertificateCheckBox.isChecked();
        String clientCertificateAlias = mClientCertificateSpinner.getAlias();
        String email = mEmailView.getText().toString();

        boolean valid = Utility.requiredFieldValid(mEmailView)
                && ((!clientCertificateChecked && Utility.requiredFieldValid(mPasswordView))
                        || (clientCertificateChecked && clientCertificateAlias != null))
                && mEmailValidator.isValidAddressOnly(email);

        mNextButton.setEnabled(valid);
        mManualSetupButton.setEnabled(valid);
        /*
         * Dim the next button's icon to 50% if the button is disabled.
         * TODO this can probably be done with a stateful drawable. Check into it.
         * android:state_enabled
         */
        Utility.setCompoundDrawablesAlpha(mNextButton, mNextButton.isEnabled() ? 255 : 128);
    }

    private String getOwnerName() {
        String name = null;
        try {
            name = getDefaultAccountName();
        } catch (Exception e) {
            Timber.e(e, "Could not get default account name");
        }

        if (name == null) {
            name = "";
        }
        return name;
    }

    private String getDefaultAccountName() {
        String name = null;
        Account account = Preferences.getPreferences(this).getDefaultAccount();
        if (account != null) {
            name = account.getName();
        }
        return name;
    }

    private void finishAutoSetup(Provider provider) {
        String email = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();
        String[] emailParts = splitEmail(email);
        String user = emailParts[0];
        String domain = emailParts[1];
        try {
            String userEnc = UrlEncodingHelper.encodeUtf8(user);
            String passwordEnc = UrlEncodingHelper.encodeUtf8(password);

            String incomingUsername = provider.incomingUsernameTemplate;
            incomingUsername = incomingUsername.replaceAll("\\$email", email);
            incomingUsername = incomingUsername.replaceAll("\\$user", userEnc);
            incomingUsername = incomingUsername.replaceAll("\\$domain", domain);

            URI incomingUriTemplate = provider.incomingUriTemplate;
            URI incomingUri = new URI(incomingUriTemplate.getScheme(), incomingUsername + ":" + passwordEnc,
                    incomingUriTemplate.getHost(), incomingUriTemplate.getPort(), null, null, null);

            String outgoingUsername = provider.outgoingUsernameTemplate;

            URI outgoingUriTemplate = provider.outgoingUriTemplate;


            URI outgoingUri;
            if (outgoingUsername != null) {
                outgoingUsername = outgoingUsername.replaceAll("\\$email", email);
                outgoingUsername = outgoingUsername.replaceAll("\\$user", userEnc);
                outgoingUsername = outgoingUsername.replaceAll("\\$domain", domain);
                outgoingUri = new URI(outgoingUriTemplate.getScheme(), outgoingUsername + ":"
                                      + passwordEnc, outgoingUriTemplate.getHost(), outgoingUriTemplate.getPort(), null,
                                      null, null);

            } else {
                outgoingUri = new URI(outgoingUriTemplate.getScheme(),
                                      null, outgoingUriTemplate.getHost(), outgoingUriTemplate.getPort(), null,
                                      null, null);


            }
            if (mAccount == null) {
                mAccount = Preferences.getPreferences(this).newAccount();
                mAccount.setChipColor(AccountCreator.pickColor(this));
            }
            mAccount.setName(getOwnerName());
            mAccount.setEmail(email);
            mAccount.setStoreUri(incomingUri.toString());
            mAccount.setTransportUri(outgoingUri.toString());

            ServerSettings incomingSettings = backendManager.decodeStoreUri(incomingUri.toString());
            mAccount.setDeletePolicy(AccountCreator.getDefaultDeletePolicy(incomingSettings.type));

            // Check incoming here.  Then check outgoing in onActivityResult()
            AccountSetupCheckSettings.actionCheckSettings(this, mAccount, CheckDirection.INCOMING);
        } catch (URISyntaxException use) {
            /*
             * If there is some problem with the URI we give up and go on to
             * manual setup.
             */
            onManualSetup();
        }
    }

    private void onNext() {
        if (mClientCertificateCheckBox.isChecked()) {

            // Auto-setup doesn't support client certificates.
            onManualSetup();
            return;
        }

        String email = mEmailView.getText().toString();
        String[] emailParts = splitEmail(email);
        String domain = emailParts[1];

        Provider provider = findProviderForDomain(domain);
        if (provider != null) {
            finishAutoSetup(provider);
        } else {
            // We don't have default settings for this account, start the manual setup process.
            onManualSetup();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (!mCheckedIncoming) {
                //We've successfully checked incoming.  Now check outgoing.
                mCheckedIncoming = true;
                AccountSetupCheckSettings.actionCheckSettings(this, mAccount, CheckDirection.OUTGOING);
            } else {
                //We've successfully checked outgoing as well.
                mAccount.setDescription(mAccount.getEmail());
                Preferences.getPreferences(this).saveAccount(mAccount);
                Core.setServicesEnabled(this);
                AccountSetupNames.actionSetNames(this, mAccount);
                finish();
            }
        }
    }

    private void onManualSetup() {
        String email = mEmailView.getText().toString();
        String[] emailParts = splitEmail(email);
        String domain = emailParts[1];

        String password = null;
        String clientCertificateAlias = null;
        AuthType authenticationType;
        if (mClientCertificateCheckBox.isChecked()) {
            authenticationType = AuthType.EXTERNAL;
            clientCertificateAlias = mClientCertificateSpinner.getAlias();
        } else {
            authenticationType = AuthType.PLAIN;
            password = mPasswordView.getText().toString();
        }

        if (mAccount == null) {
            mAccount = Preferences.getPreferences(this).newAccount();
            mAccount.setChipColor(AccountCreator.pickColor(this));
        }
        mAccount.setName(getOwnerName());
        mAccount.setEmail(email);

        // set default uris
        // NOTE: they will be changed again in AccountSetupAccountType!
        ServerSettings storeServer = new ServerSettings(Protocols.IMAP, "mail." + domain, -1,
                ConnectionSecurity.SSL_TLS_REQUIRED, authenticationType, email, password, clientCertificateAlias);
        ServerSettings transportServer = new ServerSettings(Protocols.SMTP, "mail." + domain, -1,
                ConnectionSecurity.SSL_TLS_REQUIRED, authenticationType, email, password, clientCertificateAlias);
        String storeUri = backendManager.createStoreUri(storeServer);
        String transportUri = backendManager.createTransportUri(transportServer);
        mAccount.setStoreUri(storeUri);
        mAccount.setTransportUri(transportUri);

        AccountSetupAccountType.actionSelectAccountType(this, mAccount, false);

        finish();
    }

    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.next) {
            onNext();
        } else if (id == R.id.manual_setup) {
            onManualSetup();
        }
    }

    /**
     * Attempts to get the given attribute as a String resource first, and if it fails
     * returns the attribute as a simple String value.
     * @param xml
     * @param name
     * @return
     */
    private String getXmlAttribute(XmlResourceParser xml, String name) {
        int resId = xml.getAttributeResourceValue(null, name, 0);
        if (resId == 0) {
            return xml.getAttributeValue(null, name);
        } else {
            return getString(resId);
        }
    }

    private Provider findProviderForDomain(String domain) {
        try {
            XmlResourceParser xml = getResources().getXml(R.xml.providers);
            int xmlEventType;
            Provider provider = null;
            while ((xmlEventType = xml.next()) != XmlResourceParser.END_DOCUMENT) {
                if (xmlEventType == XmlResourceParser.START_TAG
                        && "provider".equals(xml.getName())
                        && domain.equalsIgnoreCase(getXmlAttribute(xml, "domain"))) {
                    provider = new Provider();
                    provider.id = getXmlAttribute(xml, "id");
                    provider.label = getXmlAttribute(xml, "label");
                    provider.domain = getXmlAttribute(xml, "domain");
                } else if (xmlEventType == XmlResourceParser.START_TAG
                           && "incoming".equals(xml.getName())
                           && provider != null) {
                    provider.incomingUriTemplate = new URI(getXmlAttribute(xml, "uri"));
                    provider.incomingUsernameTemplate = getXmlAttribute(xml, "username");
                } else if (xmlEventType == XmlResourceParser.START_TAG
                           && "outgoing".equals(xml.getName())
                           && provider != null) {
                    provider.outgoingUriTemplate = new URI(getXmlAttribute(xml, "uri"));
                    provider.outgoingUsernameTemplate = getXmlAttribute(xml, "username");
                } else if (xmlEventType == XmlResourceParser.END_TAG
                           && "provider".equals(xml.getName())
                           && provider != null) {
                    return provider;
                }
            }
        } catch (Exception e) {
            Timber.e(e, "Error while trying to load provider settings.");
        }
        return null;
    }

    private String[] splitEmail(String email) {
        String[] retParts = new String[2];
        String[] emailParts = email.split("@");
        retParts[0] = (emailParts.length > 0) ? emailParts[0] : "";
        retParts[1] = (emailParts.length > 1) ? emailParts[1] : "";
        return retParts;
    }


    static class Provider {
        public String id;
        public String label;
        public String domain;
        public URI incomingUriTemplate;
        public String incomingUsernameTemplate;
        public URI outgoingUriTemplate;
        public String outgoingUsernameTemplate;
    }
}