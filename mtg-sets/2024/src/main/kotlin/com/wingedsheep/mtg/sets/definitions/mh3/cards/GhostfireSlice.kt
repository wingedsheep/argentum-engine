package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostGating
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Ghostfire Slice — Modern Horizons 3 #123
 * {2}{R}
 * Instant
 * Devoid (This card has no color.)
 * This spell costs {2} less to cast if an opponent controls a multicolored permanent.
 * Ghostfire Slice deals 4 damage to any target.
 *
 * The discount is a self-cast [ModifySpellCost] gated on an opponent controlling a permanent
 * matching [GameObjectFilter.Multicolored] — the same rail Distorted Curiosity uses. It only eats
 * generic mana, so the floor is {R}.
 */
val GhostfireSlice = card("Ghostfire Slice") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Devoid (This card has no color.)\n" +
        "This spell costs {2} less to cast if an opponent controls a multicolored permanent.\n" +
        "Ghostfire Slice deals 4 damage to any target."

    keywords(Keyword.DEVOID)

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGeneric(2),
            gating = CostGating.OnlyIf(Conditions.OpponentControls(GameObjectFilter.Multicolored)),
        )
    }

    spell {
        val t = target(Targets.Any)
        effect = Effects.DealDamage(4, t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "123"
        artist = "Johann Bodin"
        flavorText = "Just because you can't see it doesn't mean it won't hurt."
        imageUri = "https://cards.scryfall.io/normal/front/2/a/2adea3ee-138f-455b-a001-586883c44758.jpg?1783911271"
    }
}
