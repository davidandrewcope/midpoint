/**
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.schema;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.schema.SchemaRegistry;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.delta.ObjectDelta;
import com.evolveum.midpoint.schema.delta.PropertyDelta;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.processor.ChangeType;
import com.evolveum.midpoint.schema.processor.DiffUtil;
import com.evolveum.midpoint.schema.processor.MidPointObject;
import com.evolveum.midpoint.schema.processor.ObjectDefinition;
import com.evolveum.midpoint.schema.processor.PropertyValue;
import com.evolveum.midpoint.schema.processor.Schema;
import com.evolveum.midpoint.schema.util.JAXBUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;

/**
 * @author semancik
 *
 */
public class TestParseDiffPatch {
	
	private static final String TEST_DIR = "src/test/resources/diff/";

	@Test
	public void testUser() throws SchemaException, SAXException, IOException, JAXBException {
		System.out.println("===[ testUser ]===");
		
		SchemaRegistry reg = new SchemaRegistry();
		reg.initialize();
		
		Schema objectSchema = reg.getObjectSchema();
        assertNotNull(objectSchema);

        // "Automatic" parsing
        MidPointObject<UserType> userBefore = objectSchema.parseObject(new File(TEST_DIR, "user-jack-before.xml"), UserType.class);
        
        ObjectDefinition<UserType> userDefinition = objectSchema.findObjectDefinitionByType(SchemaConstants.I_USER_TYPE);
        assertNotNull("UserType definition not found in object schema", userDefinition);
        
        // "Manual" parsing
        JAXBElement<UserType> jaxbElement = JAXBUtil.unmarshal(new File(TEST_DIR, "user-jack-after.xml"), UserType.class);
        UserType userTypeAfter = jaxbElement.getValue();
        MidPointObject<UserType> userAfter = userDefinition.parseObjectType(userTypeAfter);
        
        // WHEN
        
        ObjectDelta<UserType> userDelta = userBefore.compareTo(userAfter);
        
        // THEN
        
        System.out.println("DELTA:");
        System.out.println(userDelta.dump());
        
        assertEquals("Wrong delta OID", userBefore.getOid(), userDelta.getOid());
        assertEquals("Wrong change type", ChangeType.MODIFY, userDelta.getChangeType());
        Collection<PropertyDelta> modifications = userDelta.getModifications();
        assertEquals("Unexpected number of modifications",2,modifications.size());
        assertReplace(userDelta, new QName(SchemaConstants.NS_C,"fullName"), "Cpt. Jack Sparrow");
        assertAdd(userDelta, new QName(SchemaConstants.NS_C,"honorificPrefix"), "Cpt.");
	}
	
	@Test
	public void testAddDelta() throws SchemaException, SAXException, IOException {
		System.out.println("===[ testAddDelta ]===");
		
		// GIVEN
		SchemaRegistry reg = new SchemaRegistry();
		reg.initialize();
		
		Schema objectSchema = reg.getObjectSchema();
        assertNotNull(objectSchema);

        // WHEN
        ObjectDelta<UserType> userDelta = DiffUtil.diff(null,new File(TEST_DIR, "user-jack-after.xml"), UserType.class, objectSchema);

        //THEN
        System.out.println("DELTA:");
        System.out.println(userDelta.dump());
        
        assertEquals("Wrong delta OID", "deadbeef-c001-f00d-1111-222233330001", userDelta.getOid());
        assertEquals("Wrong change type", ChangeType.ADD, userDelta.getChangeType());
        
        // TODO
	}

	private void assertReplace(ObjectDelta<UserType> userDelta, QName propertyName, Object... expectedValues) {
		PropertyDelta propertyDelta = userDelta.getPropertyDelta(propertyName);
		assertNotNull("Property delta for "+propertyName+" not found",propertyDelta);
		assertSet(propertyName, propertyDelta.getValuesToReplace(), expectedValues);
	}

	private void assertAdd(ObjectDelta<UserType> userDelta, QName propertyName, Object... expectedValues) {
		PropertyDelta propertyDelta = userDelta.getPropertyDelta(propertyName);
		assertNotNull("Property delta for "+propertyName+" not found",propertyDelta);
		assertSet(propertyName, propertyDelta.getValuesToAdd(), expectedValues);
	}
	
	private void assertSet(QName propertyName, Collection<PropertyValue<Object>> valuesFromDelta, Object[] expectedValues) {
		assertEquals("Wrong number of values",expectedValues.length, valuesFromDelta.size());
		for (PropertyValue<Object> valueToReplace: valuesFromDelta) {
			boolean found = false;
			for (Object value: expectedValues) {
				if (value.equals(valueToReplace.getValue())) {
					found = true;
				}
			}
			if (!found) {
				AssertJUnit.fail("Unexpected value "+valueToReplace+" in delta for "+propertyName);
			}
		}
	}


}
