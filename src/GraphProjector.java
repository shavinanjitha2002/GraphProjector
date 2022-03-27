import com.sun.javaws.exceptions.InvalidArgumentException;
import dialog.GridLimitDialog;
import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Line;
import javafx.scene.shape.Path;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.StringConverter;
import util.GraphFunction;
import util.GridSystem;
import util.Point;
import util.SimpleLine;
import widgets.FunctionCell;
import widgets.Rule;
import widgets.TangentLineBox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;


public class GraphProjector extends Application {

    // static data members
    private static final double posLineLength = 30;

    private  Pane canvas;
    private Slider mainSlider;
    private TextField mainFunctionBox;
    private Label XYLabel;
    private TangentLineBox tangentLineBox;
    private Text functionPositionText;

    //rules of the canvas
    Rule xRule;
    Rule yRule;
    // declare the main paint tools
    private Line xLine;
    private Line yLine;
    private Line tangentLine;
    private Ellipse functionPositionOval;
    private Rectangle XYBox;
    // grid system
    private GridSystem gridSystem;
    // declare the function list
    private ObservableList<GraphFunction> functionList;
    private GraphFunction currentFunction = null;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        // create the function list
        functionList = FXCollections.observableArrayList();

        // create the main layout widget of the window
        final BorderPane borderPane = new BorderPane();
        borderPane.getStylesheets().add(getClass().getResource("style.css").toExternalForm());

        // set the display panel
        setUpRight(borderPane);
        // setuo the center
        setUpCenter(borderPane);
        // setup the tool box
        setUpToolBox(borderPane);
        // setup menu
        setUpMenu(borderPane);


        // create the scene and add the border pane
        final Scene scene = new Scene(borderPane, 1900, 1050 ,Color.rgb(50, 50, 50));
        primaryStage.setTitle("Graph Projector v0.01");
        primaryStage.setScene(scene);
        primaryStage.setFullScreen(false);

        primaryStage.setX(0);
        primaryStage.setY(0);

