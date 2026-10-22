package commun;

import java.nio.ByteBuffer;

/**
 * Interface representing the buffer manager API exposed to upper layers.
 */
public interface IBufferManager {

    /**
     * Supported buffer replacement policies.
     */
    enum ReplacementPolicy {
        LRU,
        MRU
    }

    /**
     * Initializes the buffer manager with the specified configurations and components.
     *
     * @param pageSize    The size of a page in bytes.
     * @param bufferCount The number of buffer frames managed by the buffer manager.
     * @param policy      The initial page replacement policy (e.g., LRU, MRU).
     * @param diskManager The underlying disk manager instance used for disk I/O.
     * @throws DBMSException
     */
    void init(
            int pageSize,
            int bufferCount,
            ReplacementPolicy policy,
            IDiskManager diskManager
    ) throws DBMSException;

    /**
     * Responds to a page request from upper layers.
     * Retrieves the buffer containing the requested page.
     *
     * @param pageId The identifier of the requested page.
     * @return The buffer containing the page data.
     * @throws DBMSException
     */
    ByteBuffer getPage(IPageId pageId) throws DBMSException;

    /**
     * Releases a previously requested page.
     *
     * @param pageId   The identifier of the page to free.
     * @param valDirty Whether the page content has been modified.
     * @throws DBMSException
     */
    void freePage(IPageId pageId, boolean valDirty) throws DBMSException;

    /**
     * Updates the current replacement policy.
     * Overrides the policy previously configured.
     *
     * @param policy The new replacement policy to use.
     * @throws DBMSException
     */
    void setPolicy(ReplacementPolicy policy) throws DBMSException;

    /**
     * Saves all modified pages (dirty == true/1) to disk via the DiskManager,
     *
     * @throws DBMSException
     */
    void saveBuffers() throws DBMSException;

    /**
     * Retrieves the current pin count of the specified page in the buffer pool.
     *
     * @param pageId The identifier of the page.
     * @return The pin count of the page if the page is in the buffer pool
     * @throws DBMSException
     */
    int getPinCount(IPageId pageId) throws DBMSException;

    /**
     * Retrieves the dirty status of the specified page in the buffer pool.
     *
     * @param pageId The identifier of the page.
     * @return Rhe dirty flag of the page if the page is in the buffer pool 
     * @throws DBMSException
     */
    boolean getDirtyFlag(IPageId pageId) throws DBMSException;
}
