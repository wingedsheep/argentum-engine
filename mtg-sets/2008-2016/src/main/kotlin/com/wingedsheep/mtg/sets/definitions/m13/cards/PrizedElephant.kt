package com.wingedsheep.mtg.sets.definitions.m13.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Prized Elephant
 * {3}{W}
 * Creature — Elephant
 * 3/3
 * This creature gets +1/+1 as long as you control a Forest.
 * {G}: This creature gains trample until end of turn.
 */
val PrizedElephant = card("Prized Elephant") {
    manaCost = "{3}{W}"
    colorIdentity = "GW"
    typeLine = "Creature — Elephant"
    oracleText = "This creature gets +1/+1 as long as you control a Forest.\n" +
        "{G}: This creature gains trample until end of turn. (It can deal excess combat damage to the player or planeswalker it's attacking.)"
    power = 3
    toughness = 3

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(1, 1, Filters.Self),
            condition = Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Land.withSubtype("Forest")),
        )
    }

    activatedAbility {
        cost = Costs.Mana("{G}")
        effect = Effects.GrantKeyword(Keyword.TRAMPLE, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "27"
        artist = "Ioan Dumitrescu"
        imageUri = "https://cards.scryfall.io/normal/front/0/1/01597ede-94e7-44a4-93c2-7fd1db11e92a.jpg"
    }
}
