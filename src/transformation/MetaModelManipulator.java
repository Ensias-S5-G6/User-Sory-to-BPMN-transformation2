package transformation;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EOperation;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.impl.DynamicEObjectImpl;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreEList;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;


public class MetaModelManipulator {
	
	EPackage MMEpackage ; 
	EObject MMRacine ; 
	DynamicEObjectImpl MRacineNode ;
	String metaModelName  ; 
	static Map<String, EObject> activitiesTaskMap = new HashMap<String, EObject>() ; 
	static Map<String, EObject> rolePoolMap = new HashMap<String, EObject>() ; 
	
	
	
	
	public void loadMetaModel(String uri , String metaModelName) {
		
		//declaration des resources 
		ResourceSet resourceSet = new ResourceSetImpl();
		
		//obtention des informations
		resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("ecore" , new XMIResourceFactoryImpl());
		
		//chargement de la resources
		Resource MMResource = resourceSet.getResource(URI.createURI(uri), true) ; 
		
		
		this.MMRacine = MMResource.getContents().get(0) ; 
		
		 this.MMEpackage = (EPackage) MMRacine ; 
		 
		 EList<EClassifier> eClassifiers = this.MMEpackage.getEClassifiers();
		 System.out.println("***************************meta model loading************************************");
	 	System.out.println("Meta Model :"+metaModelName+" loaded successfully");
	 	System.out.println("***********************************************************************************");
	 	this.metaModelName = metaModelName ; 
	 	System.out.println("");
        for (EClassifier eClassifier : eClassifiers) {
            System.out.println(eClassifier.getName());
            System.out.print("  ");

            if (eClassifier instanceof EClass) {
                EClass eClass = (EClass) eClassifier;
                EList<EAttribute> eAttributes = eClass.getEAttributes();
                for (EAttribute eAttribute : eAttributes) {
                    System.out.print(eAttribute.getName() + "("
                            + eAttribute.getEAttributeType().getName() + ") ");
                }

                if (!eClass.getEAttributes().isEmpty()
                        && !eClass.getEReferences().isEmpty()) {
                    System.out.println();
                    System.out.print("  Références : ");
                }

                EList<EReference> eReferences = eClass.getEReferences();
                for (EReference eReference : eReferences) {
                    System.out.print(eReference.getName() + "("
                            + eReference.getEReferenceType().getName() + "["
                            + eReference.getLowerBound() + ".."
                            + eReference.getUpperBound() + "])");
                }

                if (!eClass.getEOperations().isEmpty()) {
                    System.out.println();
                    System.out.print("  Opérations : ");

                    for (EOperation eOperation : eClass.getEOperations()) {
                        System.out.println(eOperation.getEType().getName()
                                + " " + eOperation.getName());
                    }
                }
            }
            System.out.println();
        }
		
	}
	
	public DynamicEObjectImpl loadModel(String uri) throws IOException {
		//declaration des resources 
		ResourceSet resourceSet = new ResourceSetImpl();
		
		//obtention des informations
		resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("model" , new XMIResourceFactoryImpl());
		
		//associer un identifiant au model
		resourceSet.getPackageRegistry().put(this.MMEpackage.getNsURI() , this.MMEpackage);
		
		Resource Mresource = resourceSet.getResource(URI.createURI(uri), true) ;
		Mresource.load(null);
		this.MRacineNode = (DynamicEObjectImpl) Mresource.getContents().get(0);	
		EClass eClassifiers = this.MRacineNode.eClass();
		System.out.println("***************************model loading************************************");
		System.out.println("model "+this.metaModelName+" is loaded sucessfully!!!!!");
		System.out.println("*****************************************************************************");
		
		
		return this.MRacineNode ; 
		
	}
	
