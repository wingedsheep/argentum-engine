package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Species Gorger
 * {3}{G}{U}
 * Creature — Frog Beast
 * 6/6
 * At the beginning of your upkeep, return a creature you control to its owner's hand.
 *
 * Not targeted and not optional: the creature is chosen on resolution, and Species Gorger itself is
 * a legal choice (and the only one when it is your sole creature).
 */
val SpeciesGorger = card("Species Gorger") {
    manaCost = "{3}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Creature — Frog Beast"
    power = 6
    toughness = 6
    oracleText = "At the beginning of your upkeep, return a creature you control to its owner's hand."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.Pipeline {
            val creatures = gather(
                CardSource.BattlefieldMatching(filter = GameObjectFilter.Creature, player = Player.You)
            )
            val returned = chooseExactly(
                1,
                from = creatures,
                prompt = "Return a permanent you control to its owner's hand",
                useTargetingUI = true
            )
            toHand(returned)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "105"
        artist = "Min Yum"
        flavorText = "\"We raised eelhawks to control the squidflies, then waspcrabs to prey on the eelhawks. Now what do we do with all these waspcrabs?\"\n—Gulistan, Simic biomancer"
        imageUri = "https://cards.scryfall.io/normal/front/e/0/e0087a98-55cf-4c8b-a180-fb0d9c336eb2.jpg?1783940021"
    }
}
