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
 * Tazeem Raptor — Zendikar Rising #43
 * {2}{W} · Creature — Bird · 2/2
 *
 * Flying
 * When this creature enters, you may return a land you control to its owner's hand.
 *
 * The land is chosen on resolution, not targeted — the bounce-land pipeline wrapped in a may.
 */
val TazeemRaptor = card("Tazeem Raptor") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Bird"
    oracleText = "Flying\nWhen this creature enters, you may return a land you control to its owner's hand."
    power = 2
    toughness = 2

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.May(
            Effects.Pipeline {
                val lands = gather(CardSource.BattlefieldMatching(filter = GameObjectFilter.Land, player = Player.You))
                val returned = chooseExactly(
                    1,
                    from = lands,
                    prompt = "Return a land you control to its owner's hand",
                    useTargetingUI = true
                )
                toHand(returned)
            }
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "43"
        artist = "Dan Murayama Scott"
        flavorText = "\"It's good to see the eagles circling the Lighthouse once again.\"\n—Tazri"
        imageUri = "https://cards.scryfall.io/normal/front/2/9/299d60b9-0ba7-40ee-b9d0-ee8f1ea288c9.jpg"
    }
}
