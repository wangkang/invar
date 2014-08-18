package invar.lang.lex;

import java.util.List;

final class LexNode
{
    private final LexType      type;
    private final CharSequence code;
    private final int          from;
    private final int          dest;

    List<LexNode>              children;

    public LexNode(LexType type, CharSequence code, int from, int dest)
    {
        this.type = type;
        this.code = code;
        this.from = from;
        this.dest = dest;
    }

    public LexType getType ()
    {
        return type;
    }

    public void match (List<LexPattern> branches)
    {
        boolean matched = false;
        int current = this.from;
        for (LexPattern b : branches)
        {
            int p = b.match(code, current, this.dest);
            if (p < 0)
            {
                continue;
            }
            if (p >= current && p <= this.dest)
            {
                current = p;
                matched = true;
                int delta = b.getSliceDelta();
                if (delta >= 0)
                {
                    trySpawnChild(current, p - delta, b.getTokenType());
                }
            }
        }
        if (current < this.dest && matched)
        {
            match(branches);
        }
    }

    private void trySpawnChild (int pFrom, int pDest, LexType type)
    {
        if (pDest <= pFrom)
        {
            return;
        }
        LexNode n = new LexNode(type, this.code, pFrom, pDest);
        children.add(n);

    }

}
