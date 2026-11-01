package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardLayout
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Consecrate // Consume (RNA 224) — split-layout spell (CR 709).
 *
 * Consecrate {1}{W/B} — Instant
 *   Exile target card from a graveyard.
 *   Draw a card.
 *
 * Consume {2}{W}{B} — Sorcery
 *   Target player sacrifices a creature with the greatest power among creatures they control.
 *   You gain life equal to its power.
 *
 * Consume's life gain reads the sacrificed creature's last-known power from the
 * sacrifice snapshot (the Tribute to Hunger shape); the sacrificing player picks among ties.
 */
val ConsecrateConsume = card("Consecrate // Consume") {
    layout = CardLayout.SPLIT
    colorIdentity = "WB"

    face("Consecrate") {
        manaCost = "{1}{W/B}"
        typeLine = "Instant"
        oracleText = "Exile target card from a graveyard.\nDraw a card."

        spell {
            val card = target(TargetFilter.CardInGraveyard)
            effect = Effects.Move(card, Zone.EXILE) then Effects.DrawCards(1)
        }
    }

    face("Consume") {
        manaCost = "{2}{W}{B}"
        typeLine = "Sorcery"
        oracleText = "Target player sacrifices a creature with the greatest power among creatures they control. " +
            "You gain life equal to its power."

        spell {
            val player = target(Targets.Player)
            effect = Effects.Sacrifice(
                filter = GameObjectFilter.Creature.hasGreatestPower(),
                count = 1,
                target = player
            ) then Effects.GainLife(DynamicAmounts.sacrificedPower(0))
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "224"
        artist = "Igor Kieryluk"
        imageUri = "https://cards.scryfall.io/normal/front/0/0/00320106-ce51-46a9-b0f9-79b3baf4e505.jpg?1783933627"
    }
}
