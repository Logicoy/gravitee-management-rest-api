/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.management.service.impl;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.gravitee.management.model.NewPageEntity;
import io.gravitee.management.model.PageEntity;
import io.gravitee.management.model.PageListItem;
import io.gravitee.management.model.UpdatePageEntity;
import io.gravitee.management.service.IdGenerator;
import io.gravitee.management.service.PageService;
import io.gravitee.management.service.exceptions.PageAlreadyExistsException;
import io.gravitee.management.service.exceptions.PageNotFoundException;
import io.gravitee.management.service.exceptions.TechnicalManagementException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PageRepository;
import io.gravitee.repository.management.model.Page;

/**
 * @author Titouan COMPIEGNE
 */
@Component
public class PageServiceImpl extends TransactionalService implements PageService {

	private final Logger LOGGER = LoggerFactory.getLogger(PageServiceImpl.class);

	@Autowired
	private PageRepository pageRepository;

	@Autowired
	private IdGenerator idGenerator;

	@Override
	public List<PageListItem> findByApi(String apiId) {
		try {
			final Collection<Page> pages = pageRepository.findByApi(apiId);

			if (pages == null) {
				return emptyList();
			}

			return pages.stream()
					.map(this::reduce)
					.sorted((o1, o2) -> Integer.compare(o1.getOrder(), o2.getOrder()))
					.collect(Collectors.toList());

		} catch (TechnicalException ex) {
			LOGGER.error("An error occurs while trying to get API pages using api ID {}", apiId, ex);
			throw new TechnicalManagementException(
					"An error occurs while trying to get API pages using api ID " + apiId, ex);
		}
	}

	@Override
	public PageEntity findById(String pageId) {
		try {
			LOGGER.debug("Find page by ID: {}", pageId);

			Optional<Page> page = pageRepository.findById(pageId);

			if (page.isPresent()) {
				return convert(page.get());
			}

			throw new PageNotFoundException(pageId);
		} catch (TechnicalException ex) {
			LOGGER.error("An error occurs while trying to find a page using its ID {}", pageId, ex);
			throw new TechnicalManagementException(
					"An error occurs while trying to find a page using its ID " + pageId, ex);
		}
	}

	@Override
	public PageEntity create(String apiId, NewPageEntity newPageEntity) {
		try {
			LOGGER.debug("Create page {} for API {}", newPageEntity, apiId);

			String id = apiId + '_' + idGenerator.generate(newPageEntity.getName());
			Optional<Page> checkPage = pageRepository.findById(id);
			if (checkPage.isPresent()) {
				throw new PageAlreadyExistsException(id);
			}

			Page page = convert(newPageEntity);

			page.setId(apiId + '_' + idGenerator.generate(page.getName()));
			page.setApi(apiId);

			// Set date fields
			page.setCreatedAt(new Date());
			page.setUpdatedAt(page.getCreatedAt());

			Page createdPage = pageRepository.create(page);
			return convert(createdPage);
		} catch (TechnicalException ex) {
			LOGGER.error("An error occurs while trying to create {}", newPageEntity, ex);
			throw new TechnicalManagementException("An error occurs while trying create " + newPageEntity, ex);
		}
	}

	@Override
	public PageEntity update(String pageId, UpdatePageEntity updatePageEntity) {
		try {
			LOGGER.debug("Update Page {}", pageId);

			Optional<Page> optPageToUpdate = pageRepository.findById(pageId);
			if (!optPageToUpdate.isPresent()) {
				throw new PageNotFoundException(pageId);
			}

			Page pageToUpdate = optPageToUpdate.get();
			Page page = convert(updatePageEntity);

			page.setId(pageId);
			page.setUpdatedAt(new Date());

			// Copy fields from existing values
			page.setCreatedAt(pageToUpdate.getCreatedAt());
			page.setType(pageToUpdate.getType());
			page.setApi(pageToUpdate.getApi());

			// if order change, reorder all pages
			if (page.getOrder() != pageToUpdate.getOrder()) {
				reorderAndSavePages(page);
				return null;
			} else {
				Page updatedPage = pageRepository.update(page);
				return convert(updatedPage);
			}
		} catch (TechnicalException ex) {
            throw onUpdateFail(pageId, ex);
		}
	}

