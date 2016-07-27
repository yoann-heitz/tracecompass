/**********************************************************************
 * Copyright (c) 2016 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Bruno Roy - Initial API and implementation
 **********************************************************************/
package org.eclipse.tracecompass.internal.lttng2.control.ui.views.dialogs;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.tracecompass.internal.lttng2.control.core.model.LogLevelType;
import org.eclipse.tracecompass.internal.lttng2.control.core.model.TraceJulLogLevel;
import org.eclipse.tracecompass.internal.lttng2.control.ui.Activator;
import org.eclipse.tracecompass.internal.lttng2.control.ui.views.messages.Messages;

/**
 * Dialog box for collecting information about the loggers to enable.
 *
 * @author Bruno Roy
 */
public class GetLoggerInfoDialog extends BaseGetInfoDialog implements IGetLoggerInfoDialog {

    // ------------------------------------------------------------------------
    // Attributes
    // ------------------------------------------------------------------------
    /**
     * A button for selecting the log level (range 0 to level).
     */
    private Button fLogLevelButton;
    /**
     * A button for selecting the specified log level only.
     */
    private Button fLogLevelOnlyButton;
    /**
     * A Combo box for selecting the log level.
     */
    private CCombo fLogLevelCombo;
    /**
     * The selected log level.
     */
    private TraceJulLogLevel fLogLevel;
    /**
     * The type of the log level (loglevel or loglevel-only)
     */
    private LogLevelType fLogLevelType;

    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------
    /**
     * Constructor of dialog box.
     *
     * @param shell
     *            the shell for the dialog box
     */
    public GetLoggerInfoDialog(Shell shell) {
        super(shell);
    }

    // ------------------------------------------------------------------------
    // Accessors
    // ------------------------------------------------------------------------
    @Override
    public TraceJulLogLevel getLogLevel() {
        return fLogLevel;
    }
    @Override
    public LogLevelType getLogLevelType() {
        return fLogLevelType;
    }

    // ------------------------------------------------------------------------
    // Operations
    // ------------------------------------------------------------------------

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText(Messages.TraceControl_EnableLoggersDialogTitle);
        newShell.setImage(Activator.getDefault().loadIcon(TARGET_NEW_CONNECTION_ICON_FILE));
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        super.createDialogArea(parent);
        // Main dialog panel
        Composite dialogComposite = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout(1, true);
        dialogComposite.setLayout(layout);
        dialogComposite.setLayoutData(new GridData(GridData.FILL_BOTH));

        fSessionsCombo.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                fSessionIndex = fSessionsCombo.getSelectionIndex();
                fLogLevelCombo.setEnabled(fSessionIndex >= 0); // not sure what is the best method index, string or other
                fLogLevelButton.setEnabled(fSessionsCombo.getText() != null);
                fLogLevelOnlyButton.setEnabled(!fSessionsCombo.getText().isEmpty());
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
            }
        });
        // Create the log level group
        Group logLevelGroup = new Group(dialogComposite, SWT.SHADOW_NONE);
        layout = new GridLayout(2, true);
        logLevelGroup.setLayout(layout);
        logLevelGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        GridData data = new GridData(GridData.FILL_HORIZONTAL);

        fLogLevelButton = new Button(logLevelGroup, SWT.RADIO);
        fLogLevelButton.setText(Messages.TraceControl_EnableEventsLogLevelTypeName);
        fLogLevelButton.setToolTipText(Messages.TraceControl_EnableEventsLogLevelTypeTooltip);
        data = new GridData(GridData.FILL_BOTH);
        fLogLevelButton.setLayoutData(data);
        fLogLevelButton.setSelection(true);

        fLogLevelOnlyButton = new Button(logLevelGroup, SWT.RADIO);
        fLogLevelOnlyButton.setText(Messages.TraceControl_EnableEventsLogLevelOnlyTypeName);
        fLogLevelOnlyButton.setToolTipText(Messages.TraceControl_EnableEventsLogLevelOnlyTypeTooltip);
        data = new GridData(GridData.FILL_BOTH);
        fLogLevelButton.setLayoutData(data);

        TraceJulLogLevel[] levels = TraceJulLogLevel.values();

        String[] levelNames = new String[levels.length - 1];
        int k = 0;
        for (int i = 0; i < levels.length; i++) {
            if (levels[i] != TraceJulLogLevel.LEVEL_UNKNOWN) {
                levelNames[k++] = levels[i].getInName();
            }
        }

        fLogLevelCombo = new CCombo(logLevelGroup, SWT.READ_ONLY);
        fLogLevelCombo.setItems(levelNames);
        fLogLevelCombo.setToolTipText(Messages.TraceControl_EnableEventsLogLevelTooltip);
        data = new GridData(GridData.FILL_HORIZONTAL);
        data.horizontalSpan = 4;
        fLogLevelCombo.setLayoutData(data);

        // By default the combo box and the buttons are not enabled
        fLogLevelCombo.setEnabled(false);
        fLogLevelButton.setEnabled(false);
        fLogLevelOnlyButton.setEnabled(false);


        getShell().setMinimumSize(new Point(300, 200));

        return dialogComposite;
    }

    @Override
    protected void okPressed() {

        if (fSessionsCombo.getSelectionIndex() < 0) {
            MessageDialog.openError(getShell(),
                    Messages.TraceControl_EnableLoggersDialogTitle,
                    Messages.TraceControl_EnableEventsNoSessionError);
            return;
        }

        fSessionIndex = fSessionsCombo.getSelectionIndex();

        fLogLevel = null;
        // If nothing is selected in the combo box that means that all the
        // loglevels should be enabled.
        if (!fLogLevelCombo.getText().isEmpty()) {
            TraceJulLogLevel[] levels = TraceJulLogLevel.values();
            int id = fLogLevelCombo.getSelectionIndex();

            if (id < 0) {
                MessageDialog.openError(getShell(),
                        Messages.TraceControl_EnableLoggersDialogTitle,
                        Messages.TraceControl_InvalidLogLevel);
            }

            fLogLevelType = LogLevelType.LOGLEVEL_NONE;
            if (fLogLevelButton.getSelection()) {
                fLogLevelType = LogLevelType.LOGLEVEL;
            } else if (fLogLevelOnlyButton.getSelection()) {
                fLogLevelType = LogLevelType.LOGLEVEL_ONLY;
            }

            fLogLevel = levels[id];
        }
        super.okPressed();
    }

}