#!/usr/bin/env bash
cd target
git clone https://github.com/mcimbora/droolsjbpm-integration.git
cd droolsjbpm-integration/kie-server-parent
git checkout PLANNER-815
mvn clean install -DskipTests
cp kie-server-wars/kie-server/target/kie-server-8.0.0-SNAPSHOT-ee7.war ../../wildfly-10.0.0.Final/standalone/deployments/kie-server.war