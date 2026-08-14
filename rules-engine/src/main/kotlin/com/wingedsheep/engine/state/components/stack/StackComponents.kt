package com.wingedsheep.engine.state.components.stack

import com.wingedsheep.engine.state.Component
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.targets.TargetRequirement
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.wingedsheep.sdk.dsl.sneak

/**
 * Marks an entity as a spell on the stack.
 */
@Serializable
data class SpellOnStackComponent(
    val casterId: EntityId,
    val xValue: Int? = null,  // For X spells
    /**
     * Which optional-additional-cost mechanic this spell was cast with (kicker/multikicker/
     * offspring → [ChoiceSlot.KICKED], bargain → [ChoiceSlot.BARGAINED]), or null when none was
     * declared. Carried onto the resolving permanent's cast-choices bag by `StackResolver`.
     */
    val declaredCostSlot: ChoiceSlot? = null,
    val wasBlightPaid: Boolean = false,  // For BlightOrPay additional cost — true if blight path was taken
    val wasWaterbendPaid: Boolean = false,  // For optional spell waterbend additional cost (Avatar) — true if "you may waterbend {N}" was paid; readable via WaterbendWasPaid
    /**
     * The opponent promised this spell's gift additional cost (CR 702.174a), or null when the gift
     * wasn't promised. A resolving permanent carries the fact onward in its cast-choices bag
     * (ChoiceSlot.GIFT_PROMISED + ChoiceSlot.OPPONENT) so its gift trigger and
     * "if the gift was(n't) promised" riders can read it — see StackResolver.
     */
    val giftRecipient: EntityId? = null,
    /**
     * Card-definition names of the cards revealed from hand and **spliced** onto this spell
     * (CR 702.47a), in the order their text was added. Each one's `spellEffect` is executed after the
     * main spell's own effects when the spell resolves (CR 702.47b), with its own target slice from
     * [splicedTargetsOrdered].
     *
     * Names rather than entity ids: the spliced card never leaves hand, so what the spell gained is
     * its *text*, not a reference to the object — and the spell must keep resolving that text even if
     * the card is later discarded or exiled. The spliced text lives here, on the stack object, which
     * is exactly why "the spell loses any splice changes once it leaves the stack" (CR 702.47e) needs
     * no cleanup code.
     */
    val splicedCardNames: List<String> = emptyList(),
    /**
     * Targets chosen for each spliced card's own text (CR 702.47d), aligned 1:1 with
     * [splicedCardNames]. Sliced out of the flat cast-time target list, whose requirements are the
     * main spell's followed by each spliced card's — so the main spell's effects still see only their
     * own targets and a spliced card's `ContextTarget(0)` means its own first target.
     */
    val splicedTargetsOrdered: List<List<ChosenTarget>> = emptyList(),
    val chosenModes: List<Int> = emptyList(),  // For modal spells (700.2). Ordered; same index may repeat when allowRepeat.
    val modeTargetsOrdered: List<List<ChosenTarget>> = emptyList(),  // Per-mode chosen targets, aligned 1:1 with chosenModes
    val modeTargetRequirements: Map<Int, List<TargetRequirement>> = emptyMap(),  // Per-mode TargetRequirements for 608.2b re-validation at resolution
    val modeDamageDistribution: Map<Int, Map<EntityId, Int>> = emptyMap(),  // Per-mode DividedDamageEffect allocations (future)
    /** Snapshots of permanents sacrificed as additional cost (Rule 112.7a — last known info). */
    val sacrificedPermanents: List<EntitySnapshot> = emptyList(),
    val castFaceDown: Boolean = false,  // For morph - creature enters face-down
    val damageDistribution: Map<EntityId, Int>? = null,  // For DividedDamageEffect - pre-chosen damage allocation
    val chosenCreatureType: String? = null,  // For spells that choose a creature type during casting (e.g., Aphetto Dredging)
    val exiledCardCount: Int = 0,  // For variable exile additional costs (e.g., Chill Haunting)
    val additionalCostBlightAmount: Int = 0,  // For variable blight additional costs (e.g., Soul Immolation)
    val additionalCostPayXLifeAmount: Int? = null,  // For pay-X-life additional costs (e.g., Vicious Rivalry); non-null (incl. 0) marks the spell and is coalesced into xValue at resolution
    val castFromZone: Zone? = null,  // Zone the spell was cast from (e.g., HAND for normal casting)
    /**
     * Which alternative casting cost paid for this spell (CR 118.9), or null for a normal cast.
     * The individual `was*` flags below drive *rules* behaviour (warp exiles, evoke sacrifices,
     * cleave swaps the effect); this records the player's declared choice as such so the client
     * view can say how the spell was cast without a flag per mechanic — see
     * [com.wingedsheep.engine.view.CastProvenance]. Disturb, flashback, harmonize, emerge and
     * miracle have no `was*` flag at all, which is exactly why the stack could not describe them.
     */
    val alternativeCost: com.wingedsheep.engine.core.AlternativeCostType? = null,
    val wasWarped: Boolean = false,  // For warp - permanent is exiled at end step
    val wasDashed: Boolean = false,  // For dash (CR 702.109) - permanent gains haste, returns to hand at next end step
    val wasEvoked: Boolean = false,  // For evoke - permanent is sacrificed on ETB
    val wasImpending: Boolean = false,  // For impending - permanent enters with time counters and isn't a creature until they're gone
    val wasCleaved: Boolean = false,  // For cleave (CR 702.148) - spell resolves with its brackets-removed effect/target variant
    /** For sneak (CR 702.190) - permanent spell enters tapped and attacking; the flag is readable via SneakCostWasPaid. */
    val wasSneaked: Boolean = false,
    /**
     * For sneak (CR 702.190b): the player/planeswalker the returned unblocked attacker was
     * attacking. A permanent spell whose sneak cost was paid enters attacking this same
     * defender. Null when not cast for sneak. If the defender is no longer legal at
     * resolution, the resolver enters the creature not attacking (CR 506.3c) — no redirect.
     */
    val sneakAttackDefenderId: EntityId? = null,
    /** For web-slinging (CR 702.188) - the web-slinging cost was paid; readable via WebSlungCostWasPaid. */
    val wasWebSlung: Boolean = false,
    /**
     * For web-slinging (CR 702.188 / 118.9c): the mana value of the tapped creature returned to pay
     * the web-slinging cost, captured before it left the battlefield. Stamped onto the resolving
     * permanent under [com.wingedsheep.sdk.scripting.ChoiceSlot.WEB_SLUNG_RETURNED_MV] so a rider
     * like Scarlet Spider, Ben Reilly can enter with that many +1/+1 counters. 0 when not web-slung.
     */
    val webSlungReturnedManaValue: Int = 0,
    /** For mayhem (CR 702.187) — the mayhem cost was paid; readable via MayhemCostWasPaid. */
    val wasMayhem: Boolean = false,
    val beheldCards: List<EntityId> = emptyList(),  // Cards chosen via Behold (stored in pipeline as named collection)
    /**
     * Entity ids of cards discarded to pay this spell's additional discard cost
     * (`Costs.additional.DiscardCards(...)`). Read at resolution via
     * [com.wingedsheep.sdk.scripting.targets.EffectTarget.DiscardedAsCost] so a condition can
     * test the discarded card (now in the graveyard) — e.g. Grab the Prize's "if the discarded
     * card wasn't a land card". Empty when the spell carried no discard cost.
     */
    val discardedAsCostCards: List<EntityId> = emptyList(),
    /**
     * Last-known-info snapshots (Rule 112.7a) for entities chosen at cost-pay time
     * that may later leave the battlefield before the spell resolves. Populated
     * when an [com.wingedsheep.sdk.scripting.AdditionalCost.ChooseEntity] step
     * has `captureSnapshot = true` — freezes the chosen entity's projected
     * power / toughness / subtypes / controller so downstream effects (e.g.
     * `DynamicAmount.EntityProperty(EntityReference.FromCostStorage(…), …)`)
     * can read "values as they last existed on the battlefield" at resolution.
     */
    val chosenEntitySnapshots: List<EntitySnapshot> = emptyList(),
    val manaSpentWhite: Int = 0,  // Mana colors spent for mana-spent-gated triggers
    val manaSpentBlue: Int = 0,
    val manaSpentBlack: Int = 0,
    val manaSpentRed: Int = 0,
    val manaSpentGreen: Int = 0,
    val manaSpentColorless: Int = 0,
    /**
     * Producing-source subtype → count of mana carrying that subtype spent to cast this spell.
     * Read at resolution (and after it resolves onto the battlefield, via [ManaSpentReader]) by
     * `DynamicAmount.ManaSpentFromSubtype` — e.g. Bat Colony's "a 1/1 Bat for each mana from a Cave
     * spent to cast it". Empty when no tagged mana was spent. See
     * [com.wingedsheep.engine.state.components.player.ManaPoolComponent.manaBySubtype].
     */
    val manaSpentBySubtype: Map<com.wingedsheep.sdk.core.Subtype, Int> = emptyMap(),
    /**
     * Per-color mana spent on the `{X}` portion of this spell, for a color-restricted X
     * (e.g. Soul Burn's "spend only black and/or red mana on X"). Read at resolution via
     * `DynamicAmount.ManaSpentOnX`. Empty when X was unrestricted or the spell has no X.
     */
    val manaSpentOnXByColor: Map<Color, Int> = emptyMap(),
    /**
     * For split-layout cards (CR 709), the index of the face that was cast into
     * [com.wingedsheep.sdk.model.CardDefinition.cardFaces]. Threaded from
     * [com.wingedsheep.engine.core.CastSpell.faceIndex] so the resolution-time handler
     * can attach a [com.wingedsheep.engine.state.components.identity.RoomComponent] with
     * the correct face unlocked. `null` for normal single-face cards.
     */
    val faceIndex: Int? = null,
    /**
     * Names of the "as you cast this spell" condition captures (CR 601.2i) whose condition was true
     * the moment this spell finished being cast. Frozen here so the resolving effect can branch on
     * the cast-time board via [com.wingedsheep.sdk.scripting.conditions.CastTimeFlagSet] even after
     * the board has changed (e.g. Steer Clear's "if you controlled a Mount as you cast this spell").
     * Declared on the card via the `captureAtCast` DSL.
     */
    val castTimeFlags: Set<String> = emptySet()
) : Component

