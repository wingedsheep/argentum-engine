package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Time Wipe
 * {2}{W}{W}{U}
 * Sorcery
 * Return a creature you control to its owner's hand, then destroy all creatures.
 *
 * The bounce is untargeted — the creature is chosen on resolution — and mandatory: with a creature
 * on your side, one of them goes home before the sweep.
 */
val TimeWipe = card("Time Wipe") {
    manaCost = "{2}{W}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Sorcery"
    oracleText = "Return a creature you control to its owner's hand, then destroy all creatures."

    spell {
        effect = Effects.Pipeline {
            val candidates = gather(
                CardSource.BattlefieldMatching(
                    filter = GameObjectFilter.Creature,
                    player = Player.You,
                )
            )
            val returned = chooseExactly(
                1,
                from = candidates,
                prompt = "Return a creature you control to its owner's hand",
                useTargetingUI = true
            )
            toHand(returned)
        } then Effects.DestroyAll(GameObjectFilter.Creature)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "223"
        artist = "Svetlin Velinov"
        flavorText = "\"To comprehend the full answer requires years of temporal study. In short, they were now never born in the first place.\"\n—Teferi"
        imageUri = "https://cards.scryfall.io/normal/front/6/2/62c59475-6f15-48d2-b105-f49901f20d44.jpg"
    }
}
