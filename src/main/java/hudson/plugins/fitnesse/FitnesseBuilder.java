package hudson.plugins.fitnesse;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.ModelObject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Execute fitnesse tests, either by starting a new fitnesse instance
 * or by using a fitnesse instance running elsewhere.
 *  
 * @author Tim Bacon
 */
public class FitnesseBuilder extends Builder {

	public static final String START_FITNESSE = "fitnesseStart";
	public static final String FITNESSE_HOST = "fitnesseHost";
	public static final String FITNESSE_PORT = "fitnessePort";
	public static final String FITNESSE_PORT_REMOTE = "fitnessePortRemote";
	public static final String FITNESSE_PORT_LOCAL = "fitnessePortLocal";
	public static final String JAVA_OPTS = "fitnesseJavaOpts";
	public static final String PATH_TO_JAR = "fitnessePathToJar";
	public static final String PATH_TO_ROOT = "fitnessePathToRoot";
	public static final String TARGET_PAGE = "fitnesseTargetPage";
	public static final String TARGET_IS_SUITE = "fitnesseTargetIsSuite";
	public static final String PATH_TO_RESULTS = "fitnessePathToXmlResultsOut";
	
	private final Map<String, String> options;

    @DataBoundConstructor
	public FitnesseBuilder(Map<String, String> options) {
    	// Use n,v pairs to ease future extension 
    	this.options = options;
    }

    private String getOption(String key, String valueIfKeyNotFound) {
    	if (options.containsKey(key)) {
    		String value = options.get(key);
    		if (value!=null && value != "") return value; 
    	}
    	return valueIfKeyNotFound;
    }
    
    /**
     * referenced in config.jelly
     */
	public boolean getFitnesseStart() {
    	return Boolean.parseBoolean(getOption(START_FITNESSE, "False"));
    }

	/**
	 * referenced in config.jelly
	 */
    public String getFitnesseHost() {
    	return getOption(FITNESSE_HOST, "unknown_host");
    }
    
    /**
     * referenced in config.jelly
     */
    public String getFitnesseJavaOpts() {
    	return getOption(JAVA_OPTS, "");
    }
    
    /**
     * referenced in config.jelly
     */
    public int getFitnessePort() {
    	return Integer.parseInt(
			getOption(FITNESSE_PORT_REMOTE, 
				getOption(FITNESSE_PORT_LOCAL, 
					getOption(FITNESSE_PORT, "-1"))));
    }

    /**
     * referenced in config.jelly
     */
    public String getFitnessePathToJar() {
		return getOption(PATH_TO_JAR, "");
	}

    /**
     * referenced in config.jelly
     */
    public String getFitnessePathToRoot() {
    	return getOption(PATH_TO_ROOT, "");
    }

    /**
     * referenced in config.jelly
     */
	public String getFitnesseTargetPage() {
		return getOption(TARGET_PAGE, "");
    }

	/**
	 * referenced in config.jelly
	 */
    public boolean getFitnesseTargetIsSuite() {
    	return Boolean.parseBoolean(getOption(TARGET_IS_SUITE, "False"));
    }
    
    /**
     * referenced in config.jelly
     */
    public String getFitnessePathToXmlResultsOut() {
    	return getOption(PATH_TO_RESULTS, "");
    }