        primaryStage.show();


    }

    private final void setUpMenu(BorderPane borderPane){
        // create the menu bar
        final MenuBar menuBar = new MenuBar();
        borderPane.setTop(menuBar);
        // create the menu for menu bar
        final Menu optionMenu = new Menu("Option");


        // add to the menu bar
        menuBar.getMenus().addAll(optionMenu);

    }

    private void setUpRight(BorderPane borderPane){
        // create the vbox for pack the all of the items
        final VBox vBox = new VBox(5);
        // set the padding
        vBox.setPadding(new Insets(5));

        Label label1 = new Label("Current Position");
        label1.setTextAlignment(TextAlignment.LEFT);
        // create the XY label for display the current position in the screen
        XYLabel = new Label("(0, 0)");
        XYLabel.setMinWidth(150);
        XYLabel.setTextAlignment(TextAlignment.CENTER);
        XYLabel.setPadding(new Insets(2, 20, 2, 20));
        XYLabel.setId("XYLabel");
        XYLabel.setFont(new Font("verdana", 14));

        // create the horizontal seperator
        final Separator separator1 = new Separator();
        separator1.setPadding(new Insets(10, 0, 10, 0));
        separator1.setOrientation(Orientation.HORIZONTAL);

        final Label label2 = new Label("Tangent Line");
        label2.setTextAlignment(TextAlignment.LEFT);
        // create the tangent line node object
        tangentLineBox = new TangentLineBox();

        // create the another seperator
        final Separator separator2 = new Separator();
        // create the label
        final Label label3 = new Label("function position");
        label3.setTextAlignment(TextAlignment.LEFT);

        // create the label for X ana Y
        functionPositionText = new Text(String.format("X : %.3f%nY : %.3f", 0.0, 0.0));
        functionPositionText.setFont(new Font("verdana", 25));
        functionPositionText.setFill(Color.ORANGE);

        // add to the vbox
        vBox.getChildren().addAll(label1, XYLabel, separator1, label2, tangentLineBox, separator2, label3,
                                    functionPositionText);


        setUpFunctionList(vBox);
        // set the border pane left as the vbox
        borderPane.setRight(vBox);
        BorderPane.setMargin(vBox, new Insets(10));
    }

    private final void setUpCenter(BorderPane borderPane){

        // create the vbox for pack the slider and canvas
        final VBox vBox = new VBox(10);
        // create the grid for pack the rukes and canvas
        final GridPane gridPane = new GridPane();
        gridPane.setHgap(0);
        gridPane.setVgap(0);
        gridPane.setPadding(new Insets(0));
        // create the main canvas widget for all drawing capabilities
        canvas = new Pane();
        canvas.setId("canvas");
        //set uo the grid system
        setUpGrid();

        // create the x and y rules
        xRule = new Rule(Orientation.HORIZONTAL, gridSystem);
        yRule = new Rule(Orientation.VERTICAL, gridSystem);

        gridPane.add(yRule, 0, 0, 1, 2);
        gridPane.add(canvas, 1, 0);
        gridPane.add(xRule, 1, 1);
        // create the scroll area for set up the canvas
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setMaxSize(1400, 1000);
        scrollPane.setContent(gridPane);
        //set the scroll bar policy
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        //set the border pane center

        Popup canvasPopup = createCanvasPopup();

        canvas.setOnMouseMoved(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                drawCurrentLine(event);
                // update th rules
                xRule.update(event);
                yRule.update(event);
            }
        });

        canvas.setOnMousePressed(event -> {
            Point gridPoint = gridSystem.translateToGrid(new Point(event.getX(), event.getY()));
            // show the canvas popup
            Point2D point = canvas.localToScreen(event.getX(), event.getY());
            ((Text) canvasPopup.getContent().get(1)).setText(String.format("X : %.2f\nY : %.2f",
                                                            gridPoint.getX() , gridPoint.getY()));
            canvasPopup.show(canvas, point.getX(), point.getY());
        });

        HBox sliderBox = setUpSlider();
        vBox.getChildren().addAll(scrollPane, sliderBox);
        // set the vbox as the center fo the border pane
        borderPane.setCenter(vBox);

    }

    // setup the slider
    private final HBox setUpSlider(){
        // create the hox for pack the widgets
        final HBox hbox = new HBox(10);
        // create the main slider of the window
        mainSlider = new Slider(gridSystem.getX1() , gridSystem.getX2() , gridSystem.getX1());
        mainSlider.prefWidthProperty().bind(hbox.prefWidthProperty().subtract(150));
        mainSlider.setOrientation(Orientation.HORIZONTAL);
        mainSlider.setMinorTickCount(1);
        mainSlider.setDisable(true);
        // create the slider value box
        final Label sliderLabel = new Label(String.format("X : %.2f", gridSystem.getX1()));

        // add the slider value property linstner
        mainSlider.valueProperty().addListener(
                new ChangeListener<Number>() {
                    @Override
                    public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                        // set the label text
                        setSliderText(sliderLabel , newValue);
                        try {
                            updateTangentLine(newValue);
                        } catch (NullPointerException e) {
                            System.out.println();
                        }
                    }
                }
        );

        // add the slider and label
        hbox.getChildren().addAll(mainSlider, sliderLabel);
        hbox.setPadding(new Insets(10, 5, 25, 5));
        hbox.setPrefWidth(gridSystem.getWidth() - 50);

        return hbox;
    }

    private final void setUpBottom(BorderPane borderPane){
        // create the hox for pack the widgets
        final HBox hbox = new HBox(10);
        // create the main slider of the window
        mainSlider = new Slider(gridSystem.getX1() , gridSystem.getX2() , gridSystem.getX1());
        mainSlider.prefWidthProperty().bind(hbox.prefWidthProperty().subtract(150));
        mainSlider.setOrientation(Orientation.HORIZONTAL);
        mainSlider.setMinorTickCount(1);

        // create the slider value box
        final Label sliderLabel = new Label(String.format("X : %.2f", gridSystem.getX1()));

        // add the slider value property linstner
        mainSlider.valueProperty().addListener(
                new ChangeListener<Number>() {
                    @Override
                    public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                        // set the label text
                        setSliderText(sliderLabel , newValue);
                        try {
                            updateTangentLine(newValue);
                        } catch (NullPointerException e) {
                            e.printStackTrace();
                        }
                    }
                }
        );

        // add the slider and label
        hbox.getChildren().addAll(mainSlider, sliderLabel);
        hbox.setPadding(new Insets(10, 5, 25, 5));
        hbox.setPrefWidth(1400);
        // add to the main layout
        borderPane.setBottom(hbox);
    }

    private final void setUpToolBox(BorderPane borderPane){
        // create the mein vbox for this
        final VBox vBox = new VBox(20);
        vBox.setPrefWidth(400);
        // set the paddiing
        vBox.setPadding(new Insets(10));

        // call to the other builders
        setUpFunctionBox(vBox);
        // set up the zomm box
        setUpZoomBox(vBox);
        // create the grid limits change button
        final Button changeLimitsButton = new Button("Change Limits");
        changeLimitsButton.setOnAction((event -> {
            // call to the grid change dialog box
            final GridLimitDialog gridLimitDialog = new GridLimitDialog(gridSystem);
            Optional<double[]> array = gridLimitDialog.showAndWait();

            if (array.isPresent()){
                // call to the update method
                updateCanvas();
            }
        }));
        vBox.getChildren().add(changeLimitsButton);

        // set the left side
        borderPane.setLeft(vBox);
    }

    private void setUpFunctionList(VBox vBox){
        // create the title label
        final Label titleLabel = new Label("Function List");
        titleLabel.setTextAlignment(TextAlignment.LEFT);
        // grid pane for pack the buttons and lit view
        final GridPane gridPane = new GridPane();
        gridPane.setId("functionListGrid");
        gridPane.setVgap(10);
        gridPane.setPadding(new Insets(15));

        final ListView<GraphFunction> graphFunctionListView = new ListView<>(functionList);
        graphFunctionListView.setId("functionBox");
//        graphFunctionListView.setMinWidth(250);
        graphFunctionListView.setCellFactory(new Callback<ListView<GraphFunction>, ListCell<GraphFunction>>() {
            @Override
            public ListCell<GraphFunction> call(ListView<GraphFunction> param) {
                return new FunctionCell();
            }
        });
        graphFunctionListView.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                // set the current function
                if (graphFunctionListView.getSelectionModel().getSelectedItem() != null){
                    currentFunction = graphFunctionListView.getSelectionModel().getSelectedItem();
                }

            }
        });

        // create the delete button
        final Button deleteButton = new Button("delete");
        deleteButton.setId("deleteButton");
        // set the action to the delete button
        deleteButton.setOnAction((event -> {
            // call to the delete the function from the list view
            if (graphFunctionListView.getSelectionModel().getSelectedItem() != null){
                deleteFunction(graphFunctionListView.getSelectionModel().getSelectedItem());
            }
        }));
        // add to the grid pane
        gridPane.add(graphFunctionListView, 0, 0, 2, 1);
        gridPane.add(deleteButton, 0, 1);

        vBox.getChildren().addAll(titleLabel , gridPane);
    }

    private final void setUpFunctionBox(VBox vBox){
        // create the group box
        // create the line edit and add button for this
        mainFunctionBox = new TextField();
        mainFunctionBox.prefWidthProperty().bind(vBox.prefWidthProperty().subtract(30));
        mainFunctionBox.setPrefHeight(40);
        mainFunctionBox.setPromptText("Enter a function");

        // create the color chooser
        final ColorPicker colorPicker = new ColorPicker(Color.ORANGE);
        // create the hbox
        final HBox hBox = new HBox(mainFunctionBox, colorPicker);
        hBox.setSpacing(10);

        // draw button
        final Button drawButton = new Button("Draw");
        drawButton.prefWidthProperty().bind(vBox.prefWidthProperty().subtract(30));
        drawButton.setPadding(new Insets(10, 10, 10, 10));
        drawButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                try {
                    // draw the funcition
                    String functionText = mainFunctionBox.getText();
                    GraphFunction function = new GraphFunction(functionText , gridSystem, canvas);
                    function.setColor(colorPicker.getValue());
                    addFunction(function , true);
                } catch (NumberFormatException e) {
                    // show the alert mesage
                    Alert warningBox =new Alert(Alert.AlertType.ERROR);
                    warningBox.setTitle("Function Format");
                    warningBox.setContentText("Please Enter the valid function for drawing!!!");
                    warningBox.showingProperty();
                }
            }
        });

        // create the new another vbox
        VBox functionBox = new VBox(10);
        functionBox.setId("functionVBox");
        functionBox.getChildren().addAll(hBox , drawButton);

        final Label titleLabel = new Label("Function");
        titleLabel.setTextAlignment(TextAlignment.LEFT);

        vBox.getChildren().addAll(titleLabel, functionBox);
    }

    private final void setUpZoomBox(VBox vBox){
        // create the grid pane for this
        final GridPane gridPane = new GridPane();
        gridPane.setVgap(10);
        gridPane.setHgap(15);
        gridPane.setPrefWidth(350);

        // create the title label
        Label titleLabel = new Label("zoom");
        titleLabel.setTextAlignment(TextAlignment.LEFT);

        // create the zomm slider
        final Slider zoomSlider = new Slider();
        zoomSlider.setOrientation(Orientation.HORIZONTAL);
        zoomSlider.setMin(100);
        zoomSlider.setMax(500);
        zoomSlider.setPrefWidth(350);
        zoomSlider.setLabelFormatter(new StringConverter<Double>() {
            @Override
            public String toString(Double object) {
                return String.format("%.1f %%", object);
            }

            @Override
            public Double fromString(String string) {
                return null;
            }
        });

        // create the label for zoom slider value
        Label zoomLabel = new Label("100%");

        // set the zoom slider actions
        zoomSlider.valueProperty().addListener(
                new ChangeListener<Number>() {
                    @Override
                    public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                        // zooming the canvas
                        zoomCanvas(newValue);
                        // set the zoom slider vale
                        zoomLabel.setText(String.format("%.1f %%", newValue.doubleValue()));
                    }
                }
        );

        // create the zoom in and zoom out buttons
        final Button zoomInButton = new Button("+");
        final Button zoomOutButton = new Button("-");

        zoomInButton.setId("zoomButton");
        zoomOutButton.setId("zoomButton");
        // create the setOnaction to the slider
        zoomInButton.setOnAction((event -> {
            if (zoomSlider.getValue() < zoomSlider.getMax() - 20){
                zoomSlider.setValue(zoomSlider.getValue() + 20);

            }
        }));

        zoomOutButton.setOnAction(event -> {
            if (zoomSlider.getValue() > zoomSlider.getMin() + 20){
                zoomSlider.setValue(zoomSlider.getValue() - 20);
            }
        });

        // add to the grid
        gridPane.add(zoomSlider, 0, 0, 2, 1);
        gridPane.add(zoomLabel, 2, 0);
        gridPane.add(zoomInButton, 0, 1);
        gridPane.add(zoomOutButton, 1, 1);

        gridPane.setId("zoomBox");

        vBox.getChildren().addAll(titleLabel, gridPane);

    }

    final private void setSliderText(Label sliderLabel , Number value){
        sliderLabel.setText(String.format("X : %.2f", value.doubleValue()));
    }

    private void setUpGrid(){
        // create the new grid
        try {
            gridSystem = new GridSystem(-10, -10, 10, 10, 1500, 1000);
        } catch (InvalidArgumentException e) {
            e.printStackTrace();
        }
        gridSystem.setCanvas(canvas); // set the canvas
        gridSystem.setAxisColor(Color.BLUE);
        gridSystem.setLineColor(Color.rgb(100, 100, 100, 0.7));
        // draw the grid in the canvas
        gridSystem.draw();
    }

    private void addFunction(GraphFunction function, boolean isDraw){
        // add the new function to function list
        currentFunction = function;
        // add to the list
        functionList.add(function);
        if (isDraw){
            currentFunction.draw();
            // activate the main slider
            mainSlider.setDisable(false);
        }


    }

    private void drawCurrentLine(MouseEvent event){
        if (xLine == null && yLine == null){
            xLine = new Line(event.getX(), event.getY() - posLineLength,
                            event.getX(), event.getY() + posLineLength);
            yLine = new Line(event.getX() - posLineLength, event.getY(),
                            event.getX() + posLineLength, event.getY());

            xLine.setStrokeWidth(1);
            yLine.setStrokeWidth(1);

            xLine.setStroke(Color.LAWNGREEN);
            yLine.setStroke(Color.LAWNGREEN);
            // added to thec anvas
            canvas.getChildren().addAll(xLine, yLine);

        }
        else{
            xLine.setStartX(event.getX());
            xLine.setEndX(event.getX());
            xLine.setStartY(event.getY() - posLineLength);
            xLine.setEndY(event.getY() + posLineLength);

            yLine.setStartY(event.getY());
            yLine.setEndY(event.getY());
            yLine.setStartX(event.getX() - posLineLength);
            yLine.setEndX(event.getX() + posLineLength);
        }

        if (!(canvas.getChildren().contains(xLine) && canvas.getChildren().contains(yLine))){
            xLine.setEndY(gridSystem.getHeight());
            yLine.setEndX(gridSystem.getWidth());
            // add to the canvas
            canvas.getChildren().addAll(xLine, yLine);
        }
        // change the XY label text
        Point tempPoint = gridSystem.translateToGrid(new Point(event.getX(), event.getY()));
        XYLabel.setText(String.format("(%.3f, %.3f)", tempPoint.getX(), tempPoint.getY()));

    }

    private void updateTangentLine(Number value){
        if (currentFunction == null){
            throw new NullPointerException("current function point to the null value.");
        }
        // convert the value to double
        // and create the new tangent line object
        final Point center = currentFunction.getPoint(value.doubleValue());

        // build the new simple line
        final SimpleLine simpleLine = currentFunction.getTangentLine(value.doubleValue());
        Point[] points = simpleLine.getPoints(center);

        // draw the line between these two lines
        Point startPoint = gridSystem.translateToCanvas(points[0]);
        Point endPoint = gridSystem.translateToCanvas(points[1]);

        Point canvasPoint = gridSystem.translateToCanvas(center);
        if (this.tangentLine == null){
            // create the new tangent line
            this.tangentLine = new Line(startPoint.getX(), startPoint.getY(), endPoint.getX(), endPoint.getY());
            this.tangentLine.setStroke(Color.RED);
            this.tangentLine.setStrokeWidth(3);
            this.tangentLine.setSmooth(true);

            // add to the canvas
            canvas.getChildren().add(this.tangentLine);

            // create the function circle
            functionPositionOval = new Ellipse();
            functionPositionOval.setCenterX(canvasPoint.getX());
            functionPositionOval.setCenterY(canvasPoint.getY());
            functionPositionOval.setStrokeWidth(5);
            functionPositionOval.setRadiusX(7);
            functionPositionOval.setRadiusY(7);

            functionPositionOval.setStroke(Color.HOTPINK);
            functionPositionOval.setFill(Color.RED);

            // add to the canvas
            canvas.getChildren().add(functionPositionOval);

            // create the XY box
        }
        else{
            setPoints(this.tangentLine, startPoint, endPoint);
            // set the oval position
            functionPositionOval.setCenterX(canvasPoint.getX());
            functionPositionOval.setCenterY(canvasPoint.getY());
        }

        if (!(canvas.getChildren().contains(tangentLine) && canvas.getChildren().contains(functionPositionOval))){
            canvas.getChildren().addAll(tangentLine, functionPositionOval);
        }
        // update theh tangent line box
        tangentLineBox.setLine(simpleLine);
        // change the functon position text
        functionPositionText.setText(String.format("X : %.2f\nY : %.2f", value.doubleValue() ,
                                                                            currentFunction.getValue(value.doubleValue())));
    }

    private static void setPoints(Line line , Point p1, Point p2){
        if (line != null){
            line.setStartX(p1.getX());
            line.setStartY(p1.getY());

            line.setEndX(p2.getX());
            line.setEndY(p2.getY());
        }
    }

    private final  void zoomCanvas(Number value){
        double factor = value.doubleValue() / 100;
        // first delete the all of canvas system
        canvas.getChildren().clear();

        for (GraphFunction function : functionList){
            function.delete();
        }
        // redraw the grid system
        // set the new value for the grid limits
        try {
            gridSystem.setWidth(GridSystem.getBaseWidth() * factor);
            gridSystem.setHeight(GridSystem.getBasicHeight() * factor);
            gridSystem.refresh();
        } catch (Exception e) {
            e.printStackTrace();
        }
        // draw the all of the function again
        for (GraphFunction function : functionList) {
            function.draw();
        }

        // update the rueles
        xRule.update();
        yRule.update();
    }

    private final void deleteFunction(GraphFunction function){
        if (function != null){
            if (function.equals(currentFunction)){
                currentFunction = null;
            }
            // remove from the list and canvas
            // first remove from the canvas
            Path deletedPath = function.getGraphPath();
            deletedPath.getElements().clear();
            // remove from the canvas
            canvas.getChildren().remove(deletedPath);
            // lastly remove from the list view
            functionList.remove(function);
        }
    }

    private final void updateCanvas(){
        // first delete the canvas grid system and redraw
        canvas.getChildren().clear();
        gridSystem.refresh();
        // delete the all of the function graph
        for (GraphFunction function : functionList){
            function.delete();
            function.draw();
        }
        // update the rule
        xRule.update();
        yRule.update();

    }

    private final Popup createCanvasPopup(){
        // create the new popup widget
        final Popup popup = new Popup();
        popup.setAutoHide(true);
        popup.setHideOnEscape(true);
        // create the popup rectangle
        final Rectangle rect = new Rectangle(0, 0, 100, 80);
        rect.setFill(Color.DARKORANGE);
        rect.setStrokeWidth(0);

        // create the text for this
        final Text position = new Text(10, 40, "");
        position.setFont(Font.font("verdana", 18));
        position.setFill(Color.WHITE);
        // add to the popup
        popup.getContent().addAll(rect, position);

        return popup;
    }

}