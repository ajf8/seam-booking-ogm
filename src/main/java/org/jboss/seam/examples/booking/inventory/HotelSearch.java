/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.seam.examples.booking.inventory;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Stateful;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.apache.lucene.search.Query;
import org.hibernate.search.jpa.FullTextQuery;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.query.DatabaseRetrievalMethod;
import org.hibernate.search.query.ObjectLookupMethod;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.jboss.solder.logging.Logger;
import org.jboss.seam.examples.booking.model.Hotel;
import org.jboss.seam.international.status.builder.TemplateMessage;

/**
 * @author <a href="http://community.jboss.org/people/dan.j.allen">Dan Allen</a>
 */
@Named
@Stateful
@SessionScoped
public class HotelSearch {

	@Inject
	private Logger log;

	@Inject
	private SearchCriteria criteria;

	@Inject
	private Provider<FullTextEntityManager> lazyFEM;

	@Inject
	private Instance<TemplateMessage> messageBuilder;

	private boolean nextPageAvailable = false;

	private List<Hotel> hotels = new ArrayList<Hotel>();

	public void find() {
		criteria.firstPage();
		queryHotels(criteria);
	}

	public void nextPage() {
		criteria.nextPage();
		queryHotels(criteria);
	}

	public void previousPage() {
		criteria.previousPage();
		queryHotels(criteria);
	}

	@Produces
	@Named
	public List<Hotel> getHotels() {
		return hotels;
	}

	public boolean isNextPageAvailable() {
		return nextPageAvailable;
	}

	public boolean isPreviousPageAvailable() {
		return criteria.getPage() > 0;
	}

	private void queryHotels(final SearchCriteria criteria) {
		if (criteria.getQuery().length() < 1) {
			nextPageAvailable = false;
			hotels = new ArrayList<Hotel>();
			return;
		}

		FullTextEntityManager em = lazyFEM.get();

		final QueryBuilder builder = em.getSearchFactory().buildQueryBuilder()
				.forEntity(Hotel.class).get();

		final Query luceneQuery = builder.keyword().onField("name")
				.matching(criteria.getQuery()).createQuery();

		System.out.println(luceneQuery.toString());

		final FullTextQuery query = em.createFullTextQuery(luceneQuery,
				Hotel.class);
		query.initializeObjectsWith(ObjectLookupMethod.SKIP,
				DatabaseRetrievalMethod.FIND_BY_ID);

		final List<Hotel> results = query
				.setFirstResult(criteria.getFetchOffset())
				.setMaxResults(criteria.getFetchSize()).getResultList();

		nextPageAvailable = results.size() > criteria.getPageSize();
		if (nextPageAvailable) {
			// NOTE create new ArrayList since subList creates unserializable
			// list
			hotels = new ArrayList<Hotel>(results.subList(0,
					criteria.getPageSize()));
		} else {
			hotels = results;
		}

		log.info(messageBuilder
				.get()
				.text("Found {0} hotel(s) matching search term [ {1} ] (limit {2})")
				.textParams(hotels.size(), criteria.getQuery(),
						criteria.getPageSize()).build().getText());
	}
}
