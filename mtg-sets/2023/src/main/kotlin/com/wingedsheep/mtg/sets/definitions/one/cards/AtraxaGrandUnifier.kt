package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.SelectionRestriction

/**
 * Atraxa, Grand Unifier
 * {3}{G}{W}{U}{B}
 * Legendary Creature — Phyrexian Angel
 * 7/7
 * Flying, vigilance, deathtouch, lifelink
 * When Atraxa enters, reveal the top ten cards of your library. For each card type, you may put a
 * card of that type from among the revealed cards into your hand. Put the rest on the bottom of
 * your library in a random order.
 *
 * "For each card type" is [SelectionRestriction.OnePerCardType]: each kept card claims one of its
 * types, so an artifact creature can be kept as the artifact alongside a plain creature (ruling).
 */
val AtraxaGrandUnifier = card("Atraxa, Grand Unifier") {
    manaCost = "{3}{G}{W}{U}{B}"
    typeLine = "Legendary Creature — Phyrexian Angel"
    power = 7
    toughness = 7
    oracleText = "Flying, vigilance, deathtouch, lifelink\n" +
        "When Atraxa enters, reveal the top ten cards of your library. For each card type, you may put " +
        "a card of that type from among the revealed cards into your hand. Put the rest on the bottom " +
        "of your library in a random order. (Artifact, battle, creature, enchantment, instant, land, " +
        "planeswalker, and sorcery are card types.)"

    keywords(Keyword.FLYING, Keyword.VIGILANCE, Keyword.DEATHTOUCH, Keyword.LIFELINK)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            val revealed = gather(CardSource.TopOfLibrary(DynamicAmounts.fixed(10)), revealed = true)
            val (kept, rest) = chooseUpToSplit(
                10,
                from = revealed,
                restrictions = listOf(SelectionRestriction.OnePerCardType),
                prompt = "For each card type, you may put a card of that type into your hand",
                selectedLabel = "Hand",
                remainderLabel = "Bottom of library"
            )
            toHand(kept)
            toLibraryBottom(rest, order = CardOrder.Random)
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "196"
        artist = "Marta Nael"
        imageUri = "https://cards.scryfall.io/normal/front/4/a/4a1f905f-1d55-4d02-9d24-e58070793d3f.jpg?1783918003"
        ruling("2023-02-04", "If a revealed card has more than one card type, you may choose to put it into your hand for any of its types. For example, an artifact creature card could be put into your hand as the artifact card you choose or as the creature card you choose. If you choose it as the artifact card, you could also put into your hand a creature card, and vice versa.")
        ruling("2023-02-04", "At the time of this document's publication, it is exceedingly unlikely you will reveal a battle this way. This likelihood will change over time.")
    }
}
