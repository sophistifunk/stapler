package org.kohsuke.stapler;

import org.kohsuke.MetaInfServices;
import org.kohsuke.stapler.export.ModelBuilder;
import org.zeroturnaround.javarebel.ClassEventListener;
import org.zeroturnaround.javarebel.ReloaderFactory;

import javax.servlet.RequestDispatcher;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import static java.util.logging.Level.*;

/**
 * Adds JRebel reloading support.
 *
 * @author Kohsuke Kawaguchi
 */
@MetaInfServices
public class JRebelFacet extends Facet {
    private final Map<Class,MetaClass> metaClasses = new HashMap<Class, MetaClass>();

    public JRebelFacet() {
        try {
            ReloaderFactory.getInstance().addClassReloadListener(new ClassEventListener() {
                public void onClassEvent(int eventType, Class klass) {
                    synchronized (metaClasses) {
                        for (Entry<Class, MetaClass> e : metaClasses.entrySet()) {
                            if (klass.isAssignableFrom(e.getKey())) {
                                LOGGER.fine("Reloaded Stapler MetaClass for "+e.getKey());
                                e.getValue().buildDispatchers();
                            }
                        }
                    }
                    // purge the model builder cache
                    ResponseImpl.MODEL_BUILDER = new ModelBuilder();
                }

                public int priority() {
                    return PRIORITY_DEFAULT;
                }
            });
        } catch (LinkageError e) {
            LOGGER.log(FINE,"JRebel support failed to load",e);
        }
    }


    @Override
    public void buildViewDispatchers(MetaClass owner, List<Dispatcher> dispatchers) {
        synchronized (metaClasses) {
            metaClasses.put(owner.clazz,owner);
        }
    }

    @Override
    public RequestDispatcher createRequestDispatcher(RequestImpl request, Class type, Object it, String viewName) {
        return null;
    }

    @Override
    public boolean handleIndexRequest(RequestImpl req, ResponseImpl rsp, Object node, MetaClass nodeMetaClass) {
        return false;
    }

    private static final Logger LOGGER = Logger.getLogger(JRebelFacet.class.getName());
}
