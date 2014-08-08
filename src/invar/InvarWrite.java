package invar;

import invar.model.InvarPackage;
import invar.model.InvarType;
import invar.model.InvarType.TypeID;
import invar.model.TypeEnum;
import invar.model.TypeProtocol;
import invar.model.TypeStruct;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

abstract public class InvarWrite
{

    abstract protected Boolean beforeWrite (InvarContext ctx);

    abstract protected String codeOneFile (String packName,
                                           String filePath,
                                           List<TypeEnum> enums,
                                           List<TypeStruct> structs);

    abstract protected void codeRuntime (String suffix);

    final static String                     empty              = "";
    final static String                     whiteSpace         = " ";
    final static String                     br                 = "\n";
    final static String                     indent             = whiteSpace + whiteSpace + whiteSpace + whiteSpace;
    final static String                     dotToken           = "\\.";
    final static String                     ruleTypeSplit      = "::";
    final static String                     rulePackSplit      = ".";
    final static private String             GENERIC_LEFT       = "<";
    final static private String             GENERIC_RIGHT      = ">";

    final private InvarContext              context;
    final private HashMap<String,String>    exports;
    final private HashMap<String,InvarType> typeForShort;
    final private String                    dirRootPath;

    private File                            dirRoot;
    private String                          suffix;

    String                                  dirPrefix          = empty;
    Boolean                                 traceAllTypes      = false;
    Boolean                                 flattenCodeDir     = false;
    Boolean                                 onePackOneFile     = false;
    Boolean                                 lowerFileName      = false;
    Boolean                                 packNameNested     = false;
    Boolean                                 useFullName        = false;
    Boolean                                 includeSelf        = false;
    Boolean                                 impExcludeConflict = false;
    Boolean                                 impExcludeSamePack = false;
    Integer                                 methodIndentNum    = 1;
    List<String>                            impExcludePacks    = null;

    public InvarWrite(InvarContext context, String dirRootPath)
    {
        this.suffix = ".x";
        this.dirRootPath = dirRootPath;
        this.context = context;
        this.exports = new HashMap<String,String>();
        this.typeForShort = new HashMap<String,InvarType>();
    }

    final public void write (String suffix) throws Throwable
    {
        write(suffix, false);
    }

    final public void write (String suffix, Boolean merge) throws Throwable
    {
        this.suffix = suffix;
        if (getContext() == null)
            return;
        if (!merge)
            context.clearDialectTypes();

        Boolean bool = beforeWrite(getContext());
        typeForShortReset(context);

        String dir = dirRootPath;
        if (dirPrefix != null && !dirPrefix.equals(empty))
        {
            dir += dirPrefix;
        }
        File file = new File(dir);
        if (file.exists())
        {
            deleteDirs(dir);
        }
        this.dirRoot = file;
        if (bool)
        {
            if (onePackOneFile)
                flattenCodeDir = true;
            if (flattenCodeDir)
                makeFlattenDirs();
            else
                makePackageDirs();

            resetCodePathes(merge);
            startWritting(suffix);
        }
    }

    private void resetCodePathes (Boolean merge)
    {
        Iterator<String> iPack = context.getPackNames();
        while (iPack.hasNext())
        {
            InvarPackage pack = context.getPack(iPack.next());
            if (context.isBuildInPack(pack))
                continue;

            String name = pack.getName();
            Iterator<String> iType = pack.getTypeNames();
            while (iType.hasNext())
            {
                String typeName = iType.next();
                InvarType type = pack.getType(typeName);
                if (onePackOneFile == false)
                {
                    name = typeName;
                    if (flattenCodeDir)
                    {
                        name = type.fullName("_");
                    }
                }
                String path = flattenCodeDir ? name : type.fullName("/");
                //path = dirPrefix + path;
                path = wrapCodePath(path);
                switch (type.getId()) {
                case ENUM:
                case STRUCT:
                    resetCodePath(type, path, name, merge);
                    break;
                case PROTOCOL:
                    resetCodePath(type, path, name, merge);
                    TypeProtocol t = (TypeProtocol)type;
                    if (t.hasClient())
                    {
                        resetCodePath(t.getClient(), path, name, merge);
                    }
                    if (t.hasServer())
                    {
                        resetCodePath(t.getServer(), path, name, merge);
                    }
                    break;
                default:
                    break;
                }
            }
        }
    }

