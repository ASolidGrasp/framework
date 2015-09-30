package com.formation.configreader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.formation.annotations.Action;
import com.formation.annotations.ActionForm;
import com.formation.exceptions.runtime.FileNotFoundException;
import com.formation.exceptions.runtime.NoConfigurationProvidedException;
import com.formation.exceptions.runtime.OldConfigurationFileFoundWhileUsingAnnotationsException;
import com.formation.exceptions.runtime.WrongPackageNamesInPackagesToScanException;
import com.formation.exceptions.runtime.XMLParsingException;

/**
 * Classe dont le rôle est d'identifier les Action et ActionForm déclarés par
 * l'utilisateur et de les coupler.
 * @author filippo
 */
public final class ConfigurationReader
{
    /**
     * L'extension des fichiers class qui vont être lus à la recherche
     * d'annotations.
     */
    private final String classFileExtension = ".class";
    /**
     * Name of the xml configuration file where you have to specify each Action
     * and ActionForm individually.
     */
    private String oldConfigurationFileName;
    /**
     * Name of the xml configuration file where you just need to give a package
     * to scan for the framework to find the @Action and @Actionform
     * annotations.
     */
    private String newConfigurationFileName;
    /**
     * Instance de la classe initialisée dès le chargement de la classe et
     * renvoyée par la classe à chaque demande d'instanciation pour assurer son
     * unicité. Design Pattern Singleton.
     */
    private static ConfigurationReader readerInstance = new ConfigurationReader();
    /**
     * Logger pour afficher les exceptions non passées.
     */
    private Logger logger = Logger.getLogger(this.getClass());

    /**
     * Constructeur privé qu'il faut définir pour qu'il ne soit redéfini
     * automatiquement. N'étant pas accessibles on ne peut s'en servir pour
     * instancier des objets de la classe.
     */
    private ConfigurationReader()
    {
        InputStream iS = Thread.currentThread().getContextClassLoader().getResourceAsStream("configurationFiles.properties");
        Properties properties = new Properties();
        try
        {
            properties.load(iS);
        }
        catch (IOException e)
        {
            logger.error("The configurationFiles.properties file has not been found");
        }
        oldConfigurationFileName = properties.getProperty("old");
        newConfigurationFileName = properties.getProperty("new");
    }

    /**
     * Point d'accès de la classe pour récupérer son instance unique. Pour être
     * accessible avant même son instantiation il doit être static.
     * @return L'instance unique de la classe.
     */
    public static ConfigurationReader getReader()
    {
        return readerInstance;
    }

    /**
     * Renvoie les noms canoniques de l'Action et ActionForm dont l'url-pattern
     * correspond à l'url demandée.
     * @param actionsAndFormsMap
     *        Une map qui contient en clef les url-pattern et en valeur les
     *        couple Action et ActionForm associés
     * @param urlPathInfo
     *        La terminaison d'url demandée correspodant à l'action demandée.
     * @return Les noms canoniques de l'Action et ActionForm demandés sous forme
     *         de String[]
     */
    public String[] getActionAndFormCanonicalNamesByAction(Map<String, String[]> actionsAndFormsMap, String urlPathInfo)
    {
        for (Entry<String, String[]> e : actionsAndFormsMap.entrySet())
        {
            Pattern p = Pattern.compile(e.getKey());
            Matcher m = p.matcher(urlPathInfo);
            if (m.matches())
            {
                return e.getValue();
            }
        }
        return null;
    }

