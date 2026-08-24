package com.wingedsheep.engine.state.permissions

import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.conditions.Condition
import kotlinx.serialization.Serializable

/**
 * A "you may play this card" permission held in [com.wingedsheep.engine.state.GameState].
 *
 * Permissions live as a list on the game state — like
 * [com.wingedsheep.engine.mechanics.layers.ActiveFloatingEffect] for continuous P/T effects —
 * and read sites query the list rather than inspect the card.
 *
 * Why a list instead of a stamp:
 * - The same permission may apply to multiple cards (Mind's Desire storm, Cruelclaw exile pile).
 * - Conditions (Possibility Technician's "if you control a Kavu") must re-evaluate at every read,
 *   not just when the permission is granted.
 * - The granting permanent may leave play before the permission expires; the permission's
 *   lifecycle is owned by the game state, not the card.
 *
 * @param id Unique id for this permission, used for targeted removal (consume on cast, etc.).
 * @param cardIds Which cards this permission applies to. Read sites do their own zone check —
 *   a permission targeting an exiled card is irrelevant once the card moves to the graveyard.
 * @param controllerId Who may play the cards.
 * @param sourceId Granting permanent / spell, for trigger-context reconstruction. Required when
 *   [condition] is set so source-keyed conditions (`SourceHas*`, `SourceIs*`) can resolve
 *   correctly; optional otherwise.
 * @param condition Optional gate re-evaluated on every query. When present, the permission is
 *   only honored while the condition holds.
 * @param withAnyManaType If true, mana of any type can be spent to cast (Taster of Wares).
 * @param landEntersTapped If true, a land card played via this permission enters the battlefield
 *   tapped. Used by Lightstall Inquisitor-style exile-from-hand effects whose "lands played
 *   this way enter tapped" clause must be enforced on top of the played card's intrinsic ETB
 *   state, independent of the card's own script.
 * @param permanent If true, the permission is not auto-removed at end of turn cleanup. Permanent
 *   permissions are removed explicitly (e.g., when their card resolves). Non-permanent grants
 *   expire via [expiresAfterTurn].
 * @param expiresAfterTurn Turn number after whose cleanup this permission is removed. `null`
 *   plus `permanent=false` means "remove at the next end-of-turn cleanup."
 * @param timestamp Monotonic timestamp from [com.wingedsheep.engine.state.GameState.timestamp]
 *   for ordering equal permissions.
 */