    private void resetCodePath (InvarType type, String path, String name, Boolean merge)
    {
        type.setCodeName(name);
        if (!merge)
            type.setCodePath(path);
    }

    private void startWritting (String suffix) throws Exception
    {
        if (traceAllTypes)
            System.out.println("\n\n" + dumpTypeAll().toString());
        HashMap<File,String> files = new LinkedHashMap<File,String>();
        Iterator<String> i = getContext().getPackNames();
        while (i.hasNext())
        {
            InvarPackage pack = getContext().getPack(i.next());
            List<TypeEnum> enums = new LinkedList<TypeEnum>();
            List<TypeStruct> structs = new LinkedList<TypeStruct>();
            File codeDir = pack.getCodeDir();
            if (codeDir == null)
                continue;

            Iterator<String> iTypeName = pack.getTypeNames();
            while (iTypeName.hasNext())
            {
                String typeName = iTypeName.next();
                InvarType type = pack.getType(typeName);
                if (TypeID.ENUM == type.getId())
                {
                    TypeEnum t = (TypeEnum)type;
                    enums.add(t);
                }
                else if (TypeID.STRUCT == type.getId())
                {
                    TypeStruct t = (TypeStruct)type;
                    structs.add(t);
                }
                else if (TypeID.PROTOCOL == type.getId())
                {
                    TypeProtocol t = (TypeProtocol)type;
                    if (t.hasClient())
                    {
                        //structs.add(t.getClient());
                    }
                    if (t.hasServer())
                    {
                        //structs.add(t.getServer());
                    }
                    continue;
                }
                else
                {
                    // do nothing
                    continue;
                }
                if (onePackOneFile == false)
                {
                    String fileName = type.getCodeName() + suffix;
                    String filePath = type.getCodePath();
                    if (lowerFileName)
                    {
                        fileName = fileName.toLowerCase();
                        filePath = filePath.toLowerCase();
                    }
                    if (enums.size() > 0 || structs.size() > 0)
                    {
                        String text = codeOneFile(pack.getName(), filePath, enums, structs);
                        if (text.length() > 0)
                        {
                            File codeFile = new File(codeDir, fileName);
                            files.put(codeFile, text);
                        }
                    }
                    enums.clear();
                    structs.clear();

                }
            } // while (iTypeName.hasNext())
            if (onePackOneFile == true)
            {
                String fileName = pack.getName() + suffix;
                String filePath = wrapCodePath(dirPrefix + fileName);
                if (lowerFileName)
                {
                    fileName = fileName.toLowerCase();
                    filePath = filePath.toLowerCase();
                }
                File codeFile = new File(codeDir, fileName);
                files.put(codeFile, codeOneFile(pack.getName(), filePath, enums, structs));
                enums.clear();
                structs.clear();
            }
        } // while (i.hasNext())
        codeRuntime(suffix);
        writeFiles(files);
    }

    private String wrapCodePath (String path)
    {
        return "\"" + path + suffix + "\"";
    }

    final protected InvarContext getContext ()
    {
        return context;
    }

    final protected void log (Object txt)
    {
        System.out.println(txt);
    }

    final protected void logErr (Object txt)
    {
        System.out.println("error X---------> " + txt);
    }

    final protected void addExportFile (String packName, String fileName, String content)
    {
        String path = makeDirs(packName);
        exports.put(path + "/" + fileName, content);
    }

