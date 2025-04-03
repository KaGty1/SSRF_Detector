package ui.parameterSettngs;

import burp.api.montoya.MontoyaApi;
import checker.SSRFChecker;
import checker.SSRFPayloadsSetting;
import checker.filter.RequestResponseFilter;
import common.provider.UIProvider;
import scanner.SSRFHttpHandler;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

public class checkedParameterSettingsTab extends JPanel {
    private final UIProvider uiProvider = UIProvider.INSTANCE;
    private final RequestResponseFilter filter = RequestResponseFilter.INSTANCE;

    public checkedParameterSettingsTab() {
        this.setLayout(null); // 保持 null 布局
        int rootX = 0;
        int rootY = 0;

        Component keywordsSettingUIEndComponent = createKeywordsSettingUI(rootX, rootY);
        Component payloadsSettingUIEndCompoment = createPayloadsSettingUI(rootX, keywordsSettingUIEndComponent.getY() + keywordsSettingUIEndComponent.getHeight() + 20);
        Component checkStringsSettingUI = createCheckStringsSettingUI(rootX, payloadsSettingUIEndCompoment.getY() + payloadsSettingUIEndCompoment.getHeight() + 20);

        saveAllSettingUI(
                keywordsSettingUIEndComponent.getX(),
                keywordsSettingUIEndComponent.getY() // 将保存按钮与删除按钮对齐
        );
    }

    private Component createKeywordsSettingUI(int rootX, int rootY) {
        Font burpFont = uiProvider.currentDisplayFont();
        JLabel keywordsSettingLabel = new JLabel("关键字配置");
        keywordsSettingLabel.setFont(createBolderFont(burpFont));
        keywordsSettingLabel.setBounds(rootX + 20, rootY + 20, 200, 20);
        this.add(keywordsSettingLabel);

        DefaultListModel<String> keywordListModel = new DefaultListModel<>();
        for (String keyword : filter.getCheckKeywords()) {
            keywordListModel.addElement(keyword);
        }
        JList<String> keywordList = new JList<>(keywordListModel);
        JScrollPane keywordScrollPane = new JScrollPane(keywordList);
        keywordScrollPane.setBounds(
                keywordsSettingLabel.getX(),
                keywordsSettingLabel.getY() + keywordsSettingLabel.getHeight() + 5,
                300, 100
        );
        this.add(keywordScrollPane);

        JLabel addKeywordLabel = new JLabel("添加关键字:");
        setInputFontAndBounds(keywordScrollPane, addKeywordLabel);
        JTextField addKeywordInput = new JTextField();
        addKeywordInput.setBounds(
                addKeywordLabel.getX() + addKeywordLabel.getWidth() + 5,
                addKeywordLabel.getY(),
                200, 20
        );
        this.add(addKeywordLabel);
        this.add(addKeywordInput);

        JButton addButton = new JButton("添加");
        addButton.setBounds(
                addKeywordInput.getX() + addKeywordInput.getWidth() + 5,
                addKeywordInput.getY(),
                60, 20
        );
        addButton.addActionListener(e -> {
            String newKeyword = addKeywordInput.getText().trim();
            if (!newKeyword.isEmpty() && !keywordListModel.contains(newKeyword)) {
                keywordListModel.addElement(newKeyword);
                addKeywordInput.setText("");
            }
        });
        this.add(addButton);

        JButton removeButton = new JButton("删除选中");
        removeButton.setBounds(
                addButton.getX(), // 与添加按钮对齐
                addButton.getY() + addButton.getHeight() + 5, // 放在添加按钮下方
                90, 20
        );
        removeButton.addActionListener(e -> {
            int selectedIndex = keywordList.getSelectedIndex();
            if (selectedIndex != -1) {
                keywordListModel.remove(selectedIndex);
            }
        });
        this.add(removeButton);

        return removeButton; // 返回 removeButton 作为参考点
    }

    private void saveAllSettingUI(int rootX, int rootY) {
        JButton saveAllSettingButton = new JButton("保存配置");
        saveAllSettingButton.setBounds(
                rootX + 100, // 在“删除选中”按钮右侧，留出间距
                rootY,       // 与“删除选中”按钮同一行
                80, 20
        );
        saveAllSettingButton.addActionListener(e -> {
            DefaultListModel<String> model = (DefaultListModel<String>) ((JList<String>) ((JScrollPane) this.getComponent(1)).getViewport().getView()).getModel();
            ArrayList<String> updatedKeywords = new ArrayList<>();
            for (int i = 0; i < model.getSize(); i++) {
                updatedKeywords.add(model.getElementAt(i));
            }
            filter.updateCheckKeywords(updatedKeywords);
            JOptionPane.showMessageDialog(this, "关键字已成功更新！");
        });
        this.add(saveAllSettingButton);
    }

    private void setInputFontAndBounds(Component positionComponent, Component currentComponent) {
        Font burpFont = uiProvider.currentDisplayFont();
        currentComponent.setFont(createNormalFont(burpFont));
        currentComponent.setBounds(
                positionComponent.getX(),
                positionComponent.getY() + positionComponent.getHeight() + 5,
                80, 20
        );
    }

