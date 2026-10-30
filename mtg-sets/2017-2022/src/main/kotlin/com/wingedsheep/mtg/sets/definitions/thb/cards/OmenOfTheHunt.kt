package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Omen of the Hunt
 * {2}{G}
 * Enchantment
 *
 * Flash
 * When this enchantment enters, you may search your library for a basic land card, put it onto the
 * battlefield tapped, then shuffle.
 * {2}{G}, Sacrifice this enchantment: Scry 2.
 *
 * The green member of the Omen cycle. The "you may" is an optional trigger (Farhaven Elf's shape):
 * declining skips the search and the shuffle with it.
 */
val OmenOfTheHunt = card("Omen of the Hunt") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "Flash\n" +
        "When this enchantment enters, you may search your library for a basic land card, put it onto " +
        "the battlefield tapped, then shuffle.\n" +
        "{2}{G}, Sacrifice this enchantment: Scry 2."

    keywords(Keyword.FLASH)

    // When this enchantment enters, you may search your library for a basic land card, put it onto
    // the battlefield tapped, then shuffle.
    triggeredAbility {
        trigger = Triggers.self.enters()
        optional = true
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.BasicLand,
            destination = SearchDestination.BATTLEFIELD,
            entersTapped = true
        )
    }

    // {2}{G}, Sacrifice this enchantment: Scry 2.
    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{2}{G}"),
            Costs.SacrificeSelf
        )
        effect = Effects.Scry(2)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "192"
        artist = "Piotr Dura"
        flavorText = "\"My time will come, when no road will divide the endless verdure of the trees.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/d/7d4ebdea-78d4-4034-921b-735bdb11a716.jpg?1783931531"
    }
}
