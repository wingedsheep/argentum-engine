package com.wingedsheep.mtg.sets.definitions.cmr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.CollectionFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Szat's Will — Commander Legends #152 (canonical printing)
 * {4}{B} · Instant
 *
 * Choose one. If you control a commander as you cast this spell, you may choose both instead.
 * • Each opponent sacrifices a creature they control with the greatest power.
 * • Exile all opponents' graveyards, then create X 0/1 black Thrull creature tokens, where X is the
 *   greatest power among creature cards exiled this way.
 *
 * The conditional mode count is Jeska's Will's: floor 1, cap 2 when [Conditions.YouControlACommander]
 * holds as the spell is cast.
 *
 * Mode 1 is Crackling Doom's edict — each opponent narrows their own creatures to the greatest
 * power, then chooses one of the ties to sacrifice.
 *
 * Mode 2 reads X *before* exiling (the cards are only in the graveyards until then) as the
 * greatest printed power among creature cards in all opponents' graveyards, which is exactly the
 * set `ExileOpponentsGraveyards` then exiles. With no creature card among them X is 0 and no
 * tokens are created.
 */
val SzatsWill = card("Szat's Will") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Choose one. If you control a commander as you cast this spell, you may choose both instead.\n" +
        "• Each opponent sacrifices a creature they control with the greatest power.\n" +
        "• Exile all opponents' graveyards, then create X 0/1 black Thrull creature tokens, where X is the greatest power among creature cards exiled this way."

    spell {
        modal(
            chooseCount = 2,
            minChooseCount = 1,
            dynamicChooseCount = DynamicAmounts.conditional(
                condition = Conditions.YouControlACommander,
                ifTrue = 2,
                ifFalse = 1
            )
        ) {
            mode("Each opponent sacrifices a creature they control with the greatest power") {
                effect = Effects.ForEachPlayer(
                    players = Player.EachOpponent,
                    Effects.Pipeline {
                        val creatures = gather(CardSource.ControlledPermanents(Player.You, GameObjectFilter.Creature))
                        val greatest = filter(creatures, CollectionFilter.GreatestPower)
                        val toSacrifice = chooseExactly(
                            1,
                            from = greatest,
                            chooser = Chooser.Controller,
                            prompt = "Choose a creature with the greatest power to sacrifice"
                        )
                        sacrifice(toSacrifice)
                    }
                )
            }
            mode("Exile all opponents' graveyards, then create X 0/1 black Thrull creature tokens, where X is the greatest power among creature cards exiled this way") {
                effect = Effects.Pipeline {
                    val x = storeNumber(
                        DynamicAmounts.zone(Player.EachOpponent, Zone.GRAVEYARD, GameObjectFilter.Creature).maxPower()
                    )
                    run(Effects.ExileOpponentsGraveyards())
                    run(
                        Effects.CreateToken(
                            count = x.amount,
                            power = 0,
                            toughness = 1,
                            colors = setOf(Color.BLACK),
                            creatureTypes = setOf("Thrull"),
                            imageUri = "https://cards.scryfall.io/normal/front/b/9/b9f3042b-784c-4006-9bf1-60a323e60c5c.jpg?1783928591",
                        )
                    )
                }
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "152"
        artist = "Sidharth Chaturvedi"
        imageUri = "https://cards.scryfall.io/normal/front/9/8/9828be7b-1eae-4718-9e71-2f625121b00b.jpg?1783928826"
    }
}