/**
 * Marks an entity as a triggered ability on the stack.
 */
@Serializable
data class TriggeredAbilityOnStackComponent(
    val sourceId: EntityId,
    val sourceName: String,
    val controllerId: EntityId,
    val effect: Effect,
    val description: String,
    /**
     * Definition-scoped identity of the ability that put this object on the stack, shared by every
     * copy of the same card and every future instance of it. Drives batch decisions and persistent
     * yields (see [com.wingedsheep.sdk.scripting.AbilityIdentity]). Null for synthesized sources
     * (e.g. spell copies on a fresh entity) that have no card definition behind them.
     */
    val abilityIdentity: com.wingedsheep.sdk.scripting.AbilityIdentity? = null,
    /** Optional human-readable description from `TriggeredAbility.descriptionOverride`,
     *  used when displaying the ability on the stack instead of the auto-generated effect text. */
    val descriptionOverride: String? = null,
    val triggerDamageAmount: Int? = null,
    val triggeringEntityId: EntityId? = null,
    val triggeringPlayerId: EntityId? = null,
    val xValue: Int? = null,
    val triggerCounterCount: Int? = null,
    val triggerTotalCounterCount: Int? = null,
    /** Last-known counter map (counter-type-string → count) of the trigger's source on leave-battlefield. */
    val triggerLastKnownCounters: Map<String, Int>? = null,
    /** Per-player damage dealt to the trigger's source this turn, captured at LTB time (Grothama). */
    val triggerLastKnownDamageDealtByPlayers: Map<EntityId, Int>? = null,
    /** Creatures blocking/blocked by the trigger's source on leave-battlefield (CR 509 LKI, Abu Ja'far). */
    val triggerLastKnownBlockingOrBlockedByIds: List<EntityId>? = null,
    val targetingSourceEntityId: EntityId? = null,  // The spell/ability that targeted this permanent (for ward)
    /**
     * The host an attachment came *off*, captured when a "becomes unattached" trigger fired.
     * Resolves [com.wingedsheep.sdk.scripting.targets.EffectTarget.AttachedToTriggeringPermanent]
     * there, where the live link is by resolution either gone or already re-pointed at a new host
     * (Stitcher's Graft's "sacrifice that permanent"). Null for every other trigger.
     */
    val triggerUnattachedFromEntityId: EntityId? = null,
    val damageDistribution: Map<EntityId, Int>? = null,  // For DividedDamageEffect - pre-chosen damage allocation
    val copyIndex: Int? = null,    // Which copy number this is (1, 2, 3...) for storm/copy effects
    val copyTotal: Int? = null,    // Total number of copies being created
    val lastKnownPower: Int? = null,    // Power at the moment the triggering entity left the battlefield (dies/leaves)
    val lastKnownToughness: Int? = null, // Toughness at the moment the triggering entity left the battlefield (dies/leaves)
    /** Total last-known power of a creatures-died batch (CR 603.2c). Read via
     *  `ContextPropertyKey.DIED_BATCH_TOTAL_POWER` (The Skullspore Nexus). Null for non-batch triggers. */
    val diedBatchTotalPower: Int? = null,
    /** Number of mode picks recorded by the spell-cast that fired this trigger (Riku of Many Paths). */
    val triggerModesChosenCount: Int? = null,
    /** Power of the aura/equipment's attached creature, captured at trigger time; LKI for
     *  "enchanted creature ... its power" reads when the creature has left (CR 608.2h). */
    val enchantedCreatureLastKnownPower: Int? = null,
    /**
     * The permanent whose `GrantTriggeredAbility` static granted this triggered ability (an
     * Equipment/Aura granting the ability to the attached creature). Read at resolution into
     * [com.wingedsheep.engine.handlers.EffectContext.granterId] so the effect can reference its
     * granter (CR 201.5a) — e.g. Dire Blunderbuss's "sacrifice an artifact other than Dire
     * Blunderbuss". Null for the source's own printed abilities.
     */
    val granterId: EntityId? = null,
    /** Cards looked at by the scry that fired this trigger (CR 701.22). Null for non-scry triggers. */
    val triggerScryCount: Int? = null,
    /** Cards discarded in the batch that fired this trigger (CR 603.2c). Read via
     *  `ContextPropertyKey.TRIGGER_DISCARD_COUNT` (Magmakin Artillerist). Null for non-discard triggers. */
    val triggerDiscardCount: Int? = null,
    /** Discover value N of the discover that fired this trigger (CR 701.57). Null for non-discover triggers. */
    val triggerDiscoverValue: Int? = null,
    /** Damage past lethal dealt to the trigger's creature recipient (CR 120.4a). Null for non-damage triggers. */
    val triggerExcessDamageAmount: Int? = null,
    /** Recipient creature's toughness when the triggering damage was dealt (CR 603.10 LKI). Read via
     *  `ContextPropertyKey.TRIGGER_RECIPIENT_TOUGHNESS` (Taii Wakeen). Null for non-creature recipients. */
    val triggerRecipientToughness: Int? = null,
    /** Total mana spent to cast the spell that fired this trigger (Aberrant Manawurm, Expressive
     *  Firedancer). Read via `ContextPropertyKey.MANA_SPENT_ON_TRIGGERING_SPELL`. Null for non-cast triggers. */
    val triggerManaSpentOnTriggeringSpell: Int? = null,
    /** Distinct colors of mana spent to cast the spell that fired this trigger (Magmablood Archaic).
     *  Read via `ContextPropertyKey.COLORS_SPENT_ON_TRIGGERING_SPELL`. Null for non-cast triggers. */
    val triggerColorsSpentOnTriggeringSpell: Int? = null,
    /** Mana value (CR 202.3) of the spell that fired this trigger (Kellan, the Kid). Read via
     *  `ContextPropertyKey.TRIGGERING_SPELL_MANA_VALUE`. Null for non-cast triggers. */
    val triggerManaValueOfTriggeringSpell: Int? = null,
    /** Value chosen for {X} on the spell that fired this trigger (Geometer's Arthropod). Read via
     *  `ContextPropertyKey.X_VALUE_OF_TRIGGERING_SPELL`. Null for non-cast / no-{X} triggers. */
    val triggerXValueOfTriggeringSpell: Int? = null,
    // Modal fields — populated when this triggered ability is a copy of a modal spell (700.2g).
    // Copies inherit the original's chosen modes; targets either inherit too (StormCopy default)
    // or are re-chosen by the copy controller while modes stay fixed.
    val chosenModes: List<Int> = emptyList(),
    val modeTargetsOrdered: List<List<ChosenTarget>> = emptyList(),
    val modeTargetRequirements: Map<Int, List<TargetRequirement>> = emptyMap(),
    val modeDamageDistribution: Map<Int, Map<EntityId, Int>> = emptyMap(),
    /** Entities a batch trigger captured (the matching permanents in a `PermanentsEnteredEvent`
     *  batch). Seeded into the resolving ability's pipeline under
     *  `PipelineState.TRIGGER_CAPTURED_COLLECTION` so a `ForEachInCollectionEffect` payoff can
     *  iterate them ("for each of them, create a tapped copy" — Kambal). Empty for non-batch triggers. */
    val capturedEntityIds: List<EntityId> = emptyList(),
    /** Set when this triggered ability is a Saga chapter ability; on resolution the engine emits a
     *  SagaChapterResolvedEvent so "final chapter of a Saga resolves" triggers (Tom Bombadil) can fire. */
    val sagaChapterInfo: com.wingedsheep.engine.event.SagaChapterInfo? = null,
    /**
     * Pipeline state carried from a `ReflexiveTriggerEffect`'s action half (e.g. `Amass`'s army
     * reference, a discard's resolved count) into this synthetic reflexive ability's resolution —
     * merged into the built `EffectContext.pipeline` since the reflexive ability builds a fresh
     * context across the stack round-trip (CR 603.12). Null for ordinary triggered abilities.
     */
    val carriedPipeline: com.wingedsheep.engine.handlers.PipelineState? = null
) : Component {
    val hasTargets: Boolean = false  // Will be updated based on effect
}

