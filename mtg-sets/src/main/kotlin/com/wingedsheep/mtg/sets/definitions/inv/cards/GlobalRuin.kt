package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.ForEachPlayerEffect
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.effects.SelectionRestriction
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Global Ruin
 * {4}{W}
 * Sorcery
 * Each player chooses from the lands they control a land of each basic land type,
 * then sacrifices the rest.
 *
 * Not a pile card — each player keeps at most one land of each basic land type and
 * sacrifices the remainder. Composed inside [ForEachPlayerEffect]: each player gathers
 * their lands and selects them under the [SelectionRestriction.OnePerBasicLandType]
 * cap (a kept land claims every basic land type it has; a typeless land can't be kept),
 * then the unselected remainder is sacrificed.
 */
val GlobalRuin = card("Global Ruin") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Each player chooses from the lands they control a land of each basic land type, then sacrifices the rest."

    spell {
        effect = ForEachPlayerEffect(
            players = Player.Each,
            effects = Effects.PipelineSteps {
                // 1. Gather the lands this player controls.
                val lands = gather(
                    CardSource.ControlledPermanents(
                        player = Player.You,
                        filter = GameObjectFilter.Land
                    ),
                    name = "lands"
                )
                // 2. This player keeps one land of each basic land type; the rest are the remainder.
                val (kept, sacrificed) = chooseAnyNumberSplit(
                    from = lands,
                    chooser = Chooser.Controller,
                    restrictions = listOf(SelectionRestriction.OnePerBasicLandType),
                    selectedLabel = "Keep",
                    remainderLabel = "Sacrifice",
                    prompt = "Choose a land of each basic land type to keep; the rest are sacrificed.",
                    useTargetingUI = true,
                    alwaysPrompt = true,
                    name = "kept",
                    remainderName = "sacrificed"
                )
                // 3. Sacrifice the rest.
                move(
                    sacrificed,
                    CardDestination.ToZone(Zone.GRAVEYARD),
                    moveType = MoveType.Sacrifice
                )
            }
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "18"
        artist = "Greg Staples"
        imageUri = "https://cards.scryfall.io/normal/front/3/3/336474b4-2cf5-44c0-b72c-f75f1a7ed928.jpg?1562905357"
    }
}
