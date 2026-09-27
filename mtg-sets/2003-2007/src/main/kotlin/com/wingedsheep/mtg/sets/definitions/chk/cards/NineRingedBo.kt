package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Nine-Ringed Bo
 * {3}
 * Artifact
 * {T}: This artifact deals 1 damage to target Spirit creature. If that creature would die this
 * turn, exile it instead.
 *
 * Yamabushi's Flame's shape: "that creature" is the target itself (not "a creature dealt damage
 * this way"), so the [Effects.MarkExileOnDeath] mark goes on first — it holds for the rest of the
 * turn even if the damage is prevented, and a Spirit killed by this very ping is exiled.
 */
val NineRingedBo = card("Nine-Ringed Bo") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{T}: This artifact deals 1 damage to target Spirit creature. If that creature would " +
        "die this turn, exile it instead."

    activatedAbility {
        cost = Costs.Tap
        val t = target(TargetFilter(GameObjectFilter.Creature.withSubtype("Spirit")))
        effect = Effects.MarkExileOnDeath(t) then Effects.DealDamage(1, t)
        description = "{T}: Deal 1 damage to target Spirit creature. If it would die this turn, exile it instead."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "263"
        artist = "Ralph Horsley"
        flavorText = "\"We received an anonymous letter suggesting that Kumano holds a secret to defeating " +
            "the kami, but he is nowhere to be found.\"\n—General Takeno, letter to Lord Konda"
        imageUri = "https://cards.scryfall.io/normal/front/4/4/445e60aa-1a0a-499e-9887-7d17a1f80a02.jpg?1783944276"
    }
}
