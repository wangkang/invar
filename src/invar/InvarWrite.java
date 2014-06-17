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

    abstract protected String codeOneFile (String packName, List<TypeEnum> enums, List<TypeStruct> structs);

    abstract protected void codeRuntime (String suffix);

    final private InvarContext              context;
    final private File                      dirRoot;
    final private HashMap<String,String>    exports;
    final private HashMap<String,InvarType> typeForShort;

    Boolean                                 flattenCodeDir;
    Boolean                                 onePackOneFile;

    public InvarWrite(InvarContext context, String dirRootPath)
    {
        File file = new File(dirRootPath);
        if (file.exists())
            deleteDirs(dirRootPath);
        this.dirRoot = file;
        this.context = context;
        this.exports = new HashMap<String,String>();
        this.typeForShort = new HashMap<String,InvarType>();
    }

    final public void write (String suffix) throws Throwable
    {
        if (getContext() == null)
            return;
        if (dirRoot == null)
            return;
        Boolean bool = beforeWrite(getContext());
        typeForShortReset(context);
        if (bool)
        {
            if (flattenCodeDir || onePackOneFile)
                makeFlattenDirs();
            else
                makePackageDirs();
            startWritting(suffix);
        }
    }

    private void startWritting (String suffix) throws Exception
    {
        HashMap<File,String> files = new LinkedHashMap<File,String>();
        Iterator<String> i = getContext().getPackNames();
        while (i.hasNext())
        {
            InvarPackage pack = getContext().getPack(i.next());
            Iterator<String> iTypeName = pack.getTypeNames();
            List<TypeEnum> enums = new LinkedList<TypeEnum>();
            List<TypeStruct> structs = new LinkedList<TypeStruct>();
            File codeDir = pack.getCodeDir();
            if (codeDir == null)
                continue;
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
                        structs.add(t.getClient());
                    if (t.hasServer())
                        structs.add(t.getServer());
                }
                else
                {
                    // do nothing
                }
                if (onePackOneFile == false)
                {
                    File codeFile = new File(codeDir, typeName + suffix);
                    files.put(codeFile, codeOneFile(pack.getName(), enums, structs));
                    enums.clear();
                    structs.clear();
                }
            } //while (iTypeName.hasNext())

            if (onePackOneFile == true)
            {
                File codeFile = new File(codeDir, pack.getName() + suffix);
                files.put(codeFile, codeOneFile(pack.getName(), enums, structs));
                enums.clear();
                structs.clear();
            }
        }
        codeRuntime(suffix);
        writeFiles(files);
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

    final protected void exportFile (String resPath, String fileDir, String fileName)
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
                s.append(TypeID.GHOST == type.getId() ? " # " : "   ");
                s.append(fixedLen(" ", 32, pack.getName() + "." + typeName));
                if (type.getRedirect() != null)
                {
                    InvarType typeR = type.getRedirect();
                    s.append(" --->  ");
                    String namePack = typeR.getPack().getName();
                    if (!namePack.equals(""))
                        s.append(namePack + ".");
                    s.append(typeR.getName());
                    s.append(typeR.getGeneric());
                }
                s.append("\n");
            }
            s.append(fixedLen("-", 80));
            s.append("\n");
        }
        return s;
    }

    static protected String upperHeadChar (String s)
    {
        return s.substring(0, 1).toUpperCase() + s.substring(1, s.length());
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

    final static private String GENERIC_LEFT  = "<";
    final static private String GENERIC_RIGHT = ">";

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
        int iEnd = fullName.lastIndexOf(".");
        if (iEnd < 0)
            return ctx.findBuildInType(fullName);
        String packName = fullName.substring(0, iEnd);
        String typeName = fullName.substring(iEnd + 1);
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
                typeForShort.put(type.fullName(), type);
                typeForShort.put(type.getName(), type);
            }
        }
    }

    final protected InvarType getTypeByShort (String key)
    {
        return typeForShort.get(key);
    }

    final protected void setFlattenDir (Boolean flatten)
    {
        flattenCodeDir = flatten;
    }

    final protected void setOnePackOneFile (Boolean p1f1)
    {
        onePackOneFile = p1f1;
    }

    final protected boolean getOnePackOneFile ()
    {
        return onePackOneFile;
    }
}
