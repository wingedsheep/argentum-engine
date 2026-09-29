package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Fertilid's Favor
 * {3}{G}
 * Instant
 * Target player searches their library for a basic land card, puts it onto the battlefield tapped,
 * then shuffles. Put two +1/+1 counters on up to one target artifact or creature.
 *
 * The search runs under [Effects.ForEachPlayer] scoped to the targeted player, which rebinds
 * `Player.You` inside [Patterns.Library.searchLibrary] to that player — they search, choose, get
 * the tapped land, and shuffle their own library. The counter target is optional ("up to one").
 */
val FertilidsFavor = card("Fertilid's Favor") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Target player searches their library for a basic land card, puts it onto the " +
        "battlefield tapped, then shuffles. Put two +1/+1 counters on up to one target artifact or creature."

    spell {
        val player = target(Targets.Player)
        val permanent = target(TargetFilter.CreatureOrArtifact, optional = true)
        effect = Effects.ForEachPlayer(
            player.asPlayer,
            Patterns.Library.searchLibrary(
                filter = GameObjectFilter.BasicLand,
                count = 1,
                destination = SearchDestination.BATTLEFIELD,
                entersTapped = true
            )
        ) then Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, permanent)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "186"
        artist = "Kevin Sidharta"
        flavorText = "The denizens of Lorwyn were accustomed to resisting darkness."
        imageUri = "https://cards.scryfall.io/normal/front/e/0/e0bbfb58-8b22-4e8d-991c-855c29964d94.jpg?1783916969"
    }
}
