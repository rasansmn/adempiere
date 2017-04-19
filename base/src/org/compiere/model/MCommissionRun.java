/******************************************************************************
 * Product: ADempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 2006-2017 ADempiere Foundation, All Rights Reserved.         *
 * This program is free software, you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY, without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program, if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * or via info@adempiere.net or http://www.adempiere.net/license.html         *
 *****************************************************************************/
package org.compiere.model;

import java.io.File;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Properties;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.process.DocAction;
import org.compiere.process.DocOptions;
import org.compiere.process.DocumentEngine;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Language;
import org.compiere.util.Msg;

import java.util.List;
import java.util.logging.Level;

/** Generated Model for C_CommissionRun
 *  @author Adempiere (generated) 
 *  @version Release 3.9.0 - $Id$ 
 *  @author Yamel Senih, ysenih@erpcya.com, ERPCyA http://www.erpcya.com
 *  	<a href="https://github.com/adempiere/adempiere/issues/766">
 * 		@see FR [ 766 ] Improve Commission Calculation</a>
 **/
public class MCommissionRun extends X_C_CommissionRun implements DocAction, DocOptions {

	/**
	 *
	 */
	private static final long serialVersionUID = 20170318L;

    /** Standard Constructor */
    public MCommissionRun (Properties ctx, int C_CommissionRun_ID, String trxName)
    {
      super (ctx, C_CommissionRun_ID, trxName);
    }

    /** Load Constructor */
    public MCommissionRun (Properties ctx, ResultSet rs, String trxName)
    {
      super (ctx, rs, trxName);
    }
    
	/**
	 * 	Parent Constructor
	 *	@param commission parent
	 */
	public MCommissionRun (MCommission commission)
	{
		this (commission.getCtx(), 0, commission.get_TrxName());
		setClientOrg (commission);
		setC_Commission_ID (commission.getC_Commission_ID());
	}	//	MCommissionRun

	/**
	 * 	Get Amounts
	 *	@return array of amounts
	 */
	public MCommissionAmt[] getAmts()
	{
		final String whereClause = I_C_CommissionAmt.COLUMNNAME_C_CommissionRun_ID+"=?";
 		List<MCommissionAmt> list = new Query(getCtx(),I_C_CommissionAmt.Table_Name,whereClause,get_TrxName())
		.setParameters(getC_CommissionRun_ID())
		.list();
		//	Convert
		MCommissionAmt[] retValue = new MCommissionAmt[list.size()];
		list.toArray(retValue);
		return retValue;
	}	//	getAmts

	/**
	 * 	Update From Amt
	 */
	public void updateFromAmt()
	{
		MCommissionAmt[] amts = getAmts();
		BigDecimal GrandTotal = Env.ZERO;
		for (int i = 0; i < amts.length; i++)
		{
			MCommissionAmt amt = amts[i];
			GrandTotal = GrandTotal.add(amt.getCommissionAmt());
		}
		setGrandTotal(GrandTotal);
	}	//	updateFromAmt

	/**
	 * 	Get Document Info
	 *	@return document info (untranslated)
	 */
	public String getDocumentInfo()
	{
		MDocType dt = MDocType.get(getCtx(), getC_DocType_ID());
		return dt.getName() + " " + getDocumentNo();
	}	//	getDocumentInfo

	/**
	 * 	Create PDF
	 *	@return File or null
	 */
	public File createPDF ()
	{
		try
		{
			File temp = File.createTempFile(get_TableName() + get_ID() +"_", ".pdf");
			return createPDF (temp);
		}
		catch (Exception e)
		{
			log.severe("Could not create PDF - " + e.getMessage());
		}
		return null;
	}	//	getPDF

	/**
	 * 	Create PDF file
	 *	@param file output file
	 *	@return file if success
	 */
	public File createPDF (File file)
	{
	//	ReportEngine re = ReportEngine.get (getCtx(), ReportEngine.INVOICE, getC_Invoice_ID());
	//	if (re == null)
			return null;
	//	return re.getPDF(file);
	}	//	createPDF

	
	/**************************************************************************
	 * 	Process document
	 *	@param processAction document action
	 *	@return true if performed
	 */
	public boolean processIt (String processAction)
	{
		m_processMsg = null;
		DocumentEngine engine = new DocumentEngine (this, getDocStatus());
		return engine.processIt (processAction, getDocAction());
	}	//	processIt
	
	/**	Process Message 			*/
	private String		m_processMsg = null;
	/**	Just Prepared Flag			*/
	private boolean		m_justPrepared = false;

	/**
	 * 	Unlock Document.
	 * 	@return true if success 
	 */
	public boolean unlockIt()
	{
		log.info("unlockIt - " + toString());
	//	setProcessing(false);
		return true;
	}	//	unlockIt
	
	/**
	 * 	Invalidate Document
	 * 	@return true if success 
	 */
	public boolean invalidateIt()
	{
		log.info("invalidateIt - " + toString());
		setDocAction(DOCACTION_Prepare);
		return true;
	}	//	invalidateIt
	
	/**
	 *	Prepare Document
	 * 	@return new status (In Progress or Invalid) 
	 */
	public String prepareIt()
	{
		log.info(toString());
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_BEFORE_PREPARE);
		if (m_processMsg != null)
			return DocAction.STATUS_Invalid;
		
		MDocType dt = MDocType.get(getCtx(), getC_DocType_ID());

		//	Std Period open?
		if (!MPeriod.isOpen(getCtx(), getDateDoc(), dt.getDocBaseType(), getAD_Org_ID()))
		{
			m_processMsg = "@PeriodClosed@";
			return DocAction.STATUS_Invalid;
		}
		
