package com.wingedsheep.mtg.sets.definitions.j22.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Rampaging Growth
 * {3}{G} — Instant (Uncommon) — Jumpstart 2022 #43
 *
 * Search your library for a basic land card, put it onto the battlefield, then shuffle. Until end
 * of turn, that land becomes a 4/3 Insect creature with reach and haste. It's still a land.
 *
 * A library search as an inline pipeline so "that land" can be named: the found card is moved with
 * `moveTracked`, which records the entity that actually reached the battlefield, and the animation
 * runs over that collection. A failed search (CR 701.19b) leaves the collection empty, so nothing
 * is animated. `BecomeCreature` adds the creature type and base P/T without removing the land
 * types — the "it's still a land" clause — and sets no colour.
 */
val RampagingGrowth = card("Rampaging Growth") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Search your library for a basic land card, put it onto the battlefield, then " +
        "shuffle. Until end of turn, that land becomes a 4/3 Insect creature with reach and haste. " +
        "It's still a land."

    spell {
        effect = Effects.Pipeline {
            val searchable = gather(
                CardSource.FromZone(Zone.LIBRARY, Player.You, GameObjectFilter.BasicLand),
                search = true
            )
            val found = chooseUpTo(1, from = searchable)
            val landed = moveTracked(found, CardDestination.ToZone(Zone.BATTLEFIELD))
            run(Effects.ShuffleLibrary())
            run(
                Effects.ForEachInCollection(
                    landed,
                    Effects.BecomeCreature(
                        target = EffectTarget.IterationEntity,
                        power = 4,
                        toughness = 3,
                        keywords = setOf(Keyword.REACH, Keyword.HASTE),
                        creatureTypes = setOf("Insect"),
                        duration = Duration.EndOfTurn
                    )
                )
            )
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "43"
        artist = "Mike Jordana"
        flavorText = "When they had the house checked for bugs, they didn't think big enough."
        imageUri = "https://cards.scryfall.io/normal/front/5/7/57218779-7e18-4fa2-a0bb-c07052951a78.jpg?1783919179"
    }
}
