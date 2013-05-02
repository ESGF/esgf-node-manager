package esg.node.filters;

public interface ByteCountListener {
    public void setRecordID(int id);
    public void setStartTime(long startTime);
    public void setDataSizeBytes(long dataSizeBytes);

    //callback method
    public void setByteCount(long numBytes);
}
