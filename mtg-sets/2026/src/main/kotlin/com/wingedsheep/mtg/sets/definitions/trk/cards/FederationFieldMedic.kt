package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.StatePredicate

/**
 * Federation Field Medic — Star Trek #17
 * {3}{W} · Creature — Human Doctor · 3/2
 *
 * Vigilance
 * When this creature enters, choose one —
 * • Put a +1/+1 counter on target creature. It gains vigilance until end of turn.
 * • Draw a card if you control a permanent with a counter on it.
 *
 * The second mode's condition is checked on resolution, over any permanent you control
 * (not just creatures) carrying any kind of counter.
 */
val FederationFieldMedic = card("Federation Field Medic") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Doctor"
    power = 3
    toughness = 2
    oracleText = "Vigilance\nWhen this creature enters, choose one —\n" +
        "• Put a +1/+1 counter on target creature. It gains vigilance until end of turn.\n" +
        "• Draw a card if you control a permanent with a counter on it."

    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = ModalEffect.chooseOne(
            mode("Put a +1/+1 counter on target creature. It gains vigilance until end of turn.") {
                val creature = target(TargetFilter.Creature)
                effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature) then
                    Effects.GrantKeyword(Keyword.VIGILANCE, creature)
            },
            Mode.noTarget(
                Effects.If(
                    condition = Conditions.YouControl(
                        GameObjectFilter.Permanent.copy(
                            statePredicates = listOf(StatePredicate.HasAnyCounter)
                        )
                    ),
                    then = Effects.DrawCards(1)
                ),
                "Draw a card if you control a permanent with a counter on it."
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "17"
        artist = "Zoltan Boros"
        flavorText = "\"Stop whining. Sto-Vo-Kor will be there later.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/3/b312dacd-b87f-44c1-a804-bee221e70f52.jpg?1785981333"
    }
}
