package org.apache.maven.shared.dependency.graph.internal;

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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.project.DefaultDependencyResolutionRequest;
import org.apache.maven.project.DependencyResolutionException;
import org.apache.maven.project.DependencyResolutionRequest;
import org.apache.maven.project.DependencyResolutionResult;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.ProjectDependenciesResolver;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilderException;
import org.apache.maven.shared.dependency.graph.DependencyNode;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.version.VersionConstraint;

/**
 * Wrapper around Eclipse Aether dependency resolver, used in Maven 3.1.
 *
 * @see ProjectDependenciesResolver
 * @author Hervé Boutemy
 * @since 2.1
 */
@Component( role = DependencyGraphBuilder.class, hint = "maven31" )
public class Maven31DependencyGraphBuilder
    implements DependencyGraphBuilder
{
    @Requirement
    private ProjectDependenciesResolver resolver;

    @Requirement
    private ArtifactFactory factory;

    public DependencyNode buildDependencyGraph( MavenProject project, ArtifactFilter filter )
        throws DependencyGraphBuilderException
    {
        try
        {
            ProjectBuildingRequest projectBuildingRequest =
                (ProjectBuildingRequest) invoke( project, "getProjectBuildingRequest" );

            DependencyResolutionRequest request =
                new DefaultDependencyResolutionRequest( project, projectBuildingRequest.getRepositorySession() );

            DependencyResolutionResult result = resolver.resolve( request );

            return buildDependencyNode( null, (org.eclipse.aether.graph.DependencyNode) result.getDependencyGraph(),
                                        project.getArtifact(), filter );
        }
        catch ( DependencyResolutionException e )
        {
            throw new DependencyGraphBuilderException( e.getMessage(), e );
        }
        catch ( IllegalAccessException e )
        {
            throw new DependencyGraphBuilderException( e.getMessage(), e );
        }
        catch ( InvocationTargetException e )
        {
            throw new DependencyGraphBuilderException( e.getMessage(), e );
        }
        catch ( NoSuchMethodException e )
        {
            throw new DependencyGraphBuilderException( e.getMessage(), e );
        }
    }

    private Object invoke( Object object, String method )
        throws IllegalAccessException, InvocationTargetException, NoSuchMethodException
    {
        return object.getClass().getMethod( method ).invoke( object );
    }

    private Artifact getDependencyArtifact( Dependency dep )
    {
        org.eclipse.aether.artifact.Artifact artifact = dep.getArtifact();

        return factory.createDependencyArtifact( artifact.getGroupId(), artifact.getArtifactId(),
                                                 VersionRange.createFromVersion( artifact.getVersion() ),
                                                 artifact.getProperty( "type", artifact.getExtension() ),
                                                 artifact.getClassifier(), dep.getScope(), dep.isOptional() );
    }

    private DependencyNode buildDependencyNode( DependencyNode parent, org.eclipse.aether.graph.DependencyNode node,
                                                Artifact artifact, ArtifactFilter filter )
    {
        DefaultDependencyNode current =
            new DefaultDependencyNode( parent, artifact, node.getPremanagedVersion(), node.getPremanagedScope(),
                                       getVersionSelectedFromRange( node.getVersionConstraint() ) );

        List<DependencyNode> nodes = new ArrayList<DependencyNode>( node.getChildren().size() );
        for ( org.eclipse.aether.graph.DependencyNode child : node.getChildren() )
        {
            Artifact childArtifact = getDependencyArtifact( child.getDependency() );

            if ( ( filter == null ) || filter.include( childArtifact ) )
            {
                nodes.add( buildDependencyNode( current, child, childArtifact, filter ) );
            }
        }

        current.setChildren( Collections.unmodifiableList( nodes ) );

        return current;
    }

    private String getVersionSelectedFromRange( VersionConstraint constraint )
    {
        if ( ( constraint == null ) || ( constraint.getVersion() != null ) )
        {
            return null;
        }

        return constraint.getRange().toString();
    }
}
