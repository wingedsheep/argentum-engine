package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Sun-Spider, Nimble Webber
 * {3}{W/U}
 * Legendary Creature — Spider Human Hero, 3/2
 *
 * During your turn, Sun-Spider has flying.
 * When Sun-Spider enters, search your library for an Aura or Equipment card, reveal it,
 * put it into your hand, then shuffle.
 *
 * The conditional flying is a time-restricted static keyword grant to self
 * ([ConditionalStaticAbility] over [GrantKeyword] on [Filters.Self], gated by
 * [Conditions.IsYourTurn]) — same shape as Spider-Girl, Legacy Hero and Shocker, Unshakable.
 * The ETB tutor reuses [Patterns.Library.searchLibrary] to [SearchDestination.HAND] with a
 * card filter that matches any Aura or Equipment ([GameObjectFilter.Any] +
 * `withAnySubtype("Aura", "Equipment")`), revealing the found card and shuffling afterward.
 */
val SunSpiderNimbleWebber = card("Sun-Spider, Nimble Webber") {
    manaCost = "{3}{W/U}"
    colorIdentity = "WU"
    typeLine = "Legendary Creature — Spider Human Hero"
    oracleText = "During your turn, Sun-Spider has flying.\n" +
        "When Sun-Spider enters, search your library for an Aura or Equipment card, reveal it, " +
        "put it into your hand, then shuffle."
    power = 3
    toughness = 2

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.FLYING, Filters.Self),
            condition = Conditions.IsYourTurn,
        )
    }

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Any.withAnySubtype("Aura", "Equipment"),
            count = 1,
            destination = SearchDestination.HAND,
            reveal = true,
            shuffleAfter = true,
        )
        description = "When Sun-Spider enters, search your library for an Aura or Equipment card, " +
            "reveal it, put it into your hand, then shuffle."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "154"
        artist = "Justyna Dura"
        flavorText = "Breaking expectations and enjoying it."
        imageUri = "https://cards.scryfall.io/normal/front/2/b/2b54d4a5-634f-4ae3-b592-0dc527f60d56.jpg?1783905310"
    }
}
