package invar;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

final public class InvarReadData
{
    static public String                   charset      = "utf-8";
    static public Boolean                  verbose      = false;
    static public HashMap<String,Class<?>> aliasBasics  = null;
    static public HashMap<String,Class<?>> aliasEnums   = null;
    static public HashMap<String,Class<?>> aliasStructs = null;
    static private String                  suffix;

    static public void start (Object root, String path, String suffix) throws Exception
    {
        InvarReadData.suffix = suffix;
        File file = new File(path);
        if (!file.exists())
            throw new IOException("Path doesn't exist:\n" + file.getAbsolutePath());
        if (aliasBasics == null)
            throw new Exception("InvarReadData.aliasBasics is null");
        if (aliasEnums == null)
            throw new Exception("InvarReadData.aliasEnums is null");
        if (aliasStructs == null)
            throw new Exception("InvarReadData.aliasStructs is null");
        FilenameFilter filter = new FilenameFilter()
        {
            @Override
            public boolean accept (File dir, String name)
            {
                File f = new File(dir, name);
                if (f.isDirectory() && !f.getName().startsWith("."))
                    return true;
                if (name.endsWith(InvarReadData.suffix))
                    return true;
                return false;
            }
        };
        List<File> files = new ArrayList<File>();
        recursiveReadFile(files, file, filter);
        for (File f : files)
        {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(f);
            if (!doc.hasChildNodes())
                return;
            log("Read <- " + f.getAbsolutePath());
            NodeList children = doc.getChildNodes();
            int len = children.getLength();
            for (int i = 0; i < len; i++)
            {
                Node x = children.item(i);
                if (Node.ELEMENT_NODE == x.getNodeType())
                {
                    new InvarReadData(f.getAbsolutePath()).parse(root, x);
                    break;
                }
            }
        }
    }

    static public void parse (Object o, String xml) throws Exception
    {
        if (null == xml || 0 == xml.length())
            return;
        Document doc = DocumentBuilderFactory.newInstance()
                                             .newDocumentBuilder()
                                             .parse(new ByteArrayInputStream(xml.getBytes(charset)));
        if (!doc.hasChildNodes())
            return;

        NodeList children = doc.getChildNodes();
        int len = children.getLength();
        for (int i = 0; i < len; i++)
        {
            Node x = children.item(i);
            if (Node.ELEMENT_NODE == x.getNodeType())
            {
                new InvarReadData("").parsePart(o, x);
                break;
            }
        }
    }

    static public void parseFull (Object root, String xml) throws Exception
    {
        if (null == xml || 0 == xml.length())
            return;
        Document doc = DocumentBuilderFactory.newInstance()
                                             .newDocumentBuilder()
                                             .parse(new ByteArrayInputStream(xml.getBytes(charset)));
        if (!doc.hasChildNodes())
            return;

        NodeList children = doc.getChildNodes();
        int len = children.getLength();
        for (int i = 0; i < len; i++)
        {
            Node x = children.item(i);
            if (Node.ELEMENT_NODE == x.getNodeType())
            {
                new InvarReadData("NotFile").parse(root, x);
                break;
            }
        }
    }

    public InvarReadData(String path)
    {
        this.path = path;
    }
    
    public InvarReadData()
    {
        this.path = "";
    }

    public void parse (Object o, Node n) throws Exception
    {
        this.path = n.getBaseURI();
        parse(o, n, o.getClass().getName(), "");
    }

    private void parsePart (Object host, Node n) throws Exception
    {
        String key = n.getNodeName();
        String rule = getRule(host.getClass(), key, n);
        Object o = invokeGetter(key, host, n);
        parse(o, n, rule, key);
    }

    private String path;

    @SuppressWarnings("unchecked")
    private void parse (Object o, Node n, String rule, String debug) throws Exception
    {
        if (o == null)
            onError(debug + " is null.", n);
        Class<?> ClsO = loadGenericClass(rule);
        if (LinkedList.class == ClsO)
            parseVec((LinkedList<Object>)o, n, rule, debug);
        else if (HashMap.class == ClsO)
            parseMap((HashMap<Object,Object>)o, n, rule, debug);
        else
            parseStruct(o, n, rule, debug);
    }