    //添加用户自定义的ssrf检测Payload -> 需要全回显
    private Component createPayloadsSettingUI(int rootX, int rootY) {
        Font burpFont = uiProvider.currentDisplayFont();
        JLabel payloadsLabel = new JLabel("Payload 配置");
        payloadsLabel.setFont(createBolderFont(burpFont));
        payloadsLabel.setBounds(rootX + 20, rootY + 20, 200, 20);
        this.add(payloadsLabel);

        DefaultListModel<String> payloadsListModel = new DefaultListModel<>();
        for (String payload : SSRFPayloadsSetting.INSTANCE.getPayloads()) {
            payloadsListModel.addElement(payload);
        }
        JList<String> payloadsList = new JList<>(payloadsListModel);
        JScrollPane payloadsScrollPane = new JScrollPane(payloadsList);
        payloadsScrollPane.setBounds(
                payloadsLabel.getX(),
                payloadsLabel.getY() + payloadsLabel.getHeight() + 5,
                300, 100
        );
        this.add(payloadsScrollPane);

        JLabel addPayloadLabel = new JLabel("添加 Payload:");
        setInputFontAndBounds(payloadsScrollPane, addPayloadLabel);
        JTextField addPayloadInput = new JTextField();
        addPayloadInput.setBounds(
                addPayloadLabel.getX() + addPayloadLabel.getWidth() + 5,
                addPayloadLabel.getY(),
                200, 20
        );
        this.add(addPayloadLabel);
        this.add(addPayloadInput);

        JButton addButton = new JButton("添加");
        addButton.setBounds(
                addPayloadInput.getX() + addPayloadInput.getWidth() + 5,
                addPayloadInput.getY(),
                60, 20
        );
        addButton.addActionListener(e -> {
            String newPayload = addPayloadInput.getText().trim();
            if (!newPayload.isEmpty() && !payloadsListModel.contains(newPayload)) {
                payloadsListModel.addElement(newPayload);
                addPayloadInput.setText("");
                //将用户自定义的Payload添加到Payloads列表中
                SSRFPayloadsSetting.INSTANCE.getPayloads().add(newPayload);
            }
        });
        this.add(addButton);

        JButton removeButton = new JButton("删除选中");
        removeButton.setBounds(
                addButton.getX(),
                addButton.getY() + addButton.getHeight() + 5,
                90, 20
        );
        removeButton.addActionListener(e -> {
            int selectedIndex = payloadsList.getSelectedIndex();
            if (selectedIndex != -1) {
                //从//从List<String> payloads中删除选中元素
                String waitingDeletedPayload = payloadsListModel.getElementAt(selectedIndex);
                payloadsListModel.remove(selectedIndex);
                SSRFPayloadsSetting.INSTANCE.getPayloads().remove(waitingDeletedPayload);
            }
        });
        this.add(removeButton);

        return removeButton;
    }

    //添加ssrf回显标志flag -> 用来验证是否存在全回显ssrf漏洞
    private Component createCheckStringsSettingUI(int rootX, int rootY) {
        Font burpFont = uiProvider.currentDisplayFont();
        JLabel checkStringsLabel = new JLabel("检测字符串配置");
        checkStringsLabel.setFont(createBolderFont(burpFont));
        checkStringsLabel.setBounds(rootX + 20, rootY + 20, 200, 20);
        this.add(checkStringsLabel);

        DefaultListModel<String> checkStringsListModel = new DefaultListModel<>();
        for (String checkString : SSRFPayloadsSetting.INSTANCE.getCheckStrings()) {
            checkStringsListModel.addElement(checkString);
        }
        JList<String> checkStringsList = new JList<>(checkStringsListModel);
        JScrollPane checkStringsScrollPane = new JScrollPane(checkStringsList);
        checkStringsScrollPane.setBounds(
                checkStringsLabel.getX(),
                checkStringsLabel.getY() + checkStringsLabel.getHeight() + 5,
                300, 100
        );
        this.add(checkStringsScrollPane);

        JLabel addCheckStringLabel = new JLabel("添加检测字符串:");
        setInputFontAndBounds(checkStringsScrollPane, addCheckStringLabel);
        JTextField addCheckStringInput = new JTextField();
        addCheckStringInput.setBounds(
                addCheckStringLabel.getX() + addCheckStringLabel.getWidth() + 5,
                addCheckStringLabel.getY(),
                200, 20
        );
        this.add(addCheckStringLabel);
        this.add(addCheckStringInput);

        JButton addButton = new JButton("添加");
        addButton.setBounds(
                addCheckStringInput.getX() + addCheckStringInput.getWidth() + 5,
                addCheckStringInput.getY(),
                60, 20
        );
        addButton.addActionListener(e -> {
            String newCheckString = addCheckStringInput.getText().trim();
            if (!newCheckString.isEmpty() && !checkStringsListModel.contains(newCheckString)) {
                checkStringsListModel.addElement(newCheckString);
                addCheckStringInput.setText("");
                SSRFPayloadsSetting.INSTANCE.getCheckStrings().add(newCheckString);
            }
        });
        this.add(addButton);

        JButton removeButton = new JButton("删除选中");
        removeButton.setBounds(
                addButton.getX(),
                addButton.getY() + addButton.getHeight() + 5,
                90, 20
        );
        removeButton.addActionListener(e -> {
            int selectedIndex = checkStringsList.getSelectedIndex();
            if (selectedIndex != -1) {
                //从List<String> checkStrings中删除选中元素
                String waitingDeletedCheckString = checkStringsListModel.getElementAt(selectedIndex);
                checkStringsListModel.remove(selectedIndex);
                SSRFPayloadsSetting.INSTANCE.getCheckStrings().remove(waitingDeletedCheckString);
            }
        });
        this.add(removeButton);

        return removeButton;
    }









    private Font createBolderFont(Font burpFont) {
        return new Font(burpFont.getName(), Font.PLAIN, burpFont.getSize() + 7);
    }

    private Font createNormalFont(Font burpFont) {
        return new Font(burpFont.getName(), Font.PLAIN, burpFont.getSize() + 1);
    }
}


