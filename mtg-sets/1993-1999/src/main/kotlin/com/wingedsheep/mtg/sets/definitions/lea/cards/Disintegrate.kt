package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Disintegrate
 * {X}{R}
 * Sorcery
 * Disintegrate deals X damage to any target. If it's a creature, it can't be regenerated
 * this turn, and if it would die this turn, exile it instead.
 *
 * The markers are applied before the damage so state-based actions see them; per the
 * 2004-10-04 ruling the "can't be regenerated" is the spell's effect, not the damage's,
 * so it applies even if the damage is prevented.
 */
val Disintegrate = card("Disintegrate") {
    manaCost = "{X}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Disintegrate deals X damage to any target. If it's a creature, it can't be regenerated this turn, and if it would die this turn, exile it instead."

    spell {
        val t = target(Targets.Any)
        effect = Effects.CantBeRegenerated(t) then
                Effects.MarkExileOnDeath(t) then
                Effects.DealDamage(DynamicAmounts.xValue(), t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "140"
        artist = "Anson Maddocks"
        imageUri = "https://cards.scryfall.io/normal/front/8/7/8712c49e-f171-4669-bed9-87575a37af11.jpg?1783948688"
        ruling("2004-10-04", "The \"can't regenerate\" is an effect of Disintegrate and not an effect of the damage. It works even if the damage is prevented or redirected. If redirected, the damage does not take this effect with it.")
        ruling("2004-10-04", "Disintegrated creatures do not go to the graveyard at all before being exiled. They do not trigger abilities which trigger due to a creature going to the graveyard.")
    }
}
