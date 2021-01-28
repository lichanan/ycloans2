package com.yuchengtech.ycloans.db.dao.imp;


//import java.util.List;
import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.util.StringUtils;

import com.yucheng.cmis.pub.util.StringUtil;
import com.yuchengtech.ycloans.LoanVarDef;
import com.yuchengtech.ycloans.ServiceContainer;
import com.yuchengtech.ycloans.buz.BatchJobContext;
import com.yuchengtech.ycloans.common.SystemInfo;
import com.yuchengtech.ycloans.common.enumeration.ATPYState;
import com.yuchengtech.ycloans.common.enumeration.AccountState;
import com.yuchengtech.ycloans.common.enumeration.GenGlInd;
import com.yuchengtech.ycloans.common.enumeration.JobBussType;
import com.yuchengtech.ycloans.common.enumeration.LoanDevaInd;
import com.yuchengtech.ycloans.common.enumeration.LoanRateMode;
import com.yuchengtech.ycloans.common.enumeration.LoanRepayMethod;
import com.yuchengtech.ycloans.common.enumeration.LoanState;
import com.yuchengtech.ycloans.common.enumeration.LoanStpAccInd;
import com.yuchengtech.ycloans.common.enumeration.PaymentFreq;
import com.yuchengtech.ycloans.common.enumeration.YnFlag;
import com.yuchengtech.ycloans.db.DAOContainer;
import com.yuchengtech.ycloans.db.DBConst;
import com.yuchengtech.ycloans.db.DBSqlUtils;
import com.yuchengtech.ycloans.db.HibernatePager;
import com.yuchengtech.ycloans.db.Pager;
import com.yuchengtech.ycloans.db.dao.LmLoanDAO;
import com.yuchengtech.ycloans.db.domain.CheckAccountInfo;
import com.yuchengtech.ycloans.db.domain.LmGlTx;
import com.yuchengtech.ycloans.db.domain.LmGlnoLog;
import com.yuchengtech.ycloans.db.domain.LmLoan;
import com.yuchengtech.ycloans.db.domain.LmLoanCont;
import com.yuchengtech.ycloans.db.domain.LmLoanForPaym;
import com.yuchengtech.ycloans.db.domain.LoanAfterRecord;
import com.yuchengtech.ycloans.db.domain.PLoanTypGlMap;
import com.yuchengtech.ycloans.exception.YcloansDBException;

/**
 * Data access object (DAO) for domain model class LmLoan.
 * 
 * @see com.yctech.ycloans.db.LmLoan
 * @author MyEclipse Persistence Tools
 */

public class LmLoanDAOImp extends BaseDAOImp implements LmLoanDAO {
	// private static final Log log = LogFactory.getLog(LmLoanDAOImp.class);

	// private static final Log log = LogFactory.getLog(LmLoanDAOImp.class);
	/**
	 * 根据借据号查询借据对象
	 * 
	 * @param 借据号
	 * @return
	 */
	public LmLoan findById(Connection conn, java.lang.String id) {
		return (LmLoan) this.getHibernateTemplate().get(LmLoan.class, id);
	}

	/**
	 * 保存借据主表信息
	 * 
	 * @param lmLoan
	 */
	public void save(Connection conn, LmLoan lmLoan) {
		// log.debug("saving LmLoan instance");
		try {
			getHibernateTemplate().save(lmLoan);
		} catch (RuntimeException e) {
			e.printStackTrace();
			throw new YcloansDBException("find error!", e);
		}
	}

	/**
	 * 查找利息计提数据
	 * 
	 * @param conn
	 * @param bankCde
	 * @param bchCdes
	 * @return
	 */
	public Pager findByForIntAcc(Connection conn, String bankCde,
			StringBuffer bchCdes, String buzDate, String threadCntStr) {
		try {
			StringBuilder queryString = new StringBuilder();
			queryString.append("from LmLoan ll where ");

			if (SystemInfo.getSystemInfo().getSystemParameters()
					.getAccountRule().isSuspAcc()) {
				queryString.append(" ll.loanSts=?  and ll.loanIntRate>0");
			} else {
				// queryString
				// .append(" ll.loanSts=? and ll.loanStpAccInd='N' and ll.loanIntRate>0");
				queryString.append(" ll.loanSts=? and ll.loanStpAccInd='N' ");// haixia 会出现利率调整后免息的情况
																				// hbs
																				// 20150605
			}
			queryString.append("and ll.prcsPageDtInd='N' ");
			if (!SystemInfo.getSystemInfo().getAccountRule().isDevLoanAcc()) {
				queryString.append(" and ll.loanDevaInd='N' ");
			}
			queryString
					.append("and (ll.lastIntAccDt<? or ll.lastIntAccDt is null)");
			if (StringUtils.hasText(threadCntStr)) {
				queryString.append(" and ll.thdCnt in(").append(threadCntStr)
						.append(") ");
			}
			//modified by fanyl on 2015-10-09 for 核销贷款是否计提
			Serializable[] param = null;
			if(SystemInfo.getSystemInfo().getAccountRule().isChrgoAcc()) {
				queryString.append(" and ll.loanDebtSts in (?,?,?)  ");
				param = new Serializable[] {
						AccountState.GIVED.getCodeInDb(), buzDate,
						LoanState.NORMAL.getCodeInDb(),
						LoanState.DELQ.getCodeInDb(),LoanState.OFFED.getCodeInDb()};
			} else {
				queryString.append(" and ll.loanDebtSts in (?,?)  ");
				param = new Serializable[] {
						AccountState.GIVED.getCodeInDb(), buzDate, 
						LoanState.NORMAL.getCodeInDb(),
						LoanState.DELQ.getCodeInDb()};

			}

			return new HibernatePager(DAOContainer.getCommonDbDAO(false),
					queryString.toString(), param);
		} catch (RuntimeException e) {
			e.printStackTrace();
			throw new YcloansDBException("find error!", e);
		}
	}

	/**
	 * 查找利息计提数据
	 * 
	 * @param conn
	 * @param bankCde
	 * @param bchCdes
	 * @return
	 */
	public Pager findByForIntAcc1(Connection conn, String bankCde,
			StringBuffer bchCdes, String buzDate, String threadCntStr) {
		try {
			StringBuilder queryString = new StringBuilder();
			queryString.append("from LmLoan ll where ");
			// 查询条件排除委托贷款 bussTyp<>TLOAN and loanSts='ACTV'
			// lnCostProdDt 为空 或者不为当前日期
			queryString.append(" ll.bussTyp<>'TLOAN' ");
			queryString.append(" and ll.loanSts='ACTV'  ");
			/*queryString
					.append(" and (ll.lnCostProdDt is null or ll.lnCostProdDt<>? )");*/
			Serializable[] param = new Serializable[] { };
			return new HibernatePager(DAOContainer.getCommonDbDAO(false),
					queryString.toString(), param);
		} catch (RuntimeException e) {
			e.printStackTrace();
			throw new YcloansDBException("find error!", e);
		}
	}

	/**
	 * 查询可计提的借据信息
	 * 
	 * @param bankCde
	 *            银行编号
	 * @param bchCde
	 *            银行机构号
	 *@deprecated
	 * @return
	 */
	public Pager findByCD(Connection conn, String bankCde, StringBuffer bchCdes) {
		try {
			StringBuilder queryString = new StringBuilder();
			queryString
					.append("select new com.yuchengtech.ycloans.db.domain.po.LoanAccrualPo(");
			queryString.append("ll.loanNo, ll.bankCde, ll.bchCde,");
			queryString
					.append("ll.loanContNo, ll.loanSts, ll.tnr, ll.lastDueDt,");
			queryString.append("ll.intStartDt, ll.loanDebtSts, ll.fstPaymDt,");
			queryString.append("ll.dueDay, ll.loanOdInd, ll.loanDevaInd,");
			queryString
					.append("ll.loanDevaDt, ll.loanStpAccInd, ll.loanStpAccDt,");
			queryString
					.append("ll.loanActvDt, ll.loanRateMode, ll.loanRateTyp,");
			queryString.append("ll.loanIntRate, ll.loanBaseRate, ll.loanSprd,");
			queryString.append("ll.intAdjPct, ll.rateBase, ll.lastGenOdIntDt,");
			queryString.append("ll.lastIntAccDt, ll.lastSetlDt, ll.curDueDt,");
			queryString
					.append("ll.nextDueDt, ll.instFreqUnit, ll.instFreqFreq,");
			queryString.append("ll.lastRepcDt, ll.nextRepcDt, ll.loanCcy,");
			queryString
					.append("ll.origPrcp, ll.loanOsPrcp, ll.loanTnr, ll.loanOsDay,");
			queryString.append("ll.applDrawdnSeq, ll.loanGrd, ll.chargeDt,");
			queryString
					.append("ll.ratChgEffDt, ll.curMtdNo, ll.loanGlRoleCde,");
			queryString.append("ll.prcsPageDtInd, ll.bussTyp, ll.postHostInd,");
			queryString
					.append("ll.lastDeviIntAccDt, lps.psNormIntAmt, lps.psIncTaken, lps.psIntRate, lps.psRemPrcp) ");
			queryString
					.append("from LmLoan ll,LmPmShd lps, PLoanTyp pt,LmLoanCont lc where ");
			queryString
					.append("ll.loanNo=lps.id.loanNo and ll.curDueDt=lps.psDueDt and ll.loanSts=? and ");
			queryString
					.append("ll.loanContNo=lc.loanContNo and lc.loanTyp=pt.id.typCde and pt.typTruInd='N' ");
			queryString
					.append("and ll.loanDevaInd=? and ll.loanDebtSts=? and ll.bankCde=? and ll.bchCde in (");
			queryString.append(bchCdes);
			queryString.append(") and ll.prcsPageDtInd='N' ");
			String loanSts = AccountState.GIVED.getCodeInDb();
			String loanDevaInd = LoanDevaInd.NO.getCodeInDb();
			String loanDebtSts = LoanState.NORMAL.getCodeInDb();
			Serializable[] param = new Serializable[] { loanSts, loanDevaInd,
					loanDebtSts, bankCde };
			return new HibernatePager(this, queryString.toString(), param);
		} catch (RuntimeException e) {
			e.printStackTrace();
			throw new YcloansDBException("find error!", e);
		}
	}

	/**
	 * 查找利息计提数据
	 * 
	 * @param conn
	 * @param bankCde
	 * @param bchCdes
	 * @return
	 */
	public Pager findByForIntAcc(Connection conn, String bankCde,
			StringBuffer bchCdes, String buzDate) {
		try {
			StringBuilder queryString = new StringBuilder();
			queryString.append("from LmLoan ll where ");
			queryString.append(" ll.loanSts=? and ");
			queryString.append(" ll.loanIntRate>0");
			queryString.append(" and ");
			queryString
					.append("ll.loanDevaInd=? and ll.bussTyp<>'TLOAN' and  ll.loanDebtSts=? and ll.bankCde=? and ll.bchCde in (");
			queryString.append(bchCdes);
			queryString.append(") and ll.prcsPageDtInd='N' ");
			queryString.append(" and (lastIntAccDt<? or lastIntAccDt is null)");
			String loanSts = AccountState.GIVED.getCodeInDb();
			String loanDevaInd = LoanDevaInd.NO.getCodeInDb();
			String loanDebtSts = LoanState.NORMAL.getCodeInDb();
			Serializable[] param = new Serializable[] { loanSts, loanDevaInd,
					loanDebtSts, bankCde, buzDate };
			return new HibernatePager(this, queryString.toString(), param);
		} catch (RuntimeException e) {
			e.printStackTrace();
			throw new YcloansDBException("find error!", e);
		}
	}

	/**
	 * 更新需要计提的借据的分页标志为N
	 */
	public void updateForFindAccrual(Connection conn, String bankCde,
			StringBuffer bchCdes, String buzDate) {
		try {
			Session session = super.getCurrentHibernateSession();
			StringBuffer queryString = new StringBuffer();
			if (SystemInfo.getSystemInfo().getSystemParameters()
					.getAccountRule().isSuspAcc()) {
				queryString
						.append("update LmLoan ll set ll.prcsPageDtInd='N' where ll.loanSts=? and  ll.loanIntRate>0 ");
			} else {
				// queryString
				// .append("update LmLoan ll set ll.prcsPageDtInd='N' where ll.loanSts=? and ll.loanStpAccInd='N' and ll.loanIntRate>0 ");
				// haixia会出现调整后利率为0的情况
				queryString
						.append("update LmLoan ll set ll.prcsPageDtInd='N' where ll.loanSts=? and ll.loanStpAccInd='N' ");
			}
			queryString.append(" and ll.prcsPageDtInd <> 'N' ");
			if (!SystemInfo.getSystemInfo().getAccountRule().isDevLoanAcc()) {
				queryString.append(" and ll.loanDevaInd='N' ");
			}
			queryString.append("and (lastIntAccDt<? or lastIntAccDt is null) ");
			//modified by fanyl on 2015-10-09 for 核销贷款是否计提
			Query q = null;
			if(SystemInfo.getSystemInfo().getAccountRule().isChrgoAcc()) {
				queryString.append(" and ll.loanDebtSts in (?,?,?)  ");
				q = session.createQuery(queryString.toString());
				q.setParameter(0, AccountState.GIVED.getCodeInDb());
				q.setParameter(1, buzDate);
				q.setParameter(2, LoanState.NORMAL.getCodeInDb());
				q.setParameter(3, LoanState.DELQ.getCodeInDb());
				q.setParameter(4, LoanState.OFFED.getCodeInDb());

			} else {
				queryString.append(" and ll.loanDebtSts in (?,?)  ");
				q = session.createQuery(queryString.toString());
				q.setParameter(0, AccountState.GIVED.getCodeInDb());
				q.setParameter(1, buzDate);
				q.setParameter(2, LoanState.NORMAL.getCodeInDb());
				q.setParameter(3, LoanState.DELQ.getCodeInDb());
			}
			q.executeUpdate();
		} catch (RuntimeException e) {
			e.printStackTrace();
			throw new YcloansDBException("find error!", e);
		}
	}

	/**
	 * 利息计提 批量修改待处理的借据信息状态
	 * 
	 * @param conn
	 * @param bankCde
	 * @param bchCdes
	 * @param buzDate
	 * @param thdCntStr
	 */
	public void updateForFindAccrual1(Connection conn, String bankCde,
			StringBuffer bchCdes, String buzDate) {
		try {
			Session session = super.getCurrentHibernateSession();
			StringBuffer queryString = new StringBuffer();
			queryString
					.append("update LmLoan ll set ll.prcsPageDtInd='N' where ll.bussTyp<>'TLOAN'  ");
			queryString.append("and ll.loanSts='ACTV'  and ll.prcsPageDtInd <> 'N' ");
			/*queryString
					.append("and (ll.lnCostProdDt is null or ll.lnCostProdDt<>? )");*/
			Query q = session.createQuery(queryString.toString());
			//q.setParameter(0, buzDate);
			q.executeUpdate();
		} catch (RuntimeException e) {
			e.printStackTrace();
			throw new YcloansDBException("find error!", e);
		}
	}

	/**
	 * 利息计提 批量修改待处理的借据信息状态
	 */
	public synchronized void updateForFindAccrual(Connection conn,
			String bankCde, StringBuffer bchCdes, String buzDate,
			String thdCntStr) {
		try {
			Session session = super.getCurrentHibernateSession();
			StringBuffer queryString = new StringBuffer();
			if (SystemInfo.getSystemInfo().getSystemParameters()
					.getAccountRule().isSuspAcc()) {
				queryString
						.append("update LmLoan ll set ll.prcsPageDtInd='N' where ll.loanSts=?   and ll.loanIntRate>0 ");
			} else {
				// queryString
				// .append("update LmLoan ll set ll.prcsPageDtInd='N' where ll.loanSts=?  and ll.loanStpAccInd='N' and ll.loanIntRate>0 ");

				// haixia会出现利率调整为0的情况
				queryString
						.append("update LmLoan ll set ll.prcsPageDtInd='N' where ll.loanSts=?  and ll.loanStpAccInd='N' ");
			}
			if (!SystemInfo.getSystemInfo().getAccountRule().isDevLoanAcc()) {
				queryString.append(" and ll.loanDevaInd='N' ");
			}
			queryString.append(" and ll.prcsPageDtInd <> 'N'");
			/*queryString
					.append(" and ll.loanDebtSts in (?,?) ");*/
			queryString.append("and (lastIntAccDt<? or lastIntAccDt is null)");
			queryString.append(" and ll.loanOsPrcp >0");
			queryString.append(" and ll.thdCnt in(").append(thdCntStr).append(
					")");
			//modified by fanyl on 2015-10-09 for 核销贷款是否计提
			Query q = null;
			if(SystemInfo.getSystemInfo().getAccountRule().isChrgoAcc()) {
				queryString.append(" and ll.loanDebtSts in (?,?,?)  ");
				q = session.createQuery(queryString.toString());
				q.setParameter(0, AccountState.GIVED.getCodeInDb());
				q.setParameter(1, buzDate);
				q.setParameter(2, LoanState.NORMAL.getCodeInDb());
				q.setParameter(3, LoanState.DELQ.getCodeInDb());
				q.setParameter(4, LoanState.OFFED.getCodeInDb());

			} else {
				queryString.append(" and ll.loanDebtSts in (?,?)  ");
				q = session.createQuery(queryString.toString());
				q.setParameter(0, AccountState.GIVED.getCodeInDb());
				q.setParameter(1, buzDate);
				q.setParameter(2, LoanState.NORMAL.getCodeInDb());
				q.setParameter(3, LoanState.DELQ.getCodeInDb());
			}

			q.executeUpdate();
		} catch (RuntimeException e) {
			e.printStackTrace();
			throw new YcloansDBException("find error!", e);
		}
	}

	/**
	 * 更新最后扣款日或当前扣款日为当前业务日期的借据的分页标志为N
	 */
	public synchronized void updateForSetlNormInt(Connection conn,
			String bankCde, String buzDate, String thdCntStr) {
		try {
			Session session = super.getCurrentHibernateSession();
			StringBuffer queryString = new StringBuffer();
			queryString
					.append("update LmLoan ll set ll.prcsPageDtInd='N' where ll.loanSts=? ");
			if (!SystemInfo.getSystemInfo().getAccountRule().isDevLoanAcc()) {
				queryString.append(" and ll.loanDevaInd='N' ");
			}
			queryString
					.append(" and ll.prcsPageDtInd <> 'N' ");
			/*queryString
					.append("and ll.loanDevaInd=?  and ll.loanDebtSts in (?,?) ");*/
			queryString.append("and (ll.lastDueDt=? or ll.curDueDt=?)");
			//add by fanyl on 2015-12-10 for 增加利率判断条件
			queryString.append(" and ll.loanIntRate > 0 ");
			queryString.append(" and ll.thdCnt in(").append(thdCntStr).append(
					")");
			//modified by fanyl on 2015-10-09 for 核销后贷款结息
			Query q = null;
			if(SystemInfo.getSystemInfo().getAccountRule().isChrgoSetlInt()) {
				queryString.append(" and ll.loanDebtSts in (?,?,?)  ");
				q = session.createQuery(queryString.toString());
				q.setParameter(0, AccountState.GIVED.getCodeInDb());
				q.setParameter(1, buzDate);
				q.setParameter(2, buzDate);
				q.setParameter(3, LoanState.NORMAL.getCodeInDb());
				q.setParameter(4, LoanState.DELQ.getCodeInDb());
				q.setParameter(5, LoanState.OFFED.getCodeInDb());
			} else {
				queryString.append(" and ll.loanDebtSts in (?,?)  ");
				q = session.createQuery(queryString.toString());
				q.setParameter(0, AccountState.GIVED.getCodeInDb());
				q.setParameter(1, buzDate);
				q.setParameter(2, buzDate);
				q.setParameter(3, LoanState.NORMAL.getCodeInDb());
				q.setParameter(4, LoanState.DELQ.getCodeInDb());
			}
			q.executeUpdate();
		} catch (RuntimeException e) {
			e.printStackTrace();
			throw new YcloansDBException("find error!", e);
		}
	}

	/**
	 * 更新最后扣款日或当前扣款日为当前业务日期的借据的分页标志为N
	 */
	public void updateForSetlNormInt(Connection conn, String bankCde,
			String buzDate) {
		try {
			Session session = super.getCurrentHibernateSession();
			StringBuffer queryString = new StringBuffer();
			queryString
					.append("update LmLoan ll set ll.prcsPageDtInd='N' where ll.loanSts=?  ");
			queryString.append(" and ll.prcsPageDtInd <> 'N' ");
			//queryString.append(" and ll.loanDebtSts in (?,?) ");
			if (!SystemInfo.getSystemInfo().getAccountRule().isDevLoanAcc()) {
				queryString.append(" and ll.loanDevaInd='N' ");
			}
			queryString.append("and (ll.lastDueDt=? or ll.curDueDt=?)");
			//modified by fanyl on 2015-10-09 for 核销后贷款结息
			Query q = null;
			if(SystemInfo.getSystemInfo().getAccountRule().isChrgoSetlInt()) {
				queryString.append(" and ll.loanDebtSts in (?,?,?)  ");
				q = session.createQuery(queryString.toString());
				q.setParameter(0, AccountState.GIVED.getCodeInDb());
				q.setParameter(1, buzDate);
				q.setParameter(2, buzDate);
				q.setParameter(3, LoanState.NORMAL.getCodeInDb());
				q.setParameter(4, LoanState.DELQ.getCodeInDb());
				q.setParameter(5, LoanState.OFFED.getCodeInDb());
			} else {
				queryString.append(" and ll.loanDebtSts in (?,?)  ");
				q = session.createQuery(queryString.toString());
				q.setParameter(0, AccountState.GIVED.getCodeInDb());
				q.setParameter(1, buzDate);
				q.setParameter(2, buzDate);
				q.setParameter(3, LoanState.NORMAL.getCodeInDb());
				q.setParameter(4, LoanState.DELQ.getCodeInDb());
			}
			q.executeUpdate();
		} catch (RuntimeException e) {
			e.printStackTrace();
			throw new YcloansDBException("find error!", e);
		}
	}

	/**
	 * 利息计提 批量修改待处理的借据信息状态
	 */
	public synchronized void updateForFindAccrual1(Connection conn,
			String bankCde, StringBuffer bchCdes, String buzDate,
			String thdCntStr) {
		try {
			Session session = super.getCurrentHibernateSession();
			StringBuffer queryString = new StringBuffer();
			queryString
					.append("update LmLoan ll set ll.prcsPageDtInd='N' where ll.bussTyp<>'TLOAN'  ");
			queryString.append(" and ll.prcsPageDtInd <> 'N' and ll.loanSts='ACTV'  ");
			/*queryString
					.append("and (ll.lnCostProdDt is null or ll.lnCostProdDt<>? )");*/
			Query q = session.createQuery(queryString.toString());
			//q.setParameter(0, buzDate);
			q.executeUpdate();
		} catch (RuntimeException e) {
			e.printStackTrace();
			throw new YcloansDBException("find error!", e);
		}
	}

