package ui;
import common.provider.UIProvider;
import lombok.Getter;
import ui.dashboard.DashboardTab;
import ui.parameterSettngs.checkedParameterSettingsTab;
import ui.settings.SettingsTab;
import ui.vuln.VulnTab;
import javax.swing.*;
import java.awt.*;
public class UIMain extends JTabbedPane {
    private final UIProvider provider;
    @Getter
    private final DashboardTab dashboardTab = new DashboardTab();
    @Getter
    private final VulnTab vulnTab = new VulnTab();
    @Getter
    private final SettingsTab settingsTab = new SettingsTab();
    @Getter
    private checkedParameterSettingsTab checkedParameterSettingsTab = new checkedParameterSettingsTab();
    public UIMain(UIProvider provider) {
        super(); 
        this.provider = provider;
        this.addTab("扫描流量概览", dashboardTab); 
        this.addTab("疑似存在漏洞", vulnTab); 
        this.addTab("插件功能配置", settingsTab); 
        this.addTab("扫描参数配置", checkedParameterSettingsTab);
        applyTheme(this); 
    }
    private void applyTheme(Component component) {
        provider.applyThemeToComponent(component);
        if (component instanceof Container) {
            Container container = (Container) component;
            synchronized (container.getTreeLock()) {
                Component[] components = container.getComponents();
                for (Component comp : components) {
                    applyTheme(comp); 
                }
            }
        }
    }
}

