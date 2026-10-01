package nexus.io.tio.http.common;
import static org.junit.Assert.*;
import org.junit.Test;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import nexus.io.tio.core.exception.TioDecodeException;

public class HttpFramingTest {
  @Test public void rejectsWhitespaceAroundHeaderNamesAndWhitespaceOnlySeparators() throws Exception {
    for (String header : new String[]{"Content-Length : 0", " Content-Length: 0", " \t"})
      invalid("GET / HTTP/1.1\r\n" + header + "\r\n\r\n");
  }
  private void invalid(String message) throws Exception {
    ByteBuffer b=ByteBuffer.wrap(message.getBytes(StandardCharsets.US_ASCII));
    try {HttpRequestDecoder.decode(b,b.limit(),0,b.remaining(),null,new HttpConfig(0,false));fail("invalid framing accepted");}
    catch(TioDecodeException expected){}
  }
  @Test public void rejectsInvalidContentLengths() throws Exception {
    for(String value:new String[]{"-1","+1","x","", "2147483648","99999999999999999999999","0, 1"})
      invalid("POST / HTTP/1.1\r\nHost: local\r\nContent-Length: "+value+"\r\n\r\n");
  }
  @Test public void rejectsDuplicateAndTransferEncodingFraming() throws Exception {
    for(String headers:new String[]{"Content-Length: 1\r\nContent-Length: 0", "Content-Length: 0\r\nContent-Length: 0", "Transfer-Encoding: chunked", "Transfer-Encoding: chunked\r\nContent-Length: 0"})
      invalid("POST / HTTP/1.1\r\nHost: local\r\n"+headers+"\r\n\r\n");
  }
  @Test public void rejectsCompleteAndIncompleteOversizedRequestLines() throws Exception {
    String path=String.join("",Collections.nCopies(9000,"x"));invalid("GET /"+path+" HTTP/1.1\r\n\r\n");invalid("GET /"+path);
  }
  @Test public void rejectsIncompleteOversizedHeader() throws Exception {
    invalid("GET / HTTP/1.1\r\nX: "+String.join("",Collections.nCopies(40000,"x")));
  }
  @Test public void rejectsOversizedHeaderCollection() throws Exception {
    invalid("GET / HTTP/1.1\r\n"+String.join("",Collections.nCopies(4000,"X: a\r\n"))+"\r\n");
  }
  @Test public void fragmentedValidHeaderRemainsIncomplete() throws Exception {
    ByteBuffer b=ByteBuffer.wrap("GET / HTTP/1.1\r\nHost: loc".getBytes(StandardCharsets.US_ASCII));
    assertNull(HttpRequestDecoder.decode(b,b.limit(),0,b.remaining(),null,new HttpConfig(0,false)));
  }
}
