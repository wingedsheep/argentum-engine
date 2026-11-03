package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Worf, Honor-Bound Warrior — Star Trek #256
 * {R/W}{R/W} · Legendary Creature — Klingon Warrior Officer · 1/1
 *
 * Whenever you attack while creatures you control have total power 8 or greater, double Worf's
 * power until end of turn.
 * Tap two untapped artifacts and/or creatures you control: Put a +1/+1 counter on Worf. Activate
 * only as a sorcery.
 *
 * The "while …" gate is a `triggerRestriction` checked at the attack declaration (same shape as
 * Tactical Officer). Doubling power is +X/+0 where X is Worf's power as the effect begins (Grunn,
 * the Lonely King shape). The tap cost may include Worf himself — it has no {T} symbol, so
 * summoning sickness doesn't stop it.
 */
val WorfHonorBoundWarrior = card("Worf, Honor-Bound Warrior") {
    manaCost = "{R/W}{R/W}"
    colorIdentity = "RW"
    typeLine = "Legendary Creature — Klingon Warrior Officer"
    oracleText = "Whenever you attack while creatures you control have total power 8 or greater, " +
        "double Worf's power until end of turn.\n" +
        "Tap two untapped artifacts and/or creatures you control: Put a +1/+1 counter on Worf. " +
        "Activate only as a sorcery."
    power = 1
    toughness = 1

    triggeredAbility {
        trigger = Triggers.you.attacks()
        triggerRestriction = Conditions.CompareAmounts(
            DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature).sumPower(),
            ComparisonOperator.GTE,
            8
        )
        effect = Effects.ModifyStats(DynamicAmounts.sourcePower(), DynamicAmounts.fixed(0), EffectTarget.Self)
    }

    activatedAbility {
        cost = Costs.TapPermanents(2, GameObjectFilter.CreatureOrArtifact)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        timing = TimingRule.SorcerySpeed
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "256"
        artist = "Alexander Mokhov"
        flavorText = "\"Four against one is not a fair fight. I will wait if you wish to find more allies.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/2/b2d1955f-e834-4157-a139-9b6ec94bcd2a.jpg?1792508408"
    }
}
