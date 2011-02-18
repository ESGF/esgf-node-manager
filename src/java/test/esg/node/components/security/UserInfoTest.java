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

   Testing the UserInfo and UserInfoDAO objects

**/

import org.junit.*;
import static org.junit.Assert.*;
import static org.junit.Assume.*;
import static org.apache.commons.codec.digest.DigestUtils.*;
import static esg.common.Utils.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Properties;


public class UserInfoTest {

    private static final Log log = LogFactory.getLog(UserInfoTest.class);
    private static UserInfoDAO userInfoDAO = null;
    private static GroupRoleDAO groupRoleDAO = null;
    private static UserInfo gavin = null;
 
    @BeforeClass
    public static void initTest() {
        System.out.println("UserInfoTest initializing");
        
        groupRoleDAO = new GroupRoleDAO(new Properties());
        groupRoleDAO.addGroup("CMIP5");
        groupRoleDAO.addGroup("ARM");
        groupRoleDAO.addRole("admin");
        groupRoleDAO.addRole("user");


        userInfoDAO = new UserInfoDAO(new Properties());
        gavin = new UserInfo();
        gavin.setFirstName("Gavin").
            setMiddleName("Max").
            setLastName("Bell").
            setUserName("bell51").
            setEmail("gavin@llnl.gov").
            setOrganization("LLNL").
            setCity("Livermore").
            setState("California").
            setCountry("USA").
            addGroupAndRole("CMIP5","admin").
            addGroupAndRole("CMIP5","user").
            addGroupAndRole("ARM","user");
    }
    
    @Test
    public void testUserInfo() {        
        System.out.println(gavin);
    }

    @Test
    public void testAddUser() {
        System.out.print("Adding user "+gavin.getUserName()+" id="+gavin.getid()+" openid="+gavin.getOpenid()+": ");
        if(userInfoDAO.addUserInfo(gavin)) {
            System.out.println("[OK]");
            
            String origPassword = "foobar";
            String newPassword = "foobaralpha";

            String origPasswordMd5Hex = md5Hex(origPassword);
            String newPasswordMd5Hex  = md5Hex(newPassword);
            
            System.out.print("Setting password: ["+origPassword+" -> "+origPasswordMd5Hex+"]");
            if(userInfoDAO.setPassword(gavin.getOpenid(),origPassword)) {
                System.out.println("[OK]");
            }else {
                System.out.println("[FAIL]");
            }

            System.out.print("Checking password: ");
            if(userInfoDAO.checkPassword(gavin.getOpenid(),origPassword)) {
                System.out.println("[OK]");
            }else {
                System.out.println("[FAIL]");
            }

            System.out.print("Able to change password: ");
            System.out.print("["+origPassword+" -> "+origPasswordMd5Hex+"]");
            System.out.print("["+newPassword+" -> "+newPasswordMd5Hex+"]");
            if(userInfoDAO.changePassword(gavin.getOpenid(),origPassword,newPassword)) {
                System.out.println("[OK]");
            }else{
                System.out.println("[FAIL]");
            }

            System.out.print("Checking password mismatch: ["+origPassword+" ?-> "+newPasswordMd5Hex+"]");
            if(userInfoDAO.checkPassword(gavin.getOpenid(),origPassword)) {
                System.out.println("[FAIL]");
            }else {
                //This should fail! since the password is now foobaralpha! (right!?)
                System.out.println("[OK]");
            }
                                
        }else{
            System.out.println("[FAIL]");
        }
    }
    
    @Test
    public void testGetUser() {
        UserInfo dean = new UserInfo();
        System.out.println("\nCreating Fresh Dean User");
        dean.setFirstName("Dean").
            setMiddleName("N").
            setLastName("Williams").
            setUserName("williams13").
            setEmail("dean@llnl.gov").
            setDn("O=LLNL/OU=ESGF").
            setOrganization("LLNL").
            setOrgType("Research").
            setCity("Livermore").
            setState("California").
            setCountry("USA").
            addGroupAndRole("CMIP5","admin").
            addGroupAndRole("ARM","user");
        System.out.println(dean);

        boolean success = false;
        System.out.println("\nAdding Fresh Dean User To Database...");
        if(userInfoDAO.addUserInfo(dean)) System.out.println("[OK]"); else System.out.println("[FAIL]");
        System.out.print(dean);
        
        System.out.println("\nPulling out Dean user from Database...");
        dean = userInfoDAO.getUserById("https://"+getFQDN()+"/esgf-idp/openid/williams13");

        System.out.println("\nModifying Dean user object...(middle name and email)");
        dean.setMiddleName("Neill");
        dean.setEmail("williams13@llnl.gov");
        System.out.println(dean);

        System.out.println("\nModifying Dean user object and resubmitting to database...");
        if(userInfoDAO.addUserInfo(dean)) System.out.println("[OK]"); else System.out.println("[FAIL]");

        System.out.println("\nPulling out Dean user from Database after modifications...");
        dean = userInfoDAO.getUserById("https://"+getFQDN()+"/esgf-idp/openid/williams13");
        System.out.println(dean);

        System.out.println("\nRe-Adding SAME Dean user object to database...");
        if(userInfoDAO.addUserInfo(dean)) System.out.println("[OK]"); else System.out.println("[FAIL]");

    }

    @Test
    public void testSetPermissions() {
        
        groupRoleDAO.addGroup("CMIP6");
        groupRoleDAO.addGroup("CMIP7");

        groupRoleDAO.addRole("god");
        groupRoleDAO.addRole("king");

        UserInfo bob = new UserInfo();
        System.out.println("\nCreating Fresh Bob User");
        bob.setFirstName("Bob").
            setLastName("Drach").
            setUserName("drach1").
            setEmail("bob@llnl.gov").
            setDn("O=LLNL/OU=ESGF").
            setOrganization("LLNL").
            setOrgType("Research").
            setCity("Livermore").
            setState("California").
            setCountry("USA").
            addGroupAndRole("CMIP5","admin").
            addGroupAndRole("CMIP5","user").
            addGroupAndRole("CMIP6","god").
            addGroupAndRole("CMIP6","king").
            addGroupAndRole("CMIP6","admin").
            addGroupAndRole("CMIP7","admin");
        System.out.println(bob);

        if(userInfoDAO.addUserInfo(bob)) System.out.println("[OK]"); else System.out.println("[FAIL]");

        System.out.println("\nPulling out Gavin user from Database... DELETING...");
        UserInfo gavin = userInfoDAO.getUserById("https://"+getFQDN()+"/esgf-idp/openid/bell51");
        if(userInfoDAO.deleteUserInfo(gavin)) System.out.println("[OK]"); else System.out.println("[FAIL]");

        groupRoleDAO.renameGroup("CMIP5","CMIP_NOW");
        groupRoleDAO.renameRole("god","lord");
        groupRoleDAO.renameRole("admin","administrator");
        bob = userInfoDAO.refresh(bob);
        System.out.println(bob);
        
    }

    
    
}
