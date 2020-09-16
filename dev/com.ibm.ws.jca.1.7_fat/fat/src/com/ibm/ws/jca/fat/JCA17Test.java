/*******************************************************************************
 * Copyright (c) 2013, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jca.fat;

import static org.junit.Assert.assertNotNull;

import java.io.File;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
public class JCA17Test extends FATServletClient {
    @Server("com.ibm.ws.jca.fat")
    public static LibertyServer server;

    private static final String WEB_NAME = "fvtweb";
    private static final String APP_NAME = "fvtapp";

    private static final String SERVLET_AOD = "aod";
    private static final String SERVLET_AODS = "aods";
    private static final String SERVLET_DD = "JCADDServlet";
    private static final String SERVLET_DFDS = "dfds";
    private static final String SERVLET_TRAN = "transupport";

    @BeforeClass
    public static void setUp() throws Exception {
        // Build jars that will be in the RAR
        JavaArchive adapter_jar = ShrinkWrap.create(JavaArchive.class, "ResourceAdapter.jar");
        adapter_jar.addPackage("com.ibm.adapter");
        adapter_jar.addPackage("com.ibm.adapter.message");
        adapter_jar.addPackage("com.ibm.ejs.ras");
        adapter_jar.addPackage("com.ibm.inout.adapter");
        adapter_jar.addManifest();

        // Build the resource adapter
        ResourceAdapterArchive adpater_resource = ShrinkWrap.create(ResourceAdapterArchive.class, "adapter_administrator_object.rar");
        adpater_resource.as(JavaArchive.class).addPackage("fat.jca.resourceadapter");
        adpater_resource.addAsManifestResource(new File("test-resourceadapters/adapter/resources/META-INF/ra.xml"));
        adpater_resource.addAsLibrary(adapter_jar);
        adpater_resource.addManifest();
        ShrinkHelper.exportToServer(server, "connectors", adpater_resource);

        // Build the web module and application
        WebArchive fvtweb_war = ShrinkWrap.create(WebArchive.class, WEB_NAME + ".war");
        fvtweb_war.addPackage("web");
        fvtweb_war.addPackage("ejb");
        fvtweb_war.addAsWebInfResource(new File("test-applications/fvtweb/resources/WEB-INF/ejb-jar.xml"));
        fvtweb_war.addAsWebInfResource(new File("test-applications/fvtweb/resources/WEB-INF/ibm-ejb-jar-bnd.xml"));
        fvtweb_war.addAsWebInfResource(new File("test-applications/fvtweb/resources/WEB-INF/ibm-web-bnd.xml"));
        fvtweb_war.addAsWebInfResource(new File("test-applications/fvtweb/resources/WEB-INF/web.xml"));

        EnterpriseArchive fvtapp_ear = ShrinkWrap.create(EnterpriseArchive.class, APP_NAME + ".ear");
        fvtapp_ear.addAsModule(fvtweb_war);
        fvtapp_ear.addAsModule(adpater_resource);
        fvtapp_ear.addAsManifestResource(new File("test-applications/fvtapp/META-INF/application.xml"));
        ShrinkHelper.exportToServer(server, "apps", fvtapp_ear);

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("J2CA8030E",
                          "J2CA0241W",
                          "J2CA0045E.*connfactory(1|2)",
                          "SRVE0319E.*JCADDServlet",
                          "SRVE0276E.*JCADDServlet",
                          "CWNEN1006E");
    }

    private void runTest(String servlet) throws Exception {
        FATServletClient.runTest(server, WEB_NAME + "/" + servlet, getTestMethodSimpleName());
    }

    @Test
    @AllowedFFDC("java.lang.Exception")
    public void testBeanArchiveResourceAdapter() throws Exception {
        assertNotNull("Server should report : Resource Adapter Bean Archives are not supported",
                      server.waitForStringInLog("J2CA0241W"));
    }

    @Test
    public void testLookupAdministeredObjectDefinition() throws Exception {
        runTest(SERVLET_AOD);
    }

    @Test
    public void testLookupAdministeredObjectDefinitions() throws Exception {
        runTest(SERVLET_AODS);
    }

    @Test
    public void testLookupAdministeredObjectNoInterfaceName() throws Exception {
        runTest(SERVLET_AODS);
    }

    @Test
    public void testLookupAdministeredObjectWithProperties() throws Exception {
        runTest(SERVLET_AODS);
    }

    @Test
    public void testLookupConnectionFactoryWebDD() throws Exception {
        runTest(SERVLET_DD);
    }

    @Test
    public void testLookupConnectionFactoryAppDD() throws Exception {
        runTest(SERVLET_DD);
    }

    @Test
    public void testLookupConnectionFactoryEJBDD() throws Exception {
        runTest(SERVLET_DD);
    }

    @Test
    public void testLookupAdministeredObjectWebDD() throws Exception {
        runTest(SERVLET_DD);
    }

    @Test
    public void testLookupAdministeredObjectAppDD() throws Exception {
        runTest(SERVLET_DD);
    }

    @Test
    public void testLookupAdministeredObjectEJBDD() throws Exception {
        runTest(SERVLET_DD);
    }

    @Test
    public void testLookupConnectionFactoryAnnotation() throws Exception {
        runTest(""); // servlet context root is '/'
    }

    @Test
    @Mode(TestMode.FULL)
    @ExpectedFFDC("com.ibm.websphere.ce.j2c.ConnectionWaitTimeoutException")
    public void testConnectionFactoryAnnotationMaxPoolSize() throws Exception {
        runTest(""); // servlet context root is '/'
    }

    @Test
    public void testLookupConnectionFactoryDefinitionsAnnotation() throws Exception {
        runTest(SERVLET_DFDS);
    }

    @Test
    @Mode(TestMode.FULL)
    @ExpectedFFDC("com.ibm.websphere.ce.j2c.ConnectionWaitTimeoutException")
    public void testConnectionFactoryDefinitionsAnnotationMaxPoolSize() throws Exception {
        runTest(SERVLET_DFDS);
    }

    @Test
    @ExpectedFFDC({ "java.lang.IllegalArgumentException",
                    "javax.servlet.UnavailableException",
                    "com.ibm.wsspi.injectionengine.InjectionException" })
    public void testConnectionFactoryAnnotationTransactionSupport() throws Throwable {
        try {
            runTest(SERVLET_TRAN);
        } catch (Throwable e) {
            if (!e.getMessage().contains("CWNEN1006E"))
                throw e;
        }
    }
}