/**
 * Marks an entity as an activated ability on the stack.
 */
@Serializable
data class ActivatedAbilityOnStackComponent(
    val sourceId: EntityId,
    val sourceName: String,
    val controllerId: EntityId,
    val effect: Effect,
    /** Snapshots of permanents sacrificed as additional cost (Rule 112.7a — last known info). */
    val sacrificedPermanents: List<EntitySnapshot> = emptyList(),
    val xValue: Int? = null,
    val tappedPermanents: List<EntityId> = emptyList(),
    /** LKI snapshots for [tappedPermanents] — see [sacrificedPermanents]. */
    val tappedEntitySnapshots: List<EntitySnapshot> = emptyList(),
    /**
     * Counters (counter-type-string → count) the source had the moment a self-exile /
     * self-sacrifice cost was paid (CR 112.7a). Captured before the cost wipes them so the
     * resolving effect can read the pre-cost count via
     * [com.wingedsheep.sdk.scripting.values.DynamicAmount.LastKnownSourceCounters] (Lost Isle Calling).
     */
    val lastKnownSourceCounters: Map<String, Int> = emptyMap(),
    /**
     * Frozen projected P/T of the source captured before a self-exile / self-sacrifice cost moved
     * it off the battlefield (CR 112.7a). Mirrors [lastKnownSourceCounters]; read at resolution via
     * [com.wingedsheep.engine.handlers.EffectContext.lastKnownSourceSnapshot] so an
     * `EntityProperty(Source, Power)` read (Ghitu Fire-Eater / Blazing Bomb's Blow Up) sees the
     * pre-sacrifice power. Null when the cost did not sacrifice/exile the source.
     */
    val lastKnownSourceSnapshot: EntitySnapshot? = null,
    /**
     * Entity ids of the Equipment/Auras that were attached to the source when a self-sacrifice /
     * self-exile cost was paid (CR 112.7a). Captured before the cost wipes the source's
     * `AttachmentsComponent`; read at resolution via
     * [com.wingedsheep.engine.handlers.EffectContext.lastKnownSourceAttachments] so
     * [com.wingedsheep.sdk.scripting.effects.CardSource.LastKnownEquipmentAttachedToSource] can
     * re-attach "an Equipment that was attached to it" (Zack Fair).
     */
    val lastKnownSourceAttachments: List<EntityId> = emptyList(),
    /** Optional human-readable description from `ActivatedAbility.descriptionOverride`,
     *  used when displaying the ability on the stack instead of the auto-generated effect text. */
    val descriptionOverride: String? = null,
    /**
     * Definition-scoped identity of the activated ability, shared by every copy of the same card
     * and every future instance of it. Drives batch decisions and persistent yields (see
     * [com.wingedsheep.sdk.scripting.AbilityIdentity]). Null for synthesized abilities with no
     * stable [com.wingedsheep.sdk.scripting.AbilityId] behind them (e.g. crew/saddle).
     */
    val abilityIdentity: com.wingedsheep.sdk.scripting.AbilityIdentity? = null,
    /**
     * The permanent whose static ability granted this activated ability (the Equipment/Aura/permanent
     * bearing the `GrantActivatedAbility` static), captured at activation. Read at resolution into
     * [com.wingedsheep.engine.handlers.EffectContext.granterId] so the granted ability can name its
     * granter via [com.wingedsheep.sdk.scripting.targets.EffectTarget.GrantingSource] — e.g. Trusty
     * Boomerang's "Return [this Equipment] to its owner's hand". Null for non-granted abilities.
     */
    val granterId: EntityId? = null,
    /**
     * Division chosen at activation for a `DividedDamageEffect` ability (target -> damage), locked
     * onto the stack object so responding removal can't make the controller re-divide (CR 601.2d).
     * Mirrors [SpellOnStackComponent.damageDistribution]. Null for every other ability, and for a
     * divided-damage ability whose controller left the division to resolution time.
     */
    val damageDistribution: Map<EntityId, Int>? = null
) : Component {
    val hasTargets: Boolean = false  // Will be updated based on effect
}

