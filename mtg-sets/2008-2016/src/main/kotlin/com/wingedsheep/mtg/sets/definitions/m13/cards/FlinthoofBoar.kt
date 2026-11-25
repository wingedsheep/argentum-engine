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
 * Flinthoof Boar
 * {1}{G}
 * Creature — Boar
 * 2/2
 * This creature gets +1/+1 as long as you control a Mountain.
 * {R}: This creature gains haste until end of turn.
 */
val FlinthoofBoar = card("Flinthoof Boar") {
    manaCost = "{1}{G}"
    colorIdentity = "GR"
    typeLine = "Creature — Boar"
    oracleText = "This creature gets +1/+1 as long as you control a Mountain.\n" +
        "{R}: This creature gains haste until end of turn. (It can attack and {T} this turn.)"
    power = 2
    toughness = 2

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(1, 1, Filters.Self),
            condition = Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Land.withSubtype("Mountain")),
        )
    }

    activatedAbility {
        cost = Costs.Mana("{R}")
        effect = Effects.GrantKeyword(Keyword.HASTE, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "171"
        artist = "Erica Yang"
        flavorText = "Through the smoke and flames it hunts, sniffing out the panicked and confused."
        imageUri = "https://cards.scryfall.io/normal/front/7/e/7e380b99-0173-4083-a4a2-222ad98b904a.jpg"
    }
}
