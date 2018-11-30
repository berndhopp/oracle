package de.bhopp.oracle;

import java.util.function.Function;

/**
* A query-string is usually prepared before it is run against the engine. A typical preparation looks like the following
 *
 * public String apply(String query){
    return Arrays
            .stream(query.split(" "))
            .map(String::toLowerCase)
            .filter(part -> !stopWords.contains(part)
            .limit(5)
            .sort(String::size)
            .collect(joining(" "));
 * }
 * */
public interface QueryPreparator extends Function<String, String> {

}
