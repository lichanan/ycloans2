package com.gacfinance.ycloans2.db.domain;

import java.math.BigDecimal;

/**
 * LmLoan entity. @author MyEclipse Persistence Tools
 */

public class LmLoan  implements java.io.Serializable {


    // Fields    

     private String loanNo;
     private String bankCde;
     private String bchCde;
     private String custId;
     private String custName;
     private String loanTyp;
     private String loanSts;
     private Short tnr;
     private String lastDueDt;
     private String intStartDt;
     private String loanDebtSts;
     private Byte dueDay;
     private String loanOdInd;
     private String loanDevaInd;
     private String loanDevaDt;
     private String loanStpAccInd;
     private String loanStpAccDt;
     private String loanActvDt;
     private String rateBase;
     private BigDecimal loanIntRate;
     private BigDecimal loanOdIntRate;
     private String lastGenOdIntDt;
     private String lastIntAccDt;
     private String lastSetlDt;
     private String curDueDt;
     private String nextDueDt;
     private String paymFreqUnit;
     private Short paymFreqFreq;
     private String loanCcy;
     private BigDecimal origPrcp;
     private BigDecimal loanOsPrcp;
     private Short loanTnr;
     private Short loanOsDay;
     private String loanGlRoleCde;
     private String bussTyp;
     private String loanGrd;
     private String loanPaymMtd;
     private String loanPaymTyp;
     private String curGenOdIntDt;
     private String lastOdIntAccDt;
     private String prcsPageDtInd;
     private Long upVer;
     private String nextSynRateDt;
     private String instmInd;
     private BigDecimal rlIntRate;
     private String dnChannel;
     private String paymChannel;
     private String loanGraceTyp;
     private Short loanOdGrace;
     private Short loanOdIntGrace;
     private Byte thdCnt;
     private String atpySts;
     private Short intFreeDays;			//免息天数
     private BigDecimal discIntRate;		//折扣期执行利率
     private String absLoanInd;			//资产证券化贷款标识
 	 private String paymInd;//是否支付 'Y' 为是， 'N' 为否当调用支付通道成功付款成功后，标记为Y。接口中传入为空的话默认为 'Y'
     private String lastChgDt;//最后修改日期
     private String intBase;//利息计算基础 PPP 按照放款本金计算 OSP 按照剩余本金计算
	 private BigDecimal vatRate;				//增值税率
	 private String fstPaymDt;
	 private String perdType;//借据长短期标记  短期：ST；中长期：LT
	 private String compInd;  //代偿标志
	 private String ddaPayTyp;  //资金归集方式
	 private String lastGenNormIntDt; //上次结利息日期
	 private String loanTypLine;  //业务条线
	 private Short notSetlTnr;   //未结清期数
	 private String settlementInd;//放款是否经过结算系统（是否存入内部户）
	 private String lastGenProdDt; //上次滚积数日期
	 private String grdMtdCde;  //五级分类方法代码
	 private String lastGrdDt; //最近一次分类日期
	 private String newGrdCde; //新分类代码
	 private String allOver;//是否整笔逾期
	 private String prcpPlan;//是否生成本金计划
	 private String isUnionSbsy;//是否联合贷贴息
	 private String isChangeTrial;//是否已转试乘试驾
	 private String isAdjSales;//是否已调剂销售
	 private String forceStopInd;//是否已强制终止
	 public String getIsChangeTrial() {
		 return isChangeTrial;
	 }
	 public void setIsChangeTrial(String isChangeTrial) {
	 	this.isChangeTrial = isChangeTrial;
	 }
	 public String getIsAdjSales() {
		 return isAdjSales;
	 }
	 public void setIsAdjSales(String isAdjSales) {
	 	this.isAdjSales = isAdjSales;
	 }
	 public String getLastGrdDt() {
		return lastGrdDt;
	}
	public void setLastGrdDt(String lastGrdDt) {
		this.lastGrdDt = lastGrdDt;
	}
	public String getNewGrdCde() {
		return newGrdCde;
	}
	public void setNewGrdCde(String newGrdCde) {
		this.newGrdCde = newGrdCde;
	}
	public String getGrdMtdCde() {
		return grdMtdCde;
	}
	public void setGrdMtdCde(String grdMtdCde) {
		this.grdMtdCde = grdMtdCde;
	}
	// Constructors
	public String getSettlementInd() {
		return settlementInd;
	}
	public void setSettlementInd(String settlementInd) {
		this.settlementInd = settlementInd;
	}
	
