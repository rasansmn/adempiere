/******************************************************************************
 * Product: ADempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 2003-2011 e-Evolution Consultants. All Rights Reserved.      *
 * Copyright (C) 2003-2011 Victor Pérez Juárez 								  * 
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * Contributor(s): Victor Pérez Juárez  (victor.perez@e-evolution.com)		  *
 * Sponsors: e-Evolution Consultants (http://www.e-evolution.com/)            *
 *****************************************************************************/

package org.eevolution.form;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.logging.Level;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.model.MBrowse;
import org.adempiere.model.MBrowseField;
import org.compiere.Adempiere;
import org.compiere.apps.ADialog;
import org.compiere.apps.AEnv;
import org.compiere.apps.ALayout;
import org.compiere.apps.AppsAction;
import org.compiere.apps.ConfirmPanel;
import org.compiere.apps.ProcessCtl;
import org.compiere.apps.ProcessParameterPanel;
import org.compiere.apps.StatusBar;
import org.compiere.apps.Waiting;
import org.compiere.grid.ed.VEditor;
import org.compiere.minigrid.IDColumn;
import org.compiere.model.GridField;
import org.compiere.model.MPInstance;
import org.compiere.model.MQuery;
import org.compiere.process.ProcessInfo;
import org.compiere.process.ProcessInfoUtil;
import org.compiere.swing.CButton;
import org.compiere.swing.CFrame;
import org.compiere.swing.CPanel;
import org.compiere.swing.CollapsiblePanel;
import org.compiere.util.ASyncProcess;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Ini;
import org.compiere.util.Login;
import org.compiere.util.Msg;
import org.compiere.util.Splash;
import org.eevolution.grid.BrowseTable;

/**
 * UI Browser
 * 
 * @author victor.perez@e-evolution.com, victor.perez@e-evolution.com
 * <li>FR [ 3426137 ] Smart Browser
 * https://sourceforge.net/tracker/?func=detail&aid=3426137&group_id=176962&atid=879335
 * @author carlosaparada@gmail.com Carlos Parada, ERP Consultores y asociados
 * @author Yamel Senih, ysenih@erpcya.com, ERPCyA http://www.erpcya.com
 * 		<li>FR [ 245 ] Change Smart Browse to MVC
 * 		@see https://github.com/adempiere/adempiere/issues/245
 * 		<li>FR [ 246 ] Smart Browse validate parameters when is auto-query
 * 		@see https://github.com/adempiere/adempiere/issues/246
 * 		<li>FR [ 247 ] Smart Browse don't have the standard buttons
 * 		@see https://github.com/adempiere/adempiere/issues/247
 */
