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
 */
@Serializable
enum class FaceDownMode(val faceDownWard: WardCost? = null) {
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
    MANIFEST,

    /**
     * Disguise (CR 702.168): morph with ward {2}. Cast face down for {3} at sorcery speed, turned
     * face up any time you have priority by paying the card's
     * [com.wingedsheep.sdk.scripting.KeywordAbility.Disguise] cost.
     */
    DISGUISE(WardCost.Mana("{2}")),

    /**
     * Cloak (CR 701.58): manifest with ward {2}. Turned face up by paying the card's mana cost,
     * only if it is a creature card (CR 701.58b); per CR 701.58c/d a cloaked card that has morph
     * or disguise may instead be turned face up by that mechanic's procedure.
     */
    CLOAK(WardCost.Mana("{2}")),

    /**
     * Face down with no turn-up procedure — e.g. a card exiled face down for Hideaway. It is
     * simply hidden; nothing lets it be turned face up in place.
     */
    HIDDEN
}