	public Resource createModel(String uri) throws IOException {
	     ResourceSet resourceSet = new ResourceSetImpl();
	     resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("model", new XMIResourceFactoryImpl());
	     return resourceSet.createResource(URI.createURI(uri));
	}
	
	
	
	
	public void userStory2BPM(DynamicEObjectImpl userStoryRootNode , String destinationModelPath ) throws IOException {
		
		this.mapRoleToPool(userStoryRootNode);
		this.mapTaskToActivities(userStoryRootNode);
		System.out.print("activityMap : "+MetaModelManipulator.activitiesTaskMap);
		System.out.print("roleMap : "+MetaModelManipulator.rolePoolMap);
		
		EClass backlog = userStoryRootNode.eClass() ;
		EReference userStoriesRef = (EReference)backlog.getEStructuralFeature("userStories");
		EcoreEList<DynamicEObjectImpl> userStories = (EcoreEList)userStoryRootNode.eGet(userStoriesRef);
		
		ResourceSet resourceSet = new ResourceSetImpl();
	    resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("model", new XMIResourceFactoryImpl());
        Resource resource = resourceSet.createResource(URI.createURI(destinationModelPath));
		
		
        EClass defintionClass = (EClass) this.MMEpackage.getEClassifier("Definition"); 
		System.out.println("defintion : "+defintionClass);
		EReference eDefintionPool = (EReference) defintionClass.getEStructuralFeature("pools");
		EObject defintionInstance = this.MMEpackage.getEFactoryInstance().create(defintionClass);
		List<EObject> defintionPools = new ArrayList<EObject>();
	     
		for(DynamicEObjectImpl userStoryRef : userStories) {
			
			EClass userStoryClass = userStoryRef.eClass();
			EAttribute idRef = (EAttribute)getAttByName(userStoryRef, "id");
			
			//id
			Integer id = (Integer) userStoryRef.eGet(idRef);
			//task
			String taskDescription =  getEclassAttributeValue(userStoryRef , "task" , "description" , String.class);

			//role
			String roleName =  getEclassAttributeValue(userStoryRef , "role" , "roleName" , String.class);
			
			//value
			String valueAttr =  getEclassAttributeValue(userStoryRef , "value" , "value" , String.class);
			
			
			//next
			EReference nextRef = (EReference) userStoryClass.getEStructuralFeature("next");
			DynamicEObjectImpl next = (DynamicEObjectImpl)userStoryRef.eGet(nextRef);
			
			this.processAcceptanceCriteria(userStoryClass , userStoryRef);
			
			
			//pool instance
			EClass poolClass = (EClass) this.MMEpackage.getEClassifier("Pool"); 
			EReference ePoolProcess = (EReference) poolClass.getEStructuralFeature("process");
			EObject poolInstance = MetaModelManipulator.rolePoolMap.get(roleName);
			
			
			//process instance
			EClass processClass = (EClass) this.MMEpackage.getEClassifier("Process"); 
			EAttribute eProcessIdRef = (EAttribute) processClass.getEStructuralFeature("id");
			EAttribute eProceNameRef = (EAttribute) processClass.getEStructuralFeature("name");
			EReference eProcessActivitiesRef = (EReference) processClass.getEStructuralFeature("activities");
			EReference eProcessTransitionsRef = (EReference) processClass.getEStructuralFeature("transitions");
			EObject processInstance = (EObject) poolInstance.eGet(ePoolProcess);
			Integer eProcessId = (Integer) processInstance.eGet(eProcessIdRef);
			List<EObject> eProcessActivities = (List) processInstance.eGet(eProcessActivitiesRef);
			List<EObject> eProcessTransitions = (List) processInstance.eGet(eProcessTransitionsRef);
			
			//activities intance
			EObject activityInstance = MetaModelManipulator.activitiesTaskMap.get(taskDescription);

			//add activity
			eProcessActivities.add(activityInstance);
			
			//add transition
			if(next != null) {
				eProcessTransitions.add(AddTransition(taskDescription , id , next));
			}
			
			 
			//add pool to defition
			System.out.println("process intance : "+eProcessId);
			defintionPools.add(poolInstance);
			
			//add process to pool
			defintionPools.add(processInstance);


		}
		
		//add all pools to defition
		defintionInstance.eSet(eDefintionPool, defintionPools);
		
		//add defition node as a root for model
		resource.getContents().add(defintionInstance);
		
		
		resource.save(null);
		System.out.println("#############################################****************###############################################");
		System.out.println("#############################################*** Transformation done *************##########################");
		System.out.println("#############################################****************###############################################");
	}
	
