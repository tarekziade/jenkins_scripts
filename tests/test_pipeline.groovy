#!/usr/bin/env groovy
import groovy.util.GroovyTestSuite
import junit.framework.Test
import junit.textui.TestRunner
import pipeline


pipeline.metaClass.static.timeout = { args, cloj  -> return cloj()}
pipeline.metaClass.static.echo = { msg  -> println msg}



class PipelineTest extends GroovyTestCase {

   def script = new pipeline()

   void testValidURL() {
      def url = 'https://github.com/tarekziade/somerepo'
      assertTrue(script.validURL(url))
   }

   void testProject() {
      script.testProject('kintowe')

   }

}

class AllTests {
   static Test suite() {
      def allTests = new GroovyTestSuite() 
      allTests.addTestSuite(PipelineTest.class)
      return allTests
   }
}

TestRunner.run(AllTests.suite())

