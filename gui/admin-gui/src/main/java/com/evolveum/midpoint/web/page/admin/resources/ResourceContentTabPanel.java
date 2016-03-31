/*
 * Copyright (c) 2010-2016 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.web.page.admin.resources;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.ListModel;

import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.input.AutoCompleteTextPanel;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.input.QNameChoiceRenderer;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

public class ResourceContentTabPanel extends Panel {

	private static final Trace LOGGER = TraceManager.getTrace(ResourceContentTabPanel.class);

	enum Operation {
		REMOVE, MODIFY;
	}

	private static final String DOT_CLASS = ResourceContentTabPanel.class.getName() + ".";

	private static final String ID_INTENT = "intent";
	private static final String ID_OBJECT_CLASS = "objectClass";
	private static final String ID_INTENT_LABEL = "intentLabel";
	private static final String ID_OBJECT_CLASS_LABEL = "objectClassLabel";
	private static final String ID_MAIN_FORM = "mainForm";

	private static final String ID_REPO_SEARCH = "repositorySearch";
	private static final String ID_RESOURCE_SEARCH = "resourceSearch";

	private static final String ID_TABLE = "table";
//	private static final String ID_RESOURCE_TABLE = "resourceTable";


	private PageBase parentPage;
	private ShadowKindType kind;
	
	private boolean useObjectClass;

	
	private Model<Boolean> resourceSearchModel = new Model<Boolean>(false);

	private IModel<String> intentModel;
	
	private IModel<QName> objectClassModel;


	public ResourceContentTabPanel(String id, ShadowKindType kind,
			final IModel<PrismObject<ResourceType>> model, PageBase parentPage) {
		super(id, model);
		this.parentPage = parentPage;
		
		if (kind == null){
			useObjectClass = true;
		} else {
			this.kind = kind;
		}

		intentModel = new Model<String>();
		objectClassModel = new Model<QName>();
	
		initLayout(model);
	}

	
	private void initLayout(final IModel<PrismObject<ResourceType>> model) {
			
		setOutputMarkupId(true);

		Label intentLabel = new Label(ID_INTENT_LABEL, parentPage.createStringResource("ShadowType.intent"));
		intentLabel.add(new VisibleEnableBehaviour() {
			
			@Override
			public boolean isVisible() {
				return !useObjectClass;
			}
		});
		add(intentLabel);
		AutoCompleteTextPanel<String> intent = new AutoCompleteTextPanel<String>(ID_INTENT, intentModel,
				String.class) {

		
					private static final long serialVersionUID = 1L;

			@Override
			public Iterator<String> getIterator(String input) {
				RefinedResourceSchema refinedSchema = null;
				try {
					refinedSchema = RefinedResourceSchema.getRefinedSchema(model.getObject(),
							parentPage.getPrismContext());

				} catch (SchemaException e) {
					return new ArrayList<String>().iterator();
				}
				return RefinedResourceSchema.getIntentsForKind(refinedSchema, kind).iterator();

			}
			
			

		};
		intent.getBaseFormComponent().add(new OnChangeAjaxBehavior() {
			
			private static final long serialVersionUID = 1L;

			@Override
			protected void onUpdate(AjaxRequestTarget target) {
				Form mainForm = (Form) get(ID_MAIN_FORM);
				mainForm.addOrReplace(initTable(model));
				target.add(addOrReplace(mainForm));

			}
		});
		intent.setOutputMarkupId(true);
		intent.add(new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return !useObjectClass;
			}
		});
		add(intent);
		
		DropDownChoicePanel<QName> objectClass = new DropDownChoicePanel<QName>(ID_OBJECT_CLASS, objectClassModel, createObjectClassChoices(model), new QNameChoiceRenderer());
		objectClass.getBaseFormComponent().add(new OnChangeAjaxBehavior() {
			
			@Override
			protected void onUpdate(AjaxRequestTarget target) {
				Form mainForm = (Form) get(ID_MAIN_FORM);
				mainForm.addOrReplace(initTable(model));
				target.add(addOrReplace(mainForm));
				
			}
		});
		
		objectClass.add(new VisibleEnableBehaviour() {
			
			@Override
			public boolean isVisible() {
				return useObjectClass;
			}
		});
		add(objectClass);
		
		Label objectClassLabel = new Label(ID_OBJECT_CLASS_LABEL, parentPage.createStringResource("ShadowType.objectClass"));
		objectClassLabel.add(new VisibleEnableBehaviour() {
			
			@Override
			public boolean isVisible() {
				return useObjectClass;
			}
		});
		add(objectClassLabel);
		

		AjaxButton repoSearch = new AjaxButton(ID_REPO_SEARCH) {
			
			@Override
			public void onClick(AjaxRequestTarget target) {
				resourceSearchModel.setObject(Boolean.FALSE);
				Form mainForm = (Form) getParent().get(ID_MAIN_FORM);
//				mainForm.addOrReplace(initResourceContent(model));
				mainForm.addOrReplace(initRepoContent(model));
				target.add(getParent().addOrReplace(mainForm));
				target.add(this);
				target.add(getParent().get(ID_RESOURCE_SEARCH).add(AttributeModifier.replace("class", "btn btn-default")));
			}
			
			@Override
			protected void onBeforeRender() {
				super.onBeforeRender();
				if (!ResourceContentTabPanel.this.resourceSearchModel.getObject()) add(AttributeModifier.append("class", " active"));
			}
		};
		add(repoSearch);
		
		AjaxButton resourceSearch = new AjaxButton(ID_RESOURCE_SEARCH) {
			
			@Override
			public void onClick(AjaxRequestTarget target) {
				resourceSearchModel.setObject(Boolean.TRUE);
				Form mainForm = (Form) getParent().get(ID_MAIN_FORM);
				
				mainForm.addOrReplace(initResourceContent(model));
//				mainForm.addOrReplace(initRepoContent(model));
				target.add(getParent().addOrReplace(mainForm));
				target.add(this.add(AttributeModifier.append("class", " active")));
				target.add(getParent().get(ID_REPO_SEARCH).add(AttributeModifier.replace("class", "btn btn-default")));
			}
			

			@Override
			protected void onBeforeRender() {
				super.onBeforeRender();
				if (ResourceContentTabPanel.this.resourceSearchModel.getObject()) add(AttributeModifier.append("class", " active"));
			}
		};
		add(resourceSearch);
		
		Form mainForm = new Form(ID_MAIN_FORM);
		mainForm.setOutputMarkupId(true);
		mainForm.addOrReplace(initTable(model));
		add(mainForm);
		
		

	}
	
	
	private ListModel<QName> createObjectClassChoices(IModel<PrismObject<ResourceType>> model) {
		RefinedResourceSchema refinedSchema;
		try {
			refinedSchema = RefinedResourceSchema
					.getRefinedSchema(model.getObject(),parentPage.getPrismContext());
		} catch (SchemaException e) {
			warn("Could not determine defined obejct classes for resource");
			return new ListModel<QName>(new ArrayList<QName>());
		}
		Collection<ObjectClassComplexTypeDefinition> defs = refinedSchema.getObjectClassDefinitions();
		List<QName> objectClasses = new ArrayList<QName>(defs.size());
		for (ObjectClassComplexTypeDefinition def : defs ) { 
			objectClasses.add(def.getTypeName());
		}
		return new ListModel<>(objectClasses);
	}


	private ResourceContentPanel initTable(IModel<PrismObject<ResourceType>> model){
		if (resourceSearchModel.getObject()){
			return initResourceContent(model);
		} else {
			return initRepoContent(model);
		}
	}

	private ResourceContentResourcePanel initResourceContent(IModel<PrismObject<ResourceType>> model) {
		ResourceContentResourcePanel resourceContent = new ResourceContentResourcePanel(ID_TABLE, model, objectClassModel.getObject(), kind, intentModel.getObject(), parentPage);
		resourceContent.setOutputMarkupId(true);
		return resourceContent;
		
	}
	
	private ResourceContentRepositoryPanel initRepoContent(IModel<PrismObject<ResourceType>> model) {
		ResourceContentRepositoryPanel repositoryContent = new ResourceContentRepositoryPanel(ID_TABLE, model, objectClassModel.getObject(), kind, intentModel.getObject(), parentPage);
		repositoryContent.setOutputMarkupId(true);
		return repositoryContent;
	}


	private IModel<String> createDeleteConfirmString() {
		return new AbstractReadOnlyModel<String>() {
			@Override
			public String getObject() {
				return "asdasd";
				
			}
		};
	}

}