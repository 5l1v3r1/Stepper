package com.coreyd97.stepper;

import burp.IContextMenuFactory;
import burp.IContextMenuInvocation;
import burp.IHttpRequestResponse;
import com.coreyd97.stepper.ui.StepPanel;
import com.coreyd97.stepper.ui.StepSequenceTab;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.*;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class ContextMenuFactory implements IContextMenuFactory {

    private final Stepper stepper;

    public ContextMenuFactory(Stepper stepper){
        this.stepper = stepper;
    }

    @Override
    public List<JMenuItem> createMenuItems(IContextMenuInvocation invocation) {
        IHttpRequestResponse[] messages = invocation.getSelectedMessages();
        if(messages.length == 0) return null;
        ArrayList<JMenuItem> menuItems = new ArrayList<>();

        //Add x items to Stepper menu
        String addMenuTitle = String.format("Add %d %s to Stepper", messages.length, messages.length == 1 ? "item":"items");
        JMenu addStepMenu = new JMenu(addMenuTitle);

        for (Map.Entry<String, StepSequenceTab> e : this.stepper.getUI().getAllStepSetTabs().entrySet()) {
            String title = e.getKey();
            StepSequenceTab stepSequenceTab = e.getValue();
            JMenuItem item = new JMenuItem(title);
            item.addActionListener(actionEvent -> {
                for (IHttpRequestResponse message : messages) {
                    stepSequenceTab.getStepSequence().addStep(message);
                }
            });
            addStepMenu.add(item);
        }

        JMenuItem newSequence = new JMenuItem("New Sequence");
        newSequence.addActionListener(actionEvent -> {
            String name = JOptionPane.showInputDialog(Stepper.getInstance().getUI().getUiComponent(), "Enter a name to identify the sequence: ", "", JOptionPane.PLAIN_MESSAGE);
            if(name != null) {
                StepSequence stepSequence = new StepSequence(this.stepper, false, name);
                for (IHttpRequestResponse message : messages) {
                    stepSequence.addStep(message);
                }
                this.stepper.addStepSequence(stepSequence);
            }
        });

        addStepMenu.add(new JPopupMenu.Separator());
        addStepMenu.add(newSequence);

        menuItems.add(addStepMenu);


        if(invocation.getInvocationContext() == IContextMenuInvocation.CONTEXT_MESSAGE_EDITOR_REQUEST){
            menuItems.addAll(buildVariableMenuItems());
        }
        return menuItems;
    }

    private List<JMenuItem> buildVariableMenuItems(){
        List<JMenuItem> menuItems = new ArrayList<>();

        HashMap<StepSequence, HashMap<String, StepVariable>> sequenceVariableMap = new HashMap<>();

        StepSequenceTab selectedStepSet = stepper.getUI().getSelectedStepSet();
        boolean isViewingSequenceStep = false;
        if(selectedStepSet != null){
            StepPanel selectedStepPanel = selectedStepSet.getSelectedStepPanel();
            if(selectedStepPanel != null){
                isViewingSequenceStep = true;
                Step step = selectedStepPanel.getStep();
                HashMap<String, StepVariable> stepVariables = selectedStepSet.getStepSequence().getRollingVariablesUpToStep(step);
                sequenceVariableMap.put(step.getSequence(), stepVariables);
            }
        }else{
            //Message editor of another tool. Show all variables!
            sequenceVariableMap = stepper.getLatestVariablesFromAllSequences();
        }

        long varCount = sequenceVariableMap.values().stream().mapToInt(HashMap::size).sum();

        if(varCount > 0) {
            JMenu addStepVariableToClipboardMenu = new JMenu("Add Stepper Variable To Clipboard");
            JMenuItem insertVariable = new JMenuItem("Insert Stepper Variable At Cursor (NOT IMPLEMENTED)");

            if(isViewingSequenceStep){ //Only variables from a single sequence step
                Collection<StepVariable> variables = sequenceVariableMap.values().stream()
                        .map(Map::values).flatMap(Collection::stream).collect(Collectors.toList());

                List<JMenuItem> variableToClipboardMenuItems = buildAddVariableToClipboardMenuItems(variables);

                for (JMenuItem item : variableToClipboardMenuItems) {
                    addStepVariableToClipboardMenu.add(item);
                }
            }else{
                for (Map.Entry<StepSequence, HashMap<String, StepVariable>> entry : sequenceVariableMap.entrySet()) {
                    StepSequence stepSequence = entry.getKey();
                    HashMap<String, StepVariable> stringStepVariableHashMap = entry.getValue();
                    if (stringStepVariableHashMap.size() > 0) {
                        JMenu sequenceItem = new JMenu(stepSequence.getTitle());
                        List<JMenuItem> sequenceVariableToClipboardItems = ContextMenuFactory.this.buildAddVariableToClipboardMenuItems(stringStepVariableHashMap.values());
                        for (JMenuItem item : sequenceVariableToClipboardItems) {
                            sequenceItem.add(item);
                        }
                        addStepVariableToClipboardMenu.add(sequenceItem);
                    }
                }
            }


            menuItems.add(addStepVariableToClipboardMenu);
            //Not implemented yet.
            //menuItems.a
            // dd(insertVariable);
        }
        return menuItems;
    }

    private List<JMenuItem> buildAddVariableToClipboardMenuItems(Collection<StepVariable> variables){
        List<JMenuItem> menuItems = new ArrayList<>();
        for (StepVariable variable : variables) {
            JMenuItem item = new JMenuItem(variable.getIdentifier());
            item.addActionListener(actionEvent -> {
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                        new StringSelection(variable.createVariableString()), null);
            });
            menuItems.add(item);
        }
        return menuItems;
    }
}