	/**
	 * 根据贷款状态及逾期标志更新借据的分页标志
	 */
	public void updateForFindAccrual(Connection conn, String bankCde,
			String bchCde, YnFlag loanDevInd, YnFlag loanOdInd,String buzDate,String threadCntStr) {
		StringBuffer queryString = new StringBuffer();
		queryString
				.append("update LmLoan ll set ll.prcsPageDtInd='N' where ll.loanOdInd=? ");
		queryString.append(" and ll.loanSts=? and ll.prcsPageDtInd <> 'N' ");
//		queryString.append(" and ll.bussTyp<>'TLOAN'");
		if (!SystemInfo.getSystemInfo().getAccountRule().isDevLoanAcc()) {
			queryString.append("  and ll.loanDevaInd = 'N'");
		}
		if (!SystemInfo.getSystemInfo().getSystemParameters().getAccountRule()
				.isSuspAcc()) {
			queryString.append(" and ll.loanStpAccInd = 'N' ");// 是否表外
		}
		queryString.append(" and (ll.lastOdIntAccDt is null");
		queryString.append(" or ll.lastOdIntAccDt<?)");
		queryString.append(" and ll.loanOdIntRate>0");
		queryString.append(" and exists(");
		queryString.append(" from LmPmShd where id.loanNo=ll.loanNo");
		queryString.append(" and (psGenProdDt=? or psGenProdDt is null)");
		queryString.append(" )");
		if(ServiceContainer.getSystemParameter().isPayInDay()){//如果采用日间扣款模式 则会根据扣款返回状态来判断改日终任务是否执行
			queryString.append("and (ll.atpySts != 'SK' or ll.atpySts is null)");
		}
		if (StringUtils.hasText(threadCntStr)) {
			queryString.append(" and ll.thdCnt in(").append(threadCntStr)
					.append(") ");
		}
		//modified by fanyl on 2015-10-09 for 核销后贷款计提
		Serializable[] params = null;
		if(SystemInfo.getSystemInfo().getAccountRule().isChrgoAcc()) {
			queryString.append(" and ll.loanDebtSts in (?,?,?,?) ");
			params = new Serializable[8];
			params[0] = loanOdInd.getCodeInDb();
			params[1] = LoanState.ACTIVE.getCodeInDb();
			params[2] = buzDate;
			params[3] = buzDate;
			params[4] = LoanState.OVER.getCodeInDb();
			params[5] = LoanState.DELQ.getCodeInDb();
			params[6] = LoanState.NORMAL.getCodeInDb();
			params[7] = LoanState.OFFED.getCodeInDb();

		} else {
			queryString.append(" and ll.loanDebtSts in (?,?,?) ");
			params = new Serializable[7];
			params[0] = loanOdInd.getCodeInDb();
			params[1] = LoanState.ACTIVE.getCodeInDb();
			params[2] = buzDate;
			params[3] = buzDate;
			params[4] = LoanState.OVER.getCodeInDb();
			params[5] = LoanState.DELQ.getCodeInDb();
			params[6] = LoanState.NORMAL.getCodeInDb();
		}

		try {
			executeUpdateByHql(queryString.toString(), params);
		} catch (RuntimeException e) {
			e.printStackTrace();
			throw new YcloansDBException("find error!", e);
		}
	}

	/**
	 * 根据机构号查询贷款信息
	 */
	public Pager findLoanInfoByBchcde(Connection conn,
			String subBranchCommaString) {

		StringBuilder hql = new StringBuilder();
		hql.append("select lm, pt.typTruInd ");
		hql.append("From com.yuchengtech.ycloans.db.domain.LmLoan lm ,");
		hql.append("com.yuchengtech.ycloans.db.domain.LmLoanCont lc,");
		hql.append("com.yuchengtech.ycloans.db.domain.PLoanTyp pt ");
		hql.append("where lm.nextDueDt<=? and lm.loanSts=? ");
		hql.append("and lm.loanNo = lc.loanNo ");
		hql.append("and lm.loanTyp = pt.id.typCde ");
		hql.append("and lm.bchCde in (" + subBranchCommaString + ") ");
		hql.append("and lm.prcsPageDtInd='N'");
		hql.append("and lm.loanOdInd='Y'");
		hql.append("and lm.loanDebtSts in ('NORM','OVER')");
		String operationDay = SystemInfo.getSystemInfo().getBuzDate();
		Serializable[] params = new Serializable[2];
		params[0] = operationDay;
		params[1] = LoanState.ACTIVE.getCodeInDb();
		try {
			return new HibernatePager(this, hql.toString(), params);
		} catch (Exception e) {
			e.printStackTrace();
			throw new YcloansDBException("findLoanInfoByBchcde error!", e);
		}
	}
	/**
	 * 将满足条件的数据操作标识设置为未处理 N
	 */
	public void executeUpdatePrcsPageDtInd(Connection conn,
			String subBranchCommaString, String flag, String buzDate, String threadCntStr) {
		Serializable[] params = null;
		StringBuffer hql = new StringBuffer(
				"update LmLoan lm set lm.prcsPageDtInd = ? where lm.loanSts = ? and lm.prcsPageDtInd <> 'N'");
		if(ServiceContainer.getSystemParameter().isPayInDay()){//如果采用日间扣款模式 则会根据扣款返回状态来判断改日终任务是否执行
			hql.append(" and (lm.atpySts != 'SK' or lm.atpySts is null)");
		}
		if ("0".equals(flag)) {
			//结息日
			hql.append(" and ((lm.lastDueDt=? or lm.curDueDt=?) ");
			hql.append(" and ");
			hql
					.append("((lm.loanOdInd='Y' or lm.loanDevaInd='Y') ");
			//modified by fanyl on 2015-10-09 for 核销后贷款结息
			if(SystemInfo.getSystemInfo().getAccountRule().isChrgoSetlInt()) {
				hql.append(" and (lm.loanDebtSts in('NORM','DELQ','CHRGO'))) ");
			} else {
				hql.append(" and (lm.loanDebtSts in('NORM','DELQ'))) ");
			}
			hql.append("or (lm.loanDebtSts='OVER' and lm.curGenOdIntDt=?)");
			hql.append("or lm.bussTyp=? ");
			hql.append(")");
			hql
					.append("and (lm.lastGenOdIntDt<>? or lm.lastGenOdIntDt is null) ");
			params = new Serializable[] { YnFlag.NO.getCodeInDb(),
					LoanState.ACTIVE.getCodeInDb(), buzDate,buzDate,buzDate,
					JobBussType.TLOAN.getCodeInDb(), buzDate };
		} else if ("1".equals(flag)) {
			hql
					.append(" and lm.loanOdInd = ? ");
			hql.append(" and (lm.lastGenProdDt<? or lm.lastGenProdDt is null)");
			//modified by fanyl on 2015-10-09 for 核销后贷款结息
			if(SystemInfo.getSystemInfo().getAccountRule().isChrgoAcc() 
					|| SystemInfo.getSystemInfo().getAccountRule().isChrgoSetlInt()) {
				hql.append(" and lm.loanDebtSts in ('NORM','OVER','DELQ','CHRGO')");
			} else {
				hql.append(" and lm.loanDebtSts in ('NORM','OVER','DELQ')");
			}
			params = new Serializable[] { YnFlag.NO.getCodeInDb(),
					LoanState.ACTIVE.getCodeInDb(), YnFlag.YES.getCodeInDb(), buzDate };
		}
		
		if (StringUtils.hasText(threadCntStr)) {
			hql.append(" and lm.thdCnt in(").append(threadCntStr)
					.append(") ");
		}

		try {
			super.executeUpdateByHql(hql.toString(), params);
		} catch (Exception e) {
			e.printStackTrace();
			throw new YcloansDBException("updatePrcsPageDtInd error!", e);
		}
	}

	/**
	 * 将满足条件的数据操作标识设置为已处理
	 */
	public void updatePrcsPageDtIndByLoanNo(Connection conn, String loanNo) {
		try {
			LmLoan lmLoan = (LmLoan) this.getHibernateTemplate().get(
					LmLoan.class, loanNo);
			lmLoan.setPrcsPageDtInd("Y");
			this.getHibernateTemplate().update(lmLoan);
		} catch (Exception e) {
			throw new YcloansDBException("UpdatePrcsPageDtIndByLoanNo error!",
					e);
		}
	}

	/**
	 * 更新lastGenOdIntDt字段为当前业务时间
	 */
	public void updateLastGenOdIntDtBuLognNo(Connection conn, String loanNo,
			String buzDate) {

		try {
			LmLoan lmLoan = (LmLoan) this.getHibernateTemplate().get(
					LmLoan.class, loanNo);
			lmLoan.setLastGenOdIntDt(buzDate);
			this.getHibernateTemplate().update(lmLoan);
		} catch (Exception e) {
			e.printStackTrace();
			throw new YcloansDBException("updateLastGenOdIntDtBuLognNo error!",
					e);
		}

	}

	/**
	 * 根据当前业务日期和机构号选取需要做形态转移的贷款
	 */
	public Pager findShapeTrans(Connection conn, String curDueDt,
			String branchCode) {
		try {
			StringBuffer sql = new StringBuffer();
			sql.append(" from LmLoan l ");
			sql.append(" where l.loanSts = ? ");
			sql.append(" and l.prcsPageDtInd=?");
			// sql.append(" and l.loanOdInd=?");
			sql.append(" and l.loanDebtSts in(?,?)");
			// sql.append(" and l.loanStpAccInd=?");
			// sql.append(" and l.curDueDt=? and l.bchCde in ( "+branchCode+" )");
			sql
					.append("  and l.loanNo not in (select llst.id.loanNo from LmLnStsTransLog llst where l.loanNo=llst.id.loanNo and  llst.id.txDt=?)");
			sql.append(" order by  l.loanNo");

			Serializable[] params = new Serializable[] {
					LoanState.ACTIVE.getCodeInDb(), YnFlag.NO.getCodeInDb(),
					LoanState.NORMAL.getCodeInDb(),
					LoanState.OVER.getCodeInDb(), curDueDt };
			return new HibernatePager(this, sql, params);
		} catch (RuntimeException e) {
			e.printStackTrace();
			throw new YcloansDBException("find error!", e);
		}
	}

	/**
	 * 查询指定日期做形态转移的数据
	 * 
	 * @param flag
	 *            0:本金逾期形态转移 1:逾期90天转表外形态转移
	 */
	public Pager newFindShapeTrans(Connection conn, String buzDate,
			int overDayCount, String transCde, String flag) {

		try {
			StringBuffer sql = new StringBuffer();
			Serializable[] params;
			sql.append("select l from LmLoan l");
			sql
					.append(" where l.loanSts = ? and l.prcsPageDtInd=? and l.loanDebtSts in(?,?,?) ");
			
			if(ServiceContainer.getSystemParameter().isPayInDay()){//如果采用日间扣款模式 则会根据扣款返回状态来判断改日终任务是否执行
				sql.append(" and (l.atpySts != 'SK' or l.atpySts is null)");
			}
			sql.append(" and (l.loanOdInd='Y' or l.loanStpAccInd='R')");
			//modified by fanyl on 2015-09-10 for 宽限期校验提至sql中
			if ("0".equals(flag)) {
				sql.append(" and ( ( l.loanDebtSts='NORM'  and l.loanGraceTyp='E' ");
				sql.append(" and ((allOver = 'N' and ");
				sql.append(DBSqlUtils.getDateIncDay("l.nextDueDt", "l.loanOdGrace", "+"));
				sql.append(" <=? ) or ( allOver = 'Y' and ");
				sql.append(DBSqlUtils.getDateIncDay("l.lastDueDt", "l.loanOdGrace", "+"));
				sql.append(" <= ? ))) ");
				sql.append(" or (l.loanDebtSts = 'DELQ' and exists ");
				sql.append(" (from LmPmShd ps where ps.id.loanNo = l.loanNo ");
				sql.append(" and ps.prcpState = '10' and ps.psDueDt <= ? ");
				sql.append(" and ps.id.psPerdNo <> 0 and ps.setlInd='N')))");//后续期转逾期
			}
			//modified by fanyl on 2015-11-13 for 利随本清形态转移
			/*sql.append(" and ((l.lastDueDt<=? and  l.loanDebtSts='NORM' ) or");*/
			sql.append(" and (");
			sql.append(" l.loanStpAccInd='R' or ");
			sql.append(" ( l.loanStpAccInd='N'");
			/*sql.append(" and exists (");
			sql.append(" from LmPmShd lm where lm.id.loanNo=l.loanNo ");
			sql.append(" and lm.id.psPerdNo=(");
			sql.append(" select min(pm.id.psPerdNo) from LmPmShd pm where ");
			sql.append("  pm.id.loanNo=l.loanNo");
			sql.append(" and pm.id.psPerdNo>0");
			sql.append(" and pm.setlInd=?");*/
			if ("0".equals(flag)) {
				sql.append(" and (( allOver = 'N' and l.nextDueDt <= ?) or ( allOver = 'Y' and l.lastDueDt <= ?)))) ");
				params = new Serializable[] { LoanState.ACTIVE.getCodeInDb(),
						YnFlag.NO.getCodeInDb(),
						LoanState.NORMAL.getCodeInDb(),
						LoanState.OVER.getCodeInDb(),
						LoanState.DELQ.getCodeInDb(), buzDate,
						buzDate,buzDate,buzDate,buzDate ,buzDate };
			} else {
				/*sql.append(" and to_date(pm.psDueDt,'").append(dateFormat)
						.append("')+ " + String.valueOf(overDayCount));
				sql.append(" <=? ");*/
				sql.append(" and (( allOver = 'N' and ");
				sql.append(DBSqlUtils.getDateIncDay("l.nextDueDt", String.valueOf(overDayCount), "+"));
				//end modify
				sql.append(" <=?) or ( l.allOver = 'Y' and ");
				sql.append(DBSqlUtils.getDateIncDay("l.lastDueDt", String.valueOf(overDayCount), "+"));
				sql.append(" <=?)))) ");
				params = new Serializable[] { LoanState.ACTIVE.getCodeInDb(),
						YnFlag.NO.getCodeInDb(),
						LoanState.NORMAL.getCodeInDb(),
						LoanState.OVER.getCodeInDb(),
						LoanState.DELQ.getCodeInDb(),
						buzDate, buzDate, buzDate };
			}
			/*sql.append("))))");*/
			sql
					.append(" and l.loanNo not in (select llst.id.loanNo from LmLnStsTransLog llst where l.loanNo=llst.id.loanNo and  llst.id.txDt=?");
			if (LoanVarDef.LNPC.equals(transCde)) {// 本金转逾期
				sql.append(" and llst.id.transAmtTyp in ('I','P'))");
			} else {
				sql.append(" and llst.id.transAmtTyp='C')");
			}
			//过滤掉停止自动转移的数据 2015-04-15
			sql.append(" and l.loanNo not in (select lf.id.loanNo from LmLoanSuspFunc lf where lf.id.loanNo = l.loanNo ");
			sql.append(" and lf.id.suspCde = 'STPSTSCHG' and lf.procInd = 'Y')");
			sql.append(" order by  l.loanNo");
			return new HibernatePager(this, sql, params);
		} catch (RuntimeException e) {
			e.printStackTrace();
			throw new YcloansDBException("find error!", e);
		}
	}

	/**
	 * 核心数据移植补转逾期90天时，扣款日前的应计利息到应收计表外
	 */
	public Pager findCTOYLNANTrans(Connection conn, String curDueDt,
			String branchCode) {
		try {
			StringBuffer sql = new StringBuffer();
			sql.append(" from LmLoan l ");
			sql
					.append(" where l.loanSts = ? and l.loanDebtSts=? and l.bussTyp<>? and l.loanOdInd=?");
			sql.append(" and l.prcsPageDtInd=?");
			sql
					.append(
							" and l.loanNo not in(select lps.id.loanNo from LmPmShd lps where lps.id.loanNo=l.loanNo and lps.psDueDt=");
			sql.append(DBSqlUtils.getDateIncDay("l.nextDueDt", "90", "+")).append(") ");
			sql
					.append(" and l.loanNo not in (select llst.id.loanNo from LmLnStsTransLog llst where l.loanNo=llst.id.loanNo and  llst.id.txDt=?)");
			sql.append(" order by l.loanNo");

			Serializable[] params = new Serializable[] {
					LoanState.ACTIVE.getCodeInDb(),
					LoanState.NORMAL.getCodeInDb(),
					JobBussType.TLOAN.getCodeInDb(), YnFlag.YES.getCodeInDb(),
					YnFlag.NO.getCodeInDb(), curDueDt };
			return new HibernatePager(this, sql, params);
		} catch (RuntimeException e) {
			e.printStackTrace();
			throw new YcloansDBException("find error!", e);
		}
	}

	/**
	 * 修改分页标识
	 */
	public int updateCTOYLNANTransPageFlag(Connection conn, String curDueDt,
			String bchCde) {
		// String bankCde = SystemInfo.getSystemInfo().getDefaultBankCde();
		// String subBranchCommaString = SystemInfo.getSystemInfo()
		// .getSubBranchCommaStringIncludeSelf(bankCde, bchCde).toString();
		StringBuffer hql = new StringBuffer();

		hql.append(" update LmLoan l set l.prcsPageDtInd = 'N' ");
		hql
				.append(" where l.loanSts = ? and l.loanDebtSts=? and l.bussTyp<>? and l.loanOdInd=?");
		hql.append(" and l.prcsPageDtInd=?");
		hql.append(
						" and l.loanNo not in(select lps.id.loanNo from LmPmShd lps where lps.id.loanNo=l.loanNo and lps.psDueDt=");
		hql.append(DBSqlUtils.getDateIncDay("l.nextDueDt", "90", "+")).append(")");
		hql
				.append(" and l.loanNo not in (select llst.id.loanNo from LmLnStsTransLog llst where l.loanNo=llst.id.loanNo and  llst.id.txDt=?)");
		try {
			Serializable[] paramValues = new Serializable[] {
					LoanState.ACTIVE.getCodeInDb(),
					LoanState.NORMAL.getCodeInDb(),
					JobBussType.TLOAN.getCodeInDb(), YnFlag.YES.getCodeInDb(),
					YnFlag.NO.getCodeInDb(), curDueDt };
			return executeUpdateByHql(hql.toString(), paramValues);
		} catch (RuntimeException e) {
			e.printStackTrace();
			throw new YcloansDBException("find error!", e);
		}
	}

	/**
	 * 查询已逾期贷款当前正在结罚息的借据信息
	 */
	@SuppressWarnings("unchecked")
	public List<LmLoan> findNoGenOdIntDt(String buzDt) {
		StringBuilder sql = new StringBuilder();
		sql.append(" from LmLoan l");
		sql.append(" where l.loanSts='ACTV'");
		sql.append(" and (l.curGenOdIntDt is null or l.curGenOdIntDt='')");
		//modified by fanyl on 2016-04-08 for 核销后结息
		if(SystemInfo.getSystemInfo().getAccountRule().isChrgoSetlInt()) {
			sql.append(" and (l.loanDebtSts='OVER' or (l.loanDebtSts='CHRGO' and l.lastDueDt<='");
			sql.append(buzDt);
			sql.append("'))");
		} else {
			sql.append(" and l.loanDebtSts='OVER'");
		}
		if(ServiceContainer.getSystemParameter().isPayInDay()){//如果采用日间扣款模式 则会根据扣款返回状态来判断改日终任务是否执行
			sql.append(" and (l.atpySts != 'SK' or l.atpySts is null)");
		}
		return this.getHibernateTemplate().find(sql.toString());
	}

	/**
	 * 更新借据表的当前结罚息日期
	 */
	public void saveGenOdIntDt(String buzDate, PaymentFreq freq) {
		int freqFreq = 1;
		if (PaymentFreq.QUATER.equals(freq)) {
			freqFreq = 3;
		}
		if (PaymentFreq.YEAR.equals(freq)) {
			freqFreq = 12;
		}
		if (PaymentFreq.HALFYEAR.equals(freq)) {
			freqFreq = 6;
		}
		StringBuilder sql = new StringBuilder();
		if(!DBConst.MYSQL.equals(SystemInfo.getSystemInfo().getSystemParameters().getDbTyp())){
			sql.append(" update LmLoan l");
			sql.append(" set l.curGenOdIntDt=");
			if (freq.equals(PaymentFreq.WEEK)) {
				sql.append(DBSqlUtils.getDateIncDay("l.curGenOdIntDt", "7*l.paymFreqFreq", "+"));
			} else {
				sql.append(DBSqlUtils.getDateIncMonth("curGenOdIntDt", "("+freqFreq+"*l.paymFreqFreq)", "+"));
			}
			sql.append(" ,l.lastChgDt=? ");
			sql.append(" where ");
			sql.append(" l.loanSts='ACTV'");
			sql.append(" and l.curGenOdIntDt is not null");
			//modified by fanyl on 2016-04-08 for 核销后结息
			if(SystemInfo.getSystemInfo().getAccountRule().isChrgoSetlInt()) {
				sql.append(" and (l.loanDebtSts='OVER' or (l.loanDebtSts='CHRGO' and l.lastDueDt<='");
				sql.append(buzDate);
				sql.append("')) ");
			} else {
				sql.append(" and l.loanDebtSts='OVER' ");
			}
			sql.append(" and l.curGenOdIntDt <?");
			sql.append(" and l.paymFreqUnit=?");
			sql.append(" and l.loanPaymTyp<>'05'");
			executeUpdateByHql(sql.toString(), new String[] { buzDate,buzDate,
				freq.getCodeInDb() });
		}else{
			sql.append(" UPDATE LM_LOAN L");
			sql.append(" SET L.CUR_GEN_OD_INT_DT=");
			if (freq.equals(PaymentFreq.WEEK)) {
				sql.append(DBSqlUtils.getDateIncDay("L.CUR_GEN_OD_INT_DT", "7*L.PAYM_FREQ_FREQ", "+"));
			} else {
				sql.append(DBSqlUtils.getDateIncMonth("L.CUR_GEN_OD_INT_DT", freqFreq+"*L.PAYM_FREQ_FREQ", "+"));
			}
			sql.append(" ,L.last_Chg_Dt=? ");
			sql.append(" WHERE ");
			sql.append(" L.LOAN_STS='ACTV'");
			sql.append(" AND L.CUR_GEN_OD_INT_DT IS NOT NULL");
			//modified by fanyl on 2016-04-08 for 核销后结息
			if(SystemInfo.getSystemInfo().getAccountRule().isChrgoSetlInt()) {
				sql.append(" and (L.LOAN_DEBT_STS='OVER' or (L.LOAN_DEBT_STS='CHRGO' and L.LAST_DUE_DT<='");
				sql.append(buzDate);
				sql.append("')) ");
			} else {
				sql.append(" and L.LOAN_DEBT_STS='OVER'");
			}
			sql.append(" AND L.CUR_GEN_OD_INT_DT <?");
			sql.append(" AND L.PAYM_FREQ_UNIT=?");
			sql.append(" AND L.LOAN_PAYM_TYP<>'05'");
			executeUpdateBySql(sql.toString(), new String[] { buzDate,buzDate,
				freq.getCodeInDb()});
		}
	}

	/**
	 * 更新借据表的当前结罚息日期
	 */
	public void saveGenOdIntDt(Connection conn, String buzDate) {
		StringBuilder sql = new StringBuilder();
		sql.append(" update Lm_Loan ");
		sql.append(" set cur_Gen_Od_Int_Dt=");
		sql.append(DBSqlUtils.getDateIncMonth("cur_Gen_Od_Int_Dt", "1", "+"));
		sql.append(" ,last_Chg_Dt=? ");
		sql.append(" where ");
		sql.append(" loan_Sts=?");
		sql.append(" and cur_Gen_Od_Int_Dt is not null");
		//modified by fanyl on 2016-04-08 for 核销后结息
		if(SystemInfo.getSystemInfo().getAccountRule().isChrgoSetlInt()) {
			sql.append(" and (loan_Debt_Sts='OVER' or (loan_Debt_Sts='CHRGO' and last_Due_Dt<='");
			sql.append(buzDate);
			sql.append("')) ");
		} else {
			sql.append(" and loan_Debt_Sts='OVER'");
		}
		sql.append(" and cur_Gen_Od_Int_Dt <?");
		sql.append(" and loan_Paym_Typ ='05'");
		super.executeUpdateBySql(sql.toString(), new String[] {
			buzDate,	
			LoanState.ACTIVE.getCodeInDb(),
				buzDate });
	}

