package tp02_buffer;

import java.nio.ByteBuffer;

import commun.DBMSException;
import commun.IBufferManager;
import commun.IDiskManager;
import commun.IPageId;

public class BufferManager implements IBufferManager {
    
    private int pageSize;
    private int bufferCount;
    private ReplacementPolicy policy;
    private IDiskManager diskManager;

    public BufferManager(){
        this.pageSize = 0;
        this.bufferCount = 0;
        this.policy = null;
        this.diskManager = null;
    }

    @Override 
    public void init(int pageSize, int bufferCount,ReplacementPolicy policy,IDiskManager diskManager) throws DBMSException{

    }

    @Override 
    public ByteBuffer getPage(IPageId pageId) throws DBMSException {

    }

    @Override 
    public void freePage(IPageId pageId, boolean valDirty) throws DBMSException {

    }

    @Override 
    public void setPolicy(ReplacementPolicy policy) throws DBMSException {

    }
            
    @Override 
    public void saveBuffers() throws DBMSException {

    }

    @Override
    public int getPinCount(IPageId pageId) throws DBMSException {

    }

    @Override 
    public boolean getDirtyFlag(IPageId pageId) throws DBMSException {

    }
            
    
}
