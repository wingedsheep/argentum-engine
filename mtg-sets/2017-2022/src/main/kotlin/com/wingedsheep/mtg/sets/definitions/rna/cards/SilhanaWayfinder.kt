package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * Silhana Wayfinder
 * {1}{G}
 * Creature — Elf Scout
 * 2/1
 * When this creature enters, look at the top four cards of your library. You may reveal a creature
 * or land card from among them and put it on top of your library. Put the rest on the bottom of
 * your library in a random order.
 *
 * Voda Sea Scavenger's look-and-keep-one-on-top shape, narrowed to creature or land cards, with
 * the kept card revealed to the opponents before it goes back on top.
 */
val SilhanaWayfinder = card("Silhana Wayfinder") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Scout"
    power = 2
    toughness = 1
    oracleText = "When this creature enters, look at the top four cards of your library. You may reveal a creature or land card from among them and put it on top of your library. Put the rest on the bottom of your library in a random order."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            val looked = gather(CardSource.TopOfLibrary(4))
            val (kept, rest) = chooseUpToSplit(
                count = 1,
                from = looked,
                filter = GameObjectFilter.CreatureOrLand,
                prompt = "You may reveal a creature or land card to put on top of your library",
                selectedLabel = "Put on top of library",
                remainderLabel = "Put on the bottom in a random order",
                showAllCards = true
            )
            reveal(kept, revealToSelf = false)
            toLibraryBottom(rest, order = CardOrder.Random)
            toLibraryTop(kept)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "141"
        artist = "Suzanne Helmigh"
        flavorText = "\"Stay close! It's just a little farther.\""
        imageUri = "https://cards.scryfall.io/normal/front/e/a/ea6cd6a5-da6c-483c-a9f7-49e666ac0b6c.jpg?1783933664"
    }
}
