package com.wingedsheep.engine.mechanics.targeting

import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate

/**
 * The single answer to "may this stack-zone target requirement offer *abilities*, not just spells?".
 *
 * A spell is a card on the stack (CR 112.1); an activated or triggered ability on the stack is a
 * different kind of object (CR 113.3b/c, 113.7a). Nearly every stack-targeting filter is the plain
 * "target spell" shape (base filter `Any`, zone `STACK`), and offering ability entities for those
 * would make a counterspell castable at anything on the stack. So an ability is a candidate only
 * when the filter *explicitly* names an ability — "counter target ability" (Stifle), "spell or
 * ability" (Willbender, Return the Favor), "copy target activated or triggered ability you control"
 * (Gogo, Peter Parker's Camera, Echo, Scientist Supreme of A.I.M.).
 *
 * Two readers ask the question and must never disagree: [com.wingedsheep.engine.handlers.TargetFinder]
 * (the authoritative legal-target set, used when a target is actually chosen) and
 * [com.wingedsheep.engine.legalactions.utils.TargetEnumerationUtils] (the legal-action enumeration
 * the server sends to clients and the AI). When the two drift, an ability-targeting card is
 * *executable but not offered*: the engine accepts the action if a client somehow submits it, while
 * the enumerator never surfaces the ability as a legal target, so the ability is unplayable through
 * the UI and invisible to the AI. Keeping the predicate here is what stops that from recurring.
 */
object StackObjectTargeting {

    /**
     * True iff [filter] explicitly permits abilities on the stack — i.e. some [CardPredicate] in it,
     * including inside [CardPredicate.Or] / `And` / `Not` branches and [GameObjectFilter.anyOf]
     * sub-filters, names an ability. [CardPredicate.AbilitySourceMatches] counts: it reads an
     * ability's source and can only ever be true for an ability.
     */
    fun permitsAbilities(filter: GameObjectFilter): Boolean {
        fun predicateNamesAbility(p: CardPredicate): Boolean = when (p) {
            CardPredicate.IsActivatedOrTriggeredAbility,
            CardPredicate.IsTriggeredAbility,
            CardPredicate.IsActivatedAbility,
            is CardPredicate.AbilitySourceMatches -> true
            is CardPredicate.Or -> p.predicates.any(::predicateNamesAbility)
            is CardPredicate.And -> p.predicates.any(::predicateNamesAbility)
            // Negation is deliberately *not* inverted. The question this answers is "does the
            // requirement's text mention abilities at all", not "must the object be an ability" —
            // `Not(IsActivatedAbility)` still speaks of abilities, so the enumeration should offer
            // ability entities and let the filter itself reject the ones that don't match. Inverting
            // here would silently drop them before the filter ever ran.
            is CardPredicate.Not -> predicateNamesAbility(p.predicate)
            else -> false
        }
        return filter.cardPredicates.any(::predicateNamesAbility) ||
            filter.anyOf.any { permitsAbilities(it) }
    }
}
