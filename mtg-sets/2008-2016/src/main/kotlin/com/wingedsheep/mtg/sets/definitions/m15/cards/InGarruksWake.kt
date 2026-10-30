package com.wingedsheep.mtg.sets.definitions.m15.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate

/**
 * In Garruk's Wake — Magic 2015 #100 (canonical / earliest real printing)
 * {7}{B}{B} · Sorcery
 *
 * Destroy all creatures you don't control and all planeswalkers you don't control.
 *
 * One simultaneous [Effects.DestroyAll] over creatures-or-planeswalkers. "You don't control" is
 * `Not(ControlledByYou)` rather than `opponentControls()` (the Plague Wind spelling), so a
 * Two-Headed Giant teammate's permanents are swept too.
 */
val InGarruksWake = card("In Garruk's Wake") {
    manaCost = "{7}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Destroy all creatures you don't control and all planeswalkers you don't control."

    spell {
        effect = Effects.DestroyAll(
            GameObjectFilter.CreatureOrPlaneswalker.withControllerPredicate(
                ControllerPredicate.Not(ControllerPredicate.ControlledByYou)
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "100"
        artist = "Chase Stone"
        flavorText = "Beyond pain, beyond obsession and wild despair, there lies a place of twisted power only the most tormented souls can reach."
        imageUri = "https://cards.scryfall.io/normal/front/f/6/f6f2c2f6-d07f-42af-9944-70d3dac8348c.jpg?1783939183"
    }
}