    public String getPerdType() {
		return perdType;
	}

	public void setPerdType(String perdType) {
		this.perdType = perdType;
	}

	public String getIntBase() {
		return intBase;
	}

	public void setIntBase(String intBase) {
		this.intBase = intBase;
	}

	/** default constructor */
    public LmLoan() {
    }

	/** minimal constructor */
    public LmLoan(String loanNo, Byte thdCnt) {
        this.loanNo = loanNo;
        this.thdCnt = thdCnt;
    }
    
    /** full constructor */
    public LmLoan(String loanNo, String bankCde, String bchCde, String custId, String custName, String loanTyp, String loanSts, Short tnr, String lastDueDt, String intStartDt, String loanDebtSts, Byte dueDay, String loanOdInd, String loanDevaInd, String loanDevaDt, String loanStpAccInd, String loanStpAccDt, String loanActvDt, String rateBase, BigDecimal loanIntRate, BigDecimal loanOdIntRate, String lastGenOdIntDt, String lastIntAccDt, String lastSetlDt, String curDueDt, String nextDueDt, String paymFreqUnit, Short paymFreqFreq, String loanCcy, BigDecimal origPrcp, BigDecimal loanOsPrcp, Short loanTnr, Short loanOsDay, String loanGlRoleCde, String bussTyp, String loanGrd, String loanPaymMtd, String loanPaymTyp, String curGenOdIntDt, String lastOdIntAccDt, String prcsPageDtInd, Long upVer, String nextSynRateDt, String instmInd, BigDecimal rlIntRate, String dnChannel, String paymChannel, String loanGraceTyp, Short loanOdGrace, Short loanOdIntGrace, Byte thdCnt, String atpySts) {
        this.loanNo = loanNo;
        this.bankCde = bankCde;
        this.bchCde = bchCde;
        this.custId = custId;
        this.custName = custName;
        this.loanTyp = loanTyp;
        this.loanSts = loanSts;
        this.tnr = tnr;
        this.lastDueDt = lastDueDt;
        this.intStartDt = intStartDt;
        this.loanDebtSts = loanDebtSts;
        this.dueDay = dueDay;
        this.loanOdInd = loanOdInd;
        this.loanDevaInd = loanDevaInd;
        this.loanDevaDt = loanDevaDt;
        this.loanStpAccInd = loanStpAccInd;
        this.loanStpAccDt = loanStpAccDt;
        this.loanActvDt = loanActvDt;
        this.rateBase = rateBase;
        this.loanIntRate = loanIntRate;
        this.loanOdIntRate = loanOdIntRate;
        this.lastGenOdIntDt = lastGenOdIntDt;
        this.lastIntAccDt = lastIntAccDt;
        this.lastSetlDt = lastSetlDt;
        this.curDueDt = curDueDt;
        this.nextDueDt = nextDueDt;
        this.paymFreqUnit = paymFreqUnit;
        this.paymFreqFreq = paymFreqFreq;
        this.loanCcy = loanCcy;
        this.origPrcp = origPrcp;
        this.loanOsPrcp = loanOsPrcp;
        this.loanTnr = loanTnr;
        this.loanOsDay = loanOsDay;
        this.loanGlRoleCde = loanGlRoleCde;
        this.bussTyp = bussTyp;
        this.loanGrd = loanGrd;
        this.loanPaymMtd = loanPaymMtd;
        this.loanPaymTyp = loanPaymTyp;
        this.curGenOdIntDt = curGenOdIntDt;
        this.lastOdIntAccDt = lastOdIntAccDt;
        this.prcsPageDtInd = prcsPageDtInd;
        this.upVer = upVer;
        this.nextSynRateDt = nextSynRateDt;
        this.instmInd = instmInd;
        this.rlIntRate = rlIntRate;
        this.dnChannel = dnChannel;
        this.paymChannel = paymChannel;
        this.loanGraceTyp = loanGraceTyp;
        this.loanOdGrace = loanOdGrace;
        this.loanOdIntGrace = loanOdIntGrace;
        this.thdCnt = thdCnt;
        this.atpySts = atpySts;
    }

   
    // Property accessors

