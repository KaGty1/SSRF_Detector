package common.provider;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.Registration;
import burp.api.montoya.ui.UserInterface;
import burp.api.montoya.ui.editor.EditorOptions;
import burp.api.montoya.ui.editor.HttpRequestEditor;
import burp.api.montoya.ui.editor.HttpResponseEditor;
import lombok.Getter;
import ui.UIMain;

import java.awt.*;

// 定义一个枚举类 UIProvider，用于实现单例模式
public enum UIProvider {
    // 枚举的唯一实例，名为 INSTANCE，单例模式确保全局只有一个 UIProvider 对象
    INSTANCE;

    // 声明一个私有字段 userInterface，类型为 UserInterface，用于与 Burp Suite 的 UI API 交互
    private UserInterface userInterface;

    // 声明一个私有字段 uiMain，类型为 UIMain，并使用 @Getter 注解自动生成 getter 方法
    // UIMain 可能是一个自定义的 UI 主组件类
    @Getter
    private UIMain uiMain;

    // 静态方法 constructUIProvider，用于初始化 UIProvider 的 userInterface 字段
    // 参数 api 是 MontoyaApi 的实例，提供了对 Burp Suite API 的访问
    public static void constructUIProvider(MontoyaApi api) {
        // 从 MontoyaApi 中获取 UserInterface 实例并赋值给 INSTANCE 的 userInterface 字段
        UIProvider.INSTANCE.userInterface = api.userInterface();
    }

    // 注册一个新的 Suite Tab（Burp Suite 界面中的选项卡）
    // 参数 title 是选项卡的标题，component 是选项卡的内容组件
    // 返回 Registration 对象，表示注册的结果
    public Registration registerSuiteTab(String title, Component component) {
        // 将传入的 component 强制转换为 UIMain 类型并赋值给 uiMain 字段
        uiMain = (UIMain) component;
        // 调用 userInterface 的 registerSuiteTab 方法，注册新的选项卡并返回注册结果
        return userInterface.registerSuiteTab(title, component);
    }

    // 将 Burp Suite 的主题应用到指定的 UI 组件
    // 参数 component 是需要应用主题的 Swing 组件
    public void applyThemeToComponent(Component component) {
        // 调用 userInterface 的 applyThemeToComponent 方法，将当前主题应用到组件
        userInterface.applyThemeToComponent(component);
    }

    // 创建一个 HTTP 请求编辑器
    // 参数 options 是编辑器的配置选项（可变参数），如是否只读等
    // 返回 HttpRequestEditor 对象，用于编辑 HTTP 请求
    public HttpRequestEditor createHttpRequestEditor(EditorOptions... options) {
        // 调用 userInterface 的 createHttpRequestEditor 方法，创建并返回请求编辑器
        return userInterface.createHttpRequestEditor(options);
    }

    // 创建一个 HTTP 响应编辑器
    // 参数 options 是编辑器的配置选项（可变参数）
    // 返回 HttpResponseEditor 对象，用于编辑 HTTP 响应
    public HttpResponseEditor createHttpResponseEditor(EditorOptions... options) {
        // 调用 userInterface 的 createHttpResponseEditor 方法，创建并返回响应编辑器
        return userInterface.createHttpResponseEditor(options);
    }

    // 获取当前编辑器使用的字体
    // 返回 Font 对象，表示编辑器的字体设置
    public Font currentEditorFont() {
        // 调用 userInterface 的 currentEditorFont 方法，返回编辑器字体
        return userInterface.currentEditorFont();
    }

    // 获取当前显示字体（可能是界面中非编辑器部分的字体）
    // 返回 Font 对象，表示显示字体设置
    public Font currentDisplayFont() {
        // 调用 userInterface 的 currentDisplayFont 方法，返回显示字体
        return userInterface.currentDisplayFont();
    }

    // 获取 Burp Suite 主界面的 Frame 对象
    // 返回 Frame，表示整个 Burp Suite 窗口的 Swing 框架
    public Frame getSuiteFrame() {
        // 通过 userInterface 的 swingUtils().suiteFrame() 方法获取主窗口的 Frame
        return userInterface.swingUtils().suiteFrame();
    }
}