    private void reorderAndSavePages(final Page pageToReorder) throws TechnicalException {
		final Collection<Page> pages = pageRepository.findByApi(pageToReorder.getApi());
        final List<Boolean> increment = asList(true);
        pages.stream()
            .sorted((o1, o2) -> Integer.compare(o1.getOrder(), o2.getOrder()))
            .forEachOrdered(page -> {
	            try {
		            if (page.equals(pageToReorder)) {
			            increment.set(0, false);
			            page.setOrder(pageToReorder.getOrder());
		            } else {
			            final int newOrder;
			            final Boolean isIncrement = increment.get(0);
			            if (page.getOrder() < pageToReorder.getOrder()) {
				            newOrder = page.getOrder() - (isIncrement ? 0 : 1);
			            } else if (page.getOrder() > pageToReorder.getOrder())  {
				            newOrder = page.getOrder() + (isIncrement? 1 : 0);
			            } else {
				            newOrder = page.getOrder() + (isIncrement? 1 : -1);
			            }
			            page.setOrder(newOrder);
		            }
		            pageRepository.update(page);
	            } catch (final TechnicalException ex) {
		            throw onUpdateFail(page.getId(), ex);
	            }
            });
	}

    private TechnicalManagementException onUpdateFail(String pageId, TechnicalException ex) {
        LOGGER.error("An error occurs while trying to update page {}", pageId, ex);
        return new TechnicalManagementException("An error occurs while trying to update page " + pageId, ex);
    }

    @Override
	public void delete(String pageName) {
		try {
			LOGGER.debug("Delete PAGE : {}", pageName);
			pageRepository.delete(pageName);
		} catch (TechnicalException ex) {
			LOGGER.error("An error occurs while trying to delete PAGE {}", pageName, ex);
			throw new TechnicalManagementException("An error occurs while trying to delete PAGE " + pageName, ex);
		}
	}

	@Override
	public int findMaxPageOrderByApi(String apiName) {
		try {
			LOGGER.debug("Find Max Order Page for api name : {}", apiName);
			final Integer maxPageOrder = pageRepository.findMaxPageOrderByApi(apiName);
			return maxPageOrder == null ? 0 : maxPageOrder;
		} catch (TechnicalException ex) {
			LOGGER.error("An error occured when searching max order page for api name [{}]", apiName, ex);
			throw new TechnicalManagementException("An error occured when searching max order page for api name " + apiName, ex);
		}
	}

	private PageListItem reduce(Page page) {
		PageListItem pageItem = new PageListItem();

		pageItem.setId(page.getId());
		pageItem.setName(page.getName());
		pageItem.setType(page.getType());
		pageItem.setOrder(page.getOrder());
		pageItem.setLastContributor(page.getLastContributor());
		pageItem.setPublished(page.isPublished());

		return pageItem;
	}

	private static Page convert(NewPageEntity newPageEntity) {
		Page page = new Page();

		page.setName(newPageEntity.getName());
		final String type = newPageEntity.getType();
		if (type != null) {
			page.setType(type);
		}
		page.setContent(newPageEntity.getContent());
		page.setLastContributor(newPageEntity.getLastContributor());
		page.setOrder(newPageEntity.getOrder());
		return page;
	}

	private static PageEntity convert(Page page) {
		PageEntity pageEntity = new PageEntity();

		pageEntity.setId(page.getId());
		pageEntity.setName(page.getName());
		if (page.getType() != null) {
			pageEntity.setType(page.getType().toString());
		}
		pageEntity.setContent(page.getContent());
		pageEntity.setLastContributor(page.getLastContributor());
		pageEntity.setLastModificationDate(page.getUpdatedAt());
		pageEntity.setOrder(page.getOrder());
		pageEntity.setPublished(page.isPublished());
		return pageEntity;
	}

	private static Page convert(UpdatePageEntity updatePageEntity) {
		Page page = new Page();

		page.setName(updatePageEntity.getName());
		page.setContent(updatePageEntity.getContent());
        page.setLastContributor(updatePageEntity.getLastContributor());
		page.setOrder(updatePageEntity.getOrder());
		page.setPublished(updatePageEntity.isPublished());

		return page;
	}

	public IdGenerator getIdGenerator() {
		return idGenerator;
	}

	public void setIdGenerator(IdGenerator idGenerator) {
		this.idGenerator = idGenerator;
	}
}