		//	Create Commission Movements
		if((getDocStatus().equals(STATUS_Drafted)) ||
				(getDocStatus().equals(STATUS_Invalid)) ||
				(getDocStatus().equals(STATUS_InProgress) && isReCalculate())) {
			try {
				createMovements();
			} catch (Exception e) {
				m_processMsg = e.getMessage();
				return DocAction.STATUS_Invalid;
			}

			//	Add up Amounts
			m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_AFTER_PREPARE);
			if (m_processMsg != null)
				return DocAction.STATUS_Invalid;
		}
		
		m_justPrepared = true;
		if (!DOCACTION_Complete.equals(getDocAction()))
			setDocAction(DOCACTION_Complete);
		return DocAction.STATUS_InProgress;
	}	//	prepareIt
	
	/**
	 * Delete Current Movements
	 */
	private void deleteMovements() {
		// RE-Process, delete old movements
		int no = DB.executeUpdateEx("DELETE FROM C_CommissionAmt c "
				+ "WHERE C_CommissionRun_ID = ?", new Object[]{getC_CommissionRun_ID()}, get_TrxName());
		log.info("C_CommissionAmt deleted #"+ no);
	}
	
	/**
	 * Create Commission Movements
	 * @author <a href="mailto:yamelsenih@gmail.com">Yamel Senih</a> Feb 24, 2016, 1:13:03 AM
	 *  @throws Exception if not successful
	 * @return void
	 */
	private void createMovements()  throws Exception {
		String frequencyType = null;
		List<MCommission> commissionList = new ArrayList<MCommission>();
		
		deleteMovements();
		//	Get commission(s)
		if(getC_Commission_ID() != 0) {
			MCommission commission = new MCommission(getCtx(), getC_Commission_ID(), get_TrxName());
			if(commission.isActive()) {
				commissionList.add(commission);
				frequencyType = commission.getFrequencyType();
			}
		} else if(getC_CommissionGroup_ID() != 0) {
			MCommissionGroup group = new MCommissionGroup(getCtx(), getC_CommissionGroup_ID(), get_TrxName());
			commissionList = group.getLines(MCommissionGroup.COLUMNNAME_IsActive + "Y");
			frequencyType = group.getFrequencyType();
		}
		
		//	Verify if a commission or group configured exists
		if (commissionList.size()==0)
			throw new AdempiereException("@NoCommissionDefined@");
		
		//	Set Start and End
		setStartEndDate(frequencyType);
		log.info("StartDate = " + getStartDate() + ", EndDate = " + getEndDate());
		
		//	Iterate for each commission definition and  Sales Representative
		for(MCommission commission : commissionList) {
			for(MBPartner salesRep : commission.getSalesRepsOfCommission()) {
				processCommissionLine(salesRep, commission);
				if (commission.isAllowRMA()) {					
					// TODO: process devolutions which are not in the invoice lines
				}				
			}
		}
		saveEx();
	}
	
	/**
	 * 	Create Commission Detail
	 *	@param sql sql statement
	 *	@param commission parent
	 *	@param comAmt
	 */
	private void createDetail (String sql, MCommission commission, MCommissionAmt comAmt) {
		String language = Env.getAD_Language(getCtx());
		int C_InvoiceLine_ID;
		BigDecimal qtyReturned = Env.ZERO;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = DB.prepareStatement(sql, get_TrxName());
			pstmt.setInt(1, getAD_Client_ID());
			pstmt.setTimestamp(2, getStartDate());
			pstmt.setTimestamp(3, getEndDate());
			rs = pstmt.executeQuery();
			while (rs.next()) {
				C_InvoiceLine_ID = rs.getInt(5);
				if(commission.getDocBasisType().equals(MCommission.DOCBASISTYPE_Receipt)
						&& commission.isTotallyPaid()) {
					if (!invoiceCompletelyPaid(C_InvoiceLine_ID)) {
						// Commission applies only in case the invoice has been paid in whole.
						// If the Invoice Line belongs to an Invoice which has not been (completely) paid, skip commission calculation.
						continue;
					}
				}
				//	CommissionAmount, C_Currency_ID, Amt, Qty,
				MCommissionDetail cd = new MCommissionDetail (comAmt,
					rs.getInt(1), rs.getBigDecimal(2), rs.getBigDecimal(3));
					
				//	C_OrderLine_ID, C_InvoiceLine_ID,
				cd.setLineIDs(rs.getInt(4), rs.getInt(5));
				
				//	Reference, Info,
				String s = rs.getString(6);
				if (s != null)
					cd.setReference(Msg.translate(language, "Payment") + "_" + Msg.translate(language, "Invoice") + ": " + s);
				s = rs.getString(7);
				if (s != null)
					cd.setInfo(Msg.translate(language, "ProductValue") + ": " + s);
				
				//	Date
				Timestamp date = rs.getTimestamp(8);
				cd.setConvertedAmt(date);
				cd.saveEx();
				
				// Check for RMAs
				if (commission.isAllowRMA()) {
					qtyReturned = returnedItemsQty(C_InvoiceLine_ID);
					if (qtyReturned.compareTo(Env.ZERO)==1) {
						// There has been RMA(s) for this Invoice Line
						// Create one (!) Commission Detail to compensate for all RMAs.
						MCommissionDetail compensationCD = MCommissionDetail.copy(getCtx(), cd, get_TrxName());
						compensationCD.setInfo(Msg.translate(language, "CompensationFor") + " "  + cd.getInfo() 
								+ " (" + Msg.translate(language, "QtyReturned") + ": "+ qtyReturned + "), ");						
						compensationCD.correctForRMA(qtyReturned);
						compensationCD.saveEx();
					}
				}
			}
		} catch (Exception e) {
			throw new AdempiereException("System Error: " + e.getLocalizedMessage(), e);
		} finally {
			DB.close(rs, pstmt);
			rs = null; pstmt = null;
		}
		return;
	}	//	createDetail

	
	/**
	 * 	Check if invoice has been completely paid
	 *	@param C_IvoiceLine_ID Invoice line 
	 */
	private boolean invoiceCompletelyPaid(int C_IvoiceLine_ID) {
		BigDecimal payedSum = Env.ZERO;
		BigDecimal grandTotal  = Env.ZERO;
		StringBuffer sql = new StringBuffer();
		PreparedStatement pstmt = null;
		ResultSet rs = null;		
		
		sql.append("SELECT coalesce(al1.payedSum,0) as payedSum, i.grandTotal "
				+ " FROM C_Invoice i "
				+ " INNER JOIN C_InvoiceLine il on (i.C_Invoice_ID=il.C_Invoice_ID) "
				+ " LEFT JOIN (  SELECT al2.C_Invoice_ID, sum(al2.amount) as payedSum "
				+ "              FROM C_AllocationLine al2 "
				+ "              INNER JOIN C_AllocationHdr ah on (al2.c_allocationhdr_id=ah.c_allocationhdr_id) "
				+ "              WHERE al2.C_Charge_ID IS NULL AND ah.docstatus<>'RE' "
				+ "              GROUP BY al2.C_Invoice_ID "
				+ "            ) al1 on (i.C_Invoice_ID = al1.C_Invoice_ID) "
				+ " WHERE il.C_InvoiceLine_ID=" + C_IvoiceLine_ID 
				+ " AND i.DocStatus IN ('CL','CO')"
				+ " AND i.AD_Client_ID = ? "
				+ " AND i.IsSOTrx='Y'");
		
		try {
			pstmt = DB.prepareStatement(sql.toString(), get_TrxName());
			pstmt.setInt(1, getAD_Client_ID());
			rs = pstmt.executeQuery();
			while (rs.next()) {
				payedSum = rs.getBigDecimal(1);
				grandTotal  = rs.getBigDecimal(2);
			}
		}
		catch (Exception e) {
			throw new AdempiereException("System Error: " + e.getLocalizedMessage(), e);
		} finally {
			DB.close(rs, pstmt);
			rs = null; pstmt = null;
		}
		if (grandTotal.compareTo(payedSum)==1)
			return false;
		else
			return true;
	}  // invoiceCompletelyPaid

	
	/**
	 * 	Find quantity of items returned for this Invoice Line
	 *	@param C_IvoiceLine_ID Invoice line 
	 * @return quantity of items returned
	 */
	private BigDecimal returnedItemsQty(int C_IvoiceLine_ID) {
		BigDecimal qtyReturned = Env.ZERO;
		StringBuffer sql = new StringBuffer();
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		sql.append("SELECT iol2.movementqty, i.documentno as invoice, rma.documentno as rma, io.documentno as io "
				+ " FROM C_InvoiceLine il "
				+ " INNER JOIN C_Invoice i ON (il.C_Invoice_ID=i.C_Invoice_ID AND i.DocStatus NOT IN ('DR', 'IP', 'VO', 'RE') ) "
				+ " INNER JOIN M_InOutLine iol1 ON (il.C_OrderLine_ID=iol1.C_OrderLine_ID) "
				+ " INNER JOIN M_RMALine rmal ON (iol1.M_InOutLine_ID=rmal.M_InOutLine_ID) "
				+ " INNER JOIN M_RMA rma ON (rmal.M_RMA_ID=rma.M_RMA_ID AND rma.DocStatus NOT IN ('DR', 'IP', 'VO', 'RE') ) "
				+ " INNER JOIN M_InOutLine iol2 ON (rmal.M_RMALine_ID=iol2.M_RMALine_ID) "
				+ " INNER JOIN M_InOut io ON (iol2.M_InOut_ID=io.M_InOut_ID AND io.DocStatus NOT IN ('DR', 'IP', 'VO', 'RE') ) "
				+ " WHERE il.C_InvoiceLine_ID=" + C_IvoiceLine_ID 
				+ " AND i.AD_Client_ID = ? "
				+ " AND i.IsSOTrx='Y'");
		
		try {
			pstmt = DB.prepareStatement(sql.toString(), get_TrxName());
			pstmt.setInt(1, getAD_Client_ID());
			rs = pstmt.executeQuery();
			while (rs.next()) {
				qtyReturned = qtyReturned.add(rs.getBigDecimal(1));
			}
		}
		catch (Exception e) {
			throw new AdempiereException("System Error: " + e.getLocalizedMessage(), e);
		} finally {
			DB.close(rs, pstmt);
			rs = null; pstmt = null;
		}
		
		return qtyReturned;
	}  // returnedItemsQty

	/**
	 * 
	 * Process lines
	 * @param salesRep
	 * @param commission
	 * @return
	 */
	private String processCommissionLine(MBPartner salesRep, MCommission commission) {
		//	
		MCommissionLine[] commissionLines = commission.getLines();
		List<Integer> salesRegion;
		String sqlAppend = " AND p.DateTrx BETWEEN ? AND ? ";	
		for (MCommissionLine commissionLine : commissionLines) {
			salesRegion = new ArrayList<Integer>();
			MCommissionAmt comAmt = new MCommissionAmt (this, commissionLine.getC_CommissionLine_ID());
			comAmt.setC_BPartner_ID(salesRep.getC_BPartner_ID());
			comAmt.saveEx();
			//
			StringBuffer sql = new StringBuffer();
			if (MCommission.DOCBASISTYPE_Receipt.equals(commission.getDocBasisType()))
			{			
				if (commission.isListDetails())
				{
					//	the view must be change
					sql.append("SELECT h.C_Currency_ID, (l.LineNetAmt*al.Amount/h.GrandTotal) AS Amt,"
						+ " (l.QtyInvoiced*al.Amount/h.GrandTotal) AS Qty,"
						+ " NULL, l.C_InvoiceLine_ID, p.DocumentNo||'_'||h.DocumentNo,"
						+ " COALESCE(prd.Value,l.Description), h.DateInvoiced "
						+ "FROM C_Payment p"
						+ " INNER JOIN C_AllocationLine al ON (p.C_Payment_ID=al.C_Payment_ID)"
						+ " INNER JOIN C_Invoice h ON (al.C_Invoice_ID = h.C_Invoice_ID)"
						+ " INNER JOIN C_InvoiceLine l ON (h.C_Invoice_ID = l.C_Invoice_ID) "
						+ " LEFT OUTER JOIN M_Product prd ON (l.M_Product_ID = prd.M_Product_ID) "
						+ "WHERE p.DocStatus IN ('CL','CO','RE')"
						+ " AND h.IsSOTrx='Y'"
						+ " AND p.AD_Client_ID = ? "
						+ " AND al.C_Charge_ID IS NULL "
						+ sqlAppend);
				}
				else
				{
					sql.append("SELECT h.C_Currency_ID, SUM(l.LineNetAmt*al.Amount/h.GrandTotal) AS Amt,"
							+ " SUM(l.QtyInvoiced*al.Amount/h.GrandTotal) AS Qty,"
							+ " NULL, NULL, NULL, NULL, MAX(h.DateInvoiced) "
							+ "FROM C_Payment p"
							+ " INNER JOIN C_AllocationLine al ON (p.C_Payment_ID=al.C_Payment_ID)"
							+ " INNER JOIN C_Invoice h ON (al.C_Invoice_ID = h.C_Invoice_ID)"
							+ " INNER JOIN C_InvoiceLine l ON (h.C_Invoice_ID = l.C_Invoice_ID) "
							+ "WHERE p.DocStatus IN ('CL','CO','RE')"
							+ " AND h.IsSOTrx='Y'"
							+ " AND p.AD_Client_ID = ? "
							+ " AND al.C_Charge_ID IS NULL "
							+ sqlAppend);
				}
				//	Days Due
				if (commissionLine.get_ValueAsInt(MCommissionLine.COLUMNNAME_DaysFrom) != 0)
					// sql.append(" AND h.DaysDue >= ").append(commissionLine.get_ValueAsInt(MCommissionLine.COLUMNNAME_DaysFrom));
					sql.append(" AND paymenttermduedays(h.C_PaymentTerm_ID, h.DateInvoiced, p.DateTrx) >= ").append(commissionLine.get_ValueAsInt(MCommissionLine.COLUMNNAME_DaysFrom));
				if (commissionLine.get_ValueAsInt(MCommissionLine.COLUMNNAME_DaysTo) != 0)
					//sql.append(" AND h.DaysDue <= ").append(commissionLine.get_ValueAsInt(MCommissionLine.COLUMNNAME_DaysTo));
					sql.append("  AND paymenttermduedays(h.C_PaymentTerm_ID, h.DateInvoiced, p.DateTrx) <= ").append(commissionLine.get_ValueAsInt(MCommissionLine.COLUMNNAME_DaysTo));
			}
			else if (MCommission.DOCBASISTYPE_Order.equals(commission.getDocBasisType()))
			{
				if (commission.isListDetails())
				{
					sql.append("SELECT h.C_Currency_ID, l.LineNetAmt, l.QtyOrdered, "
						+ "l.C_OrderLine_ID, NULL, h.DocumentNo,"
						+ " COALESCE(prd.Value,l.Description),h.DateOrdered "
						+ "FROM C_Order h"
						+ " INNER JOIN C_OrderLine l ON (h.C_Order_ID = l.C_Order_ID)"
						+ " LEFT OUTER JOIN M_Product prd ON (l.M_Product_ID = prd.M_Product_ID) "
						+ "WHERE h.DocStatus IN ('CL','CO')"
						+ " AND h.IsSOTrx='Y'"
						+ " AND h.AD_Client_ID = ?"
						+ " AND h.DateOrdered BETWEEN ? AND ?");
				}
				else
				{
					sql.append("SELECT h.C_Currency_ID, SUM(l.LineNetAmt) AS Amt,"
						+ " SUM(l.QtyOrdered) AS Qty, "
						+ "NULL, NULL, NULL, NULL, MAX(h.DateOrdered) "
						+ "FROM C_Order h"
						+ " INNER JOIN C_OrderLine l ON (h.C_Order_ID = l.C_Order_ID) "
						+ "WHERE h.DocStatus IN ('CL','CO')"
						+ " AND h.IsSOTrx='Y'"
						+ " AND h.AD_Client_ID = ?"
						+ " AND h.DateOrdered BETWEEN ? AND ?");
				}
			}
			else 	//	Invoice Basis
			{
				if (commission.isListDetails())
				{
					sql.append("SELECT h.C_Currency_ID, l.LineNetAmt, l.QtyInvoiced, "
						+ "NULL, l.C_InvoiceLine_ID, h.DocumentNo,"
						+ " COALESCE(prd.Value,l.Description),h.DateInvoiced "
						+ "FROM C_Invoice h"
						+ " INNER JOIN C_InvoiceLine l ON (h.C_Invoice_ID = l.C_Invoice_ID)"
						+ " LEFT OUTER JOIN M_Product prd ON (l.M_Product_ID = prd.M_Product_ID) "
						+ "WHERE h.DocStatus IN ('CL','CO','RE')"
						+ " AND h.IsSOTrx='Y'"
						+ " AND h.AD_Client_ID = ?"
						+ " AND h.DateInvoiced BETWEEN ? AND ?");
				}
				else
				{
					sql.append("SELECT h.C_Currency_ID, SUM(l.LineNetAmt) AS Amt,"
						+ " SUM(l.QtyInvoiced) AS Qty, "
						+ "NULL, NULL, NULL, NULL, MAX(h.DateInvoiced) "
						+ "FROM C_Invoice h"
						+ " INNER JOIN C_InvoiceLine l ON (h.C_Invoice_ID = l.C_Invoice_ID) "
						+ "WHERE h.DocStatus IN ('CL','CO','RE')"
						+ " AND h.IsSOTrx='Y'"
						+ " AND h.AD_Client_ID = ?"
						+ " AND h.DateInvoiced BETWEEN ? AND ?");
				}
				//	Dunning Level
				if (commissionLine.getC_DunningLevel_ID() != 0) {
					sql.append(" AND h.C_DunningLevel_ID=").append(commissionLine.getC_DunningLevel_ID());
				}
				//	Collection Status
				if (commissionLine.getInvoiceCollectionType() != null) {
					sql.append(" AND h.InvoiceCollectionType='").append(commissionLine.getInvoiceCollectionType()).append("'");
				}
			}
			//	CommissionOrders/Invoices
			if (commissionLine.isCommissionOrders()) {
				MUser[] users = MUser.getOfBPartner(getCtx(), salesRep.getC_BPartner_ID(), get_TrxName());
				if (users == null || users.length == 0) {
					continue;
				}
				//	
				if (users.length == 1) {
					int SalesRep_ID = users[0].getAD_User_ID();
					sql.append(" AND h.SalesRep_ID=").append(SalesRep_ID);
				} else {
					log.warning("Not 1 User/Contact for C_BPartner_ID=" 
						+ salesRep.getC_BPartner_ID() + " but " + users.length);
					sql.append(" AND EXISTS(SELECT 1 FROM AD_User u WHERE u.AD_User_ID = h.SalesRep_ID AND u.C_BPartner_ID=")
						.append(salesRep.getC_BPartner_ID()).append(")");
				}
			}
			//	Organization
			if (commissionLine.getOrg_ID() != 0) {
				sql.append(" AND h.AD_Org_ID=").append(commissionLine.getOrg_ID());
			}
			//	BPartner
			if (commissionLine.getC_BPartner_ID() != 0) {
				sql.append(" AND h.C_BPartner_ID=").append(commissionLine.getC_BPartner_ID());
			}
			//	BPartner Group
			if (commissionLine.getC_BP_Group_ID() != 0) {
				sql.append(" AND EXISTS(SELECT 1 FROM C_BPartner "
						+ "						WHERE C_BPartner.C_BPartner_ID = h.C_BPartner_ID "
						+ "						AND C_BP_Group_ID=").append(commissionLine.getC_BP_Group_ID()).append(")");
			}
			//	Sales Region
			if (commissionLine.getC_SalesRegion_ID() != 0) {
				sql.append(" AND (h.C_BPartner_Location_ID IN "
						+ "(SELECT C_BPartner_Location_ID FROM C_BPartner_Location WHERE C_SalesRegion_ID ").append(getSalesRegionClause(salesRegion, commissionLine.getC_SalesRegion_ID())).append(")"
								+ " OR EXISTS(SELECT 1 FROM C_Order o "
								+ "					INNER JOIN C_BPartner_Location bpl ON(bpl.C_BPartner_Location_ID = o.C_BPartner_Location_ID)"
								+ "					WHERE o.C_Order_ID = h.C_Order_ID "
								+ "					AND bpl.C_SalesRegion_ID " + getSalesRegionClause(salesRegion, commissionLine.getC_SalesRegion_ID()) + "))");
			}
			//	Product
			if (commissionLine.getM_Product_ID() != 0) {
				sql.append(" AND l.M_Product_ID=").append(commissionLine.getM_Product_ID());
			}
			//	Product Category
			if (commissionLine.getM_Product_Category_ID() != 0) {
				sql.append(" AND EXISTS(SELECT 1 FROM M_Product "
						+ "					WHERE M_Product.M_Product_ID = l.M_Product_ID "
						+ "					AND M_Product_Category_ID=").append(commissionLine.getM_Product_Category_ID()).append(")");
			}
			//	Product Group
			if (commissionLine.getM_Product_Group_ID() != 0) {
				sql.append(" AND EXISTS(SELECT 1 FROM M_Product "
						+ "					WHERE M_Product.M_Product_ID = l.M_Product_ID "
						+ "					AND M_Product_Group_ID=").append(commissionLine.getM_Product_Group_ID()).append(")");
			}
			//	Product Class
			if (commissionLine.getM_Product_Class_ID() != 0) {
				sql.append(" AND EXISTS(SELECT 1 FROM M_Product "
						+ "					WHERE M_Product.M_Product_ID = l.M_Product_ID "
						+ "					AND M_Product_Class_ID=").append(commissionLine.getM_Product_Class_ID()).append(")");
			}
			//	Product Classification
			if (commissionLine.getM_Product_Classification_ID() != 0) {
				sql.append(" AND EXISTS(SELECT 1 FROM M_Product "
						+ "					WHERE M_Product.M_Product_ID = l.M_Product_ID "
						+ "					AND M_Product_Classification_ID=").append(commissionLine.getM_Product_Classification_ID()).append(")");
			}
			//	Project
			if (commissionLine.getC_Project_ID() != 0) {
				sql.append(" AND l.C_Project_ID=").append(commissionLine.getC_Project_ID());
			}
			//	Campaign
			if (commissionLine.getC_Campaign_ID() != 0) {
				sql.append(" AND l.C_Campaign_ID=").append(commissionLine.getC_Campaign_ID());
			}
			//	Channel
			if (commissionLine.getC_Channel_ID() != 0) {
				sql.append(" AND EXISTS(SELECT 1 FROM C_Campaign "
						+ "					WHERE C_Campaign.C_Campaign_ID = l.C_Campaign_ID "
						+ "					AND C_Campaign.C_Channel_ID=").append(commissionLine.getC_Channel_ID()).append(")");
			}
			//	Payment Rule
			if (commissionLine.getPaymentRule() != null) {
				sql.append(" AND h.PaymentRule='").append(commissionLine.getPaymentRule()).append("'");
			}
			sql.append(getExclusionWhere(commission.getDocBasisType(), commissionLine, commissionLines));
			if (!commission.isListDetails()) {
				sql.append(" GROUP BY h.C_Currency_ID");
			}
			//
			log.fine("Line=" + commissionLine.getLine() + " - " + sql);
			
			// Here the actual calculation is performed
			createDetail(sql.toString(), commission, comAmt);
			if(comAmt.getDetails().length==0)
				comAmt.deleteEx(true, get_TrxName());
			else  {					
				comAmt.calculateCommission();
				comAmt.saveEx();
			}
		}	//	for all commission lines
		
		//	Save Last Run
		commission.setDateLastRun(getDateDoc());
		commission.saveEx();
		return "@C_CommissionRun_ID@ = " + getDocumentNo() 
			+ " - " + getDescription();
	}	//	processLine
	
	/**
	 * Load Child from Sales Region
	 * @param salesRegionList
	 * @param salesRegionId
	 * @return void
	 */
	private void loadSalesRegion(List<Integer> salesRegionList, int salesRegionId) {
		int m_Tree_ID = MTree.getDefaultAD_Tree_ID(getAD_Client_ID(), X_C_SalesRegion.Table_Name);
		final String sql = "SELECT tn.Node_ID AS C_SalesRegion_ID " +
				"FROM AD_Tree t " +
				"INNER JOIN AD_TreeNode tn ON(t.AD_Tree_ID = tn.AD_Tree_ID) " +
				"WHERE t.AD_Tree_ID = ? AND tn.Parent_ID = ?";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try{
			pstmt = DB.prepareStatement(sql,null);
			pstmt.setInt(1, m_Tree_ID);
			pstmt.setInt(2, salesRegionId);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				MSalesRegion salesRegion = new MSalesRegion(getCtx(), rs.getInt("C_SalesRegion_ID"), get_TrxName());
				if(salesRegion.getC_SalesRegion_ID() <= 0)
					continue;
				if(!salesRegion.isSummary()) {	//	Fill
					salesRegionList.add(salesRegion.getC_SalesRegion_ID());
				} else {	//	Reload
					loadSalesRegion(salesRegionList, salesRegion.getC_SalesRegion_ID());
				}
			}
		} catch (Exception e){
			log.log(Level.SEVERE, sql, e);
		} finally{
			DB.close(rs, pstmt);
			rs = null; pstmt = null;
		}
	}
	
	/**
	 * Get Where clause for Sales Region
	 * @param salesRegionList
	 * @param salesRegionId
	 * @return
	 * @return String
	 */
	private String getSalesRegionClause(List<Integer> salesRegionList, int salesRegionId) {
		//	Reload
		if(salesRegionList.size() == 0) {	
			//	Load Sales Region
			loadSalesRegion(salesRegionList, salesRegionId);
		}
		//	
		StringBuffer srClause = new StringBuffer();
		//	Valid Count
		if(salesRegionList.size() == 0) {
			srClause.append("= " + salesRegionId);
		} else {
			srClause = new StringBuffer("IN(");
			for(int i = 0; i < salesRegionList.size(); i++) {
				//	Add separator
				if(i > 0) {
					srClause.append(", ");
				}
				//	Add Value
				srClause.append(salesRegionList.get(i));
			}
			//	Add last
			srClause.append(")");
		}
		
		return srClause.toString();
	}
	
	/**
	 * Exclusion Where
	 * @param docBasisType
	 * @param currentLine
	 * @param lines
	 * @return
	 * @return String
	 */
	private String getExclusionWhere(String docBasisType, MCommissionLine currentLine, MCommissionLine[] lines) {
		//	Array of values
		List<Integer> orgId = new ArrayList<Integer>();
		List<Integer> bPartnerId = new ArrayList<Integer>();
		List<Integer> bPGroupId = new ArrayList<Integer>();
		List<Integer> salesRegionId = new ArrayList<Integer>();
		List<Integer> productId = new ArrayList<Integer>();
		List<Integer> productCategoryId = new ArrayList<Integer>();
		List<Integer> productGroupId = new ArrayList<Integer>();
		List<Integer> productClassId = new ArrayList<Integer>();
		List<Integer> productClassificationId = new ArrayList<Integer>();
		List<Integer> projectId = new ArrayList<Integer>();
		List<Integer> campaignId = new ArrayList<Integer>();
		List<Integer> channelId = new ArrayList<Integer>();
		List<String> paymentRule = new ArrayList<String>();
		//	For Invoice
		List<Integer> dunningLevelId = new ArrayList<Integer>();
		List<String> invoiceCollectionType = new ArrayList<String>();
		//	
		for(MCommissionLine line : lines) {
			//	ignore current line
			if(line.getC_CommissionLine_ID() == currentLine.getC_CommissionLine_ID())
				continue;
			//	Do it
			//	Organization
			if (currentLine.getOrg_ID() == 0
					&& line.getOrg_ID() != 0) {
				orgId.add(line.getOrg_ID());
			}
			//	BPartner
			if (currentLine.getC_BPartner_ID() == 0
					&& line.getC_BPartner_ID() != 0) {
				bPartnerId.add(line.getC_BPartner_ID());
			}
			//	BPartner Group
			if (currentLine.getC_BP_Group_ID() == 0
					&& line.getC_BP_Group_ID() != 0) {
				bPGroupId.add(line.getC_BP_Group_ID());
			}
			//	Sales Region
			if (currentLine.getC_SalesRegion_ID() == 0
					&& line.getC_SalesRegion_ID() != 0) {
				salesRegionId.add(line.getC_SalesRegion_ID());
			}
			//	Product
			if (currentLine.getM_Product_ID() == 0
					&& line.getM_Product_ID() != 0) {
				productId.add(line.getM_Product_ID());
			}
			//	Product Category
			if (currentLine.getM_Product_Category_ID() == 0
					&& line.getM_Product_Category_ID() != 0) {
				productCategoryId.add(line.getM_Product_Category_ID());
			}
			//	Product Group
			if (currentLine.getM_Product_Group_ID() == 0
					&& line.getM_Product_Group_ID() != 0) {
				productGroupId.add(line.getM_Product_Group_ID());
			}
			//	Product Class
			if (currentLine.getM_Product_Class_ID() == 0
					&& line.getM_Product_Class_ID() != 0) {
				productClassId.add(line.getM_Product_Class_ID());
			}
			//	Product Classification
			if (currentLine.getM_Product_Classification_ID() == 0
					&& line.getM_Product_Classification_ID() != 0) {
				productClassificationId.add(line.getM_Product_Classification_ID());
			}
			//	Payment Rule
			if (currentLine.getPaymentRule() == null
					&& line.getPaymentRule() != null) {
				paymentRule.add(line.getPaymentRule());
			}
			//	Project
			if (currentLine.getC_Project_ID() == 0
					&& line.getC_Project_ID() != 0) {
				projectId.add(line.getC_Project_ID());
			}
			//	Campaign
			if (currentLine.getC_Campaign_ID() == 0
					&& line.getC_Campaign_ID() != 0) {
				campaignId.add(line.getC_Campaign_ID());
			}
			//	Channel
			if (currentLine.getC_Channel_ID() == 0
					&& line.getC_Channel_ID() != 0) {
				channelId.add(line.getC_Channel_ID());
			}
			//	Only for invoice
			if(docBasisType.equals(MCommission.DOCBASISTYPE_Invoice)) {
				//	Dunning Level
				if (currentLine.getC_DunningLevel_ID() == 0
						&& line.getC_DunningLevel_ID() != 0) {
					dunningLevelId.add(line.getC_DunningLevel_ID());
				}
				//	Collection status
				if (currentLine.getInvoiceCollectionType() == null
						&& line.getInvoiceCollectionType() != null) {
					invoiceCollectionType.add(line.getInvoiceCollectionType());
				}
			}
		}
		//	Process Values
		StringBuffer sql = new StringBuffer();
		//	Organization
		if (orgId.size() != 0) {
			sql.append(" AND h.AD_Org_ID NOT IN").append(orgId.toString().replace('[','(').replace(']',')'));
		}
		//	BPartner
		if (bPartnerId.size() != 0) {
			sql.append(" AND h.C_BPartner_ID NOT IN").append(bPartnerId.toString().replace('[','(').replace(']',')'));
		}
		//	BPartner Group
		if (bPGroupId.size() != 0) {
			sql.append(" AND EXISTS"
				+ "(SELECT 1 FROM C_BPartner bp "
				+ "		WHERE bp.C_BPartner_ID = h.C_BPartner_ID "
				+ "		AND bp.C_BP_Group_ID NOT IN").append(bPGroupId.toString().replace('[','(').replace(']',')')).append(")");
		}
		//	Sales Region
		if (salesRegionId.size() != 0) {
			sql.append(" AND EXISTS"
				+ "(SELECT 1 FROM C_BPartner_Location l "
				+ "		WHERE l.C_BPartner_Location_ID = h.C_BPartner_Location_ID "
				+ "		AND C_SalesRegion_ID NOT IN").append(salesRegionId.toString().replace('[','(').replace(']',')')).append(")");
		}
		//	Product
		if (productId.size() != 0) {
			sql.append(" AND l.M_Product_ID NOT IN").append(productId.toString().replace('[','(').replace(']',')')).append(")");
		}
		//	Product Category
		if (productCategoryId.size() != 0) {
			sql.append(" AND EXISTS"
				+ "(SELECT 1 FROM M_Product p "
				+ "		WHERE p.M_Product_ID = l.M_Product_ID "
				+ "		AND M_Product_Category_ID NOT IN").append(productCategoryId.toString().replace('[','(').replace(']',')')).append(")");
		}
		//	Product Group
		if (productGroupId.size() != 0) {
			sql.append(" AND EXISTS"
				+ "(SELECT 1 FROM M_Product p "
				+ "		WHERE p.M_Product_ID = l.M_Product_ID "
				+ "		AND M_Product_Group_ID NOT IN").append(productGroupId.toString().replace('[','(').replace(']',')')).append(")");
		}
		//	Product Class
		if (productClassId.size() != 0) {
			sql.append(" AND EXISTS"
				+ "(SELECT 1 FROM M_Product p "
				+ "		WHERE p.M_Product_ID = l.M_Product_ID "
				+ "		AND M_Product_Class_ID NOT IN").append(productClassId.toString().replace('[','(').replace(']',')')).append(")");
		}
		//	Classification
		if (productClassId.size() != 0) {
			sql.append(" AND EXISTS"
				+ "(SELECT 1 FROM M_Product p "
				+ "		WHERE p.M_Product_ID = l.M_Product_ID "
				+ "		AND M_Product_Classification_ID NOT IN").append(productClassificationId.toString().replace('[','(').replace(']',')')).append(")");
		}
		//	Project
		if (projectId.size() != 0) {
			sql.append(" AND l.C_Project_ID NOT IN").append(projectId.toString().replace('[','(').replace(']',')')).append(")");
		}
		//	Campaign
		if (campaignId.size() != 0) {
			sql.append(" AND l.C_Campaign_ID NOT IN").append(campaignId.toString().replace('[','(').replace(']',')')).append(")");
		}
		//	Channel
		if (campaignId.size() != 0) {
			sql.append(" AND EXISTS(SELECT 1 FROM C_Campaign "
					+ "					WHERE C_Campaign.C_Campaign_ID = l.C_Campaign_ID "
					+ "					AND C_Campaign.C_Channel_ID NOT IN").append(campaignId.toString().replace('[','(').replace(']',')')).append(")");
		}
		//	Payment Rule
		if (paymentRule.size() != 0) {
			sql.append(" AND h.PaymentRule NOT IN('").append(paymentRule.toString()
					.replace('[', ' ').replace(']',')').replaceAll(",", ",'")).append(")");
		}
		//	Only for invoice
		if(docBasisType.equals(MCommission.DOCBASISTYPE_Invoice)) {
			//	Dunning Level
			if (dunningLevelId.size() != 0) {
				sql.append(" AND h.C_DunningLevel_ID NOT IN").append(dunningLevelId.toString().replace('[','(').replace(']',')')).append(")");
			}
			//	Collection Status
			if (invoiceCollectionType.size() != 0) {
				sql.append(" AND h.InvoiceCollectionType NOT IN('").append(invoiceCollectionType.toString()
						.replace('[', ' ').replace(']',')').replaceAll(",", ",'")).append(")");
			}
		}
		//	
		return sql.toString();
	}
	
	
	/**
	 * 	Set Start and End Date
	 */
	private void setStartEndDate(String frequencyType) {
		GregorianCalendar cal = new GregorianCalendar(Language.getLoginLanguage().getLocale());
		cal.setTimeInMillis(getDateDoc().getTime());
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		//	Yearly
		if (MCommission.FREQUENCYTYPE_Yearly.equals(frequencyType)) {
			cal.set(Calendar.DAY_OF_YEAR, 1);
			setStartDate(new Timestamp (cal.getTimeInMillis()));
			//
			cal.add(Calendar.YEAR, 1);
			cal.add(Calendar.DAY_OF_YEAR, -1); 
			setEndDate(new Timestamp (cal.getTimeInMillis()));
			
		}
		//	Quarterly
		else if (MCommission.FREQUENCYTYPE_Quarterly.equals(frequencyType)) {
			cal.set(Calendar.DAY_OF_MONTH, 1);
			int month = cal.get(Calendar.MONTH);
			if (month < Calendar.APRIL)
				cal.set(Calendar.MONTH, Calendar.JANUARY);
			else if (month < Calendar.JULY)
				cal.set(Calendar.MONTH, Calendar.APRIL);
			else if (month < Calendar.OCTOBER)
				cal.set(Calendar.MONTH, Calendar.JULY);
			else
				cal.set(Calendar.MONTH, Calendar.OCTOBER);
			setStartDate(new Timestamp (cal.getTimeInMillis()));
			//
			cal.add(Calendar.MONTH, 3);
			cal.add(Calendar.DAY_OF_YEAR, -1); 
			setEndDate(new Timestamp (cal.getTimeInMillis()));
		}
		//	Weekly
		else if (MCommission.FREQUENCYTYPE_Weekly.equals(frequencyType)) {
			cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
			setStartDate(new Timestamp (cal.getTimeInMillis()));
			//
			cal.add(Calendar.DAY_OF_YEAR, 7); 
			setEndDate(new Timestamp (cal.getTimeInMillis()));
		}
		//	Monthly
		else {
			cal.set(Calendar.DAY_OF_MONTH, 1);
			setStartDate(new Timestamp (cal.getTimeInMillis()));
			//
			cal.add(Calendar.MONTH, 1);
			cal.add(Calendar.DAY_OF_YEAR, -1); 
			setEndDate(new Timestamp (cal.getTimeInMillis()));
		}
		log.fine("setStartEndDate = " + getStartDate() + " - " + getEndDate());
	}	//	setStartEndDate
	
	/**
	 * 	Approve Document
	 * 	@return true if success 
	 */
	public boolean  approveIt()
	{
		log.info("approveIt - " + toString());
		setIsApproved(true);
		return true;
	}	//	approveIt
	
	/**
	 * 	Reject Approval
	 * 	@return true if success 
	 */
	public boolean rejectIt()
	{
		log.info("rejectIt - " + toString());
		setIsApproved(false);
		return true;
	}	//	rejectIt
	
	/**
	 * 	Complete Document
	 * 	@return new status (Complete, In Progress, Invalid, Waiting ..)
	 */
	public String completeIt()
	{
		//	Re-Check
		if (!m_justPrepared)
		{
			String status = prepareIt();
			if (!DocAction.STATUS_InProgress.equals(status))
				return status;
		}

		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_BEFORE_COMPLETE);
		if (m_processMsg != null)
			return DocAction.STATUS_Invalid;
		
		//	Implicit Approval
		if (!isApproved())
			approveIt();
		log.info(toString());
		//
		
		//	User Validation
		String valid = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_AFTER_COMPLETE);
		if (valid != null)
		{
			m_processMsg = valid;
			return DocAction.STATUS_Invalid;
		}
		//	Set Definitive Document No
		setDefiniteDocumentNo();

		setProcessed(true);
		setDocAction(DOCACTION_Close);
		return DocAction.STATUS_Completed;
	}	//	completeIt
	
	/**
	 * 	Set the definite document number after completed
	 */
	private void setDefiniteDocumentNo() {
		MDocType dt = MDocType.get(getCtx(), getC_DocType_ID());
		if (dt.isOverwriteDateOnComplete()) {
			setStartDate(new Timestamp(System.currentTimeMillis()));
		}
		if (dt.isOverwriteSeqOnComplete()) {
			String value = null;
			int index = p_info.getColumnIndex("C_DocType_ID");
			if (index == -1)
				index = p_info.getColumnIndex("C_DocTypeTarget_ID");
			if (index != -1)		//	get based on Doc Type (might return null)
				value = DB.getDocumentNo(get_ValueAsInt(index), get_TrxName(), true);
			if (value != null) {
				setDocumentNo(value);
			}
		}
	}

	/**
	 * 	Void Document.
	 * 	Same as Close.
	 * 	@return true if success 
	 */
	public boolean voidIt()
	{
		log.info("voidIt - " + toString());
		return closeIt();
	}	//	voidIt
	
	/**
	 * 	Close Document.
	 * 	Cancel not delivered Qunatities
	 * 	@return true if success 
	 */
	public boolean closeIt()
	{
		log.info("closeIt - " + toString());

		//	Close Not delivered Qty
		setDocAction(DOCACTION_None);
		return true;
	}	//	closeIt
	
	/**
	 * 	Reverse Correction
	 * 	@return true if success 
	 */
	public boolean reverseCorrectIt()
	{
		log.info("reverseCorrectIt - " + toString());
		return false;
	}	//	reverseCorrectionIt
	
	/**
	 * 	Reverse Accrual - none
	 * 	@return true if success 
	 */
	public boolean reverseAccrualIt()
	{
		log.info("reverseAccrualIt - " + toString());
		return false;
	}	//	reverseAccrualIt
	
	/** 
	 * 	Re-activate
	 * 	@return true if success 
	 */
	public boolean reActivateIt()
	{
		log.info("reActivateIt - " + toString());
		deleteMovements();
		setProcessed(false);
		return true;
	}	//	reActivateIt
	
	
	/*************************************************************************
	 * 	Get Summary
	 *	@return Summary of Document
	 */
	public String getSummary()
	{
		StringBuffer sb = new StringBuffer();
		sb.append(getDocumentNo());
	//	sb.append(": ")
	//		.append(Msg.translate(getCtx(),"TotalLines")).append("=").append(getTotalLines())
	//		.append(" (#").append(getLines(false).length).append(")");
		//	 - Description
		if (getDescription() != null && getDescription().length() > 0)
			sb.append(" - ").append(getDescription());
		return sb.toString();
	}	//	getSummary

	/**
	 * 	Get Process Message
	 *	@return clear text error message
	 */
	public String getProcessMsg()
	{
		return m_processMsg;
	}	//	getProcessMsg
	
	/**
	 * 	Get Document Owner (Responsible)
	 *	@return AD_User_ID
	 */
	public int getDoc_User_ID()
	{
	//	return getSalesRep_ID();
		return 0;
	}	//	getDoc_User_ID

	/**
	 * 	Get Document Approval Amount
	 *	@return amount
	 */
	public BigDecimal getApprovalAmt()
	{
		return null;	//getTotalLines();
	}	//	getApprovalAmt
	
	/**
	 * 	Get Document Currency
	 *	@return C_Currency_ID
	 */
	public int getC_Currency_ID()
	{
	//	MPriceList pl = MPriceList.get(getCtx(), getM_PriceList_ID());
	//	return pl.getC_Currency_ID();
		return 0;
	}	//	getC_Currency_ID
	
	@Override
	public int customizeValidActions(String docStatus, Object processing,
			String orderType, String isSOTrx, int AD_Table_ID,
			String[] docAction, String[] options, int index) {
			if (docStatus.equals(DocumentEngine.STATUS_Completed))	{
				//	Reactivate
				options[index++] = DocumentEngine.ACTION_ReActivate;
				//	Void
				options[index++] = DocumentEngine.ACTION_Void;
				//	Close
				options[index++] = DocumentEngine.ACTION_Close;
			}	else if(docStatus.equals(DocumentEngine.STATUS_InProgress)
							|| docStatus.equals(DocumentEngine.STATUS_Drafted)){
				//	Prepare
				options[index++] = DocumentEngine.ACTION_Prepare;
				//	Complete
				options[index++] = DocumentEngine.ACTION_Complete;
				//	Void
				options[index++] = DocumentEngine.ACTION_Void;
			}
		return index;
	}

    @Override
    public String toString()
    {
      StringBuffer sb = new StringBuffer ("MCCommissionRun[")
        .append(getSummary()).append("]");
      return sb.toString();
    }
}