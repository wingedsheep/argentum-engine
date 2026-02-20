package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect

/**
 * Undead Gladiator
 * {1}{B}{B}
 * Creature — Zombie Barbarian
 * 3/1
 * {1}{B}, Discard a card: Return Undead Gladiator from your graveyard to your hand.
 *   Activate only during your upkeep.
 * Cycling {1}{B}
 */
val UndeadGladiator = card("Undead Gladiator") {
    manaCost = "{1}{B}{B}"
    typeLine = "Creature — Zombie Barbarian"
    power = 3
    toughness = 1
    oracleText = "{1}{B}, Discard a card: Return Undead Gladiator from your graveyard to your hand. Activate only during your upkeep.\nCycling {1}{B}"

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{B}"), Costs.DiscardCard)
        effect = MoveToZoneEffect(EffectTarget.Self, Zone.HAND)
        activateFromZone = Zone.GRAVEYARD
        restrictions = listOf(
            ActivationRestriction.All(
                ActivationRestriction.OnlyDuringYourTurn,
                ActivationRestriction.DuringStep(Step.UPKEEP)
            )
        )
    }

    keywordAbility(KeywordAbility.cycling("{1}{B}"))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "178"
        artist = "Jeff Easley"
        flavorText = "The Cabal loves encores."
        imageUri = "https://cards.scryfall.io/large/front/b/b/bbc779d9-3200-4369-9289-1a8e90e243b9.jpg?1562939313"
    }
}
