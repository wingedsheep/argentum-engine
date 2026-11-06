package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * Benefaction of Rhonas
 * {2}{G}
 * Sorcery
 * Reveal the top five cards of your library. You may put a creature card and/or an enchantment card
 * from among them into your hand. Put the rest into your graveyard.
 *
 * "A creature card and/or an enchantment card" is two independent up-to-one picks — a creature first,
 * then an enchantment from what's left (the Relentless Pursuit shape), so an enchantment creature can
 * fill either slot but never both.
 */
val BenefactionOfRhonas = card("Benefaction of Rhonas") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Reveal the top five cards of your library. You may put a creature card and/or an enchantment card from among them into your hand. Put the rest into your graveyard."

    spell {
        effect = Effects.Pipeline {
            val revealed = gather(CardSource.TopOfLibrary(5), revealed = true)
            val (creature, afterCreature) = chooseUpToSplit(
                1, from = revealed,
                filter = GameObjectFilter.Creature,
                prompt = "You may put a creature card into your hand",
                selectedLabel = "Put in hand",
                remainderLabel = "Leave",
                showAllCards = true
            )
            val (enchantment, rest) = chooseUpToSplit(
                1, from = afterCreature,
                filter = GameObjectFilter.Enchantment,
                prompt = "You may put an enchantment card into your hand",
                selectedLabel = "Put in hand",
                remainderLabel = "Put into graveyard",
                showAllCards = true
            )
            toHand(creature)
            toHand(enchantment)
            toGraveyard(rest)
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "156"
        artist = "Tommy Arnold"
        flavorText = "It falls to the gods to help all become worthy of the afterlife."
        imageUri = "https://cards.scryfall.io/normal/front/d/c/dc98fcdd-8482-4462-ab71-935cea48e409.jpg?1783936479"
        ruling("2017-04-18", "While resolving Benefaction of Rhonas, you could put no cards, a creature card, an enchantment card, or a creature card and an enchantment card into your hand.")
    }
}
