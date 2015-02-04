![ZooDB](https://github.com/tzaeschke/zoodb/raw/master/doc/images/logo_510412_web.png)

ZooDB
=====
ZooDB is an object oriented database based on the JDO 3.0 standard.
It is written by Tilmann Zaeschke.
It is licensed under GPLv3 (GNU Public License), see file COPYING.

ZooDB is also available via maven:

```
<dependency>
    <groupId>org.zoodb</groupId>
    <artifactId>zoodb</artifactId>
    <version>0.4.4</version>
</dependency>
```

Current Status
==============
Under development, but already in use by some university projects.


Current Features
================
- Works as normal database or as in-memory database.
- Fast (4x faster than db4o using db4o's PolePosition benchmark suite).
- Reasonably scalable, has been used successfully with 60,000,000+ objects in a 30+ GB database.
- Maximum object count: 2^63.
- Maximum database size depends on (configurable) cluster size: 2^31 * CLUSTER_SIZE. With default cluster size: 2^31 * 4KB = 8TB.
- Crash-recovery/immunity (dual flush, no log-file required).
- Standard stuff: commit/rollback, query, indexing, lazy-loading, transitive persistence & updates (persistence by reachability), automatic schema definition, embedded object support (second class objects).
- Queries support standard operators, indexing, parameters, aggregation (avg, max, min), projection, uniqueness and setting result classes (partial).
- Multi-user/-session capability (optimistic TX), but currently not terribly efficient.
- XML export/import (currently only binary attributes).
- Some examples are available in the 'examples' folder.
- Open source (GPLv3).

Current Limitations
===================
- Schema evolution is ~90% complete (updating OIDs is not properly supported, no low level queries).
  --> Queries don't work with lazy evolution.
- No backup (except copying the DB file).
- Single process usage only (No stand-alone server).
- Not thread-safe.
- JDO only partially supported:
  - Some query features not supported: group by, order by, range, variables, imports, setting result classes (partial).
  - No XML config or Annotations; configuration only via Java API.
  - Manual enhancement of classes required (insert activateRead()/activateWrite() & extend provided super-class).
- Little documentation (some example code), but follows JDO 3.0 spec.


Dependencies
============
* JDO 3.0 (Java Data Objects): 
  - URL: https://db.apache.org/jdo/
  - JAR: jdo2-api-3.0.jar
* JTA (Java Transaction API):
  - URL: http://java.sun.com/products/jta/
  - JAR: jta.jar
* JUnit (currently use 4.8.1, but should work with newer and older versions as well):
  - URL: http://www.junit.org/
  - JAR: junit-4.8.1.jar
* Java 7


Contact
=======
zoodb(AT)gmx(DOT)de