package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.bargain
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Candy Grapple
 * {1}{B}
 * Instant
 *
 * Bargain (You may sacrifice an artifact, enchantment, or token as you cast this spell.)
 * Target creature gets -3/-3 until end of turn. If this spell was bargained, that creature gets
 * -5/-5 until end of turn instead.
 *
 * The spell-rider shape of bargain (CR 702.166c): the fact is read off the spell while it is still
 * on the stack, so the payoff is a [ConditionalEffect] gated on [Conditions.WasBargained] rather
 * than anything riding a resolved permanent.
 *
 * The "-5/-5 instead" is modelled as the base -3/-3 plus a further -2/-2 on the bargained branch.
 * Both are layer 7c modifications applied to the same creature with the same end-of-turn duration,
 * so the two-effect composition and a single -5/-5 are indistinguishable in projected state — and
 * this keeps one target requirement, so the creature is chosen once at announcement regardless of
 * which branch is taken.
 */
val CandyGrapple = card("Candy Grapple") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Bargain (You may sacrifice an artifact, enchantment, or token as you cast this " +
        "spell.)\n" +
        "Target creature gets -3/-3 until end of turn. If this spell was bargained, that creature " +
        "gets -5/-5 until end of turn instead."

    bargain()

    spell {
        val creature = target("target creature", TargetCreature())
        effect = Effects.Composite(
            Effects.ModifyStats(power = -3, toughness = -3, target = creature),
            ConditionalEffect(
                condition = Conditions.WasBargained,
                effect = Effects.ModifyStats(power = -2, toughness = -2, target = creature),
            ),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "83"
        artist = "Konstantin Porubov"
        flavorText = "\"Don't you mean 'poisonous'? There's no such thing as a venomous—AGGHHH!\""
        imageUri = "https://cards.scryfall.io/normal/front/1/9/190d97bc-dbef-496d-9bd1-b785bdf8a964.jpg?1783915109"
    }
}
