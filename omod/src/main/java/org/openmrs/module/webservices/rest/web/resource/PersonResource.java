/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.webservices.rest.web.resource;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import org.apache.commons.lang.WordUtils;
import org.openmrs.OpenmrsData;

import org.openmrs.Patient;
import org.openmrs.Person;
import org.openmrs.PersonAddress;
import org.openmrs.PersonName;
import org.openmrs.annotation.Handler;
import org.openmrs.api.context.Context;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.RestUtil;
import org.openmrs.module.webservices.rest.web.annotation.PropertySetter;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.representation.DefaultRepresentation;
import org.openmrs.module.webservices.rest.web.representation.FullRepresentation;
import org.openmrs.module.webservices.rest.web.representation.RefRepresentation;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.resource.impl.DataDelegatingCrudResource;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingResourceDescription;
import org.openmrs.module.webservices.rest.web.response.ConversionException;
import org.openmrs.module.webservices.rest.web.response.ResponseException;
import org.openmrs.util.OpenmrsUtil;
import org.springframework.util.ReflectionUtils;

/**
 * {@link Resource} for Person, supporting standard CRUD operations
 */
@Resource("person")
@Handler(supports = Person.class, order = 0)
public class PersonResource extends DataDelegatingCrudResource<Person> {
	
	public PersonResource() {
		
	}
	
	/**
	 * @see org.openmrs.module.webservices.rest.web.resource.impl.DelegatingCrudResource#getRepresentationDescription(org.openmrs.module.webservices.rest.web.representation.Representation)
	 */
	@Override
	public DelegatingResourceDescription getRepresentationDescription(Representation rep) {
		if (rep instanceof RefRepresentation) {
			DelegatingResourceDescription description = new DelegatingResourceDescription();
			description.addProperty("uuid");
			description.addProperty("uri", findMethod("getUri"));
			description.addProperty("display", findMethod("getDisplayString"));
			return description;
		} else if (rep instanceof DefaultRepresentation) {
			DelegatingResourceDescription description = new DelegatingResourceDescription();
			description.addProperty("uuid");
			description.addProperty("gender");
			description.addProperty("age");
			description.addProperty("birthdate");
			description.addProperty("birthdateEstimated");
			description.addProperty("dead");
			description.addProperty("deathDate");
			description.addProperty("causeOfDeath", Representation.REF);
			description.addProperty("preferredName", "personName", Representation.REF);
			description.addProperty("preferredAddress", "personAddress", Representation.REF);
			description.addProperty("attributes", Representation.REF);
			description.addProperty("uri", findMethod("getUri"));
			return description;
		} else if (rep instanceof FullRepresentation) {
			DelegatingResourceDescription description = new DelegatingResourceDescription();
			description.addProperty("uuid");
			description.addProperty("gender");
			description.addProperty("age");
			description.addProperty("birthdate");
			description.addProperty("birthdateEstimated");
			description.addProperty("dead");
			description.addProperty("deathDate");
			description.addProperty("causeOfDeath");
            description.addProperty("preferredName", "personName", Representation.DEFAULT);
			description.addProperty("preferredAddress", "personAddress", Representation.DEFAULT);
			description.addProperty("names");
			description.addProperty("addresses");
			description.addProperty("attributes");
			description.addProperty("auditInfo", findMethod("getAuditInfo"));
			description.addProperty("uri", findMethod("getUri"));
			return description;
		}
		return null;
	}
	
	/**
	 * Returns non-voided elements for the attributes, names and addresses properties
	 * @param instance
	 * @param propertyName
	 * @return
	 * @throws ConversionException 
	 */
	@Override
	public Object getProperty(Person instance, String propertyName) throws ConversionException {
		if (propertyName.equals("attributes") || propertyName.equals("names") || propertyName.equals("addresses")) {
			String getterName = "get" + WordUtils.capitalizeFully(propertyName);
			Method getterMethod = ReflectionUtils.findMethod(instance.getClass(), getterName);
			return RestUtil.removeVoidedData((Collection<OpenmrsData>) ReflectionUtils.invokeMethod(getterMethod, instance));
		} else {
			return super.getProperty(instance, propertyName);
		}
	}
	
	/**
	 * Sets the preferred name for a person. If no name exists new name is set as preferred.
	 * 
	 * @param instance
	 * @param name
	 */
	@PropertySetter("preferredName")
	public static void setPreferredName(Person instance, PersonName name) {
		if (name.getId() == null) {
			// adding a new name and set it as preferred
			name.setPreferred(true);
			instance.addName(name);
		} else {
			// switching which name is preferred
			for (PersonName existing : instance.getNames()) {
				if (existing.isVoided())
					continue;
				if (existing.isPreferred() && !existing.equals(name))
					existing.setPreferred(false);
			}
			name.setPreferred(true);
			instance.addName(name);
		}
	}
	
	@PropertySetter("preferredAddress")
	public static void setPreferredAddress(Patient instance, PersonAddress address) {
		//un mark the current preferred address as preferred if any
		for (PersonAddress existing : instance.getAddresses()) {
			if (existing.isVoided())
				continue;
			if (existing.isPreferred() && !OpenmrsUtil.nullSafeEquals(existing, address))
				existing.setPreferred(false);
		}
		address.setPreferred(true);
		if (address.getPersonAddressId() == null)
			instance.addAddress(address);
	}
	
	/**
	 * @see org.openmrs.module.webservices.rest.web.resource.impl.DelegatingCrudResource#getByUniqueId(java.lang.String)
	 */
	@Override
	public Person getByUniqueId(String uuid) {
		return Context.getPersonService().getPersonByUuid(uuid);
	}
	
	/**
	 * @see org.openmrs.module.webservices.rest.web.resource.impl.DelegatingCrudResource#newDelegate()
	 */
	@Override
	protected Person newDelegate() {
		return new Person();
	}
	
	/**
	 * @see org.openmrs.module.webservices.rest.web.resource.impl.DelegatingCrudResource#save(java.lang.Object)
	 */
	@Override
	protected Person save(Person person) {
		return Context.getPersonService().savePerson(person);
	}
	
	/**
	 * @see org.openmrs.module.webservices.rest.web.resource.impl.DelegatingCrudResource#doSearch(java.lang.String,
	 *      org.openmrs.module.webservices.rest.web.RequestContext)
	 */
	@Override
	protected List<Person> doSearch(String query, RequestContext context) {
		return Context.getPersonService().getPeople(query, null);
	}
	
	/**
	 * @see org.openmrs.module.webservices.rest.web.resource.impl.DelegatingCrudResource#delete(java.lang.Object,
	 *      java.lang.String, org.openmrs.module.webservices.rest.web.RequestContext)
	 */
	@Override
	protected void delete(Person person, String reason, RequestContext context) throws ResponseException {
		if (person.isVoided()) {
			// DELETE is idempotent, so we return success here
			return;
		}
		Context.getPersonService().voidPerson(person, reason);
	}
	
	/**
	 * @see org.openmrs.module.webservices.rest.web.resource.impl.DelegatingCrudResource#purge(java.lang.Object,
	 *      org.openmrs.module.webservices.rest.web.RequestContext)
	 */
	@Override
	public void purge(Person person, RequestContext context) throws ResponseException {
		if (person == null) {
			// DELETE is idempotent, so we return success here
			return;
		}
		Context.getPersonService().purgePerson(person);
	}
	
	/**
	 * @param person
	 * @return fullname (for concise display purposes)
	 */
	public String getDisplayString(Person person) {
		return person.getPersonName().getFullName();
	}
}
