package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CounterTargetSource

/**
 * Nether Void
 * {3}{B}
 * World Enchantment
 *
 * Whenever a player casts a spell, counter it unless that player pays {3}.
 *
 * Symmetric: its own controller's spells are taxed too. The payer is the triggering spell's
 * caster, not the Void's controller.
 */
val NetherVoid = card("Nether Void") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "World Enchantment"
    oracleText = "Whenever a player casts a spell, counter it unless that player pays {3}."

    triggeredAbility {
        trigger = Triggers.anyPlayer.casts()
        effect = Effects.CounterUnlessPays("{3}", targetSource = CounterTargetSource.TriggeringEntity)
        description = "Whenever a player casts a spell, counter it unless that player pays {3}."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "113"
        artist = "Harold McNeill"
        imageUri = "https://cards.scryfall.io/normal/front/2/e/2e72f8cb-5bc3-4711-9b7c-a6eea9a0beaf.jpg?1783948063"
    }
}
