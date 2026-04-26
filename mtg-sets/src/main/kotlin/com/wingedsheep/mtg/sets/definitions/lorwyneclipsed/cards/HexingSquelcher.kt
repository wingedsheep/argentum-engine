package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantCantBeCountered
import com.wingedsheep.sdk.scripting.GrantWardToGroup
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Hexing Squelcher
 * {1}{R}
 * Creature — Goblin Sorcerer
 * 2/2
 * This spell can't be countered.
 * Ward—Pay 2 life.
 * Spells you control can't be countered.
 * Other creatures you control have "Ward—Pay 2 life."
 */
val HexingSquelcher = card("Hexing Squelcher") {
    manaCost = "{1}{R}"
    typeLine = "Creature — Goblin Sorcerer"
    power = 2
    toughness = 2
    oracleText = "This spell can't be countered.\n" +
        "Ward—Pay 2 life.\n" +
        "Spells you control can't be countered.\n" +
        "Other creatures you control have \"Ward—Pay 2 life.\""

    cantBeCountered = true

    keywords(Keyword.WARD)
    keywordAbility(KeywordAbility.wardLife(2))

    // Spells you control can't be countered.
    staticAbility {
        ability = GrantCantBeCountered(
            filter = GameObjectFilter.Any.youControl()
        )
    }

    // Other creatures you control have "Ward—Pay 2 life."
    staticAbility {
        ability = GrantWardToGroup(
            cost = WardCost.Life(2),
            filter = GroupFilter(GameObjectFilter.Creature.youControl()).other()
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "145"
        artist = "Matt Stewart"
        flavorText = "\"Finally, a place where the ingredients are as potent as my magics!\""
        imageUri = "https://cards.scryfall.io/normal/front/6/7/674960ce-ff33-4d5e-a24a-a4582b2e9809.jpg?1767658306"
    }
}