/**
 * Legacy ability on stack component (for backwards compatibility).
 */
@Serializable
data class AbilityOnStackComponent(
    val sourceId: EntityId,
    val controllerId: EntityId,
    val abilityId: AbilityId,
    val effect: Effect
) : Component

/**
 * Targets chosen for a spell or ability.
 *
 * @property targets The chosen targets
 * @property targetRequirements The original target requirements, used for re-validation
 *           on resolution (Rule 608.2b — targets must still be legal when the spell/ability resolves)
 * @property targetEntryStamps Object-identity stamps for the permanent targets (CR 400.7),
 *           captured when the targets were chosen — see [capture].
 */
@Serializable
data class TargetsComponent(
    val targets: List<ChosenTarget>,
    val targetRequirements: List<TargetRequirement> = emptyList(),
    val targetEntryStamps: Map<EntityId, Long> = emptyMap()
) : Component {

    companion object {
        /**
         * Build the component, snapshotting each permanent target's
         * [com.wingedsheep.engine.state.components.battlefield.BattlefieldEntryTimestampComponent]
         * as it is targeted.
         *
         * Entity ids survive zone round-trips in this engine, so "that id is still a creature on
         * the battlefield" doesn't prove the target is still the object that was targeted: a
         * permanent blinked in response (Personify, Cloudshift, bounce-and-recast) comes back as a
         * *new object* (CR 400.7), and a spell or ability that targeted the old one has an illegal
         * target — if that's its only target, it doesn't resolve (CR 608.2b). Comparing the entry
         * stamp at resolution is what makes that visible; see [isDifferentObject].
         *
         * Every path that chooses or *re-*chooses targets for a stack object goes through here
         * (putting a spell / triggered / activated ability on the stack, and the retarget
         * executors) — a target changed by Misdirection or Grip of Chaos is stamped at the moment
         * it becomes the new target. Copies inherit their source's stamps along with its targets.
         */
        fun capture(
            state: GameState,
            targets: List<ChosenTarget>,
            targetRequirements: List<TargetRequirement> = emptyList()
        ): TargetsComponent = TargetsComponent(
            targets = targets,
            targetRequirements = targetRequirements,
            targetEntryStamps = targets.filterIsInstance<ChosenTarget.Permanent>()
                .filter { it.entityId in state.getBattlefield() }
                .associate { it.entityId to entryStamp(state, it.entityId) }
        )

        /**
         * True when [entityId] is no longer the object that was targeted — it left the battlefield
         * and returned between targeting and now (CR 400.7). Ids with no captured stamp (targets
         * chosen off the battlefield, or a stack object built before the stamps existed) are
         * treated as unchanged.
         */
        fun isDifferentObject(
            state: GameState,
            entityId: EntityId,
            capturedStamps: Map<EntityId, Long>
        ): Boolean {
            val captured = capturedStamps[entityId] ?: return false
            return entryStamp(state, entityId) != captured
        }

        /**
         * The permanent's battlefield-entry stamp, or 0 for a permanent that never got one —
         * boards assembled directly (test fixtures) rather than through a real ETB. Capture and
         * comparison read it the same way, so an unstamped permanent still registers as a new
         * object once it re-enters for real.
         */
        private fun entryStamp(state: GameState, entityId: EntityId): Long =
            state.getEntity(entityId)
                ?.get<com.wingedsheep.engine.state.components.battlefield.BattlefieldEntryTimestampComponent>()
                ?.timestamp
                ?: 0L
    }
}

