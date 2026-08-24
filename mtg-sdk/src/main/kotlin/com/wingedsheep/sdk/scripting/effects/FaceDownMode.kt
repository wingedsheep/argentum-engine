package com.wingedsheep.sdk.scripting.effects

import kotlinx.serialization.Serializable

/**
 * How a card that enters a zone face down can later be turned face up — i.e. which rule defines
 * the procedure for revealing its real identity.
 *
 * Used by [MoveToZoneEffect] and [MoveCollectionEffect] when moving a card to the battlefield
 * (or, for [HIDDEN], to exile). `null` on those effects means the card enters normally, face up.
 *
 * The engine stores the resulting "turn face up" data on the permanent, and a single generic
 * special action (CR 116.2b) lets the controller turn it face up regardless of which mechanic
 * created it. Adding a new face-down mechanic is one variant here plus its turn-up-cost rule in
 * `FaceDownTurnUp` — not a new effect or action.
 *
 * [faceDownWard] carries the one *characteristic* the mode itself contributes. Per CR 708.2 a
 * face-down permanent has no characteristics beyond those listed by the rules that made it face
 * down, and disguise (CR 702.168a) and cloak (CR 701.58a) list ward {2} among theirs. It is
 * therefore part of the characteristic-defining effect, not an ability of the card underneath —
 * which is why it lives on the mode rather than on the card's keyword abilities.
 *
 * [helperCardImageUri] is the *display* counterpart: the art of the helper card paper Magic
 * supplies for that mechanic, which every surface that draws a face-down object uses in place of
 * the hidden card's own art. The client mirrors these three URLs in `web-client/src/utils/
 * cardImages.ts` for the surfaces where it renders a face-down object without a server-sent image;
 * keep the two in sync.
 */
@Serializable
enum class FaceDownMode(
    val faceDownWard: WardCost? = null,
    val helperCardImageUri: String = MORPH_HELPER_CARD_IMAGE_URI,
) {
    /**
     * Morph (CR 702.37) / Megamorph: the permanent is turned face up by paying the card's morph
     * cost, taken from its [com.wingedsheep.sdk.scripting.KeywordAbility.Morph] ability.
     */
    MORPH,

    /**
     * Manifest (CR 701.40): the permanent is turned face up by paying the card's mana cost, but
     * only if the card representing it is a creature card (CR 701.40b). A manifested non-creature
     * card has no way to be turned face up that way — though per CR 701.40c/d a manifested card
     * that has morph or disguise may instead be turned face up by *that* mechanic's procedure.
     */
    MANIFEST(helperCardImageUri = MANIFEST_HELPER_CARD_IMAGE_URI),

    /**
     * Disguise (CR 702.168): morph with ward {2}. Cast face down for {3} at sorcery speed, turned
     * face up any time you have priority by paying the card's
     * [com.wingedsheep.sdk.scripting.KeywordAbility.Disguise] cost.
     */
    DISGUISE(WardCost.Mana("{2}"), MYSTERIOUS_CREATURE_HELPER_CARD_IMAGE_URI),

    /**
     * Cloak (CR 701.58): manifest with ward {2}. Turned face up by paying the card's mana cost,
     * only if it is a creature card (CR 701.58b); per CR 701.58c/d a cloaked card that has morph
     * or disguise may instead be turned face up by that mechanic's procedure.
     */
    CLOAK(WardCost.Mana("{2}"), MYSTERIOUS_CREATURE_HELPER_CARD_IMAGE_URI),

    /**
     * Face down with no turn-up procedure — e.g. a card exiled face down for Hideaway. It is
     * simply hidden; nothing lets it be turned face up in place.
     */
    HIDDEN
}

/**
 * The Morph token (TC19 #27) — the helmeted figure paper Magic supplies for a face-down morph, and
 * the fallback for any face-down object whose mechanic is unknown.
 */
const val MORPH_HELPER_CARD_IMAGE_URI =
    "https://cards.scryfall.io/normal/front/e/9/e9375cbe-93c0-41a5-a6e3-fb4416f54a69.jpg"

/** The Manifest token (TDSK #18), used for manifested permanents (CR 701.40). */
const val MANIFEST_HELPER_CARD_IMAGE_URI =
    "https://cards.scryfall.io/normal/front/0/1/01104ab1-84e1-4c78-853d-637c6554bdf9.jpg"

/**
 * "A Mysterious Creature" (TMKM #21) — the single helper card printed for *both* disguise
 * (CR 702.168) and cloak (CR 701.58); its own reminder text covers either mechanic, so paper Magic
 * uses this one card for both and so do we.
 */
const val MYSTERIOUS_CREATURE_HELPER_CARD_IMAGE_URI =
    "https://cards.scryfall.io/normal/front/2/4/241b3b6d-a25f-4a43-b5d6-1d1079e7e498.jpg"
