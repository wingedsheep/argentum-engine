package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Kayla's Command
 * {1}{W}{W}
 * Sorcery
 *
 * Choose two —
 * • Create a 2/2 colorless Construct artifact creature token.
 * • Put a +1/+1 counter on a creature you control. It gains double strike until end of turn.
 * • Search your library for a basic Plains card, reveal it, put it into your hand, then shuffle.
 * • You gain 2 life and scry 2.
 *
 * None of the modes target (2022-10-14 ruling): the creature for the second mode is chosen on
 * resolution, so it's a non-targeting `selectTarget` inside a pipeline (Season of Gathering's
 * shape) rather than a cast-time `target(...)`.
 */
val KaylasCommand = card("Kayla's Command") {
    manaCost = "{1}{W}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Choose two —\n" +
        "• Create a 2/2 colorless Construct artifact creature token.\n" +
        "• Put a +1/+1 counter on a creature you control. It gains double strike until end of turn.\n" +
        "• Search your library for a basic Plains card, reveal it, put it into your hand, then shuffle.\n" +
        "• You gain 2 life and scry 2."

    spell {
        modal(chooseCount = 2) {
            mode("Create a 2/2 colorless Construct artifact creature token") {
                effect = Effects.CreateToken(
                    power = 2,
                    toughness = 2,
                    creatureTypes = setOf("Construct"),
                    artifactToken = true,
                    imageUri = "https://cards.scryfall.io/normal/front/0/8/087ab398-474f-4455-bef4-ea42c6913486.jpg?1783919909"
                )
            }
            mode("Put a +1/+1 counter on a creature you control. It gains double strike until end of turn") {
                effect = Effects.Pipeline {
                    val chosen = selectTarget(
                        TargetObject(filter = TargetFilter.CreatureYouControl),
                        nonTargeting = true,
                        prompt = "Choose a creature you control to get a +1/+1 counter and double strike"
                    )
                    run(Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, chosen.asTarget))
                    run(Effects.GrantKeyword(Keyword.DOUBLE_STRIKE, chosen.asTarget))
                }
            }
            mode("Search your library for a basic Plains card, reveal it, put it into your hand, then shuffle") {
                effect = Patterns.Library.searchLibrary(
                    filter = GameObjectFilter.BasicLand.withSubtype(Subtype.PLAINS),
                    count = 1,
                    destination = SearchDestination.HAND,
                    reveal = true,
                    shuffleAfter = true
                )
            }
            mode("You gain 2 life and scry 2") {
                effect = Effects.GainLife(2) then Effects.Scry(2)
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "9"
        artist = "Dominik Mayer"
        imageUri = "https://cards.scryfall.io/normal/front/0/7/0737dbec-1646-41a0-adbf-706c9bd4fc7a.jpg?1783920132"
        ruling(
            "2022-10-14",
            "None of the modes have any targets. Notably, this means that you don't have to choose a " +
                "creature for the second mode until the spell resolves."
        )
    }
}
