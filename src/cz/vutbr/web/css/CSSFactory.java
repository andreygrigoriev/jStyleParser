package cz.vutbr.web.css;

import java.lang.reflect.Method;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import cz.vutbr.web.csskit.antlr.CSSInputStream;
import cz.vutbr.web.csskit.antlr.CSSTreeParser;
import cz.vutbr.web.domassign.Analyzer;

/**
 * This class is abstract factory for other factories used during CSS parsing.
 * Use it, for example, to retrieve current(default) TermFactory,
 * current(default) SupportedCSS implementation and so on.
 * 
 * Factories need to be registered first. This can be done using Java static
 * block initialization together with Java classloader.
 * 
 * By default, factory searches automatically for implementations:
 * <code>cz.vutbr.web.csskit.TermFactoryImpl</code>
 * <code>cz.vutbr.web.domassign.SupportedCSS21</code>
 * <code>cz.vutbr.web.csskit.RuleFactoryImpl</code>
 * <code>cz.vutbr.web.domassign.SingleMapNodeData</code>
 * 
 * Example:
 * 
 * <pre>
 * public class TermFactoryImpl implemenent TermFactory {
 * 		static {
 * 			CSSFactory.registerTermFactory(new TermFactoryImpl());
 * 		}
 * 		...
 * }
 * 
 * That, default factory is set when this class is loaded by class loader.
 * 
 * <pre>
 * Class.forName(&quot;xx.package.TermFactoryImpl&quot;)
 * </pre>
 * 
 * </pre>
 * 
 * @author kapy
 * 
 */
public final class CSSFactory {
	private static Logger log = LoggerFactory.getLogger(CSSFactory.class);

	private static final String DEFAULT_TERM_FACTORY = "cz.vutbr.web.csskit.TermFactoryImpl";
	private static final String DEFAULT_SUPPORTED_CSS = "cz.vutbr.web.domassign.SupportedCSS21";
	private static final String DEFAULT_RULE_FACTORY = "cz.vutbr.web.csskit.RuleFactoryImpl";
	private static final String DEFAULT_NODE_DATA_IMPL = "cz.vutbr.web.domassign.SingleMapNodeData";

	/**
	 * Default instance of TermFactory implementation
	 */
	private static TermFactory tf;

	/**
	 * Default instance of SupportedCSS implementation
	 */
	private static SupportedCSS css;

	/**
	 * Default instance of RuleFactory implementation
	 */
	private static RuleFactory rf;

	private static Class<? extends NodeData> ndImpl;

	/**
	 * Registers new TermFactory instance
	 * 
	 * @param newFactory
	 *            New TermFactory
	 */
	public static final void registerTermFactory(TermFactory newFactory) {
		tf = newFactory;
	}

	/**
	 * Returns TermFactory registered in step above
	 * 
	 * @return TermFactory registered
	 */
	@SuppressWarnings("unchecked")
	public static final TermFactory getTermFactory() {
		if (tf == null) {
			try {
				Class<? extends TermFactory> clazz = (Class<? extends TermFactory>) Class
						.forName(DEFAULT_TERM_FACTORY);
				Method m = clazz.getMethod("getInstance");
				registerTermFactory((TermFactory) m.invoke(null));
				log.debug("Retrived {} as default TermFactory implementation.",
						DEFAULT_TERM_FACTORY);
			} catch (Exception e) {
				log.error("Unable to get TermFactory from default", e);
				throw new RuntimeException(
						"No TermFactory implementation registered!");
			}
		}
		return tf;
	}

	public static final void registerSupportedCSS(SupportedCSS newCSS) {
		css = newCSS;
	}

	@SuppressWarnings("unchecked")
	public static final SupportedCSS getSupportedCSS() {
		if (css == null) {
			try {
				Class<? extends SupportedCSS> clazz = (Class<? extends SupportedCSS>) Class
						.forName(DEFAULT_SUPPORTED_CSS);
				Method m = clazz.getMethod("getInstance");
				registerSupportedCSS((SupportedCSS) m.invoke(null));
				log.debug(
						"Retrived {} as default SupportedCSS implementation.",
						DEFAULT_SUPPORTED_CSS);
			} catch (Exception e) {
				log.error("Unable to get SupportedCSS from default", e);
				throw new RuntimeException(
						"No SupportedCSS implementation registered!");
			}
		}
		return css;
	}

	public static final void registerRuleFactory(RuleFactory newRuleFactory) {
		rf = newRuleFactory;
	}

	@SuppressWarnings("unchecked")
	public static final RuleFactory getRuleFactory() {
		if (rf == null) {
			try {
				Class<? extends RuleFactory> clazz = (Class<? extends RuleFactory>) Class
						.forName(DEFAULT_RULE_FACTORY);
				Method m = clazz.getMethod("getInstance");
				registerRuleFactory((RuleFactory) m.invoke(null));
				log.debug("Retrived {} as default RuleFactory implementation.",
						DEFAULT_RULE_FACTORY);
			} catch (Exception e) {
				log.error("Unable to get RuleFactory from default", e);
				throw new RuntimeException(
						"No RuleFactory implementation registered!");
			}
		}

		return rf;
	}

	public static final void registerNodeDataInstance(
			Class<? extends NodeData> clazz) {
		try {
			@SuppressWarnings("unused")
			NodeData test = clazz.newInstance();
			ndImpl = clazz;
		} catch (InstantiationException e) {
			throw new RuntimeException("NodeData implemenation ("
					+ clazz.getName() + ") doesn't provide sole constructor", e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException("NodeData implementation ("
					+ clazz.getName() + ") is not accesible", e);
		}

	}

	@SuppressWarnings("unchecked")
	public static final NodeData createNodeData() {
		if (ndImpl == null) {
			try {
				registerNodeDataInstance((Class<? extends NodeData>) Class
						.forName(DEFAULT_NODE_DATA_IMPL));
				log.debug("Registered {} as default NodeData instance.",
						DEFAULT_NODE_DATA_IMPL);
			} catch (Exception e) {
			}
		}

		try {
			return ndImpl.newInstance();
		} catch (Exception e) {
			throw new RuntimeException("No NodeData implementation registered");
		}
	}

	public static final StyleSheet parse(String fileName, String encoding) {
		try {
			CSSTreeParser parser = CSSTreeParser
					.createParser(new CSSInputStream(fileName, encoding));
			return parser.stylesheet();

		} catch (Exception e) {
			log.error("While parsing CSS stylesheet", e);
			return getRuleFactory().createStyleSheet();
		}
	}

	public static final StyleSheet parse(String css) {
		try {
			CSSTreeParser parser = CSSTreeParser
					.createParser(new CSSInputStream(css));
			return parser.stylesheet();
		} catch (Exception e) {
			log.error("While parsing CSS stylesheet", e);
			return getRuleFactory().createStyleSheet();
		}
	}

	public static final Map<Element, NodeData> assign(Document doc,
			StyleSheet sheet, String media, boolean useInheritance) {
		
		Analyzer analyzer = new Analyzer(sheet);
		return analyzer.evaluateDOM(doc, media, useInheritance);
	}
	
}
