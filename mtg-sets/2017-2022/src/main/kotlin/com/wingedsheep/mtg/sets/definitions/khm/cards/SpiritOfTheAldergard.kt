package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Spirit of the Aldergard
 * {3}{G}
 * Snow Creature — Bear Spirit
 * 0/4
 * When this creature enters, search your library for a snow land card, reveal it, put it into your
 * hand, then shuffle.
 * This creature gets +1/+0 for each other snow permanent you control.
 */
val SpiritOfTheAldergard = card("Spirit of the Aldergard") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Snow Creature — Bear Spirit"
    power = 0
    toughness = 4
    oracleText = "When this creature enters, search your library for a snow land card, reveal it, " +
        "put it into your hand, then shuffle.\n" +
        "This creature gets +1/+0 for each other snow permanent you control."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Land.snow(),
            count = 1,
            destination = SearchDestination.HAND,
            reveal = true,
            shuffleAfter = true
        )
    }

    staticAbility {
        ability = GrantDynamicStats(
            filter = GroupFilter.source(),
            powerBonus = DynamicAmounts.battlefield(
                Player.You,
                GameObjectFilter.Permanent.snow(),
                excludeSelf = true
            ).count(),
            toughnessBonus = DynamicAmounts.fixed(0)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "195"
        artist = "Lars Grant-West"
        imageUri = "https://cards.scryfall.io/normal/front/2/2/220df551-3820-4910-a206-14501ba02e69.jpg?1783928204"
    }
}
