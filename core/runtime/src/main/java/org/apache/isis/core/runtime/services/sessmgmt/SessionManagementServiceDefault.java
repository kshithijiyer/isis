/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.isis.core.runtime.services.sessmgmt;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.isis.applib.NonRecoverableException;
import org.apache.isis.applib.services.sessmgmt.SessionManagementService;
import org.apache.isis.core.runtime.system.context.IsisContext;
import org.apache.isis.core.runtime.system.persistence.PersistenceSession;
import org.apache.isis.core.runtime.system.session.IsisSession;
import org.apache.isis.core.runtime.system.session.IsisSessionFactory;
import org.apache.isis.core.runtime.system.transaction.IsisTransactionManager;
import org.apache.isis.core.security.authentication.AuthenticationSession;

import lombok.val;

@Singleton
public class SessionManagementServiceDefault implements SessionManagementService {
	
//	@PostConstruct
//	public void init() {
//    	isisTransactionManager = isisSessionFactory.getCurrentSession().getPersistenceSession().getTransactionManager(); 
//	}

    @Override
    public void nextSession() {

        final AuthenticationSession authenticationSession =
                isisSessionFactory.getCurrentSession().getAuthenticationSession();

		val isisTransactionManager = getTransactionManager();
        
        isisTransactionManager.endTransaction();
        
        
        isisSessionFactory.closeSession();

        isisSessionFactory.openSession(authenticationSession);
        isisTransactionManager.startTransaction();
        
    }


    @Inject IsisSessionFactory isisSessionFactory;
    //IsisTransactionManager isisTransactionManager;

    protected PersistenceSession getPersistenceSession() {
        return ofNullable(getIsisSessionFactory().getCurrentSession())
                .map(IsisSession::getPersistenceSession)
                .orElseThrow(()->new NonRecoverableException("No IsisSession on current thread."));
    }

    private IsisSessionFactory getIsisSessionFactory() {
        return requireNonNull(isisSessionFactory, "IsisSessionFactory was not injected.");
    }

    public IsisTransactionManager getTransactionManager() {
        return getPersistenceSession().getTransactionManager();
    }
    
}
