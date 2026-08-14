package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import com.wingedsheep.sdk.scripting.values.EntityReference

/** Sita Varma's power at resolution — read off the ability's source, not the iteration entity. */
private val SitaVarmasPower: DynamicAmount = DynamicAmount.EntityProperty(
    EntityReference.Source,
    EntityNumericProperty.Power
)

/**
 * Sita Varma, Masked Racer — Aetherdrift #223
 * {G}{U} · Legendary Creature — Human Rogue · 2/3
 *
 * Exhaust — {X}{G}{G}{U}: Put X +1/+1 counters on Sita Varma. Then you may have the base power and
 * toughness of each other creature you control become equal to Sita Varma's power until end of
 * turn. (Activate each exhaust ability only once.)
 *
 * Order is load-bearing and matches the printed "Then": the counters land first, so the power the
 * rest of the team copies is already `2 + X`. Inside the `ForEachInGroup` body, `EffectTarget.Self`
 * is the creature being set (the iteration entity) while `EntityReference.Source` is still Sita
 * Varma — which is what lets one body both read her power and write each other creature's.
 *
 * `SetBasePowerAndToughness` sets Layer 7b, so each affected creature's own +1/+1 counters still
 * add on top in 7d — the base is replaced, not the final value.
 */
val SitaVarmaMaskedRacer = card("Sita Varma, Masked Racer") {
    manaCost = "{G}{U}"
    colorIdentity = "GU"
    typeLine = "Legendary Creature — Human Rogue"
    power = 2
    toughness = 3
    oracleText = "Exhaust — {X}{G}{G}{U}: Put X +1/+1 counters on Sita Varma. Then you may have " +
        "the base power and toughness of each other creature you control become equal to Sita " +
        "Varma's power until end of turn. (Activate each exhaust ability only once.)"

    activatedAbility {
        cost = Costs.Mana("{X}{G}{G}{U}")
        isExhaust = true
        effect = Effects.Composite(
            Effects.AddDynamicCounters(
                Counters.PLUS_ONE_PLUS_ONE,
                DynamicAmount.XValue,
                EffectTarget.Self
            ),
            MayEffect(
                Effects.ForEachInGroup(
                    filter = GroupFilter.OtherCreaturesYouControl,
                    effect = Effects.SetBasePowerAndToughness(
                        target = EffectTarget.Self,
                        power = SitaVarmasPower,
                        toughness = SitaVarmasPower,
                        duration = Duration.EndOfTurn
                    )
                ),
                descriptionOverride = "You may have the base power and toughness of each other " +
                    "creature you control become equal to Sita Varma's power until end of turn."
            ),
        )
        description = "Exhaust — {X}{G}{G}{U}: Put X +1/+1 counters on Sita Varma. Then you may " +
            "have the base power and toughness of each other creature you control become equal " +
            "to Sita Varma's power until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "223"
        artist = "Kai Carpenter"
        imageUri = "https://cards.scryfall.io/normal/front/e/b/eb523c06-6f3e-4e19-b777-19aefc4a4e02.jpg?1783907851"

        ruling(
            "2025-02-07",
            "If an exhaust ability of a permanent is activated, and then that permanent leaves the " +
                "battlefield and returns to the battlefield, it becomes a new object so its " +
                "exhaust ability can be activated again."
        )
    }
}