    private void parseStruct (Object o, Node n, String rule, String debug) throws Exception
    {
        Class<?> ClsO = loadGenericClass(rule);
        if (o.getClass().getName() != ClsO.getName())
            onError("Object does not matches this rule: " + rule, n);
        NamedNodeMap attrs = n.getAttributes();
        if (attrs == null)
        {
            onError("Node unavailable: " + rule, n);
            return;
        }
        int attrsLen = attrs.getLength();
        for (int i = 0; i < attrsLen; i++)
        {
            Node x = attrs.item(i);
            String key = x.getNodeName();
            if (key.indexOf(":") >= 0)
                continue;
            String ruleX = getRule(ClsO, key, n);
            if (ruleX == null)
                continue;
            Class<?> ClsX = loadGenericClass(ruleX);
            String vStr = x.getNodeValue();
            Object v = parseSimple(ClsX, vStr, ruleX, debug + '.' + key, n);
            invokeSetter(v, key, o, n);
        }
        NodeList children = n.getChildNodes();
        int len = children.getLength();
        for (int i = 0; i < len; i++)
        {
            Node x = children.item(i);
            if (Node.ELEMENT_NODE != x.getNodeType())
                continue;
            String key = x.getNodeName();
            String ruleX = getRule(ClsO, key, n);
            if (ruleX == null)
                continue;
            Class<?> ClsX = loadGenericClass(ruleX);
            String vStr = null;
            Object v = null;
            if (isSimpleType(ClsX))
            {
                vStr = getAttr(x, ATTR_VALUE);
                v = parseSimple(ClsX, vStr, ruleX, debug + '.' + key, x);
                invokeSetter(v, key, o, x);
            }
            else
            {
                v = invokeGetter(key, o, x);
                if (v == null && ClsX != Vector.class)
                {
                    v = ClsX.newInstance();
                    invokeSetter(v, key, o, x);
                }
                parse(v, x, ruleX, debug + '.' + key);
            }
        }
    }

    private void parseVec (LinkedList<Object> list, Node n, String rule, String debug) throws Exception
    {
        String R = ruleRight(rule);
        if (R == null)
            onError("Unexpected type: " + rule, n);
        Class<?> Cls = loadGenericClass(R);
        NodeList children = n.getChildNodes();
        int len = children.getLength();
        for (int i = 0; i < len; i++)
        {
            Node vn = children.item(i);
            if (Node.ELEMENT_NODE != vn.getNodeType())
                continue;
            Object v = parseGenericChild(vn, Cls, R, debug + "[" + list.size() + "]");
            list.add(v);
        }
    }

    private void parseMap (HashMap<Object,Object> map, Node n, String rule, String debug) throws Exception
    {
        String R = ruleRight(rule);
        if (R == null)
            onError("Unexpected type: " + rule.toString(), n);
        String[] typeNames = R.split(GENERIC_SPLIT);
        if (typeNames.length != 2)
            onError("Unexpected type: " + rule.toString(), n);
        Class<?> ClsK = loadGenericClass(typeNames[0]);
        Class<?> ClsV = loadGenericClass(typeNames[1]);
        List<Node> children = elementNodes(n);
        int len = children.size();
        if (isSimpleType(ClsK))
        {
            for (int i = 0; i < len; i++)
            {
                Node vn = children.get(i);
                String s = getAttr(vn, ATTR_MAP_KEY);
                Object k = parseSimple(ClsK, s, typeNames[0], debug + ".k", vn);
                Object v = parseGenericChild(vn, ClsV, typeNames[1], debug + ".v");
                map.put(k, v);
            }
        }
        else
        {
            if ((0x01 & len) != 0)
                onError("Invaid amount of children: " + len, n);
            for (int i = 0; i < len; i += 2)
            {
                Node kn = children.get(i);
                Node vn = children.get(i + 1);
                Object k = parseGenericChild(kn, ClsK, typeNames[0], debug + ".k");
                Object v = parseGenericChild(vn, ClsV, typeNames[1], debug + ".v");
                map.put(k, v);
            }
        }
    }

    private Object parseGenericChild (Node cn, Class<?> Cls, String rule, String debug) throws Exception
    {
        if (isSimpleType(Cls))
            return parseSimple(Cls, getAttr(cn, ATTR_VALUE), rule, debug, cn);
        else
        {
            Object co = Cls.newInstance();
            parse(co, cn, rule, debug);
            return co;
        }
    }

