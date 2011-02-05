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

   Just from a lexicon standpoint:
   In SAML Land ------------------------ In User Land
   (Attributes Type -> Attribute Value) => (Group -> Role)

   NOTE:

   This object represents a user in the system, encapsulating their basic
   information.  By using this tuple it will be easier to see if users
   are equal at the object level. Because there are other attributes that
   do not take part in the equality, writers most be extra careful when
   using data structures to hold these objects that they be sure to
   remove the object and write put in this new one during insertion.
   Many datastructures will refuse to re-write an object that is
   considered already there!

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
    private Map<String,Set<String>> groups = null;    
    private Set<String> roleSet = null;
    private String objId = null;
    

    //At package level visibility - on purpose :-)
    UserInfo(String firstName, String lastName, String openid) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.openid = openid;
        makeId();
    }

	public String getFirstName() { return firstName; }
	public String getLastName() { return lastName; }
	public String getOpenid() { return openid; }
    
	public String getEmail() { return email; }
	public UserInfo setEmail(String email) {
        this.email = email;
        return this;
    }

    public Map<String,Set<String>> getGroups() {
        return groups;
    }
    
    //At package level visibility on purpose
    UserInfo setGroups(Map<String,Set<String>> groups) {
        this.groups = groups;
        return this;
    }
    
    public void addGroup(String name, String value) {
        //lazily instantiate groups map
        if(groups == null) {
            groups = new HashMap<String,Set<String>>();
        }

        //lazily instantiate the set of values for group if not
        //there
        if((roleSet = groups.get(name)) == null) {
            roleSet = new HashSet<String>();
        }

        //enter group associated with group value set
        roleSet.add(value);
        groups.put(name, roleSet);
    }

    //NOTE: This object is identified the the equivalence of the tuple
    //of firstName+lastName+openid. This value is computed at
    //construction.  This is encapsulated here to provide single
    //location for changing of this object's id
    private String makeId() {
        objId = ""+getFirstName()+getLastName()+getOpenid();
        return objId;
    }

    public int hashCode() { return objId.hashCode(); }

    public boolean equals(Object o) {
        if (! (o instanceof esg.node.components.security.UserInfo)) return false;
        return this.objId.equals(((UserInfo)o).objId);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("UserInfo:\n")
            .append("First Name:\t"+firstName+"\n")
            .append("Last Name:\t"+lastName+"\n")
            .append("Open ID:\t"+openid+"\n")
            .append("Email:\t"+email+"\n")
            .append("Groups:\n");
        
        for(String groupName : groups.keySet()) {
            for(String role : groups.get(groupName)) {
                sb.append("\t"+groupName+" --> "+role+"\n");
            }
        }
        return sb.toString();
    }
    
}