	/**
	 * 根据贷款状态、当前扣款日及机构信息更新借据的分页标志为N
	 */
	public int updateChangeOdPageFlag(Connection conn, String buzDate,
			String bchCdes) {
		// String bankCde = SystemInfo.getSystemInfo().getDefaultBankCde();
		// String subBranchCommaString = SystemInfo.getSystemInfo()
		// .getSubBranchCommaStringIncludeSelf(bankCde, bchCde).toString();
		StringBuilder hql = new StringBuilder(
				"update LmLoan lm set lm.prcsPageDtInd = 'N' ");
		hql.append(" where lm.loanSts = ? and lm.prcsPageDtInd <> 'N' ");
		hql.append(" and lm.curDueDt=?");//
		hql.append(" and lm.bchCde in ('").append(bchCdes).append("')");
		hql.append(" order by  lm.loanNo asc");

		try {

			Serializable[] paramValues = new Serializable[] {
					LoanState.ACTIVE.getCodeInDb(), buzDate };
			return executeUpdateByHql(hql.toString(), paramValues);
		} catch (RuntimeException e) {
			e.printStackTrace();
			throw new YcloansDBException("find error!", e);
		}
	}

	/**
	 * 更新页标志，用于形态转移
	 */
	public int updateShapeTransPageFlag(Connection conn, String curDueDt,
			String bchCde) {
		// String bankCde = SystemInfo.getSystemInfo().getDefaultBankCde();
		// String subBranchCommaString = SystemInfo.getSystemInfo()
		// .getSubBranchCommaStringIncludeSelf(bankCde, bchCde).toString();
		StringBuffer hql = new StringBuffer();
		
		hql.append(" update LmLoan l set l.prcsPageDtInd = 'N' ");
		hql.append(" where l.loanSts = ? and l.prcsPageDtInd <> 'N' and l.loanDebtSts in(?,?,?)");
		hql.append(" and (l.loanOdInd='Y' or l.loanStpAccInd='R')");
		hql.append(" and ((l.lastDueDt<=? ) or");
		hql.append(" l.loanStpAccInd='R' or ");
		hql.append(" (l.lastDueDt>? ");
		hql.append(" and l.loanStpAccInd=?");
		hql.append(" and l.nextDueDt <=?))");
		if(ServiceContainer.getSystemParameter().isPayInDay()){//如果采用日间扣款模式 则会根据扣款返回状态来判断改日终任务是否执行
			hql.append(" and (l.atpySts != 'SK' or l.atpySts is null)");
		}
		try {
			Serializable[] paramValues = new Serializable[] {
					LoanState.ACTIVE.getCodeInDb(),
					LoanState.NORMAL.getCodeInDb(),
					LoanState.OVER.getCodeInDb(), LoanState.DELQ.getCodeInDb(),
					curDueDt, curDueDt, LoanStpAccInd.NO.getCodeInDb(),
					curDueDt };
			return executeUpdateByHql(hql.toString(), paramValues);
		} catch (RuntimeException e) {
			e.printStackTrace();
			throw new YcloansDBException("find error!", e);
		}
	}

	/**
	 * 更新借据信息
	 */
	public LmLoan merge(Connection conn, LmLoan detachedInstance) {
		// log.debug("merging LmLoan instance");
		try {
			//借据表修改时统一修改最后修改日期字段
			detachedInstance.setLastChgDt(SystemInfo.getSystemInfo().getBuzDate());
			LmLoan result = (LmLoan) getHibernateTemplate().merge(
					detachedInstance);
			// log.debug("merge successful");
			return result;
		} catch (RuntimeException re) {
			throw new YcloansDBException("merge failed", re);
		}
	}

	/**
	 * 利息计提，正常处理修改借据主表（上次计提日期）
	 * 
	 * @param ll
	 * @param operationDay
	 *            业务日期
	 */
	public void update(Connection conn, String loanNo, String operationDay) {
		// log.debug("update LmLoan for accrual pick-up instance");
		try {
			Session session = super.getCurrentHibernateSession();
			String sql = "update lm_loan set LAST_INT_ACC_DT='" + operationDay
					+ "',LAST_CHG_DT='" + operationDay +"',prcs_page_dt_ind='N' where loan_no=" + loanNo;

			session.createSQLQuery(sql).executeUpdate();
		} catch (RuntimeException re) {
			throw new YcloansDBException(
					"update LmLoan for accrual pick-up failed", re);
		}
	}

	/**
	 * 利息计提，异常处理修改借据主表（上次计提日期）
	 * 
	 * @param ll
	 * @param operationDay
	 *            业务日期
	 */
	public void updateAccrualFaild(Connection conn, String loanNo) {
		// log.debug("update LmLoan for accrual pick-up instance");
		try {
			Session session = super.getCurrentHibernateSession();
			String sql = "update lm_loan set prcs_page_dt_ind='Y' where loan_no='"
					+ loanNo + "'";
			session.createSQLQuery(sql).executeUpdate();
		} catch (RuntimeException re) {
			throw new YcloansDBException(
					"update LmLoan for accrual pick-up failed", re);
		}
	}

	/**
	 * 根据贷款财务状态及逾期标志修改借据信息
	 */
	public int update(Connection conn, String loanNo, String loanDebtSts,
			String loanOdInd) {
		StringBuffer sql = new StringBuffer(30);
		sql.append("update LmLoan set loanDebtSts=?");
		sql.append(" ,loanOdInd=?,lastChgDt=? where loanNo=?");
		Serializable[] paramValues = new Serializable[4];
		paramValues[0] = loanDebtSts;
		paramValues[1] = loanOdInd;
		paramValues[2] = SystemInfo.getSystemInfo().getBuzDate();
		paramValues[3] = loanNo;
		return executeUpdateByHql(sql.toString(), paramValues);
	}

	/**
	 * 更新借据状态及核销日期
	 */
	public int updateDebtSts(Connection conn, String loanNo,
			String loanDebtSts, String date) {
		StringBuffer sql = new StringBuffer(30);
		sql.append("update LmLoan set loanDebtSts=?");
		sql.append(" ,lastChgDt=? where loanNo=?");
		Serializable[] paramValues = new Serializable[3];
		paramValues[0] = loanDebtSts;
		paramValues[1] = date;
		paramValues[2] = loanNo;
		return executeUpdateByHql(sql.toString(), paramValues);
	}

	/**
	 * 根据贷款状态且分页标志为N查询借据信息
	 * 
	 * @param conn
	 * @param buzDate
	 * @param branchCode
	 * @param bankCde
	 * @return
	 */
	public Pager findRepayDataByState(Connection conn, String buzDate,
			String branchCode, String bankCde) {
		StringBuffer subBches = SystemInfo.getSystemInfo()
				.getSubBranchCommaStringIncludeSelf(bankCde, branchCode);
		StringBuffer sql = new StringBuffer(30);
		sql.append("select l,lc");
		sql.append(" from LmLoan l,LmLoanCont lc  ");
		sql.append(" where l.loanNo=lc.loanNo");
		sql.append(" and l.loanSts=?");
		sql.append(" and lc.loanRepayMthd=?");
		sql.append(" and l.nextDueDt<=?");// 下次应扣日期
		sql.append(" and l.bchCde in(").append(subBches).append(")");
		sql.append(" and l.prcsPageDtInd=?");
		Serializable[] params = new Serializable[4];
		params[0] = LoanState.ACTIVE.getCodeInDb();
		params[1] = buzDate;
		params[2] = branchCode;
		params[3] = YnFlag.NO.getCodeInDb();
		return new HibernatePager(this, sql, params);
	}

	/**
	 * 更新待处理的借据信息的分页标志为N
	 */
	public int updatePageFlagRepayDataByState(Connection conn, String buzDate,
			String branchCode, String bankCde, String night) {
		StringBuffer subBches = SystemInfo.getSystemInfo()
				.getSubBranchCommaStringIncludeSelf(bankCde, branchCode);
		StringBuffer sql = new StringBuffer(30);
		sql.append("update LmLoan l set  l.prcsPageDtInd='"
				+ YnFlag.NO.getCodeInDb() + "'");
		sql.append(" where l.loanSts=? and l.prcsPageDtInd <> 'N'");
		// sql.append(" and night=? ");
		sql.append(" and l.bchCde in(").append(subBches).append(")");
		sql.append(" and ((l.nextDueDt<=? or");// 涓嬫搴旀墸鏃ユ湡
		sql.append(" (");
		sql.append(" loanDevaInd='Y'");
		sql.append(" and loanDebtSts='NORM')");
		sql.append(" )");
		sql.append(" or l.nextDueDt is null)");
		sql.append(" and exists (  from  LmLoanCont lc  where ");
		sql.append(" l.loanNo=lc.loanContNo  )");
		Serializable[] params = new Serializable[2];
		params[0] = LoanState.ACTIVE.getCodeInDb();
		// params[1] = LoanRepayMethod.AUTOPAY.getCodeInDb();
		// params[1] = night;
		params[1] = buzDate;
		// params[2] = branchCode;
		return executeUpdateByHql(sql.toString(), params);
	}
	/**
	 * 更新待处理的借据信息的分页标志为N
	 */
	public int updatePageFlagRepayDataByStateKr(Connection conn, String buzDate,
			String branchCode, String bankCde, String night) {
		StringBuffer subBches = SystemInfo.getSystemInfo()
				.getSubBranchCommaStringIncludeSelf(bankCde, branchCode);
		StringBuffer sql = new StringBuffer(30);
		sql.append("update LmLoan l set  l.prcsPageDtInd='"
				+ YnFlag.NO.getCodeInDb() + "'");
		sql.append(" where l.loanSts=? and l.prcsPageDtInd <> 'N'");
		// sql.append(" and night=? ");
		sql.append(" and l.bchCde in(").append(subBches).append(")");
		sql.append(" and ((l.nextDueDt<=? or");// 涓嬫搴旀墸鏃ユ湡
		sql.append(" (");
		sql.append(" loanDevaInd='Y'");
		sql.append(" and loanDebtSts='NORM')");
		sql.append(" )");
		sql.append(" or l.nextDueDt is null)");
		sql.append(" and exists (  from  LmLoanCont lc  where ");
		sql.append(" l.loanNo=lc.loanContNo  )");
		Serializable[] params = new Serializable[2];
		params[0] = LoanState.ACTIVE.getCodeInDb();
		// params[1] = LoanRepayMethod.AUTOPAY.getCodeInDb();
		// params[1] = night;
		params[1] = buzDate;
		// params[2] = branchCode;
		return executeUpdateByHql(sql.toString(), params);
	}
	
	/**
	 * 更新待处理的借据信息的分页标志为N
	 */
	public int updatePageFlagRepayDataByStateThd(Connection conn, String buzDate,
			String branchCode, String bankCde, String night,String thdCntStr) {
		StringBuffer subBches = SystemInfo.getSystemInfo()
				.getSubBranchCommaStringIncludeSelf(bankCde, branchCode);
		StringBuffer sql = new StringBuffer(30);
		sql.append("update LmLoan l set  l.prcsPageDtInd='"
				+ YnFlag.NO.getCodeInDb() + "'");
		sql.append(" where l.loanSts=? and l.prcsPageDtInd <> 'N'");
		// sql.append(" and night=? ");
		sql.append(" and l.bchCde in(").append(subBches).append(")");
		sql.append(" and ((l.nextDueDt<=? or");// 涓嬫搴旀墸鏃ユ湡
		sql.append(" (");
		sql.append(" loanDevaInd='Y'");
		sql.append(" and loanDebtSts='NORM')");
		sql.append(" )");
		sql.append(" or l.nextDueDt is null)");
		sql.append(" and exists (  from  LmLoanCont lc  where ");
		sql.append(" l.loanNo=lc.loanNo  )");
		if(!StringUtil.isNullEmpty(thdCntStr)){
			sql.append(" and l.thdCnt IN (").append(thdCntStr).append(")") ;
		}
		Serializable[] params = new Serializable[2];
		params[0] = LoanState.ACTIVE.getCodeInDb();
		// params[1] = LoanRepayMethod.AUTOPAY.getCodeInDb();
		// params[1] = night;
		params[1] = buzDate;
		// params[2] = branchCode;
		return executeUpdateByHql(sql.toString(), params);
	}

	/**
	 * 利息计提，修改借据主表（上次计提日期）
	 */
	public void updateAccrualSuc(Connection conn, String loanNo,
			String operationDay) {
		// log.debug("update LmLoan for accrual success instance");
		try {
			Session session = super.getCurrentHibernateSession();
			String sql = "update lm_loan set prcs_page_dt_ind='Y',last_int_acc_dt='"
					+ operationDay + "',LAST_CHG_DT='" + operationDay + "' where loan_no='" + loanNo + "'";
			session.createSQLQuery(sql).executeUpdate();
		} catch (RuntimeException re) {
			throw new YcloansDBException(
					"update LmLoan for accrual success failed", re);
		}
	}

	/**
	 * 查找需扣款的借据
	 */
	public Pager searchRepayDataByState(Connection conn, String buzDate,
			String branchCode, String bankCde, long jobSeq) {
		StringBuffer subBches = SystemInfo.getSystemInfo()
				.getSubBranchCommaStringIncludeSelf(bankCde, branchCode);
		StringBuffer sql = new StringBuffer(30);
		sql.append("select l");
		sql.append(" from LmLoan l ");
		sql.append(" where ");
		//7*24小时还款数据，不再进行批扣 by clq 2017/01/17 start
		sql.append(" l.loanNo not in ( ");
		sql.append(" select i.loanNo from IntfLoanTxQue i ");
		sql.append(" where i.loanNo=l.loanNo and i.prcsSts='INIT' ");
		sql.append(" and i.txTyp='").append(LoanVarDef.LNC4).append("' and i.txDt=?) ");
		//7*24小时还款数据，不再进行批扣 by clq 2017/01/17 end
		sql.append(" and ((l.loanDevaInd='Y' ");
		sql.append(" and l.loanDebtSts='NORM'");
		//sql.append(" and (l.loanDevaOrd is null or l.loanDevaOrd<>'I')");
		sql.append(" ) or ");
		sql.append(" (l.nextDueDt<=? or l.nextDueDt is null)");//
		sql.append(")");
		// /不处理已经冻结的账号
		sql.append(" and l.loanNo not in (");
		sql.append(" select  la.loanNo from LmAtpyDetl la ");
		sql.append(" where la.atpyValDt=?  ");
		sql.append(" and la.atpySts  in('").append(
				ATPYState.SU.getCodeInDb()).append("','").append(
				ATPYState.FREEZE.getCodeInDb()).append("'))");
		sql.append(" and (l.atpySts != 'SK' or l.atpySts is null)");
		sql.append(" and l.loanNo not in (");
		sql.append("  select  lms.loanNo from LmSetlmtT lms where lms.paymInd='N' and lms.sendSts in ('SU','SP','FZ') and lms.setlValDt='"+buzDate+"' ) ");
		sql.append(" and l.bchCde in( ").append(subBches).append(" ) ");
		sql.append(" and l.prcsPageDtInd=? ");
		sql.append(" and l.loanDebtSts in (? , ?,?) ");
		sql.append(" and l.loanSts=? ");
		sql.append(" order by l.loanNo ");
		Serializable[] params = new Serializable[] {
				buzDate,
				buzDate, 
				buzDate, 
				YnFlag.NO.getCodeInDb(),
				LoanState.NORMAL.getCodeInDb(),LoanState.OVER.getCodeInDb(), LoanState.DELQ.getCodeInDb(),
				LoanState.ACTIVE.getCodeInDb()};
		return new HibernatePager(this, sql, params);
	}
	/**
	 * 查找需扣款的借据
	 */
	public Pager searchRepayDataByStateThd(Connection conn, String buzDate,
			String branchCode, String bankCde, long jobSeq,String thdCntStr) {
		StringBuffer subBches = SystemInfo.getSystemInfo()
				.getSubBranchCommaStringIncludeSelf(bankCde, branchCode);
		StringBuffer sql = new StringBuffer(30);
		sql.append("select l");
		sql.append(" from LmLoan l ");
		sql.append(" where ");
		//7*24小时还款数据，不再进行批扣 by clq 2017/01/17 start
		sql.append(" l.loanNo not in (");
		sql.append(" select i.loanNo from IntfLoanTxQue i ");
		sql.append(" where i.loanNo=l.loanNo and i.prcsSts='INIT' ");
		sql.append(" and i.txTyp='").append(LoanVarDef.LNC4).append("' and i.txDt=?) ");
		//7*24小时还款数据，不再进行批扣 by clq 2017/01/17 end
		sql.append(" and ((l.loanDevaInd='Y' ");
		sql.append(" and l.loanDebtSts='NORM'");
		//sql.append(" and (l.loanDevaOrd is null or l.loanDevaOrd<>'I')");
		sql.append(" ) or ");
		sql.append(" (l.nextDueDt<=? or l.nextDueDt is null)");//
		sql.append(")");
		// /不处理已经冻结的账号
		sql.append(" and l.loanNo not in (");
		sql.append(" select  la.loanNo from LmAtpyDetl la ");
		sql.append(" where la.atpyValDt=? ");
		sql.append(" and la.atpySts  in('").append(
				ATPYState.SU.getCodeInDb()).append("','").append(
				ATPYState.FREEZE.getCodeInDb()).append("'))");
		//sql.append(" and night=?");
		sql.append(" and l.bchCde in( ").append(subBches).append(" ) ");
		sql.append(" and l.prcsPageDtInd=? ");
		sql.append(" and l.loanDebtSts in (?,?,?) ");
		sql.append(" and l.loanSts=? ");
		sql.append(" and l.thdCnt IN (").append(thdCntStr).append(")") ;
		
		sql.append(" order by l.loanNo ");
		Serializable[] params = new Serializable[] {
				buzDate,
				buzDate, 
				buzDate, 
				YnFlag.NO.getCodeInDb(),
				LoanState.NORMAL.getCodeInDb(),LoanState.OVER.getCodeInDb(), LoanState.DELQ.getCodeInDb(),
				LoanState.ACTIVE.getCodeInDb()};
		return new HibernatePager(this, sql, params);
	}
	/**
	 * 库融改造，查询欠本信息
	 * @return
	 */
	public Pager searchRepayDataKrByStateThd(Connection conn, String buzDate,
			String branchCode, String bankCde, long jobSeq,String thdCntStr){
		StringBuffer sql = new StringBuffer(30);
		sql.append("select a ");
		sql.append(" from LmLoan a ");
		sql.append(" where a.nextDueDt >? ");
		sql.append(" and exists (select b.id.loanNo from LmProcShd b ");
		sql.append(" where b.id.loanNo = a.loanNo and b.psProcAmt > b.setlProcAmt and b.psDueDt <=? ) ");
		sql.append(" and a.loanNo not in ( ");
		sql.append(" select c.loanNo from LmSetlmtT c ");
		sql.append(" where c.setlValDt =? and c.isAp = 'Y' and c.genGlInd = ?) ");
		sql.append(" and a.loanDebtSts = 'NORM' ");
		sql.append(" and a.loanSts = 'ACTV' ");
		sql.append(" and a.thdCnt in ("+thdCntStr+") ");
		sql.append(" and a.prcsPageDtInd =? ");
		sql.append(" order by a.loanNo ");
		Serializable[] params = new Serializable[] {
				buzDate,
				buzDate, 
				buzDate,
				GenGlInd.UN_PROCESSED.getCodeInDb(),
				YnFlag.NO.getCodeInDb()};
		return new HibernatePager(this, sql, params);
	}
	public Pager searchAdjInsData(Connection conn, BatchJobContext bjc) {

		StringBuffer sql = new StringBuffer();
		sql.append("SELECT L,LC");
		sql.append(" FROM LmLoan L,LmLoanCont LC  ");
		sql.append(" WHERE L.prcsPageDtInd='N' ");
		sql.append(" AND L.loanSts = 'ACTV' ");
		sql.append(" AND L.loanNo = LC.loanNo ");
		sql.append(" AND LC.loanRateMode = 'RV' ");
		sql.append(" AND ( ");
		sql.append(" (LC.nextRepcOpt ='NNR' AND LC.nextRepcDt = ? )");
		sql.append(" OR (LC.nextRepcOpt = 'DDA' AND LC.nextRepcDt = ? )");
		sql.append(" OR (LC.nextRepcOpt = 'IMM' AND EXISTS (SELECT 'X'  FROM SIntRat WHERE id.effDt=? AND id.rateTyp= LC.loanRateTyp) ) ");
		sql.append(" OR (LC.nextRepcOpt = 'FIX' AND LC.nextRepcDt = ? ) ");
		sql.append(" OR (LC.nextRepcOpt = 'NYF' AND LC.nextRepcDt = ? )");
		// modified by fanyl on 2015-03-03 for 新增利率调整选项NQF: 下一季度1日调整NMF: 下月1日调整
		sql.append(" OR (LC.nextRepcOpt = 'NQF' AND LC.nextRepcDt = ? )");
		sql.append(" OR (LC.nextRepcOpt = 'NMF' AND LC.nextRepcDt = ? )");
		sql.append(" )");

		if (StringUtils.hasText(bjc.getThreadCntStr())) {
			sql.append(" AND L.thdCnt IN (  ");
			sql.append(bjc.getThreadCntStr()).append(")");
		}

		sql.append(" ORDER BY L.loanNo ");

		Serializable[] params = { bjc.getBuzDate(), bjc.getBuzDate(),
				bjc.getBuzDate(), bjc.getBuzDate(), bjc.getBuzDate(),
				bjc.getBuzDate(),bjc.getBuzDate() };

		return new HibernatePager(this, sql, params);
	}
	
	/**
	 * 查询借据摊余成本积数相关信息
	 */
	public Pager searchForLnCostProd(Connection conn, String buzDate,
			String bankCde, String bchCde) {
		StringBuffer subBches = SystemInfo.getSystemInfo()
				.getSubBranchCommaStringIncludeSelf(bankCde, bchCde);
		StringBuffer sql = new StringBuffer(30);
		sql.append(" from LmLoan l");
		sql.append(" where l.loanSts=?");
		sql.append(" and l.loanDevaInd='Y'");
		sql.append(" and l.bchCde in(").append(subBches).append(")");
		sql.append(" and l.prcsPageDtInd=?");
		// 新增加的条件,不再已完成的状态
		// /不处理已经冻结的账号
		return new HibernatePager(this, sql, new Serializable[] {
				LoanState.ACTIVE.getCodeInDb(), YnFlag.NO.getCodeInDb() });
	}

	/**
	 * 更新借据摊余成本积数相关信息
	 */
	public int updatePageFlagLnCostProd(Connection conn, String buzDate,
			String bankCde, String bchCde) {
		StringBuffer subBches = SystemInfo.getSystemInfo()
				.getSubBranchCommaStringIncludeSelf(bankCde, bchCde);
		StringBuffer sql = new StringBuffer(30);
		sql.append("update LmLoan l set  l.prcsPageDtInd=?");
		sql.append(" where l.loanSts=?");
		sql.append(" and l.loanDevaInd='Y'");
		sql.append(" and l.bchCde in(").append(subBches).append(")");
		return executeUpdateByHql(sql.toString(), new Serializable[] {
				YnFlag.NO.getCodeInDb(), LoanState.ACTIVE.getCodeInDb() });
	}