    /**
     * {@link Builder}
     */
    @Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) 
    throws InterruptedException {
		return new FitnesseExecutor(this).execute(build, launcher, listener);
	}

    /**
     * {@link Builder}
     */
	@Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }
	
    /**
     *  See <tt>src/main/resources/hudson/plugins/fitnesse/FitnesseBuilder/config.jelly</tt>
     */
    @Extension 
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
    	
    	public FormValidation doCheckFitnesseHost(@QueryParameter String value) throws IOException, ServletException {
    		if (value.length()==0)
    			return FormValidation.error("Please specify the host of the fitnesse instance.");
    		return FormValidation.ok();
    	}
    	
    	public FormValidation doCheckFitnessePort(@QueryParameter String value) throws IOException, ServletException {
    		if (value.length()==0)
    			return FormValidation.error("Please specify the fitnesse port.");
    		try {
    			int intValue = Integer.parseInt(value);
    			if (intValue < 1)
    				return FormValidation.error("Port must be a positive integer.");
    		} catch (NumberFormatException e) {
    			return FormValidation.error("Port must be a number.");
    		}
    		return FormValidation.ok();
    	}
    	
    	public FormValidation doCheckFitnesseJavaOpts(@QueryParameter String value) throws IOException, ServletException {
        	return FormValidation.ok();
        }

    	public FormValidation doCheckFitnessePathToJar(@QueryParameter String value) throws IOException, ServletException {
    		if (value.length()==0)
    			return FormValidation.error("Please specify the path to 'fitnesse.jar'.");
    		if (! new File(value).exists())
    			return FormValidation.warning("Path does not exist.");
    		if (!value.endsWith("fitnesse.jar")
    				&& new File(value, "fitnesse.jar").exists())
    			return FormValidation.warning("Path does not end with 'fitnesse.jar': is that correct?");
    		return FormValidation.ok();
    	}

        public FormValidation doCheckFitnessePathToRoot(@QueryParameter String value) throws IOException, ServletException {
            if (value.length()==0)
                return FormValidation.error("Please specify the location of 'FitNesseRoot'.");
            if (! new File(value).exists())
            	return FormValidation.warning("Path does not exist.");
            if (!value.endsWith("FitNesseRoot")
            && new File(value, "FitNesseRoot").exists())
            	return FormValidation.warning("Path does not end with 'FitNesseRoot': is that correct?");
            return FormValidation.ok();
        }

        public FormValidation doCheckFitnesseTargetPage(@QueryParameter String value) throws IOException, ServletException {
        	if (value.length()==0)
        		return FormValidation.error("Please specify a page to execute.");
        	return FormValidation.ok();
        }

        public FormValidation doCheckFitnesseTargetIsSuite(@QueryParameter String value) throws IOException, ServletException {
            return FormValidation.ok();
        }

        public FormValidation doCheckFitnessePathToXmlResultsOut(@QueryParameter String value) throws IOException, ServletException {
        	if (value.length()==0)
        		return FormValidation.error("Please specify where to write fitnesse results to.");
        	if (! new File(value).exists()
    		|| ! new File(value).getParentFile().exists())
        		return FormValidation.warning("Path does not exist.");
        	if (!value.endsWith("xml"))
        		return FormValidation.warning("Location does not end with 'xml': is that correct?");
        	return FormValidation.ok();
        }

        /**
         * {@link BuildStepDescriptor}
         */
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // indicates that this builder can be used with all kinds of project types 
            return true;
        }

        /**
         * {@link ModelObject} 
         */
        @Override
        public String getDisplayName() {
            return "Execute fitnesse tests";
        }

        /**
         * {@link Descriptor}
         * config.jelly uses hide-able fields so take control of instance creation
         */
		@Override
		public FitnesseBuilder newInstance(StaplerRequest req, JSONObject formData)
				throws FormException {
			return new FitnesseBuilder(collectFormData(req, new String[] {
				START_FITNESSE, FITNESSE_HOST, 
				FITNESSE_PORT, FITNESSE_PORT_REMOTE, FITNESSE_PORT_LOCAL,
				JAVA_OPTS, PATH_TO_JAR, PATH_TO_ROOT, 
				TARGET_PAGE, TARGET_IS_SUITE, PATH_TO_RESULTS
				})
			);
		}

		private Map<String, String> collectFormData(StaplerRequest req, String[] keys) {
			Map<String, String> formData = new HashMap<String, String>();
			for (int i=0; i < keys.length; ++i) {
				if (keys[i] == TARGET_IS_SUITE) {
					// WTF? checkbox parm comes through as "on" or null
					formData.put(keys[i], decodeCheckboxParm(req.getParameter(keys[i])));
				} else {
					formData.put(keys[i], req.getParameter(keys[i]));
				}
			}
			return formData;
		}

		private String decodeCheckboxParm(String value) {
			if (value == null || value == "") return Boolean.FALSE.toString();
			if (value.equalsIgnoreCase(Boolean.FALSE.toString())) return value;  
			return Boolean.TRUE.toString();
		}
    }    
}

