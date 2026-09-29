package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Protocol Knight
 * {3}{U}
 * Creature — Human Knight
 * 3/4
 * When this creature enters, tap target creature an opponent controls. Put a stun counter on that
 * creature if you control another Knight.
 *
 * The Knight check is not an intervening "if": the tap always happens, and whether the stun counter
 * follows is asked at resolution. "Another" excludes this creature itself.
 */
val ProtocolKnight = card("Protocol Knight") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Knight"
    oracleText = "When this creature enters, tap target creature an opponent controls. Put a stun counter on that creature if you control another Knight. (If a permanent with a stun counter would become untapped, remove one from it instead.)"
    power = 3
    toughness = 4

    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(TargetFilter(GameObjectFilter.Creature.opponentControls()))
        effect = Effects.Tap(t) then Effects.If(
            condition = Conditions.YouControl(
                GameObjectFilter.Permanent.withSubtype(Subtype.KNIGHT),
                excludeSelf = true
            ),
            then = Effects.AddCounters(counterType = CounterType.STUN, count = 1, target = t)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "74"
        artist = "Volkan Baǵa"
        flavorText = "\"Ingress without proper writ of extraplanar intent? Unacceptable!\""
        imageUri = "https://cards.scryfall.io/normal/front/a/f/af6cb380-0b4d-4870-abef-928eb63e1702.jpg?1783917026"
    }
}
