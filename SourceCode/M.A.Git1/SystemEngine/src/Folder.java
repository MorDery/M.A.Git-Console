import java.text.SimpleDateFormat;
import java.util.*;

public class Folder {

    private final Map<String,Item> m_Items = new LinkedHashMap<>();
    private boolean m_IsRoot = false;

    public Folder(boolean i_IsRoot) {
        m_IsRoot = i_IsRoot;
    }

    @Override
    public String toString() {
        StringBuilder stringToCreate = new StringBuilder();

        for (Map.Entry<String,Item> curr : m_Items.entrySet()){
            stringToCreate.append(curr.getValue().toStringForSha1());
            stringToCreate.append(System.lineSeparator());
        }
        int last = stringToCreate.lastIndexOf("\r\n");
        if (last >= 0) {
            stringToCreate.delete(last, stringToCreate.length());
        }

        return stringToCreate.toString();
    }

    public Map<String, Item> getM_Items() {
        return m_Items;
    }
    public Map<String, Item> getM_ItemsClone() {
        return deepClone(m_Items);
    }
    public static <T> T deepClone(final T input) {
        if (input == null) return null;

        if (input instanceof Map<?, ?>) {
            return (T) deepCloneMap((Map<?, ?>) input);
        }
        return input;
    }
    private static <K, V> Map<K, V> deepCloneMap(final Map<K, V> map) {
        Map<K, V> clone;
        if (map instanceof LinkedHashMap<?, ?>) {
            clone = new LinkedHashMap<>();
        } else if (map instanceof TreeMap<?, ?>) {
            clone = new TreeMap<>();
        } else {
            clone = new HashMap<>();
        }

        for (Map.Entry<K, V> entry : map.entrySet()) {
            clone.put(deepClone(entry.getKey()), deepClone(entry.getValue()));
        }

        return clone;
    }



    public boolean isM_IsRoot() {
        return m_IsRoot;
    }

    public void setM_IsRoot(boolean m_IsRoot) {
        this.m_IsRoot = m_IsRoot;
    }

    public static class Item{
        private String m_Name;
        private String m_Id;
        private eItemType m_Type;
        private String m_LastUpdater;
        private String m_LastUpdateDate;
        private SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy-hh:mm:ss:sss");

        public enum eItemType{
            BLOB, FOLDER
        }
        public Item(String i_Name, String i_Id, eItemType i_Type, String i_LastUpdater){
            m_Name = i_Name;
            m_Id = i_Id;
            m_Type = i_Type;
            m_LastUpdater = i_LastUpdater;
            m_LastUpdateDate = formatter.format(new Date());
        }

        public Item(String i_Name, String i_Id, eItemType i_Type, String i_LastUpdater,String i_Date){
            m_Name = i_Name;
            m_Id = i_Id;
            m_Type = i_Type;
            m_LastUpdater = i_LastUpdater;
            m_LastUpdateDate = i_Date;
        }

        public String toStringForSha1() {
            return String.format("%s;%s;%s", m_Name, m_Id, m_Type.toString().toLowerCase());
        }
        public String getM_Name() {
            return m_Name;
        }

        public void setM_Name(String m_Name) {
            this.m_Name = m_Name;
        }

        public String getM_Id() {
            return m_Id;
        }

        public void setM_Id(String m_Id) {
            this.m_Id = m_Id;
        }

        public eItemType getM_Type() {
            return m_Type;
        }

        public void setM_Type(eItemType m_Type) {
            this.m_Type = m_Type;
        }

        public String getLastUpdater() {
            return m_LastUpdater;
        }

        public void setLastUpdater(String lastUpdater) {
            m_LastUpdater = lastUpdater;
        }

        public String getLastUpdateDate() {
            return m_LastUpdateDate;
        }

        public void setLastUpdateDate(String lastUpdateDate) {
            m_LastUpdateDate = lastUpdateDate;
        }

    }

}
