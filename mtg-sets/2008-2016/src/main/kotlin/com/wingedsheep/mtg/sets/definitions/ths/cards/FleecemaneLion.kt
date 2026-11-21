package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Fleecemane Lion — Theros #193
 * {G}{W} · Creature — Cat · 3/3
 *
 * {3}{G}{W}: Monstrosity 1.
 * As long as this creature is monstrous, it has hexproof and indestructible.
 *
 * The monstrous clause is two self-scoped keyword grants behind [Conditions.SourceIsMonstrous].
 */
val FleecemaneLion = card("Fleecemane Lion") {
    manaCost = "{G}{W}"
    typeLine = "Creature — Cat"
    power = 3
    toughness = 3
    oracleText = "{3}{G}{W}: Monstrosity 1. (If this creature isn't monstrous, put a +1/+1 counter on it and it becomes monstrous.)\n" +
        "As long as this creature is monstrous, it has hexproof and indestructible."

    activatedAbility {
        cost = Costs.Mana("{3}{G}{W}")
        effect = Effects.Monstrosity(1)
    }

    staticAbility {
        ability = GrantKeyword(keyword = Keyword.HEXPROOF.name, filter = GroupFilter.source())
        condition = Conditions.SourceIsMonstrous
    }

    staticAbility {
        ability = GrantKeyword(keyword = Keyword.INDESTRUCTIBLE.name, filter = GroupFilter.source())
        condition = Conditions.SourceIsMonstrous
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "193"
        artist = "Slawomir Maniak"
        imageUri = "https://cards.scryfall.io/normal/front/6/2/622d6c46-82bb-4a97-abe6-b388def44bda.jpg?1783939730"
        ruling("2013-09-15", "Once a creature becomes monstrous, it can't become monstrous again. If the creature is already monstrous when the monstrosity ability resolves, nothing happens.")
        ruling("2013-09-15", "Monstrous isn't an ability that a creature has. It's just something true about that creature. If the creature stops being a creature or loses its abilities, it will continue to be monstrous.")
    }
}
