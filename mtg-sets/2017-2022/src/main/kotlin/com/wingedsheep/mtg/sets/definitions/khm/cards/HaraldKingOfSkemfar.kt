package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Harald, King of Skemfar — Kaldheim #212
 * {1}{B}{G} · Legendary Creature — Elf Warrior · 3/2
 *
 * Menace
 * When Harald enters, look at the top five cards of your library. You may reveal an Elf, Warrior,
 * or Tyvar card from among them and put it into your hand. Put the rest on the bottom of your
 * library in a random order.
 *
 * "Elf, Warrior, or Tyvar card" is any card with one of those subtypes — Kindred Elf/Warrior
 * spells and Tyvar planeswalkers included, so the filter is over [GameObjectFilter.Any].
 */
val HaraldKingOfSkemfar = card("Harald, King of Skemfar") {
    manaCost = "{1}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Legendary Creature — Elf Warrior"
    power = 3
    toughness = 2
    oracleText = "Menace (This creature can't be blocked except by two or more creatures.)\n" +
        "When Harald enters, look at the top five cards of your library. You may reveal an Elf, Warrior, " +
        "or Tyvar card from among them and put it into your hand. Put the rest on the bottom of your " +
        "library in a random order."

    keywords(Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Library.lookAtTopRevealMatchingToHand(
            count = 5,
            filter = GameObjectFilter.Any.withSubtype("Elf") or
                GameObjectFilter.Any.withSubtype("Warrior") or
                GameObjectFilter.Any.withSubtype("Tyvar"),
            prompt = "You may reveal an Elf, Warrior, or Tyvar card from among them and put it into your hand",
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "212"
        artist = "Grzegorz Rutkowski"
        imageUri = "https://cards.scryfall.io/normal/front/9/e/9eac78a2-599f-4dba-aec7-982c5ae3f75a.jpg?1783928197"
    }
}