    final public void exportFile (String resPath, String fileDir, String fileName)
    {
        InputStream res = getClass().getResourceAsStream(resPath);
        if (res != null)
        {
            byte[] bs;
            try
            {
                bs = new byte[res.available()];
                res.read(bs);
                char[] chars = getChars(bs);
                addExportFile(fileDir, fileName, String.copyValueOf(chars));
            }
            catch (IOException e)
            {
                logErr(e.getMessage());
            }
        }
        else
        {
            logErr("Export resource does not exist: " + resPath);
        }
    }

    protected HashMap<File,String> makeProtocFile (String string)
    {
        //TODO make a protocol interface code
        HashMap<File,String> files = new HashMap<File,String>();
        return files;
    }

    private void makeFlattenDirs () throws Exception
    {
        Iterator<String> i = getContext().getPackNames();
        while (i.hasNext())
        {
            InvarPackage pack = getContext().getPack(i.next());
            if (!pack.getNeedWrite())
                continue;
            File packDir = new File(dirRoot, makeDirs(""));
            if (!packDir.exists())
            {
                throw new Exception("Dir do not exist: " + packDir.getAbsolutePath());
            }
            pack.setCodeDir(packDir);
        }
    }

    private void makePackageDirs () throws Exception
    {
        Iterator<String> i = getContext().getPackNames();
        while (i.hasNext())
        {
            InvarPackage pack = getContext().getPack(i.next());
            if (!pack.getNeedWrite())
                continue;
            File packDir = new File(dirRoot, makeDirs(pack.getName()));
            if (!packDir.exists())
            {
                throw new Exception("Dir do not exist: " + packDir.getAbsolutePath());
            }
            pack.setCodeDir(packDir);
        }
    }

    private String makeDirs (String packName)
    {
        String path = packName.replace('.', '/') + '/';
        File dir = new File(dirRoot, path);
        if (!dir.exists())
        {
            dir.mkdirs();
            File packDir = new File(dirRoot, path);
            log("mkdir -> " + packDir.getAbsolutePath());
            return path;
        }
        return path;
    }

    private void deleteDirs (String dir)
    {
        File delfolder = new File(dir);
        File oldFile[] = delfolder.listFiles();
        if (oldFile == null)
            return;
        for (int i = 0; i < oldFile.length; i++)
        {
            if (oldFile[i].isDirectory())
                deleteDirs(dir + oldFile[i].getName() + "//");
            oldFile[i].delete();
        }
        delfolder.delete();
    }

    private void writeFiles (HashMap<File,String> files) throws IOException
    {
        parseExportFiles(files);
        Iterator<File> i = files.keySet().iterator();
        while (i.hasNext())
        {
            File file = i.next();
            String content = files.get(file);
            FileWriter writer = new FileWriter(file, false);
            writer.write(content == null ? "" : content);
            writer.close();
            log("write -> " + file.getAbsolutePath());
        }
    }

    private void parseExportFiles (HashMap<File,String> files) throws IOException
    {
        Iterator<String> i = exports.keySet().iterator();
        while (i.hasNext())
        {
            String path = i.next();
            File file = new File(dirRoot, path);
            files.put(file, exports.get(path));
        }
    }

    final public StringBuilder dumpTypeAll ()
    {
        StringBuilder s = new StringBuilder();
        Iterator<String> i = getContext().getPackNames();
        while (i.hasNext())
        {
            InvarPackage pack = getContext().getPack(i.next());
            Iterator<String> iTypeName = pack.getTypeNames();
            while (iTypeName.hasNext())
            {
                String typeName = iTypeName.next();
                InvarType type = pack.getType(typeName);
                s.append(TypeID.DIALECT == type.getId() ? "  # " : "    ");//4
                s.append(fixedLen(32, pack.getName() + ruleTypeSplit + typeName));//32
                if (type.getRedirect() != null)
                {
                    InvarType typeR = type.getRedirect();
                    s.append(" --->   ");//8
                    String nameR = typeR.getName() + typeR.getGeneric();
                    String namePack = typeR.getPack().getName();
                    if (!namePack.equals(""))
                        nameR = (namePack + ruleTypeSplit) + nameR;
                    s.append(fixedLen(32, nameR));
                    s.append(fixedLen(32, type.getCodePath()));
                    s.append(whiteSpace + type.getInitValue());
                }
                s.append("\n");
            }
            for (int j = 0; j < 6; j++)
            {
                s.append(fixedLen("-", 19));
                s.append("+");
            }
            s.append("\n");
        }
        return s;
    }

