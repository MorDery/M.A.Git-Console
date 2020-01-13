public class Branch {
    private String m_Name;
    private String m_PointedCommitId;
    private boolean m_IsHead = false;
    private String m_TrackingAfter;
    private boolean m_IsRemote;
    private boolean m_Tracking;


    public Branch(String i_Name, String i_PointedCommitId, String i_TrackingAfter, boolean i_IsRemote, boolean i_Tracking){
        m_Name = i_Name;
        m_PointedCommitId = i_PointedCommitId;
        m_TrackingAfter = i_TrackingAfter;
        m_IsRemote = i_IsRemote;
        m_Tracking = i_Tracking;
    }
    public Branch(String i_Name, String i_PointedCommitId, boolean i_IsHead){
        m_Name = i_Name;
        m_PointedCommitId = i_PointedCommitId;
        m_IsHead = i_IsHead;
    }

    public Branch(String i_Name, String i_PointedCommitId){
        m_Name = i_Name;
        m_PointedCommitId = i_PointedCommitId;
    }

    public String getM_Name() {
        return m_Name;
    }

    public void setM_PointedCommitId(String m_PointedCommitId) {
        this.m_PointedCommitId = m_PointedCommitId;
    }

    public String getM_PointedCommitId() {
        return m_PointedCommitId;
    }

    public boolean isM_IsHead() {
        return m_IsHead;
    }

    public String getM_TrackingAfter() {
        return m_TrackingAfter;
    }

    public boolean getM_IsRemote() {
        return m_IsRemote;
    }

    public boolean getM_Tracking() {
        return m_Tracking;
    }

    public void setM_IsHead(boolean m_IsHead) {
        this.m_IsHead = m_IsHead;
    }
}
