package invar.lang.lex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class InvarLexer
{
    private final CharSequence     source;
    private final List<LexPattern> patterns;

    public InvarLexer(CharSequence source, List<LexPattern> patterns)
    {
        this.source = source;
        this.patterns = patterns;
    }

    public List<LexToken> scan ()
    {
        List<LexToken> tokens = new ArrayList<LexToken>();
        matchPatterns(tokens, this.patterns, 0);
        return Collections.unmodifiableList(tokens);
    }

    private void matchPatterns (List<LexToken> tokens, List<LexPattern> ps, int start)
    {
        int end = source.length();
        if (start >= end)
        {
            return;
        }

        for (LexPattern p : ps)
        {
            int result = p.match(source, start, end);
            if (result > start && result < end)
            {
                LexType type = p.getTokenType();
                if (!type.isSkipped())
                {
                    tokens.add(new LexToken(type, this.source, start, result));
                }

                List<LexPattern> children = p.getChildren();
                if (children != null && children.size() > 0)
                {
                    matchPatterns(tokens, children, result);
                }
            }
            else
            {
                if (p.isOptional())
                    continue;
                else
                    break;
            }

        }

    }
}
