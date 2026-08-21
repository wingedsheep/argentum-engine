package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Revelsong Horn
 * {2}
 * Artifact
 *
 * {1}, {T}, Tap an untapped creature you control: Target creature gets +1/+1 until end of turn.
 *
 * - Three cost atoms in printed order: the mana, the Horn's own {T}, and a *separate*
 *   [Costs.TapPermanents] for the creature. `Costs.Tap` is the `{T}` symbol on the source;
 *   `Costs.TapPermanents(1, …)` is the "tap an untapped creature you control" clause, which is a
 *   different cost and can be paid with a summoning-sick creature (CR 302.6 only gates `{T}` in the
 *   creature's *own* ability).
 * - The tapped creature may be any creature you control, including one already chosen as the
 *   ability's target — the printed line puts no restriction on the overlap.
 * - [Effects.ModifyStats] defaults to `Duration.EndOfTurn`, which is the printed "until end of turn".
 */
val RevelsongHorn = card("Revelsong Horn") {
    manaCost = "{2}"
    typeLine = "Artifact"
    oracleText = "{1}, {T}, Tap an untapped creature you control: Target creature gets +1/+1 until end of turn."

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}"),
            Costs.Tap,
            Costs.TapPermanents(1, GameObjectFilter.Creature)
        )
        target = Targets.Creature
        effect = Effects.ModifyStats(1, 1, EffectTarget.ContextTarget(0))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "261"
        artist = "Franz Vohwinkel"
        flavorText = "A deflated sigh breathed into the horn emerges as an inspiring melody."
        imageUri = "https://cards.scryfall.io/normal/front/1/0/10a4801c-fa69-47a0-bdfa-f5f110fd0976.jpg?1783942710"
    }
}
