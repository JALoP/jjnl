package com.tresys.jalop.jnl.impl.http;

import com.tresys.jalop.jnl.Mode;

public interface JNLTestInterface {
    /**
     * @param latestLogNONCE the latestLogNONCE to set
     */
    public void setLatestLogNONCE(final long latestLogNONCE);

    /**
     * @param latestAuditNONCE the latestAuditNONCE to set
     */
    public void setLatestAuditNONCE(final long latestAuditNONCE);

    /**
     * @param latestJournalNONCE the latestJournalNONCE to set
     */
    public void setLatestJournalNONCE(final long latestJournalNONCE);

    public Mode getMode();

    /**
     * @return the latestLogNONCE
     */
    public long getLatestLogNONCE();

    /**
     * @return the latestAuditNONCE
     */
    public long getLatestAuditNONCE();

    /**
     * @return the latestJournalNONCE
     */
    public long getLatestJournalNONCE();
}
