package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Feign Death
 * {B}
 * Instant
 *
 * Until end of turn, target creature gains "When this creature dies, return it to the
 * battlefield tapped under its owner's control with a +1/+1 counter on it."
 *
 * The granted clause is a self-bound dies trigger granted for the turn. Its return moves the card
 * graveyard → battlefield tapped (gated on `fromZone = GRAVEYARD`, so it no-ops if the card already
 * left the graveyard) and the +1/+1 counter rides the move as an entry counter, so the creature
 * *enters with* the counter rather than receiving it afterwards. `MoveToZoneEffect` returns under
 * the owner's control by default.
 */
val FeignDeath = card("Feign Death") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Until end of turn, target creature gains \"When this creature dies, return it to the battlefield tapped under its owner's control with a +1/+1 counter on it.\""

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.GrantTriggeredAbility(
            ability = TriggeredAbility.create(
                trigger = Triggers.self.dies(),
                effect = Effects.Move(
                    target = EffectTarget.Self,
                    destination = Zone.BATTLEFIELD,
                    placement = ZonePlacement.Tapped,
                    fromZone = Zone.GRAVEYARD,
                    addCounterType = CounterType.PLUS_ONE_PLUS_ONE
                ),
                descriptionOverride = "When this creature dies, return it to the battlefield tapped under its owner's control with a +1/+1 counter on it."
            ),
            target = t,
            duration = Duration.EndOfTurn
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "103"
        artist = "Maria Zolotukhina"
        flavorText = "A drastic measure for dangerous times."
        imageUri = "https://cards.scryfall.io/normal/front/3/2/32af7061-fca6-4a06-be3c-01881e6b96f7.jpg?1783926495"
    }
}
