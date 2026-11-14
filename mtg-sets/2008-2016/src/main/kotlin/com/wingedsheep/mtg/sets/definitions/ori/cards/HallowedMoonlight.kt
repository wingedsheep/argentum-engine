package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.RedirectZoneChange
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Hallowed Moonlight — Magic Origins #16
 * {1}{W} · Instant
 * Until end of turn, if a creature would enter and it wasn't cast, exile it instead.
 * Draw a card.
 *
 * Mistcaller's floating battlefield-entry [RedirectZoneChange] without its `nontoken()` qualifier,
 * granted by the resolving spell (the Malicious Eclipse idiom: anchored to the caster, it outlives
 * the instant). Tokens are never cast, so a creature token that would be created this turn is put
 * into exile instead and then ceases to exist (2015-06-22 ruling). A creature spell that was cast
 * carries its cast origin and enters normally, wherever it was cast from; a copy of one wasn't cast
 * (CR 707.10), so it is exiled instead.
 */
val HallowedMoonlight = card("Hallowed Moonlight") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Until end of turn, if a creature would enter and it wasn't cast, exile it instead.\nDraw a card."

    spell {
        effect = Effects.GrantReplacementEffect(
            replacement = RedirectZoneChange(
                newDestination = Zone.EXILE,
                appliesTo = EventPattern.ZoneChangeEvent(
                    filter = GameObjectFilter.Creature.notCast(),
                    to = Zone.BATTLEFIELD,
                ),
            ),
            target = EffectTarget.Self,
            duration = Duration.EndOfTurn,
        ) then Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "16"
        artist = "Mike Bierek"
        flavorText = "Creatures of darkness instinctively hide from the light."
        imageUri = "https://cards.scryfall.io/normal/front/9/4/94fd0c0f-4a6a-47cf-9f50-df0bbf19aae4.jpg?1783938362"
    }
}
