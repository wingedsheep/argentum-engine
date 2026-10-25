package com.wingedsheep.mtg.sets.definitions.mh1.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.minus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Sisay, Weatherlight Captain
 * {2}{W}
 * Legendary Creature — Human Soldier
 * 2/2
 * Sisay gets +1/+1 for each color among other legendary permanents you control.
 * {W}{U}{B}{R}{G}: Search your library for a legendary permanent card with mana value less than
 * Sisay's power, put that card onto the battlefield, then shuffle.
 *
 * - The bonus counts distinct (projected) colors among legendary permanents you control other than
 *   Sisay herself — `battlefield(excludeSelf = true).distinctColors()`, maxing out at 5.
 * - "Mana value less than Sisay's power" is `manaValueAtMostDynamic(power − 1)`, read on
 *   resolution; if Sisay has left the battlefield, her last known power is used (ruling).
 */
private val sisayColorBonus: DynamicAmount = DynamicAmounts.battlefield(
    Player.You,
    GameObjectFilter.Permanent.legendary(),
    excludeSelf = true,
).distinctColors()

val SisayWeatherlightCaptain = card("Sisay, Weatherlight Captain") {
    manaCost = "{2}{W}"
    colorIdentity = "WUBRG"
    typeLine = "Legendary Creature — Human Soldier"
    power = 2
    toughness = 2
    oracleText = "Sisay gets +1/+1 for each color among other legendary permanents you control.\n" +
        "{W}{U}{B}{R}{G}: Search your library for a legendary permanent card with mana value less than " +
        "Sisay's power, put that card onto the battlefield, then shuffle."

    staticAbility {
        ability = GrantDynamicStats(
            filter = GroupFilter.source(),
            powerBonus = sisayColorBonus,
            toughnessBonus = sisayColorBonus,
        )
    }

    activatedAbility {
        cost = Costs.Mana("{W}{U}{B}{R}{G}")
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Permanent.legendary()
                .manaValueAtMostDynamic(DynamicAmounts.sourcePower() - 1),
            destination = SearchDestination.BATTLEFIELD,
        )
        description = "Search your library for a legendary permanent card with mana value less than " +
            "Sisay's power, put that card onto the battlefield, then shuffle."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "29"
        artist = "Anna Steinbauer"
        imageUri = "https://cards.scryfall.io/normal/front/5/a/5a293c45-1e73-4527-be2f-2dcd5c47b610.jpg?1783933156"
        ruling("2019-06-14", "If Sisay leaves the battlefield after you've activated its last ability but before that ability resolves, use its last known existence on the battlefield to determine what cards you can find.")
        ruling("2019-06-14", "If a card in a library has {X} in its mana cost, X is considered to be 0.")
    }
}
