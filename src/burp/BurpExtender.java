package burp;

import burp.aes.*;
import burp.execjs.*;
import burp.utils.BurpStateListener;
import org.iq80.leveldb.*;

import static org.iq80.leveldb.impl.Iq80DBFactory.*;

import java.io.*;

import java.awt.*;
import java.util.HashMap;
import javax.script.ScriptException;
import javax.swing.*;

public class BurpExtender implements IBurpExtender, ITab {

    public IExtensionHelpers helpers;
    public IBurpExtenderCallbacks callbacks;
    public PrintWriter stdout;
    public PrintWriter stderr;
    public DB store;
    public HashMap<String, IIntruderPayloadProcessor> IPProcessors = new HashMap<>();

    public JPanel aesPanel;
    public AesConfig AesConfig;
    public AesUIHandler AesUI;

    public JSEngine JSEngine;
    public JTabbedPane mainPanel;
    public JPanel rsaPanel;
    public JPanel desPanel;
    public JPanel execJsPanel;

    public boolean RegIPProcessor(String extName, IIntruderPayloadProcessor processor) {
        if (IPProcessors.containsKey(extName)) {
            JOptionPane.showMessageDialog(mainPanel, "This name already exist!");
            return false;
        }
        callbacks.registerIntruderPayloadProcessor(processor);
        IPProcessors.put(extName, processor);
        return true;
    }

    public void RemoveIPProcessor(String extName) {
        if (IPProcessors.containsKey(extName)) {
            IIntruderPayloadProcessor processor = IPProcessors.get(extName);
            callbacks.removeIntruderPayloadProcessor(processor);
            IPProcessors.remove(extName);
        }
    }

    @Override
    public void registerExtenderCallbacks(IBurpExtenderCallbacks callbacks) throws FileNotFoundException, ScriptException, NoSuchMethodException {
        this.callbacks = callbacks;
        this.helpers = callbacks.getHelpers();
        this.stdout = new PrintWriter(callbacks.getStdout(), true);
        this.stderr = new PrintWriter(callbacks.getStderr(), true);
        callbacks.setExtensionName("BurpCrypto");
        callbacks.registerExtensionStateListener(new BurpStateListener(this));

        Options options = new Options();
        options.createIfMissing(true);
        try {
            this.store = factory.open(new File("BurpCrypto.ldb"), options);
            callbacks.issueAlert("LevelDb init success!");
        } catch (IOException e) {
            callbacks.issueAlert("LevelDb init failed! error message: " + e.getMessage());
        }
        this.JSEngine = new JSEngine(this);
        stdout.println("BurpCrypto loaded successfully!");
        InitUi();
    }

    private void InitUi() {
        this.AesUI = new AesUIHandler(this);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                BurpExtender bthis = BurpExtender.this;
                bthis.mainPanel = new JTabbedPane();
                bthis.aesPanel = AesUI.getPanel();

                bthis.rsaPanel = new JPanel();
                bthis.desPanel = new JPanel();
                bthis.execJsPanel = new JPanel();
                bthis.mainPanel.addTab("AES", bthis.aesPanel);
                bthis.mainPanel.addTab("RSA", bthis.rsaPanel);
                bthis.mainPanel.addTab("DES", bthis.desPanel);
                bthis.mainPanel.addTab("Exec Js", bthis.execJsPanel);
                bthis.callbacks.addSuiteTab(bthis);
            }
        });
    }


    @Override
    public String getTabCaption() {
        return "BurpCrypto";
    }

    @Override
    public Component getUiComponent() {
        return this.mainPanel;
    }
}
