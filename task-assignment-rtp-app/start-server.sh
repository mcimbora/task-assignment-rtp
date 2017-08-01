#!/usr/bin/env bash
target/wildfly-10.0.0.Final/bin/add-user.sh -u planner -p Planner123_ -ro kie-server -r ApplicationRealm -up application-users.properties -gp application-roles.properties
target/wildfly-10.0.0.Final/bin/standalone.sh --server-config=standalone-full.xml
