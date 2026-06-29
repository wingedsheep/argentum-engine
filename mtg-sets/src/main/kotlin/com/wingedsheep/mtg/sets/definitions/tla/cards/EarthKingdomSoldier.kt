package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ForEachTargetEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Earth Kingdom Soldier
 * {4}{G/W}
 * Creature — Human Soldier
 * 3/4
 *
 * Vigilance
 * When this creature enters, put a +1/+1 counter on each of up to two target creatures you control.
 *
 * The ETB mirrors Byrke, Long Ear of the Law's "each of up to two target creatures" shape — an
 * optional `count = 2` [TargetCreature] (filtered to creatures you control) fanned out with
 * [ForEachTargetEffect] so each chosen creature gets one +1/+1 counter.
 */
val EarthKingdomSoldier = card("Earth Kingdom Soldier") {
    manaCost = "{4}{G/W}"
    colorIdentity = "GW"
    typeLine = "Creature — Human Soldier"
    power = 3
    toughness = 4
    oracleText = "Vigilance\n" +
        "When this creature enters, put a +1/+1 counter on each of up to two target creatures you control."

    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        target(
            "up to two target creatures you control",
            TargetCreature(count = 2, optional = true, filter = TargetFilter.CreatureYouControl)
        )
        effect = ForEachTargetEffect(
            listOf(Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.ContextTarget(0)))
        )
        description = "When this creature enters, put a +1/+1 counter on each of up to two target creatures you control."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "216"
        artist = "Rafater"
        flavorText = "Trained to be steady as the earth beneath their feet."
        imageUri = "https://cards.scryfall.io/normal/front/2/d/2d543d35-945a-4ffa-beb7-7c5d4f894f79.jpg?1764121553"
    }
}
