/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
def cli = new CliBuilder(usage: 'updateGeneratedArchetypeSources.groovy -n [name] -v [version]')
cli.with {
    n(longOpt: 'name', args: 1, required: true, argName: 'name', 'Application name (eg \'simpleapp\' or \'helloworld\')')
    v(longOpt: 'version', args: 1, required: true, argName: 'version', 'Isis core version to use as parent POM')
}


/////////////////////////////////////////////////////
//
// constants
//
/////////////////////////////////////////////////////

def BASE="target/generated-sources/archetype/"
def ROOT=BASE + "src/main/resources/"


def supplemental_models_text="""<?xml version="1.0" encoding="UTF-8"?>
<!--
  Licensed to the Apache Software Foundation (ASF) under one
  or more contributor license agreements.  See the NOTICE file
  distributed with this work for additional information
  regarding copyright ownership.  The ASF licenses this file
  to you under the Apache License, Version 2.0 (the
  "License"); you may not use this file except in compliance
  with the License.  You may obtain a copy of the License at
  
         http://www.apache.org/licenses/LICENSE-2.0
         
  Unless required by applicable law or agreed to in writing,
  software distributed under the License is distributed on an
  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  KIND, either express or implied.  See the License for the
  specific language governing permissions and limitations
  under the License.
-->
<supplementalDataModels 
  xmlns="http://maven.apache.org/supplemental-model/1.0.0"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://maven.apache.org/supplemental-model/1.0.0 
            http://maven.apache.org/xsd/supplemental-model-1.0.0.xsd">

</supplementalDataModels>
"""

/////////////////////////////////////////////////////
//
// Parse command line
//
/////////////////////////////////////////////////////

def options = cli.parse(args)
if (!options) {
    return
}

application_name=options.n
isis_version=options.v

println "isis_version = ${isis_version}"

/////////////////////////////////////////////////////
//
// update archetype's own pom.xml's groupId
//
/////////////////////////////////////////////////////

def pomFile=new File(BASE+"pom.xml")

println "updating ${pomFile.path}"

def pomXml = new XmlSlurper(false,false).parseText(pomFile.text)

pomXml.appendNode {
  parent {
    groupId("org.apache.isis.core")
    artifactId("isis")
    version(isis_version)
    relativePath("../../../core/pom.xml")
  }
}
pomXml.groupId='org.apache.isis.archetype'

def fragmentToAdd = new XmlSlurper( false, false ).parseText( '''<properties>
    <archetype.test.skip>true</archetype.test.skip>
</properties>''' )
pomXml.appendNode(fragmentToAdd)


pomFile.text = withLicense(pomXml)



/////////////////////////////////////////////////////
//
// update archetype's resource's pom.xml's
// <revision> and <isis.version>
//
/////////////////////////////////////////////////////

def resourcePomXmlFile=new File(BASE+"src/main/resources/archetype-resources/pom.xml")

println "updating ${resourcePomXmlFile.path}"

def resourcePomXml = new XmlSlurper(false,false).parseText(resourcePomXmlFile.text)

resourcePomXml.properties.revision='0.0.1-SNAPSHOT'
resourcePomXml.properties['isis.version']=isis_version

resourcePomXmlFile.text = withLicense(resourcePomXml)


/////////////////////////////////////////////////////
//
// update archetype-metadata.xml
//
/////////////////////////////////////////////////////


def metaDataFile=new File(ROOT+"META-INF/maven/archetype-metadata.xml")

println "updating ${metaDataFile.path}"

def metaDataXml = new XmlSlurper(false,false).parseText(metaDataFile.text)

metaDataXml.modules.module.fileSets.fileSet.each { fileSet ->
    if(fileSet.directory=='ide/eclipse') {
        fileSet.@filtered='true'
    }
}


metaDataFile.text = withLicense(metaDataXml)


/////////////////////////////////////////////////////
//
// update the .launch files
//
/////////////////////////////////////////////////////

new File(ROOT+"archetype-resources/").eachDirRecurse() { dir ->

    dir.eachFileMatch(~/.*[.]launch/) { launchFile ->

        println "updating ${launchFile.path}"

        def launchXml = new XmlSlurper(false,false).parseText(launchFile.text)
        def projectAttr = launchXml.stringAttribute.find { it.@key=="org.eclipse.jdt.launching.PROJECT_ATTR" }
        String oldValue=projectAttr.@value
        def newValue = oldValue.replaceAll("${application_name}[^-]*-","\\\${rootArtifactId}-")
        projectAttr.@value=newValue

        launchFile.text = """#set( \$symbol_pound = '#' )
#set( \$symbol_dollar = '\$' )
#set( \$symbol_escape = '\\' )
"""
        launchFile.append(XmlUtil.serialize(launchXml))
     }
}


///////////////////////////////////////////////////
//
// add empty supplemental-models.xml
//
///////////////////////////////////////////////////

def appendedResourcesDir = new File(BASE + "src/main/appended-resources")
appendedResourcesDir.mkdir()
def supplementalModelsFile=new File(appendedResourcesDir, "supplemental-models.xml")
supplementalModelsFile.text = supplemental_models_text



///////////////////////////////////////////////////
//
// helper methods
//
///////////////////////////////////////////////////


String withLicense(pomXml) {

    def license_using_xml_comments="""<?xml version="1.0" encoding="UTF-8"?>
<!--
  Licensed to the Apache Software Foundation (ASF) under one
  or more contributor license agreements.  See the NOTICE file
  distributed with this work for additional information
  regarding copyright ownership.  The ASF licenses this file
  to you under the Apache License, Version 2.0 (the
  "License"); you may not use this file except in compliance
  with the License.  You may obtain a copy of the License at
  
         http://www.apache.org/licenses/LICENSE-2.0
         
  Unless required by applicable law or agreed to in writing,
  software distributed under the License is distributed on an
  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  KIND, either express or implied.  See the License for the
  specific language governing permissions and limitations
  under the License.
-->
"""

    return license_using_xml_comments +
            groovy.xml.XmlUtil.serialize(pomXml).replaceFirst("<\\?xml version=\"1.0\".*\\?>", "")

}