	/*
	 * 根据机构编号查询借据分页信息
	 * 
	 * @see
	 * com.yuchengtech.ycloans.db.dao.LmLoanDAO#findLoanByBchcde(java.sql.Connection
	 * , java.lang.String, com.yuchengtech.ycloans.common.enumeration.YnFlag)
	 */
	public Pager findLoanByBchcde(Connection conn, String subBranchCommaString,
			YnFlag loanDevInd) {
		StringBuffer sqlbuff = new StringBuffer(30);
		sqlbuff.append(" From LmLoan lm where lm.loanDevaInd = ?  ");
		sqlbuff.append(" and lm.loanSts = ? and lm.bchCde in ("
				+ subBranchCommaString + ")");

		Serializable[] params = new Serializable[2];
		params[0] = loanDevInd.getCodeInDb();
		params[1] = LoanState.ACTIVE.getCodeInDb();
		try {
			return new HibernatePager(this, sqlbuff, params);
		} catch (Exception e) {
			e.printStackTrace();
			throw new YcloansDBException("findLoanInfoByBchcde error!", e);
		}
	}

	/*
	 * 根据机构编号查询借据分页信息
	 * 
	 * @see
	 * com.yuchengtech.ycloans.db.dao.LmLoanDAO#findLoanByBchcde(java.sql.Connection
	 * , java.lang.String, com.yuchengtech.ycloans.common.enumeration.YnFlag)
	 */
	public Pager findLoanByBchcde(Connection conn, String bchCde,
			YnFlag loanDevInd, YnFlag loanOdInd, String buzDate,String threadCntStr) {
		StringBuffer sqlbuff = new StringBuffer(30);
		sqlbuff.append("select lm From LmLoan lm where  ");
		sqlbuff.append("lm.prcsPageDtInd='N' and lm.loanOdInd=? ");
		sqlbuff.append(" and lm.loanSts = ? ");
//		sqlbuff.append(" and lm.bussTyp<>'TLOAN'");
		//宽限期
		sqlbuff.append("and (");
		sqlbuff.append(DBSqlUtils.getDateIncDay("lm.nextDueDt", "lm.loanOdGrace", "+"));
		sqlbuff.append(" <= ?) ");
//		sqlbuff.append(" or (lm.loanGraceTyp='P' and lm.nextDueDt <= ?)) ");
		if (!SystemInfo.getSystemInfo().getAccountRule().isDevLoanAcc()) {
			sqlbuff.append("  and lm.loanDevaInd = 'N'");
		}
		if (!SystemInfo.getSystemInfo().getSystemParameters().getAccountRule()
				.isSuspAcc()) {
			sqlbuff.append(" and lm.loanStpAccInd = 'N' ");// 是否表外
		}
		sqlbuff.append(" and (lm.lastOdIntAccDt is null");
		sqlbuff.append(" or lm.lastOdIntAccDt<?)");
		// sqlbuff.append(" and lm.loanOdIntRate>0"); haixia会出现利率调整后免息的情况 20150605
		sqlbuff.append(" and exists (");
		sqlbuff.append(" from LmPmShd where id.loanNo=lm.loanNo");
		sqlbuff.append(" and (psGenProdDt=? or psGenProdDt is null)");
		sqlbuff.append(" ) ");
		//modified by fanyl on 2015-10-09 for 核销后贷款计提
		Serializable[] params = null;
		if(SystemInfo.getSystemInfo().getAccountRule().isChrgoAcc()) {
			sqlbuff.append(" and lm.loanDebtSts in (?,?,?,?) ");
			params = new Serializable[9];
			params[0] = loanOdInd.getCodeInDb();
			params[1] = LoanState.ACTIVE.getCodeInDb();
			params[2] = buzDate;
			params[3] = buzDate;
			params[4] = buzDate;
//			params[5] = buzDate;
			params[5] = LoanState.OVER.getCodeInDb();
			params[6] = LoanState.DELQ.getCodeInDb();
			params[7] = LoanState.NORMAL.getCodeInDb();
			params[8] = LoanState.OFFED.getCodeInDb();

		} else {
			sqlbuff.append(" and lm.loanDebtSts in (?,?,?) ");
			params = new Serializable[8];
			params[0] = loanOdInd.getCodeInDb();
			params[1] = LoanState.ACTIVE.getCodeInDb();
			params[2] = buzDate;
			params[3] = buzDate;
			params[4] = buzDate;
			params[5] = LoanState.OVER.getCodeInDb();
			params[6] = LoanState.DELQ.getCodeInDb();
			params[7] = LoanState.NORMAL.getCodeInDb();
		}
		if(ServiceContainer.getSystemParameter().isPayInDay()){//如果采用日间扣款模式 则会根据扣款返回状态来判断改日终任务是否执行
			sqlbuff.append(" and (lm.atpySts != 'SK' or lm.atpySts is null)");
		}
		if (StringUtils.hasText(threadCntStr)) {
			sqlbuff.append(" and lm.thdCnt in(").append(threadCntStr)
					.append(") ");
		}
		
		try {
			return new HibernatePager(this, sqlbuff, params);
			// return
			// this.getHibernateTemplate().find(sqlbuff.toString(),params);
		} catch (Exception e) {
			e.printStackTrace();
			throw new YcloansDBException("findLoanInfoByBchcde error!", e);
		}
	}

	/**
	 * 获得贷款减值计提(LNCP)操作对象
	 */
	public Pager getLoanDevCalculate(Connection conn, String buzDate) {
		try {
			StringBuffer queryString = new StringBuffer();

			queryString.append("from LmLoan ll");
			queryString.append(" where ");
			queryString.append("ll.loanSts = 'ACTV' and ");
			queryString.append("ll.loanDevaInd = 'Y' ");
			//queryString.append(" and ll.lastDeviIntAccDt <= ? ");
			if(ServiceContainer.getSystemParameter().isPayInDay()){//如果采用日间扣款模式 则会根据扣款返回状态来判断改日终任务是否执行
				queryString.append(" and (ll.atpySts != 'SK' or ll.atpySts is null)");
			}
			/*Serializable[] params = new Serializable[1];
			params[0] = buzDate;*/

			return new HibernatePager(this, queryString.toString(),
					new Serializable[] { });
		} catch (RuntimeException re) {
			re.printStackTrace();
			throw new YcloansDBException("getLoanDevCalculate error!", re);
		}
	}

	/**
	 * 查询减值贷款且贷款状态为正常和逾期的借据信息
	 */
	public Pager findLoanDev(Connection conn, String buzDate) {
		try {
			StringBuffer queryString = new StringBuffer();

			queryString
					.append("from LmLoan ll where ll.loanSts='ACTV' ");
			queryString
					.append("and ll.loanDebtSts in('NORM','OVER') and ll.loanDevaInd='Y' and ll.lastSetlDt=?");
			Serializable[] params = new Serializable[1];
			params[0] = buzDate;

			return new HibernatePager(this, queryString.toString(), params);
		} catch (RuntimeException re) {
			re.printStackTrace();
			throw new YcloansDBException("findLoanDev error!", re);
		}
	}

	/**
	 * 贷款减值确定，更新LoanDevaInd
	 */
	public void updateDevaInd(Connection conn, String deviInd, String buzDate,
			String loanNo) {
		try {
			// String hql =
			// "update LmLoan ll set ll.loanDevaInd = ?, ll.loanStpAccInd = ?, ll.loanStpAccDt = ? where ll.loanNo = ?";
			String hql = "update LmLoan ll set ll.loanDevaInd = ?,ll.lastChgDt=?   where ll.loanNo = ?";
			Serializable[] params = new Serializable[3];
			params[0] = deviInd;
			// params[1] = deviInd;
			// if ("N".equals(deviInd)) {
			// params[2] = null;
			// } else if ("Y".equals(deviInd)) {
			// params[2] = buzDate;
			// }
			// params[3] = loanNo;
			params[1] = buzDate;
			params[2] = loanNo;
			executeUpdateByHql(hql.toString(), params);
		} catch (RuntimeException re) {
			re.printStackTrace();
			throw new YcloansDBException("updateDevaInd error");
		}
	}

	/**
	 * 根据扣款日和借据状态更新借据的当前应扣款日
	 */
	public void updateLmLoanByPsDueDtandLoanSts(Connection conn, String psDueDt) {
		StringBuilder sql = new StringBuilder();
		sql.append(" update LmLoan l set l.curDueDt = ");
		sql   // 7x24小时 只查询未结清的还款计划表 by clq 2017-02-28
				.append(" (select min(lp.psDueDt) from LmPmShd lp where lp.setlInd = 'N' and lp.id.loanNo = l.loanNo and lp.psDueDt>= ? and lp.id.psPerdNo>0) ");
		sql.append(" ,l.lastChgDt=? ");
		sql.append(" where l.loanSts=? ");
		
		try {
			Serializable[] params = new Serializable[3];
			params[0] = psDueDt;
			params[1] = psDueDt;
			params[2] = LoanState.ACTIVE.getCodeInDb();
			super.executeUpdateByHql(sql.toString(), params);

		} catch (RuntimeException re) {
			throw new YcloansDBException(
					"updateLmLoanByPsDueDtandLoanSts error");
		}
	}

	/**
	 * 更新借据的逾期标志
	 */
	public void updateLoanInfo(String buzDate) {
		StringBuilder sql = new StringBuilder();
		sql.append(" update LmLoan l set l.loanOdInd ='Y' ");
		sql.append(" ,l.lastChgDt=? ");
		sql.append(" where l.loanSts='ACTV' ");
		sql.append(" and l.loanOdInd='N'");
		sql.append(" and l.loanPaymTyp<>'05'");
		sql.append(" and (");
		sql.append(" (");
		sql.append(" l.loanDevaInd='Y'");
		sql.append("  and l.loanDebtSts='NORM' ");
		sql.append(" )");
		sql.append(" or");
		sql.append(" exists(from LmPmShd");
		sql.append(" where id.loanNo=l.id.loanNo");
		sql.append(" and psDueDt<=?");
		sql.append(" and id.psPerdNo>0");
		sql.append(" and setlInd='N')");
		sql.append(" )");
		try {
			executeUpdateByHql(sql.toString(), new String[] { buzDate,buzDate });
		} catch (RuntimeException re) {
			throw new YcloansDBException("updateLoanInfo error");
		}
	}

	/**
	 * 删除借据信息
	 * 
	 * @param conn
	 * @param lmloan
	 */
	public void delete(Connection conn, LmLoan lmloan) {
		// log.debug("delete LmLoan instance");
		try {
			getHibernateTemplate().delete(lmloan);
		} catch (RuntimeException e) {
			e.printStackTrace();
			throw new YcloansDBException("find error!", e);
		}
	}

	/**
	 * 贷款减值计提，修改借据主表（上次减值计提日期）
	 */
	public void updateLastDeviDt(Connection conn, String loanNo,
			String operationDay) {
		try {
			StringBuilder sql = new StringBuilder();
			sql.append(" update lm_loan l set l.last_devi_int_acc_dt =? ");
			sql.append(" ,l.lastChgDt=? ");
			sql.append(" where l.loan_no=?");

			this.getCurrentHibernateSession().createSQLQuery(sql.toString())
					.setParameter(0, operationDay).setParameter(1, operationDay).setParameter(2, loanNo)
					.executeUpdate();

		} catch (RuntimeException re) {
			re.printStackTrace();
			throw new YcloansDBException(
					"updateLmLoanByPsDueDtandLoanSts error");
		}
	}

	/**
	 * 根据借据状态查询借据信息
	 */
	@SuppressWarnings("unchecked")
	public List findByLoanSys(Connection conn) {
		try {
			Session session = super.getCurrentHibernateSession();
			StringBuffer queryString = new StringBuffer();

			queryString.append("from LmLoan ll ");
			queryString.append("where ll.loanSts = 'ACTV'");

			Query q = session.createQuery(queryString.toString());

			return q.list();
		} catch (RuntimeException re) {
			re.printStackTrace();
			throw new YcloansDBException("getLoan error!", re);
		}
	}

	/**
	 * 联合查询lm_loan表和lm_ln_info表
	 */
	public List<CheckAccountInfo> findByLoanNoAndBch(Connection conn) {
		return null;
	}

	/**
	 * 根据合同查询列表
	 */
	@SuppressWarnings("unchecked")
	public List<LmLoan> findByLoanContNo(Connection conn, String loanContNo) {
		// log.debug("find LmLoan by loanContNo");
		try {
			StringBuffer queryString = new StringBuffer();
			queryString.append("from LmLoan where loanNo=?");
			Serializable[] params = new Serializable[1];
			params[0] = loanContNo;
			return getHibernateTemplate().find(queryString.toString(), params);
		} catch (RuntimeException e) {
			// log.error("find LmLoan by loanContNo failed", e);
			throw new YcloansDBException("find LmLoan  by loanContNo error!");
		}
	}

	/**
	 * 根据机构号查询借据列表
	 */
	@SuppressWarnings("unchecked")
	public List<LmLoan> findByBchCde(Connection conn, String bchCde) {
		// log.debug("find lmloan by bchcde");
		try {
			StringBuffer queryString = new StringBuffer();
			queryString.append("from LmLoan where bchCde=?");
			Serializable[] params = new Serializable[1];
			params[0] = bchCde;
			return getHibernateTemplate().find(queryString.toString(), params);
		} catch (RuntimeException e) {
			// log.error("find LmLoan by bchCde failed", e);
			throw new YcloansDBException("find LmLoan by bchCde error!");
		}
	}

	/**
	 * 更新减值正常贷款的分页标志
	 */
	public void updateLoanDevCalculatePrcs(Connection conn, String buzDate,
			String prcsPageDtInd) {
		try {
			StringBuffer queryString = new StringBuffer();
			queryString.append( "update LmLoan l set l.prcsPageDtInd =? where l.loanSts = 'ACTV' and l.loanDevaInd = 'Y'");
			if(ServiceContainer.getSystemParameter().isPayInDay()){//如果采用日间扣款模式 则会根据扣款返回状态来判断改日终任务是否执行
				queryString.append(" and (l.atpySts != 'SK' or l.atpySts is null)");
			}
			String hql =queryString.toString();
			Serializable[] params = new Serializable[1];
			params[0] = prcsPageDtInd;
			executeUpdateByHql(hql, params);
		} catch (RuntimeException re) {
			re.printStackTrace();
			throw new YcloansDBException("update error!", re);
		}
	}

	/**
	 * 查询需要作结息处理或者滚积数的所有借据
	 * 
	 * @param conn
	 * @param subBranchCommaString
	 * @return
	 */
	public Pager findLoanInfoToKnotInstOrRollingPlot(Connection conn,
			String subBranchCommaString, String flag, String buzDate, String threadCntStr) {
		StringBuilder hql = new StringBuilder();
		hql.append("select lm, lc ");
		hql.append("From LmLoan lm ,LmLoanCont lc ");
		hql.append("where lm.loanSts=? ");
		hql.append("and lm.loanNo = lc.loanNo ");
		hql.append(" and lc.loanOdCpdInd = 'Y'");//是否计算罚息
		if(ServiceContainer.getSystemParameter().isPayInDay()){//如果采用日间扣款模式 则会根据扣款返回状态来判断改日终任务是否执行
			hql.append(" and (lm.atpySts != 'SK' or lm.atpySts is null)");
		}
		// hql.append("and lm.bchCde in (");
		// hql.append(subBranchCommaString);
		// hql.append(") ");
		hql.append("and lm.prcsPageDtInd='N' ");
		hql.append(" and lm.loanOdIntRate>0");
		if ("0".equals(flag)) {
			//结息日
			
			hql.append(" and ((lm.lastDueDt=? or lm.curDueDt=?)");
			hql.append(" and ");
			hql
					.append("((lm.loanOdInd='Y' or lm.loanDevaInd='Y') ");
			hql.append("and (");
			hql.append(DBSqlUtils.getDateIncDay("lm.nextDueDt", "lm.loanOdGrace", "+"));
			hql.append(" < ?) ");
			//modified by fanyl on 2015-10-09 for 核销后贷款结息
			if(SystemInfo.getSystemInfo().getAccountRule().isChrgoSetlInt()) {
				hql.append(" and (lm.loanDebtSts in('NORM','DELQ','CHRGO'))) ");
			} else {
				hql.append(" and (lm.loanDebtSts in('NORM','DELQ'))) ");
			}
			
			hql.append("or (lm.loanDebtSts='OVER' and lm.curGenOdIntDt=?)");
			hql.append("or lm.bussTyp=? ");
			hql.append(")");
			hql
					.append("and (lm.lastGenOdIntDt<>? or lm.lastGenOdIntDt is null) ");
		} else if ("1".equals(flag)) {
			hql.append(" and lm.loanOdInd='Y'");
			hql.append(" and (lm.lastGenProdDt<? or lm.lastGenProdDt is null)");
			//modified by fanyl on 2015-10-09 for 核销后贷款结息
			if(SystemInfo.getSystemInfo().getAccountRule().isChrgoAcc() 
					|| SystemInfo.getSystemInfo().getAccountRule().isChrgoSetlInt()) {
				hql.append(" and lm.loanDebtSts in ('NORM','OVER','DELQ','CHRGO')");
			} else {
				hql.append(" and lm.loanDebtSts in ('NORM','OVER','DELQ')");
			}
		}
		if (StringUtils.hasText(threadCntStr)) {
			hql.append(" and lm.thdCnt in(").append(threadCntStr)
					.append(") ");
		}
		Serializable[] params;
		if ("0".equals(flag)) {
			params = new Serializable[7];
		} else {
			params = new Serializable[2];
		}
		params[0] = LoanState.ACTIVE.getCodeInDb();
		params[1] = buzDate;
		if ("0".equals(flag)) {
			params[2] = buzDate;
			params[3] = buzDate;
			params[4] = buzDate;
			params[5] = JobBussType.TLOAN.getCodeInDb();
			params[6] = buzDate;
		}
		try {
			return new HibernatePager(this, hql.toString(), params);
		} catch (Exception e) {
			e.printStackTrace();
			throw new YcloansDBException(
					"findLoanInfoToKnotInstOrRollingPlot error!", e);
		}
	}

	/**
	 * 根据最后到期日查询借据信息
	 */
	public List<LmLoan> findByFindAccrual(Connection conn, String buzDate) {

		return null;
	}

	/**
	 * 更新需要计提的借据的分页标志为N
	 * 
	 * @param conn
	 * @param bankCde
	 * @param bchCde
	 * @param loanDevInd
	 * @param loanOdInd
	 * @param buzDate
	 */
	public void updateForFindAccrual(Connection conn, String bankCde,
			String bchCde, YnFlag loanDevInd, YnFlag loanOdInd, String buzDate) {

	}

	/**
	 * 根据贷款状态和贷款财务状态查询借据信息
	 */
	public List<LmLoan> findByStsAndDebt(Connection conn, String loanSts,
			String loanDebtSts) {
		return null;
	}

	/**
	 * 查询所有需要利息计提的借据个数
	 */
	public int findCountByForIntAcc(Connection conn, String buzDate) {
		try {
			StringBuilder hql = new StringBuilder();
			hql.append("select count(*) from LmLoan ll where ");
			hql
					.append(" ll.loanSts=? and ll.loanStpAccInd=? and ll.loanIntRate>0 ");
			hql
					.append(" and ll.loanDevaInd=? and ll.bussTyp<>'TLOAN' and  ll.loanDebtSts=? ");
			hql.append(" and (lastIntAccDt<? or lastIntAccDt is null)");

			Serializable[] params = new Serializable[] {
					LoanState.ACTIVE.getCodeInDb(),
					LoanStpAccInd.NO.getCodeInDb(),
					LoanDevaInd.NO.getCodeInDb(),
					LoanState.NORMAL.getCodeInDb(), buzDate };
			return Integer.parseInt(queryFirstColumnByHql(conn, hql.toString(),
					params).toString());
		} catch (RuntimeException e) {
			throw new YcloansDBException("find error!", e);
		}
	}

	/**
	 * 查询不在某机构内的正常借据个数
	 */
	public int checkBchInLoanNo() {

		try {
			StringBuilder queryString = new StringBuilder();
			queryString.append("select count(*) from LmLoan ll where ");
			queryString.append(" ll.bchCde not in( ");
			queryString.append(" select id.bchCde from SBch");
			queryString.append(" ) ");
			queryString.append(" and ll.loanSts='ACTV'");
			queryString.append(" and ll.loanDebtSts='NORM' ");
			return Integer.parseInt(queryFirstColumnByHql(null,
					queryString.toString(), new Serializable[] {}).toString());
		} catch (RuntimeException e) {
			throw new YcloansDBException("find error!", e);
		}
	}

	/**
	 * 更新正常拖欠逾期借据的分页标志为N，该借据不在当天未撤销的主动还款数据中
	 */
	public int updatePageFlagForRepay(Connection conn, String buzDate) {
		StringBuffer sql = new StringBuffer(30);
		sql.append("update LmLoan l set  l.prcsPageDtInd='"
				+ YnFlag.NO.getCodeInDb() + "'");
		sql.append(" where l.loanSts=? and l.prcsPageDtInd <> 'N'");
		sql.append(" and l.loanDebtSts in (? ,?, ?)");
		sql.append(" and l.nextDueDt<=? or l.nextDueDt is null");
		//sql.append(" and exists (  from  LmLoanCont lc  where ");
		//sql.append(" l.loanContNo=lc.loanContNo  )");
		sql
				.append(" and l.loanNo not in (select lod.loanNo from LmOlDdaLog lod where (lod.revsInd is null or lod.revsInd<>?)  and lod.olDdaDt=? and lod.funcId is not null and lod.funcId = ? )");
		sql.append(" and (l.atpySts != 'SK' or l.atpySts is null)");
		Serializable[] params = new Serializable[8];
		params[0] = LoanState.ACTIVE.getCodeInDb();
		params[1] = LoanState.NORMAL.getCodeInDb();
		params[2] = LoanState.DELAY.getCodeInDb();
		params[3] = LoanState.OVER.getCodeInDb();
		params[4] = buzDate;
		params[5] = YnFlag.YES.getCodeInDb();
		params[6] = buzDate;
		params[7] = LoanVarDef.LNC4;
		return executeUpdateByHql(sql.toString(), params);
	}

	/**
	 * 撤销所有正常或逾期的借据信息
	 */
	public Pager searchACTVLoans(Connection conn) {
		try {
			StringBuffer queryString = new StringBuffer();
			queryString.append("from LmLoan  where ");
			queryString.append("loanSts = 'ACTV' and ");
			queryString
					.append("(loanDebtSts = 'NORM' or  loanDebtSts = 'OVER')and ");
			queryString.append("bussTyp = 'NLOAN' ");
			Serializable[] params = new Serializable[] {};

			return new HibernatePager(this, queryString.toString(), params);
		} catch (RuntimeException re) {
			re.printStackTrace();
			throw new YcloansDBException("searchACTVLoans error!", re);
		}
	}

	/**
	 * 五级分类借据列表
	 */
	public Pager findLoanPagerByBchcdes(Connection conn, String bankCde,
			String bchCde) {

		StringBuffer bchCdes = SystemInfo.getSystemInfo()
				.getSubBranchCommaStringIncludeSelf(bankCde, bchCde);

		StringBuffer sqlbuff = new StringBuffer(30);
		sqlbuff
				.append("From LmLoan ll where ll.loanSts = ? and ll.prcsPageDtInd='N' ");
		sqlbuff
				.append("and ll.loanDebtSts in ('OVER','NORM') and ll.loanOdInd='Y' ");
		sqlbuff.append("and ll.bankCde=? and ll.bchCde in (");
		sqlbuff.append(bchCdes).append(")");
		Serializable[] params = new Serializable[2];
		params[0] = LoanState.ACTIVE.getCodeInDb();
		params[1] = bankCde;

		try {
			return new HibernatePager(this, sqlbuff, params);

		} catch (Exception e) {
			e.printStackTrace();
			throw new YcloansDBException("findLoanInfoByBchcdes error!", e);
		}
	}

