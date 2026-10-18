package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * Blossom Prancer
 * {3}{G}{G}
 * Creature — Spirit
 * 4/4
 *
 * Reach
 * When this creature enters, look at the top five cards of your library. You may reveal a creature
 * or enchantment card from among them and put it into your hand. Put the rest on the bottom of your
 * library in a random order. If you didn't put a card into your hand this way, you gain 4 life.
 *
 * The optional `chooseUpToSplit(1)` is the "you may"; the life gain keys off the kept pile being
 * empty — declined, or no creature/enchantment among the five (same shape as Contagious Vorrac).
 */
val BlossomPrancer = card("Blossom Prancer") {
    manaCost = "{3}{G}{G}"
    typeLine = "Creature — Spirit"
    power = 4
    toughness = 4
    oracleText = "Reach\nWhen this creature enters, look at the top five cards of your library. You may reveal a " +
        "creature or enchantment card from among them and put it into your hand. Put the rest on the bottom of " +
        "your library in a random order. If you didn't put a card into your hand this way, you gain 4 life."

    keywords(Keyword.REACH)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            val looked = gather(CardSource.TopOfLibrary(5))
            val (kept, rest) = chooseUpToSplit(
                1,
                from = looked,
                filter = GameObjectFilter.Creature or GameObjectFilter.Enchantment,
                prompt = "You may reveal a creature or enchantment card and put it into your hand",
                selectedLabel = "Put in hand",
                remainderLabel = "Put on bottom",
                showAllCards = true
            )
            toHand(kept, revealed = true)
            toLibraryBottom(rest, order = CardOrder.Random)
            ifNotEmpty(kept) {
                run(Effects.Nothing)
            } orElse {
                run(Effects.GainLife(4))
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "175"
        artist = "Ryan Yee"
        imageUri = "https://cards.scryfall.io/normal/front/d/2/d26abdf2-efd7-40e5-bfc6-d75fcadd26f8.jpg?1783923854"
    }
}
