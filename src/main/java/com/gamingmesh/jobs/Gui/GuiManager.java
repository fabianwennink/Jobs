package com.gamingmesh.jobs.Gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import com.gamingmesh.jobs.Jobs;
import com.gamingmesh.jobs.container.ActionType;
import com.gamingmesh.jobs.container.Boost;
import com.gamingmesh.jobs.container.CurrencyType;
import com.gamingmesh.jobs.container.Job;
import com.gamingmesh.jobs.container.JobInfo;
import com.gamingmesh.jobs.container.JobProgression;
import com.gamingmesh.jobs.container.JobsPlayer;

public class GuiManager {

    public HashMap<UUID, GuiInfoList> GuiList = new HashMap<>();

    private List<Integer> whiteList = Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 45, 46, 47, 48, 49, 50, 51, 52, 53);
    private List<Integer> yellowList = Arrays.asList(9, 17, 18, 26, 27, 35, 36, 44);
    private List<Integer> orangeList = Arrays.asList(9, 17, 18, 26, 27, 35, 36, 44);

    public void CloseInventories() {
        for (Entry<UUID, GuiInfoList> one : GuiList.entrySet()) {
            Player player = Bukkit.getPlayer(one.getKey());
            if (player != null) {
                player.closeInventory();
            }
        }
    }

    public boolean isInGui(Player player) {
        return GuiList.containsKey(player.getUniqueId());
    }

    public Job getJobBySlot(Player player, int slot) {
        GuiInfoList info = GuiList.get(player.getUniqueId());
        return info.getJobList().get(slot);
    }

    public Inventory CreateJobsGUI(Player player) {

        ArrayList<Job> JobsList = new ArrayList<>();
        for (Job job : Jobs.getJobs()) {
            if (Jobs.getGCManager().getHideJobsWithoutPermission())
                if (!Jobs.getCommandManager().hasJobPermission(player, job))
                    continue;
            JobsList.add(job);
        }

        GuiInfoList guiInfo = new GuiInfoList(player.getName());

        if (!this.isInGui(player))
            player.closeInventory();

        GuiList.put(player.getUniqueId(), guiInfo);

        int GuiSize = Jobs.getGCManager().getJobsGUIRows() * 9;

        JobsPlayer JPlayer = Jobs.getPlayerManager().getJobsPlayer(player);

        List<JobProgression> pJobs = JPlayer.getJobProgression();

        String title = Jobs.getLanguage().getMessage("command.info.gui.pickjob");
        if (title.length() > 32)
            title = title.substring(0, 30) + "..";

        int neededSlots = JobsList.size() + ((JobsList.size() / Jobs.getGCManager().getJobsGUIGroupAmount()) * Jobs.getGCManager().getJobsGUISkipAmount()) + Jobs.getGCManager().getJobsGUIStartPosition();
        int neededRows = (int) Math.ceil(neededSlots / 9D);

        // Resizing GUI in case we have more jobs then we could fit in current setup
        GuiSize = Jobs.getGCManager().getJobsGUIRows() > neededRows ? GuiSize : neededRows * 9;

        // Lets avoid oversized GUI
        GuiSize = GuiSize > 54 ? 54 : GuiSize;

        Inventory GuiInv = Bukkit.createInventory(null, GuiSize, title);

        int i = 0;
        int pos = Jobs.getGCManager().getJobsGUIStartPosition() - 1;

        // Changing start position to 0 in case we have more jobs then we can fit in current setup
        pos = JobsList.size() > 28 ? JobsList.size() <= 42 ? 0 : -1 : pos;
        int group = 0;
        main:
        for (int z = 0; z < JobsList.size(); z++) {
            group++;

            if (group > Jobs.getGCManager().getJobsGUIGroupAmount()) {
                group = 1;
                // Only add skip if we can fit all of them in max sized Gui
                if (JobsList.size() <= 42) {
                    pos += Jobs.getGCManager().getJobsGUISkipAmount();

                }
            }

//	    pos += 2;
//	    for (int x = 1; x <= 7; x++) {
            pos++;
            if (i >= JobsList.size())
                break main;
            Job job = JobsList.get(i);

            ArrayList<String> Lore = new ArrayList<>();

            for (JobProgression onePJob : pJobs) {
                if (onePJob.getJob().getName().equalsIgnoreCase(job.getName()))
                    Lore.add(Jobs.getLanguage().getMessage("command.info.gui.working"));
                Lore.add("");
            }

            int maxlevel = job.getMaxLevel(JPlayer);
            if (maxlevel > 0)
                Lore.add(Jobs.getLanguage().getMessage("command.info.gui.max") + maxlevel);

            if (Jobs.getGCManager().ShowTotalWorkers)
                Lore.add(Jobs.getLanguage().getMessage("command.browse.output.totalWorkers", "[amount]", job.getTotalPlayers()));

            if (Jobs.getGCManager().useDynamicPayment && Jobs.getGCManager().ShowPenaltyBonus)
                if (job.getBonus() < 0)
                    Lore.add(Jobs.getLanguage().getMessage("command.browse.output.penalty", "[amount]", (int) (job.getBonus() * 100) * -1));
                else
                    Lore.add(Jobs.getLanguage().getMessage("command.browse.output.bonus", "[amount]", (int) (job.getBonus() * 100)));

            Lore.addAll(Arrays.asList(job.getDescription().split("/n")));

            if (job.getMaxSlots() != null)
                Lore.add(Jobs.getLanguage().getMessage("command.info.gui.leftSlots") + ((job.getMaxSlots() - Jobs.getUsedSlots(job)) > 0 ? (job.getMaxSlots() - Jobs
                    .getUsedSlots(job)) : 0));

            Lore.add(Jobs.getLanguage().getMessage("command.info.gui.actions"));
            for (ActionType actionType : ActionType.values()) {
                List<JobInfo> info = job.getJobInfo(actionType);
                if (info != null && !info.isEmpty()) {
                    Lore.add(Jobs.getLanguage().getMessage("command.info.output." + actionType.getName().toLowerCase() + ".info"));
                }
            }

            Lore.add("");
            Lore.add(Jobs.getLanguage().getMessage("command.info.gui.leftClick"));
            Lore.add(Jobs.getLanguage().getMessage("command.info.gui.rightClick"));

            ItemStack GuiItem = job.getGuiItem();

            ItemMeta meta = GuiItem.getItemMeta();

            String name = job.getName().toLowerCase();
            String lowerName = name.substring(0, 1).toUpperCase() + name.substring(1);

            meta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "" + lowerName);
            meta.setLore(Lore);
            GuiItem.setItemMeta(meta);

            guiInfo.addJob(pos, job);
            GuiInv.setItem(pos, GuiItem);
            i++;
//	    }

        }

        styleInventory(GuiInv, GuiInv.getSize());

        guiInfo.setInv(GuiInv);
        return GuiInv;
    }

    public Inventory CreateJobsSubGUI(Player player, Job job) {

        Inventory tempInv = Bukkit.createInventory(null, 54, "");

        ItemStack GuiItem = job.getGuiItem();
        JobsPlayer JPlayer = Jobs.getPlayerManager().getJobsPlayer(player);

        Boost boost = Jobs.getPlayerManager().getFinalBonus(JPlayer, job);

        int level = 1;
        JobProgression prog = JPlayer.getJobProgression(job);
        if (prog != null)
            level = prog.getLevel();

        int numjobs = JPlayer.getJobProgression().size();

        List<ItemStack> items = new ArrayList<>();
        int i = 0;
        for (ActionType actionType : ActionType.values()) {
            List<JobInfo> info = job.getJobInfo(actionType);

            if (info == null || info.isEmpty())
                continue;

            ArrayList<String> Lore = new ArrayList<>();
            Lore.add(Jobs.getLanguage().getMessage("command.info.output." + actionType.getName().toLowerCase() + ".info"));
            int y = 1;
            for (int z = 0; z < info.size(); z++) {

                String itemName = info.get(z).getRealisticName();

                double income = info.get(z).getIncome(level, numjobs);

                income = boost.getFinalAmount(CurrencyType.MONEY, income);
                String incomeColor = income >= 0 ? "" : ChatColor.DARK_RED.toString();

                double xp = info.get(z).getExperience(level, numjobs);
                xp = boost.getFinalAmount(CurrencyType.EXP, xp);
                String xpColor = xp >= 0 ? "" : ChatColor.GRAY.toString();

                double points = info.get(z).getPoints(level, numjobs);
                points = boost.getFinalAmount(CurrencyType.POINTS, points);
                String pointsColor = xp >= 0 ? "" : ChatColor.RED.toString();

                if (income == 0D && points == 0D && xp == 0D)
                    continue;

                String val = "";

                if (income != 0.0)
                    val += Jobs.getLanguage().getMessage("command.info.help.money", "%money%", incomeColor + String.format(Jobs.getGCManager().getDecimalPlacesMoney(), income));

                if (points != 0.0)
                    val += Jobs.getLanguage().getMessage("command.info.help.points", "%points%", pointsColor + String.format(Jobs.getGCManager().getDecimalPlacesPoints(), points));

                if (xp != 0.0)
                    val += Jobs.getLanguage().getMessage("command.info.help.exp", "%exp%", xpColor + String.format(Jobs.getGCManager().getDecimalPlacesExp(), xp));

                Lore.add(Jobs.getLanguage().getMessage("command.info.help.material", "%material%", itemName) + val);

                if (y >= 10) {
                    y = 1;

                    if (z == info.size() - 1)
                        continue;
                    ItemMeta meta = GuiItem.getItemMeta();
                    String name = job.getName().toLowerCase();
                    String lowerName = name.substring(0, 1).toUpperCase() + name.substring(1);

                    meta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "" + lowerName);
                    meta.setLore(Lore);
                    GuiItem.setItemMeta(meta);
                    //GuiInv.setItem(i, GuiItem);
                    tempInv.setItem(i, GuiItem);

                    GuiItem = job.getGuiItem();
                    Lore = new ArrayList<>();
                    Lore.add(Jobs.getLanguage().getMessage("command.info.output." + actionType.getName().toLowerCase() + ".info"));
                    i++;
                }
                y++;
            }
            ItemMeta meta = GuiItem.getItemMeta();

            String name = job.getName().toLowerCase();
            String lowerName = name.substring(0, 1).toUpperCase() + name.substring(1);

            meta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "" + lowerName);
            meta.setLore(Lore);
            GuiItem.setItemMeta(meta);
            //GuiInv.setItem(i, GuiItem);
            tempInv.setItem(i, GuiItem);
            i++;
        }

        for (ItemStack one : tempInv.getContents()) {
            if (one != null)
                items.add(one);
        }

        GuiInfoList mainGui = GuiList.get(player.getUniqueId());
        if (mainGui == null) {
            CreateJobsGUI(player);
            mainGui = GuiList.get(player.getUniqueId());
        }

        int GuiSize = mainGui != null && mainGui.getInv() != null ? mainGui.getInv().getSize() : Jobs.getGCManager().getJobsGUIRows() * 9;
        int backButton = Jobs.getGCManager().getJobsGUIBackButton();

        String title = Jobs.getLanguage().getMessage("command.info.gui.jobinfo", "[jobname]", job.getName());
        if (title.length() > 32)
            title = title.substring(0, 30) + "..";
        Inventory GuiInv = Bukkit.createInventory(null, GuiSize, title);

        styleInventory(GuiInv, GuiInv.getSize());

        int pos = 0;
        for (int i1 = 0; i1 < items.size(); i1++) {
            if (i1 % 7 == 0 && i1 != 0) pos += 2;

            GuiInv.setItem((i1 + 10) + pos, items.get(i1));
        }

        ItemStack skull = Jobs.getGCManager().guiBackButton;

        ItemMeta skullMeta = skull.getItemMeta();
        skullMeta.setDisplayName(Jobs.getLanguage().getMessage("command.info.gui.back"));

        skull.setItemMeta(skullMeta);

        GuiInv.setItem(backButton, skull);

        GuiInfoList guiInfo = new GuiInfoList(player.getName());
        guiInfo.setJobInfo(true);
        guiInfo.setbackButton(backButton);
        GuiList.put(player.getUniqueId(), guiInfo);

        // Back button
        ItemStack backbutton = Jobs.getGCManager().guiBackButton;

        ItemMeta backbuttonMeta = backbutton.getItemMeta();
        backbuttonMeta.setDisplayName(Jobs.getLanguage().getMessage("command.info.gui.back"));

        backbutton.setItemMeta(backbuttonMeta);

        GuiInv.setItem(backButton, backbutton);

        return GuiInv;
    }

    /**
     * Fills the border of the given inventory with the glass placeholders.
     *
     * @param inventory     The inventory.
     * @param inventorySize The inventory size.
     */
    public static void styleInventory(Inventory inventory, int inventorySize) {
        int rows = inventorySize / 9;

        // Top line of the inventory.
        for (int slot = 0; slot < 9; slot++) {
            if (slot < 2 || slot > 6) {
                inventory.setItem(slot, Jobs.getGCManager().guiOrangeFiller);
            } else {
                inventory.setItem(slot, Jobs.getGCManager().guiYellowFiller);
            }
        }

        // Bottom line of the inventory.
        for (int slot = (inventorySize - 9); slot < inventorySize; slot++) {
            if (slot < (inventorySize - 7) || slot > (inventorySize - 3)) {
                inventory.setItem(slot, Jobs.getGCManager().guiOrangeFiller);
            } else {
                inventory.setItem(slot, Jobs.getGCManager().guiYellowFiller);
            }
        }

        // Lef, right and middle lines of the inventory.
        for (int row = 1; row < rows - 1; row++) {
            int leftSlot = 9 * row;
            int rightSlot = leftSlot + 8;

            // Left and right
            inventory.setItem(leftSlot, Jobs.getGCManager().guiWhiteFiller);
            inventory.setItem(rightSlot, Jobs.getGCManager().guiWhiteFiller);
        }
    }
}
