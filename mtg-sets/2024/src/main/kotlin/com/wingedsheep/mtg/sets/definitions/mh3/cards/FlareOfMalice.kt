package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.SelfAlternativeCost
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.CollectionFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Flare of Malice {2}{B}{B}
 * Instant
 *
 * You may sacrifice a nontoken black creature rather than pay this spell's mana cost.
 * Each opponent sacrifices a creature or planeswalker with the greatest mana value among
 * creatures and planeswalkers they control.
 *
 * Each opponent in turn gathers their own creatures and planeswalkers, narrows them to those tied
 * for the greatest mana value, and picks which tied permanent to sacrifice (inside
 * [Effects.ForEachPlayer] the iterated opponent is the controller).
 */
val FlareOfMalice = card("Flare of Malice") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "You may sacrifice a nontoken black creature rather than pay this spell's mana cost.\n" +
        "Each opponent sacrifices a creature or planeswalker with the greatest mana value among " +
        "creatures and planeswalkers they control."

    selfAlternativeCost = SelfAlternativeCost(
        manaCost = ManaCost.parse("{0}"),
        additionalCosts = listOf(
            Costs.additional.SacrificePermanent(GameObjectFilter.Creature.withColor(Color.BLACK).nontoken())
        )
    )

    spell {
        effect = Effects.ForEachPlayer(
            players = Player.EachOpponent,
            Effects.Pipeline {
                val candidates = gather(
                    CardSource.ControlledPermanents(Player.You, GameObjectFilter.CreatureOrPlaneswalker)
                )
                val greatest = filter(candidates, CollectionFilter.GreatestManaValue)
                val sacrificed = chooseExactly(
                    1,
                    from = greatest,
                    chooser = Chooser.Controller,
                    prompt = "Choose a creature or planeswalker with the greatest mana value to sacrifice",
                    useTargetingUI = true
                )
                sacrifice(sacrificed)
            }
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "95"
        artist = "David Palumbo"
        flavorText = "\"Suffer as I have suffered. Lose as I have lost.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/9/19efb9ce-62eb-4cbf-b01e-979f3fd09ba6.jpg?1783911280"
    }
}
