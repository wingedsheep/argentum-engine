package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetPlayer

/**
 * Multiple Choice
 * {X}{U}
 * Sorcery
 * If X is 1, scry 1, then draw a card.
 * If X is 2, you may choose a player. They return a creature they control to its owner's hand.
 * If X is 3, create a 4/4 blue and red Elemental creature token.
 * If X is 4 or more, do all of the above.
 *
 * Three resolution-time branches, each gated on the cast X (`xValue()`): branch *n* runs when
 * X is exactly *n* or X is 4 or more, so X ≥ 4 runs all three in printed order and X = 0 does
 * nothing (ruling).
 *
 * "You may choose a player" is not targeting (the spell has no targets — ruling), so it is a
 * `nonTargeting` optional player selection; declining stores nobody and the bounce no-ops. The
 * chosen player then picks a creature *they* control: `ForEachPlayer` over the one-player
 * collection rebinds "you" and the chooser to that player, who returns one of their creatures
 * (a choice, not a target — hexproof doesn't stop it). Choosing yourself is legal.
 */
val MultipleChoice = card("Multiple Choice") {
    manaCost = "{X}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "If X is 1, scry 1, then draw a card.\n" +
        "If X is 2, you may choose a player. They return a creature they control to its owner's hand.\n" +
        "If X is 3, create a 4/4 blue and red Elemental creature token.\n" +
        "If X is 4 or more, do all of the above."

    fun xIsOrFourPlus(n: Int) = Conditions.Any(
        Conditions.CompareAmounts(DynamicAmounts.xValue(), ComparisonOperator.EQ, n),
        Conditions.CompareAmounts(DynamicAmounts.xValue(), ComparisonOperator.GTE, 4),
    )

    spell {
        effect = (
            Effects.If(
                condition = xIsOrFourPlus(1),
                then = Effects.Scry(1) then Effects.DrawCards(1),
            ) then
            Effects.If(
                condition = xIsOrFourPlus(2),
                then = Effects.Pipeline {
                    val chosenPlayer = selectTarget(
                        TargetPlayer(optional = true, descriptionOverride = "a player"),
                        nonTargeting = true,
                        name = "multipleChoicePlayer",
                        prompt = "You may choose a player — they return a creature they control to its owner's hand",
                    )
                    run(
                        Effects.ForEachPlayer(
                            chosenPlayer.asPlayers,
                            Effects.Pipeline {
                                val creatures = gather(
                                    CardSource.BattlefieldMatching(
                                        filter = GameObjectFilter.Creature,
                                        player = Player.You,
                                    ),
                                    name = "multipleChoiceCreatures",
                                )
                                val bounced = chooseExactly(
                                    1,
                                    from = creatures,
                                    prompt = "Return a creature you control to its owner's hand",
                                    useTargetingUI = true,
                                )
                                toHand(bounced)
                            },
                        )
                    )
                },
            ) then
            Effects.If(
                condition = xIsOrFourPlus(3),
                then = Effects.CreateToken(
                    power = 4,
                    toughness = 4,
                    colors = setOf(Color.BLUE, Color.RED),
                    creatureTypes = setOf("Elemental"),
                ),
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "48"
        artist = "Campbell White"
        imageUri = "https://cards.scryfall.io/normal/front/0/8/08c9890f-7081-42f6-89eb-35c83911b443.jpg?1783927376"
        ruling("2021-04-16", "Multiple Choice doesn't have any targets.")
        ruling("2021-04-16", "You can choose 0 as the value of X as you cast Multiple Choice, but it will have no effect when it resolves.")
        ruling("2021-04-16", "If an effect allows you to cast Multiple Choice without paying its mana cost, you must choose 0 as the value of X.")
    }
}