	/**
	 * 更新正常或逾期状态且在指定范围机构内的借据的分页标志为N
	 */
	public void updateForPrcsPageDtInd(Connection conn, String bankCde,
			String bchCde) {
		StringBuffer bchCdes = SystemInfo.getSystemInfo()
				.getSubBranchCommaStringIncludeSelf(bankCde, bchCde);
		StringBuffer queryString = new StringBuffer();
		queryString
				.append("update LmLoan ll set ll.prcsPageDtInd='N' where ll.loanSts=? and ll.prcsPageDtInd <> 'N'");
		queryString
				.append("and ll.loanDebtSts in('OVER','NORM') and ll.loanOdInd='Y' ");
		queryString.append("and ll.bankCde=? and ll.bchCde in (");
		queryString.append(bchCdes).append(")");

		Serializable[] paramValues = new Serializable[2];
		paramValues[0] = LoanState.ACTIVE.getCodeInDb();
		paramValues[1] = bankCde;

		try {
			executeUpdateByHql(queryString.toString(), paramValues);
		} catch (RuntimeException e) {
			e.printStackTrace();
			throw new YcloansDBException("update error!", e);
		}
	}

	/**
	 * 根据holdFee更新借据的下次扣款日
	 * 
	 */
	public void updateLmLoanByHoldFee(Connection conn, String buzDt) {

		StringBuilder sql = new StringBuilder();
		sql
				.append("update lm_loan l set l.next_due_dt=?,l.LAST_CHG_DT=? where (l.next_due_dt<>? or l.next_due_dt is null) ");
		sql
				.append("and l.loan_no in(select lh.loan_no from lm_hold_fee_tx lh ");
		sql
				.append("where lh.loan_no=l.loan_no and lh.hold_setl_dt<=? and lh.setl_ind=? )");
		try {
			getCurrentHibernateSession().createSQLQuery(sql.toString())
					.setParameter(0, buzDt).setParameter(1, buzDt).setParameter(2, buzDt)
					.setParameter(3, buzDt).setParameter(4,
							YnFlag.NO.getCodeInDb()).executeUpdate();

		} catch (RuntimeException re) {
			throw new YcloansDBException("updateLmLoanByHoldFee error");
		}
	}

	/**
	 * 根据下次同步利率日期nextSynRateDt查找借据信息
	 * 
	 */
	public Pager findLmLoanByNextSynRateDt(Connection conn, String buzDt,
			String bankCde, String bchCde) {

		try {
			StringBuffer queryString = new StringBuffer();
			queryString.append("from LmLoan ll where ll.loanSts=? ");
			queryString
					.append("and ll.nextSynRateDt=? and ll.prcsPageDtInd=? ");

			Serializable[] params = new Serializable[3];
			params[0] = LoanState.ACTIVE.getCodeInDb();
			params[1] = buzDt;
			params[2] = YnFlag.NO.getCodeInDb();

			return new HibernatePager(this, queryString.toString(), params);

		} catch (RuntimeException re) {
			re.printStackTrace();
			throw new YcloansDBException("findLmLoanByNextSynRateDt error!", re);
		}
	}
	/**
	 * 根据贷款状态查询借据信息
	 */
	public Pager findLmLoanByLoanSts(Connection conn, String buzDt,
			String bankCde, String bchCde) {

		try {
			StringBuffer queryString = new StringBuffer();
			queryString.append("from LmLoan ll where ll.loanSts=? ");
			Serializable[] params = new Serializable[1];
			params[0] = LoanState.SETL.getCodeInDb();
			return new HibernatePager(this, queryString.toString(), params);

		} catch (RuntimeException re) {
			re.printStackTrace();
			throw new YcloansDBException("findLmLoanByLoanSts error!", re);
		}
	}

	/**
	 * 更新正常状态且指定下次同步方式表利率日期的借据的分页标志为N
	 */
	public void updatePageIndByNextSynRateDt(Connection conn, String buzDt,
			String bankCde, String bchCde) {

		StringBuffer queryString = new StringBuffer();
		queryString
				.append("update LmLoan ll set ll.prcsPageDtInd='N' where ll.loanSts=? ");
		queryString.append(" and ll.prcsPageDtInd <> 'N' and ll.nextSynRateDt=? ");

		Serializable[] paramValues = new Serializable[2];
		paramValues[0] = LoanState.ACTIVE.getCodeInDb();
		paramValues[1] = buzDt;

		try {
			executeUpdateByHql(queryString.toString(), paramValues);
		} catch (RuntimeException e) {
			e.printStackTrace();
			throw new YcloansDBException("update error!", e);
		}
	}

	/**
	 * 更新已结清借据的分页标志为N
	 */
	public void updatePageIndByLoanSts(Connection conn, String buzDt,
			String bankCde, String bchCde) {

		StringBuffer queryString = new StringBuffer();
		queryString
				.append("update LmLoan ll set ll.prcsPageDtInd='N' where ll.loanSts=?  and ll.prcsPageDtInd <> 'N'");
		Serializable[] paramValues = new Serializable[1];
		paramValues[0] = LoanState.SETL.getCodeInDb();

		try {
			executeUpdateByHql(queryString.toString(), paramValues);
		} catch (RuntimeException e) {
			e.printStackTrace();
			throw new YcloansDBException("update error!", e);
		}
	}

	/**
	 * 根据指定日期与业务日期查询LM_LOAN
	 */
	@SuppressWarnings("unchecked")
	public List<LmLoan> findBybuzDate(Connection conn, String buzDate) {
		StringBuffer hql = new StringBuffer();
		hql.append(" from LmLoan  where lastDueDt <= ? ");
		hql
				.append(" and loanRateMode =? and id.loanNo not in (select loanNo from OldLmLoan)");
		Serializable[] paramValues = new Serializable[] { buzDate,
				LoanRateMode.RV.getCodeInDb() };
		try {
			return getHibernateTemplate().find(hql.toString(), paramValues);
		} catch (RuntimeException e) {
			e.printStackTrace();
			throw new YcloansDBException("findByAppointedDay LmLoan error!", e);
		}
	}

	/**
	 * 根据借据号修改借据的利率模式（FX: 固定利率）
	 */
	public void updateByLoanNo(Connection conn, String buzDate) {
		try {
			StringBuffer sql = new StringBuffer();
			sql
					.append(" update LmLoan set loanRateMode=?,lastChgDt=? where loanRateMode =? and  id.loanNo ");
			sql
					.append(" in (select id.loanNo from LmLoan  where lastDueDt<= ? ");
			sql.append(" and id.loanNo not in (select loanNo from OldLmLoan) ");
			sql.append(" ) ");
			Serializable[] params = new Serializable[] {
					LoanRateMode.FX.getCodeInDb(),
					buzDate,
					LoanRateMode.RV.getCodeInDb(), buzDate };

			executeUpdateByHql(sql.toString(), params);
		} catch (RuntimeException re) {
			throw new YcloansDBException("updateLoansByLoanNo LmLoan failed",
					re);
		}
	}

	/**
	 * 普通贷款台帐前更新分页标志
	 * 
	 * @param conn
	 */
	public void updateByBussTyp(Connection conn) {
		StringBuffer queryString = new StringBuffer();
		queryString.append("update LmLoan  set prcsPageDtInd='N' ");
		queryString.append(" where prcsPageDtInd <> 'N' and bussTyp = '"
				+ JobBussType.NLOAN.getCodeInDb() + "' ");
		queryString.append(" or bussTyp = '" + JobBussType.TLOAN.getCodeInDb()
				+ "' ");
		try {
			executeUpdateByHql(queryString.toString(), null);
		} catch (RuntimeException e) {
			throw new YcloansDBException("update error!", e);
		}
	}

	/**
	 * 更新指定借据的分页标志为Y
	 */
	public void updateBatComAcc(Connection conn, String loanNo) {
		StringBuffer queryString = new StringBuffer();
		queryString.append("update LmLoan  set prcsPageDtInd='Y' ");
		queryString.append(" where loanNo = ?");
		Serializable[] params = new Serializable[] { loanNo };
		try {
			executeUpdateByHql(queryString.toString(), params);
		} catch (RuntimeException e) {
			throw new YcloansDBException("updateBatComAcc error!", e);
		}
	}

	/**
	 * 普通贷款台帐
	 */
	public Pager findByBussTyp(Connection conn) {

		try {
			StringBuffer queryString = new StringBuffer();
			queryString
					.append(" select new com.yuchengtech.ycloans.db.domain.BatComAcc( ");
			queryString.append(" l.loanNo as loanNo,");
			queryString.append(" '' as acctNo,");
			queryString.append(" l.loanTyp as loanTyp,");
			queryString.append(" l.loanDebtSts as loanDebtSts,");
			queryString.append(" l.loanOsPrcp as loanOsPrcp,");
			queryString.append(" l.lastDueDt as lastDueDt,");
			queryString.append(" l.loanPaymTyp as loanPaymTyp,");
			queryString.append(" l.loanPaymMtd as loanPaymMtd,");
			queryString.append(" l.loanIntRate as loanIntRate,");
			queryString.append(" lc.odRateAdjPct as odRateAdjPct,");
			queryString.append(" l.loanOdIntRate as loanOdIntRate,");
			queryString.append(" lc.loanDiverAdjPct as diverAdjPct,");
			queryString.append(" lc.loanDiverIntRate as loanDiverIntRate,");
			queryString.append(" l.bchCde as bchCde,");
			queryString
					.append(" (select osAcctAmt from LmLnInfo a where a.id.loanNo = l.loanNo and a.id.acctAmtTyp='A03') as offReceInt,");
			queryString
					.append(" (select osAcctAmt from LmLnInfo a where a.id.loanNo = l.loanNo and a.id.acctAmtTyp='A01') as innerReceInt,");
			queryString
					.append(" (select osAcctAmt from LmLnInfo a where a.id.loanNo = l.loanNo and a.id.acctAmtTyp='H01') as overdueReceInt) ");
			queryString
					.append(" from LmLoan l, LmLoanCont lc where l.loanNo = lc.loanNo and l.prcsPageDtInd='N' AND ");
			queryString.append(" (l.bussTyp = '"
					+ JobBussType.NLOAN.getCodeInDb() + "'");
			queryString.append(" or l.bussTyp = '"
					+ JobBussType.TLOAN.getCodeInDb() + "')");

			return new HibernatePager(this, queryString, null);

		} catch (RuntimeException re) {
			re.printStackTrace();
			throw new YcloansDBException("findByBussTyp error!", re);
		}
	}

	/**
	 * 查询下次需要扣款的借据个数
	 */
	public int findCountRepayDataByJobContext(Connection conn,
			BatchJobContext bjc, long jobSeq) {
		String buzDate = bjc.getBuzDate();
		StringBuffer sql = new StringBuffer(30);
		sql.append("select count(*)");
		sql.append(" from LmLoan l ");
		sql.append(" where ");
		sql.append(" l.loanSts=?");
		sql.append(" and l.loanDebtSts in (? , ?)");
		sql.append(" and (");
		sql.append(" (l.nextDueDt<=? or l.nextDueDt is null)");// 涓嬫搴旀墸鏃ユ湡
		sql.append(")");
		if (jobSeq > 0) {
			if (StringUtils.hasText(bjc.getThreadCntStr())) {
				sql.append(" and l.thdCnt in(").append(bjc.getThreadCntStr())
						.append(")");
			}
		}
		// 新增加的条件,不再已完成的状态
		// /不处理已经冻结的账号
		sql.append(" and l.loanNo not in (");
		sql.append("  select  la.loanNo from LmAtpyDetl la");
		if (jobSeq > 0) {
			sql.append(" where la.atpyValDt=? and la.atpySeqNo=?");
		} else {
			sql.append(" where la.atpyValDt=? ");
		}
		sql.append(" and la.atpyPaymAmt>0 ");
		sql.append(" and la.atpySts  in('").append(
				ATPYState.FREEZE.getCodeInDb()).append("','").append(
				ATPYState.COMPLETE.getCodeInDb()).append("'))");
		// sql.append(" and night=?");
		Serializable[] params = (jobSeq > 0) ? new Serializable[6]
				: new Serializable[5];
		params[0] = LoanState.ACTIVE.getCodeInDb();
		params[1] = LoanState.NORMAL.getCodeInDb();
		params[2] = LoanState.OVER.getCodeInDb();
		params[3] = buzDate;
		params[4] = buzDate;
		if (jobSeq > 0)
			params[5] = jobSeq;
		return Integer.parseInt(queryFirstColumnByHql(conn, sql.toString(),
				params).toString());
	}

	/**
	 * 查询需要扣款的借据分页对象
	 */
	public Pager searchRepayDataByJobContext(Connection conn,
			BatchJobContext bjc, long jobSeq) {
		String buzDate = bjc.getBuzDate();
		StringBuffer sql = new StringBuffer(30);
		sql.append("select l");
		sql.append(" from LmLoan l ");
		sql.append(" where ");
		sql.append(" l.loanSts=?");
		sql.append(" and l.loanDebtSts in (? , ?,?)");
		sql.append(" and (");
		sql.append(" (l.loanDevaInd='Y' ");
		sql.append(" and l.loanDebtSts='NORM'");
		//sql.append(" and (l.loanDevaOrd is null or l.loanDevaOrd<>'I')");
		sql.append(" ) or ");
		sql.append(" (l.nextDueDt<=? or l.nextDueDt is null)");// 涓嬫搴旀墸鏃ユ湡
		sql.append(")");
		if (StringUtils.hasText(bjc.getThreadCntStr())) {
			sql.append(" and l.thdCnt in(").append(bjc.getThreadCntStr())
					.append(")");
		}
		sql.append(" and l.prcsPageDtInd=?");
		// 新增加的条件,不再已完成的状态
		// /不处理已经冻结的账号
		sql.append(" and l.loanNo not in (");
		sql.append("  select  la.loanNo from LmAtpyDetl la");
		sql.append(" where la.atpyValDt=? and la.atpySeqNo=?");
		sql.append(" and la.atpySts  in('").append(
				ATPYState.COMPLETE.getCodeInDb()).append("','").append(
				ATPYState.FREEZE.getCodeInDb()).append("'))");
		// sql.append(" and night=?");
		//7*24小时还款数据，不再进行批扣
		sql.append(" and l.loanNo not in (");
		sql.append(" select i.loanNo from IntfLoanTxQue i ");
		sql.append(" where i.loanNo=l.loanNo and i.prcsSts='INIT' ");
		sql.append(" and i.txTyp='").append(LoanVarDef.LNC4).append("' and i.txDt=?) ");
		sql.append(" order by l.loanNo");
		Serializable[] params = new Serializable[] {
				LoanState.ACTIVE.getCodeInDb(), LoanState.NORMAL.getCodeInDb(),
				LoanState.DELQ.getCodeInDb(), LoanState.OVER.getCodeInDb(),
				buzDate, YnFlag.NO.getCodeInDb(), buzDate, jobSeq, buzDate };
		return new HibernatePager(this, sql, params);
	}

	/**
	 * 查询需要扣款的借据条数
	 */
	public int findRepayDataCount(Connection conn, String buzDate) {
		StringBuffer sql = new StringBuffer(30);
		sql.append(" select count(*) ");
		sql.append(" from LmLoan l,LmLoanCont lc  ");
		sql.append(" where l.loanNo=lc.loanNo");
		sql.append(" and l.loanSts=?");
		sql.append(" and lc.loanRepayMthd='").append(
				LoanRepayMethod.AUTOPAY.getCodeInDb()).append("'");
		sql.append(" and l.loanDebtSts in (? , ?)");
		sql.append(" and (");
		sql.append(" (l.loanDevaInd='Y' ");
		sql.append(" and l.loanDebtSts='NORM'");
		//sql.append(" and (l.loanDevaOrd is null or l.loanDevaOrd<>'I')");
		sql.append(" ) or ");
		sql.append(" (l.nextDueDt<=? or l.nextDueDt is null)");// 涓嬫搴旀墸鏃ユ湡
		sql.append(")");
		// 新增加的条件,不再已完成的状态
		// /不处理已经冻结的账号
		sql.append(" and l.loanNo not in (");
		sql.append("  select  la.loanNo from LmAtpyDetl la");
		sql.append(" where la.atpyValDt=?").append(")");
		// sql.append(" and night=?");
		Serializable[] params = new Serializable[5];
		params[0] = LoanState.ACTIVE.getCodeInDb();
		params[1] = LoanState.NORMAL.getCodeInDb();
		params[2] = LoanState.OVER.getCodeInDb();
		params[3] = buzDate;
		params[4] = buzDate;
		return Integer.parseInt(executeHqlQueryReturnOneValue(sql.toString(),
				params).toString());
	}

	/**
	 * 根据线程号更新借据分页状态
	 */
	public synchronized int updatePageFlagRepayByThdCnt(Connection conn,
			BatchJobContext bjc, String night) {

		String buzDate = bjc.getBuzDate();
		StringBuffer sql = new StringBuffer(30);
		sql.append("update LmLoan l set  l.prcsPageDtInd='"
				+ YnFlag.NO.getCodeInDb() + "'");
		sql.append(" where l.loanSts=? and l.prcsPageDtInd <> 'N'");
		// sql.append(" and night=? ");
		if (StringUtils.hasText(bjc.getThreadCntStr())) {
			sql.append(" and l.thdCnt in(").append(bjc.getThreadCntStr())
					.append(")");
		}
		sql.append(" and (l.nextDueDt<=? or");// 涓嬫搴旀墸鏃ユ湡
		sql.append(" (");
		sql.append(" loanDevaInd='Y'");
		sql.append(" and loanDebtSts='NORM')");
		sql.append("  or");
		sql.append("  l.nextDueDt is null)");
		//sql.append(" and exists (  from  LmLoanCont lc  where ");
		//sql.append(" l.loanContNo=lc.loanContNo  )");
		//sql.append("and not exists(  from LmAtpySts s where  s.loanNo=l.loanNo  ");
		sql.append(" and (l.atpySts != 'SK' or l.atpySts is null)");
		Serializable[] params = new Serializable[2];
		params[0] = LoanState.ACTIVE.getCodeInDb();
		// params[1] = LoanRepayMethod.AUTOPAY.getCodeInDb();
		// params[1] = night;
		params[1] = buzDate;
		// params[2] = branchCode;
		return executeUpdateByHql(sql.toString(), params);
	}

	/**
	 * 查询分页标志为N，且指定线程号的借据信息
	 */
	public Pager searchLoanForCheck(Connection conn, BatchJobContext bjc) {
		StringBuffer sql = new StringBuffer(30);
		sql.append("select l");
		sql.append(" from LmLoan l where l.prcsPageDtInd='"
				+ YnFlag.NO.getCodeInDb() + "'");

		if (StringUtils.hasText(bjc.getThreadCntStr())) {
			sql.append(" and l.thdCnt in(").append(bjc.getThreadCntStr())
					.append(")");
		}
		Serializable[] params = new Serializable[0];

		return new HibernatePager(this, sql, params);
	}

	/**
	 * 更新指定线程号的借据的分页标志为N
	 */
	public synchronized int updateLoanForCheck(Connection conn,
			BatchJobContext bjc) {
		StringBuffer sqlbuff = new StringBuffer(30);
		sqlbuff.append("update LmLoan lm set  lm.prcsPageDtInd='N' ");
		sqlbuff.append(" where lm.prcsPageDtInd <> 'N' and lm.loanOdInd=? ");
		sqlbuff.append(" and lm.loanSts = ? ");
		sqlbuff.append(" and lm.loanNo in (select lh.loanNo from LmHoldFeeTxHdr lh where lh.loanNo=lm.loanNo) ");
		if(ServiceContainer.getSystemParameter().isPayInDay()){//如果采用日间扣款模式 则会根据扣款返回状态来判断改日终任务是否执行
			sqlbuff.append(" and (lm.atpySts != 'SK' or lm.atpySts is null)");
		}
		if (StringUtils.hasText(bjc.getThreadCntStr())) {
			sqlbuff.append(" and lm.thdCnt in(").append(bjc.getThreadCntStr())
					.append(")");
		}
		Serializable[] params = new Serializable[2];
		params[0] = YnFlag.YES.getCodeInDb();
		params[1] = LoanState.ACTIVE.getCodeInDb();
		return executeUpdateByHql(sqlbuff.toString(), params);
	}

	/**
	 * 查询未结清借据且分页标志为N的借据分页信息
	 */
	public Pager findPaymentBatch(Object object, String buzDate,
			String threadCntStr) {
		try {
			StringBuilder queryString = new StringBuilder();
			queryString.append("from LmLoan ll where ");
			queryString.append(" ll.loanSts <>? ");
			queryString.append("  and ll.loanDebtSts<>?  ");
			queryString.append(" and ll.prcsPageDtInd='N' ");
			if (StringUtils.hasText(threadCntStr)) {
				queryString.append(" and ll.thdCnt in(").append(threadCntStr)
						.append(") ");
			}
			Serializable[] param = new Serializable[] {
					AccountState.SETTLED.getCodeInDb(),
					LoanState.SETL.getCodeInDb() };

			return new HibernatePager(DAOContainer.getCommonDbDAO(false),
					queryString.toString(), param);
		} catch (RuntimeException e) {
			e.printStackTrace();
			throw new YcloansDBException("find error!", e);
		}
	}

	/**
	 * 更新未结清借据的分页标志为N
	 */
	public void updateForPaymentBatch(Object object, String buzDate,
			String threadCntStr) {
		try {
			Session session = super.getCurrentHibernateSession();
			StringBuffer queryString = new StringBuffer();
			queryString
					.append("update LmLoan ll set ll.prcsPageDtInd='N' where ll.loanSts <>?  and ll.loanDebtSts<>? and ll.prcsPageDtInd <> 'N' ");
			queryString.append(" and ll.thdCnt in(").append(threadCntStr)
					.append(")");
			Query q = session.createQuery(queryString.toString());
			q.setParameter(0, AccountState.SETTLED.getCodeInDb());
			q.setParameter(1, LoanState.SETL.getCodeInDb());
			q.executeUpdate();
		} catch (RuntimeException e) {
			e.printStackTrace();
			throw new YcloansDBException("find error!", e);
		}

	}

	/**
	 * 更新未结清借据的分页标志为N
	 */
	public void updateForPaymentBatch(Object object, String buzDate) {
		try {
			Session session = super.getCurrentHibernateSession();
			StringBuffer queryString = new StringBuffer();
			queryString
					.append("update LmLoan ll set ll.prcsPageDtInd='N' where ll.loanSts <>?  and ll.loanDebtSts<>? and ll.prcsPageDtInd <> 'N' ");
			Query q = session.createQuery(queryString.toString());
			q.setParameter(0, AccountState.SETTLED.getCodeInDb());
			q.setParameter(1, LoanState.SETL.getCodeInDb());
			q.executeUpdate();
		} catch (RuntimeException e) {
			e.printStackTrace();
			throw new YcloansDBException("find error!", e);
		}
	}

	/**
	 * 更新还款计划表的逾期标志
	 * */
	public int updatePsOdInd(Connection conn, String buzDt) {

		StringBuffer sqlBf = new StringBuffer();
		sqlBf.append(" update LmPmShd l set l.lastChgDt = ?,l.prcpState = '30',l.intState = '30',l.psOdInd = 'Y' ");
		sqlBf.append(" where exists (select lm.loanNo from LmLoan lm where lm.loanNo = l.id.loanNo and lm.loanStpAccInd <> 'N' ");
		if(ServiceContainer.getSystemParameter().isPayInDay()){//如果采用日间扣款模式 则会根据扣款返回状态来判断改日终任务是否执行
			sqlBf.append(" and (lm.atpySts != 'SK' or lm.atpySts is null)");
		}
		sqlBf.append(")");
		sqlBf.append(" and l.id.psPerdNo<>0 and l.psDueDt <= ? and (l.prcpState <> '30' or l.intState <> '30') ");
		sqlBf.append(" and l.setlInd = 'N'");  //只能修改未结清的数据
	
		
		try {
			return executeUpdateByHql(sqlBf.toString(), new Serializable[] { buzDt,buzDt });		
		} catch (RuntimeException re) {
			re.printStackTrace();
			throw new YcloansDBException("updatePsOdInd error");
		}
	}

