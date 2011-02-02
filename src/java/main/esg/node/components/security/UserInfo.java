/***************************************************************************
*                                                                          *
*  Organization: Lawrence Livermore National Lab (LLNL)                    *
*   Directorate: Computation                                               *
*    Department: Computing Applications and Research                       *
*      Division: S&T Global Security                                       *
*        Matrix: Atmospheric, Earth and Energy Division                    *
*       Program: PCMDI                                                     *
*       Project: Earth Systems Grid Federation (ESGF) Data Node Software   *
*  First Author: Gavin M. Bell (gavin@llnl.gov)                            *
*                                                                          *
****************************************************************************
*                                                                          *
*   Copyright (c) 2009, Lawrence Livermore National Security, LLC.         *
*   Produced at the Lawrence Livermore National Laboratory                 *
*   Written by: Gavin M. Bell (gavin@llnl.gov)                             *
*   LLNL-CODE-420962                                                       *
*                                                                          *
*   All rights reserved. This file is part of the:                         *
*   Earth System Grid Federation (ESGF) Data Node Software Stack           *
*                                                                          *
*   For details, see http://esgf.org/esg-node/                             *
*   Please also read this link                                             *
*    http://esgf.org/LICENSE                                               *
*                                                                          *
*   * Redistribution and use in source and binary forms, with or           *
*   without modification, are permitted provided that the following        *
*   conditions are met:                                                    *
*                                                                          *
*   * Redistributions of source code must retain the above copyright       *
*   notice, this list of conditions and the disclaimer below.              *
*                                                                          *
*   * Redistributions in binary form must reproduce the above copyright    *
*   notice, this list of conditions and the disclaimer (as noted below)    *
*   in the documentation and/or other materials provided with the          *
*   distribution.                                                          *
*                                                                          *
*   Neither the name of the LLNS/LLNL nor the names of its contributors    *
*   may be used to endorse or promote products derived from this           *
*   software without specific prior written permission.                    *
*                                                                          *
*   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS    *
*   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT      *
*   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS      *
*   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL LAWRENCE    *
*   LIVERMORE NATIONAL SECURITY, LLC, THE U.S. DEPARTMENT OF ENERGY OR     *
*   CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,           *
*   SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT       *
*   LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF       *
*   USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND    *
*   ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,     *
*   OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT     *
*   OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF     *
*   SUCH DAMAGE.                                                           *
*                                                                          *
***************************************************************************/
package esg.node.components.security;

/**
   Description:
   Container object to hold node user information
   (uses "fluent" mutator functions to make life easier when populating)
**/

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

public class UserInfo {

    private String firstName = null;
    private String lastName = null;
    private String openid = null;
    private String email = null;
    private Map<String,Set<String>> attributes = null;    
    private Set<String> attributeValueSet = null;

    //At package level visibility - on purpose :-)
    UserInfo() {}

	public String getFirstName() { return firstName; }
    public UserInfo setFirstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

	public String getLastName() { return lastName; }
    public UserInfo setLastName(String lastName) {
        this.lastName = lastName;
        return this;
    }
    
	public String getOpenid() { return openid; }
	public UserInfo setOpenid(String openid) {
        this.openid = openid;
        return this;
    }
    
	public String getEmail() { return email; }
	public UserInfo setEmail(String email) {
        this.email = email;
        return this;
    }

    public Map<String,Set<String>> getAttributes() {
        return attributes;
    }
    
    //At package level visibility on purpose
    UserInfo setAttributes(Map<String,Set<String>> attributes) {
        this.attributes = attributes;
        return this;
    }
    
    public void addAttribute(String name, String value) {
        //lazily instantiate attributes map
        if(attributes == null) {
            attributes = new HashMap<String,Set<String>>();
        }

        //lazily instantiate the set of values for attribute if not
        //there
        if((attributeValueSet = attributes.get(name)) == null) {
            attributeValueSet = new HashSet<String>();
        }

        //enter attribute associated with attribute value set
        attributeValueSet.add(value);
        attributes.put(name, attributeValueSet);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("UserInfo:\n")
            .append("First Name:\t"+firstName+"\n")
            .append("Last Name:\t"+lastName+"\n")
            .append("Open ID:\t"+openid+"\n")
            .append("Email:\t"+email+"\n")
            .append("Attributes:\n");
        
        for(String attributeName : attributes.keySet()) {
            for(String attributeValue : attributes.get(attributeName)) {
                sb.append("\t"+attributeName+" --> "+attributeValue+"\n");
            }
        }
        return sb.toString();
    }
    
}