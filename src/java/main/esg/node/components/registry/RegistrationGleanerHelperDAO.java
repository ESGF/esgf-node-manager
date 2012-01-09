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
package esg.node.components.registry;

import esg.common.generated.registration.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.sql.DataSource;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import esg.common.db.DatabaseResource;

/**
   Description:
   Provides database access support to the RegistrationGleaner.
*/
public class RegistrationGleanerHelperDAO {

    private static final Log log = LogFactory.getLog(RegistrationGleanerHelperDAO.class);

    private Properties props = null;
    private DataSource dataSource = null;
    private QueryRunner queryRunner = null;
    private ResultSetHandler<Map<String,String>> attributeGroupsResultSetHandler = null;
    
    //-------------------------------------
    //QUERY SHOPPE
    //-------------------------------------
    private static final String attributeGroupsQuery = 
        "SELECT name, description FROM esgf_security.group";
    //-------------------------------------

    public RegistrationGleanerHelperDAO(Properties props) {
        this.props = props;

        if (DatabaseResource.getInstance() == null) {
            DatabaseResource.init(props.getProperty("db.driver","org.postgresql.Driver")).setupDataSource(props);
        }
        
        this.setDataSource(DatabaseResource.getInstance().getDataSource());
        init();
    }

    public void setDataSource(DataSource dataSource) {
        log.trace("Helper, Setting Up RegistrationGleaners's Pooled Data Source");
        this.dataSource = dataSource;
        this.queryRunner = new QueryRunner(dataSource);
    }
    
    //Create handlers for query results
    public void init() {
        attributeGroupsResultSetHandler = new ResultSetHandler<Map<String,String>>() {
            public Map<String,String> handle(ResultSet rs) throws SQLException {
                HashMap<String,String> results = new HashMap<String,String>();
                String name = null;
                String desc = null;
                java.util.regex.Matcher typeMatcher = AtsWhitelistGleaner.TYPE_PATTERN.matcher("");
                while(rs.next()) {
                    name = rs.getString(1);
                    desc = rs.getString(2);
                    typeMatcher.reset(name);
                    if(typeMatcher.find()) {
                        log.trace("Skipping "+name);
                        continue;
                    }
                    results.put(name,desc);
                }
                return results;
            }
        };
    }
    
    public void loadAttributeServiceGroups(AttributeService attrSvc) {
        try{
            Map<String,String> results = queryRunner.query(attributeGroupsQuery,attributeGroupsResultSetHandler);
            for(String name : results.keySet()) {
                Group newGroup = new Group();
                log.info("Adding Group: "+name+" - "+results.get(name));
                newGroup.setName(name);
                newGroup.setDescription(results.get(name));
                attrSvc.getGroup().add(newGroup);
            }
        }catch(SQLException e) {
            log.error(e);
        }
    }
}