	/**
	 * 贷款业务
	 * 
	 * 滞纳金核算部分
	 * 
	 * 查找逾期的贷款
	 * 
	 * @param no
	 * @param yes
	 * @return
	 */
	public Pager findLoanByLoanOdInd(Connection conn,String buzDt,String funcId) {
		StringBuffer sqlbuff = new StringBuffer(30);
		sqlbuff
				.append(" select lm from LmLoan lm where ");
		sqlbuff
		.append(" lm.prcsPageDtInd = ? and lm.loanOdInd =? and lm.loanSts = ? " );
		//modified by fanyl on 2015-09-10 for 宽限期校验提至sql中
		sqlbuff.append(" and ((lm.loanGraceTyp='E' and ");
		sqlbuff.append(DBSqlUtils.getDateIncDay("lm.nextDueDt", "lm.loanOdGrace", "+")).append(" <= ?) ");
		sqlbuff.append(" or (lm.loanGraceTyp='P' and lm.nextDueDt <= ?) or lm.loanGraceTyp is null) ");
		//modified by fanyl on 2015-11-19 for 滞纳金结记sql修改
		sqlbuff.append(" and lm.loanNo in (select lm.loanNo from LmHoldFeeTxHdr lh where lh.loanNo=lm.loanNo ");
		//滞纳金结记
		if(LoanVarDef.LNLF.equals(funcId)) {
			sqlbuff.append(" and lh.feeTyp = '");
			sqlbuff.append(LoanVarDef.FEE_TYP_LATE);
			sqlbuff.append("' and (lh.nextSetlDt is null or lh.nextSetlDt <= ?)) ");
		} else if(LoanVarDef.LNLP.equals(funcId)) {//违约金结记
			sqlbuff.append(" and lh.feeTyp = '");
			sqlbuff.append(LoanVarDef.FEE_TYP_PENAL);
			sqlbuff.append("' and (lh.lastSetlDt is null or lh.lastSetlDt < ?)) ");
		}
		if(ServiceContainer.getSystemParameter().isPayInDay()){//如果采用日间扣款模式 则会根据扣款返回状态来判断改日终任务是否执行
			sqlbuff.append(" and (lm.atpySts != 'SK' or lm.atpySts is null)");
		}
		Serializable[] params = null;
		params = new Serializable[] { YnFlag.NO.getCodeInDb(),
				YnFlag.YES.getCodeInDb(), LoanState.ACTIVE.getCodeInDb(),buzDt,
				buzDt,buzDt};		
		try {
			return new HibernatePager(this, sqlbuff, params);
		} catch (Exception e) {
			e.printStackTrace();
			throw new YcloansDBException("findLoanInfoByBchcde error!", e);
		}

	}

	/**
	 * @desc 查询可做计提的借据
	 */
	public List<LmLoan> searchLoanDevPage(Connection conn, String buzDate) {
		try {
			StringBuffer queryString = new StringBuffer();
			queryString.append("select ll ");
			queryString.append(" from LmLoan ll where ll.loanSts='ACTV' ");
			queryString
					.append(" and ll.loanDebtSts in('NORM','OVER') and ll.loanOsPrcp>0  ");
			queryString
					.append(" and ( ll.lastDeviIntAccDt<? or ll.lastDeviIntAccDt is null)");
			Serializable[] params = new Serializable[1];
			params[0] = buzDate;
			return this.getHibernateTemplate().find(queryString.toString(),
					params);
		} catch (RuntimeException re) {
			re.printStackTrace();
			throw new YcloansDBException("searchContByLoanDevPage error!", re);
		}
	}

	/**
	 * 更新借据的逾期标志
	 */
	public void updateLoanOdInd(String buzDate) {
		try {
			StringBuilder queryString = new StringBuilder();
			queryString.append("update LmLoan l");
			queryString.append(" set l.loanOdInd='Y'");
			queryString.append(" ,l.lastChgDt=?");
			queryString.append(" where l.loanOdInd='N'"); 
			queryString.append(" and ((l.nextDueDt<=? and l.allOver = 'N') or (l.lastDueDt<=? and l.allOver = 'Y')) and l.loanSts='ACTV'"); 
			
//			queryString.append(" and exists ("); 
//			queryString.append(" from LmAtpyDetl la");
//			queryString.append(" where la.loanNo=l.loanNo");
			/*queryString.append(" and to_date(la.atpyValDt,'").append(dateFormat)
			.append("') = ?");*/
//			queryString.append(" and la.atpyValDt=?");
//			queryString.append(" and la.atpyTxAmt<=0)");
			if(ServiceContainer.getSystemParameter().isPayInDay()){//如果采用日间扣款模式 则会根据扣款返回状态来判断改日终任务是否执行
				queryString.append(" and (l.atpySts != 'SK' or l.atpySts is null)");
			}
			executeUpdateByHql(queryString.toString(), new Serializable[] { buzDate,buzDate,buzDate });
		} catch (RuntimeException e) {
			e.printStackTrace();
			throw new YcloansDBException("updateLoanOdInd error!", e);
		}
	}
	
	/**
	 * 更新借据的LOAN_DEBT_STS
	 */
	public void updateLoanDebtSts() {
		try {
			StringBuilder queryString = new StringBuilder();
			queryString.append("update LmLoan l");
			queryString.append(" set l.loanDebtSts ='NORM' ");
			queryString.append(" ,l.lastChgDt=?");
			queryString.append(" where l.loanSts = 'ACTV' "); 
			queryString.append(" and l.loanOdInd ='N' ");
			queryString.append(" and l.loanDebtSts !='NORM' and l.loanDebtSts !='CHRGO' "); 
			String buzDate = SystemInfo.getSystemInfo().getBuzDate();
			executeUpdateByHql(queryString.toString(), new Serializable[] {buzDate});
		} catch (RuntimeException e) {
			e.printStackTrace();
			throw new YcloansDBException("updateLoanOdInd error!", e);
		}
	}
		/**
	 * 查询需要从溢缴款扣款的借据分页对象
	 * @param conn
	 * @param bjc
	 * @return
	 */
	public Pager searchIntrAcctPaymData(Connection conn, BatchJobContext bjc) {
		String buzDate = bjc.getBuzDate();
		StringBuffer sql = new StringBuffer(30);
		sql.append("select l,lc");
		sql.append(" from LmLoan l,LmLoanCont lc  ");
		sql.append(" where l.loanNo=lc.loanNo");
		sql.append(" and l.loanSts=? and l.loanDebtSts in (?,?,?)");
		sql.append(" and (");
		sql.append(" 	(l.loanDevaInd='Y' and l.loanDebtSts='NORM' )");
		sql.append(" 	or (l.nextDueDt<=? or l.nextDueDt is null)");
		sql.append(" )");
		if (StringUtils.hasText(bjc.getThreadCntStr())) {
			sql.append(" and l.thdCnt in(").append(bjc.getThreadCntStr()).append(")");
		}
		sql.append(" and l.prcsPageDtInd=?");
		sql.append(" and exists");
		sql.append(" ( select 1 from LmIntrAcctInfo lp where lp.id.idType=lc.idType and lp.id.issCtry=lc.issCtry and lp.id.idNo=lc.idNo and lp.osAmt > 0)");
		Serializable[] params = new Serializable[] {
				LoanState.ACTIVE.getCodeInDb(), LoanState.NORMAL.getCodeInDb(),
				LoanState.DELQ.getCodeInDb(), LoanState.OVER.getCodeInDb(),
				buzDate, YnFlag.NO.getCodeInDb()
		};
		return new HibernatePager(this, sql, params);
	}
	

	/**
	 * 更新需要从溢缴款扣款的借据分页对象
	 * @param conn
	 * @param bjc
	 * @return
	 */
	public int updateIntrAcctPaymData(Connection conn, BatchJobContext bjc) {
		String buzDate = bjc.getBuzDate();
		StringBuffer sql = new StringBuffer(30);
		sql.append("update LmLoan l set l.prcsPageDtInd=?");
		sql.append(" where  l.loanSts=? and l.loanDebtSts in (?,?,?) and l.prcsPageDtInd <> 'N' ");
		sql.append(" and (");
		sql.append(" 	(l.loanDevaInd='Y' and l.loanDebtSts='NORM'  ");
		//sql.append(" and (l.loanDevaOrd is null or l.loanDevaOrd<>'I')");
		sql.append(")");
		sql.append(" 	or (l.nextDueDt<=? or l.nextDueDt is null)");
		sql.append(" )");
		if (StringUtils.hasText(bjc.getThreadCntStr())) {
			sql.append(" and l.thdCnt in(").append(bjc.getThreadCntStr()).append(")");
		}
		sql.append(" and exists");
		sql.append(" ( select 1 from LmIntrAcctInfo lp,LmLoanCont lc");
		sql.append(" where l.loanNo= lc.loanNo and lp.id.idType=lc.idType");
		sql.append(" and lp.id.issCtry=lc.issCtry and lp.id.idNo=lc.idNo and lp.osAmt > 0)");
		Serializable[] params = new Serializable[] {
				YnFlag.NO.getCodeInDb(),
				LoanState.ACTIVE.getCodeInDb(), 
				LoanState.NORMAL.getCodeInDb(),
				LoanState.DELQ.getCodeInDb(), 
				LoanState.OVER.getCodeInDb(),
				buzDate
		};
		return executeUpdateByHql(sql.toString(), params);
	}
	
	/**
	 * 贷款业务
	 * 
	 * 费用收取日期结费用
	 * @param no
	 * @param yes
	 * @return
	 */
	public Pager findLoanByHoldFee(Connection conn, String buzDt) {
		StringBuffer sqlbuff = new StringBuffer(30);
		sqlbuff
				.append(" from LmLoan lm where lm.prcsPageDtInd = ? and lm.loanSts = ?");
		sqlbuff
				.append(" and exists (select lm.loanNo from LmHoldFeeTx lh where lh.loanNo=lm.loanNo ");
		sqlbuff
				.append(" and (lh.setlInd is null or lh.setlInd = ?) and lh.holdSetlDt = ? and (lh.accInd is null or lh.accInd='N')");
		sqlbuff.append(" and (lh.feeResource is null or lh.feeResource=?)");
		sqlbuff.append(" and (lh.lastSetlDt is null or lh.lastSetlDt < ?))");
//		if(ServiceContainer.getSystemParameter().isPayInDay()){//如果采用日间扣款模式 则会根据扣款返回状态来判断改日终任务是否执行
//			sqlbuff.append(" and lm.atpySts != 'SK'  ) ");
//		}
		Serializable[] params = new Serializable[] { YnFlag.NO.getCodeInDb(),
				 LoanState.ACTIVE.getCodeInDb(),YnFlag.NO.getCodeInDb(),buzDt,
				 LoanVarDef.FEE_RESOURCE_CUSTER,buzDt };
		try {
			return new HibernatePager(this, sqlbuff, params);
		} catch (Exception e) {
			e.printStackTrace();
			throw new YcloansDBException("findLoanInfoByBchcde error!", e);
		}

	}
	
	/**
	 * 费用收取日期结费用批量更新分页标志
	 */
	public void updateForFindHoldFee(Connection conn,String buzDt) {
		StringBuffer queryString = new StringBuffer();
		queryString
				.append("update LmLoan ll set ll.prcsPageDtInd=? where ll.loanSts = ?  and ll.prcsPageDtInd <> 'N' ");
		queryString
				.append(" and exists(select lh.loanNo from LmHoldFeeTx lh where lh.loanNo=ll.loanNo ");
		queryString
				.append(" and (lh.setlInd is null or lh.setlInd = ?) and lh.holdSetlDt = ? and (lh.accInd is null or lh.accInd='N')");
		queryString.append("and (lh.feeResource is null or lh.feeResource=?)");
		queryString.append(" and (lh.lastSetlDt is null or lh.lastSetlDt < ?))");
		if(ServiceContainer.getSystemParameter().isPayInDay()){//如果采用日间扣款模式 则会根据扣款返回状态来判断改日终任务是否执行
			queryString.append(" and (ll.atpySts != 'SK' or ll.atpySts is null)");
		}
		Serializable[] paramValues = new Serializable[] {
				YnFlag.NO.getCodeInDb(), LoanState.ACTIVE.getCodeInDb(),
				YnFlag.NO.getCodeInDb(),buzDt, LoanVarDef.FEE_RESOURCE_CUSTER, buzDt};
		try {
			executeUpdateByHql(queryString.toString(), paramValues);
		} catch (RuntimeException e) {
			e.printStackTrace();
			throw new YcloansDBException("find error!", e);
		}
	}
	/**
	 * 查询指定机构的小于等于当前业务日期的没有结清的待收费分页对象
	 * 
	 * @param conn
	 * @param buzDt
	 * @param bchCdes
	 * @return
	 */
	public Pager findForFeeChgSts(Connection conn, String buzDt) {

		try {
			StringBuffer queryString = new StringBuffer();
			//modified by fanyl on 2015-12-01 for 优化
			queryString.append(" from LmLoan lm where  lm.loanSts =? and lm.prcsPageDtInd=? and lm.loanDebtSts in(?,?,?) ");
			queryString.append("and lm.loanNo in (select lh.loanNo from LmHoldFeeTx lh where lh.loanNo = lm.loanNo ");
			queryString.append("and (lh.accInd is null or lh.accInd = 'N')");
			queryString.append("and (lh.feeResource is null or lh.feeResource = 'CUSTER') ");
			queryString.append("AND (((lh.acctOffBsInd is null or lh.acctOffBsInd = 'N') ");
			queryString.append("and (lh.setlInd is null or lh.setlInd = 'N') AND lm.loanStpAccInd = 'Y' ) or ");
			queryString.append("(lh.acctOffBsInd = 'R' AND lm.loanStpAccInd = 'N')) )");
//			queryString.append(" (lh.accInd is null or lh.accInd='N') ");
//			queryString.append(" and (lh.feeResource is null or lh.feeResource='CUSTER') ");
//			//表内转表外
//			queryString.append(" and (((lh.acctOffBsInd is null or lh.acctOffBsInd='N') ");
//			queryString.append(" and (lh.setlInd is null or lh.setlInd='N')) ");
//			//表外转表内
//			queryString.append(" or (lh.acctOffBsInd='R'))) and lm.loanStpAccInd = ? ");
			/*//表内转表外
			queryString
			.append(" and ((exists (select lh.loanNo from LmHoldFeeTx lh ");
			queryString
			.append(" where lh.loanNo=lm.loanNo and lh.holdSetlDt<=? ");
			queryString
			.append(" and (lh.acctOffBsInd is null or lh.acctOffBsInd='N') and (lh.setlInd is null or lh.setlInd='N') ");
			queryString
			.append(" and (lh.accInd is null or lh.accInd='N'))) ");
			//表外转表内
			queryString
			.append(" or (exists (select lh.loanNo from LmHoldFeeTx lh where lh.loanNo=lm.loanNo ");
			queryString
			.append(" and lh.acctOffBsInd='R' and (lh.accInd is null or lh.accInd='N')))) ");*/
			if(ServiceContainer.getSystemParameter().isPayInDay()){//如果采用日间扣款模式 则会根据扣款返回状态来判断改日终任务是否执行
				queryString.append(" and (lm.atpySts != 'SK' or lm.atpySts is null)");
			}
			Serializable[] param = new Serializable[] { LoanState.ACTIVE.getCodeInDb(),
					YnFlag.NO.getCodeInDb(),
					LoanState.NORMAL.getCodeInDb(),
					LoanState.OVER.getCodeInDb(),
					LoanState.DELQ.getCodeInDb()};


			return new HibernatePager(this, queryString, param);
		} catch (RuntimeException re) {
			throw new YcloansDBException("find error!", re);
		}
		
	}
	
	/**
	 * 更新小于等于当前业务日期的没有结清的待收费分页对象的分页标志为N
	 * 
	 * @param conn
	 * @param buzDt
	 * @param bchCdes
	 * @return
	 */
	public void updateForFeeChgSts(Connection conn, String buzDt) {

		try {
			StringBuilder queryString = new StringBuilder();
			queryString
			.append(" update LmLoan lm set lm.prcsPageDtInd=? where ");
			queryString.append(" lm.loanSts = ? and lm.loanDebtSts in(?,?,?)  and lm.prcsPageDtInd <> 'N' ");
			//modified by fanyl on 2015-12-01 for 优化
			queryString.append("and lm.loanNo in (select lh.loanNo from LmHoldFeeTx lh where lh.loanNo = lm.loanNo and ");
			queryString.append(" (lh.accInd is null or lh.accInd = 'N')");
			queryString.append("and (lh.feeResource is null or lh.feeResource = 'CUSTER') ");
			queryString.append("AND (((lh.acctOffBsInd is null or lh.acctOffBsInd = 'N') ");
			queryString.append("and (lh.setlInd is null or lh.setlInd = 'N') AND lm.loanStpAccInd = 'Y' ) or ");
			queryString.append("(lh.acctOffBsInd = 'R' AND lm.loanStpAccInd = 'N')) )");
			//modified by fanyl on 2015-12-01 for 优化
//			queryString.append("and lm.loanNo in (select lh.loanNo from LmHoldFeeTx lh where lh.loanNo = lm.loanNo and ");
//			queryString.append(" (lh.accInd is null or lh.accInd='N') ");
//			queryString.append(" and (lh.feeResource is null or lh.feeResource='CUSTER') ");
//			//表内转表外
//			queryString.append(" and (((lh.acctOffBsInd is null or lh.acctOffBsInd='N') ");
//			queryString.append(" and (lh.setlInd is null or lh.setlInd='N')) ");
//			//表外转表内
//			queryString.append(" or (lh.acctOffBsInd='R')) and lm.loanStpAccInd = ?) ");
			/*//表内转表外
			queryString
			.append(" and (exists (select lh.loanNo from LmHoldFeeTx lh ");
			queryString
			.append("where lh.loanNo=lm.loanNo and lh.holdSetlDt<=? ");
			queryString
			.append(" and (lh.acctOffBsInd='N' or lh.acctOffBsInd is null) and (lh.setlInd is null or lh.setlInd='N') ");
			queryString
			.append(" and (lh.accInd is null or lh.accInd='N')) ");
			//表外转表内
			queryString
			.append(" or (exists (select lh.loanNo from LmHoldFeeTx lh where lh.loanNo=lm.loanNo ");
			queryString
			.append(" and lh.acctOffBsInd='R' and (lh.accInd is null or lh.accInd='N')))) ");*/
			if(ServiceContainer.getSystemParameter().isPayInDay()){//如果采用日间扣款模式 则会根据扣款返回状态来判断改日终任务是否执行
				queryString.append(" and (lm.atpySts != 'SK' or lm.atpySts is null)");
			}
			Serializable[] param = new Serializable[] { YnFlag.NO.getCodeInDb(),
					LoanState.ACTIVE.getCodeInDb(),
					LoanState.NORMAL.getCodeInDb(),
					LoanState.OVER.getCodeInDb(),
					LoanState.DELQ.getCodeInDb()};

			executeUpdateByHql(queryString.toString(), param);
		} catch (RuntimeException re) {
			throw new YcloansDBException("find error!", re);
		}
	}
	
	public BigDecimal[] findSumPrcpGroupByGrd(Connection conn,List<PLoanTypGlMap> PLTList) {
		String buzDate = SystemInfo.getSystemInfo().getBuzDate() ;
		StringBuffer sqls = new StringBuffer() ;
		sqls.append("  NOT EXISTS " ) ;
		sqls.append(" (SELECT 1 FROM LmGrdChgLog ll " ) ;
		sqls.append(" WHERE ll.chgDateFrom <= '").append(buzDate).append("'") ;
		sqls.append(" AND ll.chgDateEnd>= '").append(buzDate).append("'");
		sqls.append(" AND ll.suspCde IN ('D','A') " ) ;
		sqls.append(" AND ll.loanNo=lm.loanNo )" ) ;
		sqls.append(" AND " ) ;
		String sqlAppend = sqls.toString() ;
		BigDecimal[] sumPrcp = new BigDecimal[7];
		StringBuffer tempSql = new StringBuffer() ;

		 tempSql.append("and ( ") ;
		for(PLoanTypGlMap pl:PLTList) {
			tempSql.append(" lm.loanTyp = '").append(pl.getTypCde()).append("' or ");
		}
		tempSql.append(" 1=2 ) ");
		String sqltemp=tempSql.toString();
		/*String sql = "select loan1.grd1,loan2.grd2,loan3.grd3,loan4.grd4,loan5.grd5 from " +
				       "(select sum(LOAN_OS_PRCP) as grd1 from lm_loan where loan_grd = '' or loan_grd = '10') loan1," +
				       "(select sum(LOAN_OS_PRCP) as grd2 from lm_loan where loan_grd = '20') loan2," +
				       "(select sum(LOAN_OS_PRCP) as grd3 from lm_loan where loan_grd = '30') loan3," +
				       "(select sum(LOAN_OS_PRCP) as grd4 from lm_loan where loan_grd = '40') loan4," +
				       "(select sum(LOAN_OS_PRCP) as grd5 from lm_loan where loan_grd = '50') loan5;";
		List list = executeQuerySql(sql, new Serializable[]{});*/
//		String sql = "SELECT SUM(lm.LOAN_OS_PRCP) AMT FROM LM_LOAN lm WHERE "+ "( lm.LOAN_GRD IS NULL OR lm.LOAN_GRD = '10' ) AND lm.LOAN_STS != 'SETL'";
		String sql = "SELECT SUM(lm.loanOsPrcp)  FROM LmLoan lm WHERE " + sqlAppend +   "( lm.loanGrd IS NULL OR lm.loanGrd = '10' ) AND lm.loanSts != 'SETL' AND lm.loanSts !='PSETL' AND lm.loanDebtSts !='CHRGO' AND lm.loanSts != 'RESC' "+sqltemp;
		List list = super.executeQueryHql(sql, new Serializable[]{});
		if(!CollectionUtils.isEmpty(list)){
			if(list.get(0)!=null){
				sumPrcp[0] = (BigDecimal)list.get(0);
			}else{
				sumPrcp[0] = BigDecimal.ZERO;
			}
		}else{
			sumPrcp[0] = BigDecimal.ZERO;
		}
		sql = "SELECT SUM(lm.loanOsPrcp) FROM LmLoan lm WHERE " + sqlAppend + " lm.loanGrd = '20' AND lm.loanSts != 'SETL' AND lm.loanSts !='PSETL' AND lm.loanDebtSts !='CHRGO' AND lm.loanSts != 'RESC' "+sqltemp;
		list = super.executeQueryHql(sql, new Serializable[]{});
		if(!CollectionUtils.isEmpty(list)){
			if(list.get(0)!=null){
				sumPrcp[1] = (BigDecimal)list.get(0);
			}else{
				sumPrcp[1] = BigDecimal.ZERO;
			}
		}else{
			sumPrcp[1] = BigDecimal.ZERO;
		}
		sql = "SELECT SUM(lm.loanOsPrcp) FROM LmLoan lm WHERE " + sqlAppend + " lm.loanGrd = '30' AND lm.loanSts != 'SETL' AND lm.loanSts !='PSETL' AND lm.loanDebtSts !='CHRGO' AND lm.loanSts != 'RESC' "+sqltemp;
		list = super.executeQueryHql(sql, new Serializable[]{});
		if(!CollectionUtils.isEmpty(list)){
			if(list.get(0)!=null){
				sumPrcp[2] = (BigDecimal)list.get(0);
			}else{
				sumPrcp[2] = BigDecimal.ZERO;
			}
		}else{
			sumPrcp[2] = BigDecimal.ZERO;
		}
		sql = "SELECT SUM(lm.loanOsPrcp) FROM LmLoan lm WHERE " + sqlAppend + " lm.loanGrd = '40' AND lm.loanSts != 'SETL' AND lm.loanSts !='PSETL' AND lm.loanDebtSts !='CHRGO' AND lm.loanSts != 'RESC' "+sqltemp;
		list = super.executeQueryHql(sql, new Serializable[]{});
		if(!CollectionUtils.isEmpty(list)){
			if(list.get(0)!=null){
				sumPrcp[3] = (BigDecimal)list.get(0);
			}else{
				sumPrcp[3] = BigDecimal.ZERO;
			}
		}else{
			sumPrcp[3] = BigDecimal.ZERO;
		}
		sql = "SELECT SUM(lm.loanOsPrcp) FROM LmLoan lm WHERE " + sqlAppend + " lm.loanGrd = '50' AND lm.loanSts != 'SETL' AND lm.loanSts !='PSETL' AND lm.loanDebtSts !='CHRGO' AND lm.loanSts != 'RESC' "+sqltemp;
		list = super.executeQueryHql(sql, new Serializable[]{});
		if(!CollectionUtils.isEmpty(list)){
			if(list.get(0)!=null){
				sumPrcp[4] = (BigDecimal)list.get(0);
			}else{
				sumPrcp[4] = BigDecimal.ZERO;
			}
		}else{
			sumPrcp[4] = BigDecimal.ZERO;
		}
		sql = "SELECT SUM(lm.loanOsPrcp) FROM LmLoan lm WHERE " + sqlAppend + " lm.loanGrd = '60' AND lm.loanSts != 'SETL' AND lm.loanSts !='PSETL' AND lm.loanDebtSts !='CHRGO' AND lm.loanSts != 'RESC' "+sqltemp;
		list = super.executeQueryHql(sql, new Serializable[]{});
		if(!CollectionUtils.isEmpty(list)){
			if(list.get(0)!=null){
				sumPrcp[5] = (BigDecimal)list.get(0);
			}else{
				sumPrcp[5] = BigDecimal.ZERO;
			}
		}else{
			sumPrcp[5] = BigDecimal.ZERO;
		}
		sql = "SELECT SUM(lm.loanOsPrcp) FROM LmLoan lm WHERE " + sqlAppend + " lm.loanGrd = '70' AND lm.loanSts != 'SETL' AND lm.loanSts !='PSETL' AND lm.loanDebtSts !='CHRGO' AND lm.loanSts != 'RESC' "+sqltemp;
		list = super.executeQueryHql(sql, new Serializable[]{});
		if(!CollectionUtils.isEmpty(list)){
			if(list.get(0)!=null){
				sumPrcp[6] = (BigDecimal)list.get(0);
			}else{
				sumPrcp[6] = BigDecimal.ZERO;
			}
		}else{
			sumPrcp[6] = BigDecimal.ZERO;
		}
		return sumPrcp;
	}
	
