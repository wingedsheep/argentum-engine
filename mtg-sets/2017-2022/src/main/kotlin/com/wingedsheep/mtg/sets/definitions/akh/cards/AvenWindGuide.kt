package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.embalm
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Aven Wind Guide — Amonkhet #195
 * {2}{W}{U} · Creature — Bird Warrior · Uncommon
 * 2/3
 *
 * Flying, vigilance
 * Creature tokens you control have flying and vigilance.
 * Embalm {4}{W}{U}
 */
val AvenWindGuide = card("Aven Wind Guide") {
    manaCost = "{2}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Creature — Bird Warrior"
    power = 2
    toughness = 3
    oracleText = "Flying, vigilance\n" +
        "Creature tokens you control have flying and vigilance.\n" +
        "Embalm {4}{W}{U} ({4}{W}{U}, Exile this card from your graveyard: Create a token that's a copy of it, " +
        "except it's a white Zombie Bird Warrior with no mana cost. Embalm only as a sorcery.)"

    keywords(Keyword.FLYING, Keyword.VIGILANCE)

    staticAbility {
        ability = GrantKeyword(
            keyword = Keyword.FLYING,
            filter = GroupFilter((GameObjectFilter.Creature and GameObjectFilter.Token).youControl())
        )
    }

    staticAbility {
        ability = GrantKeyword(
            keyword = Keyword.VIGILANCE,
            filter = GroupFilter((GameObjectFilter.Creature and GameObjectFilter.Token).youControl())
        )
    }

    embalm("{4}{W}{U}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "195"
        artist = "Sidharth Chaturvedi"
        imageUri = "https://cards.scryfall.io/normal/front/4/1/414bc716-2b1b-4731-bd0c-f4a5caf26e40.jpg?1783936465"
    }
}
