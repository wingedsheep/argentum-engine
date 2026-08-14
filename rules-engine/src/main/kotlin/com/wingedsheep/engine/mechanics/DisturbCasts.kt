package com.wingedsheep.engine.mechanics

import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Single source of truth for "can this card be disturb-cast, at what cost, and as which face?" —
 * used by the cast-from-graveyard enumerator, the cast zone resolver, and the cast handler.
 *
 * Disturb (CR 702.146a) is printed on the *front* face of a transforming double-faced card and
 * means "You may cast this card transformed from your graveyard by paying [cost] rather than its
 * mana cost." Two consequences shape every read site:
 *
 *  - The resulting spell is **back face up** (CR 712.8c), so its characteristics — card types
 *    (hence timing), targets, P/T, abilities — all come from [castFace], not from the printed front
 *    face. Only the cost comes from the front face's disturb keyword.
 *  - The back face must be a permanent for the cast to be legal at all; a card whose back face is
 *    absent or non-permanent has no disturb offer (CR 712.10's "nothing happens" applied at the
 *    source — such a card is never printed, but the guard keeps a malformed definition from
 *    producing an uncastable spell on the stack).
 *
 * Unlike [FlashbackGrants] / [HarmonizeGrants] there is no runtime-grant source: no card grants
 * disturb to another, so a printed keyword is the only input.
 */
object DisturbCasts {

    /** The printed disturb keyword on [cardDef], or null when it has none. */
    fun printedDisturb(cardDef: CardDefinition?): KeywordAbility.Disturb? =
        cardDef?.keywordAbilities?.filterIsInstance<KeywordAbility.Disturb>()?.firstOrNull()

    /**
     * The face a disturb cast of [cardDef] puts on the stack — its back face — or null when
     * [cardDef] has no disturb keyword or no permanent back face to transform into.
     */
    fun castFace(cardDef: CardDefinition?): CardDefinition? {
        if (printedDisturb(cardDef) == null) return null
        return cardDef?.backFace?.takeIf { it.isPermanent }
    }
}
