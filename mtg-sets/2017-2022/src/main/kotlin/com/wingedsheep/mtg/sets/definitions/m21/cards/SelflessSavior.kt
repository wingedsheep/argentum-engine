package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Selfless Savior — Core Set 2021 #36
 * {W} · Creature — Dog · 1/1
 *
 * Sacrifice this creature: Another target creature you control gains indestructible until end of turn.
 */
val SelflessSavior = card("Selfless Savior") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Creature — Dog"
    power = 1
    toughness = 1
    oracleText = "Sacrifice this creature: Another target creature you control gains indestructible until end " +
        "of turn. (Damage and effects that say \"destroy\" don't destroy it.)"

    activatedAbility {
        cost = Costs.SacrificeSelf
        val creature = target(TargetFilter.OtherCreatureYouControl)
        effect = Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, creature, Duration.EndOfTurn)
        description = "Sacrifice this creature: Another target creature you control gains indestructible " +
            "until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "36"
        artist = "Ralph Horsley"
        flavorText = "She raised him from an orphaned pup and gave him a life of love. With his last act, he " +
            "thanked her."
        imageUri = "https://cards.scryfall.io/normal/front/6/9/6911759c-7177-402c-a95a-f9f46efaf521.jpg?1783930734"
    }
}
