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
 * Arctic Aven
 * {2}{U}
 * Creature — Bird Wizard
 * 2/1
 * Flying
 * This creature gets +1/+1 as long as you control a Plains.
 * {W}: This creature gains lifelink until end of turn.
 */
val ArcticAven = card("Arctic Aven") {
    manaCost = "{2}{U}"
    colorIdentity = "WU"
    typeLine = "Creature — Bird Wizard"
    oracleText = "Flying\n" +
        "This creature gets +1/+1 as long as you control a Plains.\n" +
        "{W}: This creature gains lifelink until end of turn. (Damage dealt by this creature also causes you to gain that much life.)"
    power = 2
    toughness = 1

    keywords(Keyword.FLYING)

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(1, 1, Filters.Self),
            condition = Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Land.withSubtype("Plains")),
        )
    }

    activatedAbility {
        cost = Costs.Mana("{W}")
        effect = Effects.GrantKeyword(Keyword.LIFELINK, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "42"
        artist = "Igor Kieryluk"
        imageUri = "https://cards.scryfall.io/normal/front/0/6/06f6aab1-c400-4d87-b68e-f36552e7417f.jpg"
    }
}
