package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Luminous Broodmoth
 * {2}{W}{W}
 * Creature — Insect
 * 3/4
 *
 * Flying
 * Whenever a creature you control without flying dies, return it to the battlefield under its
 * owner's control with a flying counter on it.
 *
 * Valkyrie's Call shape: a filtered dies trigger whose [EffectTarget.TriggeringEntity] is the card
 * now in the graveyard. `fromZone = GRAVEYARD` makes "return it" a no-op if the card left the
 * graveyard (or was a token) before resolution; `addCounterType` has it enter with the flying
 * counter. The returned creature has flying, so a second death doesn't retrigger.
 */
val LuminousBroodmoth = card("Luminous Broodmoth") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Insect"
    power = 3
    toughness = 4
    oracleText = "Flying\nWhenever a creature you control without flying dies, return it to the " +
        "battlefield under its owner's control with a flying counter on it."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl().withoutKeyword(Keyword.FLYING)).dies()
        effect = Effects.Move(
            EffectTarget.TriggeringEntity,
            Zone.BATTLEFIELD,
            fromZone = Zone.GRAVEYARD,
            addCounterType = CounterType.FLYING
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "21"
        artist = "Lie Setiawan"
        flavorText = "It soars through currents of moonbeams to give hope to the forsaken."
        imageUri = "https://cards.scryfall.io/normal/front/b/b/bb65df55-d6a6-4a57-a903-e5eb17637982.jpg?1783931087"
    }
}