    public String getLoanNo() {
        return this.loanNo;
    }
    
    public void setLoanNo(String loanNo) {
        this.loanNo = loanNo;
    }

    public String getBankCde() {
        return this.bankCde;
    }
    
    public void setBankCde(String bankCde) {
        this.bankCde = bankCde;
    }

    public String getBchCde() {
        return this.bchCde;
    }
    
    public void setBchCde(String bchCde) {
        this.bchCde = bchCde;
    }

    public String getCustId() {
        return this.custId;
    }
    
    public void setCustId(String custId) {
        this.custId = custId;
    }

    public String getCustName() {
        return this.custName;
    }
    
    public void setCustName(String custName) {
        this.custName = custName;
    }

    public String getLoanTyp() {
        return this.loanTyp;
    }
    
    public void setLoanTyp(String loanTyp) {
        this.loanTyp = loanTyp;
    }

    public String getLoanSts() {
        return this.loanSts;
    }
    
    public void setLoanSts(String loanSts) {
        this.loanSts = loanSts;
    }

    public Short getTnr() {
        return this.tnr;
    }
    
    public void setTnr(Short tnr) {
        this.tnr = tnr;
    }

    public String getLastDueDt() {
        return this.lastDueDt;
    }
    
    public void setLastDueDt(String lastDueDt) {
        this.lastDueDt = lastDueDt;
    }

    public String getIntStartDt() {
        return this.intStartDt;
    }
    
    public void setIntStartDt(String intStartDt) {
        this.intStartDt = intStartDt;
    }

    public String getLoanDebtSts() {
        return this.loanDebtSts;
    }
    
    public void setLoanDebtSts(String loanDebtSts) {
        this.loanDebtSts = loanDebtSts;
    }

    public Byte getDueDay() {
        return this.dueDay;
    }
    
    public void setDueDay(Byte dueDay) {
        this.dueDay = dueDay;
    }

    public String getLoanOdInd() {
        return this.loanOdInd;
    }
    
    public void setLoanOdInd(String loanOdInd) {
        this.loanOdInd = loanOdInd;
    }

    public String getLoanDevaInd() {
        return this.loanDevaInd;
    }
    
    public void setLoanDevaInd(String loanDevaInd) {
        this.loanDevaInd = loanDevaInd;
    }

    public String getLoanDevaDt() {
        return this.loanDevaDt;
    }
    
    public void setLoanDevaDt(String loanDevaDt) {
        this.loanDevaDt = loanDevaDt;
    }

    public String getLoanStpAccInd() {
        return this.loanStpAccInd;
    }
    
    public void setLoanStpAccInd(String loanStpAccInd) {
        this.loanStpAccInd = loanStpAccInd;
    }

    public String getLoanStpAccDt() {
        return this.loanStpAccDt;
    }
    
    public void setLoanStpAccDt(String loanStpAccDt) {
        this.loanStpAccDt = loanStpAccDt;
    }

    public String getLoanActvDt() {
        return this.loanActvDt;
    }
    
    public void setLoanActvDt(String loanActvDt) {
        this.loanActvDt = loanActvDt;
    }

    public String getRateBase() {
        return this.rateBase;
    }
    
    public void setRateBase(String rateBase) {
        this.rateBase = rateBase;
    }

    public BigDecimal getLoanIntRate() {
        return this.loanIntRate;
    }
    
    public void setLoanIntRate(BigDecimal loanIntRate) {
        this.loanIntRate = loanIntRate;
    }

    public BigDecimal getLoanOdIntRate() {
        return this.loanOdIntRate;
    }
    
    public void setLoanOdIntRate(BigDecimal loanOdIntRate) {
        this.loanOdIntRate = loanOdIntRate;
    }

    public String getLastGenOdIntDt() {
        return this.lastGenOdIntDt;
    }
    
    public void setLastGenOdIntDt(String lastGenOdIntDt) {
        this.lastGenOdIntDt = lastGenOdIntDt;
    }

    public String getLastIntAccDt() {
        return this.lastIntAccDt;
    }
    
    public void setLastIntAccDt(String lastIntAccDt) {
        this.lastIntAccDt = lastIntAccDt;
    }

    public String getLastSetlDt() {
        return this.lastSetlDt;
    }
    
    public void setLastSetlDt(String lastSetlDt) {
        this.lastSetlDt = lastSetlDt;
    }