    /**
     * Construit une map qui contient en clef les url-pattern et en valeur les
     * couple Action et ActionForm associés. On cherche d'abord les annotations
     * pour construire la configuration puis si celles-ci ne sont pas trouvées
     * on se rabat sur le fichier de configuration où les classes sont déclarées
     * individuellement.
     * @return la susdite map.
     */
    public Map<String, String[]> buildActionsAndFormsMap()
    {
        // si présence d'annotations tout doit être fait en annotations et le
        // fichier ne doit pas exister
        // Pour chaque package-scan
        Map<String, String[]> buildActionsAndFormsMapFromAnnotations = buildActionsAndFormsMapFromAnnotations();
        boolean annotationFound = (!buildActionsAndFormsMapFromAnnotations.isEmpty());
        if (annotationFound)
        {
            if (!oldConfigFileFound())
            {
                return buildActionsAndFormsMapFromAnnotations;
            }
            else
            {
                throw new OldConfigurationFileFoundWhileUsingAnnotationsException("In order to avoid confusion with the annotations you're using please remove your old-fashioned xml configuration file named " + oldConfigurationFileName + " located in src/main/resources");
            }
        }
        else
        {
            // si aucune annotation : toute la config est chargée depuis le
            // fichier s'il est trouvé
            if (oldConfigFileFound())
            {
                return buildActionsAndFormsMapFromFile();
            }
            else
            {
                throw new NoConfigurationProvidedException("For the framework to know your Actions and ActionForms you have whether to annotate them whit the @Action @ActionForm annotation and to declare their package in the new-fashioned configuration file or to declare them individually in the old-fashioned configuration file. Neither has been found.");
            }
        }
    }

    /**
     * Méthode qui va chercher dans l'application hôte les classes Action et
     * ActionForm et les mettre dans une map.
     * @return Une Map<String, String[]> avec :
     *         <ul>
     *         <li>Pour clef : l'url-pattern associé à l'action</li>
     *         <li>Pour valeur : le couple des noms canoniques de Action et
     *         ActionForm</li>
     *         </ul>
     */
    private Map<String, String[]> buildActionsAndFormsMapFromAnnotations()
    {
        Map<String, String[]> actionsMap = new LinkedHashMap<String, String[]>();
        Map<String, String> formsMap = new LinkedHashMap<String, String>();

        ArrayList<String> packagesToScan = packagesToScan();
        for (String packageName : packagesToScan)
        {
            Iterable<Class<?>> classesHote;
            try
            {
                classesHote = getClasses(packageName);
            }
            catch (ClassNotFoundException | IOException e)
            {
                // usage exceptionnel d'exceptions groupées A|B car les deux
                // types d'exceptions viennent d'un même problème : mauvais nom
                // de package
                throw new WrongPackageNamesInPackagesToScanException("At least one package to scan for @Action or @ActionForm annotations does not exist. Please check your " + newConfigurationFileName + " configuration file." + "\n" + e.getMessage());
            }
            for (Class<?> aClass : classesHote)
            {
                actionsMap = addClassToActionsMapIfCompliant(aClass, actionsMap);
                formsMap = addClassToActionFormsMapIfCompliant(aClass, formsMap);
            }
        }
        return mergeMaps(actionsMap, formsMap);
    }

    /**
     * Fusionne la map contenant les informations relevées sur les actions avec
     * la map contenant les informations relevées sur les actionForms pour
     * proposer une map unique avec
     * <ul>
     * <li>Pour clef : l'url-pattern associé à l'action. C'est pratique pour
     * pouvoir obtenir directement la valeur sur un getKey à un match de pattern
     * près.</li>
     * <li>Pour valeur : les canonical names de Action et de l'ActionForm
     * associé.</li>
     * </ul>
     * @param actionsMap
     *        La map contenant les informations relevées sur les actions
     * @param formsMap
     *        La map contenant les informations relevées sur les actionForms
     * @return La map détaillée en description.
     */
    private Map<String, String[]> mergeMaps(Map<String, String[]> actionsMap, Map<String, String> formsMap)
    {
        Map<String, String[]> actionsAndFormsMap = new LinkedHashMap<String, String[]>();
        // on assemble les deux maps en une seule
        for (Entry<String, String[]> actionsMapEntry : actionsMap.entrySet())
        {
            actionsAndFormsMap.put(actionsMapEntry.getValue()[1], new String[]
            {
                    actionsMapEntry.getValue()[0],
                    formsMap.get(actionsMapEntry.getKey())
            });
        }
        return actionsAndFormsMap;
    }