public class VBrowser extends Browser implements ActionListener,
		VetoableChangeListener, ChangeListener, ListSelectionListener,
		TableModelListener, ASyncProcess {
	CFrame m_frame = new CFrame();
	/**
	 * get Browse
	 * @param browse_ID
	 */
	public static CFrame openBrowse(int browse_ID) {
		MBrowse browse = new MBrowse(Env.getCtx(), browse_ID , null);
		boolean modal = true;
		int WindowNo = 0;
		String value = "";
		String keyColumn = "";
		boolean multiSelection = true;
		String whereClause = null;
		CFrame ff = new CFrame();
		return new VBrowser(ff, modal , WindowNo, value, browse, keyColumn,multiSelection, whereClause)
		.getFrame();
		
	}

	/**
	 * Detail Protected Constructor.
	 * 
	 * @param frame
	 *            parent
	 * @param modal
	 *            modal
	 * @param WindowNo
	 *            window no
	 * @param value
	 *            QueryValue
	 * @param browse
	 *            table name
	 * @param keyColumn
	 *            key column (ignored)
	 * @param multiSelection
	 *            multiple selections
	 * @param whereClause
	 *            where clause
	 */
	public VBrowser(CFrame frame, boolean modal, int WindowNo, String value,
			MBrowse browse, String keyColumn, boolean multiSelection,
			String whereClause) {
		super(modal, WindowNo, value, browse, keyColumn, multiSelection,
				whereClause);
		m_frame = frame;
		m_frame.setTitle(browse.getTitle());
		m_frame.getContentPane().add(statusBar, BorderLayout.SOUTH);
		windowNo = Env.createWindowNo(m_frame);
		setContextWhere(whereClause);		
		//	Init Smart Browse
		init();
	} // InfoGeneral

	/** Process Parameters Panel */
	private ProcessParameterPanel parameterPanel;
	/** StatusBar **/
	protected StatusBar statusBar = new StatusBar();
	/** Worker */
	private Worker m_worker = null;
	/** Waiting Dialog */
	private Waiting m_waiting = null;
	/**	Tool Bar 					*/
	private CButton bCancel;
	private CButton bDelete;
	private CButton bExport;
	private CButton bOk;
	private CButton bSearch;
	private CButton bSelectAll;
	private CButton bZoom;
	private CPanel buttonSearchPanel;
	private javax.swing.JScrollPane centerPanel;
	private CPanel footButtonPanel;
	private CPanel footPanel;
	private CPanel graphPanel;
	private CPanel processPanel;
	private CPanel searchTab;
	private javax.swing.JTabbedPane tabsPanel;
	private javax.swing.JToolBar toolsBar;
	private CPanel topPanel;
	/**	Table						*/
	private BrowseTable detail;
	private CollapsiblePanel collapsibleSearch;
	private VBrowserSearch  searchPanel;

	
	@Override
	public void init() {
		initComponents();
		statInit();
		//m_frame.setPreferredSize(getPreferredSize());
		//
		int no = detail.getRowCount();
		setStatusLine(
				Integer.toString(no) + " "
						+ Msg.getMsg(Env.getCtx(), "SearchRows_EnterQuery"),
				false);
		setStatusDB(Integer.toString(no));
		//	
		if (isExecuteQueryByDefault()
				&& !hasMandatoryParams())
			executeQuery();
	}
	
	
	/**
	 * Static Setup - add fields to parameterPanel (GridLayout)
	 */
	private void statInit() {
		searchPanel.setLayout(new ALayout());
		int cols = 0;
		int col = 2;
		int row = 0;
		for (MBrowseField field : m_Browse.getCriteriaFields()) {
			String title = field.getName();
			String name = field.getAD_View_Column().getColumnName();
			searchPanel.addField(field, row, cols, name, title);
			cols = cols + col;

			if (field.isRange())
				cols = cols + col;

			if (cols >= 4) {
				cols = 0;
				row++;
			}
		}
		
		searchPanel.dynamicDisplay();
		
		if (m_Browse.getAD_Process_ID() > 0) {
			//	FR [ 245 ]
			initProcessInfo();
			parameterPanel = new ProcessParameterPanel(getWindowNo() , getBrowseProcessInfo());
			parameterPanel.setMode(ProcessParameterPanel.MODE_HORIZONTAL);
			parameterPanel.init();
			processPanel.add(parameterPanel, BorderLayout.CENTER);
		}
	}

	/**
	 * Set Status Line
	 * 
	 * @param text
	 *            text
	 * @param error
	 *            error
	 */
	public void setStatusLine(String text, boolean error) {
		statusBar.setStatusLine(text, error);
		Thread.yield();
	} // setStatusLine

	/**
	 * Set Status DB
	 * 
	 * @param text
	 *            text
	 */
	public void setStatusDB(String text) {
		statusBar.setStatusDB(text);
	} // setStatusDB

	/**************************************************************************
	 * Execute Query
	 */
	protected void executeQuery() {
		//	FR [ 245 ]
		String errorMsg = evaluateMandatoryFilter();
		if (errorMsg == null) {
			if (getAD_Window_ID() > 1)
				bZoom.setEnabled(true);

			bSelectAll.setEnabled(true);
			bExport.setEnabled(true);

			if (isDeleteable())
				bDelete.setEnabled(true);

			p_loadedOK = initBrowser();

			Env.setContext(Env.getCtx(), 0, "currWindowNo", getWindowNo());
			if (parameterPanel != null)
				parameterPanel.refreshContext();

			if (m_worker != null && m_worker.isAlive())
				return;
			//	
			int no = testCount();
			if (no > 0) {
				if(!ADialog.ask(getWindowNo(), m_frame, "InfoHighRecordCount",
						String.valueOf(no))) {
					return;
				}
			}

			m_frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			setStatusLine(Msg.getMsg(Env.getCtx(), "StartSearch"), false);
			m_worker = new Worker();
			m_worker.start();
		} else {
			ADialog.error(windowNo, getForm().getContentPane(), 
					"FillMandatory", Msg.parseTranslation(Env.getCtx(), errorMsg));
		}
	} // executeQuery
	
	/**
	 * General Init
	 * @return true, if success
	 */
	private boolean initBrowser() {
		//	
		initBrowserTable(detail);
		//	
		if (browserFields.size() == 0) {
			ADialog.error(getWindowNo(), m_frame, "Error", "No Browse Fields");
			log.log(Level.SEVERE, "No Browser for view=" + m_View.getName());
			return false;
		}
		return true;
	} // initInfo
	
	/**
	 * Zoom
	 */
	private void cmd_zoom() {
		
		m_frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		
		MQuery query = getMQuery(detail);
		if(query != null)
			AEnv.zoom(m_frame , getAD_Window_ID() , query);
		
		m_frame.setCursor(Cursor.getDefaultCursor());
		bZoom.setSelected(false);
	} // cmd_zoom
	
	/**
	 * Show a list to select one or more items to delete.
	 */
	private void cmd_deleteSelection(){
		m_frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		if (ADialog.ask(getWindowNo(), m_frame, "DeleteSelection"))
		{	
			int records = deleteSelection(detail);
			setStatusLine(Msg.getMsg(Env.getCtx(), "Deleted") + records, false);
		}	
		m_frame.setCursor(Cursor.getDefaultCursor());
		bDelete.setSelected(false);
		executeQuery();
		
	}//cmd_deleteSelection

	public void dispose(boolean ok) {
		log.config("OK=" + ok);
		m_ok = ok;

		// End Worker
		if (m_worker != null) {
			// worker continues, but it does not block UI
			if (m_worker.isAlive())
				m_worker.interrupt();
			log.config("Worker alive=" + m_worker.isAlive());
		}
		m_worker = null;
		//	
		saveResultSelection(detail);
		saveSelection(detail);

		m_frame.removeAll();
		m_frame.dispose();
		if (m_Browse.getAD_Process_ID() <= 0)
			return;

		MPInstance instance = new MPInstance(Env.getCtx(),
				m_Browse.getAD_Process_ID(), getBrowseProcessInfo().getRecord_ID());
		instance.saveEx();

		DB.createT_Selection(instance.getAD_PInstance_ID(), getSelectedKeys(),
				null);
		
		ProcessInfo pi = getBrowseProcessInfo();
		pi.setAD_PInstance_ID(instance.getAD_PInstance_ID());
		pi.setWindowNo(getWindowNo());
		parameterPanel.saveParameters();
		ProcessInfoUtil.setParameterFromDB(pi);
		setBrowseProcessInfo(pi);
		//Save Values Browse Field Update
		createT_Selection_Browse(instance.getAD_PInstance_ID());
		// Execute Process
		ProcessCtl worker = new ProcessCtl(this, pi.getWindowNo() , pi , null);
		worker.start(); // complete tasks in unlockUI /
        Env.clearWinContext(getWindowNo());
	} // dispose

	/**
	 * Instance tool bar
	 */
	private void setupToolBar() {
		bOk = ConfirmPanel.createOKButton(false);
		bOk.addActionListener(this);
		bSearch = ConfirmPanel.createRefreshButton(true);
		bSearch.addActionListener(this);
		bCancel = ConfirmPanel.createCancelButton(false);
		bCancel.addActionListener(this);
		bZoom = ConfirmPanel.createZoomButton(true);
		bZoom.addActionListener(this);
		bExport = ConfirmPanel.createExportButton(true);
		bExport.addActionListener(this);
		bDelete = ConfirmPanel.createDeleteButton(true);
		bDelete.addActionListener(this);
		AppsAction selectAllAction = new AppsAction("SelectAll", null, Msg.getMsg(Env.getCtx(),"SelectAll"));
		selectAllAction.setDelegate(this);
		bSelectAll = (CButton) selectAllAction.getButton();
		toolsBar = new javax.swing.JToolBar();
	}

	/**
	 * Init View componets
	 */
	private void initComponents() {

		toolsBar = new javax.swing.JToolBar();
		tabsPanel = new javax.swing.JTabbedPane();
		searchTab = new CPanel();
		topPanel = new CPanel();
		searchPanel = new VBrowserSearch(getWindowNo());
		buttonSearchPanel = new CPanel();
		centerPanel = new javax.swing.JScrollPane();
		detail = new BrowseTable(this);
		detail.setRowSelectionAllowed(true);
		footPanel = new CPanel();
		footButtonPanel = new CPanel(new FlowLayout(FlowLayout.RIGHT));
		processPanel = new CPanel();
		graphPanel = new CPanel();

		setupToolBar();

		toolsBar.setRollover(true);
		
		bSelectAll.setText(Msg.getMsg(Env.getCtx(),"SelectAll").replaceAll("[&]",""));
		bSelectAll.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
		bSelectAll.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
		bSelectAll.setEnabled(false);

		toolsBar.add(bSelectAll);

		bZoom.setText(Msg.getMsg(Env.getCtx(),"Zoom").replaceAll("[&]",""));
		bZoom.setFocusable(false);
		bZoom.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
		bZoom.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
		bZoom.setEnabled(false);
		
		if (AD_Window_ID > 0)
			toolsBar.add(bZoom);

		bExport.setText(Msg.getMsg(Env.getCtx(),("Export")));
		bExport.setFocusable(false);
		bExport.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
		bExport.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
		bExport.setEnabled(false);
		
		toolsBar.add(bExport);

		bDelete.setText(Msg.getMsg(Env.getCtx(),"Delete").replaceAll("[&]",""));
		bDelete.setFocusable(false);
		bDelete.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
		bDelete.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
		bDelete.setEnabled(false);
		
		if(isDeleteable())
			toolsBar.add(bDelete);

		m_frame.getContentPane()
				.add(toolsBar, java.awt.BorderLayout.PAGE_START);

		searchTab.setLayout(new java.awt.BorderLayout());

		topPanel.setLayout(new java.awt.BorderLayout());

		searchPanel.setLayout(new java.awt.GridBagLayout());
		
		collapsibleSearch = new CollapsiblePanel(Msg.getMsg(Env.getCtx(),("SearchCriteria")));
		collapsibleSearch.add(searchPanel);
		topPanel.add(collapsibleSearch, java.awt.BorderLayout.NORTH);

		bSearch.setText(Msg.getMsg(Env.getCtx(), "StartSearch"));

		buttonSearchPanel.add(bSearch);
		collapsibleSearch.add(buttonSearchPanel);

		searchTab.add(topPanel, java.awt.BorderLayout.NORTH);
		
		centerPanel.setViewportView(detail);

		searchTab.add(centerPanel, java.awt.BorderLayout.CENTER);

		footPanel.setLayout(new java.awt.BorderLayout());

//		bCancel.setText(Msg.getMsg(Env.getCtx(), "Cancel").replaceAll("[&]",""));
		
		footButtonPanel.add(bCancel);

//		bOk.setText(Msg.getMsg(Env.getCtx(), "Ok").replaceAll("[&]",""));
		
		footButtonPanel.add(bOk);

		footPanel.add(footButtonPanel, java.awt.BorderLayout.SOUTH);

		processPanel.setLayout(new java.awt.BorderLayout());
		footPanel.add(processPanel, java.awt.BorderLayout.CENTER);

		searchTab.add(footPanel, java.awt.BorderLayout.SOUTH);

		tabsPanel.addTab(Msg.getMsg(Env.getCtx(), "Search"), searchTab);

		graphPanel.setLayout(new java.awt.BorderLayout());
		
		//	Instance Table
		detail = new BrowseTable(this);
		centerPanel.setViewportView(detail);
		
		m_frame.getContentPane().add(tabsPanel, java.awt.BorderLayout.CENTER);
	}
	
	/**
	 * Process Action
	 */
	private void cmd_Process() {
		m_frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		m_ok = true;
		// End Worker
		if (m_worker != null) {
			// worker continues, but it does not block UI
			if (m_worker.isAlive())
				m_worker.interrupt();
			log.config("Worker alive=" + m_worker.isAlive());
		}
		m_worker = null;
		
		saveResultSelection(detail);
		saveSelection(detail);
		
		if (m_Browse.getAD_Process_ID() > 0 && getSelectedKeys() != null)
		{

			MPInstance instance = new MPInstance(Env.getCtx(),
					m_Browse.getAD_Process_ID(), getBrowseProcessInfo().getRecord_ID());
			instance.saveEx();
	
			DB.createT_Selection(instance.getAD_PInstance_ID(), getSelectedKeys(),
					null);
			
			ProcessInfo pi = getBrowseProcessInfo();
			pi.setWindowNo(getWindowNo());
			pi.setAD_PInstance_ID(instance.getAD_PInstance_ID());
			// call process 
			parameterPanel.saveParameters();
			ProcessInfoUtil.setParameterFromDB(pi);
			setBrowseProcessInfo(pi);
			//Save Values Browse Field Update
			createT_Selection_Browse(instance.getAD_PInstance_ID());
			// Execute Process
			ProcessCtl worker = new ProcessCtl(this, pi.getWindowNo() , pi , null);
			m_waiting = new Waiting (m_frame, Msg.getMsg(Env.getCtx(), "Processing"), false, getBrowseProcessInfo().getEstSeconds());
			worker.run(); // complete tasks in unlockUI /
			m_waiting.doNotWait();
			setStatusLine(pi.getSummary(), pi.isError());
			
		}
		m_frame.setCursor(Cursor.getDefaultCursor());
		p_loadedOK = initBrowser();
		collapsibleSearch.setCollapsed(false);
	}

	/**
	 * Cancel Action
	 */
	private void cmd_Cancel() {
		this.dispose();
	}

	/**
	 *  Dispose
	 */
	public void dispose() {
		searchPanel.dispose();
		m_frame.removeAll();
		m_frame.dispose();
	}   //  dis

	/**
	 * Action Search
	 */
	private void cmd_Search() {
		bZoom.setEnabled(true);
		bSelectAll.setEnabled(true);
		bExport.setEnabled(true);
		bDelete.setEnabled(true);
		Env.setContext(Env.getCtx(), 0, "currWindowNo", getWindowNo());

		if (m_Browse.getAD_Process_ID() > 0)
			parameterPanel.refreshContext();

		executeQuery();
	}

	/**
	 * Action for export
	 */
	private void cmd_Export() {
		m_frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		try 
		{	File file = exportXLS(detail);
			Env.startBrowser(file.toURI().toString());
		} catch (Exception e) {
			throw new AdempiereException("Failed to render report", e);
		}
		m_frame.setCursor(Cursor.getDefaultCursor());
		bExport.setSelected(false);
	}

	/**
	 * Get form
	 * @return
	 */
	public CFrame getForm() {
		return m_frame;
	}
	
	/**
	 * Worker
	 */
	class Worker extends Thread {
		private PreparedStatement m_pstmt = null;
		private ResultSet m_rs = null;
		private String dataSql = null;

		/**
		 * Do Work (load data)
		 */
		public void run() {
			//	Set Collapsed
			collapsibleSearch.setCollapsed(!isCollapsibleByDefault());
			//	
			long start = System.currentTimeMillis();
			int no = 0;
			dataSql = getSQL();
			//	Row
			int row = 0;
			try {
				m_pstmt = getStatement(dataSql);
				log.fine("Start query - "
						+ (System.currentTimeMillis() - start) + "ms");
				m_rs = m_pstmt.executeQuery();
				log.fine("End query - " + (System.currentTimeMillis() - start)
						+ "ms");
				//	Loop
				while (m_rs.next()) {
					if (this.isInterrupted()) {
						log.finer("Interrupted");
						close();
						return;
					}
					no++;
//					int row = detail.getRowCount();
					detail.setRowCount(row + 1);
					int colOffset = 1; // columns start with 1
					int columnDisplayIndex =0;
					int column = 0;
					for (MBrowseField field : browserFields) {
						Object value = null;
						if (field.isKey() && !field.getAD_View_Column().getColumnSQL().equals("'Row' AS \"Row\""))
							value = new IDColumn(m_rs.getInt(column + colOffset));
						else if (field.isKey() && !field.getAD_View_Column().getColumnSQL().equals("'Row' AS \"Row\""))
							value  = new IDColumn(no);
						else if (DisplayType.TableDir == field.getAD_Reference_ID()
							  || DisplayType.Table == field.getAD_Reference_ID()
							  || DisplayType.Integer == field.getAD_Reference_ID()
							  || DisplayType.PAttribute == field.getAD_Reference_ID()
							  || DisplayType.Account == field.getAD_Reference_ID()) {
							Integer id = m_rs.getInt(column + colOffset);
							value = id != 0 ? id : null;
						}
						else if (DisplayType.isNumeric(field.getAD_Reference_ID()))
							value = m_rs.getBigDecimal(column + colOffset);
						else if (DisplayType.isDate(field.getAD_Reference_ID()))
							value = m_rs.getTimestamp(column + colOffset);
						else if (DisplayType.YesNo == field.getAD_Reference_ID()){
							value = m_rs.getString(column + colOffset);
							if (value != null)
								value= value.equals("Y");
						}
						else
							value = m_rs.getObject(column + colOffset);

						GridField gridField = MBrowseField.createGridFieldVO(field , getWindowNo());
						gridField.setValue(value, true);
						detail.setValueAt(row, columnDisplayIndex, gridField);
						if (field.isDisplayed())
							columnDisplayIndex++;

						column ++;
					}
					//	Increment Row
					row++;
				}
			} catch (SQLException e) {
				log.log(Level.SEVERE, dataSql, e);
			}
			close();
			//
			//no = detail.getRowCount();
			log.fine("#" + no + " - " + (System.currentTimeMillis() - start)
					+ "ms");
			if (detail.isShowTotals())
				detail.addTotals();
			detail.autoSize();
			//
			m_frame.setCursor(Cursor.getDefaultCursor());
			setStatusLine(
					Integer.toString(no) + " "
							+ Msg.getMsg(Env.getCtx(), "SearchRows_EnterQuery"),
					false);
			setStatusDB(Integer.toString(no));
			if (no == 0)
				log.fine(dataSql);
			else {
				detail.getSelectionModel().setSelectionInterval(0, 0);
				detail.requestFocus();
			}
			isAllSelected = isSelectedByDefault();
			selectedRows(detail);
			//	Set Collapsed
			collapsibleSearch.setCollapsed(isCollapsibleByDefault());
		} // run

		/**
		 * Close ResultSet and Statement
		 */
		private void close() {
			DB.close(m_rs, m_pstmt);
			m_rs = null;
			m_pstmt = null;
		}
	} // Worker

	public static void main(String args[]) {
		Splash.getSplash();
		// Adempiere.startup(true); // needs to be here for UI
		// Adempiere.startupEnvironment(false);
		Adempiere.startup(true);
		Ini.setProperty(Ini.P_UID, "SuperUser");
		Ini.setProperty(Ini.P_PWD, "System");
		Ini.setProperty(Ini.P_ROLE, "GardenWorld Admin");
		Ini.setProperty(Ini.P_CLIENT, "GardenWorld");
		Ini.setProperty(Ini.P_ORG, "HQ");
		Ini.setProperty(Ini.P_WAREHOUSE, "HQ Warehouse");
		Ini.setProperty(Ini.P_LANGUAGE, "English");
		// Ini.setProperty(Ini.P_PRINTER,"MyPrinter");
		Login login = new Login(Env.getCtx());
		login.batchLogin();

		Properties m_ctx = Env.getCtx();
		MBrowse browse = new MBrowse(m_ctx, 50025, null);
		CFrame frame = new CFrame();
		boolean modal = true;
		int WindowNo = 0;
		String value = "";
		String keyColumn = "";
		boolean multiSelection = true;
		String whereClause = "";
		VBrowser browser = new VBrowser(frame, modal, WindowNo, value, browse,
				keyColumn, multiSelection, whereClause);
		// browser.setPreferredSize(getPreferredSize ());
		browser.getFrame().setVisible(true);
		browser.getFrame().pack();
	}

	public CFrame getFrame() {
		return m_frame;
	}

	/**
	 * Get Preferred Size
	 * 
	 * @return size
	 */
	public Dimension getPreferredSize() {
		Dimension size = m_frame.getPreferredSize();
		if (size.width > WINDOW_WIDTH)
			size.width = WINDOW_WIDTH - 30;
		return size;
	} // getPreferredSize

	/**
	 * Action Performed
	 */
	public void actionPerformed(ActionEvent actionEvent) {
		String action = actionEvent.getActionCommand();
		if (action == null || action.length() == 0)
			return;
		log.info( "VBrowser - actionPerformed: " + action);
		if (actionEvent.getSource().equals(bSelectAll)) {
			selectedRows(detail);
		} else if(actionEvent.getSource().equals(bSearch)) {
			cmd_Search();
		} else if(actionEvent.getSource().equals(bCancel)) {
			cmd_Cancel();
		} else if(actionEvent.getSource().equals(bOk)) {
			cmd_Process();
		} else if(actionEvent.getSource().equals(bExport)) {
			cmd_Export();
		} else if(actionEvent.getSource().equals(bDelete)) {
			cmd_deleteSelection();
		} else if(actionEvent.getSource().equals(bZoom)) {
			cmd_zoom();
		}		
	}

	public void vetoableChange(PropertyChangeEvent evt)
			throws PropertyVetoException {
		
	}

	public void stateChanged(ChangeEvent e) {
		
	}

	public void valueChanged(ListSelectionEvent e) {
		
	}

	public void tableChanged(TableModelEvent e) {
		
	}

	public void executeASync(ProcessInfo pi) {
		
	}

	public boolean isUILocked() {
		return false;
	}

	public void lockUI(ProcessInfo pi) {
		
	}

	public void unlockUI(ProcessInfo pi) {
		
	}
	
	@Override
	public LinkedHashMap<Object, GridField> getPanelParameters() {
		LinkedHashMap<Object, GridField> m_List = new LinkedHashMap<Object, GridField>();
		for (Entry<Object, Object> entry : searchPanel.getParameters().entrySet()) {
			VEditor editor = (VEditor) entry.getValue();
			GridField field = editor.getField();
			field.setValue(editor.getValue(), true);
			m_List.put(entry.getKey(), field);
		}
		//	Default Return
		return m_List;
	}
}
