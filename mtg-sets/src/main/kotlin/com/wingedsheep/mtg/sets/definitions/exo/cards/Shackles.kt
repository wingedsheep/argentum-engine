package com.wingedsheep.mtg.sets.definitions.exo.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Shackles
 * {2}{W}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature doesn't untap during its controller's untap step.
 * {W}: Return this Aura to its owner's hand.
 *
 * "Doesn't untap during its controller's untap step" is granted to the enchanted creature
 * as the DOESNT_UNTAP ability flag; the untap step (BeginningPhaseManager.performUntapStep)
 * skips any permanent whose projected keywords contain it.
 */
val Shackles = card("Shackles") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature doesn't untap during its controller's untap step.\n" +
        "{W}: Return this Aura to its owner's hand."

    auraTarget = Targets.Creature

    staticAbility {
        ability = GrantKeyword(AbilityFlag.DOESNT_UNTAP.name)
    }

    activatedAbility {
        cost = Costs.Mana("{W}")
        effect = MoveToZoneEffect(EffectTarget.Self, Zone.HAND)
        description = "{W}: Return this Aura to its owner's hand."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "18"
        artist = "Heather Hudson"
        imageUri = "https://cards.scryfall.io/normal/front/c/5/c5315668-b8ef-49ab-a8f5-144adc7bcd84.jpg?1562088793"
    }
}
