package org.ehcache.jcache;

import net.sf.ehcache.Ehcache;
import org.ehcache.jcache.JCache;
import org.ehcache.jcache.JCacheConfiguration;
import org.junit.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.Duration;
import javax.cache.expiry.ModifiedExpiryPolicy;
import javax.cache.processor.EntryProcessor;
import javax.cache.processor.EntryProcessorException;
import javax.cache.processor.MutableEntry;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class JCacheAndEhcacheAccessTest {

    @Test
    public void ehcacheIsBeingPickedAsCacheProvider() {
        final MutableConfiguration mutableConfiguration = new MutableConfiguration()
            .setExpiryPolicyFactory(ModifiedExpiryPolicy.factoryOf(new Duration(TimeUnit.MINUTES, 10)))
            .setStoreByValue(true);

        Cache foo = Caching.getCachingProvider().getCacheManager().createCache("foo", new JCacheConfiguration(mutableConfiguration));
        assertThat(foo, is(notNullValue()));
        assertThat(foo, is(JCache.class));
    }

    @Test
    public void namedEhcacheDotXMLReadWhenOneExists() {
        javax.cache.Cache jcache = Caching.getCachingProvider().getCacheManager().getCache("sampleCache");
        assertThat(jcache, is(notNullValue()));
        assertThat((jcache.unwrap(Ehcache.class)), is(notNullValue()));
    }

    @Test
    public void namedEhcachePropertiesUsedWhenOneExists() {
        JCache jcache = (JCache) Caching.getCachingProvider().getCacheManager().getCache("sampleCache");
        assertThat(jcache, is(notNullValue()));
        assertThat("Store by value is only true if copyOnRead and copyOnWrite are both configured in the xml config",
                jcache.getConfiguration(JCacheConfiguration.class).isStoreByValue(), is(false));
//        assertThat(jcache.getConfiguration().getExpiry(CacheConfiguration.ExpiryType.ACCESSED).getTimeUnit(),
//                is(equalTo(TimeUnit.SECONDS)));
//        assertThat(jcache.getConfiguration().getExpiry(CacheConfiguration.ExpiryType.ACCESSED).getDurationAmount(),
//                is(360L));
//
//        assertThat(jcache.getConfiguration().getExpiry(CacheConfiguration.ExpiryType.MODIFIED).getTimeUnit(),
//                is(equalTo(TimeUnit.SECONDS)));
//        assertThat(jcache.getConfiguration().getExpiry(CacheConfiguration.ExpiryType.MODIFIED).getDurationAmount(),
//                is(1000L));
//        assertThat(jcache.getConfiguration(JCacheConfiguration.class).getEhcacheConfiguration().isOverflowToDisk(), is(true));
    }

    @Test
    public void nullCacheWhenNoCacheExists() {
        JCache jcache = (JCache) Caching.getCachingProvider().getCacheManager().getCache("nonexistent-cache");
        assertThat(jcache, nullValue());
    }

    @Test
    public void testUpdatesCacheWhenSettingMutableEntryValue() {
        final CacheManager cacheManager = Caching.getCachingProvider().getCacheManager();
        Cache<String, AtomicBoolean> cache = cacheManager.createCache("testUpdatesCacheWhenSettingMutableEntryValue", new MutableConfiguration<String, AtomicBoolean>());
        try {
            cache.put("key", new AtomicBoolean());
            assertThat(cache.invoke("key", new EntryProcessor<String, AtomicBoolean, Boolean>() {
                @Override
                public Boolean process(final MutableEntry<String, AtomicBoolean> entry, final Object... arguments) throws EntryProcessorException {
                    final AtomicBoolean value = entry.getValue();
                    final boolean previous = value.getAndSet(true);
                    entry.setValue(value);
                    return previous;
                }
            }), is(false));
            assertThat(cache.get("key").get(), is(true));
        } finally {
            cacheManager.destroyCache(cache.getName());
        }
    }

}
