package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.core.Zone

/**
 * Lingering Death
 * {1}{B}
 * Enchantment — Aura
 * Enchant creature
 * At the beginning of the end step of enchanted creature's controller, that player sacrifices that creature.
 */
val LingeringDeath = card("Lingering Death") {
    manaCost = "{1}{B}"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nAt the beginning of the end step of enchanted creature's controller, that player sacrifices that creature."

    auraTarget = Targets.Creature

    triggeredAbility {
        trigger = Triggers.EnchantedCreatureControllerEndStep
        effect = MoveToZoneEffect(
            target = EffectTarget.EnchantedCreature,
            destination = Zone.GRAVEYARD
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "69"
        artist = "Matt Thompson"
        flavorText = "\"Looks bad. I don't know if he'll make it through the night.\" —Cabal cleric"
        imageUri = "https://cards.scryfall.io/normal/front/f/1/f174fd76-f28d-4272-8cb0-7f66cd60579e.jpg?1562536737"
    }
}