    public String getCurDueDt() {
        return this.curDueDt;
    }
    
    public void setCurDueDt(String curDueDt) {
        this.curDueDt = curDueDt;
    }

    public String getNextDueDt() {
        return this.nextDueDt;
    }
    
    public void setNextDueDt(String nextDueDt) {
        this.nextDueDt = nextDueDt;
    }

    public String getPaymFreqUnit() {
        return this.paymFreqUnit;
    }
    
    public void setPaymFreqUnit(String paymFreqUnit) {
        this.paymFreqUnit = paymFreqUnit;
    }

    public Short getPaymFreqFreq() {
        return this.paymFreqFreq;
    }
    
    public void setPaymFreqFreq(Short paymFreqFreq) {
        this.paymFreqFreq = paymFreqFreq;
    }

    public String getLoanCcy() {
        return this.loanCcy;
    }
    
    public void setLoanCcy(String loanCcy) {
        this.loanCcy = loanCcy;
    }

    public BigDecimal getOrigPrcp() {
        return this.origPrcp;
    }
    
    public void setOrigPrcp(BigDecimal origPrcp) {
        this.origPrcp = origPrcp;
    }

    public BigDecimal getLoanOsPrcp() {
        return this.loanOsPrcp;
    }
    
    public void setLoanOsPrcp(BigDecimal loanOsPrcp) {
        this.loanOsPrcp = loanOsPrcp;
    }

    public Short getLoanTnr() {
        return this.loanTnr;
    }
    
    public void setLoanTnr(Short loanTnr) {
        this.loanTnr = loanTnr;
    }

    public Short getLoanOsDay() {
        return this.loanOsDay;
    }
    
    public void setLoanOsDay(Short loanOsDay) {
        this.loanOsDay = loanOsDay;
    }

    public String getLoanGlRoleCde() {
        return this.loanGlRoleCde;
    }
    
    public void setLoanGlRoleCde(String loanGlRoleCde) {
        this.loanGlRoleCde = loanGlRoleCde;
    }

    public String getBussTyp() {
        return this.bussTyp;
    }
    
    public void setBussTyp(String bussTyp) {
        this.bussTyp = bussTyp;
    }

    public String getLoanGrd() {
        return this.loanGrd;
    }
    
    public void setLoanGrd(String loanGrd) {
        this.loanGrd = loanGrd;
    }

    public String getLoanPaymMtd() {
        return this.loanPaymMtd;
    }
    
    public void setLoanPaymMtd(String loanPaymMtd) {
        this.loanPaymMtd = loanPaymMtd;
    }

    public String getLoanPaymTyp() {
        return this.loanPaymTyp;
    }
    
    public void setLoanPaymTyp(String loanPaymTyp) {
        this.loanPaymTyp = loanPaymTyp;
    }

    public String getCurGenOdIntDt() {
        return this.curGenOdIntDt;
    }
    
    public void setCurGenOdIntDt(String curGenOdIntDt) {
        this.curGenOdIntDt = curGenOdIntDt;
    }

    public String getLastOdIntAccDt() {
        return this.lastOdIntAccDt;
    }
    
    public void setLastOdIntAccDt(String lastOdIntAccDt) {
        this.lastOdIntAccDt = lastOdIntAccDt;
    }

    public String getPrcsPageDtInd() {
        return this.prcsPageDtInd;
    }
    
    public void setPrcsPageDtInd(String prcsPageDtInd) {
        this.prcsPageDtInd = prcsPageDtInd;
    }

    public Long getUpVer() {
        return this.upVer;
    }
    
    public void setUpVer(Long upVer) {
        this.upVer = upVer;
    }

    public String getNextSynRateDt() {
        return this.nextSynRateDt;
    }
    
    public void setNextSynRateDt(String nextSynRateDt) {
        this.nextSynRateDt = nextSynRateDt;
    }

    public String getInstmInd() {
        return this.instmInd;
    }
    
    public void setInstmInd(String instmInd) {
        this.instmInd = instmInd;
    }

    public BigDecimal getRlIntRate() {
        return this.rlIntRate;
    }
    
    public void setRlIntRate(BigDecimal rlIntRate) {
        this.rlIntRate = rlIntRate;
    }

    public String getDnChannel() {
        return this.dnChannel;
    }
    
