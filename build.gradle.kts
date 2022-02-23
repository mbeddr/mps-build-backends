plugins {
    kotlin("jvm") version "1.6.10" apply false
}

val kotlinApiVersion by extra { "1.6" }
val kotlinVersion by extra { "$kotlinApiVersion.10" }

val kotlinArgParserVersion by extra { "2.0.7" }
val mpsVersion by extra { "2020.3.4" }

//this version needs to align with the version shiped with MPS found in the /lib folder otherwise, runtime problems will
//surface because mismatching jars on the classpath.
val fastXmlJacksonVersion by extra { "2.11.+" }
