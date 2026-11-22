package com.wingedsheep.mtg.sets.definitions.m11.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.predicates.StatePredicate
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Elixir of Immortality
 * {1}
 * Artifact
 * {2}, {T}: You gain 5 life. Shuffle this artifact and your graveyard into their owner's library.
 *
 * The artifact (only while it is still this object on the battlefield) and every card in your
 * graveyard are gathered into one collection and moved in a single shuffled move: each card goes to
 * its owner's library and every affected library is shuffled once — a stolen Elixir goes home to
 * its owner's library while your graveyard goes into yours (the card's rulings). An Elixir already
 * in your graveyard is shuffled in as part of the graveyard; one anywhere else stays put.
 */
val ElixirOfImmortality = card("Elixir of Immortality") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{2}, {T}: You gain 5 life. Shuffle this artifact and your graveyard into their owner's library."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap)
        effect = Effects.Pipeline {
            run(Effects.GainLife(5))
            val elixirAndGraveyard = gather(
                CardSource.FromMultipleZones(
                    zones = listOf(Zone.BATTLEFIELD, Zone.GRAVEYARD),
                    player = Player.You,
                    filter = GameObjectFilter.Any.withStatePredicate(
                        StatePredicate.Or(listOf(StatePredicate.IsSource, StatePredicate.InZone(Zone.GRAVEYARD)))
                    ),
                )
            )
            move(
                elixirAndGraveyard,
                CardDestination.ToZone(Zone.LIBRARY, Player.You, ZonePlacement.Shuffled),
            )
        }
        description = "{2}, {T}: You gain 5 life. Shuffle this artifact and your graveyard into their owner's library."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "206"
        artist = "Zoltan Boros & Gabor Szikszai"
        flavorText = "\"Bottled life. Not as tasty as I'm used to, rather stale, but it has the same effect.\"\n—Baron Sengir"
        imageUri = "https://cards.scryfall.io/normal/front/9/9/99bd4740-9b1f-40a6-a14d-2c0d642b848b.jpg?1783941791"
        ruling(
            "2011-09-22",
            "As the ability resolves, you’ll shuffle Elixir of Immortality into its owner’s library directly " +
                "from the battlefield, if it’s still there.",
        )
        ruling(
            "2011-09-22",
            "If Elixir of Immortality is in your graveyard at the time the ability resolves, you’ll still wind " +
                "up shuffling it into your library because you shuffle your entire graveyard into your library. " +
                "If it’s anywhere else by that time, including in another player’s graveyard, it remains where " +
                "it is and you shuffle just your graveyard into your library.",
        )
        ruling(
            "2011-09-22",
            "If you gain control of another player’s Elixir of Immortality and activate it, the Elixir of " +
                "Immortality will be shuffled into its owner’s library and the cards in your graveyard will be " +
                "shuffled into your library. You are considered to have shuffled each affected library (even " +
                "if, as a shortcut, each player physically shuffles their own library).",
        )
        ruling(
            "2010-08-15",
            "Paying the activation cost of Elixir of Immortality’s ability doesn’t cause it to leave the " +
                "battlefield. If you have a way to untap it, you can activate the ability multiple times in " +
                "response to itself.",
        )
    }
}
