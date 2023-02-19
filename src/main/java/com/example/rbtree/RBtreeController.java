package com.example.rbtree;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Background;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class RBtreeController {

    static final int countOfNodes = 30;
    @FXML
    private Label NodesCountText;
    @FXML
    private  AnchorPane AnchorPaneMain;

    @FXML
    private TextField DeleteText;

    @FXML
    private TextField InsertText;

    @FXML
    private TextField ResultText;

    @FXML
    private TextField SearchText;

    static final String url = "jdbc:mysql://localhost:3306/mydbtest";
    static final String user = "root";
    static final String password = "admin";
    public static Connection connection;
    public static Statement statement;
    public static ResultSet resultSet;
    static Node root;

    static List<Line> vl = new ArrayList<>();
    static List<Line> hl = new ArrayList<>();
    static List<Button> bl = new ArrayList<>();
    @FXML
    void DeleteClick(ActionEvent event) throws SQLException, IOException {

        String number = DeleteText.getText();
        DeleteText.setText(number.trim());
        if(!(number = DeleteText.getText()).isEmpty()){
            setOp();
            boolean result = delete(Integer.parseInt(number));
            DeleteText.clear();
            printTree();
            if (result)
                NodesCountText.setText(Integer.toString(Integer.parseInt(NodesCountText.getText())-1));
        }
    }
    static boolean delete(int number) throws SQLException {
        if(search(number)){

            int countNodes = 0;
            Stack<Node> stack = new Stack<>();
            Node current = root;
            while (current != null || !stack.empty()) {
                while (current != null) {
                    stack.push(current); //заповнюємо стек
                    current = current.left; //переміщаємо вузол на лівого нащадка
                }
                if(Integer.parseInt(bl.get(countNodes).getText()) == number){
                    vl.get(countNodes).setOpacity(0);
                    hl.get(countNodes).setOpacity(0);
                    bl.get(countNodes).setOpacity(0);
                    break;
                }
                countNodes++;
                current = stack.lastElement().right; //переміщаємо вузол на правого нащадка
                stack.pop(); //видаляємо останній елемент стека
            }

            Node temp = root;
            while(temp.data!=number){
                String query = "select number from T"+temp.data;
                resultSet = statement.executeQuery(query);
                resultSet.next();
                if(resultSet.getInt("number")>number)
                    temp = temp.left;
                else
                    temp = temp.right;
            }

            if(temp.left == null && temp.right == null && temp.parent==null){
                statement.executeUpdate("drop table t"+root.data);
                root = null;
            }else{
                if(temp.left == null || temp.right == null){
                    eraseNode(temp);
                }

                else{
                    Node lastNode = temp.right;
                    Node minNode = lastNode;
                    while (lastNode != null) {
                        minNode = lastNode;
                        lastNode = lastNode.left;
                    }
                    int oldName = temp.data;
                    eraseNode(minNode);
                    temp.data = minNode.data;

                    statement.executeUpdate("rename table t"+oldName+" to t"+temp.data);
                    statement.executeUpdate("update t"+temp.data+" set number = "+temp.data);
                }
            }
            return true;
        }else return false;
    }

    static void eraseNode(Node node) throws SQLException {
        char oldColor = node.color;
        Node newNode;
        if(node.left!=null){
            replacingNodes(node.parent, node, node.left);
            changeLevel(node.left, false);
            newNode = node.left;
        }
        else if(node.right!=null){
            replacingNodes(node.parent, node, node.right);
            changeLevel(node.right, false);
            newNode = node.right;
        }
        else{
            newNode = new Node(0, node.level+1, 'B');
            newNode.parent = node;
            statement.executeUpdate("update t"+newNode.data+" set parentId = "+1+";");
            statement.executeUpdate("alter table T" + newNode.data + " add foreign key (parentId) references T" + node.data + " (id);");

            replacingNodes(node.parent, node, newNode);
        }
        if(oldColor == 'B'){
            repairTree(newNode);
        }
        if(newNode.data == 0){
            statement.executeUpdate("drop table t"+newNode.data);
            if(newNode.parent!=null){
                if(newNode.parent.left == newNode)
                    newNode.parent.left = null;
                else newNode.parent.right = null;
                newNode.parent = null;
            }
        }
    }
    static void repairTree(Node node) throws SQLException {
        if (node == root) {
            node.color = 'B';
            statement.executeUpdate("update t"+node.data+" set color = '"+node.color+"';");
            return;
        }
        if (node.parent != null) {
            Node sib = node.parent.left == node ? node.parent.right : node.parent.left;
            if (sib != null) {
                if (sib.color == 'R') {
                    sib.color = 'B';
                    node.parent.color = 'R';
                    statement.addBatch("update t"+sib.data+" set color = '"+sib.color+"';");
                    statement.addBatch("update t"+node.parent.data+" set color = '"+node.parent.color+"';");
                    statement.executeBatch();
                    statement.clearBatch();
                    if (node == node.parent.left)
                        leftRotate(node.parent);
                    else rightRotate(node.parent);

                    sib = node.parent.left == node ? node.parent.right : node.parent.left;
                }

                if ((sib.left == null && sib.right == null) || (sib.left != null && sib.right != null && sib.left.color == 'B' && sib.right.color == 'B')) {
                    sib.color = 'R';
                    statement.executeUpdate("update t"+sib.data+" set color = '"+sib.color+"';");
                    if (node.parent.color == 'R') {
                        node.parent.color = 'B';
                        statement.executeUpdate("update t"+node.parent.data+" set color = '"+node.parent.color+"';");
                    }
                    else {
                        repairTree(node.parent);
                    }
                } else {
                    if (node == node.parent.left && (sib.right == null || sib.right.color == 'B')) {
                        if (sib.left != null) {
                            sib.left.color = 'B';
                            statement.executeUpdate("update t"+sib.left.data+" set color = '"+sib.left.color+"';");
                        }
                        sib.color = 'R';
                        statement.executeUpdate("update t"+sib.data+" set color = '"+sib.color+"';");
                        rightRotate(sib);
                        sib = node.parent.right;
                    } else if (node == node.parent.right && (sib.left == null || sib.left.color == 'B')) {
                        if (sib.right != null) {
                            sib.right.color = 'B';
                            statement.executeUpdate("update t"+sib.right.data+" set color = '"+sib.right.color+"';");
                        }
                        sib.color = 'R';
                        statement.executeUpdate("update t"+sib.data+" set color = '"+sib.color+"';");
                        leftRotate(sib);
                        sib = node.parent.left;
                    }

                    sib.color = node.parent.color;
                    statement.executeUpdate("update t"+sib.data+" set color = '"+sib.color+"';");
                    node.parent.color = 'B';
                    statement.executeUpdate("update t"+node.parent.data+" set color = '"+node.parent.color+"';");
                    if (node == node.parent.left) {
                        sib.right.color = 'B';
                        statement.executeUpdate("update t"+sib.right.data+" set color = '"+sib.right.color+"';");
                        leftRotate(node.parent);
                    } else {
                        sib.left.color = 'B';
                        statement.executeUpdate("update t"+sib.left.data+" set color = '"+sib.left.color+"';");
                        rightRotate(node.parent);
                    }

                }
            }

        }


    }
    static void replacingNodes(Node oldParent, Node node, Node newNode) throws SQLException {
        if(oldParent!=null){
            if(oldParent.right == node)
                oldParent.right = newNode;
            else if(oldParent.left == node)
                oldParent.left = newNode;
        }else root = newNode;


        resultSet = statement.executeQuery("select parentId from T"+node.data);
        resultSet.next();
        if(resultSet.getInt("parentId")==1){
            statement.executeUpdate("alter table T"+node.data+" drop foreign key T"+node.data+"_ibfk_1;");
        }

        if(newNode!=null) {
            statement.executeUpdate("alter table T" + newNode.data + " drop foreign key T" + newNode.data + "_ibfk_1;");
            if(oldParent!=null) {
                statement.executeUpdate("alter table T" + newNode.data + " add foreign key (parentId) references T" + oldParent.data + " (id);");
            }
        }
        statement.executeUpdate("drop table T"+node.data);
        node = newNode;
        if(node != null){
            node.parent = oldParent;
        }
        if (root == node) statement.executeUpdate("update t"+root.data+" set parentId = "+0);
    }

    @FXML
    void InsertClick(ActionEvent event) throws SQLException, IOException {
        if(Integer.parseInt(NodesCountText.getText())<countOfNodes){

            String number = InsertText.getText();
            InsertText.setText(number.trim());
            number = InsertText.getText();
            if(!number.isEmpty() && !number.equals("0") && !number.contains("-")){
                setOp();
                boolean result = insert(Integer.parseInt(number));
                InsertText.clear();
                printTree();
                if (result)
                    NodesCountText.setText(Integer.toString(Integer.parseInt(NodesCountText.getText())+1));
            }
        }
    }

    static boolean insert(int number) throws SQLException {
        if (!search(number)) {
            Node temp = root;
            int count = 0;
            Node lastNode = root;
            while (temp != null) {
                count++;
                lastNode = temp;
                if (number > temp.data)
                    temp = temp.right;
                else if (number < temp.data)
                    temp = temp.left;
            }

            Node newNode = new Node(number, count, 'R');
            newNode.parent = lastNode;

            if (lastNode != null) {
                statement.executeUpdate("update T"+newNode.data+" set parentId = "+1+";");
                statement.executeUpdate("alter table T"+newNode.data+" add foreign key (parentId) references T"+lastNode.data+" (id);");
                if (newNode.data > lastNode.data)
                    lastNode.right = newNode;
                else lastNode.left = newNode;
            } else root = newNode;


            treeColorEditing(newNode);
            return true;
        }
        return false;
    }

    //балансування
    static void treeColorEditing(Node node) throws SQLException {
        // новий вузол == корінь
        Node parent = node.parent; //батько
        String sql;
        if(parent==null) {
            node.color = 'B';
            sql = "update T"+node.data+" set color = '"+node.color+"';";
            statement.executeUpdate(sql);
            return;
        }

        //Якщо батько чорний, то виходимо з методу
        if(parent.color == 'B')
            return;

        //якщо в дереві два елемента, перший з яких - червоний (корінь), тобто початок програми
        Node gParent = parent.parent;
        if(gParent==null){
            parent.color = 'B';
            sql = "update T"+parent.data+" set color = '"+parent.color+"';";
            statement.executeUpdate(sql);
            return;
        }

        //визначаємо дядька
        Node uncle;
        if(parent==gParent.left)
            uncle = gParent.right;
        else if(parent==gParent.right)
            uncle = gParent.left;
        else uncle = null;

        //якщо батько і дядько червоні
        if(uncle!=null && uncle.color == 'R'){
            parent.color = 'B';
            gParent.color = 'R';
            uncle.color = 'B';
            statement.addBatch("update T"+parent.data+" set color = '"+parent.color+"';");
            statement.addBatch("update T"+gParent.data+" set color = '"+gParent.color+"';");
            statement.addBatch("update T"+uncle.data+" set color = '"+uncle.color+"';");
            statement.executeBatch();
            statement.clearBatch();
            //можемо пофарбувати в червоний корінь, тому викличемо рекурсію
            treeColorEditing(gParent);
        }

        else if(gParent.right == parent){
            if(parent.left == node){
                rightRotate(parent);
                parent = node;
            }
            leftRotate(gParent);
            parent.color = 'B';
            gParent.color = 'R';
            statement.addBatch("update T"+parent.data+" set color = '"+parent.color+"';");
            statement.addBatch("update T"+gParent.data+" set color = '"+gParent.color+"';");
            statement.executeBatch();
            statement.clearBatch();
        }else{
            if(parent.right == node){
                leftRotate(parent);
                parent = node;
            }
            rightRotate(gParent);
            parent.color = 'B';
            gParent.color = 'R';
            statement.addBatch("update T"+parent.data+" set color = '"+parent.color+"';");
            statement.addBatch("update T"+gParent.data+" set color = '"+gParent.color+"';");
            statement.executeBatch();
            statement.clearBatch();
        }

    }

    static void setOp(){
        for(int i = 0;i<countOfNodes;i++){
            vl.get(i).setOpacity(0);
            hl.get(i).setOpacity(0);
            bl.get(i).setOpacity(0);
        }
    }
    static void printTree() throws SQLException, IOException {
        Node current = root; //поточний вузол
        int startX = 350;
        int startY = 150;
        if(current == null){
            System.out.println("tree is empty!");
            Label tempLabel = new Label("Tree is empty!");
            tempLabel.setAlignment(Pos.CENTER);
            tempLabel.setFont(new Font("System", 26));
        }
        else{
            int countPrint = 0;
            Stack<Node> temp = new Stack<>();
            int start = current.data; //визначаємо корінь
            while (current != null || !temp.empty()) {
                while (current != null) {
                    temp.push(current); //заповнюємо стек
                    current = current.left; //переміщаємо вузол на лівого нащадка
                }

                resultSet = statement.executeQuery("select number, color from t"+ temp.lastElement().data);
                resultSet.next();

                int curX = countPrint*60, curY = temp.lastElement().level*100;

                vl.get(countPrint).setStartX(startX+curX);
                vl.get(countPrint).setStartY(startY+curY);
                vl.get(countPrint).setEndX(startX+curX);
                vl.get(countPrint).setEndY(startY+90+curY);
                vl.get(countPrint).setOpacity(1);

                hl.get(countPrint).setStartX(startX-25+curX);
                hl.get(countPrint).setStartY(startY+curY);
                hl.get(countPrint).setEndX(startX+25+curX);
                hl.get(countPrint).setEndY(startY+curY);
                hl.get(countPrint).setOpacity(1);

                bl.get(countPrint).setPrefWidth(50);
                bl.get(countPrint).setPrefHeight(50);
                bl.get(countPrint).setText(Integer.toString(resultSet.getInt("number")));
                bl.get(countPrint).setFont(new Font("System", 20));
                bl.get(countPrint).setAlignment(Pos.CENTER);
                bl.get(countPrint).setTextFill(Color.WHITE);

                if(resultSet.getString("color").equals("B"))
                    bl.get(countPrint).setBackground(Background.fill(Color.BLACK));
                else bl.get(countPrint).setBackground(Background.fill(Color.RED));

                bl.get(countPrint).setLayoutX(startX-25+curX);
                bl.get(countPrint).setLayoutY(startY+65+curY);
                bl.get(countPrint).setOpacity(1);



                if (temp.lastElement().data < start || temp.lastElement().data > start) { //форматоване виведення
                    for (int i = 0; i < temp.lastElement().level; i++) {
                        System.out.print("     ");
                    }
                    System.out.println("╠════"+ resultSet.getInt("number") + "-" + resultSet.getString("color"));
                }
                else System.out.println("════╡" + resultSet.getInt("number") + "-" + resultSet.getString("color")); //+"-"+temp.lastElement().level
                countPrint++;
                current = temp.lastElement().right; //переміщаємо вузол на правого нащадка
                temp.pop(); //видаляємо останній елемент стека
            }
            System.out.println("_______________________");
        }
    }

    @FXML
    void SearchClick(ActionEvent event) {
        String number = SearchText.getText();
        SearchText.setText(number.trim());
        if(!(number = SearchText.getText()).isEmpty()){
            ResultText.setText(Boolean.toString(search(Integer.parseInt(number))));
        }else ResultText.clear();
    }

    static boolean search(int number){
        Node temp = root;
        if (temp == null)
            return false;
        while(temp.data!=number){
            if(number>temp.data)
                temp = temp.right;
            else temp = temp.left;
            if (temp == null)
                return false;
        }
        return true;
    }

    static void leftRotate(Node node) throws SQLException {
        Node right = node.right;

        right.level--;
        changeLevel(right.right, false);
        node.level++;
        changeLevel(node.left, true);

        node.right = right.left;
        if(right.left!=null)
            right.left.parent = node;
        right.left = node;

        Node oldParent = node.parent;

        resultSet = statement.executeQuery("select parentId from T"+node.data);
        resultSet.next();
        if(resultSet.getInt("parentId")==1){
            statement.addBatch("alter table T"+node.data+" drop foreign key T"+node.data+"_ibfk_1;");
        }else{
            statement.addBatch("update T"+node.data+" set parentId = "+1+";");
        }
        statement.addBatch("alter table T"+node.data+" add foreign key (parentId) references T"+right.data+" (id);");
        if(node.right!=null){
            statement.addBatch("alter table T"+node.right.data+" drop foreign key T"+node.right.data+"_ibfk_1;");
            statement.addBatch("alter table T"+node.right.data+" add foreign key (parentId) references T"+node.data+" (id);");
        }
        statement.addBatch("alter table T"+right.data+" drop foreign key T"+right.data+"_ibfk_1;");
        statement.addBatch("update T"+right.data+" set parentId = "+0+";");
        node.parent = right;
        if(oldParent!=null){
            statement.addBatch("update T"+right.data+" set parentId = "+1+";");
            statement.addBatch("alter table T"+right.data+" add foreign key (parentId) references T"+oldParent.data+" (id);");
            if(oldParent.right == node)
                oldParent.right = right;
            else if(oldParent.left == node)
                oldParent.left = right;
        }
        else root = right;
        statement.executeBatch();
        statement.clearBatch();
        right.parent = oldParent;
    }
    static void rightRotate(Node node) throws SQLException {
        Node left = node.left;

        left.level--;
        changeLevel(left.left, false);
        node.level++;
        changeLevel(node.right, true);

        node.left = left.right;
        if (left.right != null)
            left.right.parent = node;
        left.right = node;

        Node oldParent = node.parent;

        resultSet = statement.executeQuery("select parentId from T"+node.data);
        resultSet.next();
        if(resultSet.getInt("parentId")==1){
            statement.addBatch("alter table T"+node.data+" drop foreign key T"+node.data+"_ibfk_1;");
        }else{
            statement.addBatch("update T"+node.data+" set parentId = "+1+";");
        }

        statement.addBatch("alter table T"+node.data+" add foreign key (parentId) references T"+left.data+" (id);");
        if(node.left!=null) {
            statement.addBatch("alter table T" + node.left.data + " drop foreign key T" + node.left.data + "_ibfk_1;");
            statement.addBatch("alter table T" + node.left.data + " add foreign key (parentId) references T" + node.data + " (id);");
        }
        statement.addBatch("alter table T"+left.data+" drop foreign key T"+left.data+"_ibfk_1;");
        statement.addBatch("update T"+left.data+" set parentId = "+0+";");
        node.parent = left;
        if(oldParent!=null){
            statement.addBatch("update T"+left.data+" set parentId = "+1+";");
            statement.addBatch("alter table T"+left.data+" add foreign key (parentId) references T"+oldParent.data+" (id);");

            if(oldParent.right == node)
                oldParent.right = left;
            else if(oldParent.left == node)
                oldParent.left = left;
        }
        else root = left;
        statement.executeBatch();
        statement.clearBatch();
        left.parent = oldParent;
    }
    static void changeLevel(Node node, boolean flag){
        Node current = node;
        Stack<Node> temp = new Stack<>();
        while (current != null || !temp.empty()) {
            while (current != null) {
                temp.push(current);
                if(flag)
                    current.level++;
                else current.level--;
                current = current.left;
            }
            current = temp.lastElement().right;
            temp.pop();
        }
    }


    @FXML
    private void initialize() throws SQLException, IOException {
        //AnchorPaneMain = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/com/example/rbtree/RBtree.fxml")));
        connection = DriverManager.getConnection(url, user, password);
        statement = connection.createStatement();
        String query = "show tables from mydbtest;";
        resultSet = statement.executeQuery(query);
        String nameTable;
        while (resultSet.next()) {
            nameTable = resultSet.getString("Tables_in_mydbtest");
            Statement tempSt = connection.createStatement();
            ResultSet tempRS = tempSt.executeQuery("select parentId from "+nameTable);
            tempRS.next();
            if(tempRS.getInt("parentId")==1){
                tempSt.executeUpdate("alter table " + nameTable + " drop foreign key " + nameTable + "_ibfk_1;");
                tempSt.executeUpdate("update " + nameTable + " set parentId = "+0);
            }
        }
        resultSet = statement.executeQuery(query);
        while (resultSet.next()) {
            nameTable = resultSet.getString("Tables_in_mydbtest");
            Statement tempSt = connection.createStatement();
            tempSt.executeUpdate("drop table " + nameTable);
        }
        for(int i = 0;i<countOfNodes;i++){
            Line vertLine = new Line();
            Line horLine = new Line();
            Button number = new Button();
            vertLine.setOpacity(0);
            horLine.setOpacity(0);
            number.setOpacity(0);
            number.setDisable(true);
            vl.add(vertLine);
            hl.add(horLine);
            bl.add(number);
            AnchorPaneMain.getChildren().addAll(vertLine, horLine, number);
        }
    }
}
