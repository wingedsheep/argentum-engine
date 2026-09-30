package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Bonepicker Skirge
 * {2}{B}
 * Creature — Phyrexian Imp
 * 2/2
 * Flying
 * Corrupted — As long as an opponent has three or more poison counters, this creature has
 * deathtouch and lifelink.
 *
 * Two keyword grants over the same [Conditions.Corrupted], each scoped to this creature.
 */
val BonepickerSkirge = card("Bonepicker Skirge") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Phyrexian Imp"
    oracleText = "Flying\nCorrupted — As long as an opponent has three or more poison counters, this creature has deathtouch and lifelink."
    power = 2
    toughness = 2

    keywords(Keyword.FLYING)

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.DEATHTOUCH, GroupFilter.source()),
            condition = Conditions.Corrupted
        )
    }

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.LIFELINK, GroupFilter.source()),
            condition = Conditions.Corrupted
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "86"
        artist = "Dave Kendall"
        flavorText = "\"Get away from there! I must rebuild that one for the next bout. Shoo, shoo!\"\n—Keskit, the Flesh Sculptor"
        imageUri = "https://cards.scryfall.io/normal/front/b/8/b83f4e41-a5f5-4929-9816-06dc1c228474.jpg?1783918050"
    }
}
