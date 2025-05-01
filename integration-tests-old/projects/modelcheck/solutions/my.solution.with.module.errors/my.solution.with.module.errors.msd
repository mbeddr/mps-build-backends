<?xml version="1.0" encoding="UTF-8"?>
<solution name="my.solution.with.module.errors" uuid="002f5c40-2348-4a91-b6cd-3c9a74048c9f" moduleVersion="0" compileInMPS="true">
  <models>
    <modelRoot contentPath="${module}" type="default">
      <sourceRoot location="models" />
    </modelRoot>
  </models>
  <facets>
    <facet type="java">
      <classes generated="true" path="${module}/classes_gen" />
    </facet>
  </facets>
  <sourcePath />
  <dependencies>
    <dependency reexport="false">86530839-2a21-40f5-8fea-f98ec8d7d852(my.solution.non.existing)</dependency>
  </dependencies>
  <languageVersions />
  <dependencyVersions>
    <module reference="86530839-2a21-40f5-8fea-f98ec8d7d852(my.solution.non.existing)" version="0" />
    <module reference="002f5c40-2348-4a91-b6cd-3c9a74048c9f(my.solution.with.module.errors)" version="0" />
  </dependencyVersions>
</solution>

