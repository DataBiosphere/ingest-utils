package org.broadinstitute.monster.common

/**
 * Trait for allowing quick/predictable access to test fixtures in specs.
 */
trait TestResources {
  /** Returns a `java.io.File` representing the named resource; resources are presumed to exist in
    * the same package as the calling class; hence `org.broadinstitute.gpp.spectacle.Tests` calling
    * `resourceFile("foo.txt")` would return a file pointing to
    * `new File("${ROOT}/src/test/resources/org/broadinstitute/gpp/spectacle", "foo.txt")`
    */
  def resourceFile(name: String): File = resourcePath(name).toFile
  /** Returns a `java.io.file.Path` representing the named resource.
    *
    * I.e., this is the same as `resourceFile()` above, but it returns a `Path` instead of a
    * `File`.
    */
  def resourcePath(name: String): Path = {
    val c = this.getClass.getResource(name)
    assert(c != null, s"Can't find resource at path $name for ${this.getClass.getName}")
    Paths.get(c.getPath)
  }
}
