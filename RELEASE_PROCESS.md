# Release Process

Freestyle Release Process is semi-automated thanks to [sbt-org-policies](https://github.com/47deg/sbt-org-policies) plugin, which is being used in [frees-io organization](https://github.com/frees-io) through [sbt-freestyle](https://github.com/frees-io/sbt-freestyle) plugin. To cut a new release, follow these steps:

1. Release [freestyle-core](https://github.com/frees-io/freestyle), updating file `version.sbt` (don't forget removing the `SNAPSHOT` suffix).

2. Once step 1 is completed, send a new pull request to the `sbt-org-policies` project, updating the Freestyle version [here](https://github.com/47deg/sbt-org-policies/blob/bfabcceb52639a7bea3cc8474d660360dca8e5d2/core/src/main/scala/sbtorgpolicies/libraries.scala#L27). Additionally, in order to publish a new version of `sbt-org-policies` plugin with this new version of Freestyle, don't forget to request a new patch release (just remove the `SNAPSHOT` suffix) in the [version.sbt file](https://github.com/47deg/sbt-org-policies/blob/master/version.sbt).

3. Once steps 1 and 2 are completed, a new pull request should be sent. In this case to the `sbt-freestyle` project, where you need to bump the recently released `sbt-org-policies` version in a couple of places: [plugins.sbt](https://github.com/frees-io/sbt-freestyle/blob/bb834af7fc14539e00f84bd801974d29e7c6b872/project/plugins.sbt#L2) and [build.sbt](https://github.com/frees-io/sbt-freestyle/blob/bb834af7fc14539e00f84bd801974d29e7c6b872/build.sbt#L15). Notice we don't need to modify the project version this time.

4. Before starting with this step, double check the new `sbt-freestyle` SNAPSHOT is already at [Sonatype](https://oss.sonatype.org/content/repositories/snapshots/io/frees/sbt-freestyle_2.10_0.13/0.0.1-SNAPSHOT/). Afterwards, we are going to release the [freestyle-integrations](https://github.com/frees-io/freestyle-integrations) project, where the version must fit (version.sbt file) with the released version in step 1 for `Freestyle` core project. Also, it's important to set up the latest released version as a default value [here](https://github.com/frees-io/freestyle-integrations/blob/d3e398700e15809e049b5bb8fec6c551b9d4c0d0/build.sbt#L1-L1).

5. Regarding documentation, in this step, we are going to release the [freestyle-docs](https://github.com/frees-io/freestyle-docs) project. As in the previous step, the build version must also fit with the one released for `freestyle` core project. Finally, as we did in the above, we have to modify the latest released version as a default value [here](https://github.com/frees-io/freestyle-docs/blob/65fa2944e5a2f8dd420804b26ae0dcbb17a22e9e/build.sbt#L5).

6. Should we do the same for the rest of the project in the Freestyle's ecosystem?
