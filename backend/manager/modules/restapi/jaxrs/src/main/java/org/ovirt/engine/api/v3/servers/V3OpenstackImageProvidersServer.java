/*
Copyright (c) 2016 Red Hat, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package org.ovirt.engine.api.v3.servers;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.ovirt.engine.api.resource.openstack.OpenstackImageProvidersResource;
import org.ovirt.engine.api.v3.V3Server;
import org.ovirt.engine.api.v3.types.V3OpenStackImageProvider;
import org.ovirt.engine.api.v3.types.V3OpenStackImageProviders;

@Produces({"application/xml", "application/json"})
public class V3OpenstackImageProvidersServer extends V3Server<OpenstackImageProvidersResource> {
    public V3OpenstackImageProvidersServer(OpenstackImageProvidersResource delegate) {
        super(delegate);
    }

    @POST
    @Consumes({"application/xml", "application/json"})
    public Response add(V3OpenStackImageProvider provider) {
        return adaptAdd(getDelegate()::add, provider);
    }

    @GET
    public V3OpenStackImageProviders list() {
        return adaptList(getDelegate()::list);
    }

    @Path("{id}")
    public V3OpenstackImageProviderServer getProviderResource(@PathParam("id") String id) {
        return new V3OpenstackImageProviderServer(getDelegate().getProviderResource(id));
    }
}
