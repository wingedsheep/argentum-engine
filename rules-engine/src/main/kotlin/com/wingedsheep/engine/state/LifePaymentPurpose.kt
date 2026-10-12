package com.wingedsheep.engine.state

/**
 * Why a player is paying life — the context a life-payment prohibition is scoped to
 * ([com.wingedsheep.sdk.scripting.PlayersCantPayLifeForCosts], Karn's Sylex: "Players can't pay
 * life to cast spells or to activate abilities that aren't mana abilities").
 *
 * Every [GameState.canPayLife] caller names one, so a new payment site has to decide which context
 * it is rather than inherit a permissive default.
 */
enum class LifePaymentPurpose {
    /** Part of the total cost of casting a spell: additional/alternative life, Phyrexian pips (CR 601.2h). */
    CAST_SPELL,

    /** Part of the cost of activating an ability that isn't a mana ability (CR 602.2b). */
    ACTIVATE_ABILITY,

    /** Part of the cost of activating a mana ability (CR 605) — Mana Confluence, Thran Portal's tax. */
    ACTIVATE_MANA_ABILITY,

    /**
     * Anything else: life paid while a spell or ability resolves (ward, "unless that player pays N
     * life"), or for a special action. No cost-scoped prohibition reaches it.
     */
    OTHER;

    companion object {
        fun activating(isManaAbility: Boolean): LifePaymentPurpose =
            if (isManaAbility) ACTIVATE_MANA_ABILITY else ACTIVATE_ABILITY
    }
}