    /**
     * Si la classe a une annotation @Action et qu'elle implémente l'interface
     * Action alors elle est ajoutée à la map des Actions.
     * @param aClass
     *        La classe dont on cherche à savoir si elle est annotée @Action et
     *        si elle implémente l'interface Action.
     * @param actionsMap
     *        La map des Actions relevées dans l'application hôte.
     * @return Une map Map<String, String[]> avec :
     *         <ul>
     *         <li>pour clef : le nom du formulaire associé</li>
     *         <li>pour valeur : un couple constitué du nom canonique de la
     *         classe Action et de son url-pattern</li>
     *         </ul>
     */
    private Map<String, String[]> addClassToActionsMapIfCompliant(Class<?> aClass, Map<String, String[]> actionsMap)
    {
        if (aClass.isAnnotationPresent(Action.class) && doesImplement(aClass, com.formation.archetypes.Action.class))
        {
            Action actionAnnotation = (Action) aClass.getAnnotation(Action.class);
            actionsMap.put(actionAnnotation.formName(), new String[]
            {
                    aClass.getCanonicalName(), actionAnnotation.urlPattern()
            });
        }
        return actionsMap;
    }

    /**
     * Si la classe a une annotation @ActionForm et qu'elle hérite de la classe
     * abstraite ActionForm alors elle est ajoutée à la map des ActionForms.
     * @param aClass
     *        La classe dont on cherche à savoir si elle est annotée @ActionForm
     *        et si elle hérite de la classe abstraite ActionForm.
     * @param formsMap
     *        La map des ActionForms relevés dans l'application hôte.
     * @return Une map Map<String, String[]> avec :
     *         <ul>
     *         <li>pour clef : le nom du formulaire</li>
     *         <li>pour valeur : le nom canonique de la classe ActionForm</li>
     *         </ul>
     */
    private Map<String, String> addClassToActionFormsMapIfCompliant(Class<?> aClass, Map<String, String> formsMap)
    {
        if (aClass.isAnnotationPresent(ActionForm.class) &&  com.formation.archetypes.ActionForm.class.isAssignableFrom(aClass))
        {
            ActionForm actionFormAnnotation = (ActionForm) aClass.getAnnotation(ActionForm.class);
            formsMap.put(actionFormAnnotation.name(), aClass.getCanonicalName());
        }
        return formsMap;
    }

    /**
     * Mehode qui verifie la présence du fichier de configuration vieux-style ou
     * les action et actionForm doivent être déclarés individuellement.
     * @return Vrai si le fichier est trouvé.
     */
    private boolean oldConfigFileFound()
    {
        InputStream oldIS = Thread.currentThread().getContextClassLoader().getResourceAsStream("/" + oldConfigurationFileName);
        if (oldIS != null)
        {
            return true;
        }
        return false;
    }

    /**
     * Renvoie la liste des packages à scanner spécifiés dans le fichier de
     * configuration.
     * @return ArrayList des packages à scanner.
     */
    private ArrayList<String> packagesToScan()
    {
        ArrayList<String> packagesToScan = new ArrayList<String>();
        DocumentBuilderFactory dBF = DocumentBuilderFactory.newInstance();
        try
        {
            DocumentBuilder dB = dBF.newDocumentBuilder();
            InputStream iS = Thread.currentThread().getContextClassLoader().getResourceAsStream("/" + newConfigurationFileName);
            Document document = dB.parse(iS);
            packagesToScan = getPackagesToScanInDocument(document);
        }
        catch (SAXException e)
        {
            throw new XMLParsingException("The framework xml configuration file is not properly formed and cannot be parsed. Please check its structure. It name is " + oldConfigurationFileName + " and is placed in your src/main/resources folder" + "\n" + e.getMessage());
        }
        catch (IOException e)
        {
            throw new FileNotFoundException("The configuration file hasn't been found. It must be named " + oldConfigurationFileName + " and be placed in your src/main/resources folder." + "\n" + e.getMessage());
        }
        catch (ParserConfigurationException e)
        {
            logger.error(e.getMessage());
        }
        return packagesToScan;
    }

