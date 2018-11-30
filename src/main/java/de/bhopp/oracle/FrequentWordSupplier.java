package de.bhopp.oracle;

import java.util.Collection;
import java.util.function.Supplier;
/*
* The supplier for the list of frequent words in the database (backend), this is the most tricky to implement.
*
* An implementation for postgres may look like the following. First of all, some preparations in
* SQL need to be run
*
<code>
    CREATE SCHEMA ts;
    GRANT USAGE ON SCHEMA ts TO public;
    COMMENT ON SCHEMA ts IS 'text search objects';

    CREATE TEXT SEARCH DICTIONARY ts.english_simple_dict (
    TEMPLATE = pg_catalog.simple
      , STOPWORDS = english
    );

    CREATE TEXT SEARCH CONFIGURATION ts.english_simple (COPY = simple);
    ALTER  TEXT SEARCH CONFIGURATION ts.english_simple
    ALTER MAPPING FOR asciiword WITH ts.english_simple_dict;  -- 1, 'Word, all ASCII'
</code>
*
* then, the database may be queried like this:
*
* <code>
    public Collection<FrequentWord> get(){
         Stream<Object[]> s = entityManagerProvider
                .get()
                .createNativeQuery(
                    "SELECT word, ndoc " +
                    "FROM ts_stat($$SELECT to_tsvector('ts.english_simple', statement_text) FROM statements$$) " +
                    "ORDER BY ndoc DESC"
            )
            .setMaxResults(500)
            .getResultStream();

            return s.map(arr -> new FrequentWord((String)arr[0], (int)arr[1])).collect(toList());
    }
* </code>
* */
public interface FrequentWordSupplier extends Supplier<Collection<FrequentWord>> {
}
