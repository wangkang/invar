package invar.lang;

import java.util.HashMap;

public final class CodeParser
{
    final SyntaxNode               syntax;
    final HashMap<String,CodeFile> files;

    public CodeParser()
    {
        syntax = SyntaxNode.build();
        files = new HashMap<String,CodeFile>(64);
    }

    public void parse (String fileText, String path)
    {
        if (path == null)
        {
            throw new ArithmeticException("Argument 'path' is null");
        }
        if (fileText == null)
        {
            throw new ArithmeticException("Argument 'fileText' is null");
        }
        CodeFile file = new CodeFile(fileText);
        files.put(path, file);

        syntax.reset();
        int len = file.numLines();
        SyntaxNode node = syntax;
        for (int i = 0; i < len; i++)
        {
            CodeLine line = file.getLine(i);
            node = node.parse(line, 0);
        }
        
        System.out.println("\nCodeParser.parse()\n" + file.toString());
    }
}
