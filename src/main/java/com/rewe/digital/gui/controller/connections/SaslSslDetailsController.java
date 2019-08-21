package com.rewe.digital.gui.controller.connections;

import com.rewe.digital.gui.controls.FilePicker;
import com.rewe.digital.model.connection.BrokerSecuritySettings;
import com.rewe.digital.model.connection.BrokerSecurityType;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import javax.inject.Named;
import java.net.URL;
import java.util.ResourceBundle;

@Named
public class SaslSslDetailsController implements Initializable {
    public FilePicker truststoreLocation;
    public PasswordField truststorePassword;
    public FilePicker keystoreLocation;
    public PasswordField keystorePassword;
    public PasswordField sslKeyPassword;
    public TextField saslMechanism;
    public ComboBox<String> securityTypeSelection;
    public TextField loginUser;
    public PasswordField loginPassword;

    private static SaslSslDetailsController instance;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        instance = this;
    }

    public BrokerSecuritySettings getSecuritySettings() {
        return new BrokerSecuritySettings(
                BrokerSecurityType.valueOf(instance.securityTypeSelection.getValue()),
                instance.saslMechanism.getText(),
                instance.truststoreLocation.getText(),
                instance.truststorePassword.getText(),
                instance.keystoreLocation.getText(),
                instance.keystorePassword.getText(),
                instance.sslKeyPassword.getText(),
                instance.loginUser.getText(),
                instance.loginPassword.getText()
        );
    }

    public void applySecuritySettings(BrokerSecuritySettings brokerSecuritySettings) {
        instance.saslMechanism.setText(brokerSecuritySettings.getSaslMechanism());
        instance.truststoreLocation.setText(brokerSecuritySettings.getSslTruststoreLocation());
        instance.truststorePassword.setText(brokerSecuritySettings.getPlainSslTruststorePassword());
        instance.keystoreLocation.setText(brokerSecuritySettings.getSslKeystoreLocation());
        instance.keystorePassword.setText(brokerSecuritySettings.getPlainSslKeystorePassword());
        instance.sslKeyPassword.setText(brokerSecuritySettings.getPlainSslKeyPassword());
        instance.loginUser.setText(brokerSecuritySettings.getLoginUser());
        instance.loginPassword.setText(brokerSecuritySettings.getPlainLoginPassword());
        if (brokerSecuritySettings.getSecurityType() != null) {
            instance.securityTypeSelection.getSelectionModel().select(brokerSecuritySettings.getSecurityType().name());
        } else {
            instance.securityTypeSelection.getSelectionModel().select(BrokerSecurityType.PLAINTEXT.name());
        }
    }

    public void resetSettings() {
        instance.saslMechanism.setText("");
        instance.truststoreLocation.setText("");
        instance.truststorePassword.setText("");
        instance.keystoreLocation.setText("");
        instance.keystorePassword.setText("");
        instance.sslKeyPassword.setText("");
        instance.loginUser.setText("");
        instance.loginPassword.setText("");
        instance.securityTypeSelection.getSelectionModel().select(BrokerSecurityType.PLAINTEXT.name());
    }
}