    public void setDnChannel(String dnChannel) {
        this.dnChannel = dnChannel;
    }

    public String getPaymChannel() {
        return this.paymChannel;
    }
    
    public void setPaymChannel(String paymChannel) {
        this.paymChannel = paymChannel;
    }

    public String getLoanGraceTyp() {
        return this.loanGraceTyp;
    }
    
    public void setLoanGraceTyp(String loanGraceTyp) {
        this.loanGraceTyp = loanGraceTyp;
    }

    public Short getLoanOdGrace() {
        return this.loanOdGrace;
    }
    
    public void setLoanOdGrace(Short loanOdGrace) {
        this.loanOdGrace = loanOdGrace;
    }

    public Short getLoanOdIntGrace() {
        return this.loanOdIntGrace;
    }
    
    public void setLoanOdIntGrace(Short loanOdIntGrace) {
        this.loanOdIntGrace = loanOdIntGrace;
    }

    public Byte getThdCnt() {
        return this.thdCnt;
    }
    
    public void setThdCnt(Byte thdCnt) {
        this.thdCnt = thdCnt;
    }

    public String getAtpySts() {
        return this.atpySts;
    }
    
    public void setAtpySts(String atpySts) {
        this.atpySts = atpySts;
    }

	public Short getIntFreeDays() {
		return intFreeDays;
	}

	public void setIntFreeDays(Short intFreeDays) {
		this.intFreeDays = intFreeDays;
	}

	public BigDecimal getDiscIntRate() {
		return discIntRate;
	}

	public void setDiscIntRate(BigDecimal discIntRate) {
		this.discIntRate = discIntRate;
	}

	public String getAbsLoanInd() {
		return absLoanInd;
	}

	public void setAbsLoanInd(String absLoanInd) {
		this.absLoanInd = absLoanInd;
	}

	public void setPaymInd(String paymInd) {
		this.paymInd = paymInd;
	}

	public String getPaymInd() {
		return paymInd;
	}

	public void setLastChgDt(String lastChgDt) {
		this.lastChgDt = lastChgDt;
	}

	public String getLastChgDt() {
		return lastChgDt;
	}
   	
	public BigDecimal getVatRate() {
		return vatRate;
	}

	public void setVatRate(BigDecimal vatRate) {
		this.vatRate = vatRate;
	}

	public String getFstPaymDt() {
		return fstPaymDt;
	}

	public void setFstPaymDt(String fstPaymDt) {
		this.fstPaymDt = fstPaymDt;
	}
	public String getDdaPayTyp() {
		return ddaPayTyp;
	}

	public void setDdaPayTyp(String ddaPayTyp) {
		this.ddaPayTyp = ddaPayTyp;
	}

	public String getCompInd() {
		return compInd;
	}

	public void setCompInd(String compInd) {
		this.compInd = compInd;
	}

	public String getLastGenNormIntDt() {
		return lastGenNormIntDt;
	}

	public void setLastGenNormIntDt(String lastGenNormIntDt) {
		this.lastGenNormIntDt = lastGenNormIntDt;
	}

	public String getLoanTypLine() {
		return loanTypLine;
	}

	public void setLoanTypLine(String loanTypLine) {
		this.loanTypLine = loanTypLine;
	}
	
	public Short getNotSetlTnr(){
		return notSetlTnr;
	}
	public void setNotSetlTnr(Short notSetlTnr){
		this.notSetlTnr = notSetlTnr;
		
	}
	
	public String getLastGenProdDt() {
		return lastGenProdDt;
	}
	public void setLastGenProdDt(String lastGenProdDt) {
		this.lastGenProdDt = lastGenProdDt;
	}
	public String getAllOver() {
		return allOver;
	}
	public void setAllOver(String allOver) {
		this.allOver = allOver;
	}
	public String getPrcpPlan() {
		return prcpPlan;
	}
	public void setPrcpPlan(String prcpPlan) {
		this.prcpPlan = prcpPlan;
	}
	public String getIsUnionSbsy() {
		return isUnionSbsy;
	}
	public void setIsUnionSbsy(String isUnionSbsy) {
		this.isUnionSbsy = isUnionSbsy;
	}
	public String getForceStopInd() {
		return forceStopInd;
	}
	public void setForceStopInd(String forceStopInd) {
		this.forceStopInd = forceStopInd;
	}
	
}