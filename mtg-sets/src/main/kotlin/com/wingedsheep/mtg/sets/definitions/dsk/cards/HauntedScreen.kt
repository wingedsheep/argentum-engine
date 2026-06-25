package com.wingedsheep.mtg.sets.definitions.dsk.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Haunted Screen
 * {3}
 * Artifact
 *
 * {T}: Add {W} or {B}.
 * {T}, Pay 1 life: Add {G}, {U}, or {R}.
 * {7}: Put seven +1/+1 counters on this artifact. It becomes a 0/0 Spirit creature in
 *      addition to its other types. Activate only once.
 *
 * Following the established dual-mana-rock convention (Troll-Horn Cameo), each "or" mana
 * ability is modeled as one single-color mana ability per producible color. The animate
 * ability puts seven +1/+1 counters (so the 0/0 base becomes a 7/7) and makes the artifact
 * a Spirit creature in addition to its other types via [Effects.BecomeCreature]
 * (CREATURE is always added, the artifact type is kept), permanently. "Activate only once"
 * maps to [ActivationRestriction.Once].
 */
val HauntedScreen = card("Haunted Screen") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{T}: Add {W} or {B}.\n{T}, Pay 1 life: Add {G}, {U}, or {R}.\n{7}: Put seven " +
        "+1/+1 counters on this artifact. It becomes a 0/0 Spirit creature in addition to its " +
        "other types. Activate only once."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.WHITE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLACK)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.PayLife(1))
        effect = Effects.AddMana(Color.GREEN)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.PayLife(1))
        effect = Effects.AddMana(Color.BLUE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.PayLife(1))
        effect = Effects.AddMana(Color.RED)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Mana("{7}")
        effect = Effects.Composite(
            Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 7, EffectTarget.Self),
            Effects.BecomeCreature(
                target = EffectTarget.Self,
                power = 0,
                toughness = 0,
                creatureTypes = setOf("Spirit"),
                duration = Duration.Permanent,
            ),
        )
        restrictions = listOf(ActivationRestriction.Once)
        description = "{7}: Put seven +1/+1 counters on this artifact. It becomes a 0/0 Spirit " +
            "creature in addition to its other types. Activate only once."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "250"
        artist = "Sean Murray"
        imageUri = "https://cards.scryfall.io/normal/front/5/e/5e7d552f-a7ac-4a49-a582-9d378137005f.jpg?1726286805"
    }
}
