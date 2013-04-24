package esg.node.filters;

public interface ByteCountListener {
    public void setRecordID(int id);
    public void setStartTime(long startTime);
    public void setByteCount(long numBytes);
}
