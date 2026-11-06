package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Gideon's Resolve — Amonkhet #272 (Planeswalker Deck)
 * {4}{W} · Enchantment · Rare
 *
 * When this enchantment enters, you may search your library and/or graveyard for a card named
 * Gideon, Martial Paragon, reveal it, and put it into your hand. If you search your library this
 * way, shuffle.
 * Creatures you control get +1/+1.
 *
 * Elspeth's Devotee's [Patterns.Library.searchLibraryAndOrGraveyard] naming Gideon, plus Glorious
 * Anthem's static.
 */
val GideonsResolve = card("Gideon's Resolve") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, you may search your library and/or graveyard for a card " +
        "named Gideon, Martial Paragon, reveal it, and put it into your hand. If you search your " +
        "library this way, shuffle.\nCreatures you control get +1/+1."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Library.searchLibraryAndOrGraveyard(
            filter = GameObjectFilter.Any.named("Gideon, Martial Paragon"),
            destination = SearchDestination.HAND,
            reveal = true,
            optional = true
        )
        description = "When this enchantment enters, you may search your library and/or graveyard for a " +
            "card named Gideon, Martial Paragon, reveal it, and put it into your hand. If you search " +
            "your library this way, shuffle."
    }

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(GameObjectFilter.Creature.youControl())
        )
    }

    metadata {
        inBooster = false
        rarity = Rarity.RARE
        collectorNumber = "272"
        artist = "Jakub Kasper"
        imageUri = "https://cards.scryfall.io/normal/front/a/3/a30b8c40-39ac-423f-b7da-845a16cd1b63.jpg?1783936437"
    }
}
