package edu.wpi.grip.ui.pipeline;

import edu.wpi.grip.core.Pipeline;
import edu.wpi.grip.core.Step;
import edu.wpi.grip.core.sockets.InputSocket;
import edu.wpi.grip.core.sockets.OutputSocket;
import edu.wpi.grip.core.sockets.Socket;
import edu.wpi.grip.core.sockets.SocketHint;
import edu.wpi.grip.ui.Controller;
import edu.wpi.grip.ui.annotations.ParametrizedController;
import edu.wpi.grip.ui.components.ExceptionWitnessResponderButton;
import edu.wpi.grip.ui.dragging.StepDragService;
import edu.wpi.grip.ui.pipeline.input.InputSocketController;
import edu.wpi.grip.ui.pipeline.input.InputSocketControllerFactory;
import edu.wpi.grip.ui.util.ControllerMap;
import edu.wpi.grip.ui.util.StyleClassNameUtility;

import com.google.inject.assistedinject.Assisted;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Labeled;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import javax.inject.Inject;

/**
 * A JavaFX control that shows a step in the pipeline.  This control shows the name of the operation
 * as well as a list of input sockets and output sockets.
 */
@ParametrizedController(url = "Step.fxml")
public class StepController implements Controller {

  private final Pipeline pipeline;
  private final InputSocketControllerFactory inputSocketControllerFactory;
  private final OutputSocketController.Factory outputSocketControllerFactory;
  private final ExceptionWitnessResponderButton.Factory exceptionWitnessResponderButtonFactory;
  private final StepDragService stepDragService;
  private final Step step;
  private final List<InputSocketController> inputSockets;
  private boolean expanded = true;
  @FXML
  private VBox root;
  @FXML
  private Labeled title;
  @FXML
  private ImageView icon;
  @FXML
  private HBox buttons;
  @FXML
  private VBox inputs;
  @FXML
  private VBox outputs;
  @FXML
  private ImageView expandIcon;
  @FXML
  private Button expand;
  private ControllerMap<InputSocketController, Node> inputSocketMapManager;
  private ControllerMap<OutputSocketController, Node> outputSocketMapManager;

  @Inject
  StepController(Pipeline pipeline,
                 InputSocketControllerFactory inputSocketControllerFactory,
                 OutputSocketController.Factory outputSocketControllerFactory,
                 ExceptionWitnessResponderButton.Factory exceptionWitnessResponderButtonFactory,
                 StepDragService stepDragService,
                 @Assisted Step step) {
    this.pipeline = pipeline;
    this.inputSocketControllerFactory = inputSocketControllerFactory;
    this.outputSocketControllerFactory = outputSocketControllerFactory;
    this.exceptionWitnessResponderButtonFactory = exceptionWitnessResponderButtonFactory;
    this.stepDragService = stepDragService;
    this.step = step;
    inputSockets = new ArrayList<>();
  }

  @FXML
  private void initialize() {
    inputSocketMapManager = new ControllerMap<>(inputs.getChildren());
    outputSocketMapManager = new ControllerMap<>(outputs.getChildren());

    root.getStyleClass().add(StyleClassNameUtility.classNameFor(step));
    title.setText(step.getOperationDescription().name());
    step.getOperationDescription().icon().ifPresent(icon -> this.icon.setImage(
        new Image(InputStream.class.cast(icon))));
    buttons.getChildren().add(0, exceptionWitnessResponderButtonFactory.create(step, "Step Error"));

    if (step.getInputSockets().stream()
        .allMatch(inputSocket -> inputSocket.getSocketHint().getView()
            .equals(SocketHint.View.NONE))){
      expand.setManaged(false);
    } else {
      expandIcon.setImage(new Image("/edu/wpi/grip/ui/icons/up.png"));
    }

    // Add a SocketControlView for each input socket and output socket
    for (InputSocket<?> inputSocket : step.getInputSockets()) {
      InputSocketController tempSocket = inputSocketControllerFactory.create(inputSocket);
      inputSocketMapManager.add(tempSocket);
      if(!inputSocket.getSocketHint().getView().equals(SocketHint.View.NONE)) {
        inputSockets.add(tempSocket);
      }
    }

    for (OutputSocket<?> outputSocket : step.getOutputSockets()) {
      outputSocketMapManager.add(outputSocketControllerFactory.create(outputSocket));
    }

    root.setOnDragDetected(event -> {
      stepDragService.beginDrag(this.step, root, "step");
      event.consume();
    });

    root.setOnDragDone(event -> {
      stepDragService.completeDrag();
      event.consume();
    });

  }

  /**
   * An unmodifiable collection of {@link InputSocketController}s corresponding to the input sockets
   * of this step.
   */
  public Collection<InputSocketController> getInputSockets() {
    return inputSocketMapManager.keySet();
  }

  /**
   * An unmodifiable collection of {@link InputSocketController}s corresponding to the output
   * sockets of this step.
   */
  public Collection<OutputSocketController> getOutputSockets() {
    return outputSocketMapManager.keySet();
  }

  public VBox getRoot() {
    return root;
  }

  public Step getStep() {
    return step;
  }

  @FXML
  private void deleteStep() {
    pipeline.removeStep(step);
  }

  @FXML
  private void moveStepLeft() {
    pipeline.moveStep(step, -1);
  }

  @FXML
  private void moveStepRight() {
    pipeline.moveStep(step, +1);
  }

  @FXML
  private void expand() {
    if (expanded) {
      for (InputSocketController input : inputSockets) {
        if (input.getSocket().getConnections().isEmpty()) {
          inputSocketMapManager.remove(input);
        }
      }
      expandIcon.setImage(new Image("/edu/wpi/grip/ui/icons/down.png"));
      expanded = false;
    } else {
      for (InputSocketController input : inputSockets) {
        if (!inputSocketMapManager.containsKey(input)) {
          inputSocketMapManager.add(input);
        }
      }
      expandIcon.setImage(new Image("/edu/wpi/grip/ui/icons/up.png"));
      expanded = true;
    }

  }

  /**
   * Used for assisted injects.  Guice will automatically create an instance of this interface so we
   * can create step controllers.  This lets us use injection with StepController even though it
   * requires a {@link Step} (which is not an injected dependency).
   */
  public interface Factory {
    StepController create(Step step);
  }
}
