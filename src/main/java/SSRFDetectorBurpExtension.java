import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.extension.Extension;
import common.logger.AutoSSRFLogger;
import common.pool.CollaboratorThreadPool;
import common.pool.UIThreadPool;
import common.provider.CollaboratorProvider;
import common.provider.HttpProvider;
import common.provider.MontoyaApiProvider;
import common.provider.UIProvider;
import scanner.SSRFHttpHandler;
import scanner.SSRFScanCheck;
import ui.UIMain;

public class SSRFDetectorBurpExtension implements BurpExtension {
    private final MontoyaApiProvider montoyaApiProvider = MontoyaApiProvider.INSTANCE;
    private AutoSSRFLogger logger;
    private final UIProvider uiProvider = UIProvider.INSTANCE;
    @Override
    public void initialize(MontoyaApi api) {
        initMontoyaApiProvider(api);
        initTools(api);
        initStartBanner();
        loading();
        initEndBanner();
        unload();
    }
    private void initMontoyaApiProvider(MontoyaApi montoyaApi) {
        MontoyaApiProvider.constructInstance(montoyaApi);
    }
    private void initTools(MontoyaApi api) {
        AutoSSRFLogger.constructAutoSSRFLogger(api);
        logger = AutoSSRFLogger.INSTANCE;
        HttpProvider.constructHttpProvider(api);
        CollaboratorProvider.constructCollaboratorProvider(api);
        UIProvider.constructUIProvider(api);
    }
    private void initStartBanner() {
        logger.logToOutput("  ____ ____  ____  _____ ____       _            _             \n" +
                " / ___/ ___||  _ \\|  ___|  _ \\  ___| |_ ___  ___| |_ ___  _ __ \n" +
                " \\___ \\___ \\| |_) | |_  | | | |/ _ \\ __/ _ \\/ __| __/ _ \\| '__|\n" +
                "  ___) |__) |  _ <|  _| | |_| |  __/ ||  __/ (__| || (_) | |   \n" +
                " |____/____/|_| \\_\\_|   |____/ \\___|\\__\\___|\\___|\\__\\___/|_|   \n" +
                "                                                               ");
        logger.logToOutput("Author: KaGty1");
        logger.logToOutput("Github: https://github.com/kaGty1");
        logger.logToOutput("Blog: https://KaGty1.github.io");
        logger.logToOutput("The plugin is loading ....");
    }
    private void loading() {
        UIMain uiMain = new UIMain(uiProvider);
        uiProvider.registerSuiteTab("SSRF_Detector", uiMain);
        logger.logToOutput(uiMain.toString());
        SSRFScanCheck ssrfScanCheck = new SSRFScanCheck();
        montoyaApiProvider.registerScanCheck(ssrfScanCheck);
        SSRFHttpHandler ssrfHttpHandler = new SSRFHttpHandler();
        HttpProvider.INSTANCE.registerHttpHandler(ssrfHttpHandler);
    }
    private void initEndBanner() {
        logger.logToOutput("插件加载完成");
    }
    private void unload() {
        Extension extension = montoyaApiProvider.getMontoyaApi().extension();
        extension.registerUnloadingHandler(() -> {
            CollaboratorThreadPool.INSTANCE.getPool().shutdownNow();
            UIThreadPool.INSTANCE.getPool().shutdownNow();
        });
    }
}
