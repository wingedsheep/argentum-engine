package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Bind the Monster
 * {U}
 * Enchantment — Aura
 * Enchant creature
 * When this Aura enters, tap enchanted creature. It deals damage to you equal to its power.
 * Enchanted creature doesn't untap during its controller's untap step.
 *
 * The enchanted creature is the damage source (`damageSource = EnchantedCreature`), so its
 * lifelink, deathtouch or infect apply to the damage. Power is read as the trigger resolves; if
 * the Aura has left by then, "enchanted creature" names the creature it last enchanted, read by
 * last-known information if that creature is gone too.
 */
val BindTheMonster = card("Bind the Monster") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "When this Aura enters, tap enchanted creature. It deals damage to you equal to its power.\n" +
        "Enchanted creature doesn't untap during its controller's untap step."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Tap(EffectTarget.EnchantedCreature) then Effects.DealDamage(
            amount = DynamicAmounts.enchantedCreaturePower(),
            target = EffectTarget.Controller,
            damageSource = EffectTarget.EnchantedCreature,
        )
        description = "When this Aura enters, tap enchanted creature. It deals damage to you equal to its power."
    }

    staticAbility {
        ability = GrantKeyword(AbilityFlag.DOESNT_UNTAP.name)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "48"
        artist = "Tran Nguyen"
        flavorText = "Sarulf's snarl of rage shook the World Tree."
        imageUri = "https://cards.scryfall.io/normal/front/0/8/080be8d1-e453-4250-b917-f0eb26644895.jpg?1783928268"

        ruling(
            "2021-02-05",
            "The enters-the-battlefield ability triggers even if the enchanted creature is already " +
                "tapped. That creature will still deal damage to you.",
        )
        ruling(
            "2021-02-05",
            "Use the power of the enchanted creature as the enters-the-battlefield ability resolves " +
                "to determine how much damage is dealt.",
        )
        ruling(
            "2021-02-05",
            "If Bind the Monster is no longer on the battlefield as the enters-the-battlefield ability " +
                "resolves, use the power of the creature it was last enchanting to determine how much " +
                "damage is dealt. Damage is dealt even if that creature is also not on the battlefield " +
                "at that time.",
        )
    }
}
