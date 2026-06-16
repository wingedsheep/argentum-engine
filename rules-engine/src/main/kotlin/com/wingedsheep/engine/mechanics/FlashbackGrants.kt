package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Single source of truth for "does this card have flashback, and at what cost?" — used by every
 * flashback read site (the cast-from-graveyard enumerator, the cast handler / zone resolver, and
 * the stack resolver's exile-on-resolution clause).
 *
 * Flashback (CR 702.34) can be either printed on the card ([KeywordAbility.Flashback] in the
 * card's keyword abilities) or granted at runtime to a specific card entity (Archmage's Newt:
 * "target instant or sorcery card in your graveyard gains flashback until end of turn"). Routing
 * all call sites through here keeps the two sources consistent so a granted flashback behaves
 * identically to a printed one (cost, exile on resolution).
 *
 * Mirrors [HarmonizeGrants] for the harmonize keyword.
 */
object FlashbackGrants {

    /**
     * The effective flashback ability for [cardId], or null if it has none. A printed flashback
     * on [cardDef] wins; otherwise the most recently granted runtime flashback for this entity is
     * returned (a later grant overrides an earlier one for the same card).
     */
    fun effectiveFlashback(
        state: GameState,
        cardId: EntityId,
        cardDef: CardDefinition?
    ): KeywordAbility.Flashback? {
        cardDef?.keywordAbilities
            ?.firstOrNull { it is KeywordAbility.Flashback }
            ?.let { return it as KeywordAbility.Flashback }

        return state.grantedKeywordAbilities
            .lastOrNull { it.entityId == cardId && it.ability is KeywordAbility.Flashback }
            ?.let { it.ability as KeywordAbility.Flashback }
    }
}