    private Object parseSimple (Class<?> vType, String s, String rule, String debug, Node x) throws Exception
    {
        if (rule.equals("int8"))
            checkNumber(s, -0x80L, 0x7FL, debug, x);
        else if (rule.equals("int16"))
            checkNumber(s, -0x8000L, 0x7FFFL, debug, x);
        else if (rule.equals("int32"))
            checkNumber(s, -0x80000000L, 0x7FFFFFFFL, debug, x);
        else if (rule.equals("uint8"))
            checkNumber(s, 0L, 0xFFL, debug, x);
        else if (rule.equals("uint16"))
            checkNumber(s, 0L, 0xFFFFL, debug, x);
        else if (rule.equals("uint32"))
            checkNumber(s, 0L, 0xFFFFFFFFL, debug, x);
        else
        {
        }
        Object arg = null;
        if (String.class == vType)
            arg = s;
        else if (Byte.class == vType)
            arg = Byte.decode(s);
        else if (Short.class == vType)
            arg = Short.decode(s);
        else if (Integer.class == vType)
            arg = Integer.decode(s);
        else if (Long.class == vType)
            arg = Long.decode(s);
        else if (Float.class == vType)
            arg = Float.valueOf(s);
        else if (Double.class == vType)
            arg = Double.valueOf(s);
        else if (Boolean.class == vType)
            arg = Boolean.valueOf(s);
        else if (aliasEnums.containsValue(vType))
        {
            arg = parseEnumObject(vType, s);
            if (arg == null)
                onError("Enum value is invalid.", x);
        }
        else
        {
            onError("Not a simple value.", x);
        }
        if (verbose)
        {
            StringBuilder code = new StringBuilder();
            code.append(fixedLen(40, debug));
            code.append(" : ");
            code.append(fixedLen(32, rule));
            code.append(" : ");
            code.append(arg);
            log(code);
        }
        return arg;
    }

    private void checkNumber (String s, Long min, Long max, String debug, Node x) throws Exception
    {
        Long v = Long.decode(s);
        if (v < min || v > max)
        {
            onError(debug + " = " + v + ". Number is out of range [" + min + ", " + max + "]", x);
        }
    }

    private static Object parseEnumObject (Class<?> type, String s) throws Exception
    {
        Integer v = Integer.parseInt(s);
        Object o = null;
        Method[] mets = type.getMethods();
        for (Method m : mets)
        {
            if (!m.getName().equals("parse"))
                continue;
            o = m.invoke(type, v);
            break;
        }
        return o;
    }

    private String getAttr (Node n, String name) throws Exception
    {
        String v = getAttrOptional(n, name);
        if (v.equals(""))
            onError("Attribute '" + name + "' is required.", n);
        return v;
    }

    private String getRule (Class<?> ClsO, String key, Node n) throws Exception
    {
        HashMap<String,Method> map = getGetters(ClsO);
        Method method = map.get(key);
        if (method == null)
        {
            String nameGetter = PREFIX_GETTER + upperHeadChar(key);
            method = map.get(nameGetter);
            if (method == null && key != ATTR_MAP_KEY)
                onError("No getter named '" + nameGetter + "' in " + ClsO, n);
        }
        if (method == null)
            return null;
        String rule = method.getGenericReturnType().toString();
        InvarRule anno = method.getAnnotation(InvarRule.class);
        if (anno != null)
            rule = anno.T();
        return rule;
    }

    private Object invokeGetter (String key, Object o, Node x) throws Exception
    {
        HashMap<String,Method> map = getGetters(o.getClass());
        Method method = map.get(key);
        if (method == null)
        {
            String nameGetter = PREFIX_GETTER + upperHeadChar(key);
            method = map.get(nameGetter);
            if (method == null)
            {
                onError("No getter named \"" + nameGetter + "\" in " + o.getClass(), x);
                return null;
            }
        }
        return method.invoke(o);
    }

    private void invokeSetter (Object value, String key, Object o, Node n) throws Exception
    {
        HashMap<String,Method> map = getSetters(o.getClass());
        Method method = map.get(key);
        if (method == null)
        {
            String nameSetter = PREFIX_SETTER + upperHeadChar(key);
            method = map.get(nameSetter);
            if (method == null)
            {
                onError("No setter named \"" + nameSetter + "()\" in " + o.getClass(), n);
                return;
            }
        }
        method.invoke(o, value);
    }

    private Class<?> loadGenericClass (String rule) throws Exception
    {
        String name = ruleLeft(rule);
        Class<?> Cls = getClassByAlias(name);
        if (Cls == null)
            Cls = Class.forName(name);
        if (Cls == null)
            onError("No Class matches this rule: " + rule, null);
        return Cls;
    }

    private void onError (String hint, Node n) throws Exception
    {
        throw new Exception("\n" + hint + "\n" + formatXmlNode(n) + "\n" + path);
    }

    static private final String GENERIC_LEFT  = "<";
    static private final String GENERIC_RIGHT = ">";
    static private final String GENERIC_SPLIT = ",";
    static private final String PREFIX_SETTER = "set";
    static private final String PREFIX_GETTER = "get";
    static private final String ATTR_MAP_KEY  = "key";
    static private final String ATTR_VALUE    = "value";

    static private boolean isSimpleType (Class<?> vType)
    {
        if (String.class == vType)
            return true;
        else if (Boolean.class == vType)
            return true;
        else if (Byte.class == vType)
            return true;
        else if (Short.class == vType)
            return true;
        else if (Integer.class == vType)
            return true;
        else if (Long.class == vType)
            return true;
        else if (Float.class == vType)
            return true;
        else if (Double.class == vType)
            return true;
        else if (aliasEnums.containsValue(vType))
            return true;
        else
            return false;
    }

