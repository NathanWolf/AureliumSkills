package com.archyx.aureliumskills.skills.abilities;

import com.archyx.aureliumskills.AureliumSkills;
import com.archyx.aureliumskills.Options;
import com.archyx.aureliumskills.skills.PlayerSkill;
import com.archyx.aureliumskills.skills.Skill;
import com.archyx.aureliumskills.skills.SkillLoader;
import com.archyx.aureliumskills.skills.Source;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Random;

public class ArcheryAbilities implements Listener {

    private final Random r = new Random();
    private final Plugin plugin;

    public ArcheryAbilities(Plugin plugin) {
        this.plugin = plugin;
    }

    public static double getModifiedXp(Player player, Source source) {
        PlayerSkill skill = SkillLoader.playerSkills.get(player.getUniqueId());
        double output = Options.getXpAmount(source);
        if (AureliumSkills.abilityOptionManager.isEnabled(Ability.ARCHER)) {
            double modifier = 1;
            modifier += Ability.ARCHER.getValue(skill.getAbilityLevel(Ability.ARCHER)) / 100;
            output *= modifier;
        }
        return output;
    }

    public void applyCrit(EntityDamageByEntityEvent event, PlayerSkill playerSkill, Player player) {
        if (Critical.isCrit(playerSkill)) {
            event.setDamage(event.getDamage() * Critical.getCritMultiplier(playerSkill));
            player.setMetadata("skillsCritical", new FixedMetadataValue(plugin, true));
            new BukkitRunnable() {
                @Override
                public void run() {
                    player.removeMetadata("skillsCritical", plugin);
                }
            }.runTaskLater(plugin, 1L);
        }
    }

    public void bowMaster(EntityDamageByEntityEvent event, PlayerSkill playerSkill) {
        double multiplier = 1 + (Ability.BOW_MASTER.getValue(playerSkill.getAbilityLevel(Ability.BOW_MASTER)) / 100);
        event.setDamage(event.getDamage() * multiplier);
    }

    public void stun(PlayerSkill playerSkill, LivingEntity entity) {
        if (r.nextDouble() < (Ability.STUN.getValue(playerSkill.getAbilityLevel(Ability.STUN)) / 100)) {
            if (entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED) != null) {
                AttributeInstance speed = entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
                if (speed != null) {
                    //Applies stun
                    double reducedSpeed = speed.getValue() * 0.2;
                    AttributeModifier modifier = new AttributeModifier("AureliumSkills-Stun", -1 * reducedSpeed, AttributeModifier.Operation.ADD_NUMBER);
                    speed.addModifier(modifier);
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            AttributeInstance newSpeed = entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
                            if (newSpeed != null) {
                                for (AttributeModifier attributeModifier : newSpeed.getModifiers()) {
                                    if (attributeModifier.getName().equals("AureliumSkills-Stun")) {
                                        newSpeed.removeModifier(attributeModifier);
                                    }
                                }
                            }
                        }
                    }.runTaskLater(plugin, 40L);
                }
            }
        }
    }

    public void piercing(EntityDamageByEntityEvent event, PlayerSkill playerSkill, Player player, Arrow arrow) {
        if (r.nextDouble() < (Ability.PIERCING.getValue(playerSkill.getAbilityLevel(Ability.PIERCING)) / 100)) {
            arrow.setBounce(false);
            Vector velocity = arrow.getVelocity();
            Arrow newArrow = event.getEntity().getWorld().spawnArrow(arrow.getLocation(), velocity, (float) velocity.length(), 0.0f);
            newArrow.setShooter(player);
            newArrow.setKnockbackStrength(arrow.getKnockbackStrength());
            newArrow.setFireTicks(arrow.getFireTicks());
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void archeryListener(EntityDamageByEntityEvent event) {
        if (Options.isEnabled(Skill.ARCHERY)) {
            if (!event.isCancelled()) {
                if (event.getDamager() instanceof Arrow) {
                    Arrow arrow = (Arrow) event.getDamager();
                    if (arrow.getShooter() instanceof Player) {
                        Player player = (Player) arrow.getShooter();
                        //Check disabled worlds
                        if (AureliumSkills.worldManager.isInDisabledWorld(player.getLocation())) {
                            return;
                        }
                        //Check for permission
                        if (!player.hasPermission("aureliumskills.archery")) {
                            return;
                        }
                        //Applies abilities
                        if (SkillLoader.playerSkills.containsKey(player.getUniqueId())) {
                            PlayerSkill playerSkill = SkillLoader.playerSkills.get(player.getUniqueId());
                            AbilityOptionManager options = AureliumSkills.abilityOptionManager;
                            if (options.isEnabled(Ability.BOW_MASTER)) {
                                bowMaster(event, playerSkill);
                            }
                            if (options.isEnabled(Ability.CRIT_CHANCE)) {
                                applyCrit(event, playerSkill, player);
                            }
                            if (options.isEnabled(Ability.STUN)) {
                                stun(playerSkill, player);
                            }
                            if (options.isEnabled(Ability.PIERCING)) {
                                piercing(event, playerSkill, player, arrow);
                            }
                        }
                    }
                }
            }
        }
    }

}