    /**
     * Crée la liste des packages à scanner spécifiés dans le fichier de
     * configuration à partir dun document XML chargé en mémoire.
     * @param document
     *        Document XML chargé en mémoire correspondant au fichier de
     *        configuration des packages à scanner pour trouver Action et
     *        ActionForm.
     * @return Une ArrayList<String> contenant tous les packages à scanner.
     */
    private ArrayList<String> getPackagesToScanInDocument(Document document)
    {
        ArrayList<String> packagesToScan = new ArrayList<String>();
        Element rootElement = document.getDocumentElement();
        NodeList packagesToScanElements = rootElement.getChildNodes();
        for (int i = 0; i < packagesToScanElements.getLength(); i++)
        {
            if (packagesToScanElements.item(i).getNodeType() == Node.ELEMENT_NODE)
            {
                String packageName = packagesToScanElements.item(i).getTextContent();
                packagesToScan.add(packageName);
            }
        }
        return packagesToScan;
    }

    /**
     * Renvoie toutes les classes d'un package donné grâce à sa méthode appelée
     * findClasses.
     * @param packageName
     *        Le nom du package donné.
     * @return L'ensemble des classes trouvées dans un package donné, en
     *         cherchant dans les sous packages.
     * @throws IOException
     *         Renvoyée si le chemin correspondant au package n'est pas trouvé.
     * @throws ClassNotFoundException
     *         Renvoyée si les classes trouvées ne peuvent être chargées en
     *         mémoire.
     */
    private Iterable<Class<?>> getClasses(String packageName) throws IOException, ClassNotFoundException
    {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        String path = packageName.replace('.', '/');
        Enumeration<URL> resources = classLoader.getResources(path);
        List<File> dirs = new ArrayList<File>();
        while (resources.hasMoreElements())
        {
            URL resource = resources.nextElement();
            dirs.add(new File(resource.getFile()));
        }
        List<Class<?>> classes = new ArrayList<Class<?>>();
        for (File directory : dirs)
        {
            classes.addAll(findClasses(directory, packageName));
        }
        return classes;
    }

    /**
     * Renvoie toutes les classes d'un package donné.
     * @param directory
     *        Le dossier associé au package
     * @param packageName
     *        Le nom du package
     * @return La liste des classes du package.
     * @throws ClassNotFoundException
     *         Quand la classe trouvée ne peut être chargée en mémoire.
     */
    private List<Class<?>> findClasses(File directory, String packageName) throws ClassNotFoundException
    {
        List<Class<?>> classes = new ArrayList<Class<?>>();
        if (!directory.exists())
        {
            return classes;
        }
        File[] files = directory.listFiles();
        return classesInFiles(files, packageName);
    }

    /**
     * Renvoie l'ensemble des classes contenues dans un ensemble de dossiers et
     * fichiers.
     * @param files
     *        Un tableau de dossiers et fichiers.
     * @param packageName
     *        Le nom du package qui les contient.
     * @return Une List<Class> des classes trouvées.
     * @throws ClassNotFoundException
     *         Quand la classe trouvée ne peut être chargée en mémoire.
     */
    private List<Class<?>> classesInFiles(File[] files, String packageName) throws ClassNotFoundException
    {
        List<Class<?>> classes = new ArrayList<Class<?>>();
        for (File file : files)
        {
            if (file.isDirectory())
            {
                classes.addAll(findClasses(file, packageName + "." + file.getName()));
            }
            else if (file.getName().endsWith(".class"))
            {
                classes.add(Class.forName(packageName + '.' + file.getName().substring(0, file.getName().length() - classFileExtension.length())));
            }
        }
        return classes;
    }