	private void mapTaskToActivities(DynamicEObjectImpl userStoryRootNode) {
		
		EClass backlog = userStoryRootNode.eClass() ;
		EReference userStoriesRef = (EReference)backlog.getEStructuralFeature("userStories");
		EcoreEList<DynamicEObjectImpl> userStories = (EcoreEList)userStoryRootNode.eGet(userStoriesRef);
		
		for(DynamicEObjectImpl userStoryRef : userStories) {
			
			EClass userStoryClass = userStoryRef.eClass();
			EAttribute idRef = (EAttribute)getAttByName(userStoryRef, "id");
			String id = String.valueOf(userStoryRef.eGet(idRef));
			
			//task
			String taskDescription =  getEclassAttributeValue(userStoryRef , "task" , "description" , String.class);
				
			EClass activtyClass = (EClass) this.MMEpackage.getEClassifier("Activity");
			EAttribute eactivtyId = (EAttribute)activtyClass.getEStructuralFeature("id"); 
			EAttribute activtyName = (EAttribute) activtyClass.getEStructuralFeature("name"); 
			EObject activityInstance = this.MMEpackage.getEFactoryInstance().create(activtyClass);
			activityInstance.eSet(eactivtyId, Integer.valueOf((int) Math.random()));
			activityInstance.eSet(activtyName, taskDescription);
			
			MetaModelManipulator.activitiesTaskMap.put(taskDescription, activityInstance);
		}
	}
	
	private void mapRoleToPool(DynamicEObjectImpl userStoryRootNode) {
		EClass backlog = userStoryRootNode.eClass() ;
		EReference userStoriesRef = (EReference)backlog.getEStructuralFeature("userStories");
		EcoreEList<DynamicEObjectImpl> userStories = (EcoreEList)userStoryRootNode.eGet(userStoriesRef);
		
		for(DynamicEObjectImpl userStoryRef : userStories) {
			
			EAttribute idRef = (EAttribute)getAttByName(userStoryRef, "id");
			String id = String.valueOf(userStoryRef.eGet(idRef));
			
			//role
			String roleName =  getEclassAttributeValue(userStoryRef , "role" , "roleName" , String.class);
				
			EClass poolClass = (EClass) this.MMEpackage.getEClassifier("Pool"); 
			EAttribute ePoolId = (EAttribute) poolClass.getEStructuralFeature("id");
			EAttribute ePoolName = (EAttribute) poolClass.getEStructuralFeature("name");
			EReference ePoolProcess = (EReference) poolClass.getEStructuralFeature("process");
			EObject poolInstance = this.MMEpackage.getEFactoryInstance().create(poolClass);
			poolInstance.eSet(ePoolId, Integer.valueOf((int) Math.random()));
			poolInstance.eSet(ePoolName, roleName);
			
			//process intance
			EClass processClass = (EClass) this.MMEpackage.getEClassifier("Process"); 
			EAttribute eProcessId = (EAttribute) processClass.getEStructuralFeature("id");
			EAttribute eProceName = (EAttribute) processClass.getEStructuralFeature("name");
			EReference eProcessActivities = (EReference) processClass.getEStructuralFeature("activities");
			EReference eProcessTransitions = (EReference) processClass.getEStructuralFeature("transitions");
			
			EObject processInstance = this.MMEpackage.getEFactoryInstance().create(processClass);
			processInstance.eSet(eProcessId , userStoryRef.eGet(idRef));
			processInstance.eSet(eProceName, String.valueOf(userStoryRef.eGet(idRef)));
			processInstance.eSet(eProcessActivities, new ArrayList<EObject>());
			processInstance.eSet(eProcessTransitions, new ArrayList<EObject>());
			
			poolInstance.eSet(ePoolProcess, processInstance);
			
			MetaModelManipulator.rolePoolMap.put(roleName, poolInstance);
		}
	}
	
	
	private EObject AddTransition(String  currTask , Integer currId , DynamicEObjectImpl next ) {
		
		//from instance
		EObject from = MetaModelManipulator.activitiesTaskMap.get(currTask);
		
		//next id 
		EAttribute nextIdRef = (EAttribute)getAttByName(next, "id");
		Integer nextId = (Integer)next.eGet(nextIdRef);
		
		
		//next task
		String nextVal =  getEclassAttributeValue(next , "task" , "description" , String.class);
		EObject to = MetaModelManipulator.activitiesTaskMap.get(nextVal);
		
		//transition instance
		EClass transitionClass = (EClass) this.MMEpackage.getEClassifier("Transition"); 
		EAttribute transitionId = (EAttribute)transitionClass.getEStructuralFeature("id"); EAttribute
		transitionName = (EAttribute) transitionClass.getEStructuralFeature("name");
		EReference transitionFrom = (EReference) transitionClass.getEStructuralFeature("from"); 
		EReference transitionTo = (EReference) transitionClass.getEStructuralFeature("to"); 
		
		EObject transitionInstance = this.MMEpackage.getEFactoryInstance().create(transitionClass);
		transitionInstance.eSet(transitionId, Integer.valueOf((int) Math.random()));
		transitionInstance.eSet(transitionName, currId+"-"+nextId);
		transitionInstance.eSet(transitionFrom, from);
		transitionInstance.eSet(transitionTo, to);
		
		return transitionInstance  ; 
	}
	
