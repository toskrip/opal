package org.obiba.opal.elmo.concepts;

import javax.xml.namespace.QName;

import org.obiba.opal.sesame.support.SesameUtil;
import org.openrdf.elmo.annotations.rdf;
import org.openrdf.model.URI;

@rdf(Opal.NS + "MissingCategory")
public interface MissingCategory extends Category {

  public static final QName QNAME = new QName(Opal.NS, "MissingCategory");

  public static final URI URI = SesameUtil.toUri(QNAME);

}
