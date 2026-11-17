package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Mastery of the Unseen
 * {1}{W}
 * Enchantment
 * Whenever a permanent you control is turned face up, you gain 1 life for each creature you control.
 * {3}{W}: Manifest the top card of your library.
 *
 * The life gained counts creatures you control as the trigger resolves.
 */
val MasteryOfTheUnseen = card("Mastery of the Unseen") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "Whenever a permanent you control is turned face up, you gain 1 life for each creature " +
        "you control.\n" +
        "{3}{W}: Manifest the top card of your library. (Put it onto the battlefield face down as a 2/2 " +
        "creature. Turn it face up any time for its mana cost if it's a creature card.)"

    triggeredAbility {
        trigger = Triggers.you.permanentTurnedFaceUp()
        effect = Effects.GainLife(DynamicAmounts.creaturesYouControl())
    }

    activatedAbility {
        cost = Costs.Mana("{3}{W}")
        effect = Patterns.Library.manifest()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "19"
        artist = "Daniel Ljunggren"
        imageUri = "https://cards.scryfall.io/normal/front/4/b/4b844764-89d4-43a2-8041-3a9a8b4f2912.jpg?1783938714"
        ruling(
            "2014-11-24",
            "If something tries to turn a face-down instant or sorcery card on the battlefield face up, reveal " +
                "that card to show all players it's an instant or sorcery card. The permanent remains on the " +
                "battlefield face down. Abilities that trigger when a permanent turns face up won't trigger, " +
                "because even though you revealed the card, it never turned face up."
        )
    }
}
