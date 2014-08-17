package invar.lang;

import invar.lang.lex.LexToken;
import java.util.HashMap;
import java.util.List;

public final class CodeParser
{

    final HashMap<String,CodeFile> files;

    public CodeParser()
    {

        files = new HashMap<String,CodeFile>(64);
    }

    public void parse (String fileText, String path) throws Exception
    {
        if (path == null)
        {
            throw new Exception("Argument 'path' is null");
        }
        if (fileText == null)
        {
            throw new Exception("Argument 'fileText' is null");
        }
        CodeFile file = new CodeFile(fileText, path);

        List<LexToken> tokens = file.scan();

        for (LexToken token : tokens)
        {
            System.out.println("CodeParser.parse() token: ");
            System.out.println(token.toString());
        }

        files.put(path, file);
        System.out.println("\nCodeParser.parse()\n" + file.toString());

    }
}
