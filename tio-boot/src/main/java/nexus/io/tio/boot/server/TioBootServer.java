package nexus.io.tio.boot.server;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;

import nexus.io.context.ServerListener;
import nexus.io.hook.HookCan;
import nexus.io.tio.boot.aspect.IGateWayCheckAspect;
import nexus.io.tio.boot.aspect.IRequiresAuthenticationAspect;
import nexus.io.tio.boot.aspect.IRequiresPermissionsAspect;
import nexus.io.tio.boot.decode.TioDecodeExceptionHandler;
import nexus.io.tio.boot.email.EmailSender;
import nexus.io.tio.boot.encrypt.TioEncryptor;
import nexus.io.tio.boot.exception.TioBootExceptionHandler;
import nexus.io.tio.boot.http.controller.ControllerInterceptor;
import nexus.io.tio.boot.http.handler.controller.TioBootHttpControllerRouter;
import nexus.io.tio.boot.http.handler.internal.RequestStatisticsHandler;
import nexus.io.tio.boot.http.handler.internal.ResponseStatisticsHandler;
import nexus.io.tio.boot.http.handler.internal.StaticResourceHandler;
import nexus.io.tio.boot.http.interceptor.HttpInteceptorConfigure;
import nexus.io.tio.boot.logging.LoggingInterceptor;
import nexus.io.tio.boot.swagger.TioSwaggerV2Config;
import nexus.io.tio.boot.user.UserAuthentication;
import nexus.io.tio.boot.watch.DirectoryWatcher;
import nexus.io.tio.boot.websocket.WebSocketRouter;
import nexus.io.tio.http.common.HttpConfig;
import nexus.io.tio.http.common.handler.ITioHttpRequestHandler;
import nexus.io.tio.http.server.handler.HttpRequestHandler;
import nexus.io.tio.http.server.intf.HttpRequestInterceptor;
import nexus.io.tio.http.server.router.HttpRequestFunctionRouter;
import nexus.io.tio.http.server.router.HttpRequestGroovyRouter;
import nexus.io.tio.http.server.router.HttpRequestRouter;
import nexus.io.tio.server.ServerTioConfig;
import nexus.io.tio.server.TioServer;
import nexus.io.tio.server.intf.ServerAioHandler;
import nexus.io.tio.server.intf.ServerAioListener;
import nexus.io.tio.utils.context.TioAppCan;
import nexus.io.tio.websocket.server.WebsocketServerConfig;
import nexus.io.tio.websocket.server.handler.IWebSocketHandler;

public class TioBootServer {

  private static TioBootServer me = new TioBootServer();

  public static TioBootServer me() {
    return me;
  }

  private TioBootServer() {
  }

  private TioServer tioServer;
  private ServerTioConfig serverTioConfig;
  private HttpConfig httpConfig;
  private WebsocketServerConfig wsServerConfig;

  private ITioHttpRequestHandler httpRequestDispatcher;

  private HttpRequestInterceptor httpRequestValidationInterceptor;
  private ControllerInterceptor controllerInterceptor;
  private LoggingInterceptor loggingInterceptor;
  private HttpRequestInterceptor httpRequestInterceptorDispatcher;

  private IWebSocketHandler webSocketHandlerDispatcher;

  private HttpInteceptorConfigure httpInteceptorConfigure;
  private WebSocketRouter webSocketRouter;
  private Integer workThreadNum;
  private ThreadFactory workThreadFactory;
  private ExecutorService bizExecutor;

  /**
   * 服务监听器
   */
  private ServerListener tioBootServerListener;

  /**
   * routes
   */
  private TioBootHttpControllerRouter controllerRouter;

  /**
   * httpRoutes
   */
  private HttpRequestRouter requestRouter;

  /**
   * httpReqeustGroovyRoute
   */
  private HttpRequestGroovyRouter requestGroovyRouter;

  /**
   * 
   */
  private HttpRequestFunctionRouter requestFunctionRouter;

  /**
   * ServerTcpHandler
   */
  private ServerAioHandler serverAioHandler;
  /**
   * 
   */
  private ServerAioListener serverAioListener;

  private RequestStatisticsHandler requestStatisticsHandler;

  private ResponseStatisticsHandler responseStatisticsHandler;

  private TioBootExceptionHandler exceptionHandler;

  private TioDecodeExceptionHandler decodeExceptionHandler;

  private IGateWayCheckAspect gateWayCheckAspect;
  private IRequiresAuthenticationAspect requiresAuthenticationAspect;
  private IRequiresPermissionsAspect requiresPermissionsAspect;
  /**
   * user authentication
   */
  private UserAuthentication userAuthentication;

  /**
   * Forward to other system
   */
  private HttpRequestHandler forwardHandler;
  private StaticResourceHandler staticResourceHandler;
  /**
   * Not Found
   */
  private HttpRequestHandler notFoundHandler;

  private TioSwaggerV2Config swaggerV2Config;
  private EmailSender emailSender;

  private DirectoryWatcher staticResourcesDirectoryWatcher;
  private TioEncryptor tioEncryptor;

