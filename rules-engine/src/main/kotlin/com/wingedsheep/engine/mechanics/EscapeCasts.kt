package com.wingedsheep.engine.mechanics

import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Single source of truth for "can this card escape, and at what cost?" — used by the
 * cast-from-graveyard enumerator, the cast zone resolver, the cost totaller and the additional-cost
 * collector, so every read site agrees.
 *
 * Escape (CR 702.138a) is a static ability that functions while the card is in its owner's
 * graveyard: "You may cast this card from your graveyard by paying [cost] rather than paying its
 * mana cost." Printed-only for now; a future grant source ("each nonland card in your graveyard
 * has escape", Underworld Breach) plugs in here, the way [FlashbackGrants] layers its grants.
 */
object EscapeCasts {

    /** The printed escape keyword on [cardDef], or null when it has none. */
    fun printedEscape(cardDef: CardDefinition?): KeywordAbility.Escape? =
        cardDef?.keywordAbilities?.filterIsInstance<KeywordAbility.Escape>()?.firstOrNull()
}
