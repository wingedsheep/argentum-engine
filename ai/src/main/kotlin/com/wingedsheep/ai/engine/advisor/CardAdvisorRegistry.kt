package com.wingedsheep.ai.engine.advisor

/**
 * Registry that maps card names to their [CardAdvisor] instances.
 *
 * Advisors are registered via [CardAdvisorModule]s, typically one per set.
 * Lookup is O(1) by card name.
 */
class CardAdvisorRegistry {
    private val advisors = mutableMapOf<String, CardAdvisor>()

    fun register(advisor: CardAdvisor) {
        for (name in advisor.cardNames) {
            advisors[name] = advisor
        }
    }

    fun getAdvisor(cardName: String): CardAdvisor? = advisors[cardName]
}
