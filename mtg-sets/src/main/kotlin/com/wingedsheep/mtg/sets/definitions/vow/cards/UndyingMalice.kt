package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.AddCountersEffect
import com.wingedsheep.sdk.scripting.effects.GrantTriggeredAbilityEffect
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Undying Malice
 * {B}
 * Instant
 *
 * Until end of turn, target creature gains "When this creature dies, return it to the
 * battlefield tapped under its owner's control with a +1/+1 counter on it."
 *
 * Models the granted clause as a SELF-bound dies trigger (battlefield → graveyard) granted for the
 * turn — the same leaves-the-battlefield return shape used by Earthbend's return-tapped clause. On
 * resolution the creature is moved graveyard → battlefield tapped (gated with `fromZone = GRAVEYARD`
 * so it no-ops if it already left the graveyard), then a +1/+1 counter is placed on it. The
 * graveyard → battlefield return keeps the same entity id, so `EffectTarget.Self` in the follow-up
 * [AddCountersEffect] lands on the returned permanent. `MoveToZoneEffect` returns under the owner's
 * control by default, matching "under its owner's control".
 */
val UndyingMalice = card("Undying Malice") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Until end of turn, target creature gains \"When this creature dies, return it to the battlefield tapped under its owner's control with a +1/+1 counter on it.\""

    spell {
        val t = target("target", Targets.Creature)
        effect = GrantTriggeredAbilityEffect(
            ability = TriggeredAbility.create(
                trigger = Triggers.Dies.event,
                binding = Triggers.Dies.binding,
                effect = Effects.Composite(
                    Effects.Move(
                        target = EffectTarget.Self,
                        destination = Zone.BATTLEFIELD,
                        placement = ZonePlacement.Tapped,
                        fromZone = Zone.GRAVEYARD
                    ),
                    AddCountersEffect(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self),
                ),
                descriptionOverride = "When this creature dies, return it to the battlefield tapped under its owner's control with a +1/+1 counter on it."
            ),
            target = t,
            duration = Duration.EndOfTurn
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "134"
        artist = "Igor Kieryluk"
        flavorText = "Few take kindly to being put in their place, especially if their place is the grave."
        imageUri = "https://cards.scryfall.io/normal/front/8/e/8eb38041-043a-4b18-9d9a-f1283684e8f1.jpg?1783924849"
    }
}
