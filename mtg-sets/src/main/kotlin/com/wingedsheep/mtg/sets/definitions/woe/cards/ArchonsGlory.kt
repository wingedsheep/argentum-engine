package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.bargain
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Archon's Glory
 * {W}
 * Instant
 *
 * Bargain (You may sacrifice an artifact, enchantment, or token as you cast this spell.)
 * Target creature gets +2/+2 until end of turn. If this spell was bargained, that creature also
 * gains flying and lifelink until end of turn.
 *
 * The spell-rider shape of bargain (CR 702.166c), the same one [CandyGrapple] uses: the bargained
 * fact is stamped on the spell as it's cast, so the payoff is a [ConditionalEffect] gated on
 * [Conditions.WasBargained] and read while the spell is still resolving.
 *
 * "Also" rather than "instead" here, so the two halves simply stack: the +2/+2 always applies and
 * the bargained branch adds two layer 6 keyword grants on top of it. All three share the one
 * target requirement, so the creature is chosen once at announcement — and if it's an illegal
 * target by resolution the whole spell fizzles, bargained or not.
 */
val ArchonsGlory = card("Archon's Glory") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Bargain (You may sacrifice an artifact, enchantment, or token as you cast this " +
        "spell.)\n" +
        "Target creature gets +2/+2 until end of turn. If this spell was bargained, that creature " +
        "also gains flying and lifelink until end of turn."

    bargain()

    spell {
        val creature = target("target creature", TargetCreature())
        effect = Effects.Composite(
            Effects.ModifyStats(power = 2, toughness = 2, target = creature),
            ConditionalEffect(
                condition = Conditions.WasBargained,
                effect = Effects.Composite(
                    Effects.GrantKeyword(Keyword.FLYING, creature),
                    Effects.GrantKeyword(Keyword.LIFELINK, creature),
                ),
            ),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "2"
        artist = "Anastasia Ovchinnikova"
        flavorText = "A winged dawn dispels the terrors of night."
        imageUri = "https://cards.scryfall.io/normal/front/e/7/e71768e7-4ef7-4fb2-838b-eb3a7f662d38.jpg?1783915136"

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
