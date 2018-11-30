package de.bhopp.oracle;


import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.vaadin.flow.data.provider.AbstractDataProvider;
import com.vaadin.flow.data.provider.Query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.NavigableSet;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;

import static java.util.Collections.synchronizedNavigableSet;
import static java.util.Comparator.comparing;
import static java.util.Comparator.comparingInt;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

/**
* A simple-to-use search-oracle based on the frequency of words in the backend
* */
public class SearchOracle extends AbstractDataProvider<String, String> {
    private final NavigableSet<FrequentWord> backingSet = synchronizedNavigableSet(new TreeSet<>(comparing(FrequentWord::getWord)));
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final FrequentWordSupplier frequentWordSupplier;
    private final QueryPreparator queryPreparator;
    private final int suggestionLimit;

    private Cache<String, List<String>> cache;

    public SearchOracle(FrequentWordSupplier frequentWordSupplier, QueryPreparator queryPreparator, int updatePeriodSeconds, int suggestionLimit, int cacheSeconds) {
        requireNonNull(frequentWordSupplier);
        requireNonNull(queryPreparator);
        if (updatePeriodSeconds <= 0) throw new IllegalArgumentException();
        if (suggestionLimit <= 0) throw new IllegalArgumentException();
        if (cacheSeconds <= 0) throw new IllegalArgumentException();

        cache = Caffeine
                .newBuilder()
                .expireAfterAccess(cacheSeconds, TimeUnit.SECONDS)
                .build();

        this.frequentWordSupplier = frequentWordSupplier;
        this.queryPreparator = queryPreparator;
        this.suggestionLimit = suggestionLimit;

        new Timer().schedule(new TimerTask() {
               @Override
               public void run() {
                    loadBackingSet();
               }
            },
            updatePeriodSeconds * 1000
        );

        loadBackingSet();
    }

    private void loadBackingSet(){
        final Collection<FrequentWord> mostFrequentWords = frequentWordSupplier.get();

        try (CloseableLock ignored = CloseableLock.of(lock.writeLock())) {
            cache.invalidateAll();
            backingSet.clear();
            backingSet.addAll(mostFrequentWords);
        }
    }

    private List<String> suggest(String query) {
        requireNonNull(query);

        final String preparedQuery = queryPreparator.apply(query);

        if (preparedQuery.isEmpty()) {
            return Collections.emptyList();
        }

        return cache.get(preparedQuery, this::loadInternal);
    }

    private List<String> loadInternal(String query) {
        final List<FrequentWord> matches = new ArrayList<>();

        FrequentWord searchEntry = new FrequentWord(query, 0);

        try (CloseableLock ignored = CloseableLock.of(lock.readLock())) {
            for (FrequentWord ceiling = backingSet.ceiling(searchEntry); ceiling != null && ceiling.getWord().startsWith(query); ceiling = backingSet.higher(ceiling)) {
                matches.add(ceiling);
            }
        }

        return matches
                .stream()
                .sorted(comparingInt(FrequentWord::getCount))
                .map(FrequentWord::getWord)
                .limit(suggestionLimit)
                .collect(toList());
    }

    @Override
    public Stream<String> fetch(Query<String, String> query) {
        return query
                .getFilter()
                .map(this::suggest)
                .map(Collection::stream)
                .orElseGet(Stream::empty);
    }

    @Override
    public boolean isInMemory() {
        return true;
    }

    @Override
    public int size(Query<String, String> query) {
        return query
                .getFilter()
                .map(this::suggest)
                .map(Collection::size)
                .orElse(0);
    }
}