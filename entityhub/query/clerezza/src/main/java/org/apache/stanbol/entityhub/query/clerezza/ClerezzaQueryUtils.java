package org.apache.stanbol.entityhub.query.clerezza;

import java.util.Iterator;

import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.stanbol.entityhub.model.clerezza.RdfRepresentation;
import org.apache.stanbol.entityhub.model.clerezza.RdfValueFactory;
import org.apache.stanbol.entityhub.query.sparql.SparqlQueryUtils;
import org.apache.stanbol.entityhub.servicesapi.model.rdf.RdfResourceEnum;
import org.apache.stanbol.entityhub.servicesapi.util.AdaptingIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides Clerezza specific Query utilities that are in addition to those
 * provided by {@link SparqlQueryUtils}.
 * @author Rupert Westenthaler
 *
 */
public class ClerezzaQueryUtils {
    
    private static final Logger log = LoggerFactory.getLogger(ClerezzaQueryUtils.class);

    private static final RdfValueFactory valueFavtory = RdfValueFactory.getInstance();
    /**
     * {@link UriRef} constant for {@link RdfResourceEnum#queryResult}
     * 
     * @see RdfResourceEnum.fieldQueryResult
     */
    public static final UriRef FIELD_QUERY_RESULT = new UriRef(RdfResourceEnum.queryResult.getUri());
    /**
     * {@link UriRef} constant for {@link RdfResourceEnum#QueryResultSet}
     * 
     * @see RdfResourceEnum.FieldQueryResultSet
     */
    public static final UriRef FIELD_QUERY_RESULT_SET = new UriRef(RdfResourceEnum.QueryResultSet.getUri());

    /**
     * @param query
     * @param resultGraph
     * @return
     */
    public static Iterator<RdfRepresentation> parseQueryResultsFromMGraph(final TripleCollection resultGraph) {
        Iterator<Triple> resultTripleIterator = resultGraph.filter(FIELD_QUERY_RESULT_SET,
            FIELD_QUERY_RESULT, null);
        Iterator<RdfRepresentation> resultIterator = new AdaptingIterator<Triple,RdfRepresentation>(
                resultTripleIterator, new AdaptingIterator.Adapter<Triple,RdfRepresentation>() {
                    /*
                     * Anonymous implementation of an Adapter that converts the filtered Triples of the
                     * resulting graph to RdfRepresentations
                     */
                    @Override
                    public RdfRepresentation adapt(Triple value, Class<RdfRepresentation> type) {
                        Resource object = value.getObject();
                        if (object == null) {
                            return null;
                        } else if (object instanceof UriRef) {
                            return valueFavtory.createRdfRepresentation((UriRef) object, resultGraph);
                        } else {
                            log.warn("Unable to create representation for FieldQueryResult " + object
                                     + " because this Resource is not of Type UriRef (type: "
                                     + object.getClass() + ") -> result gets ignored");
                            return null;
                        }
                    }
                }, RdfRepresentation.class);
        return resultIterator;
    }
    
}
