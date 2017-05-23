package com.hollyshi.mybatis.generator;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.mybatis.generator.api.*;
import org.mybatis.generator.api.dom.java.Field;
import org.mybatis.generator.api.dom.java.JavaElement;
import org.mybatis.generator.api.dom.java.Method;
import org.mybatis.generator.api.dom.java.TopLevelClass;
import org.mybatis.generator.api.dom.xml.*;
import org.mybatis.generator.internal.JDBCConnectionFactory;

/**
 * Created by sky on 17/03/08.
 * MyBatis Generator Plugin
 */
public class MyGeneratorPlugin extends PluginAdapter {
    private final static int MAX_LENGTH = 70;
    private static final String AUTHOR = "modelClassAuthor";
    public boolean validate(List<String> list) {
        return true;
    }

    @Override
    public void initialized(IntrospectedTable introspectedTable) {
        // Mapper rename to DAO
        introspectedTable.setMyBatis3JavaMapperType(
                introspectedTable.getMyBatis3JavaMapperType().replaceAll("Mapper$", "Dao"));
        // **Mapper.xml rename to **.xml
        introspectedTable.setMyBatis3XmlMapperFileName(
                introspectedTable.getMyBatis3XmlMapperFileName().replaceAll("Mapper\\.xml", "\\.xml"));
    }

    /**
     * generate batch insert sql
     * @return
     */
    @Override
    public boolean sqlMapDocumentGenerated(Document document, IntrospectedTable introspectedTable) {
        XmlElement parentElement = document.getRootElement();

        XmlElement batchInsertElement = new XmlElement("insert");
        batchInsertElement.addAttribute(new Attribute("id", "batchInsert"));
        batchInsertElement.addAttribute(new Attribute("parameterType", "java.util.List"));

        StringBuilder sb = new StringBuilder();
        sb.append("insert into ");
        sb.append(introspectedTable.getAliasedFullyQualifiedTableNameAtRuntime());
        sb.append(" (");

        List<IntrospectedColumn> columns = introspectedTable.getAllColumns();
        for (int i = 0; i < columns.size(); i++) {
            IntrospectedColumn column = columns.get(i);
            sb.append(column.getActualColumnName());

            addNewElementIfTooLong(sb, batchInsertElement, i, columns.size(), true);
        }
        sb.append(")");
        batchInsertElement.addElement(new TextElement(sb.toString()));
        sb.setLength(0);
        sb.append("values");
        batchInsertElement.addElement(new TextElement(sb.toString()));

        String item = "item";
        XmlElement forEachElement = new XmlElement("foreach");
        forEachElement.addAttribute(new Attribute("collection", "list"));
        forEachElement.addAttribute(new Attribute("item", item));
        forEachElement.addAttribute(new Attribute("index", "index"));
        forEachElement.addAttribute(new Attribute("separator", ","));
        sb.setLength(0);
        sb.append("(");
        for (int i = 0; i < columns.size(); i++) {
            IntrospectedColumn column = columns.get(i);
            sb.append("#{").append(item).append(".");
            sb.append(column.getJavaProperty());
            sb.append(", jdbcType=");
            sb.append(column.getJdbcTypeName());
            sb.append("}");

            addNewElementIfTooLong(sb, forEachElement, i, columns.size(), false);
        }
        if (sb.length() > 0) {
            forEachElement.addElement(new TextElement(sb.toString()));
        }
        forEachElement.addElement(new TextElement(")"));

        batchInsertElement.addElement(forEachElement);

        parentElement.addElement(batchInsertElement);
        return super.sqlMapDocumentGenerated(document, introspectedTable);
    }

    private void addNewElementIfTooLong(StringBuilder sb, XmlElement element, int i, int size, boolean needIndent) {
        if (i != size - 1) {
            sb.append(", ");
            if (sb.length() > MAX_LENGTH) {
                if (needIndent) {
                    element.addElement(new TextElement(sb.toString()));
                } else {
                    element.addElement(new TextElement(sb.toString()));
                }
                sb.setLength(0);
                if (needIndent) {
                    sb.append("  ");
                }
            }
        }
    }

    /**
     * add remark for field
     * @return
     */
    @Override
    public boolean modelFieldGenerated(Field field, TopLevelClass topLevelClass,
               IntrospectedColumn introspectedColumn, IntrospectedTable introspectedTable,
               ModelClassType modelClassType) {
        addRemark(introspectedColumn, field);
        return true;
    }

    /**
     * add remark for getter method of filed
     * @return
     */
    @Override
    public boolean modelGetterMethodGenerated(Method method, TopLevelClass topLevelClass,
              IntrospectedColumn introspectedColumn, IntrospectedTable introspectedTable,
              ModelClassType modelClassType) {
        addRemark(introspectedColumn, method);
        return true;
    }

    /**
     * add remark for setter method of filed
     * @return
     */
    @Override
    public boolean modelSetterMethodGenerated(Method method, TopLevelClass topLevelClass,
              IntrospectedColumn introspectedColumn, IntrospectedTable introspectedTable,
              ModelClassType modelClassType) {
        addRemark(introspectedColumn, method);
        return true;
    }

    private void addRemark(IntrospectedColumn introspectedColumn, JavaElement element) {
        String remark = introspectedColumn.getRemarks();
        element.addJavaDocLine("/** ");
        element.addJavaDocLine(" * " + remark);
        element.addJavaDocLine(" */");
    }

    /**
     * add remark for Class
     * @return
     */
    @Override
    public boolean modelBaseRecordClassGenerated(TopLevelClass topLevelClass,
             IntrospectedTable introspectedTable) {
        String remarks = "";
        String author = getProperties().getProperty(AUTHOR);
        if (null == author || "".equals(author)) {
            author = System.getProperty("user.name");
        }

        FullyQualifiedTable table = introspectedTable.getFullyQualifiedTable();
        try {
            Connection connection = new JDBCConnectionFactory(context.getJdbcConnectionConfiguration()).getConnection();
            ResultSet rs = connection.getMetaData().getTables(table.getIntrospectedCatalog(),
                    table.getIntrospectedSchema(), table.getIntrospectedTableName(), null);

            if (null != rs && rs.next()) {
                remarks = rs.getString("REMARKS");
            }
            closeConnection(connection, rs);
        } catch (SQLException e) {}

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        topLevelClass.addJavaDocLine("/**");
        topLevelClass.addJavaDocLine(" * " + remarks);
        topLevelClass.addJavaDocLine(" * @author " + author);
        topLevelClass.addJavaDocLine(" * @date " + format.format(new Date()));
        topLevelClass.addJavaDocLine(" */");
        return true;
    }

    private void closeConnection(Connection connection, ResultSet rs) {
        if (null != rs) {
            try {
                rs.close();
            } catch (SQLException e) {}
        }
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {}
        }

    }
}