    static private HashMap<String,Method> getSetters (Class<?> ClsO)
    {
        HashMap<String,Method> methods = mapClassSetters.get(ClsO);
        if (methods == null)
        {
            Method[] meths = ClsO.getMethods();
            methods = new HashMap<String,Method>();
            for (Method method : meths)
            {
                if (method.getName().startsWith(PREFIX_SETTER))
                {
                    methods.put(method.getName(), method);
                    InvarRule anno = method.getAnnotation(InvarRule.class);
                    if (anno != null)
                    {
                        String shortName = anno.S();
                        methods.put(shortName, method);
                    }
                }
            }
            mapClassSetters.put(ClsO, methods);
        }
        return methods;
    }

    static private HashMap<String,Method> getGetters (Class<?> ClsO)
    {
        HashMap<String,Method> methods = mapClassGetters.get(ClsO);
        if (methods == null)
        {
            Method[] meths = ClsO.getMethods();
            methods = new HashMap<String,Method>();
            for (Method method : meths)
            {
                if (method.getName().startsWith(PREFIX_GETTER))
                {
                    methods.put(method.getName(), method);
                    InvarRule anno = method.getAnnotation(InvarRule.class);
                    if (anno != null)
                    {
                        String shortName = anno.S();
                        methods.put(shortName, method);
                    }
                }
            }
            mapClassGetters.put(ClsO, methods);
        }
        return methods;
    }

    static private String ruleLeft (String rule)
    {
        String name = rule;
        if (rule.indexOf(GENERIC_LEFT) >= 0)
        {
            name = rule.substring(0, rule.indexOf(GENERIC_LEFT));
        }
        return name;
    }

    static private String ruleRight (String rule)
    {
        int iBegin = rule.indexOf(GENERIC_LEFT) + 1;
        int iEnd = rule.lastIndexOf(GENERIC_RIGHT);
        if (iBegin > 0 && iEnd > iBegin)
        {
            return rule.substring(iBegin, iEnd);
        }
        return null;
    }

    static private Class<?> getClassByAlias (String name)
    {
        Class<?> ClsN = aliasBasics.get(name);
        if (ClsN == null)
            ClsN = aliasEnums.get(name);
        if (ClsN == null)
            ClsN = aliasStructs.get(name);
        return ClsN;
    }

    static private String getAttrOptional (Node node, String name)
    {
        String v = "";
        NamedNodeMap attrs = node.getAttributes();
        if (attrs == null)
            return v;
        Node n = attrs.getNamedItem(name);
        if (n != null)
            v = n.getNodeValue();
        return v;
    }

    static private List<Node> elementNodes (Node n)
    {
        List<Node> nodes = new ArrayList<Node>();
        NodeList children = n.getChildNodes();
        int len = children.getLength();
        for (int i = 0; i < len; i++)
        {
            Node cn = children.item(i);
            if (Node.ELEMENT_NODE != cn.getNodeType())
                continue;
            nodes.add(cn);
        }
        return nodes;
    }

    static private String formatXmlNode (Node n)
    {
        NamedNodeMap attrs = n.getAttributes();
        StringBuilder code = new StringBuilder();
        code.append("<" + n.getNodeName());
        int len = attrs != null ? attrs.getLength() : 0;
        for (int i = 0; i < len; i++)
        {
            Node a = attrs.item(i);
            code.append(" " + a.toString());
        }
        code.append(" />");
        return code.toString();
    }

    static private String upperHeadChar (String s)
    {
        return s.substring(0, 1).toUpperCase() + s.substring(1, s.length());
    }

    static private String fixedLen (Integer len, String str)
    {
        String blank = " ";
        int delta = len - str.length();
        if (delta > 0)
            for (int i = 0; i < delta; i++)
                str += blank;
        return str;
    }

    static private void log (Object txt)
    {
        System.out.println(txt);
    }

    static private void recursiveReadFile (List<File> all, File file, FilenameFilter filter)
    {
        if (all.size() > 1024)
            return;
        if (file.isFile())
            all.add(file);
        else if (file.isDirectory())
        {
            File[] files = file.listFiles(filter);
            for (int i = 0; i < files.length; i++)
                recursiveReadFile(all, files[i], filter);
        }
        else
        {
        }
    }

    static private final//
    HashMap<Class<?>,HashMap<String,Method>> mapClassSetters = new HashMap<Class<?>,HashMap<String,Method>>();
    static private final//
    HashMap<Class<?>,HashMap<String,Method>> mapClassGetters = new HashMap<Class<?>,HashMap<String,Method>>();

}