    /**
     * Construit, en lisant le fichier de configuration old-style, une map qui
     * contient en clef les url-pattern et en valeur les couple Action et
     * ActionForm associés.
     * @return La map demandée.
     */
    private Map<String, String[]> buildActionsAndFormsMapFromFile()
    {
        DocumentBuilderFactory dBF = DocumentBuilderFactory.newInstance();
        Map<String, String[]> actionMap = new LinkedHashMap<String, String[]>();
        Map<String, String> formMap = new LinkedHashMap<String, String>();
        try
        {
            DocumentBuilder dB = dBF.newDocumentBuilder();
            InputStream iS = Thread.currentThread().getContextClassLoader().getResourceAsStream("/" + oldConfigurationFileName);

            Document document = dB.parse(iS);
            actionMap = getActionMapFromDocument(document);
            formMap = getActionFormMapFromDocument(document);
        }
        catch (SAXException e)
        {
            throw new XMLParsingException("The framework configuration file is not properly formed. Please check its structure. It name is " + oldConfigurationFileName + " and is placed in your src/main/resources folder" + "\n" + e.getMessage());
        }
        catch (IOException e)
        {
            throw new FileNotFoundException("The configuration file hasn't been found. It must be named " + oldConfigurationFileName + " and be placed in your src/main/resources folder." + "\n" + e.getMessage());
        }
        catch (ParserConfigurationException e)
        {
            logger.error(e.getMessage());
        }

        return mergeMaps(actionMap, formMap);

    }

    /**
     * Renvoie la map des Action trouvées dans le document XML chargé en mémoire
     * passé en argument.
     * @param document
     *        Le fichier XML de configuration chargé en mémoire.
     * @return Une Map<String, String[]> avec :
     *         <ul>
     *         <li>en clef : le petit nom de l'ActionForm associé à l'Action</li>
     *         <li>en valeur : un tableau de deux chaînes avec :
     *         <ul>
     *         <li>le nom canonique de la classe</li>
     *         <li>l'url-pattern de l'action</li>
     *         </ul>
     *         </li>
     *         </ul>
     */
    private Map<String, String[]> getActionMapFromDocument(Document document)
    {
        Map<String, String[]> actionMap = new LinkedHashMap<String, String[]>();

        Element rootElement = document.getDocumentElement();
        Node actionsElement = rootElement.getElementsByTagName("actions").item(0);
        NodeList actionElements = actionsElement.getChildNodes();
        // je parcours les noeuds action
        for (int i = 0; i < actionElements.getLength(); i++)
        {
            if (actionElements.item(i).getNodeType() == Node.ELEMENT_NODE)
            {
                Node currentActionElement = actionElements.item(i);
                String[] actionNodeProperties = getActionNodeProperties(currentActionElement);
                actionMap.put(actionNodeProperties[2], new String[]
                {
                        actionNodeProperties[1], actionNodeProperties[0]
                });
            }
        }
        return actionMap;
    }

    /**
     * Renvoie la map des ActionForm trouvés dans le document XML chargé en
     * mémoire passé en argument.
     * @param document
     *        Le fichier XML de configuration chargé en mémoire.
     * @return Une Map<String, String[]> avec :
     *         <ul>
     *         <li>en clef : le petit nom de l'ActionForm associé à l'Action</li>
     *         <li>en valeur : le nom canonique de la classe de l'ActionForm</li>
     *         </ul>
     */
    private Map<String, String> getActionFormMapFromDocument(Document document)
    {
        Map<String, String> formMap = new LinkedHashMap<String, String>();
        Element rootElement = document.getDocumentElement();
        Node formsElement = rootElement.getElementsByTagName("forms").item(0);
        NodeList formElements = formsElement.getChildNodes();
        // je parcours les noeuds form
        for (int i = 0; i < formElements.getLength(); i++)
        {
            if (formElements.item(i).getNodeType() == Node.ELEMENT_NODE)
            {
                Node currentFormElement = formElements.item(i);
                String[] formNodeProperties = getFormNodeProperties(currentFormElement);
                formMap.put(formNodeProperties[0], formNodeProperties[1]);
            }
        }
        return formMap;
    }

