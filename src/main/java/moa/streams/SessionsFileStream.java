package moa.streams;

import java.io.*;
import moa.core.InputStreamProgressMonitor;
import com.yahoo.labs.samoa.instances.InstancesHeader;
import moa.core.ObjectRepository;
import moa.options.AbstractOptionHandler;
import com.github.javacliparser.FileOption;
import moa.core.Example;
import moa.core.InstanceExample;
import moa.tasks.TaskMonitor;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.SparseInstance;

/**
 * Reads a stream from a file 
 * Format csv, delimiter is comma ','
 * Every line represents one session
 * First number of line is user id, 
 * next numbers represents visits of individual pages, or categories in session
 * @author Tomas Chovanak
 */
public class SessionsFileStream extends AbstractOptionHandler 
    implements InstanceStream {

    @Override
    public String getPurposeString() {
        return "A stream read from a file in format csv. Each row is one session. "
                + "First column is UID. Next columns are pages visited in session.";
    }
    
    private static final long serialVersionUID = 1L;

    public FileOption sessionFileOption = new FileOption("sessionFile", 'f',
            "Session file to load.", null, "data", false);

    protected BufferedReader fileReader;

    protected boolean hitEndOfFile;
    
    private int counter = 0;

    protected Instance lastInstanceRead;

    protected int numInstancesRead;

    protected InputStreamProgressMonitor fileProgressMonitor;
    
    public SessionsFileStream(){
    }
    
    public SessionsFileStream(String sessionFileName){
        this.sessionFileOption.setValue(sessionFileName);
        restart();
    }
    
    @Override
    protected void prepareForUseImpl(TaskMonitor tm, ObjectRepository or) {
        restart();
    }

    public void getDescription(StringBuilder sb, int i) {
    }

    public InstancesHeader getHeader() {
        return null;
    }

    public long estimatedRemainingInstances() {
        double progressFraction = this.fileProgressMonitor.getProgressFraction();
        if ((progressFraction > 0.0) && (this.numInstancesRead > 0)) {
            return (long) ((this.numInstancesRead / progressFraction) - this.numInstancesRead);
        }
        return -1;
    }

    public boolean hasMoreInstances() {
        return !this.hitEndOfFile;
    }

    @Override
    public Example nextInstance() {
        Instance prevInstance = this.lastInstanceRead;
        this.hitEndOfFile = !readNextInstanceFromFile();
        return new InstanceExample(prevInstance);
    }

    public boolean isRestartable() {
        return true;
    }

    public void restart() {
        try {
            if (this.fileReader != null) {
                this.fileReader.close();
            }
            InputStream fileStream = new FileInputStream(this.sessionFileOption.getFile());
            this.fileProgressMonitor = new InputStreamProgressMonitor(
                    fileStream);
            this.fileReader = new BufferedReader(new InputStreamReader(
                    this.fileProgressMonitor));
            this.numInstancesRead = 0;
            this.lastInstanceRead = null;
            this.hitEndOfFile = !readNextInstanceFromFile();
        } catch (IOException ioe) {
            throw new RuntimeException("sessionFileStream restart failed.", ioe);
        }
    }

    protected boolean readNextInstanceFromFile() {
        try {
            String line = this.fileReader.readLine();
            if(line != null){
                String[] lineSplitted = line.split(",");
                int nItems = lineSplitted.length + 1;
                double[] attValues = new double[nItems];
                int[] indices = new int[nItems];
                // to each session instance we add one item representing group user belongs to 
                attValues[0] = -1; // -1 means no group - it will be later on added to instance as result of clustering process
                indices[0] = 0;
                for(int idx = 1; idx < nItems; idx++){
                    attValues[idx] = Integer.parseInt(lineSplitted[idx-1]);
                    indices[idx] = idx;
                }
                this.lastInstanceRead = new SparseInstance(1.0,attValues,indices,nItems);
                return true;
            }
            if (this.fileReader != null) {
                this.fileReader.close();
                this.fileReader = null;
            }
            return false;
        } catch (IOException ex) {
            throw new RuntimeException(
                    "SessionFileStream failed to read instance from stream.", ex);
        }
    }
    
}
