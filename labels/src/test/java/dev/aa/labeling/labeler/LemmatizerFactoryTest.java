package dev.aa.labeling.labeler;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LemmatizerFactoryTest {

    @Test
    void testCreateRussianLemmatizer_lowercase() {
        Lemmatizer lemmatizer = LemmatizerFactory.createLemmatizer("ru");
        assertTrue(lemmatizer instanceof RussianLemmatizer);
    }

    @Test
    void testCreateRussianLemmatizer_uppercase() {
        Lemmatizer lemmatizer = LemmatizerFactory.createLemmatizer("RU");
        assertTrue(lemmatizer instanceof RussianLemmatizer);
    }

    @Test
    void testCreateRussianLemmatizer_locale() {
        Lemmatizer lemmatizer = LemmatizerFactory.createLemmatizer("ru-RU");
        assertTrue(lemmatizer instanceof RussianLemmatizer);
    }

    @Test
    void testCreateEnglishLemmatizer_lowercase() {
        Lemmatizer lemmatizer = LemmatizerFactory.createLemmatizer("en");
        assertTrue(lemmatizer instanceof EnglishLemmatizer);
    }

    @Test
    void testCreateEnglishLemmatizer_uppercase() {
        Lemmatizer lemmatizer = LemmatizerFactory.createLemmatizer("EN");
        assertTrue(lemmatizer instanceof EnglishLemmatizer);
    }

    @Test
    void testCreateEnglishLemmatizer_locale() {
        Lemmatizer lemmatizer = LemmatizerFactory.createLemmatizer("en-US");
        assertTrue(lemmatizer instanceof EnglishLemmatizer);
    }

    @Test
    void testCreateHebrewLemmatizer_lowercase() {
        Lemmatizer lemmatizer = LemmatizerFactory.createLemmatizer("he");
        assertTrue(lemmatizer instanceof HebrewLemmatizer);
    }

    @Test
    void testCreateHebrewLemmatizer_uppercase() {
        Lemmatizer lemmatizer = LemmatizerFactory.createLemmatizer("HE");
        assertTrue(lemmatizer instanceof HebrewLemmatizer);
    }

    @Test
    void testCreateHebrewLemmatizer_locale() {
        Lemmatizer lemmatizer = LemmatizerFactory.createLemmatizer("he-IL");
        assertTrue(lemmatizer instanceof HebrewLemmatizer);
    }

    @Test
    void testCreateNoOpLemmatizer_null() {
        Lemmatizer lemmatizer = LemmatizerFactory.createLemmatizer(null);
        assertTrue(lemmatizer instanceof NoOpLemmatizer);
    }

    @Test
    void testCreateNoOpLemmatizer_unknown() {
        Lemmatizer lemmatizer = LemmatizerFactory.createLemmatizer("xx");
        assertTrue(lemmatizer instanceof NoOpLemmatizer);
    }

    @Test
    void testCreateNoOpLemmatizer_empty() {
        Lemmatizer lemmatizer = LemmatizerFactory.createLemmatizer("");
        assertTrue(lemmatizer instanceof NoOpLemmatizer);
    }
}
