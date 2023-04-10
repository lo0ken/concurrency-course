package course.concurrency.exams.refactoring;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;


public class MountTableRefresherService {

    private static final String ADMIN_ADDRESS = "AdminAddress";

    private Others.RouterStore routerStore = new Others.RouterStore();
    private long cacheUpdateTimeout;
    private Others.MountTableManager manager;

    public MountTableRefresherService(Others.MountTableManager manager) {
        this.manager = manager;
    }

    /**
     * All router admin clients cached. So no need to create the client again and
     * again. Router admin address(host:port) is used as key to cache RouterClient
     * objects.
     */
    private Others.LoadingCache<String, Others.RouterClient> routerClientsCache;

    /**
     * Removes expired RouterClient from routerClientsCache.
     */
    private ScheduledExecutorService clientCacheCleanerScheduler;

    public void serviceInit()  {
        long routerClientMaxLiveTime = 15L;
        this.cacheUpdateTimeout = 10L;
        routerClientsCache = new Others.LoadingCache<String, Others.RouterClient>();
        routerStore.getCachedRecords().stream().map(Others.RouterState::getAdminAddress)
                .forEach(addr -> routerClientsCache.add(addr, new Others.RouterClient()));

        initClientCacheCleaner(routerClientMaxLiveTime);
    }

    public void serviceStop() {
        clientCacheCleanerScheduler.shutdown();
        // remove and close all admin clients
        routerClientsCache.cleanUp();
    }

    private void initClientCacheCleaner(long routerClientMaxLiveTime) {
        ThreadFactory tf = new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread();
                t.setName("MountTableRefresh_ClientsCacheCleaner");
                t.setDaemon(true);
                return t;
            }
        };

        clientCacheCleanerScheduler =
                Executors.newSingleThreadScheduledExecutor(tf);
        /*
         * When cleanUp() method is called, expired RouterClient will be removed and
         * closed.
         */
        clientCacheCleanerScheduler.scheduleWithFixedDelay(
                () -> routerClientsCache.cleanUp(), routerClientMaxLiveTime,
                routerClientMaxLiveTime, TimeUnit.MILLISECONDS);
    }

    /**
     * Refresh mount table cache of this router as well as all other routers.
     */
    public void refresh()  {

        List<Others.RouterState> cachedRecords = routerStore.getCachedRecords();
        List<MountTableRefresherThread> refreshThreads = new ArrayList<>();
        for (Others.RouterState routerState : cachedRecords) {
            String adminAddress = routerState.getAdminAddress();
            if (adminAddress == null || adminAddress.length() == 0) {
                // this router has not enabled router admin.
                continue;
            }
            if (isLocalAdmin(adminAddress)) {
                /*
                 * Local router's cache update does not require RPC call, so no need for
                 * RouterClient
                 */
                refreshThreads.add(getLocalRefresher(adminAddress));
            } else {
                refreshThreads.add(new MountTableRefresherThread(
                    //new Others.MountTableManager(adminAddress), adminAddress));
                    manager, adminAddress));
            }
        }
        if (!refreshThreads.isEmpty()) {
            invokeRefresh(refreshThreads);
        }
    }

    protected MountTableRefresherThread getLocalRefresher(String adminAddress) {
        return new MountTableRefresherThread(manager, adminAddress);
    }

    private void removeFromCache(String adminAddress) {
        routerClientsCache.invalidate(adminAddress);
    }

    private void invokeRefresh(List<MountTableRefresherThread> refreshThreads) {
        List<CompletableFuture<Boolean>> futures = refreshThreads.stream()
                .map(thread -> CompletableFuture.supplyAsync(() ->
                                manager.refresh()
                        ).orTimeout(cacheUpdateTimeout, TimeUnit.MILLISECONDS)
                        .exceptionally(t -> {
                            if (t instanceof TimeoutException) {
                                log("Not all router admins updated their cache");
                            }
                            if (t instanceof InterruptedException) {
                                log("Mount table cache refresher was interrupted.");
                            }
                            return false;
                        }))
                .collect(Collectors.toList());

        logResult(futures);
    }

    private boolean isLocalAdmin(String adminAddress) {
        return adminAddress.contains("local");
    }

    private void logResult(List<CompletableFuture<Boolean>> futures) {
        long successCount = futures.stream().filter(CompletableFuture::join).count();
        long failureCount = futures.stream()
                .filter(f -> !f.join())
                .peek(f -> removeFromCache(ADMIN_ADDRESS))
                .count();
        log(String.format(
                "Mount table entries cache refresh successCount=%d,failureCount=%d",
                successCount, failureCount));
    }

    public void log(String message) {
        System.out.println(message);
    }

    public void setCacheUpdateTimeout(long cacheUpdateTimeout) {
        this.cacheUpdateTimeout = cacheUpdateTimeout;
    }
    public void setRouterClientsCache(Others.LoadingCache cache) {
        this.routerClientsCache = cache;
    }

    public void setRouterStore(Others.RouterStore routerStore) {
        this.routerStore = routerStore;
    }
}