package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Knockout Maneuver — Tarkir: Dragonstorm #147
 * {2}{G} · Sorcery
 *
 * Put a +1/+1 counter on target creature you control, then it deals damage equal to
 * its power to target creature an opponent controls.
 *
 * The two clauses run in order: add the +1/+1 counter first, then have the buffed
 * creature deal damage equal to its (now-increased) power to the opposing creature.
 * `damageSource = ContextTarget(0)` makes the controlled creature the source of the
 * damage; `DynamicAmounts.targetPower(0)` reads its power at resolution (after the
 * counter is applied).
 */
val KnockoutManeuver = card("Knockout Maneuver") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Put a +1/+1 counter on target creature you control, then it deals damage equal to its power to target creature an opponent controls."

    spell {
        val mine = target("creature you control", Targets.CreatureYouControl)
        val theirs = target("creature an opponent controls", Targets.CreatureOpponentControls)
        effect = Effects.Composite(listOf(
            Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, mine),
            Effects.DealDamage(
                amount = DynamicAmounts.targetPower(0),
                target = theirs,
                damageSource = EffectTarget.ContextTarget(0)
            )
        ))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "147"
        artist = "Aaron J. Riley"
        flavorText = "An exceptional bout during the Zenith celebration will be noted by Temur elders and result in recruitment for higher ranks."
        imageUri = "https://cards.scryfall.io/normal/front/9/d/9d218831-2a41-46a3-8e9d-93462cae5cab.jpg?1743204555"
        ruling("2025-04-04", "You can't cast Knockout Maneuver unless you choose a creature you control and a creature an opponent controls as targets.")
        ruling("2025-04-04", "If either creature is an illegal target as Knockout Maneuver resolves, the creature you control won't deal damage. If the creature you control is an illegal target, you won't put a +1/+1 counter on it even if it's still on the battlefield.")
    }
}
