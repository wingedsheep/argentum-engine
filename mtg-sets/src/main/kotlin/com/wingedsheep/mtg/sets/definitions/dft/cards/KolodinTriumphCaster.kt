package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Kolodin, Triumph Caster — Aetherdrift #210
 * {R}{W} · Legendary Creature — Human Pilot · 2/3
 *
 * Mounts and Vehicles you control have haste.
 * Whenever a Mount you control enters, it becomes saddled until end of turn.
 * Whenever a Vehicle you control enters, it becomes an artifact creature until end of turn.
 *
 * The two triggers are the free halves of Guidelight Matrix's two activated abilities, so they
 * reuse the same primitives:
 *  - Mount half → [Effects.BecomeSaddled], the marker half of a Saddle ability (CR 702.171b):
 *    an until-end-of-turn "saddled" status with no P/T or type change, which saddled-gated
 *    payoffs (Lagorin, Soul of Alacria) read.
 *  - Vehicle half → [Effects.AddCardType] "Creature" for the turn. A Vehicle is already an
 *    artifact (CR 301.7) and its printed P/T and keywords apply once it's a creature, so the
 *    Layer-4 type grant alone is the whole animation — exactly what crew does.
 *
 * Both fire off the entering permanent itself ([EffectTarget.TriggeringEntity], "it"), so they
 * are non-targeted and can't be fizzled. [TriggerBinding.ANY] rather than OTHER because the
 * oracle text says "a Mount/Vehicle you control", with no "another" clause — Kolodin is a Human
 * Pilot so it can never satisfy either filter itself, but a copy effect that made it a Mount
 * should still see its own entry.
 *
 * Haste is granted by a single Layer-6 [GrantKeyword] over the union filter rather than two
 * statics, matching the one printed ability.
 */
private val MountsAndVehiclesYouControl = GroupFilter(
    GameObjectFilter.Permanent
        .withAnyOfSubtypes(listOf(Subtype("Mount"), Subtype.VEHICLE))
        .youControl()
)

val KolodinTriumphCaster = card("Kolodin, Triumph Caster") {
    manaCost = "{R}{W}"
    colorIdentity = "RW"
    typeLine = "Legendary Creature — Human Pilot"
    power = 2
    toughness = 3
    oracleText = "Mounts and Vehicles you control have haste.\n" +
        "Whenever a Mount you control enters, it becomes saddled until end of turn.\n" +
        "Whenever a Vehicle you control enters, it becomes an artifact creature until end of turn."

    staticAbility {
        ability = GrantKeyword(Keyword.HASTE, MountsAndVehiclesYouControl)
    }

    triggeredAbility {
        trigger = Triggers.entersBattlefield(
            filter = GameObjectFilter.Permanent.withSubtype(Subtype("Mount")).youControl(),
            binding = TriggerBinding.ANY
        )
        effect = Effects.BecomeSaddled(EffectTarget.TriggeringEntity)
        description = "Whenever a Mount you control enters, it becomes saddled until end of turn."
    }

    triggeredAbility {
        trigger = Triggers.entersBattlefield(
            filter = GameObjectFilter.Permanent.withSubtype(Subtype.VEHICLE).youControl(),
            binding = TriggerBinding.ANY
        )
        effect = Effects.AddCardType("Creature", EffectTarget.TriggeringEntity, Duration.EndOfTurn)
        description =
            "Whenever a Vehicle you control enters, it becomes an artifact creature until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "210"
        artist = "Michal Ivan"
        flavorText = "\"Stay in formation! We win this together, or not at all.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/6/36bcd476-a236-4434-b191-5c8b6fa7be7b.jpg?1783907857"
    }
}