  /**
   * @param serverTioConfig
   * @param wsServerConfig
   * @param httpConfig
   */
  public void init(ServerTioConfig serverTioConfig, WebsocketServerConfig wsServerConfig, HttpConfig httpConfig) {
    this.tioServer = new TioServer(serverTioConfig);
    this.serverTioConfig = serverTioConfig;
    this.wsServerConfig = wsServerConfig;
    this.httpConfig = httpConfig;
  }

  public void start(String bindIp, Integer bindPort) throws IOException {
    tioServer.start(bindIp, bindPort);
  }

  /**
   * 关闭
   * 
   * @return
   */
  public boolean stop() {
    boolean stop = tioServer.stop();
    HookCan.me().stop();
    TioAppCan.me().clean();
    me = new TioBootServer();
    return stop;
  }

  public boolean isRunning() {
    return tioServer != null;
  }

  public TioServer getTioServer() {
    return tioServer;
  }

  public WebsocketServerConfig getWsServerConfig() {
    return wsServerConfig;
  }

  public HttpConfig getHttpConfig() {
    return httpConfig;
  }

  public static TioBootServer create() {
    return me;
  }

  public TioBootServer runOn() {
    return this;
  }

  public TioBootServer bindAddress(Object object) {
    return null;
  }

  public ServerTioConfig getServerTioConfig() {
    return serverTioConfig;
  }

  public void setServerTioConfig(ServerTioConfig serverTioConfig) {
    this.serverTioConfig = serverTioConfig;
  }

  public ITioHttpRequestHandler getHttpRequestDispatcher() {
    return httpRequestDispatcher;
  }

  public void setHttpRequestDispatcher(ITioHttpRequestHandler httpRequestDispatcher) {
    this.httpRequestDispatcher = httpRequestDispatcher;
  }

  public HttpRequestInterceptor getHttpRequestValidationInterceptor() {
    return httpRequestValidationInterceptor;
  }

  public void setHttpRequestValidationInterceptor(HttpRequestInterceptor httpRequestValidationInterceptor) {
    this.httpRequestValidationInterceptor = httpRequestValidationInterceptor;
  }

  public ControllerInterceptor getControllerInterceptor() {
    return controllerInterceptor;
  }

  public void setControllerInterceptor(ControllerInterceptor controllerInterceptor) {
    this.controllerInterceptor = controllerInterceptor;
  }

  public LoggingInterceptor getLoggingInterceptor() {
    return loggingInterceptor;
  }

  public void setLoggingInterceptor(LoggingInterceptor loggingInterceptor) {
    this.loggingInterceptor = loggingInterceptor;
  }

  public HttpRequestInterceptor getHttpRequestInterceptorDispatcher() {
    return httpRequestInterceptorDispatcher;
  }

  public void setHttpRequestInterceptorDispatcher(HttpRequestInterceptor httpRequestInterceptorDispatcher) {
    this.httpRequestInterceptorDispatcher = httpRequestInterceptorDispatcher;
  }

  public IWebSocketHandler getWebSocketHandlerDispatcher() {
    return webSocketHandlerDispatcher;
  }

  public void setWebSocketHandlerDispatcher(IWebSocketHandler webSocketHandlerDispatcher) {
    this.webSocketHandlerDispatcher = webSocketHandlerDispatcher;
  }

  public HttpInteceptorConfigure getHttpInteceptorConfigure() {
    return httpInteceptorConfigure;
  }

  public void setHttpInteceptorConfigure(HttpInteceptorConfigure httpInteceptorConfigure) {
    this.httpInteceptorConfigure = httpInteceptorConfigure;
  }

  public WebSocketRouter getWebSocketRouter() {
    return webSocketRouter;
  }

  public void setWebSocketRouter(WebSocketRouter webSocketRouter) {
    this.webSocketRouter = webSocketRouter;
  }

  public Integer getWorkThreadNum() {
    return workThreadNum;
  }

  public void setWorkThreadNum(Integer workThreadNum) {
    this.workThreadNum = workThreadNum;
  }

  public ThreadFactory getWorkThreadFactory() {
    return workThreadFactory;
  }

  public void setWorkThreadFactory(ThreadFactory workThreadFactory) {
    this.workThreadFactory = workThreadFactory;
  }

  public ExecutorService getBizExecutor() {
    return bizExecutor;
  }

  public void setBizExecutor(ExecutorService bizExecutor) {
    this.bizExecutor = bizExecutor;
  }

  public ServerListener getTioBootServerListener() {
    return tioBootServerListener;
  }

  public void setTioBootServerListener(ServerListener tioBootServerListener) {
    this.tioBootServerListener = tioBootServerListener;
  }

  public TioBootHttpControllerRouter getControllerRouter() {
    return controllerRouter;
  }

  public void setControllerRouter(TioBootHttpControllerRouter controllerRouter) {
    this.controllerRouter = controllerRouter;
  }

  public HttpRequestRouter getRequestRouter() {
    return requestRouter;
  }

  public void setRequestRouter(HttpRequestRouter requestRouter) {
    this.requestRouter = requestRouter;
  }

  public HttpRequestGroovyRouter getRequestGroovyRouter() {
    return requestGroovyRouter;
  }

