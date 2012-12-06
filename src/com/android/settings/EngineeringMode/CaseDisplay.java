/*
 * Copyright (C) 2009-2010 Broadcom Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.EngineeringMode;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.text.format.DateUtils;
import android.widget.TextView;
import android.os.AsyncResult;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.EngineeringMode.*;

import com.android.internal.telephony.IccCard;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.RILConstants.SimCardID;

import com.broadcom.diagnostics.BcmEngModeDiagServiceAdapter;
import com.broadcom.diagnostics.BcmSummaryEngModeDiag;
import com.broadcom.diagnostics.BcmDiagListener;

import java.util.Timer;
import java.util.TimerTask;
import android.content.ServiceConnection;
import android.content.ComponentName;
import android.content.Intent;
import android.os.IBinder;

import com.android.settings.Utils;

public class CaseDisplay extends Activity implements BcmDiagListener {
    private TextView mInfo1;
    private TextView mInfo2;
    private TextView mCaseTitle;
    private static final int EVENT_SIM1_GET_CPDIAG_RESULT = 0;
    private static final int EVENT_SIM2_GET_CPDIAG_RESULT = 1;
    private TextView mElapsedTime;

    private static final int CASE_GSM_SERVING_CELL_PARAMETERS= 1;
    private static final int CASE_NETWORK_PARAMETERS= 2;
    private static final int CASE_CALL_CONTROL_FAILURE_CAUSE= 4;
    private static final int CASE_RADIO_RESOURSE_FAILURE_CAUSE_CODE= 5;
    private static final int CASE_GPRS_EDGE_DATA_TRANSFER= 6;
    private static final int CASE_CELL_SELECTION_RESELECTION_SCREEN= 8;
    private static final int CASE_RAB_SCREEN= 9;
    private static final int CASE_RAC_SCREEN= 10;
    private static final int CASE_DEDICATED_CHANNEL_POWER_SCREEN= 11;
    private static final int CASE_INTRA_FREQUENCY_MEASUREMENT_SCREEN= 12;
    private static final int CASE_INTER_FREQUENCY_MEASUREMENT_SCREEN= 13;
    private static final int CASE_INTERRAT_FREQUENCY_MEASUREMENT_SCREEN= 14;
    private static final int CASE_HSDPA_SCREEN= 15;
    private static final int CASE_RACH_SCREEN= 16;
    private static final int CASE_CPC_PARAMETERS= 17;
    private static final int CASE_FAST_DORMANCY= 18;
    private static final int CASE_RECEIVE_DIVERSITY= 19;
    private static final int CASE_CB_CMAS_PLAN= 20;
    private static final int CASE_AMR= 21;

    private int      mRefreshTimeValue;
    private Phone mPhone;
    private Phone mPhone2;
    private boolean mSIM1Available;
    private boolean mSIM2Available;
    private TimerTask mPoller;
    private int mTestItem;

    private static final String LOG_TAG = CaseDisplay.class.getSimpleName();
    private static final boolean DBG = EngMode.DBG;
    private BcmEngModeDiagServiceAdapter bcmDiagServiceAdapter;
    private Timer timer;


    private String getGsmServingCellParameters(BcmSummaryEngModeDiag  CpDiagData)
    {
          String InfoStr;
          InfoStr= "BCCH Information:Which One Parmeter!\n"
                       +"RX Power Level in dBm:"+CpDiagData.gsm_param.rxlev+"\n"
                       +"TX Power Level in dBm:"+CpDiagData.gsm_param.txpwr+"\n"
                       +"Time Slot:"+CpDiagData.gsm_param.timeslot_assigned+"\n"
                       +"Timing Advance:"+CpDiagData.gsm_param.timing_advance+"\n";

            //- 0 DTX was not used
            //- 1 DTX was used.
            if(CpDiagData.gsm_param.dtx_used == 1) {
                InfoStr=InfoStr+"RX Quality:"+CpDiagData.gsm_param.rxqualsub+"\n";
            }
            else
            {
                InfoStr=InfoStr+"RX Quality:"+CpDiagData.gsm_param.rxqualfull+"\n";
            }

            InfoStr=InfoStr+"Radio Link Timeout Value:"+CpDiagData.gsm_param.radio_link_timeout+"\n"
                                   +"Current Channel Type:"+CpDiagData.gsm_param.chan_type+"\n"
                                   +"Paging Information:"+CpDiagData.gsm_param.bs_pa_mfrms+"\n"
                                   +"IMSI Attach:"+CpDiagData.mm_param.gmm_attach_type+"\n";

            return InfoStr;
    }

      private String getNetworkParameters(BcmSummaryEngModeDiag  CpDiagData)
    {
          String InfoStr;

          if(1 == CpDiagData.mm_param.rat)
          {
              InfoStr="MCC:"+CpDiagData.umts_param.plmn_id.mcc+"\n"
                           +"MNC:"+CpDiagData.umts_param.plmn_id.mnc+"\n"
                           +"LAC:"+CpDiagData.umts_param.lac+"\n"
                           +"Downlink UARFCN:"+CpDiagData.umts_param.dl_uarfcn+"\n"
                           +"Uplink UARFCN:"+CpDiagData.umts_param.ul_uarfcn+"\n"
                           +"Cell Id:"+CpDiagData.umts_param.cell_id+"\n";
          }else if(0 == CpDiagData.mm_param.rat){
              InfoStr="MCC:"+CpDiagData.gsm_param.mcc+"\n"
                           +"MNC:"+CpDiagData.gsm_param.mnc+"\n"
                           +"LAC:"+CpDiagData.gsm_param.lac+"\n"
                           +"ARFCN of Serving Cell:"+CpDiagData.gsm_param.arfcn+"\n"
                           +"Cell Id:"+CpDiagData.gsm_param.ci+"\n";
          }else{
              InfoStr="\n";
          }

          InfoStr=InfoStr+"Bsic:"+CpDiagData.gsm_param.bsic+"\n"
                                +"Network Control Order:"+CpDiagData.gsm_param.nco+"\n"
                                +"Network Operation Mode:"+CpDiagData.mm_param.nom+"\n"
                                +"Support for BTS Test:Parmeter Not Found!\n"
                                +"Ciphering Status:"+CpDiagData.gsm_param.cipher_on+"\n"
                                +"Frequency Hopping:Which One Parmeter!\n";

            return InfoStr;
    }

    private String getCallControlFailureCauseCode(BcmSummaryEngModeDiag  CpDiagData)
    {
          String InfoStr;

          InfoStr="Radio Link Failure:"+CpDiagData.umts_param.rrc_counters.radio_link_failure+"\n"
                       +"UMTS GSM Handover:"+CpDiagData.umts_param.rrc_counters.umts_gsm_handover+"\n"
                       +"RLC Unrecoverable:"+CpDiagData.umts_param.rrc_counters.rlc_unrecoverable_error+"\n"
                       +"NAS Triggered Release:"+CpDiagData.umts_param.rrc_counters.nas_triggered_release+"\n"
                       +"Normal Release:"+CpDiagData.umts_param.rrc_counters.normal_release+"\n"
                       +"Configuration Failure:"+CpDiagData.umts_param.rrc_counters.configuration_failure+"\n"
                       +"N300 Failure:"+CpDiagData.umts_param.rrc_counters.n300_failure+"\n"
                       +"T314 T315 Failure:"+CpDiagData.umts_param.rrc_counters.t314_t315_failure+"\n"
                       +"N302 Failure:"+CpDiagData.umts_param.rrc_counters.n302_failure+"\n"
                       +"T316 T317 T307 Failure:"+CpDiagData.umts_param.rrc_counters.t316_t317_t307_failure+"\n"
                       +"Other Failure:"+CpDiagData.umts_param.rrc_counters.other_failure+"\n";

            return InfoStr;
    }

    private String getRadioResourseFailureCauseCode(BcmSummaryEngModeDiag  CpDiagData)
    {
          String InfoStr;

         //UMTS
        if(1 == CpDiagData.mm_param.rat){
            if(CpDiagData.umts_param.chn_rel_cause == 0) {
               InfoStr="Radio Resource Failure Cause Code:Protocol Error -- Unspecified!\n";
            }
            else
            if(CpDiagData.umts_param.chn_rel_cause == 17) {
               InfoStr="Radio Resource Failure Cause Code:Normal Release!\n";
            }
            else
            if(CpDiagData.umts_param.chn_rel_cause == 19) {
               InfoStr="Radio Resource Failure Cause Code:Pre-Emptive Release!\n";
            }
            else
            if(CpDiagData.umts_param.chn_rel_cause == 20) {
               InfoStr="Radio Resource Failure Cause Code:Congestion!\n";
            }
            else
            if(CpDiagData.umts_param.chn_rel_cause == 21) {
               InfoStr="Radio Resource Failure Cause Code:Re-Establishment Reject!\n";
            }
            else
            if(CpDiagData.umts_param.chn_rel_cause == 22) {
               InfoStr="Radio Resource Failure Cause Code:Directed Signaling Connection Re-Establishment!\n";
            }
            else
            if(CpDiagData.umts_param.chn_rel_cause == 23) {
               InfoStr="Radio Resource Failure Cause Code:User Inactivity!\n";
            }
            else
            {
                InfoStr="Radio Resource Failure Cause Code:Mapping is wrong! ID:"+CpDiagData.umts_param.chn_rel_cause+"\n";
            }
            return InfoStr;
        }
        else //GSM
        if(0 == CpDiagData.mm_param.rat){
            if(CpDiagData.gsm_param.chn_rel_cause == 0) {
               InfoStr="Radio Resource Failure Cause Code:Protocol Error -- Unspecified!\n";
            }
            else
            if(CpDiagData.gsm_param.chn_rel_cause == 13) {
               InfoStr="Radio Resource Failure Cause Code:Abnormal release!\n";
            }
            else
            if(CpDiagData.gsm_param.chn_rel_cause == 19) {
               InfoStr="Radio Resource Failure Cause Code:Pre-Emptive Release!\n";
            }
            else
            if(CpDiagData.gsm_param.chn_rel_cause == 38) {
               InfoStr="Radio Resource Failure Cause Code:Normal Release\n";
            }
            else
            {
                InfoStr="Radio Resource Failure Cause Code:Mapping is wrong! ID:"+CpDiagData.gsm_param.chn_rel_cause+"\n";
            }
            return InfoStr;
        }  else{
            return "\n";
        }
    }

    private String getGprsEdgeDataTransfer(BcmSummaryEngModeDiag  CpDiagData)
    {
        String InfoStr;

        int tempValue , i, NumOfSlots;
/*
        switch(CpDiagData.mm_param.ps_state) {
                case UNKNOWNSTATE:
                InfoStr="GPRS State:Unknow State!\n";
                break;
                case NO_NETWORK_AVAILABLE:
                InfoStr="GPRS State:No Network Available!\n";
                break;
                case SEARCH_FOR_NETWORK:
                InfoStr="GPRS State:Search For Network!\n";
                break;
                case EMERGENCY_CALLS_ONLY:
                InfoStr="GPRS State:Emergency Calls Only!\n";
                break;
                case LIMITEDSERVICE:
                InfoStr="GPRS State:Limited Service!\n";
                break;
                case FULL_SERVICE:
                InfoStr="GPRS State:Full Service!\n";
                break;
                case PLMN_LIST_AVAILABLE:
                InfoStr="GPRS State:Plmn List Available!\n";
                break;
                case DISABLEDSTATE:
                InfoStr="GPRS State:Disabled State!\n";
                break;
                case DETACHEDSTATE:
                InfoStr="GPRS State:Detached State!\n";
                break;
                case NOGPRSCELL:
                InfoStr="GPRS State:No GPRS Cell!\n";
                break;
                case SUSPENDEDSTATE:
                InfoStr="GPRS State:Suspended State!\n";
                break;
                default:
                InfoStr="Fail! No Supported State!\n";
                break;
            }
   */
            InfoStr="Fail! No Supported State!\n";//test only

            InfoStr=InfoStr+"PDP State:\n"
                                  +"Ms Initated PDP Context Activation Attempt:"+CpDiagData.ext_param.sm_ext_param.mo_pdp_attempt_cnt+"\n"
                                  +"Radio Priority of PDP Context:"+CpDiagData.ext_param.sm_ext_param.pdp_priority+"\n"
                                  +"Sec Radio Priority of PDP Context:"+CpDiagData.ext_param.sm_ext_param.sec_pdp_priority+"\n";


            NumOfSlots = 0;
            tempValue = CpDiagData.gsm_param.gprs_packet_param.dl_timeslot_assigned;

            for(i=0;i<8;i++){
                if((tempValue & 0x1)==1) {
                    NumOfSlots++;
                }
                tempValue = tempValue>>1;
            }

            tempValue = CpDiagData.gsm_param.gprs_packet_param.ul_timeslot_assigned;

            for(i=0;i<8;i++){
                if((tempValue & 0x1)==1) {
                    NumOfSlots++;
                }
                tempValue = tempValue>>1;
            }

            InfoStr=InfoStr+"Number of Slots Supported:"+NumOfSlots+"\n"
                                  +"DL Coding scheme:\n";

            for(i=0;i<8;i++){
                InfoStr=InfoStr+"Slot"+i+", CS("+CpDiagData.gsm_param.gprs_packet_param.dl_cs_mode_per_ts[i]+")\n";
            }

            InfoStr=InfoStr+"UL Coding scheme:\n";

            for(i=0;i<8;i++){
                InfoStr=InfoStr+"Slot"+i+", CS("+CpDiagData.gsm_param.gprs_packet_param.ul_cs_mode_per_ts[i]+")\n";
            }

            InfoStr=InfoStr+"Timers (RAU): Parameter Not Sure\n"
                                  +"Average Throughput: Not Support\n"
                                  +"BLER Rate: Not Support\n";

            return InfoStr;
    }

    private String getCellSelectionReselectionScreen(BcmSummaryEngModeDiag  CpDiagData)
    {
          String InfoStr;
          int i;

           InfoStr="Cell Selection: Not Support\n"
                     +"Rs:"+ CpDiagData.umts_param.ranking_value+"\n";

            for(i=0;i<CpDiagData.umts_param.no_umts_ncells;i++) {
                //5 means UMTS_RANKED
                if(CpDiagData.umts_param.umts_ncell.A[i].cell_type == 5) {
                    InfoStr=InfoStr+"Rn["+i+"](UMTS):"+CpDiagData.umts_param.umts_ncell.A[i].ranking_value+"\n";
                }
            }

            for(i=0;i<CpDiagData.umts_param.no_gsm_ncells;i++) {
                    InfoStr=InfoStr+"Rn["+i+"](UMTS):"+CpDiagData.umts_param.gsm_ncell.A[i].ranking_value+"\n";
            }

            InfoStr=InfoStr+"Counter for UMTS to GSM Reselection:"+CpDiagData.umts_param.rrc_counters.umts_gsm_reselect_success+"\n"
                                  +"Counter for GSM to UMTS Reselection:"+CpDiagData.umts_param.rrc_counters.gsm_umts_reselect_success+"\n";

            return InfoStr;
    }

    private String getRABScreen(BcmSummaryEngModeDiag  CpDiagData)
    {
            String InfoStr;
            int i, j;


           InfoStr="** RAB ID of the Assigned Bearer **\nn";

            if(CpDiagData.umts_param.rab_rb_info.no_rabs != 0) {
                for(i=0;i<CpDiagData.umts_param.rab_rb_info.no_rabs;i++) {
                    InfoStr=InfoStr+"rab_id:"+CpDiagData.umts_param.rab_rb_info.per_rab_info[i].rab_id+", domain:"+CpDiagData.umts_param.rab_rb_info.per_rab_info[i].domain+"\n";
                }
            }
            else
            {
                InfoStr=InfoStr+"No RAB_ID & Domain\n";
            }

            InfoStr=InfoStr+"Data Rate of the RAB depending on its Class:Not Support\n"
                                  +"** Signalling Radio Bearer Id **\n";

            if(CpDiagData.umts_param.rab_rb_info.no_srbs != 0) {
                for(i=0;i<CpDiagData.umts_param.rab_rb_info.no_srbs;i++) {
                    InfoStr=InfoStr+"** SRB ID:"+CpDiagData.umts_param.rab_rb_info.srb_id[i]+"**\n";
                }
            }
            else
            {
                InfoStr=InfoStr+"No SRB_ID\n";
            }

            InfoStr=InfoStr+"Traffic Class:Not Support\n"
                                  +"Max Bit Rate:Not Support\n"
                                  +"Residual BERatio:Not Support\n"
                                  +"Guaranteed Bit Rate:Not Support\n";

           return InfoStr;
    }

    private String getRACScreen(BcmSummaryEngModeDiag  CpDiagData)
    {
           String InfoStr;

           InfoStr="Current RRC State:"+CpDiagData.umts_param.rrc_state+"\n"
                     +"RRC Connection Rejection Cause:"+CpDiagData.umts_param.chn_rel_cause+"\n"
                     +"RRC Connection Release Cause:"+CpDiagData.umts_param.chn_rel_cause+"\n";

           return InfoStr;
    }

    private String getDedicateChannelPowerScreen(BcmSummaryEngModeDiag  CpDiagData)
    {
           String InfoStr;

           InfoStr="UE TX Power:"+CpDiagData.umts_param.tx_pwr+"\n"
                      +"TX Power Control Algorithm:"+CpDiagData.umts_param.l1_info.power_control_algorithm+"\n"
                      +"TX Power Control Step Size:"+CpDiagData.umts_param.l1_info.power_control_step_size+"\n"
                      +"SIR:"+CpDiagData.umts_param.dch_report.meas_sir+"\n"
                      +"BLER:"+CpDiagData.umts_param.dch_report.meas_bler+"\n"
                      +"UTRA Carrier RSSI:Not Support\n";

           return InfoStr;
    }

    private String getIntraFrequencyMeasurementScreen(BcmSummaryEngModeDiag  CpDiagData)
    {
            String InfoStr;
            int i, Cell_in_Active_Set_Count=0, Cell_in_Monitor_Set_Count=0;

            InfoStr="*** Following Cell Parameters in The Active Set ***\n";

            for(i=0;i<CpDiagData.umts_param.no_umts_ncells;i++) {
                    //0 means ACTIVE_SET
                    if(CpDiagData.umts_param.umts_ncell.A[i].cell_type == 0) {

                       InfoStr=InfoStr+"** Cell Number:"+i+"**\n"
                                              +"DL UARFCN:"+CpDiagData.umts_param.umts_ncell.A[i].dl_uarfcn+"\n"
                                              +"Primary Scrambling Code:"+CpDiagData.umts_param.umts_ncell.A[i].cpich_sc+"\n"
                                              +"RSCP:"+CpDiagData.umts_param.umts_ncell.A[i].cpich_rscp+"\n"
                                              +"EC2N0:"+CpDiagData.umts_param.umts_ncell.A[i].cpich_ecn0+"\n";

                        Cell_in_Active_Set_Count++;
                    }
            }

            if(Cell_in_Active_Set_Count == 0)
            {
                InfoStr=InfoStr+"No Cell Parameters in The Active Set\n";
            }

            InfoStr=InfoStr+"*** Cell Parameters in The Monitored Set for Best 6 Cells ***\n";

            for(i=0;i<CpDiagData.umts_param.no_umts_ncells;i++) {
                //2 means MONITORED
                //since it's intra frequency, so dl_uarfcn should be the same as serving cell
                if((CpDiagData.umts_param.umts_ncell.A[i].cell_type == 2) &&
                   (CpDiagData.umts_param.umts_ncell.A[i].dl_uarfcn == CpDiagData.umts_param.dl_uarfcn)) {

                   InfoStr=InfoStr+"** Cell Number:"+i+"**\n"
                                          +"DL UARFCN:"+CpDiagData.umts_param.umts_ncell.A[i].dl_uarfcn+"\n"
                                          +"Primary Scrambling Code:"+CpDiagData.umts_param.umts_ncell.A[i].cpich_sc+"\n"
                                          +"RSCP:"+CpDiagData.umts_param.umts_ncell.A[i].cpich_rscp+"\n"
                                          +"EC2N0:"+CpDiagData.umts_param.umts_ncell.A[i].cpich_ecn0+"\n";

                    Cell_in_Monitor_Set_Count++;
                }
            }

            if(Cell_in_Monitor_Set_Count == 0)
            {
                InfoStr=InfoStr+"No Cell Parameters in The Monitored Set\n";
            }

           return InfoStr;
    }

    private String getInterFrequencyMeasurementScreen(BcmSummaryEngModeDiag  CpDiagData)
    {
            String InfoStr;
            int i, Cell_Count=0;

            InfoStr="\n";
            for(i=0;i<CpDiagData.umts_param.no_umts_ncells;i++) {
                    if(CpDiagData.umts_param.dl_uarfcn != CpDiagData.umts_param.umts_ncell.A[i].dl_uarfcn) {

                       InfoStr=InfoStr+"** Cell Number:"+i+"**\n"
                                  +"DL UARFCN:"+CpDiagData.umts_param.umts_ncell.A[i].dl_uarfcn+"\n"
                                  +"Primary Scrambling Code:"+CpDiagData.umts_param.umts_ncell.A[i].cpich_sc+"\n"
                                  +"RSCP:"+CpDiagData.umts_param.umts_ncell.A[i].cpich_rscp+"\n"
                                  +"EC2N0:"+CpDiagData.umts_param.umts_ncell.A[i].cpich_ecn0+"\n";
                        Cell_Count++;
                    }
            }

            if(Cell_Count == 0) {
                InfoStr=InfoStr+"No Cell Parameters\n";
            }

           return InfoStr;
    }

    private String getInterRATFrequencyMeasurementScreen(BcmSummaryEngModeDiag  CpDiagData)
    {
            String InfoStr;
            int ranking_val_id;
            int i;
            InfoStr="\n";
            if(CpDiagData.umts_param.no_gsm_ncells != 0) {
                    for(i=0;i<CpDiagData.umts_param.no_gsm_ncells;i++) {
                       InfoStr=InfoStr+"Cell["+i+"]\n"
                                  +"Cellular System:Not Support!\n"
                                  +"Channel Number::"+CpDiagData.umts_param.gsm_ncell.A[i].arfcn+"\n"
                                  +"BSIC:"+CpDiagData.umts_param.gsm_ncell.A[i].bsic+"\n"
                                  +"Cell Id:"+CpDiagData.umts_param.gsm_ncell.A[i].ci+"\n"
                                  +"Compressed Mode Active or Not :Not Support!\n"
                                  +"GSM Carrier RSSI:"+CpDiagData.umts_param.gsm_ncell.A[i].rxlev+"\n";

                }
            }
            else
            {
                InfoStr=InfoStr+"No Cell Parameters\n";
            }


            return InfoStr;
    }

    private String getHSDPAScreen(BcmSummaryEngModeDiag  CpDiagData)
    {
           String InfoStr;

           InfoStr="Number of Assigned HS-SCCH Codes:"+CpDiagData.umts_param.hspa_config.no_hsscch_codes+"]\n"
                      +"Data Rate:"+CpDiagData.umts_param.hsdpa_l1_l2_info.data_rate+"\n"
                      +"CQI:"+CpDiagData.umts_param.hsdpa_l1_l2_info.cqi+"\n"
                      +"BLER:"+CpDiagData.umts_param.hsdpa_l1_l2_info.bler+"\n";

           return InfoStr;
    }

    private String getRACHScreen(BcmSummaryEngModeDiag  CpDiagData)
    {
           String InfoStr;

           InfoStr="Power of First Preamble:"+CpDiagData.umts_param.rach_info.initial_tx_pwr+"\n"
                      +"Number of Preambles:"+CpDiagData.umts_param.rach_info.preamble_transmission_count+"\n"
                      +"AICH Status:Not Support!\n"
                      +"Power Message Was Transmitted at:"+CpDiagData.umts_param.rach_info.msg_power+"\n";

           return InfoStr;
    }

    private String getCPCParameter(BcmSummaryEngModeDiag  CpDiagData)
    {
           String InfoStr;
           InfoStr="CPC Parameters:Not Support!\n";
           return InfoStr;
    }

    private String getFastDormancy(BcmSummaryEngModeDiag  CpDiagData)
    {
           String InfoStr;
           InfoStr="Option to Turn Fast Dormancy On/Off:Not Support!\n";
           return InfoStr;
    }

    private String getReceiveDiversity(BcmSummaryEngModeDiag  CpDiagData)
    {
           String InfoStr;
           InfoStr="Option to Turn Receive Diversity On/Off:Not Support!\n";
           return InfoStr;
    }

    private String getCBCmasPlan(BcmSummaryEngModeDiag  CpDiagData)
    {
           String InfoStr;

           InfoStr="CTCH schedule period:Not Support!\n"
                      +"CTCH Length:Not Support!\n";

           return InfoStr;
    }

    private String getAMR(BcmSummaryEngModeDiag  CpDiagData)
    {
            String InfoStr;
           InfoStr="Type of AMR Codec:"+CpDiagData.umts_param.amr_info.umts_amr_codec_mode+"\n"
                      +"Upgrade and Downgrade of the AMR Codec:Not Support!\n"
                      +"Option to Turn WB-AMR On/Off If Supported:Not Support!\n";

           return InfoStr;
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
           int SimId=SimCardID.ID_ZERO.toInt();
            switch (msg.what) {
                    case EVENT_SIM2_GET_CPDIAG_RESULT:
                            SimId=SimCardID.ID_ONE.toInt();
                    case EVENT_SIM1_GET_CPDIAG_RESULT:
                            String InfoStr="N/A";
                      BcmSummaryEngModeDiag  CpDiagData = (BcmSummaryEngModeDiag ) msg.obj;
                            switch (mTestItem){
                                case CASE_GSM_SERVING_CELL_PARAMETERS:
                                         InfoStr=getGsmServingCellParameters(CpDiagData);
                                break;

                                case CASE_NETWORK_PARAMETERS:
                                         InfoStr=getNetworkParameters(CpDiagData);
                                break;

                                case CASE_CALL_CONTROL_FAILURE_CAUSE:
                                         InfoStr=getCallControlFailureCauseCode(CpDiagData);
                                break;

                                case CASE_RADIO_RESOURSE_FAILURE_CAUSE_CODE:
                                         InfoStr=getRadioResourseFailureCauseCode(CpDiagData);
                                break;

                                case CASE_GPRS_EDGE_DATA_TRANSFER:
                                         InfoStr=getGprsEdgeDataTransfer(CpDiagData);
                                break;

                                case CASE_CELL_SELECTION_RESELECTION_SCREEN:
                                         InfoStr=getCellSelectionReselectionScreen(CpDiagData);
                                break;

                                case CASE_RAB_SCREEN:
                                         InfoStr=getRABScreen(CpDiagData);
                                break;

                                case CASE_RAC_SCREEN:
                                         InfoStr=getRACScreen(CpDiagData);
                                break;

                                case CASE_DEDICATED_CHANNEL_POWER_SCREEN:
                                         InfoStr=getDedicateChannelPowerScreen(CpDiagData);
                                break;

                                case CASE_INTRA_FREQUENCY_MEASUREMENT_SCREEN:
                                         InfoStr=getIntraFrequencyMeasurementScreen(CpDiagData);
                                break;

                                case CASE_INTER_FREQUENCY_MEASUREMENT_SCREEN:
                                         InfoStr=getInterFrequencyMeasurementScreen(CpDiagData);
                                break;

                                case CASE_INTERRAT_FREQUENCY_MEASUREMENT_SCREEN:
                                         InfoStr=getInterRATFrequencyMeasurementScreen(CpDiagData);
                                break;

                                case CASE_HSDPA_SCREEN:
                                         InfoStr=getHSDPAScreen(CpDiagData);
                                break;

                                case CASE_RACH_SCREEN:
                                         InfoStr=getRACHScreen(CpDiagData);
                                break;

                                case CASE_CPC_PARAMETERS:
                                         InfoStr=getCPCParameter(CpDiagData);
                                break;

                                case CASE_FAST_DORMANCY:
                                         InfoStr=getFastDormancy(CpDiagData);
                                break;

                                case CASE_RECEIVE_DIVERSITY:
                                         InfoStr=getReceiveDiversity(CpDiagData);
                                break;

                                case CASE_CB_CMAS_PLAN:
                                         InfoStr=getCBCmasPlan(CpDiagData);
                                break;

                                case CASE_AMR:
                                         InfoStr=getAMR(CpDiagData);
                                break;

                                default:
                                log("Unhandled Item="+ mTestItem);
                                break;
                            }

                        long uptime = SystemClock.elapsedRealtime();
                        mElapsedTime.setText(DateUtils.formatElapsedTime(uptime / 1000));
                        if(SimId==SimCardID.ID_ONE.toInt())
                        {
                            mInfo2.setText(InfoStr);
                        }else{
                            mInfo1.setText(InfoStr);
                        }
                        break;
            }
        }
    };


    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        try {
            mTestItem = Integer.parseInt(getIntent().getDataString());
        } catch (NumberFormatException ex) {
            mTestItem = 1; //default test item1
        }

        log("mTestItem="+ mTestItem);
        mRefreshTimeValue=(EngMode.getRefreshTime()*1000);
        log("mRefreshTimeValue="+mRefreshTimeValue);
        if (Utils.isSupportDualSim()) {
            setContentView(R.layout.engmode_casedisplay_brcm);
        } else {
            setContentView(R.layout.engmode_casedisplay);
        }

        mPhone = PhoneFactory.getDefaultPhone(SimCardID.ID_ZERO);
        mPhone2 = PhoneFactory.getDefaultPhone(SimCardID.ID_ONE);
        mSIM1Available=true;
        mSIM2Available=true;


        if (null != mPhone) {
            IccCard iccCard = mPhone.getIccCard();
            if(iccCard.getState() == IccCardConstants.State.READY) {
                if(!iccCard.hasIccCard() || IccCardConstants.State.ABSENT == iccCard.getState()) {
                    mSIM1Available = false;
                }
            } else {
                mSIM1Available = false;
            }
        }

        if (null != mPhone2) {
            IccCard iccCard = mPhone2.getIccCard();
            if(iccCard.getState() == IccCardConstants.State.READY) {
                if(!iccCard.hasIccCard() || IccCardConstants.State.ABSENT == iccCard.getState()) {
                     mSIM2Available = false;
                }
            } else {
                mSIM2Available = false;
            }
        }

        // pass this object as the Context and the BcmDiagListener
        bcmDiagServiceAdapter = new BcmEngModeDiagServiceAdapter(this, this);
    }

    // implement BcmDiagListener methods
    public void onServiceReady() {
    timer = new Timer("CPDiag_Timer");
       mPoller = new TimerTask() {
        @Override
        public void run() {
               log( "Timer task doing work");
                     if(mSIM1Available)
                     {
                            log( "Get SIM1 Data");
                    BcmSummaryEngModeDiag  cpDiagSim1Data =  bcmDiagServiceAdapter.getSummaryDiagData(SimCardID.ID_ZERO.toInt());
                    mHandler.sendMessage(mHandler.obtainMessage( EVENT_SIM1_GET_CPDIAG_RESULT, cpDiagSim1Data));
                     }
                     if(mSIM2Available)
                     {
                            log( "Get SIM2 Data");
                    BcmSummaryEngModeDiag  cpDiagSim2Data =  bcmDiagServiceAdapter.getSummaryDiagData(SimCardID.ID_ONE.toInt());
                    mHandler.sendMessage(mHandler.obtainMessage( EVENT_SIM2_GET_CPDIAG_RESULT, cpDiagSim2Data));
                     }
        };
    };

       if(mRefreshTimeValue!=0)
       {
           timer.schedule( mPoller,100,mRefreshTimeValue);
       }else{
           timer.schedule( mPoller,100);
       }

    log("Service is ready!");
    }

    public void onServiceDown() {
        log( "Service went down purging timer!");
        timer.cancel();
        timer.purge();
        timer = null;
    }
    @Override
    public void onResume() {
        super.onResume();
        log("onResume...");
        mElapsedTime = (TextView)findViewById(R.id.elapsedTime);
        mInfo1 = (TextView)findViewById(R.id.phone1info);
        mInfo2 = (TextView)findViewById(R.id.phone2info);
        mCaseTitle = (TextView)findViewById(R.id.CaseTitle);

        mCaseTitle.setText(R.string.EngineeringMode_item1+mTestItem-1);

         if(!mSIM1Available)
         {
                mInfo1.setText("No SIM1");
         }

         if(!mSIM2Available)
         {
                mInfo2.setText("No SIM2");
         }


         bcmDiagServiceAdapter.ConnectService();
    }

    @Override
    public void onPause() {
        super.onPause();
        log("onPause...");

        mHandler.removeMessages(EVENT_SIM1_GET_CPDIAG_RESULT);
        mHandler.removeMessages(EVENT_SIM2_GET_CPDIAG_RESULT);
        timer.cancel();
        timer.purge();
        bcmDiagServiceAdapter.DisconnectService();

    }

    static void log(String msg) {
        if(DBG) Log.d(LOG_TAG, msg);
    }
}
