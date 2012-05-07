package org.jboss.seam.examples.booking.cdi;

import javax.enterprise.inject.Produces;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;

/**
 * @author Emmanuel Bernard
 */
public class FTEMProvider {
        @PersistenceContext
        EntityManager em;

        @Produces
        public FullTextEntityManager getFTEM() {
                return Search.getFullTextEntityManager( em );
        }
}