/**
 * Represents a chosen target for a spell or ability.
 */
@Serializable
sealed interface ChosenTarget {
    @Serializable
    @SerialName("Player")
    data class Player(val playerId: EntityId) : ChosenTarget

    @Serializable
    @SerialName("Permanent")
    data class Permanent(val entityId: EntityId) : ChosenTarget

    @Serializable
    @SerialName("Card")
    data class Card(
        val cardId: EntityId,
        val ownerId: EntityId,
        val zone: Zone
    ) : ChosenTarget

    @Serializable
    @SerialName("Spell")
    data class Spell(val spellEntityId: EntityId) : ChosenTarget
}

/**
 * Additional context for spell resolution.
 */
@Serializable
data class SpellContextComponent(
    val additionalData: Map<String, String> = emptyMap()
) : Component

/**
 * Marks a spell or ability on the stack as having been granted extra keywords (by enum name)
 * while it remains on the stack. Used by effects like Spinerock Tyrant that grant wither to
 * a copied or original spell. The component disappears with the spell entity when it leaves
 * the stack.
 */
@Serializable
data class SpellGrantedKeywordsComponent(
    val keywords: Set<String> = emptySet()
) : Component

/**
 * Riders attached to a spell *copy* that resolves into a token (CR 707.10f — a copy of a permanent
 * spell becomes a token as it resolves). Read by [com.wingedsheep.engine.mechanics.stack.StackResolver]
 * when the copy resolves into a permanent: [addedKeywords] are baked onto the resulting token's
 * base keywords, and — if [sacrificeAtStep] is set — a delayed trigger is registered to sacrifice
 * the token at the beginning of that step ([sacrificeOnlyOnControllersTurn] gates it to the
 * controller's turn). The spell-copy sibling of the same fields on
 * `CreateTokenCopyOfTargetEffect`; used for "Copy target creature spell you control. The copy gains
 * haste and 'At the beginning of the end step, sacrifice this token.'" (Choreographed Sparks).
 */
@Serializable
data class SpellCopyTokenRidersComponent(
    val addedKeywords: Set<Keyword> = emptySet(),
    val sacrificeAtStep: Step? = null,
    val sacrificeOnlyOnControllersTurn: Boolean = false
) : Component

/**
 * Cast-this-way entry rider for a spell cast from the graveyard under a
 * [com.wingedsheep.sdk.scripting.MayCastFromGraveyard] grant that carries rider fields
 * (The Tomb of Aclazotz). The rider is frozen onto the stack spell at cast time — from the
 * *specific* grant that authorized the cast, so it survives grant expiry and is unambiguous when
 * several graveyard-cast grants are active. Read by
 * [com.wingedsheep.engine.mechanics.stack.StackResolver] when the spell resolves onto the
 * battlefield: the permanent enters with one [entersWithCounter] counter and gains
 * [addedSubtype] "in addition to its other types". The component disappears with the spell entity
 * when it leaves the stack (countered / fizzled spells simply never apply it).
 */
@Serializable
data class GraveyardCastRiderComponent(
    val entersWithCounter: com.wingedsheep.sdk.core.CounterType? = null,
    val addedSubtype: String? = null
) : Component
