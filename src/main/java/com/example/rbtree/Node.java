package com.example.rbtree;


import java.sql.SQLException;

import static com.example.rbtree.RBtreeController.statement;


public class Node {
    Node left = null;
    Node right = null;
    Node parent = null;

    char color;
    int data;
    int level;

    public Node(int data, int level, char color) throws SQLException {
        this.data = data;
        this.level = level;
        this.color = color;
        String sql = "CREATE TABLE T"+data+
                " (id INT PRIMARY KEY AUTO_INCREMENT, "+
                "parentId INT, "+
                "number INT, "+
                "color VARCHAR(1));";
        statement.executeUpdate(sql);
        sql = "insert into T"+data+" (parentId, number, color) values ("+0+", "+data+", '"+color+"');";
        statement.execute(sql);
    }

    public Node() {
    }
}


