package com.litongjava.tio.http.common;

public interface ResponseHeaderKey {
  // Set-Cookie: UserID=JohnDoe; Max-Age=3600; Version=1
  String Set_Cookie = "Set-Cookie".toLowerCase(); // Set-Cookie: UserID=JohnDoe; Max-Age=3600; Version=1
  String Content_Length = "Content-Length".toLowerCase(); // 65
  String Cache_Control = "Cache-Control".toLowerCase(); // "public, max-age:86400"
  String ETag = "ETag";
  String Connection = "Connection".toLowerCase(); // Upgrade, keep-alive
  String Keep_Alive = "Keep-Alive".toLowerCase(); // Keep-Alive:timeout=20
  String Sec_WebSocket_Accept = "Sec-WebSocket-Accept".toLowerCase();
  String Upgrade = "Upgrade".toLowerCase();
  String Access_Control_Allow_Credentials = "Access-Control-Allow-Credentials".toLowerCase();

  /**
   * Content-Disposition: attachment;filename=FileName.txt 文件下载
   */
  String Content_Disposition = "Content-Disposition".toLowerCase();

  String Transfer_Encoding = "Transfer-Encoding".toLowerCase();
  /**
   * 文档的编码（Encode）方法。只有在解码之后才可以得到Content-Type头指定的内容类型。
   * 利用gzip压缩文档能够显著地减少HTML文档的下载时间。
   * Java的GZIPOutputStream可以很方便地进行gzip压缩，但只有Unix上的Netscape和Windows上的IE 4、IE 5才支持它。
   * 因此，Servlet应该通过查看Accept-Encoding头（即request.getHeader("Accept-Encoding")）检查浏览器是否支持gzip，
   * 为支持gzip的浏览器返回经gzip压缩的HTML页面，为其他浏览器返回普通页面。
   */
  String Content_Encoding = "Content-Encoding".toLowerCase();
  /**
   * 表示后面的文档属于什么MIME类型。Servlet默认为text/plain，但通常需要显式地指定为text/html。
   * 由于经常要设置Content-Type，因此HttpServletResponse提供了一个专用的方法setContentType。
   */
  String Content_Type = "Content-Type".toLowerCase();
  /**
   * 当前的GMT时间。你可以用setDateHeader来设置这个头以避免转换时间格式的麻烦。
   */
  String Date = "Date".toLowerCase();
  /**
   * 应该在什么时候认为文档已经过期，从而不再缓存它？
   */
  String Expires = "Expires".toLowerCase();
  String Pragma = "Pragma".toLowerCase();
  /**
   * 文档的最后改动时间。客户可以通过If-Modified-Since请求头提供一个日期，该请求将被视为一个条件GET，
   * 只有改动时间迟于指定时间的文档才会返回，否则返回一个304（Not
   * Modified）状态。Last-Modified也可用setDateHeader方法来设置。
   */
  String Last_Modified = "Last-Modified".toLowerCase();
  /**
   * 表示客户应当到哪里去提取文档。Location通常不是直接设置的，而是通过HttpServletResponse的sendRedirect方法，该方法同时设置状态代码为302。
   */
  String Location = "Location".toLowerCase();
  /**
   * 表示浏览器应该在多少时间之后刷新文档，以秒计。除了刷新当前文档之外，你还可以通过setHeader("Refresh", "5;
   * URL=http://host/path")让浏览器读取指定的页面。 注意这种功能通常是通过设置HTML页面HEAD区的＜META
   * HTTP-EQUIV="Refresh"
   * CONTENT="5;URL=http://host/path"＞实现，这是因为，自动刷新或重定向对于那些不能使用CGI或Servlet的HTML编写者十分重要。但是，对于Servlet来说，直接设置Refresh头更加方便。
   * 
   * 注意Refresh的意义是"N秒之后刷新本页面或访问指定页面"，而不是"每隔N秒刷新本页面或访问指定页面"。因此，连续刷新要求每次都发送一个Refresh头，而发送204状态代码则可以阻止浏览器继续刷新，不管是使用Refresh头还是＜META
   * HTTP-EQUIV="Refresh" ...＞。
   * 
   * 注意Refresh头不属于HTTP 1.1正式规范的一部分，而是一个扩展，但Netscape和IE都支持它。
   */
  String Refresh = "Refresh".toLowerCase();
  /**
   * 服务器名字。Servlet一般不设置这个值，而是由Web服务器自己设置。
   */
  String Server = "Server".toLowerCase();

  /**
   *
   */
  String Access_Control_Allow_Origin = "Access-Control-Allow-Origin".toLowerCase(); // value: *

  String Access_Control_Allow_Methods = "Access-Control-Allow-Methods".toLowerCase();

  String Access_Control_Max_Age = "Access-Control-Max-Age".toLowerCase();
  /**
   *
   */
  String Access_Control_Allow_Headers = "Access-Control-Allow-Headers".toLowerCase(); // .toLowerCase(); //value:
                                                                                      // x-requested-with,content-type

  /**
   * 是否是从缓存中获取的数据，tio-httpserver特有的头部信息
   */
  String tio_from_cache = "tio-from-cache";

  /**
   * 
   */
  String tio_webpack_used_cache = "tio-webpack-used-cache";
  String vary = "vary";
  String allow = "allow";
  String origin = "origin";
  String x_content_type_options = "X-Content-Type-Options".toLowerCase();
  String referrer_policy = "Referrer-Policy".toLowerCase();
  String cross_origin_opener_policy = "Cross-Origin-Opener-Policy".toLowerCase();
  String Accept_Ranges = "Accept-Ranges".toLowerCase();
  String Content_Range = "Content-Range".toLowerCase();
}