package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.RegenerateEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Duskworker — Mirrodin #166
 * {4} · Artifact Creature — Construct · 2/2
 *
 * Whenever this creature becomes blocked, regenerate it.
 * {3}: This creature gets +1/+0 until end of turn.
 *
 * Modelling notes:
 * - The *unfiltered* [Triggers.BecomesBlocked] is the right shape here: the printed text has no
 *   blocker restriction, so a gang block still yields exactly one trigger and one regeneration
 *   shield (contrast Ogre Leadfoot in this set, which needs the per-blocker filtered form).
 * - The shield is applied at resolution and lasts until end of turn, so it survives the whole
 *   combat-damage step and can still save the Duskworker from a later removal spell that turn.
 */
val Duskworker = card("Duskworker") {
    manaCost = "{4}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Construct"
    power = 2
    toughness = 2
    oracleText = "Whenever this creature becomes blocked, regenerate it.\n" +
        "{3}: This creature gets +1/+0 until end of turn."

    triggeredAbility {
        trigger = Triggers.BecomesBlocked
        effect = RegenerateEffect(EffectTarget.Self)
        description = "Whenever this creature becomes blocked, regenerate it."
    }

    activatedAbility {
        cost = Costs.Mana("{3}")
        effect = Effects.ModifyStats(1, 0, EffectTarget.Self)
        description = "{3}: This creature gets +1/+0 until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "166"
        artist = "Greg Staples"
        flavorText = "At the setting of each sun, it emerges to clean Mirrodin's floor of the day's carrion."
        imageUri = "https://cards.scryfall.io/normal/front/7/d/7d18dd3b-980b-4833-93fd-79dc3a260da4.jpg?1783944523"
    }
}
