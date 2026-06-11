package com.wingedsheep.mtg.sets

import com.wingedsheep.sdk.serialization.CardValidationError
import com.wingedsheep.sdk.serialization.CardValidator
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty

/**
 * Corpus-wide gate for the [CardValidationError.UninferableSuccessCriterion] check
 * (sdk-analysis §1.3 "close the fail-open defaults"): every `Gate.DoAction` whose
 * criterion is `SuccessCriterion.Auto` must wrap an action shape Auto can infer
 * success from. Unrecognized shapes used to silently count as "it happened" —
 * now the card must state an explicit criterion (`Always` / `CollectionNonEmpty`),
 * and this test is what makes that a build-time failure for the whole corpus.
 */
class SuccessCriterionValidationTest : FunSpec({

    test("no registered card relies on SuccessCriterion.Auto's former fail-open default") {
        val errors = MtgSetCatalog.all.flatMap { set ->
            set.cards.flatMap { card ->
                CardValidator.validate(card)
                    .filterIsInstance<CardValidationError.UninferableSuccessCriterion>()
                    .map { "[${set.code}] ${it.message}" }
            }
        }
        errors.shouldBeEmpty()
    }
})
