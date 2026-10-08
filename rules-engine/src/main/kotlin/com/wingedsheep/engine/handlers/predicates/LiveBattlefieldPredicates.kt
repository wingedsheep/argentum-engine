package com.wingedsheep.engine.handlers.predicates

import com.wingedsheep.sdk.scripting.predicates.CardPredicate

/**
 * Predicates only the live evaluator can answer, because they compare the candidate with the
 * controller's battlefield — "doesn't have the same name as a token you control" (The Apprentice's
 * Folly, Yenna). The matchers without a battlefield in scope (layer projection, cast permissions,
 * cast records) report `false` for them, and that is only fail-closed for the positive form: under
 * `Not` it would flip to "matches everything". Those matchers make a negated one fail closed too.
 */
internal object LiveBattlefieldPredicates {
    fun answerableOnlyLive(predicate: CardPredicate): Boolean =
        predicate is CardPredicate.SharesNameWithPermanentYouControl
}
