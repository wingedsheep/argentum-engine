package com.wingedsheep.engine.mechanics

import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardLayout

/**
 * Single source of truth for "may this card be cast as its **back** face from hand, and as which
 * face?" — used by the hand cast enumerator, the cast zone resolver, and the cast handler.
 *
 * A modal double-faced card (CR 712.3) lets its caster choose a face before putting it on the
 * stack (CR 712.11b), and only that face is evaluated for legality (CR 712.11c). Where the back
 * face is a **permanent** — the Marvel Super Heroes hero cycle, Jennifer Walters // The Sensational
 * She-Hulk and friends — that means a second cast option from hand, for the back face's own printed
 * mana cost, resolving onto the battlefield back face up (CR 712.13).
 *
 * Deliberately the same shape as [DisturbCasts], because the engine path is the same one: both put
 * a card on the stack **transformed**, so the spell's characteristics — card types (hence timing),
 * targets, P/T, abilities, name — all come from the back face rather than the printed front
 * (CR 712.8c / 712.8f). The two differ only in where the card is cast from (hand, not graveyard),
 * what supplies the cost (the back face's own mana cost, not a front-face keyword), and mana value:
 * CR 712.8f has no "mana value comes from the front face" exception, so unlike a disturbed spell a
 * modal back face keeps its own.
 *
 * Cards whose modal back is a *spell* (Flamescroll Celebrant // Revel in Silence) are not handled
 * here — they carry their back in `cardFaces` and are enumerated as a secondary spell face, since a
 * spell face never becomes a permanent.
 */
object ModalDfcCasts {

    /**
     * The back face a modal-DFC cast of [cardDef] puts on the stack, or null when [cardDef] is not
     * a modal DFC with a permanent back face.
     *
     * Returning the face rather than a boolean matches [DisturbCasts.castFace]: every caller needs
     * the face's characteristics immediately (timing from its card types, its target requirements,
     * the name to label the offer with), so the permission check and the face lookup are one
     * question.
     */
    fun castFace(cardDef: CardDefinition?): CardDefinition? {
        if (cardDef?.layout != CardLayout.MODAL_DFC) return null
        return cardDef.backFace?.takeIf { it.isPermanent }
    }
}
