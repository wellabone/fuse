/*
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fusesource.process.fabric.commands;

import org.apache.felix.gogo.commands.Command;
import org.fusesource.process.fabric.ContainerInstallOptions;
import org.fusesource.process.manager.Installation;

import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

@Command(name = "ps", scope = "process", description = "Lists the currently installed managed processes.")
public class ProcessList extends ContainerProcessCommandSupport {

    static final String[] HEADERS = {"[id]", "[pid]", "[name]"};
    static final String FORMAT = "%7s %9s %s";

    @Override
    void doWithAuthentication(String jmxUser, String jmxPassword) throws Exception {
        ContainerInstallOptions options = ContainerInstallOptions.builder()
                .container(container)
                .user(jmxUser)
                .password(jmxPassword)
                .build();

        List<Installation> installations = getContainerProcessManager().listInstallations(options);
        printInstallations(installations, System.out);
    }

    protected void printInstallations(List<Installation> installations, PrintStream out) {
        out.println(String.format(FORMAT, HEADERS));
        for (Installation installation : installations) {
            int id = installation.getId();
            Integer pid = null;
            try {
                pid = installation.getController().getPid();
            } catch (IOException e) {
                System.err.println("Failed to find pid for id: " + id + ". " + e);
            }
            out.println(String.format(FORMAT, "" + id, (pid != null) ? pid.toString() : "", installation.getName()));
        }
    }
}
