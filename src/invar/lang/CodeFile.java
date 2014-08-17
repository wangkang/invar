package invar.lang;

import invar.lang.lex.InvarLexer;
import invar.lang.lex.LexPattern;
import invar.lang.lex.LexToken;
import java.util.ArrayList;
import java.util.List;

final class CodeFile
{
    private final String       path;
    private final CharSequence code;
    private final CodeLine[]   lines;
    private final InvarLexer   lexer;

    public CodeFile(final CharSequence code, final String path)
    {
        this.code = code;
        this.path = path;
        this.lines = initCodeLines(code, this);
        this.lexer = new InvarLexer(code, LexPattern.FILE);
    }

    public List<LexToken> scan ()
    {
        return lexer.scan();
    }

    public int numLines ()
    {
        return lines.length;
    }

    public CodeLine getLine (int i)
    {
        return lines[i];
    }

    public String getPath ()
    {
        return path;
    }

    public CharSequence getCode ()
    {
        return code;
    }

    public String toString ()
    {
        StringBuilder s = new StringBuilder();
        for (CodeLine line : lines)
        {
            s.append(line.toString());
        }
        return s.toString();
    }

    static private CodeLine[] initCodeLines (final CharSequence code, final CodeFile file)
    {
        ArrayList<CodeLine> list = new ArrayList<CodeLine>();
        int from = 0;
        int dest = 0;
        int delta = 0;
        int len = code.length();
        while (dest < len)
        {
            char c = code.charAt(dest);
            ++dest;
            if ('\r' == c)
            {
                --delta;
            }
            else if ('\n' == c)
            {
                --delta;
                list.add(new CodeLine(list.size(), from, dest + delta, file));
                from = dest;
            }
            else
            {
                delta = 0;
            }
        }
        if (from < dest)
        {
            list.add(new CodeLine(list.size(), from, dest, file));
        }
        return list.toArray(new CodeLine[list.size()]);
    }

}