@Serializable
data class MayPlayPermission(
    val id: EntityId,
    val cardIds: Set<EntityId>,
    val controllerId: EntityId,
    val sourceId: EntityId? = null,
    val condition: Condition? = null,
    val withAnyManaType: Boolean = false,
    /**
     * If true, cards played via this permission may be cast at instant speed — "as though they
     * had flash" (CR 702.8) — even if they are sorceries, creatures, or other non-instant cards.
     * Read by the from-exile cast enumerator and the cast handler's timing check; combined with a
     * turn-scoped [condition] this expresses "During your turn, you may cast … as though they had
     * flash" (Azula, Cunning Usurper). Does not waive any cost.
     */
    val asThoughFlash: Boolean = false,
    val landEntersTapped: Boolean = false,
    val permanent: Boolean = false,
    val expiresAfterTurn: Int? = null,
    /**
     * When set, playing a card granted by this permission emits a
     * [com.wingedsheep.engine.core.CardPlayedFromPermissionEvent] carrying this id, which
     * fires a linked event-based delayed triggered ability — the "When you play a card this
     * way, …" rider (Fires of Mount Doom). The rider's [DelayedTriggeredAbility] shares this
     * id. Null when the grant has no rider.
     */
    val riderLinkId: String? = null,
    /**
     * Whose turn ends a turn-keyed window ([expiresAfterTurn]), when that differs from
     * [controllerId]. Defaults to null → the window keys off [controllerId] (the normal
     * single-player impulse case, where the player who may play the cards is the same player
     * whose "next turn" closes the window).
     *
     * Memory Vessel grants each player a permission to play *their own* exiled cards
     * ([controllerId] = the card's owner) but the window lasts "until **your** next turn" — the
     * activating player's next turn, the same for everyone. Setting this to the activating player
     * makes the cleanup expiry key off that player's turn instead of each owner's.
     */
    val expiryControllerId: EntityId? = null,
    /**
     * When true, this permission is revoked as soon as its [sourceId] grants another
     * `supersededBySameSource` permission — i.e. the source exiles another card. Models
     * "you may play that card until you exile another card with this [permanent]" (Superior
     * Foes of Spider-Man): only the source's most-recently-exiled card stays playable, and
     * the superseded card remains in exile but can no longer be played. Set together with
     * [permanent] = true (the window otherwise persists across turns) by
     * [com.wingedsheep.engine.handlers.effects.library.GrantMayPlayFromExileExecutor] when the
     * grant carries `MayPlayExpiry.UntilSourceExilesAnother`.
     */
    val supersededBySameSource: Boolean = false,
    /**
     * When true, this permission lasts only for as long as its "you" still controls [sourceId] —
     * "you may cast it for as long as you control this creature" (Taster of Wares). "You" is
     * [expiryControllerId] when set and [controllerId] otherwise, the same resolution the turn-keyed
     * window uses. For this flag the granting executor always pins [expiryControllerId] to the
     * player whose ability granted the permission, so the window measures against them even under
     * an `ownerControls` grouping, where [controllerId] is each card's owner — an owner does not
     * control the source, so keying the window off them would revoke the grant immediately.
     *
     * Enforced by revocation, not by a gate: [com.wingedsheep.engine.mechanics.sba.permanent
     * .EndedDurationExpiryCheck] deletes the permission on the first state-based check after the
     * source leaves the battlefield or its projected controller changes. Deleting rather than
     * gating is what makes the window one-way, as CR 611.2b requires — a gate would silently
     * reopen if the source came back or control reverted.
     *
     * Set together with [permanent] = true (the window is not turn-keyed, so cleanup must not
     * take it first) by [com.wingedsheep.engine.handlers.effects.library
     * .GrantMayPlayFromExileExecutor] when the grant carries
     * [com.wingedsheep.sdk.scripting.effects.MayPlayExpiry.WhileYouControlSource]. Meaningless
     * without a [sourceId]; the executor refuses to build such a permission.
     */
    val endsWhenSourceUncontrolled: Boolean = false,
    /**
     * When true, this permission lasts only for as long as [sourceId] is **on the battlefield** —
     * "for as long as this permanent remains on the battlefield", regardless of who controls it and
     * regardless of who holds the permission.
     *
     * The controller-blind sibling of [endsWhenSourceUncontrolled], and the difference is
     * load-bearing rather than cosmetic: Shared Fate grants play permission to *every* player,
     * including opponents who never control the enchantment, so a window keyed to "you control the
     * source" would revoke each opponent's grant on the first state-based check. Because the window
     * reads only the source's zone, a control change leaves the permission alone — right for a
     * grant that models a static ability, which keeps functioning under a new controller.
     *
     * Enforced the same way and for the same reason: [com.wingedsheep.engine.mechanics.sba.permanent
     * .EndedDurationExpiryCheck] deletes rather than gates, so the latch is one-way (CR 611.2b) and
     * the source returning cannot revive it. Set together with [permanent] = true by
     * [com.wingedsheep.engine.handlers.effects.library.GrantMayPlayFromExileExecutor] when the grant
     * carries [com.wingedsheep.sdk.scripting.effects.MayPlayExpiry.WhileSourceOnBattlefield].
     * Meaningless without a [sourceId]; the executor refuses to build such a permission.
     */
    val endsWhenSourceLeavesBattlefield: Boolean = false,
    /**
     * When true, this permission authorizes casting spells only — a land among [cardIds] can
     * never be played through it. Mirrors
     * [com.wingedsheep.sdk.scripting.effects.GrantMayPlayFromExileEffect.nonLandOnly]: "cast"
     * wording (Ragavan, Nimble Pilferer) never covers a land's play-as-a-special-action (CR
     * 305.1), unlike "play" wording (Light Up the Stage). Read by the from-exile enumerator
     * before it offers a `PlayLand` action for an exiled land.
     */
    val nonLandOnly: Boolean = false,
    /**
     * When set, this permission authorizes casting the card's **alternative face** at this index
     * (`CardDefinition.cardFaces[castFaceIndex]`) instead of its primary characteristics. The
     * enumerator reads the face's cost, type line, timing, cast restrictions, and target
     * requirements, and threads the index onto the emitted `CastSpell`; the cast handler rejects
     * any other face for a permission-based cast.
     *
     * Mirrors [com.wingedsheep.sdk.scripting.effects.GrantMayPlayFromExileEffect.castFaceIndex] —
     * "you may cast it from your graveyard as an Adventure" (Mosswood Dreadknight, CR 715.3), where
     * only the Adventure face is castable and the creature face stays locked in the graveyard.
     */
    val castFaceIndex: Int? = null,
    /**
     * When set, this permission authorizes casting only spells of this **color** — the color is
     * checked against the characteristics of the face actually being cast, not the card sitting in
     * exile. Mirrors
     * [com.wingedsheep.sdk.scripting.effects.GrantMayPlayFromExileEffect.castColorRestriction]:
     * "you may cast red spells from among them" (Chandra, Dressed to Kill's −7), whose ruling is
     * explicit that a modal double-faced card's blue back face may **not** be cast this way even
     * though the exiled card itself is red. Read by the from-exile enumerator and independently
     * enforced by the cast handler.
     */
    val castColorRestriction: com.wingedsheep.sdk.core.Color? = null,
    /**
     * When true, a cast authorized by this permission puts the card on the stack **transformed** —
     * back face up (CR 712.8c), so the back face supplies the spell's characteristics, targets, and
     * the permanent it becomes. The front face still supplies nothing but the card's identity.
     *
     * Mirrors [com.wingedsheep.sdk.scripting.effects.CastFromCollectionWithoutPayingCostEffect
     * .castTransformed]: "exile it, then you may cast it transformed without paying its mana cost"
     * (CR 310.12b, the Siege defeat trigger). Distinct from [castFaceIndex], which selects an
     * alternative *face* of a multi-face card (an Adventure, a split half) — a transforming
     * double-faced card's back face is `CardDefinition.backFace`, not a `cardFaces` entry, and it
     * goes on the stack as the same card turned over rather than as a separate half.
     *
     * Ignored for a card with no back face, so it is safe on a permission covering a mixed pile.
     */
    val castTransformed: Boolean = false,
    val timestamp: Long
) {
    init {
        // gateOpen falls back sourceId ?: cardId when building the EffectContext, which would
        // make SourceHas* / SourceIs* conditions silently misfire (they'd read the exiled card
        // as the source). Require a real sourceId whenever a condition is attached.
        require(condition == null || sourceId != null) {
            "MayPlayPermission with a condition must specify sourceId (condition: ${condition!!.description})"
        }
        // The "for as long as you control it" window is evaluated entirely from sourceId; without
        // one it could never close, which is the opposite of what the duration says.
        require(!endsWhenSourceUncontrolled || sourceId != null) {
            "MayPlayPermission with endsWhenSourceUncontrolled must specify sourceId"
        }
        // Same reasoning as above: the "while the source is on the battlefield" window is
        // evaluated entirely from sourceId, so without one it could never close.
        require(!endsWhenSourceLeavesBattlefield || sourceId != null) {
            "MayPlayPermission with endsWhenSourceLeavesBattlefield must specify sourceId"
        }
    }
}
