package de.bhopp.oracle;

/**
 * A word that appears frequently in the database. A list stopword-filter is suggested,
 * to exclude words like 'and' that are of no interest when searching.
 */
public class FrequentWord{
    private final String word;
    private final int count;

    public FrequentWord(String word, int count) {
        this.word = word;
        this.count = count;
    }

    public String getWord() {
        return word;
    }

    public int getCount() {
        return count;
    }

    @Override
    public boolean equals(Object obj) {
        return ((FrequentWord)obj).getWord().equals(word);
    }
}