package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardFace
import com.wingedsheep.sdk.model.CardLayout
import com.wingedsheep.sdk.model.EntityId

/**
 * Single source of truth for aftermath (CR 702.127) — used by the graveyard cast enumerator, the
 * hand split-face enumerator, the cast-legality chokepoint (`CastPermissionUtils.reasonCannotCast`,
 * which both the cast validator and the final legal-action filter consult), and the cast records
 * that arm the leave-the-stack exile.
 *
 * CR 702.127a makes aftermath three static abilities of one half of a split card:
 *  1. "You may cast this half of this split card from your graveyard" — [graveyardCastFace];
 *  2. "This half of this split card can't be cast from any zone other than a graveyard" —
 *     [zoneFailure];
 *  3. "If this spell was cast from a graveyard, exile it instead of putting it anywhere else any
 *     time it would leave the stack" — [exilesOnLeavingStack].
 *
 * The marker is [Keyword.AFTERMATH] on the half's [CardFace], so the other half of the card is an
 * ordinary split half: castable from hand, and *not* from the graveyard through aftermath.
 */
object AftermathCasts {

    /** Whether [faceIndex] names an aftermath half of [cardDef]. */
    fun isAftermathFace(cardDef: CardDefinition?, faceIndex: Int?): Boolean {
        if (faceIndex == null || cardDef?.layout != CardLayout.SPLIT) return false
        return cardDef.cardFaces.getOrNull(faceIndex)?.let(::isAftermath) == true
    }

    fun isAftermath(face: CardFace): Boolean = Keyword.AFTERMATH in face.keywords

    /**
     * The aftermath half [action] would cast from [playerId]'s own graveyard, or null when the
     * action doesn't name one or the card isn't there. "Your graveyard" (CR 702.127a): only the
     * owner may cast it this way, and only from their own graveyard.
     */
    fun graveyardCastFace(state: GameState, playerId: EntityId, cardDef: CardDefinition?, action: CastSpell): CardFace? {
        if (!isAftermathFace(cardDef, action.faceIndex)) return null
        if (action.cardId !in state.getGraveyard(playerId)) return null
        return cardDef?.cardFaces?.getOrNull(action.faceIndex ?: return null)
    }

    /**
     * Why [action] may not cast an aftermath half from where the card is, or null when it may (or
     * the action casts no aftermath half). An aftermath half can't be cast from any zone other
     * than a graveyard — hand, exile, library and command zone casts of it are all illegal, whatever
     * permission offered them.
     */
    fun zoneFailure(state: GameState, cardDef: CardDefinition?, action: CastSpell): String? {
        if (!isAftermathFace(cardDef, action.faceIndex)) return null
        val owner = state.getEntity(action.cardId)?.get<CardComponent>()?.ownerId ?: return null
        if (action.cardId in state.getGraveyard(owner)) return null
        val faceName = cardDef?.cardFaces?.getOrNull(action.faceIndex ?: 0)?.name ?: "This half"
        return "$faceName has aftermath and can only be cast from a graveyard"
    }

    /** Whether a spell cast as [faceIndex] of [cardDef] from [castFromGraveyard] is exiled whenever it leaves the stack. */
    fun exilesOnLeavingStack(cardDef: CardDefinition?, faceIndex: Int?, castFromGraveyard: Boolean): Boolean =
        castFromGraveyard && isAftermathFace(cardDef, faceIndex)
}
