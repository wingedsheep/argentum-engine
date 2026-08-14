package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sanctuary Wall — Murders at Karlov Manor #32
 * {1}{W} · Artifact Creature — Wall · 0/4
 *
 * Defender
 * {2}{W}, {T}: Tap target creature. You may put a stun counter on it. If you do, put a stun counter
 * on this creature.
 *
 * A tapper that pays for the upgrade in its own untap step. The stun counter is the *whole* choice:
 * without it the target simply untaps next turn, and with it the target skips an untap (CR 122.6d)
 * but so does the Wall, meaning the ability can only be used every other turn. Declining is often
 * right, which is why the "may" is real and not decorative.
 *
 * Modelled as `Tap` followed by a `MayEffect` over both counter placements. The two counters go
 * together — the printed "if you do" only comes apart in the corner case where the target genuinely
 * can't receive the counter (an effect like Solemnity, or a permanent that can't have counters put
 * on it): the rules would then skip the Wall's counter too, while this script places it. There is no
 * "counters were added" `SuccessCriterion` to gate on today, so that corner is knowingly unmodelled
 * rather than silently assumed away.
 *
 * The Wall taps as part of the cost, so it is already tapped when the ability resolves; the stun
 * counter it takes therefore bites on its controller's very next untap step.
 *
 * Defender is only on the printed card — nothing here grants or removes it, and the Wall's 0 power
 * makes the "tap an attacker before blocks" line the point rather than a combat trick.
 */
val SanctuaryWall = card("Sanctuary Wall") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Artifact Creature — Wall"
    oracleText = "Defender\n" +
        "{2}{W}, {T}: Tap target creature. You may put a stun counter on it. If you do, put a stun " +
        "counter on this creature. (If a permanent with a stun counter would become untapped, remove " +
        "one from it instead.)"
    power = 0
    toughness = 4

    keywords(Keyword.DEFENDER)

    activatedAbility {
        val creature = target("target creature", Targets.Creature)
        cost = Costs.Composite(
            Costs.Mana("{2}{W}"),
            Costs.Tap
        )
        effect = Effects.Composite(
            Effects.Tap(creature),
            MayEffect(
                Effects.Composite(
                    Effects.AddCounters(Counters.STUN, 1, creature),
                    Effects.AddCounters(Counters.STUN, 1, EffectTarget.Self)
                ),
                descriptionOverride = "Put a stun counter on it? (Sanctuary Wall also gets one.)"
            )
        )
        description = "Tap target creature. You may put a stun counter on it. If you do, put a stun " +
            "counter on this creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "32"
        artist = "Josu Solano"
        imageUri = "https://cards.scryfall.io/normal/front/4/a/4a009ba2-c7b9-4cf6-bb90-9d6fd589e932.jpg?1783912918"
    }
}
