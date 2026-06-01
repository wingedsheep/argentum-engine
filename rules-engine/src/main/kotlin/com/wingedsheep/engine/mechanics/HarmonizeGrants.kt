package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Single source of truth for "does this card have harmonize, and at what cost?" — used by every
 * harmonize read site (the cast-from-graveyard enumerator, the cast handler, the
 * alternative-payment handler, and the stack resolver's exile-on-resolution clause).
 *
 * Harmonize (CR 702.180) can be either printed on the card ([KeywordAbility.Harmonize] in the
 * card's keyword abilities) or granted at runtime to a specific card entity (Songcrafter Mage:
 * "target instant or sorcery card in your graveyard gains harmonize until end of turn"). Routing
 * all call sites through here keeps the two sources consistent so a granted harmonize behaves
 * identically to a printed one (cost, tap-for-power reduction, exile on resolution).
 */
object HarmonizeGrants {

    /**
     * The effective harmonize ability for [cardId], or null if it has none. A printed harmonize
     * on [cardDef] wins; otherwise the most recently granted runtime harmonize for this entity is
     * returned (a later grant overrides an earlier one for the same card).
     */
    fun effectiveHarmonize(
        state: GameState,
        cardId: EntityId,
        cardDef: CardDefinition?
    ): KeywordAbility.Harmonize? {
        cardDef?.keywordAbilities
            ?.firstOrNull { it is KeywordAbility.Harmonize }
            ?.let { return it as KeywordAbility.Harmonize }

        return state.grantedKeywordAbilities
            .lastOrNull { it.entityId == cardId && it.ability is KeywordAbility.Harmonize }
            ?.let { it.ability as KeywordAbility.Harmonize }
    }
}
