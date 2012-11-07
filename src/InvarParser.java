import invar.InvarAlias;
import invar.InvarParseXml;
import invar.config.ConfigData;
import java.io.InputStream;
import java.util.HashMap;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * @author wkang
 */
final public class InvarParser
{

    static public void main(String[] args) throws Throwable
    {
        HashMap<String,Class<?>> alias = InvarAlias.mapAliasType();

        parseFile("example/data.xml", new ConfigData(), alias);
    }

    private static void parseFile(String path, Object o, HashMap<String,Class<?>> alias) throws Throwable
    {
        InputStream input = InvarParser.class.getResourceAsStream(path);
        Document doc = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder().parse(input);
        if (!doc.hasChildNodes())
            return;
        Node nRoot = doc.getFirstChild();
        //new InvarParseXmlData(alias).parse2(o, nRoot.getChildNodes());
        new InvarParseXml(alias).parse(o, nRoot, "");
    }
}