  public void setRequestGroovyRouter(HttpRequestGroovyRouter requestGroovyRouter) {
    this.requestGroovyRouter = requestGroovyRouter;
  }

  public HttpRequestFunctionRouter getRequestFunctionRouter() {
    return requestFunctionRouter;
  }

  public void setRequestFunctionRouter(HttpRequestFunctionRouter requestFunctionRouter) {
    this.requestFunctionRouter = requestFunctionRouter;
  }

  public ServerAioHandler getServerAioHandler() {
    return serverAioHandler;
  }

  public void setServerAioHandler(ServerAioHandler serverAioHandler) {
    this.serverAioHandler = serverAioHandler;
  }

  public ServerAioListener getServerAioListener() {
    return serverAioListener;
  }

  public void setServerAioListener(ServerAioListener serverAioListener) {
    this.serverAioListener = serverAioListener;
  }

  public RequestStatisticsHandler getRequestStatisticsHandler() {
    return requestStatisticsHandler;
  }

  public void setRequestStatisticsHandler(RequestStatisticsHandler requestStatisticsHandler) {
    this.requestStatisticsHandler = requestStatisticsHandler;
  }

  public ResponseStatisticsHandler getResponseStatisticsHandler() {
    return responseStatisticsHandler;
  }

  public void setResponseStatisticsHandler(ResponseStatisticsHandler responseStatisticsHandler) {
    this.responseStatisticsHandler = responseStatisticsHandler;
  }

  public TioBootExceptionHandler getExceptionHandler() {
    return exceptionHandler;
  }

  public void setExceptionHandler(TioBootExceptionHandler exceptionHandler) {
    this.exceptionHandler = exceptionHandler;
  }

  public TioDecodeExceptionHandler getDecodeExceptionHandler() {
    return decodeExceptionHandler;
  }

  public void setDecodeExceptionHandler(TioDecodeExceptionHandler decodeExceptionHandler) {
    this.decodeExceptionHandler = decodeExceptionHandler;
  }

  public IGateWayCheckAspect getGateWayCheckAspect() {
    return gateWayCheckAspect;
  }

  public void setGateWayCheckAspect(IGateWayCheckAspect gateWayCheckAspect) {
    this.gateWayCheckAspect = gateWayCheckAspect;
  }

  public IRequiresAuthenticationAspect getRequiresAuthenticationAspect() {
    return requiresAuthenticationAspect;
  }

  public void setRequiresAuthenticationAspect(IRequiresAuthenticationAspect requiresAuthenticationAspect) {
    this.requiresAuthenticationAspect = requiresAuthenticationAspect;
  }

  public IRequiresPermissionsAspect getRequiresPermissionsAspect() {
    return requiresPermissionsAspect;
  }

  public void setRequiresPermissionsAspect(IRequiresPermissionsAspect requiresPermissionsAspect) {
    this.requiresPermissionsAspect = requiresPermissionsAspect;
  }

  public UserAuthentication getUserAuthentication() {
    return userAuthentication;
  }

  public void setUserAuthentication(UserAuthentication userAuthentication) {
    this.userAuthentication = userAuthentication;
  }

  public HttpRequestHandler getForwardHandler() {
    return forwardHandler;
  }

  public void setForwardHandler(HttpRequestHandler forwardHandler) {
    this.forwardHandler = forwardHandler;
  }

  public StaticResourceHandler getStaticResourceHandler() {
    return staticResourceHandler;
  }

  public void setStaticResourceHandler(StaticResourceHandler staticResourceHandler) {
    this.staticResourceHandler = staticResourceHandler;
  }

  public HttpRequestHandler getNotFoundHandler() {
    return notFoundHandler;
  }

  public void setNotFoundHandler(HttpRequestHandler notFoundHandler) {
    this.notFoundHandler = notFoundHandler;
  }

  public TioSwaggerV2Config getSwaggerV2Config() {
    return swaggerV2Config;
  }

  public void setSwaggerV2Config(TioSwaggerV2Config swaggerV2Config) {
    this.swaggerV2Config = swaggerV2Config;
  }

  public EmailSender getEmailSender() {
    return emailSender;
  }

  public void setEmailSender(EmailSender emailSender) {
    this.emailSender = emailSender;
  }

  public DirectoryWatcher getStaticResourcesDirectoryWatcher() {
    return staticResourcesDirectoryWatcher;
  }

  public void setStaticResourcesDirectoryWatcher(DirectoryWatcher staticResourcesDirectoryWatcher) {
    this.staticResourcesDirectoryWatcher = staticResourcesDirectoryWatcher;
  }

  public TioEncryptor getTioEncryptor() {
    return tioEncryptor;
  }

  public void setTioEncryptor(TioEncryptor tioEncryptor) {
    this.tioEncryptor = tioEncryptor;
  }

  public void setTioServer(TioServer tioServer) {
    this.tioServer = tioServer;
  }

  public void setHttpConfig(HttpConfig httpConfig) {
    this.httpConfig = httpConfig;
  }

  public void setWsServerConfig(WebsocketServerConfig wsServerConfig) {
    this.wsServerConfig = wsServerConfig;
  }
}
