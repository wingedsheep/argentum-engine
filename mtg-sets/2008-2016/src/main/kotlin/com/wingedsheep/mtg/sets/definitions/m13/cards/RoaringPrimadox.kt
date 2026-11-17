package com.wingedsheep.mtg.sets.definitions.m13.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Roaring Primadox
 * {3}{G}
 * Creature — Beast
 * 4/4
 * At the beginning of your upkeep, return a creature you control to its owner's hand.
 */
val RoaringPrimadox = card("Roaring Primadox") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Beast"
    power = 4
    toughness = 4
    oracleText = "At the beginning of your upkeep, return a creature you control to its owner's hand."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.Pipeline {
            val creatures = gather(
                CardSource.BattlefieldMatching(
                    filter = GameObjectFilter.Creature,
                    player = Player.You,
                )
            )
            val returned = chooseExactly(
                1,
                from = creatures,
                prompt = "Return a creature you control to its owner's hand",
                useTargetingUI = true
            )
            toHand(returned)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "188"
        artist = "James Ryman"
        flavorText = "\"They're easy enough to find. Question is, are you sure you want to find one?\"\n—Juruk, Kalonian tracker"
        imageUri = "https://cards.scryfall.io/normal/front/1/9/19529b2f-03f0-469d-92d4-e2a2a933d5dc.jpg"
    }
}
