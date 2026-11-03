package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Branching Timeline
 * {6}{G}{G}
 * Sorcery
 *
 * Strange new worlds — Reveal the top X cards of your library, where X is the number of
 * differently named lands you control. Put all permanent cards revealed this way onto the
 * battlefield tapped and the rest on the bottom of your library in a random order.
 *
 * "Strange new worlds" is an ability word with no rules meaning.
 *
 * Animist's Awakening's reveal → split → move pipeline with the permanent filter and the TRK
 * "differently named lands" count for X. All permanent cards move in one step, so they enter
 * simultaneously; a short library reveals what there is.
 */
val BranchingTimeline = card("Branching Timeline") {
    manaCost = "{6}{G}{G}"
    typeLine = "Sorcery"
    oracleText = "Strange new worlds — Reveal the top X cards of your library, where X is the number of " +
        "differently named lands you control. Put all permanent cards revealed this way onto the " +
        "battlefield tapped and the rest on the bottom of your library in a random order."

    spell {
        effect = Effects.Pipeline {
            val revealed = gather(
                CardSource.TopOfLibrary(
                    count = DynamicAmounts.differentlyNamedLandsYouControl(),
                    player = Player.You,
                ),
                revealed = true,
            )
            val (permanents, rest) = filterSplit(revealed, GameObjectFilter.Permanent)
            move(
                permanents,
                CardDestination.ToZone(Zone.BATTLEFIELD, Player.You, ZonePlacement.Tapped),
            )
            move(
                rest,
                CardDestination.ToZone(Zone.LIBRARY, Player.You, ZonePlacement.Bottom),
                order = CardOrder.Random,
            )
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "185"
        artist = "Borja Pindado"
        flavorText = "What might have been is the horizon of what could be."
        imageUri = "https://cards.scryfall.io/normal/front/4/a/4a2231ff-677d-4041-9413-b8d683846920.jpg?1793013249"
    }
}