	private void processAcceptanceCriteria(EClass userStoryClass , DynamicEObjectImpl userStoryRef) {
		

		EReference acceptanceCriteriasRef = (EReference)userStoryClass.getEStructuralFeature("acceptanceCriteria");
		System.out.println("reference " + acceptanceCriteriasRef);
		EcoreEList<DynamicEObjectImpl> acceptanceCriterias = (EcoreEList)userStoryRef.eGet(acceptanceCriteriasRef);
		
	
		
		for(DynamicEObjectImpl acceptanceCriteriaRef : acceptanceCriterias ) {
			EClass acceptanceCriteriaClass = acceptanceCriteriaRef.eClass();
		
			EAttribute when = (EAttribute)acceptanceCriteriaClass.getEStructuralFeature("when");
			EAttribute given = (EAttribute)acceptanceCriteriaClass.getEStructuralFeature("given");
			EAttribute then = (EAttribute)acceptanceCriteriaClass.getEStructuralFeature("then");

			
			System.out.println("Acceptance Criteria : -------------------------------------------------------");
			System.out.println("	given : "+acceptanceCriteriaRef.eGet(given));
			System.out.println("	when : "+acceptanceCriteriaRef.eGet(when));
			System.out.println("	then : "+acceptanceCriteriaRef.eGet(then));
			
		}
	}
	
	private EAttribute getAttByName(EObject objet , String name) {
		EClass eClass = objet.eClass() ; 
		for(Iterator<EAttribute> iter = eClass.getEAllAttributes().iterator() ; iter.hasNext() ;) {
			EAttribute attribute = iter.next();
			if(attribute.getName().equalsIgnoreCase(name)) {
				return attribute ; 
			}
		}
		return null ; 
	}
	
	
	private <T> T getEclassAttributeValue(DynamicEObjectImpl rootRef, String refName, String attrName, Class<T> valueType) {
	    EClass rootClass = rootRef.eClass();
	    EReference objectRef = (EReference) rootClass.getEStructuralFeature(refName);
	    DynamicEObjectImpl objectRefInstance = (DynamicEObjectImpl) rootRef.eGet(objectRef);
	    EAttribute attr = (EAttribute) getAttByName(objectRefInstance, attrName);
	    return valueType.cast(objectRefInstance.eGet(attr));
	}
	
	
	
	
	public static void main(String atgs[]) {
		//load source meta model
		MetaModelManipulator metaModelSource = new MetaModelManipulator(); 
		metaModelSource.loadMetaModel("metaModel/user_story.ecore" , "USER STORY");
		
		//load destination meta model
		MetaModelManipulator metaModelDestination = new MetaModelManipulator(); 
		metaModelDestination.loadMetaModel("metaModel/bpmn.ecore" ,  "BPMN");
		
    	try {
    		
    		//load source model
			DynamicEObjectImpl modelSource =  metaModelSource.loadModel("model/user_story.model");
			
			//transformation
			metaModelDestination.userStory2BPM(modelSource , "model/bpmn.model");			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }


}
