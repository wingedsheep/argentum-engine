package com.wingedsheep.mtg.sets.definitions.mat.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.CollectionFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Blot Out {2}{B}
 * Instant
 *
 * Target opponent exiles a creature or planeswalker they control with the greatest mana value
 * among creatures and planeswalkers they control.
 *
 * Inside [Effects.ForEachPlayer] over the targeted opponent, that opponent is the controller: they
 * gather their creatures and planeswalkers, narrow them to those tied for the greatest mana value,
 * and choose which tied permanent to exile.
 */
val BlotOut = card("Blot Out") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Target opponent exiles a creature or planeswalker they control with the greatest mana " +
        "value among creatures and planeswalkers they control."

    spell {
        target = Targets.Opponent
        effect = Effects.ForEachPlayer(
            players = Player.TargetOpponent,
            Effects.Pipeline {
                val candidates = gather(
                    CardSource.ControlledPermanents(Player.You, GameObjectFilter.CreatureOrPlaneswalker)
                )
                val greatest = filter(candidates, CollectionFilter.GreatestManaValue)
                val exiled = chooseExactly(
                    1,
                    from = greatest,
                    chooser = Chooser.Controller,
                    prompt = "Choose a creature or planeswalker with the greatest mana value to exile",
                    useTargetingUI = true
                )
                exile(exiled)
            }
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "12"
        artist = "Chris Cold"
        flavorText = "After the invasion, many wanted to leave a life of violence. But once you're a Reckoner, you die a Reckoner."
        imageUri = "https://cards.scryfall.io/normal/front/4/2/424eb4c7-647e-4168-9471-f299e8e77f5f.jpg?1783916525"
    }
}
