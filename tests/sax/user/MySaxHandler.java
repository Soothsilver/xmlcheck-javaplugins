package user;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.SAXException;

import java.lang.Exception;

public class MySaxHandler extends DefaultHandler {
    // overrides of DefaultHandler methods
    // Do nothing.
    @Override
    public void characters(char[] ch,
                           int start,
                           int length)
            throws SAXException
    {
        throw new SAXException("My User Exception");
    }
}