package org.obiba.opal.web.gwt.app.client.administration.taxonomies.presenter;

import org.obiba.opal.web.gwt.app.client.js.JsArrays;
import org.obiba.opal.web.model.client.opal.TermDto;

import com.google.gwt.core.client.JsArray;

public class TermArrayUtils {
  private TermArrayUtils() {}

  public static TermDto findTerm(JsArray<TermDto> terms, String termName) {
    if(termName == null) return null;

    JsArray<TermDto> termsArray = JsArrays.toSafeArray(terms);

    // find in child
    for(int i = 0; i < termsArray.length(); i++) {
      if(termsArray.get(i).getName().equals(termName)) {
        return termsArray.get(i);
      }

      // Find in child
      TermDto t = findTerm(termsArray.get(i).getTermsArray(), termName);
      if(t != null) {
        return t;
      }
    }

    return null;
  }

  public static TermDto findParent(TermDto parent, JsArray<TermDto> terms, TermDto termToFind) {
    if(termToFind == null) return null;

    JsArray<TermDto> termsArray = JsArrays.toSafeArray(terms);

    // find in child
    for(int i = 0; i < termsArray.length(); i++) {
      if(termsArray.get(i).getName().equals(termToFind.getName())) {
        return parent;
      }

      // Find in child
      TermDto t = findParent(termsArray.get(i), terms.get(i).getTermsArray(), termToFind);
      if(t != null) {
        return t;
      }
    }

    return null;
  }
}