package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.bargain
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.TargetCreatureOrPlaneswalker
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Stonesplitter Bolt
 * {X}{R}
 * Instant
 *
 * Bargain (You may sacrifice an artifact, enchantment, or token as you cast this spell.)
 * Stonesplitter Bolt deals X damage to target creature or planeswalker. If this spell was bargained,
 * it deals twice X damage to that permanent instead.
 *
 * The spell-rider shape of bargain (CR 702.166c), read off the spell while it's still on the stack —
 * so the payoff gates on [Conditions.WasBargained], as in [CandyGrapple].
 *
 * Unlike Candy Grapple, though, the "instead" is **not** composed as a base hit plus a second top-up
 * hit. Damage is not a layer-7c modification that silently merges: two instances of X damage and one
 * instance of 2X are distinguishable in the rules — damage-prevention shields absorb per instance,
 * damage-doubling and redirection replacement effects apply per instance, and "whenever a source deals
 * damage" triggers would fire twice. So the conditional lives in the *amount* instead, via
 * [DynamicAmount.Conditional], leaving exactly one [Effects.DealDamage] on either branch.
 * `Multiply(XValue, 2)` is "twice X" rather than a hardcoded doubling of the final number, so it
 * stays correct when X is 0.
 *
 * One target requirement, chosen at announcement regardless of branch — the printed text says
 * "that permanent", not a second target.
 */
val StonesplitterBolt = card("Stonesplitter Bolt") {
    manaCost = "{X}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Bargain (You may sacrifice an artifact, enchantment, or token as you cast this " +
        "spell.)\n" +
        "Stonesplitter Bolt deals X damage to target creature or planeswalker. If this spell was " +
        "bargained, it deals twice X damage to that permanent instead."

    bargain()

    spell {
        val victim = target("target creature or planeswalker", TargetCreatureOrPlaneswalker())
        effect = Effects.DealDamage(
            amount = DynamicAmount.Conditional(
                condition = Conditions.WasBargained,
                ifTrue = DynamicAmount.Multiply(DynamicAmount.XValue, 2),
                ifFalse = DynamicAmount.XValue,
            ),
            target = victim,
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "151"
        artist = "Alexandr Leskinen"
        imageUri = "https://cards.scryfall.io/normal/front/f/b/fb22f79c-3075-439d-a072-ceaabe35d76f.jpg?1783915088"

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
