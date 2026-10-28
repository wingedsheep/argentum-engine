package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Kazandu Stomper
 * {5}{G}
 * Creature — Beast
 * 6/5
 * Trample
 * When this creature enters, return up to two lands you control to their owner's hand.
 *
 * The bounce doesn't target: the lands are chosen as the trigger resolves (Gather → Select → Move).
 */
val KazanduStomper = card("Kazandu Stomper") {
    manaCost = "{5}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Beast"
    oracleText = "Trample (This creature can deal excess combat damage to the player or planeswalker it's attacking.)\n" +
        "When this creature enters, return up to two lands you control to their owner's hand."
    power = 6
    toughness = 5

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        description = "When this creature enters, return up to two lands you control to their owner's hand."
        effect = Effects.Pipeline {
            val lands = gather(
                CardSource.BattlefieldMatching(
                    filter = GameObjectFilter.Land,
                    player = Player.You,
                )
            )
            val bounced = chooseUpTo(
                2,
                from = lands,
                prompt = "Return up to two lands you control to their owner's hand",
                useTargetingUI = true
            )
            toHand(bounced)
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "191"
        artist = "Sam Rowan"
        flavorText = "Elves revere cycles of renewal, but they prefer cycles that don't involve their homes getting smashed."
        imageUri = "https://cards.scryfall.io/normal/front/a/f/afdfe5aa-8b15-4a89-a22a-03baf6afa4e7.jpg?1783929338"
    }
}
