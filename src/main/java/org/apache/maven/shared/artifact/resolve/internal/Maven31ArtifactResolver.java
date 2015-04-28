package org.apache.maven.shared.artifact.resolve.internal;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.List;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.shared.artifact.resolve.ArtifactResolver;
import org.apache.maven.shared.artifact.resolve.ArtifactResolverException;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.repository.RemoteRepository;

@Component( role = ArtifactResolver.class, hint = "maven31" )
public class Maven31ArtifactResolver
    implements ArtifactResolver
{
    @Requirement
    private RepositorySystem repositorySystem;

    public org.apache.maven.artifact.Artifact resolveArtifact( ProjectBuildingRequest buildingRequest,
                                 org.apache.maven.artifact.Artifact mavenArtifact,
                                 List<ArtifactRepository> remoteRepositories )
        throws ArtifactResolverException
    {
        ArtifactRequest request = new ArtifactRequest();

        Artifact aetherArtifact =
            (Artifact) Invoker.invoke( RepositoryUtils.class, "toArtifact", org.apache.maven.artifact.Artifact.class,
                                       mavenArtifact );
        request.setArtifact( aetherArtifact );

        List<RemoteRepository> aetherRepositories =
            (List<RemoteRepository>) Invoker.invoke( RepositoryUtils.class, "toRepos",
                                                     List.class, remoteRepositories );        
        request.setRepositories( aetherRepositories );

        RepositorySystemSession session =
            (RepositorySystemSession) Invoker.invoke( buildingRequest, "getRepositorySession" );

        try
        {
            Artifact resolvedArtifact = repositorySystem.resolveArtifact( session, request ).getArtifact();
            
            return (org.apache.maven.artifact.Artifact) Invoker.invoke( RepositoryUtils.class, "toArtifact",
                                                                        Artifact.class, resolvedArtifact );
        }
        catch ( ArtifactResolutionException e )
        {
            throw new ArtifactResolverException( e.getMessage(), e );
        }
    }

}