    /**
     * Récupère dans un noeud action du fichier de configuration les détails de
     * ce noeud.
     * @param actionNode
     *        Le noeud action dont on doit extraire les informations.
     * @return Un tableau de 3 chaînes avec :
     *         <ul>
     *         <li>l'url-pattern de l'action</li>
     *         <li>le nom canonique de la classe</li>
     *         <li>le petit nom de l'ActionForm associé</li>
     *         </ul>
     */
    private String[] getActionNodeProperties(Node actionNode)
    {
        NodeList currentActionElementChildren = actionNode.getChildNodes();
        Node actionURLPattern = null;
        Node actionClassPath = null;
        Node formName = null;
        // je parcours le propriétés des noeuds action
        for (int j = 0; j < currentActionElementChildren.getLength(); j++)
        {
            Node currentActionPropertyNode = currentActionElementChildren.item(j);
            short nodeType = currentActionPropertyNode.getNodeType();
            String nodeName = currentActionPropertyNode.getNodeName();
            if (nodeType == Node.ELEMENT_NODE)
            {
                if (nodeName.equals("url-pattern"))
                {
                    actionURLPattern = currentActionPropertyNode;
                }
                else if (nodeName.equals("action-class"))
                {
                    actionClassPath = currentActionPropertyNode;
                }
                else if (nodeName.equals("form-name"))
                {
                    formName = currentActionPropertyNode;
                }
                else
                {
                    logger.debug("this case should never happen if your xml configuration file respects the appropriate schema");
                }
            }
        }
        return new String[]
        {
                actionURLPattern.getTextContent(),
                actionClassPath.getTextContent(),
                formName.getTextContent()
        };
    }

    /**
     * Récupère dans un noeud action du fichier de configuration les détails de
     * ce noeud.
     * @param formNode
     *        Le noeud action dont on doit extraire les informations.
     * @return Un tableau de 2 chaînes avec :
     *         <ul>
     *         <li>le petit nom de l'ActionForm</li>
     *         <li>le nom canonique de la classe de l'ActionForm</li>
     *         </ul>
     */
    private String[] getFormNodeProperties(Node formNode)
    {
        NodeList currentFormElementChildren = formNode.getChildNodes();
        Node formName = null;
        Node formClassPath = null;
        // je parcours les propriétés des noeuds form
        for (int j = 0; j < currentFormElementChildren.getLength(); j++)
        {
            Node currentNode = currentFormElementChildren.item(j);
            short nodeType = currentNode.getNodeType();
            String nodeName = currentNode.getNodeName();
            if (nodeType == Node.ELEMENT_NODE)
            {
                if (nodeName.equals("form-name"))
                {
                    formName = currentNode;
                }
                else if (nodeName.equals("form-class"))
                {
                    formClassPath = currentNode;
                }
                else
                {
                    logger.debug("this case should never happen if your xml configuration file respects the appropriate schema");
                }
            }
        }
        return new String[]
        {
                formName.getTextContent(), formClassPath.getTextContent()
        };
    }

    /**
     * Vérifie que la classe donnée implémente l'interface donnée.
     * @param aClass
     *        Une classe dont on veut vérifier qu'elle implémente une interface.
     * @param anInterface
     *        L'interface dont on veut s'assurer qu'elle est implémentée par la
     *        classe passée.
     * @return true si la classe implémente l'interface, false autrement.
     */
    private boolean doesImplement(Class aClass, Class anInterface)
    {
        for (int i = 0; i < aClass.getInterfaces().length; i++)
        {
            if (aClass.getInterfaces()[i].equals(anInterface))
            {
                return true;
            }
        }
        return false;
    }
}
