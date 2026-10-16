package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Speakeasy Server
 * {4}{W}
 * Creature — Bird Citizen
 * 3/3
 * Flying
 * When this creature enters, you gain 1 life for each other creature you control.
 */
val SpeakeasyServer = card("Speakeasy Server") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Bird Citizen"
    oracleText =
        "Flying\nWhen this creature enters, you gain 1 life for each other creature you control."
    power = 3
    toughness = 3

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GainLife(
            DynamicAmounts.battlefield(
                Player.You,
                GameObjectFilter.Creature,
                excludeSelf = true,
            ).count(),
            EffectTarget.Controller,
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "32"
        artist = "Scott Murphy"
        flavorText =
            "\"Whatever ails you, friends, this will cure it for tonight! And leave you with a little inspiration for tomorrow.\""
        imageUri =
            "https://cards.scryfall.io/normal/front/e/0/e014d9d0-e6e1-4afb-b94f-b927a09b76c1.jpg?1783923151"
    }
}