    static protected String fixedLenBackward (String blank, Integer len, String str)
    {
        int delta = len - str.length();
        if (delta > 0)
            for (int i = 0; i < delta; i++)
                str = blank + str;
        return str;
    }

    static protected String fixedLen (String blank, Integer len, String str)
    {
        int delta = len - str.length();
        if (delta > 0)
            for (int i = 0; i < delta; i++)
                str += blank;
        return str;
    }

    static protected String fixedLen (Integer len, String str)
    {
        return fixedLen(" ", len, str);
    }

    static protected String fixedLen (String blank, Integer len)
    {
        return fixedLen(blank, len, "");
    }

    static protected char[] getChars (byte[] bytes)
    {
        Charset cs = Charset.forName("UTF-8");
        ByteBuffer bb = ByteBuffer.allocate(bytes.length);
        bb.put(bytes);
        bb.flip();
        CharBuffer cb = cs.decode(bb);
        return cb.array();
    }

    static protected byte[] getBytes (char[] chars)
    {
        Charset cs = Charset.forName("UTF-8");
        CharBuffer cb = CharBuffer.allocate(chars.length);
        cb.put(chars);
        cb.flip();
        ByteBuffer bb = cs.encode(cb);
        return bb.array();
    }

    static protected void checkKeywords (String s, String[] ks) throws Exception
    {
        if (Arrays.binarySearch(ks, s) >= 0)
        {
            throw new Exception(s + " is a reserved word.");
        }
    }

    final protected String ruleLeft (String rule)
    {
        String name = rule;
        if (rule.indexOf(GENERIC_LEFT) >= 0)
        {
            name = rule.substring(0, rule.indexOf(GENERIC_LEFT));
        }
        return name;
    }

    final protected String ruleRight (String rule)
    {
        int iBegin = rule.indexOf(GENERIC_LEFT) + 1;
        int iEnd = rule.lastIndexOf(GENERIC_RIGHT);
        if (iBegin > 0 && iEnd > iBegin)
        {
            return rule.substring(iBegin, iEnd);
        }
        return null;
    }

    final protected InvarType findType (InvarContext ctx, String fullName)
    {
        int iEnd = fullName.lastIndexOf(ruleTypeSplit);
        if (iEnd < 0)
            return ctx.findBuildInType(fullName);
        String packName = fullName.substring(0, iEnd);
        String typeName = fullName.substring(iEnd + ruleTypeSplit.length());
        InvarPackage pack = ctx.getPack(packName);
        if (pack == null)
            return null;
        return pack.getType(typeName);
    }

    final protected void packNameReset (InvarContext context, Boolean capitalize)
    {
        typeForShort.clear();
        Iterator<String> i = context.getPackNames();
        while (i.hasNext())
        {
            InvarPackage pack = context.getPack(i.next());
            if (pack.getNeedWrite())
            {
                pack.capitalizeNameHead(capitalize);
            }
        }
    }

    private void typeForShortReset (InvarContext context)
    {
        typeForShort.clear();
        Iterator<String> i = context.getPackNames();
        while (i.hasNext())
        {
            InvarPackage pack = context.getPack(i.next());
            Iterator<String> iTypeName = pack.getTypeNames();
            while (iTypeName.hasNext())
            {
                String typeName = iTypeName.next();
                InvarType type = pack.getType(typeName);
                typeForShort.put(type.getName(), type);
                typeForShort.put(type.fullName(ruleTypeSplit), type);
            }
        }
    }

    final protected InvarType getTypeByShort (String key)
    {
        key = key.trim();
        return typeForShort.get(key);
    }

}
