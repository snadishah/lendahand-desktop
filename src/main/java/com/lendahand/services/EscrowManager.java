package com.lendahand.services;

import com.lendahand.controllers.WalletController;
import com.lendahand.dao.TaskDAO;
import com.lendahand.dao.UserDAO;
import com.lendahand.models.Task;
import com.lendahand.models.User;

public class EscrowManager {

    public static boolean holdFunds(int posterID, double amount) {
        User poster = UserDAO.getUserById(posterID);
        if (poster == null || poster.getWalletBalance() < amount) return false;

        UserDAO.updateWallet(posterID, poster.getWalletBalance() - amount);
        WalletController.logTransaction(posterID, "ESCROW_HOLD", amount,
            String.format("Rs. %,.2f held in escrow for accepted bid", amount));
        return true;
    }

    public static void releaseFunds(int taskID) {
        Task task = TaskDAO.getTaskById(taskID);
        if (task == null) return;

        int    helperID = task.getHelperID();
        double amount   = task.getAcceptedAmount();
        User   helper   = UserDAO.getUserById(helperID);
        if (helper == null) return;

        UserDAO.updateWallet(helperID, helper.getWalletBalance() + amount);
        TaskDAO.markDone(taskID);
        WalletController.logTransaction(helperID, "ESCROW_RELEASE", amount,
            String.format("Earned Rs. %,.2f — task #%d completed", amount, taskID));
    }

    public static void refundFunds(int taskID) {
        Task task = TaskDAO.getTaskById(taskID);
        if (task == null) return;

        int    posterID = task.getPosterID();
        double amount   = task.getAcceptedAmount();
        User   poster   = UserDAO.getUserById(posterID);
        if (poster == null) return;

        UserDAO.updateWallet(posterID, poster.getWalletBalance() + amount);
        TaskDAO.cancelTask(taskID);
        WalletController.logTransaction(posterID, "REFUND", amount,
            String.format("Rs. %,.2f refunded — task #%d cancelled", amount, taskID));
    }
}