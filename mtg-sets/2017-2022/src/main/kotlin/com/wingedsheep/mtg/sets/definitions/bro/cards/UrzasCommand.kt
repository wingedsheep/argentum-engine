package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Urza's Command
 * {2}{U}{U}
 * Instant
 *
 * Choose two —
 * • Creatures you don't control get -2/-0 until end of turn.
 * • Create a tapped Powerstone token.
 * • Create a tapped 0/0 colorless Construct artifact creature token with "This token gets +1/+1 for
 *   each artifact you control."
 * • Scry 1, then draw a card.
 *
 * "You don't control" is `Not(ControlledByYou)`, not "an opponent controls" — in Two-Headed Giant a
 * teammate's creatures are affected too (Cyclonic Rift's spelling).
 */
val UrzasCommand = card("Urza's Command") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Choose two —\n" +
        "• Creatures you don't control get -2/-0 until end of turn.\n" +
        "• Create a tapped Powerstone token.\n" +
        "• Create a tapped 0/0 colorless Construct artifact creature token with \"This token gets +1/+1 " +
        "for each artifact you control.\"\n" +
        "• Scry 1, then draw a card."

    spell {
        modal(chooseCount = 2) {
            mode("Creatures you don't control get -2/-0 until end of turn") {
                effect = Patterns.Group.modifyStatsForAll(
                    -2,
                    0,
                    GroupFilter(
                        GameObjectFilter.Creature.withControllerPredicate(
                            ControllerPredicate.Not(ControllerPredicate.ControlledByYou)
                        )
                    )
                )
            }
            mode("Create a tapped Powerstone token") {
                effect = Effects.CreatePowerstone(tapped = true)
            }
            mode("Create a tapped 0/0 Construct artifact creature token that gets +1/+1 for each artifact you control") {
                effect = Effects.CreateToken(
                    power = 0,
                    toughness = 0,
                    colors = emptySet(),
                    creatureTypes = setOf("Construct"),
                    artifactToken = true,
                    tapped = true,
                    staticAbilities = listOf(
                        GrantDynamicStats(
                            filter = GroupFilter.source(),
                            powerBonus = DynamicAmounts.battlefield(
                                Player.You,
                                GameObjectFilter.Artifact
                            ).count(),
                            toughnessBonus = DynamicAmounts.battlefield(
                                Player.You,
                                GameObjectFilter.Artifact
                            ).count()
                        )
                    ),
                    imageUri = "https://cards.scryfall.io/normal/front/9/1/914fecab-24c0-4179-84d6-ded78c29134f.jpg?1783919909"
                )
            }
            mode("Scry 1, then draw a card") {
                effect = Effects.Scry(1) then Effects.DrawCards(1)
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "70"
        artist = "Dominik Mayer"
        imageUri = "https://cards.scryfall.io/normal/front/b/9/b9b4a02c-e57f-4287-943e-6bc859ac149e.jpg?1783920102"
        ruling(
            "2022-10-14",
            "The ability of that Construct token does count the Construct itself, so it'll be at least 1/1."
        )
        ruling(
            "2022-10-14",
            "Although all the cards in The Brothers' War that create Powerstone tokens create a tapped " +
                "Powerstone token, entering the battlefield tapped isn't part of the token's definition. " +
                "Notably, if you create a token that is a copy of a Powerstone token, the token copy won't " +
                "enter the battlefield tapped."
        )
    }
}
