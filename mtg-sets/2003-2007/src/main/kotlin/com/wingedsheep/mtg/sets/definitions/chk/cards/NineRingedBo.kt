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
 * "That creature" is the target itself (not "a creature dealt damage this way"), so the
 * [Effects.MarkExileOnDeath] mark holds for the rest of the turn even if the damage is prevented.
 * It follows the damage in the printed order: the mark is a floating effect and the Spirit only
 * dies to state-based actions after the ability resolves, so a Spirit killed by this very ping is
 * still exiled.
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
        effect = Effects.DealDamage(1, t) then Effects.MarkExileOnDeath(t)
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
