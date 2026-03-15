package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Sentinel of the Pearl Trident
 * {4}{U}
 * Creature — Merfolk Soldier
 * 3/3
 * Flash
 * When Sentinel of the Pearl Trident enters, you may exile target historic permanent
 * you control. If you do, return that card to the battlefield under its owner's control
 * at the beginning of the next end step.
 */
val SentinelOfThePearlTrident = card("Sentinel of the Pearl Trident") {
    manaCost = "{4}{U}"
    typeLine = "Creature — Merfolk Soldier"
    oracleText = "Flash\nWhen Sentinel of the Pearl Trident enters, you may exile target historic permanent you control. If you do, return that card to the battlefield under its owner's control at the beginning of the next end step."
    power = 3
    toughness = 3

    keywords(Keyword.FLASH)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val t = target("historic", TargetPermanent(
            filter = TargetFilter(GameObjectFilter.Historic.youControl())
        ))
        effect = MayEffect(EffectPatterns.exileUntilEndStep(t))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "65"
        artist = "Jonas De Ro"
        imageUri = "https://cards.scryfall.io/normal/front/7/d/7da73230-2562-4e7d-9f03-b0508b9a31e0.jpg?1562738372"
        ruling("2020-08-07", "If a token is exiled this way, it will cease to exist and won't return to the battlefield.")
        ruling("2020-08-07", "The exiled card will return to the battlefield at the beginning of the end step even if Sentinel of the Pearl Trident is no longer on the battlefield.")
        ruling("2020-08-07", "Auras attached to the exiled permanent will be put into their owners' graveyards. Equipment attached to the exiled permanent will become unattached and remain on the battlefield. Any counters on the exiled permanent will cease to exist. Once the exiled permanent returns, it's considered a new object with no relation to the object that it was.")
    }
}
