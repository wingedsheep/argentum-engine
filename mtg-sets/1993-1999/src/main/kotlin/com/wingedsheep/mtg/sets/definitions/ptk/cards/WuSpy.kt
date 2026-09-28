package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Wu Spy
 * {1}{U}
 * Creature — Human Soldier Rogue
 * 1 / 1
 *
 * When this creature enters, look at the top two cards of target player's library.
 * Put one of them into their graveyard.
 *
 * The card not chosen is never moved, so it stays on top of the library.
 */
val WuSpy = card("Wu Spy") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Soldier Rogue"
    power = 1
    toughness = 1
    oracleText = "When this creature enters, look at the top two cards of target player's library. Put one of them into their graveyard."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(Targets.Player)
        effect = Effects.Pipeline {
            val looked = gather(CardSource.TopOfLibrary(2, Player.TargetPlayer))
            val (toGraveyardCards, _) = chooseExactlySplit(
                1,
                from = looked,
                selectedLabel = "Put in graveyard",
                remainderLabel = "Leave on top"
            )
            toGraveyard(toGraveyardCards, Player.TargetPlayer)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "63"
        artist = "Zhao Tan"
        imageUri = "https://cards.scryfall.io/normal/front/f/7/f77b8e34-e7e9-4ac6-bc21-0ef52c6696c7.jpg?1783946119"
    }
}
