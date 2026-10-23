package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Hallowed Respite (Innistrad: Midnight Hunt #227)
 * {W}{U} · Sorcery
 * Exile target nonlegendary creature, then return it to the battlefield under its owner's control.
 * If it entered under your control, put a +1/+1 counter on it. Otherwise, tap it.
 * Flashback {1}{W}{U}
 *
 * The plain `Move` to the battlefield returns the card under its owner's control, and the target
 * handle follows the card to its new object (Momentary Blink, Essence Flux). "Entered under your
 * control" is then a control check on the returned permanent.
 */
val HallowedRespite = card("Hallowed Respite") {
    manaCost = "{W}{U}"
    colorIdentity = "WU"
    typeLine = "Sorcery"
    oracleText = "Exile target nonlegendary creature, then return it to the battlefield under its owner's control. " +
        "If it entered under your control, put a +1/+1 counter on it. Otherwise, tap it.\n" +
        "Flashback {1}{W}{U} (You may cast this card from your graveyard for its flashback cost. Then exile it.)"

    spell {
        val creature = target(TargetFilter.Creature.nonlegendary())
        effect = Effects.Exile(creature) then
            Effects.Move(creature, Zone.BATTLEFIELD) then
            Effects.If(
                condition = Conditions.TargetMatchesFilter(GameObjectFilter.Any.youControl(), creature),
                then = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature),
                otherwise = Effects.Tap(creature)
            )
    }

    keywordAbility(KeywordAbility.flashback("{1}{W}{U}"))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "227"
        artist = "Jason A. Engle"
        imageUri = "https://cards.scryfall.io/normal/front/f/2/f2877523-6321-47e6-8be4-755ec21676c6.jpg?1783925561"
    }
}
