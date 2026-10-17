package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Bouncer's Beatdown
 * {2}{G}
 * Instant
 * This spell costs {2} less to cast if it targets a black permanent.
 * Bouncer's Beatdown deals X damage to target creature or planeswalker, where X is the greatest
 * power among creatures you control. If that creature or planeswalker would die this turn, exile
 * it instead.
 *
 * The discount is the Ride's End shape — [SpellCostTarget.SelfCast] over
 * [CostReductionSource.FixedIfAnyTargetMatches] gated on the chosen target being a black permanent.
 * X is read at resolution (greatest power among your creatures, 0 if you control none), and the
 * Scorching Dragonfire pairing of damage plus [Effects.MarkExileOnDeath] handles the exile rider.
 */
val BouncersBeatdown = card("Bouncer's Beatdown") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "This spell costs {2} less to cast if it targets a black permanent.\n" +
        "Bouncer's Beatdown deals X damage to target creature or planeswalker, where X is the greatest " +
        "power among creatures you control. If that creature or planeswalker would die this turn, exile it instead."

    spell {
        val t = target(Targets.CreatureOrPlaneswalker)
        effect = Effects.DealDamage(
            DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature).maxPower(),
            t,
        ) then Effects.MarkExileOnDeath(t)
    }

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.FixedIfAnyTargetMatches(
                    amount = 2,
                    filter = GameObjectFilter.Permanent.withColor(Color.BLACK),
                ),
            ),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "135"
        artist = "Mathias Kollros"
        imageUri = "https://cards.scryfall.io/normal/front/5/4/54772e15-d99d-4eec-ba8d-b9202a7e318b.jpg?1783923110"
    }
}