	/**
	 * @desc 通过loanNo查询相应字段
	 * @param conn
	 * @param loanNo
	 * @return
	 */
	public LmLoanForPaym findForPaym(Connection conn, String loanNo) {
		StringBuffer queryString = new StringBuffer();
		queryString
				.append(" select new com.yuchengtech.ycloans.db.domain.LmLoanForPaym( ");
		queryString.append(" l.loanNo,l.loanTyp,l.lastDueDt,l.loanDevaInd,l.loanDebtSts,");
		queryString.append(" l.loanOdInd,l.loanStpAccInd,l.loanIntRate) ");
		queryString.append(" from ");
		queryString.append(" LmLoan l where l.loanNo= ? ");
		Object[] params = new Object[] { loanNo };
		List<LmLoanForPaym> lmLoanList = this.getHibernateTemplate().find(
				queryString.toString(), params);
		if (CollectionUtils.isEmpty(lmLoanList))
			return null;
		else
			return lmLoanList.get(0);
	}

	public int updateAtpyStsByLoanNo(Connection conn, String loanNo,
			String atpySts) {

		Serializable[] params = {atpySts, loanNo};
		StringBuffer hql = new StringBuffer(
				"update LmLoan lm set lm.atpySts = ? where lm.loanNo = ?");
		try {
			return super.executeUpdateByHql(hql.toString(), params);
		} catch (Exception e) {
			e.printStackTrace();
			throw new YcloansDBException("updatePrcsPageDtInd error!", e);
		}
	}

	public long getLoanAmount(Connection conn) {
		return 0;
	}
	
	/**
	 * 查找折扣到期的贷款
	 */
	public Pager findFreeIntLoans(Connection conn, String buzDate) {
		StringBuilder sql = new StringBuilder();
		sql.append(" select lm ");
		sql.append(" from LmLoan lm ");
		sql.append(" where ");
		sql.append(" loanSts=? ");
		sql.append(" and lm.intFreeDays > 0 ");
		sql.append(" and ");
		sql.append(DBSqlUtils.getDateIncDay("lm.intStartDt", "lm.intFreeDays", "+"));
		sql.append("= ? and lm.prcsPageDtInd='N' ");
		Serializable[] param = new Serializable[] {LoanState.ACTIVE.getCodeInDb(),buzDate};
		return new HibernatePager(this, sql.toString(), param);
	}
	
	/**
	 * 更新折扣到期的借据的分页标志为N
	 */
	public void updatePageFreeIntLoans(Connection conn, String buzDt) {

		StringBuffer queryString = new StringBuffer();
		queryString
				.append("update LmLoan ll set ll.prcsPageDtInd='N' where ll.loanSts=? ");
		queryString.append(" and (ll.prcsPageDtInd = 'Y' or ll.prcsPageDtInd is null) and ll.intFreeDays > 0 ");
		queryString.append(" and ");
		queryString.append(DBSqlUtils.getDateIncDay("ll.intStartDt", "ll.intFreeDays", "+"));
		queryString.append("= ?");
		Serializable[] paramValues = new Serializable[2];
		paramValues[0] = LoanState.ACTIVE.getCodeInDb();
		paramValues[1] = buzDt;

		try {
			executeUpdateByHql(queryString.toString(), paramValues);
		} catch (RuntimeException e) {
			e.printStackTrace();
			throw new YcloansDBException("update error!", e);
		}
	}
	
	/**
	 * 贷款业务
	 * 
	 * 减值计提单项减值模式的查询处理
	 * @param no
	 * @param yes
	 * @return
	 */
	public Pager findLoanBySingleDevi(Connection conn, String buzDate) {
		StringBuffer sqlbuff = new StringBuffer(30);
		sqlbuff.append(" from LmLoan lm  where lm.prcsPageDtInd = ? and lm.loanSts = ? ");
		sqlbuff.append(" and not exists (select 1 from LmGrdChgLog lg where lm.loanNo=lg.loanNo and (suspCde='A' or suspCde='D') ");
		sqlbuff.append(" and lg.chgDateFrom <= ? and lg.chgDateEnd >= ?) ");//排除表LM_GRD_CHG_LOG 中SUSP_CDE=A 或 D 且在调整有效期内的数据by weiwei
		Serializable[] params = new Serializable[4];
		params[0] = YnFlag.NO.getCodeInDb();
		params[1] = LoanState.ACTIVE.getCodeInDb();
		params[2] = buzDate;
		params[3] = buzDate;
		
		try {
			return new HibernatePager(this, sqlbuff, params);
		} catch (Exception e) {
			e.printStackTrace();
			throw new YcloansDBException("findLoanInfoByBchcde error!", e);
		}

	}
	/**
	 * 贷款业务
	 * 
	 * 减值计提单项减值模式的修改分页标志
	 * @param no
	 * @param yes
	 * @return
	 */
	public void updateLoanBySingleDevi(Connection conn) {
		StringBuffer sqlbuff = new StringBuffer(30);
		sqlbuff
				.append(" update LmLoan ll set ll.prcsPageDtInd='N' where ll.loanSts=? and ll.prcsPageDtInd <> 'N'");
		

		Serializable[] params = new Serializable[] { LoanState.ACTIVE.getCodeInDb()};
		try {
			executeUpdateByHql(sqlbuff.toString(), params);
		} catch (Exception e) {
			e.printStackTrace();
			throw new YcloansDBException("findLoanInfoByBchcde error!", e);
		}

	}
	
	/**
	 * 更新逾期90天后再整笔逾期借据的LOAN_DEBT_STS
	 */
	public void updateDebtSts(String buzDate) {
		try {
			StringBuilder queryString = new StringBuilder();
			queryString.append("update LmLoan l");
			queryString.append(" set l.loanDebtSts ='OVER' ");
			queryString.append(" ,l.lastChgDt=?");
			queryString.append(" where l.loanSts = 'ACTV' "); 
			queryString.append(" and l.loanDebtSts = 'DELQ' "); 
			queryString.append(" and l.loanStpAccInd ='Y' ");
			queryString.append(" and l.lastDueDt <= ? "); 
			executeUpdateByHql(queryString.toString(), new Serializable[] {buzDate,buzDate});
		} catch (RuntimeException e) {
			e.printStackTrace();
			throw new YcloansDBException("updateLoanOdInd error!", e);
		}
	}

	public List<LmGlTx> findList(Connection conn, Map param) {
		// TODO Auto-generated method stub
		StringBuffer sql = new StringBuffer();
		List paramList = new ArrayList();
		sql.append("SELECT A.*,B.LOAN_CONT_NO,B.ID_NO,B.ID_TYPE FROM  LM_LOAN_CONT B LEFT JOIN LM_LOAN  A ON B.LOAN_NO = A.LOAN_NO	WHERE 1=1 ");
		if(param.get("LOAN_CONT_NO") != null && !"".equals(param.get("LOAN_CONT_NO"))){
			sql.append("AND LOAN_CONT_NO = ?");
			paramList.add(param.get("LOAN_CONT_NO"));
		}
		if(param.get("LOAN_NO") != null && !"".equals(param.get("LOAN_NO"))){
			sql.append("AND LOAN_NO = ?");
			paramList.add(param.get("LOAN_NO"));
		}
		if(param.get("ID_NO") != null && !"".equals(param.get("ID_NO"))){
			sql.append("AND ID_NO = ?");
			paramList.add(param.get("ID_NO"));
		}
		if(param.get("CUST_NAME") != null && !"".equals(param.get("CUST_NAME"))){
			sql.append("AND CUST_NAME like ?");
			paramList.add(param.get("CUST_NAME"));
		}
		if(param.get("LOAN_ACTV_DT") != null && !"".equals(param.get("LOAN_ACTV_DT"))){
			sql.append("AND LOAN_ACTV_DT = ?");
			paramList.add(param.get("LOAN_ACTV_DT"));
		}
		sql.append(" ORDER BY TX_NO, SEQ_NO");
		List<LmGlTx> list = this.getHibernateTemplate().find(sql.toString(), paramList.toArray());
		return list;	
		}

	public long findListSiz(Connection conn, Map param) {
		// TODO Auto-generated method stub
		return 0;
	}

	public List<LmGlnoLog> findLogNo(Connection conn, LmGlnoLog lmGlnoLog) {
		// TODO Auto-generated method stub
		return null;
	}

	public List<LmGlnoLog> findGlLog(Connection conn, LmGlnoLog lmGlnoLog) {
		// TODO Auto-generated method stub
		return null;
	}

	public List<LmGlTx> findList1(Connection conn, Map param) {
		// TODO Auto-generated method stub
		return null;
	}
	
	/**
	 * 放款撤销修改借据状态
	 * @author liumq
	 */
	public void updateUnDo(Connection conn,String loanNo){
		
		try{
			StringBuffer queryString = new StringBuffer();
			queryString.append("update LmLoan l");
			queryString.append(" set l.loanDebtSts =? ");
			queryString.append(" ,l.loanSts=?");
			queryString.append(" ,l.loanOsPrcp = 0 ");
			queryString.append(" where l.loanNo = ? "); 
			Serializable[] param = new Serializable[] {LoanState.UNDO.getCodeInDb(),LoanVarDef.LOAN_STS_RESC,loanNo};
			
			executeUpdateByHql(queryString.toString(), param);
		} catch (RuntimeException re) {
			throw new YcloansDBException("find error!", re);
		}
	}

	public LmGlnoLog findmaxTxLogSeqByLoanNo(Connection conn, String loanNo) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * 根据扣款日和借据状态更新借据的当前应扣款日
	 */
	public void updateLmLoanByPsDueDtandLoanNextDueDt(Connection conn, String psDueDt) {
		StringBuilder sql = new StringBuilder();
		sql.append(" update LmLoan l set l.nextDueDt = ");
		sql
				.append(" (select min(lp.psDueDt) from LmPmShd lp where lp.id.loanNo = l.loanNo  and lp.id.psPerdNo>0 and setlInd = ?) ");
		sql.append(" where l.loanSts=? and l.loanDebtSts = ?");
		
		try {
			Serializable[] params = new Serializable[3];
			params[0] = YnFlag.NO.getCodeInDb();
			params[1] = LoanState.ACTIVE.getCodeInDb();
			params[2] = LoanState.NORMAL.getCodeInDb();
			super.executeUpdateByHql(sql.toString(), params);

		} catch (RuntimeException re) {
			throw new YcloansDBException(
					"updateLmLoanByPsDueDtandLoanSts error");
		}
	}
	/**
	 *  查询需要自动代偿数据
	* @param conn
	* @param bjc
	* @return
	*/
	public Pager searchPaymCooprComp(Connection conn, String sqls, String buzDate, String dealerCde) {
		StringBuffer sql = new StringBuffer(30);
		sql.append("select l");
		sql.append(" from LmLoan l,LmLoanCont lc  ");
		sql.append(" where l.loanNo=lc.loanNo");
		sql.append(" and l.loanSts=? and l.loanDebtSts in (?,?,?)");
		sql.append(" and (");
		sql.append(" 	(l.loanDevaInd='Y' and l.loanDebtSts='NORM' )");
		sql.append(" 	or (l.nextDueDt<=? or l.nextDueDt is null)");
		sql.append(" )");
		sql.append(" and lc.dealerCde = ?");
		sql.append(sqls);
		sql.append(" and l.prcsPageDtInd=?");
		sql.append(" and not exists");
		sql.append(" ( select 1 from LmAtpyDetl lp where lp.loanNo = l.loanNo and lp.atpySts in (?, ?))");
		Serializable[] params = new Serializable[] {
			LoanState.ACTIVE.getCodeInDb(), LoanState.NORMAL.getCodeInDb(),
			LoanState.DELQ.getCodeInDb(), LoanState.OVER.getCodeInDb(), 
			buzDate, dealerCde,  YnFlag.NO.getCodeInDb(), ATPYState.SU.getCodeInDb(), ATPYState.FREEZE.getCodeInDb()
		};
		return new HibernatePager(this, sql, params);
	}
	
	/**
	 * 更新需要自动代偿数据
	* @param conn
	* @param bjc
	* @return
	*/
	public int updatePaymCooprComp(Connection conn, String sqls, String buzDate) {
		StringBuffer sql = new StringBuffer(30);
		sql.append("update ");
		sql.append(" LmLoan l set prcsPageDtInd = ?");
		sql.append(" where ");
		sql.append(" l.loanSts=? and l.loanDebtSts in (?,?,?)");
		sql.append(" and (");
		sql.append(" 	(l.loanDevaInd='Y' and l.loanDebtSts='NORM' )");
		sql.append(" 	or (l.nextDueDt<=? or l.nextDueDt is null)");
		sql.append(" )");
		sql.append(sqls);
		sql.append(" and l.prcsPageDtInd <> ?");
		sql.append(" and not exists");
		sql.append(" ( select 1 from LmAtpyDetl lp where lp.loanNo = l.loanNo and lp.atpySts in (?, ?))");
		Serializable[] params = new Serializable[] { YnFlag.NO.getCodeInDb(),
			LoanState.ACTIVE.getCodeInDb(), LoanState.NORMAL.getCodeInDb(),
			LoanState.DELQ.getCodeInDb(), LoanState.OVER.getCodeInDb(),
			buzDate, YnFlag.NO.getCodeInDb(), ATPYState.SU.getCodeInDb(), ATPYState.FREEZE.getCodeInDb()
		};
		return executeUpdateByHql(sql.toString(), params);
	}

	@SuppressWarnings("unchecked")
	public List<LoanAfterRecord> findPage(Connection conn, String strDt,
			String endDt, String targetPage, String recordSize, String custId,
			String bchCde,String setlInd ) {
		try {
			Serializable[] objs = new Serializable[]{};
			List<String> list = new ArrayList<String>();	
			StringBuffer stringBuffer = new StringBuffer();
			stringBuffer.append(" select new com.yuchengtech.ycloans.db.domain.LoanAfterRecord ");
			stringBuffer.append(" (l.loan_No as loanNo , c.LOAN_CONT_NO as loanContNo, l.cust_Id as custId, l.cust_name as custName, c.id_type as idType,");
			stringBuffer.append(" l.loan_actv_dt as loanActvDt, l.orig_prcp as origPrcp, l.tnr as tnr, c.loan_hted_ind as loanHtedInd, l.loan_os_prcp as loanOsPrcp");
			stringBuffer.append(" from lmLoan l, lmLoanCont c ");
			stringBuffer.append(" where l.loanNo = c.loanNo");
			stringBuffer.append(" and l.loanActvDt >= ? and l.loanActvDt < ? ");
			list.add(strDt);
			list.add(endDt);
			if(!StringUtil.isNullEmpty(custId)){
				stringBuffer.append(" and l.custId = ?");
				list.add(custId);
			}	
			 if(!StringUtil.isNullEmpty(bchCde)){
				stringBuffer.append(" and l.bchCde = ?");
				list.add(bchCde);
			}
			 if(!StringUtil.isNullEmpty(setlInd)){
					if("Y".equals(setlInd) || "y".equals(setlInd)){
						stringBuffer.append(" and l.loan_sts = 'SETL'");
					}else if("N".equals(setlInd) || "n".equals(setlInd)){
						stringBuffer.append(" and l.loan_sts != 'SETL'");
					}		
				}
			stringBuffer.append(" order by l.loanNo");
			for (int i = 0; i < list.size(); i++) {
				objs[i] = list.get(i);
			}
			return (List<LoanAfterRecord>) new HibernatePager(this, stringBuffer.toString(),objs).findPage(Integer.parseInt(targetPage), Integer.parseInt(recordSize));
		} catch (RuntimeException re) {
			throw new YcloansDBException(
					"findPage  failed");
		}
}
	public Double find(Connection conn, LmLoan lmLoan,
			LmLoanCont lmLoanCont,String htedInd) {
		List<String> list = new ArrayList<String>();
		StringBuffer sql = new StringBuffer();
		try{
		sql.append("select sum(l.loanOsPrcp) from LmLoan l, LmLoanCont c where l.loanNo = c.loanNo");
		
		if(!StringUtil.isNullEmpty(lmLoanCont.getIdNo()) && !StringUtil.isNullEmpty(lmLoanCont.getIdType()) && !StringUtil.isNullEmpty(lmLoanCont.getIssCtry())){
			sql.append(" and c.idNo = ? and c.idType = ? and c.issCtry = ? ");
			list.add(lmLoanCont.getIdNo());
			list.add(lmLoanCont.getIdType());
			list.add(lmLoanCont.getIssCtry());
			
		}
		if(!StringUtil.isNullEmpty(lmLoan.getLoanNo())){
			sql.append(" and l.loanNo = ? ");
			list.add(lmLoan.getLoanNo());
		}
		if(!StringUtil.isNullEmpty(lmLoanCont.getLoanContNo())){
			sql.append(" and c.loanContNo = ? ");
			list.add(lmLoanCont.getLoanContNo());
		}
		if(!StringUtil.isNullEmpty(lmLoan.getLoanTyp())){
			sql.append("and l.loanTyp = ?");
            list.add(lmLoan.getLoanTyp());
		}
		if (!StringUtil.isNullEmpty(lmLoanCont.getDealerCde())) {
			sql.append("and c.dealerCde = ? ");
			list.add(lmLoanCont.getDealerCde());
		}
		if(!StringUtil.isNullEmpty(lmLoan.getBchCde())){
			sql.append("and l.bchCde = ?");
			list.add(lmLoan.getBchCde());
		}
		if (!StringUtil.isNullEmpty(lmLoan.getBchCde())) {
			sql.append(" and l.bch_cde = ? ");
			list.add(lmLoan.getBchCde());
		}
		if("y".equals(htedInd) || "Y".equals(htedInd)){
			sql.append("and c.loanHtedInd = 'y'");		
		}
		Serializable[] params = new Serializable[list.size()];
		for (int i = 0; i < list.size(); i++) {
			params[i]=list.get(i);
		}
		Object result = queryFirstColumnByHql(conn, sql.toString(), params);
		if (result == null) {
			return 0.00;
		} else {
			return Double.valueOf(result.toString());
		}
		}catch(RuntimeException re){
			throw new YcloansDBException("find error",re);
			
		}
	}
	
	public int findCount(Connection conn, String strDt, String endDt,
			 String custId, String bchCde ,String setlInd) {
		try {
			List<String> list = new ArrayList<String>();	
			StringBuffer sql = new StringBuffer();
			sql.append("select count(*) ");	
			sql.append(" from lm_loan l, lm_loan_cont c  where l.loan_no = c.loan_no");
			sql.append(" and l.loan_actv_dt >= ? and l.loan_actv_dt < ? ");
			list.add(strDt);
			list.add(endDt);
			if(!StringUtil.isNullEmpty(custId)){
				sql.append(" and l.cust_id = ?");
				list.add(custId);
			}
			if(!StringUtil.isNullEmpty(bchCde)){
				sql.append(" and l.bch_cde = ?");
				list.add(bchCde);
			}
			if(!StringUtil.isNullEmpty(setlInd)){
				if("Y".equals(setlInd) || "y".equals(setlInd)){
					sql.append(" and l.loan_sts = 'SETL'");
				}else if("N".equals(setlInd) || "n".equals(setlInd)){
					sql.append(" and l.loan_sts != 'SETL'");
				}		
			}
			Serializable[] params = new Serializable[] {};
			for(int i = 0; i < list.size(); i++){
				params[i] = list.get(i);
			}
			Object result = queryFirstColumnByHql(conn, sql.toString(), params);
			if (result == null) {
				return 0;
			} else {
				return Integer.parseInt(result.toString());
			}
		} catch (RuntimeException re) {
			throw new YcloansDBException(
					"findPage  failed");
		}
		
	}


	public List<String> findGrdMtdCde(Connection conn, String loanSts,String threadCntStr) {
		try {
			//账务状态-核销
			String chrgoSts = "CHRGO";
			StringBuffer queryString = new StringBuffer();
			queryString
					.append("select ll.grdMtdCde from LmLoan ll where  ll.loanSts = ?  and ll.loanDebtSts <> ? ");
			if (StringUtils.hasText(threadCntStr)) {
				queryString.append(" and ll.thdCnt in(").append(threadCntStr)
						.append(") ");
			}
			queryString.append("group by ll.grdMtdCde");
			Serializable[] params = new Serializable[2];
			params[0] = loanSts;
			params[1] = chrgoSts;
			return (List<String>)this.getHibernateTemplate().find(queryString.toString(), params);
		} catch (Exception e) {
			e.printStackTrace();
			throw new YcloansDBException("findLmloans error!");
		}
	}
	
