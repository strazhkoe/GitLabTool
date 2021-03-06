package com.lgc.gitlabtool.git.main;

import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.lgc.gitlabtool.git.connections.RESTConnector;
import com.lgc.gitlabtool.git.entities.MessageType;
import com.lgc.gitlabtool.git.exceptions.Log4jExceptionHandler;
import com.lgc.gitlabtool.git.services.ConsoleService;
import com.lgc.gitlabtool.git.services.ServiceProvider;
import com.lgc.gitlabtool.git.ui.UserInterface;
import com.lgc.gitlabtool.git.ui.javafx.JavaFXUI;
import com.lgc.gitlabtool.git.util.ProjectPropertiesUtil;


public class Main {

    private static final Logger logger = LogManager.getLogger(Main.class);

    private static final ConsoleService _consoleService = ServiceProvider.getInstance()
            .getService(ConsoleService.class);

    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler(new Log4jExceptionHandler());
        final UserInterface ui = new JavaFXUI();
        logger.debug("==================== application started");
        _consoleService.addMessage(ProjectPropertiesUtil.getProjectName().toUpperCase() +
                                   " version: " + ProjectPropertiesUtil.getProjectVersion() +
                                   " (" + ProjectPropertiesUtil.getBuildTimestamp() + " "
                                    + ProjectPropertiesUtil.getCommitHash() + ") ", MessageType.SIMPLE);
        detectProxy();
        ui.run(args);
    }

	/**
	* This method detect proxy...
	*/
    private static void detectProxy() {
        System.setProperty("java.net.useSystemProxies", "true");
        logger.info("detecting proxies");
        List<?> l = null;
        try {
            l = ProxySelector.getDefault().select(new URI(RESTConnector.URL_MAIN_PART));
        }
        catch (URISyntaxException e) {
            logger.error("", e);
        }
        if (l != null) {
            for (Object name : l) {
                java.net.Proxy proxy = (java.net.Proxy) name;
                logger.info("proxy type: " + proxy.type());

                InetSocketAddress addr = (InetSocketAddress) proxy.address();

                if (addr == null) {
                    logger.info("No Proxy");
                } else {
                    logger.info("proxy hostname: " + addr.getHostName());
                    logger.info("proxy port: " + addr.getPort());
                    System.setProperty("http.proxyHost", addr.getHostName());
                    System.setProperty("http.proxyPort", Integer.toString(addr.getPort()));
                }
            }
        }
    }
}
