package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.SpendAnyManaTypeForSpells
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * Oath of Nissa
 * {G}
 * Legendary Enchantment
 * When Oath of Nissa enters, look at the top three cards of your library. You may reveal a
 * creature, land, or planeswalker card from among them and put it into your hand. Put the rest on
 * the bottom of your library in any order.
 * You may spend mana as though it were mana of any color to cast planeswalker spells.
 *
 * The second ability is [SpendAnyManaTypeForSpells] with `asThoughAnyColor = true`: any mana pays
 * a planeswalker spell's colored pips, but a `{C}` pip still needs colorless mana — colorless is a
 * type, not a color.
 */
val OathOfNissa = card("Oath of Nissa") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Legendary Enchantment"
    oracleText = "When Oath of Nissa enters, look at the top three cards of your library. You may reveal a " +
        "creature, land, or planeswalker card from among them and put it into your hand. Put the rest on the " +
        "bottom of your library in any order.\n" +
        "You may spend mana as though it were mana of any color to cast planeswalker spells."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            val looked = gather(CardSource.TopOfLibrary(3))
            val (picked, rest) = chooseUpToSplit(
                1, from = looked,
                filter = GameObjectFilter.Creature or GameObjectFilter.Land or GameObjectFilter.Planeswalker,
                prompt = "You may reveal a creature, land, or planeswalker card to put into your hand",
                selectedLabel = "Put in hand",
                remainderLabel = "Put on bottom",
                showAllCards = true
            )
            toHand(picked, revealed = true)
            toLibraryBottom(rest)
        }
    }

    staticAbility {
        ability = SpendAnyManaTypeForSpells(GameObjectFilter.Planeswalker, asThoughAnyColor = true)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "140"
        artist = "Wesley Burt"
        imageUri = "https://cards.scryfall.io/normal/front/6/1/613513ea-55dd-4ae8-93b2-247e468112a6.jpg?1783937900"
        ruling("2016-01-22", "The “legend rule” checks the full names of legendary permanents. You can control Oath of Gideon and Oath of Chandra at the same time, for example.")
        ruling("2016-01-22", "Even if you control a particular Oath, you can cast another of that Oath to take advantage of its enters-the-battlefield ability. It will enter the battlefield, causing its first ability to trigger. You will then choose one to keep, as detailed above, and then its enters-the-battlefield ability will resolve.")
        ruling("2016-01-22", "Each of the Oaths is legendary: if you control more than one of any particular Oath, you choose one to remain on the battlefield and the others are put into your graveyard.")
    }
}
