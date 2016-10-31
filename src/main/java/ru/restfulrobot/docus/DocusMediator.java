package ru.restfulrobot.docus;

import java.util.ArrayList;

import ru.restfulrobot.docus.DocTree.Document;

// класс хранения найденных результатов
public class DocusMediator {

    static ArrayList<Document> searchResults;

    static void resetSearchResultArray() {
        searchResults = new ArrayList<Document>();
    }

    static void appendResult(Document doc) {
        if (!searchResults.contains(doc)) {
            searchResults.add(doc);
        }
    }

    static Boolean contatinsDoc(Document doc) {
        if (searchResults != null && searchResults.size() != 0) {
            return searchResults.contains(doc);
        }
        return false;
    }

}