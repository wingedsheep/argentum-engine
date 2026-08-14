package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.string.shouldStartWith

/**
 * `ReplaceDrawWithEffect.description` appends its `restrictions` clause unconditionally:
 *
 * ```kotlin
 * append("If ${appliesTo.description} while $restrictionDesc, ")
 * ```
 *
 * so an effect with no restrictions renders a dangling "while ," — every sibling in
 * `ReplacementEffect.kt` guards the empty case instead. This is player-facing text, not just
 * a debug string: it flows through `GatheredReplacement.description` into
 * `ChooseOptionDecision.options` and into the optional-replacement prompt
 * ("Use $cardName? ${gathered.description}"). Parallel Thoughts, Underrealm Lich and every
 * Words-cycle shield carry no restrictions, so they all render the broken form.
 *
 * The restricted form reads correctly today and matches Phial of Galadriel's oracle text
 * ("If you would draw a card while you have no cards in hand, draw two cards instead"), so
 * the fix is to make the ` while <clause>` segment conditional — not to restructure it into
 * `ModifyDrawAmount`'s leading-restriction shape. Both cases are pinned below so either
 * mistake fails.
 */
class ReplaceDrawWithEffectDescriptionTest : DescribeSpec({

    describe("ReplaceDrawWithEffect.description") {

        it("reads naturally when there are no restrictions") {
            val effect = ReplaceDrawWithEffect(replacementEffect = DrawCardsEffect(2))

            effect.description shouldNotContain " while ,"
            effect.description shouldNotContain " while "
            effect.description shouldStartWith "If you would draw a card,"
        }

        it("reads naturally when the replacement is optional and unrestricted") {
            val effect = ReplaceDrawWithEffect(
                replacementEffect = DrawCardsEffect(1),
                optional = true
            )

            effect.description shouldNotContain " while ,"
            effect.description shouldContain "you may "
            effect.description shouldStartWith "If you would draw a card,"
        }

        it("keeps the 'while <condition>' clause when restrictions are present") {
            val effect = ReplaceDrawWithEffect(
                replacementEffect = DrawCardsEffect(2),
                restrictions = listOf(Conditions.EmptyHand)
            )

            effect.description shouldNotContain " while ,"
            effect.description shouldContain " while "
            effect.description shouldStartWith "If you would draw a card while "
        }
    }
})
