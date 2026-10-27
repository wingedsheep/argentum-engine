package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantAlternativeCastingCost
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Runeforge Champion — Kaldheim #26
 * {2}{W} · Creature — Dwarf Warrior · 2/3
 *
 * When this creature enters, you may search your library and/or graveyard for a Rune card, reveal
 * it, and put it into your hand. If you search your library this way, shuffle.
 * You may pay {1} rather than pay the mana cost for Rune spells you cast.
 *
 * - The search is the shared library + graveyard up-to-one pick (Fang-Druid Summoner's identical
 *   "you may search … and/or graveyard" shape).
 * - The cost line is a battlefield-granted alternative cost (CR 118.9) filtered to Rune spells,
 *   the same grant Jodah and Primal Prayers use.
 */
val RuneforgeChampion = card("Runeforge Champion") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Dwarf Warrior"
    power = 2
    toughness = 3
    oracleText = "When this creature enters, you may search your library and/or graveyard for a Rune card, " +
        "reveal it, and put it into your hand. If you search your library this way, shuffle.\n" +
        "You may pay {1} rather than pay the mana cost for Rune spells you cast."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Library.searchMultipleZones(
            zones = listOf(Zone.LIBRARY, Zone.GRAVEYARD),
            filter = GameObjectFilter.Any.withSubtype("Rune"),
            count = 1,
            destination = SearchDestination.HAND,
            reveal = true,
        )
    }

    staticAbility {
        ability = GrantAlternativeCastingCost(
            cost = "{1}",
            spellFilter = GameObjectFilter.Any.withSubtype("Rune"),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "26"
        artist = "Andrey Kuzinskiy"
        imageUri = "https://cards.scryfall.io/normal/front/4/3/4368b087-05a0-4a88-ab43-0cd16446239b.jpg?1783928277"
        ruling(
            "2021-02-05",
            "Runeforge Champion's last ability provides an alternative cost to cast Rune spells. You can't " +
                "combine this with other alternative costs that may apply. In the unusual case that a Rune " +
                "spell has additional costs, you can pay those in addition to the {1}. If those additional " +
                "costs are mandatory, you must pay them in order to cast the spell."
        )
        ruling("2021-02-05", "Runeforge Champion's last ability doesn't change when you can cast a Rune spell.")
        ruling("2021-02-05", "Casting a Rune spell for an alternative cost doesn't change its mana cost or mana value.")
    }
}
