/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.apache.stanbol.rules.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.stanbol.ontologymanager.ontonet.api.ONManager;
import org.apache.stanbol.ontologymanager.ontonet.impl.ONManagerImpl;
import org.apache.stanbol.rules.base.api.RuleStore;
import org.apache.stanbol.rules.manager.changes.AddRule;
import org.apache.stanbol.rules.manager.changes.LoadRuleFile;
import org.apache.stanbol.rules.manager.changes.RemoveRule;
import org.apache.stanbol.rules.manager.changes.RuleStoreImpl;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

/**
 *
 * @author elvio
 */
public class RemoveRuleTest {

    public RemoveRuleTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    	Dictionary<String, Object> configuration = new Hashtable<String, Object>();
    	onm = new ONManagerImpl(null,null, new Hashtable<String, Object>());
    	store = new RuleStoreImpl(onm, configuration,"./src/main/resources/RuleOntology/TestKReSOntologyRules.owl");
    }

    @After
    public void tearDown() {
    	store = null;
    	onm = null;
    }

    public RuleStore store = null;
    public ONManager onm = null;

    /**
     * Test of removeRule method, of class RemoveRule.
     */
    @Test
    public void testRemoveRule() {
//        RuleStore store  = new RuleStoreImpl("./src/main/resources/RuleOntology/TestKReSOntologyRules.owl");
        
        //Load the example file
        LoadRuleFile load = new LoadRuleFile("./src/main/resources/RuleOntology/TestRuleFileExample.txt",store);
        AddRule rule = new AddRule(load.getStore());
        rule.addRule("MyRuleProva","Body -> Head",null);
        String ruleName = "MyRuleProva";
        RemoveRule instance = new RemoveRule(rule.getStore());
        boolean expResult = true;
        boolean result = instance.removeRule(ruleName);
        if(result){
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        }else{fail("Some errors occur with removeRule of KReSRemoveRule.");}
    }

    /**
     * Test of removeRule method, of class RemoveRule.
     */
    @Test
    public void testRemoveSingleRule() throws OWLOntologyStorageException {
//        RuleStore store  = new RuleStoreImpl("./src/main/resources/RuleOntology/TestKReSOntologyRules.owl");
        String owlID = store.getOntology().getOntologyID().toString().replace("<", "").replace(">", "") + "#";

        //Load the example file
        LoadRuleFile load = new LoadRuleFile("./src/main/resources/RuleOntology/TestRuleFileExample.txt",store);
        IRI rule = IRI.create(owlID+"MyRuleB");
        IRI recipe = IRI.create(owlID+"MyRecipe");
        RemoveRule instance = new RemoveRule(load.getStore());
        boolean expResult = true;
        boolean result = instance.removeRuleFromRecipe(rule, recipe);
       
        if(result){
            assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        }else{fail("Some errors occur with removeRule of KReSRemoveRule.");}
    }

}