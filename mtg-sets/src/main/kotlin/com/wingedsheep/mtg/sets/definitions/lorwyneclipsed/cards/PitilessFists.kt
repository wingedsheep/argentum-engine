package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Pitiless Fists
 * {3}{G}
 * Enchantment — Aura
 * Enchant creature you control
 * When this Aura enters, enchanted creature fights up to one target creature an opponent controls.
 * Enchanted creature gets +2/+2.
 */
val PitilessFists = card("Pitiless Fists") {
    manaCost = "{3}{G}"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature you control\n" +
        "When this Aura enters, enchanted creature fights up to one target creature an opponent controls. " +
        "(Each deals damage equal to its power to the other.)\n" +
        "Enchanted creature gets +2/+2."

    auraTarget = Targets.CreatureYouControl

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val opponentCreature = target(
            "creature an opponent controls",
            TargetCreature(optional = true, filter = TargetFilter.CreatureOpponentControls)
        )
        effect = Effects.Fight(EffectTarget.EnchantedCreature, opponentCreature)
    }

    staticAbility {
        ability = ModifyStats(2, 2)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "187"
        artist = "A. M. Sartor"
        imageUri = "https://cards.scryfall.io/normal/front/2/9/295d828b-b2e7-41c7-afbc-5fb5f4eb242c.jpg?1767872922"
    }
}
