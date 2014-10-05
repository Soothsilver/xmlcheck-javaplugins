package name.hon2a.asmp.domsax;

import java.io.IOException;
import java.io.OutputStream;

class NullOutputStream extends OutputStream
{
    @Override
    public void write(int b) throws IOException {
        // Do nothing.
    }
}
