package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.bargain
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.MarkExileOnDeathEffect
import com.wingedsheep.sdk.scripting.targets.TargetCreatureOrPlaneswalker

/**
 * Torch the Tower
 * {R}
 * Instant
 *
 * Bargain (You may sacrifice an artifact, enchantment, or token as you cast this spell.)
 * Torch the Tower deals 2 damage to target creature or planeswalker. If this spell was bargained,
 * instead it deals 3 damage to that permanent and you scry 1.
 * If a permanent dealt damage by Torch the Tower would die this turn, exile it instead.
 *
 * The spell-rider shape of bargain (CR 702.166c): the fact is read off the spell while it is still
 * on the stack, so the payoff is gated on [Conditions.WasBargained] at resolution.
 *
 * "Instead" here swaps the whole damage clause, so this is a true either/or branch
 * ([ConditionalEffect] with an `elseEffect`) rather than a base effect plus a rider — the bargained
 * branch deals 3, not 2 + 1, which matters for damage-replacement effects that scale off the amount.
 * The scry rides along on the bargained branch only.
 *
 * [MarkExileOnDeathEffect] carries the last clause. Per the WOE ruling, that replacement exiles the
 * target if it would die this turn **for any reason**, not just from this damage — which is exactly
 * what the marker does — so it applies unconditionally alongside either damage branch.
 */
val TorchTheTower = card("Torch the Tower") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Bargain (You may sacrifice an artifact, enchantment, or token as you cast this " +
        "spell.)\n" +
        "Torch the Tower deals 2 damage to target creature or planeswalker. If this spell was " +
        "bargained, instead it deals 3 damage to that permanent and you scry 1.\n" +
        "If a permanent dealt damage by Torch the Tower would die this turn, exile it instead."

    bargain()

    spell {
        val permanent = target("target creature or planeswalker", TargetCreatureOrPlaneswalker())
        effect = Effects.Composite(
            ConditionalEffect(
                condition = Conditions.WasBargained,
                effect = Effects.Composite(
                    Effects.DealDamage(3, permanent),
                    Effects.Scry(1),
                ),
                elseEffect = Effects.DealDamage(2, permanent),
            ),
            MarkExileOnDeathEffect(permanent),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "153"
        artist = "Uriah Voth"
        imageUri = "https://cards.scryfall.io/normal/front/b/3/b3d6027c-813f-46df-95b4-e2e305a67620.jpg?1783915088"

        ruling(
            "2023-09-01",
            "Torch the Tower's last replacement effect will exile the target permanent if it would " +
                "die this turn for any reason, not just due to lethal damage or having 0 loyalty."
        )
        ruling(
            "2023-09-01",
            "You may sacrifice only one artifact, enchantment, or token to pay a spell's bargain cost."
        )
        ruling(
            "2023-09-01",
            "If you copy a bargained spell, the copy is also bargained."
        )
    }
}
