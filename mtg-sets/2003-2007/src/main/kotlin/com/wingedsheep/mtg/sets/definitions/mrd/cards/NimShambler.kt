package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStatsEffect
import com.wingedsheep.sdk.scripting.effects.RegenerateEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Nim Shambler — Mirrodin #72
 * {2}{B}{B} · Creature — Zombie · 2/1
 *
 * This creature gets +1/+0 for each artifact you control.
 * Sacrifice a creature: Regenerate this creature.
 *
 * The nim's power is a [GrantDynamicStatsEffect] scoped to the source itself
 * ([GroupFilter.source]) — a Layer 7c bonus that recomputes continuously rather than a snapshot,
 * so it grows and shrinks as artifacts enter and leave. Toughness is untouched: the Shambler
 * stays a 1-toughness liability no matter how wide the artifact board gets.
 *
 * The regeneration cost is a bare sacrifice with no mana attached, so the Shambler can eat the
 * rest of the board — including itself, which is a legal (if pointless) choice: sacrificing the
 * Shambler to its own ability pays the cost, and the regeneration shield then has nothing to
 * protect.
 */
val NimShambler = card("Nim Shambler") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie"
    power = 2
    toughness = 1
    oracleText = "This creature gets +1/+0 for each artifact you control.\n" +
        "Sacrifice a creature: Regenerate this creature."

    staticAbility {
        ability = GrantDynamicStatsEffect(
            filter = GroupFilter.source(),
            powerBonus = DynamicAmounts.battlefield(Player.You, GameObjectFilter.Artifact).count(),
            toughnessBonus = DynamicAmount.Fixed(0)
        )
    }

    activatedAbility {
        cost = Costs.Sacrifice(GameObjectFilter.Creature)
        effect = RegenerateEffect(EffectTarget.Self)
        description = "Sacrifice a creature: Regenerate this creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "72"
        artist = "Adam Rex"
        flavorText = "Called \"the Dross\" by its inhabitants, Mephidross is home to the nim, " +
            "Mirrodin's mindless, ravenous undead."
        imageUri = "https://cards.scryfall.io/normal/front/e/5/e59c09a0-a374-46c1-978f-ec7478dc7ab7.jpg?1783944546"
    }
}