	public void updateByLmLoanPrcsPageDtInd(Connection conn, String prcsPageDtInd, String loanNo){
		StringBuffer hql = new StringBuffer(
				"update LmLoan lm set lm.prcsPageDtInd = ?, upVer = upVer+1 where loanNo = ? ");
		Serializable[] params = new Serializable[] { prcsPageDtInd, loanNo };
		super.executeUpdateByHql(hql.toString(), params);
	}

	public void updateLastIntAccDtByLmLoan(Connection conn, String loanNo,
			String buzDueDt) {
		StringBuilder sql = new StringBuilder();
		sql.append(" update LmLoan l set l.lastIntAccDt = ? , upVer = upVer+1");
		sql.append(" where l.id.loanNo = ?");
		
		try {
			Serializable[] params = new Serializable[2];
			params[0] = buzDueDt;
			params[1] = loanNo;
			super.executeUpdateByHql(sql.toString(), params);

		} catch (RuntimeException re) {
			throw new YcloansDBException(
					"updateLastIntAccDtByLmLoan error");
		}
	}
	
	public BigDecimal findLoanOsPrcpByCustId(Connection conn,
			String custId) {
		StringBuffer sql = new StringBuffer();
		try{
			sql.append("select sum(l.loanOsPrcp) from LmLoan l, LmLoanCont c where l.loanNo = c.loanNo");
			sql.append(" and c.custId = ? ");
			Serializable[] params = new Serializable[]{custId};
			Object result = queryFirstColumnByHql(conn, sql.toString(), params);
			if (result == null) {
				return BigDecimal.ZERO;
			} else {
				return new BigDecimal(result.toString());
			}
		}catch(RuntimeException re){
			throw new YcloansDBException("find error",re);
			
		}
	}
	/**
	 * 贷款业务
	 * 
	 * 贴息收取日期结贴息
	 * @param no
	 * @param yes
	 * @return
	 */
	public Pager findLoanBySbsyPager(Connection conn, String buzDt) {
		StringBuffer sqlbuff = new StringBuffer(30);
		sqlbuff.append(" from LmLoan lm where lm.prcsPageDtInd = ? and lm.loanSts = ?");
		sqlbuff.append(" and exists (select ls.id.loanNo from LmSbsyShd ls where ls.id.loanNo=lm.loanNo ");
		sqlbuff.append(" and (ls.setlInd is null or ls.setlInd = 'N')");
		sqlbuff.append(" and ls.psDueDt  = ? and (ls.lastSetlDt is null or ls.lastSetlDt < ?))");

		Serializable[] params = new Serializable[] { 
					YnFlag.NO.getCodeInDb(),
					LoanState.ACTIVE.getCodeInDb(),
					buzDt,
					buzDt };
		try {
			return new HibernatePager(this, sqlbuff, params);
		} catch (Exception e) {
			e.printStackTrace();
			throw new YcloansDBException("findLoanInfoBySbsy error!", e);
		}

	}
	
	/**
	 * 贴息收取日期结贴息批量更新分页标志
	 */
	public void updateForFindSbsy(Connection conn,String buzDt) {
		StringBuffer queryString = new StringBuffer();
		queryString
				.append("update LmLoan ll set ll.prcsPageDtInd='N' where ll.loanSts = ?  and ll.prcsPageDtInd <> 'N' ");
		queryString
				.append(" and exists (select ls.id.loanNo from LmSbsyShd ls where ls.id.loanNo=ll.loanNo ");
		queryString
				.append(" and (ls.setlInd is null or ls.setlInd = 'N') and ls.psDueDt = ? ");
		queryString.append(" and (ls.lastSetlDt is null or ls.lastSetlDt < ?))");

		Serializable[] paramValues = new Serializable[] { 
				LoanState.ACTIVE.getCodeInDb(),
				buzDt,
				buzDt };

		
		try {
			executeUpdateByHql(queryString.toString(), paramValues);		
		} catch (RuntimeException e) {
			e.printStackTrace();
			throw new YcloansDBException("update error!", e);
		}
	}

	/* (non-Javadoc)
	 * @see com.yuchengtech.ycloans.db.dao.LmLoanDAO#findSBSYListByLoanNo(java.util.Map, java.sql.Connection, int, int)
	 */
	public List<Map<String, Serializable>> findSBSYListByLoanNo(Map<String, String> map, Connection conn, int start, int pageSize) {
		// TODO Auto-generated method stub
		return null;
	}
	public int findSBSYListByLoanNoSum(Map<String, String> map, Connection conn) {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see com.yuchengtech.ycloans.db.dao.LmLoanDAO#findSBSYDetailListByLoanNo(java.util.Map, java.sql.Connection, int, int)
	 */
	public List<Map<String, Serializable>> findSBSYDetailListByLoanNo(Map<String, String> map, Connection conn, int start, int pageSize) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.yuchengtech.ycloans.db.dao.LmLoanDAO#findNotSBSYList(java.util.Map, java.sql.Connection, int, int)
	 */
	public List<Map<String, Serializable>> findNotSBSYList(Map<String, String> map, Connection conn, int start, int pageSize) {
		// TODO Auto-generated method stub
		return null;
	}
	public int findNotSBSYListSum(Map<String, String> map, Connection conn) {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see com.yuchengtech.ycloans.db.dao.LmLoanDAO#findSBSYListBYVehChassis(java.util.Map, java.sql.Connection, int, int)
	 */
	public List<Map<String, Serializable>> findSBSYListBYVehChassis(Map<String, String> map, Connection conn, int start, int pageSize) {
		// TODO Auto-generated method stub
		return null;
	}
	public Pager findByForSbsyIntAcc(Connection conn, String bankCde,
			StringBuffer bchCdes, String buzDate, String threadCntStr) {
		try {
			StringBuilder queryString = new StringBuilder();
			queryString.append("from LmLoan ll where ");

			if (SystemInfo.getSystemInfo().getSystemParameters()
					.getAccountRule().isSuspAcc()) {
				queryString.append(" ll.loanSts=?  ");
			} else {
				queryString.append(" ll.loanSts=? and ll.loanStpAccInd='N' ");
			}
			queryString.append(" and ll.prcsPageDtInd='N' ");
			queryString.append(" and exists (select ls.id.loanNo from LmLnSbsyMtd ls where ls.id.loanNo = ll.loanNo and ls.sbsyEndDt > ?) ");
			if (!SystemInfo.getSystemInfo().getAccountRule().isDevLoanAcc()) {
				queryString.append(" and ll.loanDevaInd='N' ");
			}
			if (StringUtils.hasText(threadCntStr)) {
				queryString.append(" and ll.thdCnt in(").append(threadCntStr)
						.append(") ");
			}
			//modified by fanyl on 2015-10-09 for 核销贷款是否计提
			Serializable[] param = null;
			if(SystemInfo.getSystemInfo().getAccountRule().isChrgoAcc()) {
				queryString.append(" and ll.loanDebtSts in (?,?,?)  ");
				param = new Serializable[] {						
						AccountState.GIVED.getCodeInDb(), buzDate,
						LoanState.NORMAL.getCodeInDb(),
						LoanState.DELQ.getCodeInDb(),LoanState.OFFED.getCodeInDb()};
			} else {
				queryString.append(" and ll.loanDebtSts in (?,?)  ");
				param = new Serializable[] {
						AccountState.GIVED.getCodeInDb(), buzDate,
						LoanState.NORMAL.getCodeInDb(),
						LoanState.DELQ.getCodeInDb()};

			}

			return new HibernatePager(DAOContainer.getCommonDbDAO(false),
					queryString.toString(), param);
		} catch (RuntimeException e) {
			e.printStackTrace();
			throw new YcloansDBException("find error!", e);
		}
	}

	public void updateForSbsyIntAcc(Connection conn, String bankCde,
			StringBuffer bchCdes, String buzDate) {
		try {
			Session session = super.getCurrentHibernateSession();
			StringBuffer queryString = new StringBuffer();
			if (SystemInfo.getSystemInfo().getSystemParameters()
					.getAccountRule().isSuspAcc()) {
				queryString
						.append("update LmLoan ll set ll.prcsPageDtInd='N' where ll.loanSts=?  ");
			} else {
				// queryString
				// .append("update LmLoan ll set ll.prcsPageDtInd='N' where ll.loanSts=? and ll.loanStpAccInd='N' and ll.loanIntRate>0 ");
				// haixia会出现调整后利率为0的情况
				queryString
						.append("update LmLoan ll set ll.prcsPageDtInd='N' where ll.loanSts=? and ll.loanStpAccInd='N' ");
			}
			queryString.append(" and ll.prcsPageDtInd <> 'N' ");
			queryString.append(" and exists (select ls.id.loanNo from LmLnSbsyMtd ls where ls.id.loanNo = ll.loanNo and ls.sbsyEndDt > ?) ");
			if (!SystemInfo.getSystemInfo().getAccountRule().isDevLoanAcc()) {
				queryString.append(" and ll.loanDevaInd='N' ");
			}
			//modified by fanyl on 2015-10-09 for 核销贷款是否计提
			Query q = null;
			if(SystemInfo.getSystemInfo().getAccountRule().isChrgoAcc()) {
				queryString.append(" and ll.loanDebtSts in (?,?,?)  ");
				q = session.createQuery(queryString.toString());
				q.setParameter(0, AccountState.GIVED.getCodeInDb());
				q.setParameter(1, buzDate);
				q.setParameter(2, LoanState.NORMAL.getCodeInDb());
				q.setParameter(3, LoanState.DELQ.getCodeInDb());
				q.setParameter(4, LoanState.OFFED.getCodeInDb());

			} else {
				queryString.append(" and ll.loanDebtSts in (?,?)  ");
				q = session.createQuery(queryString.toString());
				q.setParameter(0, AccountState.GIVED.getCodeInDb());
				q.setParameter(1, buzDate);
				q.setParameter(2, LoanState.NORMAL.getCodeInDb());
				q.setParameter(3, LoanState.DELQ.getCodeInDb());
			}
			q.executeUpdate();
		} catch (RuntimeException e) {
			e.printStackTrace();
			throw new YcloansDBException("find error!", e);
		}
	}

	public void updateForSbsyIntAcc(Connection conn, String bankCde,
			StringBuffer bchCdes, String buzDate, String thdCntStr) {
		try {
			Session session = super.getCurrentHibernateSession();
			StringBuffer queryString = new StringBuffer();
			if (SystemInfo.getSystemInfo().getSystemParameters()
					.getAccountRule().isSuspAcc()) {
				queryString
						.append("update LmLoan ll set ll.prcsPageDtInd='N' where ll.loanSts=?  ");
			} else {
				queryString
						.append("update LmLoan ll set ll.prcsPageDtInd='N' where ll.loanSts=?  and ll.loanStpAccInd='N' ");
			}
			if (!SystemInfo.getSystemInfo().getAccountRule().isDevLoanAcc()) {
				queryString.append(" and ll.loanDevaInd='N' ");
			}
			queryString.append(" and ll.prcsPageDtInd <> 'N' ");
			queryString.append(" and exists (select ls.id.loanNo from LmLnSbsyMtd ls where ls.id.loanNo = ll.loanNo and ls.sbsyEndDt > ?) ");
			queryString.append(" and ll.thdCnt in(").append(thdCntStr).append(
					")");
			//modified by fanyl on 2015-10-09 for 核销贷款是否计提
			Query q = null;
			if(SystemInfo.getSystemInfo().getAccountRule().isChrgoAcc()) {
				queryString.append(" and ll.loanDebtSts in (?,?,?)  ");
				q = session.createQuery(queryString.toString());
				q.setParameter(0, AccountState.GIVED.getCodeInDb());
				q.setParameter(1, buzDate);
				q.setParameter(2, LoanState.NORMAL.getCodeInDb());
				q.setParameter(3, LoanState.DELQ.getCodeInDb());
				q.setParameter(4, LoanState.OFFED.getCodeInDb());

			} else {
				queryString.append(" and ll.loanDebtSts in (?,?)  ");
				q = session.createQuery(queryString.toString());
				q.setParameter(0, AccountState.GIVED.getCodeInDb());
				q.setParameter(1, buzDate);
				q.setParameter(2, LoanState.NORMAL.getCodeInDb());
				q.setParameter(3, LoanState.DELQ.getCodeInDb());
			}

			q.executeUpdate();
		} catch (RuntimeException e) {
			e.printStackTrace();
			throw new YcloansDBException("find error!", e);
		}
	}

	@Override
	public Pager findSbsyIntLoans(Connection conn, String buzDate) {
		StringBuilder sql = new StringBuilder();
		sql.append(" select lm ");
		sql.append(" from LmLoan lm ");
		sql.append(" where  loanSts=? and lm.prcsPageDtInd='N' ");
		sql.append(" and exists (select ls.id.loanNo from LmLnSbsyMtd ls where ls.id.loanNo = lm.loanNo and ls.sbsyEndDt = ?) ");
		Serializable[] param = new Serializable[] {LoanState.ACTIVE.getCodeInDb(),buzDate};
		return new HibernatePager(this, sql.toString(), param);
	}

	@Override
	public void updatePageSbsyIntLoans(Connection conn, String buzDt) {
		StringBuffer queryString = new StringBuffer();
		queryString
				.append("update LmLoan ll set ll.prcsPageDtInd='N' where ll.loanSts=? ");
		queryString.append(" and (ll.prcsPageDtInd = 'Y' or ll.prcsPageDtInd is null) ");
		queryString.append(" and exists (select ls.id.loanNo from LmLnSbsyMtd ls where ls.id.loanNo = ll.loanNo and ls.sbsyEndDt = ?) ");
		Serializable[] paramValues = new Serializable[2];
		paramValues[0] = LoanState.ACTIVE.getCodeInDb();
		paramValues[1] = buzDt;

		try {
			executeUpdateByHql(queryString.toString(), paramValues);
		} catch (RuntimeException e) {
			e.printStackTrace();
			throw new YcloansDBException("update error!", e);
		}
	}

	public int updatePageFlagRepayDataByStateKr(Connection conn, String buzDate, String branchCode, String bankCde,
			String night, String thdCntStr) {
		StringBuffer sql = new StringBuffer(30);
		sql.append("update LmLoan a set a.prcsPageDtInd='N' ");
		sql.append(" where a.nextDueDt >? ");
		sql.append(" and exists (select b.id.loanNo from LmProcShd b ");
		sql.append(" where b.id.loanNo = a.loanNo and b.psProcAmt > b.setlProcAmt and b.psDueDt <=? ) ");
		sql.append(" and a.loanNo not in ( ");
		sql.append(" select c.loanNo from LmSetlmtT c ");
		sql.append(" where c.setlValDt =? and c.isAp = 'Y' and c.genGlInd = ?) ");
		sql.append(" and a.loanDebtSts = 'NORM' ");
		sql.append(" and a.loanSts = 'ACTV' ");
		sql.append(" and a.thdCnt in ("+thdCntStr+") ");
		sql.append(" and (a.prcsPageDtInd <> 'N' or a.prcsPageDtInd is null) ");
		sql.append(" order by a.loanNo ");
		Serializable[] params = new Serializable[] {
				buzDate,
				buzDate, 
				buzDate,
				GenGlInd.UN_PROCESSED.getCodeInDb()};
		return executeUpdateByHql(sql.toString(), params);
	}

	@Override
	public void UpdateLoanGrd(Connection conn) {
		StringBuffer sql = new StringBuffer();
		Session session=getHibernateTemplate().getSessionFactory().openSession();
		Query q = null;
		sql.append( "update lm_loan l set loan_grd =(select acc.seven_cla as loan_grd from acc_loan acc  where acc.seven_cla is not null and l.loan_no = bill_no )");
	    sql.append("where loan_no in( ");
	    sql.append("    select acc.bill_no as loan_no");
	    sql.append("    from acc_loan acc ");
	    sql.append("    where acc.seven_cla is not null ) ");
		q = session.createSQLQuery(sql.toString());
		q.executeUpdate();
		session.close();
	}

	public List<LmLoan> findByCustId(Connection conn, String custId) {
		// TODO Auto-generated method stub
		return null;
	}
	
	/**
	 * 查询指定机构的小于等于当前业务日期的没有结清的贴息分页对象
	 * 
	 * @param conn
	 * @param buzDt
	 * @param bchCdes
	 * @return
	 */
	public Pager findForSbsyChgSts(Connection conn, String buzDt) {

		try {
			StringBuffer queryString = new StringBuffer();
			//modified by fanyl on 2015-12-01 for 优化
			queryString.append(" from LmLoan lm where  lm.loanSts =? and lm.prcsPageDtInd=? and lm.loanDebtSts in(?,?,?) ");
			queryString.append("and lm.loanNo in (select lh.loanNo from LmSbsyShd lh where lh.id.loanNo = lm.loanNo ");
			queryString.append("and (((lh.acctOffBsInd is null or lh.acctOffBsInd = 'N') ");
			queryString.append("and (lh.setlInd is null or lh.setlInd = 'N') AND lm.loanStpAccInd = 'Y' ) or ");
			queryString.append("(lh.acctOffBsInd = 'R' AND lm.loanStpAccInd = 'N')) )");
			if(ServiceContainer.getSystemParameter().isPayInDay()){//如果采用日间扣款模式 则会根据扣款返回状态来判断改日终任务是否执行
				queryString.append(" and (lm.atpySts != 'SK' or lm.atpySts is null)");
			}
			Serializable[] param = new Serializable[] { LoanState.ACTIVE.getCodeInDb(),
					YnFlag.NO.getCodeInDb(),
					LoanState.NORMAL.getCodeInDb(),
					LoanState.OVER.getCodeInDb(),
					LoanState.DELQ.getCodeInDb()};


			return new HibernatePager(this, queryString, param);
		} catch (RuntimeException re) {
			throw new YcloansDBException("find error!", re);
		}
		
	}
	
	/**
	 * 更新小于等于当前业务日期的没有结清的贴息分页对象的分页标志为N
	 * 
	 * @param conn
	 * @param buzDt
	 * @param bchCdes
	 * @return
	 */
	public void updateForSbsyChgSts(Connection conn, String buzDt) {

		try {
			StringBuilder queryString = new StringBuilder();
			queryString
			.append(" update LmLoan lm set lm.prcsPageDtInd=? where ");
			queryString.append(" lm.loanSts = ? and lm.loanDebtSts in(?,?,?)  and lm.prcsPageDtInd <> 'N' ");
			//modified by fanyl on 2015-12-01 for 优化
			queryString.append("and lm.loanNo in (select lh.id.loanNo from lmSbsyShd lh where lh.id.loanNo = lm.loanNo ");			
			queryString.append("AND (((lh.acctOffBsInd is null or lh.acctOffBsInd = 'N') ");
			queryString.append("and (lh.setlInd is null or lh.setlInd = 'N') AND lm.loanStpAccInd = 'Y' ) or ");
			queryString.append("(lh.acctOffBsInd = 'R' AND lm.loanStpAccInd = 'N')) )");
			if(ServiceContainer.getSystemParameter().isPayInDay()){//如果采用日间扣款模式 则会根据扣款返回状态来判断改日终任务是否执行
				queryString.append(" and (lm.atpySts != 'SK' or lm.atpySts is null)");
			}
			Serializable[] param = new Serializable[] { YnFlag.NO.getCodeInDb(),
					LoanState.ACTIVE.getCodeInDb(),
					LoanState.NORMAL.getCodeInDb(),
					LoanState.OVER.getCodeInDb(),
					LoanState.DELQ.getCodeInDb()};

			executeUpdateByHql(queryString.toString(), param);
		} catch (RuntimeException re) {
			throw new YcloansDBException("find error!", re);
		}
	}
	
	/**
	 * 查找利息计提数据
	 * 
	 * @param conn
	 * @param buzDate
	 * @param threadCntStr
	 * @return
	 */
	public Pager findByForCore(Connection conn, String buzDate, String threadCntStr) {
		try {
			StringBuilder queryString = new StringBuilder();
			queryString.append(" SELECT ll,LC from LmLoan ll,LmLoanCont LC where ");
			queryString.append("  ll.loanNo = LC.loanNo and ");
			queryString.append(" ( ll.loanSts <> ? or (ll.loanSts = ? and ll.lastSetlDt = ? ))");
			queryString.append(" and ll.prcsPageDtInd='N' ");
			if (StringUtils.hasText(threadCntStr)) {
				queryString.append(" and ll.thdCnt in(").append(threadCntStr).append(") ");
			}
			queryString.append(" order by ll.loanNo");
			Serializable[] param = new Serializable[] {
					AccountState.SETTLED.getCodeInDb(), AccountState.SETTLED.getCodeInDb(), buzDate};
			return new HibernatePager(DAOContainer.getCommonDbDAO(false),
					queryString.toString(), param);
		} catch (RuntimeException e) {
			e.printStackTrace();
			throw new YcloansDBException("find error!", e);
		}
	}
	
	/**
	 * 发送核心分录借据 批量修改待处理的借据信息状态
	 */
	public synchronized void updateForCore(Connection conn,String buzDate,
			String threadCntStr) {
		try {
			Session session = super.getCurrentHibernateSession();
			StringBuffer queryString = new StringBuffer();
			queryString.append("update LmLoan ll set ll.prcsPageDtInd='N' where  ");
			queryString.append(" ( ll.loanSts <> ?  or (ll.loanSts = ? and ll.lastSetlDt = ? ))");
			queryString.append(" and (ll.prcsPageDtInd = 'Y' or ll.prcsPageDtInd is null)");
			if (StringUtils.hasText(threadCntStr)) {
				queryString.append(" and ll.thdCnt in(").append(threadCntStr).append(") ");
			}
			Query q = null;
			q = session.createQuery(queryString.toString());
			q.setParameter(0, AccountState.SETTLED.getCodeInDb());
			q.setParameter(1, AccountState.SETTLED.getCodeInDb());
			q.setParameter(2, buzDate);

			q.executeUpdate();
		} catch (RuntimeException e) {
			e.printStackTrace();
			throw new YcloansDBException("find error!", e);
		}
	}
	

	public void updateToCoreLmGlTx(Connection conn, String threadCntStr, String buzDate) {
		StringBuffer queryString = new StringBuffer(
				"update Lm_Loan lm set lm.prcs_Page_Dt_Ind = 'Y', lm.up_Ver = lm.up_Ver+1 where ");
		queryString.append("exists( SELECT * FROM (");
		queryString.append("SELECT a.loan_No FROM ");
		queryString.append(" (SELECT ll.loan_No from Lm_Loan ll where ");
		queryString.append(" ( ll.loan_Sts <> ? or (ll.loan_Sts = ? and ll.last_Setl_Dt = ? ))");
		queryString.append(" and ll.prcs_Page_Dt_Ind='N' ");
		if (StringUtils.hasText(threadCntStr)) {
			queryString.append(" and ll.thd_Cnt in(").append(threadCntStr).append(") ");
		}
		queryString.append(" order by ll.loan_No ) a");
		queryString.append(" WHERE rownum <= ?) c WHERE c.loan_No = lm.loan_No)");
		Serializable[] param = new Serializable[] {
				AccountState.SETTLED.getCodeInDb(), AccountState.SETTLED.getCodeInDb(), buzDate, DBConst.PAGE_RECORD};
		executeUpdateBySql(queryString.toString(), param);
	}

	@Override
	public String findBatState(Connection conn) {
		try {
			StringBuffer sql = new StringBuffer();
			sql.append("select bat_state  from etl_work_date ");
			Serializable[] params = new Serializable[] {};
				return (String) super.executeDirectQueryReturnOneValue(sql.toString());
			
		} catch (RuntimeException re) {
			throw new YcloansDBException(
					"findPage  failed");
		}
		
	}
	
}