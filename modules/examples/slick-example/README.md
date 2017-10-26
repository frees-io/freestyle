This project is an example of [Freestyle-Slick](http://frees.io/docs/integrations/slick/) integration with a Postgresql database.

## Generate

### Mapping

We are using ìn this project [Slick-CodeGen](http://slick.lightbend.com/doc/3.2.0/codegen-api/index.html#package).

To generate the file `Tables.scala` with the mapping you can execute `sbt slick-gen`.

### Schema

The application generates the necessary schema to run itself and drop it when it finishes.

***Note***: Set your own user and password in `application.conf` and `build.sbt`.

[comment]: # (Start Copyright)
# Copyright

Freestyle is designed and developed by 47 Degrees

Copyright (C) 2017 47 Degrees. <http://47deg.com>

[comment]: # (End Copyright)