package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.effects.SacrificeSelfEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * See Red — Rivals of Ixalan #112 (canonical printing)
 * {2}{R} · Enchantment — Aura
 *
 * Enchant creature
 * Enchanted creature gets +2/+1 and has first strike.
 * At the beginning of your end step, if you didn't attack with a creature this turn, sacrifice
 * this Aura.
 *
 * The sacrifice is the Curious Obsession shape: an intervening "if" on the Aura's own end-step
 * trigger, read off the controller's "attacked this turn" marker — any creature attacking
 * satisfies it, not just the enchanted one.
 */
val SeeRed = card("See Red") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature gets +2/+1 and has first strike.\n" +
        "At the beginning of your end step, if you didn't attack with a creature this turn, " +
        "sacrifice this Aura."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = ModifyStats(2, 1)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.FIRST_STRIKE)
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        interveningIf = Conditions.Not(Conditions.YouAttackedThisTurn)
        effect = SacrificeSelfEffect
        description = "At the beginning of your end step, if you didn't attack with a creature " +
            "this turn, sacrifice this Aura."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "112"
        artist = "Steve Prescott"
        imageUri = "https://cards.scryfall.io/normal/front/4/6/4686cbaf-1de8-4f30-8027-c6c8a85dde7a.jpg?1783935294"
    }
}
