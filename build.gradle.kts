group = "org.graphiks"
version = (project.findProperty("releaseVersion") as? String)
    ?.takeIf { it.isNotBlank() }
    ?: "0.1.0-SNAPSHOT"
