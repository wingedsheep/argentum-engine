package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.EventPattern.*
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.events.AttackPredicate
import com.wingedsheep.sdk.scripting.events.RecipientFilter
import com.wingedsheep.sdk.scripting.events.SourceFilter
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate

import com.wingedsheep.sdk.scripting.references.Player

/**
 * Facade object providing convenient access to trigger specifications.
 *
 * Each constant returns a [TriggerSpec] that bundles a [EventPattern] with a [TriggerBinding].
 *
 * Usage:
 * ```kotlin
 * Triggers.EntersBattlefield
 * Triggers.Dies
 * Triggers.Attacks
 * ```
 */
object Triggers {

    // =========================================================================
    // Zone Change Triggers
    // =========================================================================

    // -------------------------------------------------------------------------
    // Enters the battlefield
    //
    // High-frequency primitives below. Reach for `entersBattlefield(...)` for
    // any other (filter, binding) combination — face-down filter, type filter,
    // ANY-binding scopes, etc.
    // -------------------------------------------------------------------------

    /**
     * When this permanent enters the battlefield. (SELF, no filter.)
     */
    val EntersBattlefield: TriggerSpec = TriggerSpec(
        event = ZoneChangeEvent(to = Zone.BATTLEFIELD),
        binding = TriggerBinding.SELF
    )

    /**
     * When another creature you control enters the battlefield. (OTHER binding,
     * filter = Creature.youControl().)
     */
    val OtherCreatureEnters: TriggerSpec = TriggerSpec(
        event = ZoneChangeEvent(
            filter = GameObjectFilter.Creature.youControl(),
            to = Zone.BATTLEFIELD
        ),
        binding = TriggerBinding.OTHER
    )

    /**
     * Landfall — whenever a land you control enters the battlefield.
     * (OTHER binding, filter = Land.youControl().)
     */
    val LandYouControlEnters: TriggerSpec = TriggerSpec(
        event = ZoneChangeEvent(
            filter = GameObjectFilter.Land.youControl(),
            to = Zone.BATTLEFIELD
        ),
        binding = TriggerBinding.OTHER
    )

    /**
     * Generic "enters the battlefield" trigger factory. Use the named
     * constants above (`EntersBattlefield`, `OtherCreatureEnters`,
     * `LandYouControlEnters`) when their defaults match; reach for this
     * factory for any other combination of (filter, binding).
     *
     * Examples:
     * - "Whenever a face-down creature enters the battlefield":
     *   `entersBattlefield(filter = GameObjectFilter.Creature.faceDown(),
     *                      binding = TriggerBinding.ANY)`
     * - "Whenever another permanent you control enters the battlefield":
     *   `entersBattlefield(filter = GameObjectFilter.Any.youControl(),
     *                      binding = TriggerBinding.OTHER)`
     * - "Whenever an enchantment you control enters the battlefield" (Eerie):
     *   `entersBattlefield(filter = GameObjectFilter.Enchantment.youControl(),
     *                      binding = TriggerBinding.ANY)`
     */
    fun entersBattlefield(
        filter: GameObjectFilter = GameObjectFilter.Any,
        binding: TriggerBinding = TriggerBinding.SELF,
    ): TriggerSpec = TriggerSpec(
        event = ZoneChangeEvent(filter = filter, to = Zone.BATTLEFIELD),
        binding = binding,
    )

    /**
     * Eerie (part 2) — Whenever you fully unlock a Room (DSK).
     * Fires when both doors of a Room permanent you control have been unlocked.
     */
    val RoomFullyUnlocked: TriggerSpec = TriggerSpec(
        event = RoomFullyUnlockedEvent(Player.You),
        binding = TriggerBinding.ANY
    )

    /**
     * "When you unlock this door, …" (CR 709.5h, DSK Rooms).
     *
     * Authored on a Room's face script. SELF binding combined with the engine's face-aware
     * detection means this trigger only fires when the face that owns the ability becomes
     * unlocked (whether via ETB on the cast face or via the unlock special action). Triggers
     * authored on already-unlocked faces never re-fire when *another* face is unlocked.
     */
    val OnDoorUnlocked: TriggerSpec = TriggerSpec(
        event = DoorUnlockedEvent(Player.You),
        binding = TriggerBinding.SELF
    )

    // -------------------------------------------------------------------------
    // Leaves the battlefield / dies
    //
    // High-frequency primitives below. Reach for `leavesBattlefield(...)` for
    // any other (filter, to, excludeTo, binding) combination — leaves-but-not-
    // dies, leaves-to-any-destination, ANY-binding tribal death scopes, etc.
    // -------------------------------------------------------------------------

    /**
     * When this permanent leaves the battlefield (any destination). (SELF.)
     */
    val LeavesBattlefield: TriggerSpec = TriggerSpec(
        event = ZoneChangeEvent(from = Zone.BATTLEFIELD),
        binding = TriggerBinding.SELF
    )

    /**
     * When this creature dies (CR 700.4 — battlefield to graveyard). (SELF.)
     */
    val Dies: TriggerSpec = TriggerSpec(
        event = ZoneChangeEvent(from = Zone.BATTLEFIELD, to = Zone.GRAVEYARD),
        binding = TriggerBinding.SELF
    )

    /**
     * When any creature dies. (ANY binding, filter = Creature.)
     */
    val AnyCreatureDies: TriggerSpec = TriggerSpec(
        event = ZoneChangeEvent(
            filter = GameObjectFilter.Creature,
            from = Zone.BATTLEFIELD,
            to = Zone.GRAVEYARD
        ),
        binding = TriggerBinding.ANY
    )

    /**
     * When a creature you control dies. (ANY binding, filter = Creature.youControl().)
     */
    val YourCreatureDies: TriggerSpec = TriggerSpec(
        event = ZoneChangeEvent(
            filter = GameObjectFilter.Creature.youControl(),
            from = Zone.BATTLEFIELD,
            to = Zone.GRAVEYARD
        ),
        binding = TriggerBinding.ANY
    )

    /**
     * When this permanent (typically non-creature, e.g. an enchantment or
     * artifact) is put into the graveyard from the battlefield. (SELF.)
     * Same event shape as [Dies]; the rename clarifies non-creature intent
     * in card definitions.
     */
    val PutIntoGraveyardFromBattlefield: TriggerSpec = TriggerSpec(
        event = ZoneChangeEvent(from = Zone.BATTLEFIELD, to = Zone.GRAVEYARD),
        binding = TriggerBinding.SELF
    )

    /**
     * Generic "leaves the battlefield" trigger factory. Use the named
     * constants above ([LeavesBattlefield], [Dies], [AnyCreatureDies],
     * [YourCreatureDies], [PutIntoGraveyardFromBattlefield]) when their
     * defaults match; reach for this factory for any other combination of
     * (filter, to, excludeTo, binding).
     *
     * Examples:
     * - "Whenever another creature dies (any controller)":
     *   `leavesBattlefield(filter = GameObjectFilter.Creature, to = Zone.GRAVEYARD,
     *                      binding = TriggerBinding.OTHER)`
     * - "Whenever a Goblin you don't control dies":
     *   `leavesBattlefield(filter = GameObjectFilter.Creature.withSubtype(Subtype.GOBLIN),
     *                      to = Zone.GRAVEYARD, binding = TriggerBinding.OTHER)`
     * - "Whenever a creature you control leaves the battlefield without dying":
     *   `leavesBattlefield(filter = GameObjectFilter.Creature.youControl(),
     *                      excludeTo = Zone.GRAVEYARD, binding = TriggerBinding.ANY)`
     * - "Whenever a creature you control leaves the battlefield (any zone)":
     *   `leavesBattlefield(filter = GameObjectFilter.Creature.youControl(),
     *                      binding = TriggerBinding.ANY)`
     */
    fun leavesBattlefield(
        filter: GameObjectFilter = GameObjectFilter.Any,
        to: Zone? = null,
        excludeTo: Zone? = null,
        binding: TriggerBinding = TriggerBinding.SELF,
    ): TriggerSpec = TriggerSpec(
        event = ZoneChangeEvent(
            filter = filter,
            from = Zone.BATTLEFIELD,
            to = to,
            excludeTo = excludeTo,
        ),
        binding = binding,
    )

    // =========================================================================
    // Combat Triggers
    // =========================================================================

    // -------------------------------------------------------------------------
    // Attacks (per-attacker `AttackEvent`)
    //
    // High-frequency primitive below. Reach for `attacks(...)` for any other
    // (filter, alone, binding) combination — attacks-alone, ANY-binding scopes,
    // creature-you-control / nontoken-creature-you-control variants, etc.
    // -------------------------------------------------------------------------

    /**
     * When this creature attacks. (SELF.)
     */
    val Attacks: TriggerSpec = TriggerSpec(
        event = AttackEvent(),
        binding = TriggerBinding.SELF
    )

    /**
     * Generic "attacks" trigger factory. Use [Attacks] for the SELF-only
     * unfiltered case; reach for this factory for any other combination.
     *
     * `requires` is a conjunctive set of [AttackPredicate] cases. Adding a
     * new attack-time mechanic later (further attacker-count shapes,
     * with-another-matching-creature, …) is one new sealed-case in
     * [AttackPredicate] + one matcher branch — no new factory parameter.
     *
     * Examples:
     * - "When this creature attacks alone":
     *   `attacks(requires = setOf(AttackPredicate.Alone))`
     * - "Whenever any creature attacks":
     *   `attacks(binding = TriggerBinding.ANY)`
     * - "Whenever a creature you control attacks":
     *   `attacks(filter = GameObjectFilter.Creature.youControl(),
     *            binding = TriggerBinding.ANY)`
     * - "Whenever a nontoken creature you control attacks":
     *   `attacks(filter = GameObjectFilter.Creature.youControl().nontoken(),
     *            binding = TriggerBinding.ANY)`
     * - "Battalion — whenever ~ and at least two other creatures attack":
     *   `attacks(requires = setOf(AttackPredicate.AttackerCountAtLeast(3)))`
     */
    fun attacks(
        filter: GameObjectFilter? = null,
        requires: Set<AttackPredicate> = emptySet(),
        binding: TriggerBinding = TriggerBinding.SELF,
    ): TriggerSpec = TriggerSpec(
        event = AttackEvent(filter = filter, requires = requires),
        binding = binding,
    )

    /**
     * When you attack (declare attackers).
     */
    val YouAttack: TriggerSpec = TriggerSpec(
        event = YouAttackEvent(minAttackers = 1),
        binding = TriggerBinding.ANY
    )

    /**
     * When you attack with one or more creatures matching the given filter.
     * Example: "Whenever you attack with one or more Lizards"
     */
    fun YouAttackWithFilter(filter: GameObjectFilter): TriggerSpec = TriggerSpec(
        event = YouAttackEvent(minAttackers = 1, attackerFilter = filter),
        binding = TriggerBinding.ANY
    )

    /**
     * When one or more creatures attack you.
     * Fires once per combat (not per attacker) when the trigger's controller is
     * declared as defender for at least one creature.
     * Example: "Whenever one or more creatures attack you, ..." (Orim's Prayer).
     */
    val CreaturesAttackYou: TriggerSpec = TriggerSpec(
        event = CreaturesAttackYouEvent(minAttackers = 1),
        binding = TriggerBinding.ANY
    )

    // -------------------------------------------------------------------------
    // Blocks / becomes blocked
    //
    // High-frequency primitives below. Reach for `blocks(...)` / `becomesBlocked(...)`
    // for any other (filter, binding) combination.
    // -------------------------------------------------------------------------

    /**
     * When this creature blocks. (SELF.)
     */
    val Blocks: TriggerSpec = TriggerSpec(
        event = BlockEvent(),
        binding = TriggerBinding.SELF
    )

    /**
     * When this creature becomes blocked. (SELF.)
     */
    val BecomesBlocked: TriggerSpec = TriggerSpec(
        event = BecomesBlockedEvent(),
        binding = TriggerBinding.SELF
    )

    /**
     * When this creature attacks and isn't blocked. (SELF.)
     *
     * Fires for the SELF attacker if it reaches end of Declare Blockers with no
     * blockers assigned (CR 509.3g). Used for cards like Merchant Ship: "Whenever
     * Merchant Ship attacks and isn't blocked, you gain 2 life."
     */
    val AttacksAndIsntBlocked: TriggerSpec = TriggerSpec(
        event = BecomesUnblockedEvent,
        binding = TriggerBinding.SELF
    )

    /**
     * Generic "blocks" trigger factory. Use [Blocks] for the SELF-only
     * unfiltered case; reach for this factory for (filter, binding) variants
     * like "Whenever a creature you control blocks" (ANY binding + filter).
     *
     * [attackerFilter] constrains the blocked attacker — "this creature blocks a
     * creature with flying" is `blocks(attackerFilter = withKeyword(FLYING))`.
     * TriggerContext.triggeringEntityId is set to the blocked attacker. Only the
     * SELF binding honors [attackerFilter] (the detector's ANY branch ignores it),
     * so combining ANY + [attackerFilter] is rejected rather than silently misfiring.
     */
    fun blocks(
        filter: GameObjectFilter? = null,
        binding: TriggerBinding = TriggerBinding.SELF,
        attackerFilter: GameObjectFilter? = null,
    ): TriggerSpec {
        require(attackerFilter == null || binding == TriggerBinding.SELF) {
            "attackerFilter is only supported with TriggerBinding.SELF"
        }
        return TriggerSpec(
            event = BlockEvent(filter = filter, attackerFilter = attackerFilter),
            binding = binding,
        )
    }

    /**
     * Generic "becomes blocked" trigger factory. Use [BecomesBlocked] for the
     * SELF-only unfiltered case; reach for this factory for filtered variants
     * (Berserk Murlodont: `becomesBlocked(filter = Beast, binding = ANY)`) and
     * the ANY-binding "a creature you control becomes blocked" shape.
     */
    fun becomesBlocked(
        filter: GameObjectFilter? = null,
        binding: TriggerBinding = TriggerBinding.SELF,
    ): TriggerSpec = TriggerSpec(
        event = BecomesBlockedEvent(filter = filter),
        binding = binding,
    )

    /**
     * When this creature blocks or becomes blocked by a creature matching the filter.
     * TriggerContext.triggeringEntityId = the combat partner.
     * Sole consumer of [BlocksOrBecomesBlockedByEvent].
     */
    fun BlocksOrBecomesBlockedBy(filter: GameObjectFilter): TriggerSpec = TriggerSpec(
        event = BlocksOrBecomesBlockedByEvent(partnerFilter = filter),
        binding = TriggerBinding.SELF
    )

    // -------------------------------------------------------------------------
    // Damage dealt (outgoing) — see also `dealsDamage(...)` factory below for
    // axis combinations not covered by these named constants.
    // -------------------------------------------------------------------------

    /**
     * When this creature deals damage (any type, any recipient). Binding: SELF.
     */
    val DealsDamage: TriggerSpec = TriggerSpec(
        event = DealsDamageEvent(),
        binding = TriggerBinding.SELF
    )

    /**
     * When this creature deals combat damage to a player. Binding: SELF.
     */
    val DealsCombatDamageToPlayer: TriggerSpec = TriggerSpec(
        event = DealsDamageEvent(damageType = DamageType.Combat, recipient = RecipientFilter.AnyPlayer),
        binding = TriggerBinding.SELF
    )

    /**
     * When this creature deals combat damage to a creature. Binding: SELF.
     */
    val DealsCombatDamageToCreature: TriggerSpec = TriggerSpec(
        event = DealsDamageEvent(damageType = DamageType.Combat, recipient = RecipientFilter.AnyCreature),
        binding = TriggerBinding.SELF
    )

    /**
     * Whenever a creature dealt damage by this permanent this turn dies (Soul Collector shape).
     * Only consumer of [CreatureDealtDamageBySourceDiesEvent].
     */
    val CreatureDealtDamageByThisDies: TriggerSpec = TriggerSpec(
        event = CreatureDealtDamageBySourceDiesEvent,
        binding = TriggerBinding.SELF
    )

    /**
     * Generic "deals damage" trigger factory. Use the named constants above
     * (`DealsDamage`, `DealsCombatDamageToPlayer`, `DealsCombatDamageToCreature`)
     * when their defaults match; reach for this factory for any other combination
     * of (damageType, recipient, sourceFilter, binding).
     *
     * Examples:
     * - "Whenever a creature you control deals combat damage to a player":
     *   `dealsDamage(DamageType.Combat, RecipientFilter.AnyPlayer,
     *                GameObjectFilter.Creature.youControl(), TriggerBinding.ANY)`
     * - "Whenever enchanted creature deals damage":
     *   `dealsDamage(binding = TriggerBinding.ATTACHED)`
     * - "Whenever this deals combat damage to a player or planeswalker":
     *   `dealsDamage(DamageType.Combat, RecipientFilter.AnyPlayerOrPlaneswalker)`
     */
    fun dealsDamage(
        damageType: DamageType = DamageType.Any,
        recipient: RecipientFilter = RecipientFilter.Any,
        sourceFilter: GameObjectFilter? = null,
        binding: TriggerBinding = TriggerBinding.SELF,
        requireExcess: Boolean = false,
    ): TriggerSpec = TriggerSpec(
        event = DealsDamageEvent(
            damageType = damageType,
            recipient = recipient,
            sourceFilter = sourceFilter,
            requireExcess = requireExcess,
        ),
        binding = binding,
    )

    // =========================================================================
    // Phase/Step Triggers
    // =========================================================================

    /**
     * At the beginning of your upkeep.
     */
    val YourUpkeep: TriggerSpec = TriggerSpec(
        event = StepEvent(Step.UPKEEP, Player.You),
        binding = TriggerBinding.ANY
    )

    /**
     * At the beginning of your draw step.
     */
    val YourDrawStep: TriggerSpec = TriggerSpec(
        event = StepEvent(Step.DRAW, Player.You),
        binding = TriggerBinding.ANY
    )

    /**
     * At the beginning of each upkeep.
     */
    val EachUpkeep: TriggerSpec = TriggerSpec(
        event = StepEvent(Step.UPKEEP, Player.Each),
        binding = TriggerBinding.ANY
    )

    /**
     * At the beginning of each opponent's upkeep.
     */
    val EachOpponentUpkeep: TriggerSpec = TriggerSpec(
        event = StepEvent(Step.UPKEEP, Player.EachOpponent),
        binding = TriggerBinding.ANY
    )

    /**
     * At the beginning of your end step.
     */
    val YourEndStep: TriggerSpec = TriggerSpec(
        event = StepEvent(Step.END, Player.You),
        binding = TriggerBinding.ANY
    )

    /**
     * At the beginning of each end step.
     */
    val EachEndStep: TriggerSpec = TriggerSpec(
        event = StepEvent(Step.END, Player.Each),
        binding = TriggerBinding.ANY
    )

    /**
     * At the beginning of combat on your turn.
     */
    val BeginCombat: TriggerSpec = TriggerSpec(
        event = StepEvent(Step.BEGIN_COMBAT, Player.You),
        binding = TriggerBinding.ANY
    )

    /**
     * At the beginning of each combat.
     */
    val EachCombat: TriggerSpec = TriggerSpec(
        event = StepEvent(Step.BEGIN_COMBAT, Player.Each),
        binding = TriggerBinding.ANY
    )

    /**
     * At the beginning of your first main phase.
     */
    val FirstMainPhase: TriggerSpec = TriggerSpec(
        event = StepEvent(Step.PRECOMBAT_MAIN, Player.You),
        binding = TriggerBinding.ANY
    )

    /**
     * At the beginning of your postcombat main phase (second main phase).
     */
    val YourPostcombatMain: TriggerSpec = TriggerSpec(
        event = StepEvent(Step.POSTCOMBAT_MAIN, Player.You),
        binding = TriggerBinding.ANY
    )

    /**
     * Generic "at the beginning of <step>" trigger factory. Use the named
     * constants above (`YourUpkeep`, `YourEndStep`, `BeginCombat`,
     * `EachUpkeep`, `EachEndStep`, …) when their defaults match; reach for
     * this factory for any other combination of (step, player, binding).
     *
     * Examples:
     * - "At the beginning of enchanted creature's controller's upkeep"
     *   (aura grants an upkeep trigger to its target):
     *   `phase(Step.UPKEEP, Player.You, binding = TriggerBinding.ATTACHED)`
     * - "At the beginning of the end step of enchanted creature's
     *   controller" (Lingering Death):
     *   `phase(Step.END, Player.You, binding = TriggerBinding.ATTACHED)`
     */
    fun phase(
        step: Step,
        player: Player = Player.You,
        binding: TriggerBinding = TriggerBinding.ANY,
    ): TriggerSpec = TriggerSpec(
        event = StepEvent(step, player),
        binding = binding,
    )

    /**
     * When this creature is turned face up.
     */
    val TurnedFaceUp: TriggerSpec = TriggerSpec(
        event = TurnFaceUpEvent,
        binding = TriggerBinding.SELF
    )

    /**
     * Generic "is turned face up" factory — use [TurnedFaceUp] for SELF;
     * reach for this factory for ATTACHED ("enchanted creature is turned
     * face up", Fatal Mutation) or other bindings.
     */
    fun turnedFaceUp(binding: TriggerBinding = TriggerBinding.SELF): TriggerSpec =
        TriggerSpec(event = TurnFaceUpEvent, binding = binding)

    /**
     * Whenever a creature is turned face up (any controller).
     * Use [player] to filter: Player.You (default), Player.Any, etc.
     */
    fun CreatureTurnedFaceUp(player: Player = Player.You): TriggerSpec = TriggerSpec(
        event = CreatureTurnedFaceUpEvent(player),
        binding = TriggerBinding.ANY
    )

    /**
     * When you gain control of this permanent from another player.
     * Used by Risky Move.
     */
    val GainControlOfSelf: TriggerSpec = TriggerSpec(
        event = ControlChangeEvent,
        binding = TriggerBinding.SELF
    )

    // =========================================================================
    // Library-to-Graveyard Batching Triggers
    // =========================================================================

    /**
     * Whenever one or more creature cards are put into your graveyard from your library.
     * Batching trigger — fires at most once per event batch regardless of how many creatures were milled.
     */
    val CreaturesPutIntoGraveyardFromLibrary: TriggerSpec = TriggerSpec(
        event = CardsPutIntoGraveyardFromLibraryEvent(
            filter = GameObjectFilter.Creature
        ),
        binding = TriggerBinding.ANY
    )

    /**
     * Whenever one or more cards matching [filter] are put into your graveyard from anywhere.
     * Batching trigger — fires at most once per event batch regardless of how many cards entered,
     * and regardless of which source zone they came from.
     *
     * Example: "Whenever one or more permanent cards are put into your graveyard from anywhere"
     *   → CardsPutIntoYourGraveyard(GameObjectFilter.Permanent)
     */
    fun CardsPutIntoYourGraveyard(filter: GameObjectFilter = GameObjectFilter.Any): TriggerSpec = TriggerSpec(
        event = CardsPutIntoYourGraveyardEvent(filter = filter),
        binding = TriggerBinding.ANY
    )

    /**
     * Whenever one or more permanent cards are put into your graveyard from anywhere.
     * Batching trigger.
     */
    val PermanentCardsPutIntoYourGraveyard: TriggerSpec = TriggerSpec(
        event = CardsPutIntoYourGraveyardEvent(filter = GameObjectFilter.Permanent),
        binding = TriggerBinding.ANY
    )

    // =========================================================================
    // Leave-Graveyard Batching Triggers
    // =========================================================================

    /**
     * Whenever one or more cards matching [filter] leave your graveyard.
     * Batching trigger — fires at most once per event batch regardless of how many cards left,
     * and regardless of where they went (cast/exiled/reanimated/returned to hand, etc.).
     *
     * Pair with `triggerCondition = Conditions.IsYourTurn` for the common
     * "leave your graveyard during your turn" wording, and `oncePerTurn = true`
     * for "this ability triggers only once each turn" (e.g. Kishla Skimmer).
     */
    fun CardsLeaveYourGraveyard(filter: GameObjectFilter = GameObjectFilter.Any): TriggerSpec = TriggerSpec(
        event = CardsLeftYourGraveyardEvent(filter = filter),
        binding = TriggerBinding.ANY
    )

    // =========================================================================
    // Card Drawing Triggers
    // =========================================================================

    /**
     * Whenever you draw a card.
     */
    val YouDraw: TriggerSpec = TriggerSpec(
        event = DrawEvent(Player.You),
        binding = TriggerBinding.ANY
    )

    /**
     * Whenever an opponent draws a card. Fires once per card an opponent draws (CR 121.2).
     */
    val OpponentDraws: TriggerSpec = TriggerSpec(
        event = DrawEvent(Player.EachOpponent),
        binding = TriggerBinding.ANY
    )

    /**
     * Whenever an opponent draws a card, **except** the first card they draw in each of
     * their draw steps (CR 504.1's turn-based draw). Fires once per non-exempt card.
     * Orcish Bowmasters / A-Orcish Bowmasters.
     */
    val OpponentDrawsExceptFirstEachDrawStep: TriggerSpec = TriggerSpec(
        event = DrawEvent(Player.EachOpponent, exceptFirstInDrawStep = true),
        binding = TriggerBinding.ANY
    )

    /**
     * Whenever you reveal a creature card from the first draw of a turn.
     * Used with RevealFirstDrawEachTurn static ability (Primitive Etchings).
     */
    val RevealCreatureFromDraw: TriggerSpec = TriggerSpec(
        event = CardRevealedFromDrawEvent(cardFilter = GameObjectFilter.Creature),
        binding = TriggerBinding.ANY
    )

    /**
     * Whenever you reveal any card from the first draw of a turn.
     * Used with RevealFirstDrawEachTurn static ability.
     */
    val RevealCardFromDraw: TriggerSpec = TriggerSpec(
        event = CardRevealedFromDrawEvent(cardFilter = null),
        binding = TriggerBinding.ANY
    )

    /**
     * Whenever a player draws their Nth card each turn (CR 121.2).
     *
     * The draw analogue of [NthSpellCast]: tracks per-player draw count via
     * `CardsDrawnThisTurnComponent` and fires exactly once when the Nth card
     * is drawn — including when a single multi-card draw crosses the threshold.
     * Putting a card into a player's hand without the word "draw" (CR 121.5)
     * does not advance the count.
     *
     * Example: "Whenever you draw your second card each turn" → `NthCardDrawn(2)`.
     *
     * @param n The card number (e.g., 2 for "second card")
     * @param player Which player's draw count to track (default: you)
     */
    fun NthCardDrawn(n: Int, player: Player = Player.You): TriggerSpec = TriggerSpec(
        event = NthCardDrawnEvent(nthCard = n, player = player),
        binding = TriggerBinding.ANY
    )

    // =========================================================================
    // Spell Triggers
    // =========================================================================

    // -------------------------------------------------------------------------
    // Spell cast (always Player.You, ANY binding).
    //
    // High-frequency type-primitive constants below; reach for `youCastSpell(...)`
    // factory for any from-zone / kicked / treasure-mana / OR-filter combination.
    // -------------------------------------------------------------------------

    /**
     * Whenever you cast a spell.
     */
    val YouCastSpell: TriggerSpec = TriggerSpec(
        event = SpellCastEvent(player = Player.You),
        binding = TriggerBinding.ANY
    )

    /**
     * Whenever you cast a creature spell.
     */
    val YouCastCreature: TriggerSpec = TriggerSpec(
        event = SpellCastEvent(spellFilter = GameObjectFilter.Creature, player = Player.You),
        binding = TriggerBinding.ANY
    )

    /**
     * Whenever you cast a noncreature spell.
     */
    val YouCastNoncreature: TriggerSpec = TriggerSpec(
        event = SpellCastEvent(spellFilter = GameObjectFilter.Noncreature, player = Player.You),
        binding = TriggerBinding.ANY
    )

    /**
     * Whenever you cast an instant or sorcery.
     */
    val YouCastInstantOrSorcery: TriggerSpec = TriggerSpec(
        event = SpellCastEvent(spellFilter = GameObjectFilter.InstantOrSorcery, player = Player.You),
        binding = TriggerBinding.ANY
    )

    /**
     * Whenever you cast an enchantment spell.
     */
    val YouCastEnchantment: TriggerSpec = TriggerSpec(
        event = SpellCastEvent(spellFilter = GameObjectFilter.Enchantment, player = Player.You),
        binding = TriggerBinding.ANY
    )

    /**
     * Whenever you cast a historic spell.
     * Historic = artifact, legendary, or Saga.
     */
    val YouCastHistoric: TriggerSpec = TriggerSpec(
        event = SpellCastEvent(spellFilter = GameObjectFilter.Historic, player = Player.You),
        binding = TriggerBinding.ANY
    )

    /**
     * Whenever you cast a spell with a specific creature subtype.
     * Example: "Whenever you cast a Lizard spell" → `YouCastSubtype(Subtype.LIZARD)`.
     */
    fun YouCastSubtype(subtype: Subtype): TriggerSpec = TriggerSpec(
        event = SpellCastEvent(spellFilter = GameObjectFilter.Any.withSubtype(subtype), player = Player.You),
        binding = TriggerBinding.ANY
    )

    /**
     * Generic "you cast a spell" trigger factory.
     *
     * Use the named constants above (`YouCastSpell`, `YouCastCreature`,
     * `YouCastNoncreature`, `YouCastInstantOrSorcery`, `YouCastEnchantment`,
     * `YouCastHistoric`) when their defaults match. Reach for this factory
     * for any other combination of spell-filter + cast-time predicates.
     *
     * `requires` is a conjunctive set of [SpellCastPredicate] cases. Adding a
     * new cast-time mechanic later (was-copied, was-overloaded,
     * paid-additional-life-cost, …) is one new sealed-case in
     * [SpellCastPredicate] + one matcher branch — no new factory parameter.
     *
     * Examples:
     * - "Whenever you cast a spell from your hand":
     *   `youCastSpell(requires = setOf(SpellCastPredicate.CastFromZone(Zone.HAND)))`
     * - "Whenever you cast an instant or sorcery from your hand":
     *   `youCastSpell(spellFilter = GameObjectFilter.InstantOrSorcery,
     *                 requires = setOf(SpellCastPredicate.CastFromZone(Zone.HAND)))`
     * - "Whenever you cast a kicked spell":
     *   `youCastSpell(requires = setOf(SpellCastPredicate.WasKicked))`
     * - "Whenever you cast a spell using mana from a Treasure":
     *   `youCastSpell(requires = setOf(
     *       SpellCastPredicate.PaidWithManaFromSubtype(Subtype.TREASURE)))`
     *   The same shape will cover Food / Clue / Blood / Powerstone / Map
     *   once the engine's mana-pool tracker tags those subtypes; no SDK
     *   change needed.
     * - "Whenever you cast a noncreature or Lizard spell":
     *   `youCastSpell(spellFilter = GameObjectFilter.Noncreature or
     *                               GameObjectFilter.Any.withSubtype(Subtype.LIZARD))`
     */
    fun youCastSpell(
        spellFilter: GameObjectFilter = GameObjectFilter.Any,
        requires: Set<SpellCastPredicate> = emptySet(),
    ): TriggerSpec = TriggerSpec(
        event = SpellCastEvent(
            spellFilter = spellFilter,
            player = Player.You,
            requires = requires,
        ),
        binding = TriggerBinding.ANY,
    )

    // -------------------------------------------------------------------------
    // Spell cast by another player (any player / an opponent).
    //
    // `Player.Each` / `Player.EachOpponent` are matched at runtime by
    // `TriggerMatcher.matchesPlayer`. Bind the payoff to the caster with
    // `Player.TriggeringPlayer`.
    // -------------------------------------------------------------------------

    /**
     * Whenever a player (any player, including you) casts a spell.
     */
    val AnyPlayerCastsSpell: TriggerSpec = TriggerSpec(
        event = SpellCastEvent(player = Player.Each),
        binding = TriggerBinding.ANY
    )

    /**
     * Whenever an opponent casts a spell.
     */
    val OpponentCastsSpell: TriggerSpec = TriggerSpec(
        event = SpellCastEvent(player = Player.EachOpponent),
        binding = TriggerBinding.ANY
    )

    /**
     * Whenever a player (any player, including you) chooses one or more targets — i.e. casts a
     * spell, activates an ability, or puts a triggered ability on the stack with at least one
     * target. The triggering entity is that spell/ability, so the payoff can read and change its
     * targets (Psychic Battle).
     */
    val AnyPlayerChoosesTargets: TriggerSpec = TriggerSpec(
        event = TargetsChosenEvent(player = Player.Each),
        binding = TriggerBinding.ANY
    )

    /**
     * Whenever a player (any player, including you) casts a spell matching [spellFilter].
     * Example: "Whenever a player casts a creature spell" → `anyPlayerCasts(GameObjectFilter.Creature)`.
     */
    fun anyPlayerCasts(
        spellFilter: GameObjectFilter = GameObjectFilter.Any,
        requires: Set<SpellCastPredicate> = emptySet(),
    ): TriggerSpec = TriggerSpec(
        event = SpellCastEvent(spellFilter = spellFilter, player = Player.Each, requires = requires),
        binding = TriggerBinding.ANY
    )

    /**
     * Whenever an opponent casts a spell matching [spellFilter].
     * Example: "Whenever an opponent casts a multicolored spell" →
     * `opponentCasts(GameObjectFilter.Multicolored)`.
     */
    fun opponentCasts(
        spellFilter: GameObjectFilter = GameObjectFilter.Any,
        requires: Set<SpellCastPredicate> = emptySet(),
    ): TriggerSpec = TriggerSpec(
        event = SpellCastEvent(spellFilter = spellFilter, player = Player.EachOpponent, requires = requires),
        binding = TriggerBinding.ANY
    )

    // =========================================================================
    // Discard Triggers
    // =========================================================================

    /**
     * Whenever an opponent discards a card.
     */
    val AnyOpponentDiscards: TriggerSpec = TriggerSpec(
        event = DiscardEvent(player = Player.EachOpponent),
        binding = TriggerBinding.ANY
    )

    /**
     * Whenever you discard a card.
     */
    val YouDiscard: TriggerSpec = TriggerSpec(
        event = DiscardEvent(player = Player.You),
        binding = TriggerBinding.ANY
    )

    /**
     * Generic "discards a card" trigger factory. Use [AnyOpponentDiscards] /
     * [YouDiscard] when their defaults match; reach for this factory for
     * card-type filters or "any player discards" scopes.
     *
     * Examples:
     * - "Whenever a player discards a card":
     *   `discards(player = Player.Each)`
     * - "Whenever an opponent discards a creature card":
     *   `discards(player = Player.EachOpponent, cardFilter = GameObjectFilter.Creature)`
     *
     * Note: fires once per card discarded — e.g. an opponent discarding 3 cards
     * in one resolution fires this 3 times. Mirrors how [YouDraw] handles
     * multi-card draws.
     */
    fun discards(
        player: Player = Player.Each,
        cardFilter: GameObjectFilter? = null,
    ): TriggerSpec = TriggerSpec(
        event = DiscardEvent(player = player, cardFilter = cardFilter),
        binding = TriggerBinding.ANY,
    )

    // =========================================================================
    // Stack Triggers
    // =========================================================================

    /**
     * Whenever a spell or ability is put onto the stack (any player).
     * Used for Grip of Chaos: "Whenever a spell or ability is put onto the stack..."
     */
    val AnySpellOrAbilityOnStack: TriggerSpec = TriggerSpec(
        event = SpellOrAbilityOnStackEvent,
        binding = TriggerBinding.ANY
    )

    /**
     * Whenever an opponent activates an ability that isn't a mana ability (CR 605 / 606).
     * Mana abilities don't use the stack, so they never fire this; loyalty abilities (which are
     * activated abilities) do. Used for Flamescroll Celebrant.
     */
    val OpponentActivatesAbility: TriggerSpec = TriggerSpec(
        event = AbilityActivatedEvent(player = Player.EachOpponent),
        binding = TriggerBinding.ANY
    )

    // =========================================================================
    // Counter Triggers
    // =========================================================================

    /**
     * Whenever you put one or more +1/+1 counters on a creature you control.
     */
    val PlusOneCountersPlacedOnYourCreature: TriggerSpec = TriggerSpec(
        event = CountersPlacedEvent(
            counterType = Counters.PLUS_ONE_PLUS_ONE,
            filter = GameObjectFilter.Creature.youControl()
        ),
        binding = TriggerBinding.ANY
    )

    /**
     * Whenever one or more counters of any type are put on a permanent matching [filter],
     * optionally only the first time counters land on that permanent this turn.
     *
     * Defaults to "a creature you control … for the first time this turn" — the Stalwart
     * Successor shape. Pass a different [filter] or `firstTimeEachTurn = false` to reuse for
     * other "counters placed" payoffs. The triggering permanent is available via
     * [com.wingedsheep.sdk.scripting.targets.EffectTarget.TriggeringEntity].
     */
    fun countersPlacedOn(
        filter: GameObjectFilter = GameObjectFilter.Creature.youControl(),
        counterType: String = Counters.ANY,
        firstTimeEachTurn: Boolean = true,
    ): TriggerSpec = TriggerSpec(
        event = CountersPlacedEvent(
            counterType = counterType,
            filter = filter,
            firstTimeEachTurn = firstTimeEachTurn,
        ),
        binding = TriggerBinding.ANY
    )

    // =========================================================================
    // Damage Received (incoming)
    // =========================================================================

    /**
     * Whenever this creature is dealt damage (any source). Binding: SELF.
     * For source-restricted variants (damaged by a creature/spell/color/etc.)
     * or enchanted-creature wiring, use [takesDamage].
     */
    val TakesDamage: TriggerSpec = TriggerSpec(
        event = DamageReceivedEvent(),
        binding = TriggerBinding.SELF
    )

    /**
     * Generic "is dealt damage" trigger factory. Use [TakesDamage] for the
     * default SELF/any-source case; reach for this factory for source restrictions
     * (creature, spell, color) or for attached bindings (e.g. "enchanted creature
     * is dealt damage": `takesDamage(binding = TriggerBinding.ATTACHED)`).
     */
    fun takesDamage(
        source: SourceFilter = SourceFilter.Any,
        binding: TriggerBinding = TriggerBinding.SELF,
    ): TriggerSpec = TriggerSpec(
        event = DamageReceivedEvent(source = source),
        binding = binding,
    )

    // -------------------------------------------------------------------------
    // Aura / Equipment binding (TriggerBinding.ATTACHED)
    //
    // No named constants for the "enchanted/equipped creature does X" shapes —
    // they collapse to the existing event factories with `binding = ATTACHED`:
    //
    // - "Enchanted creature dies":
    //     leavesBattlefield(to = Zone.GRAVEYARD, binding = TriggerBinding.ATTACHED)
    // - "Enchanted/equipped creature leaves the battlefield":
    //     leavesBattlefield(binding = TriggerBinding.ATTACHED)
    // - "Enchanted/equipped creature attacks":
    //     attacks(binding = TriggerBinding.ATTACHED)
    // - "Enchanted permanent becomes tapped":
    //     becomesTapped(binding = TriggerBinding.ATTACHED)
    // - "Enchanted creature is turned face up":
    //     turnedFaceUp(binding = TriggerBinding.ATTACHED)
    // - "At the beginning of enchanted creature's controller's <step>":
    //     phase(step, Player.You, binding = TriggerBinding.ATTACHED)
    // -------------------------------------------------------------------------

    // =========================================================================
    // Tap/Untap Triggers
    // =========================================================================

    /**
     * Whenever this permanent becomes tapped.
     */
    val BecomesTapped: TriggerSpec = TriggerSpec(
        event = TapEvent(),
        binding = TriggerBinding.SELF
    )

    /**
     * Whenever this permanent becomes untapped.
     */
    val BecomesUntapped: TriggerSpec = TriggerSpec(
        event = UntapEvent,
        binding = TriggerBinding.SELF
    )

    /**
     * Generic "becomes tapped" factory — use [BecomesTapped] for SELF;
     * reach for this factory for ATTACHED ("enchanted permanent becomes
     * tapped", Uncontrolled Infestation) or other bindings. Pass [filter] with
     * [TriggerBinding.ANY] for "whenever a creature or land becomes tapped"
     * (Temporal Distortion).
     */
    fun becomesTapped(
        binding: TriggerBinding = TriggerBinding.SELF,
        filter: GameObjectFilter? = null
    ): TriggerSpec =
        TriggerSpec(event = TapEvent(filter), binding = binding)

    /**
     * Whenever any player taps a land for mana. (ANY binding.)
     *
     * Backs the "Whenever a player taps a land for mana" family (Overabundance, Mana Flare,
     * Heartbeat of Spring). For "an opponent" / "you" variants or a land-type restriction, use
     * [landTappedForMana].
     */
    val AnyPlayerTapsLandForMana: TriggerSpec = TriggerSpec(
        event = LandTappedForMana(player = Player.Each),
        binding = TriggerBinding.ANY
    )

    /**
     * "Whenever <player> taps a <landFilter> for mana" factory.
     */
    fun landTappedForMana(
        player: Player = Player.Each,
        landFilter: GameObjectFilter? = null,
        binding: TriggerBinding = TriggerBinding.ANY
    ): TriggerSpec = TriggerSpec(
        event = LandTappedForMana(player = player, landFilter = landFilter),
        binding = binding
    )

    // =========================================================================
    // Cycling Triggers
    // =========================================================================

    /**
     * When you cycle this card.
     */
    val YouCycleThis: TriggerSpec = TriggerSpec(
        event = CycleEvent(Player.You),
        binding = TriggerBinding.SELF
    )

    /**
     * Whenever a player cycles a card.
     */
    val AnyPlayerCycles: TriggerSpec = TriggerSpec(
        event = CycleEvent(Player.Each),
        binding = TriggerBinding.ANY
    )

    // =========================================================================
    // Crime Triggers (Outlaws of Thunder Junction)
    // =========================================================================

    /**
     * Whenever you commit a crime.
     * Used by Forsaken Miner: "Whenever you commit a crime, you may pay {B}.
     * If you do, return this card from your graveyard to the battlefield."
     */
    val YouCommitCrime: TriggerSpec = TriggerSpec(
        event = CommitCrimeEvent(Player.You),
        binding = TriggerBinding.ANY
    )

    // =========================================================================
    // Plot Triggers (Outlaws of Thunder Junction)
    // =========================================================================

    /**
     * When this card becomes plotted (CR 718). SELF binding — fires for the very card that
     * was plotted, while it sits face up in exile. Used by Aloe Alchemist: "When this card
     * becomes plotted, target creature gets +3/+2 and gains trample until end of turn."
     */
    val BecomesPlotted: TriggerSpec = TriggerSpec(
        event = BecomesPlottedEvent,
        binding = TriggerBinding.SELF
    )

    // =========================================================================
    // Gift Triggers
    // =========================================================================

    /**
     * Whenever you give a gift.
     * Used by Jolly Gerbils: "Whenever you give a gift, draw a card."
     */
    val YouGiveAGift: TriggerSpec = TriggerSpec(
        event = GiftGivenEvent(Player.You),
        binding = TriggerBinding.ANY
    )

    // =========================================================================
    // Transform Triggers
    // =========================================================================

    /**
     * When this creature transforms.
     */
    val Transforms: TriggerSpec = TriggerSpec(
        event = TransformEvent(),
        binding = TriggerBinding.SELF
    )

    /**
     * When this creature transforms into its back face.
     */
    val TransformsToBack: TriggerSpec = TriggerSpec(
        event = TransformEvent(intoBackFace = true),
        binding = TriggerBinding.SELF
    )

    /**
     * When this creature transforms into its front face.
     */
    val TransformsToFront: TriggerSpec = TriggerSpec(
        event = TransformEvent(intoBackFace = false),
        binding = TriggerBinding.SELF
    )

    // =========================================================================
    // Targeting Triggers
    // =========================================================================

    /**
     * When this permanent becomes the target of a spell or ability.
     */
    val BecomesTarget: TriggerSpec = TriggerSpec(
        event = BecomesTargetEvent(),
        binding = TriggerBinding.SELF
    )

    /**
     * Whenever this permanent becomes the target of a spell or ability an opponent
     * controls (Cactarantula). Self-bound counterpart of
     * [CreatureYouControlBecomesTargetByOpponent].
     */
    val BecomesTargetByOpponent: TriggerSpec = TriggerSpec(
        event = BecomesTargetEvent(byOpponent = true),
        binding = TriggerBinding.SELF
    )

    /**
     * Whenever a creature you control with a specific filter becomes the target
     * of a spell or ability.
     */
    fun BecomesTarget(filter: GameObjectFilter): TriggerSpec = TriggerSpec(
        event = BecomesTargetEvent(targetFilter = filter),
        binding = TriggerBinding.ANY
    )

    /**
     * Whenever a creature you control becomes the target of a spell or ability
     * an opponent controls.
     *
     * [includeSpellTargets] also fires when an opponent targets a matching *spell* you control on
     * the stack — the "... or a creature spell you control" half of Surrak, Elusive Hunter. Leave it
     * false (default) for the plain "a creature you control" wording (Pawpatch Recruit, Elrond),
     * which is permanent-only.
     */
    fun CreatureYouControlBecomesTargetByOpponent(
        filter: GameObjectFilter = GameObjectFilter.Creature,
        includeSpellTargets: Boolean = false
    ): TriggerSpec = TriggerSpec(
        event = BecomesTargetEvent(
            targetFilter = filter.youControl(),
            byOpponent = true,
            includeSpellTargets = includeSpellTargets
        ),
        binding = TriggerBinding.ANY
    )

    /**
     * Valiant — Whenever this creature becomes the target of a spell or ability
     * you control for the first time each turn.
     */
    val Valiant: TriggerSpec = TriggerSpec(
        event = BecomesTargetEvent(byYou = true, firstTimeEachTurn = true),
        binding = TriggerBinding.SELF
    )

    // =========================================================================
    // Life Triggers
    // =========================================================================

    /**
     * Whenever you gain life.
     */
    val YouGainLife: TriggerSpec = TriggerSpec(
        event = LifeGainEvent(Player.You),
        binding = TriggerBinding.ANY
    )

    /**
     * Whenever a player gains life.
     */
    val AnyPlayerGainsLife: TriggerSpec = TriggerSpec(
        event = LifeGainEvent(Player.Each),
        binding = TriggerBinding.ANY
    )

    // =========================================================================
    // Life Loss Triggers
    // =========================================================================

    /**
     * Whenever you lose life.
     */
    val YouLoseLife: TriggerSpec = TriggerSpec(
        event = LifeLossEvent(Player.You),
        binding = TriggerBinding.ANY
    )

    /**
     * Whenever a player loses life.
     */
    val AnyPlayerLosesLife: TriggerSpec = TriggerSpec(
        event = LifeLossEvent(Player.Each),
        binding = TriggerBinding.ANY
    )

    /**
     * Whenever you gain or lose life.
     * Used for cards like Moonstone Harbinger.
     */
    val YouGainOrLoseLife: TriggerSpec = TriggerSpec(
        event = LifeGainOrLossEvent(Player.You),
        binding = TriggerBinding.ANY
    )

    // =========================================================================
    // Sacrifice Triggers
    // =========================================================================

    /**
     * Whenever you sacrifice one or more permanents matching the filter.
     * Batching trigger — fires at most once per event batch.
     *
     * Example: "Whenever you sacrifice one or more Foods"
     * → YouSacrificeOneOrMore(GameObjectFilter.Artifact.withSubtype("Food"))
     */
    fun YouSacrificeOneOrMore(filter: GameObjectFilter = GameObjectFilter.Any): TriggerSpec = TriggerSpec(
        event = PermanentsSacrificedEvent(filter = filter),
        binding = TriggerBinding.ANY
    )

    /**
     * When you sacrifice this permanent.
     * Distinct from [PutIntoGraveyardFromBattlefield] / [Dies], which fire on any
     * battlefield-to-graveyard transition (including destruction).
     */
    val Sacrificed: TriggerSpec = TriggerSpec(
        event = PermanentsSacrificedEvent(),
        binding = TriggerBinding.SELF
    )

    // =========================================================================
    // Leave Battlefield Without Dying Triggers
    // =========================================================================

    /**
     * Whenever one or more other creatures you control leave the battlefield without dying.
     * Batching trigger — fires at most once per event batch.
     * "Without dying" means the creature moves to any zone other than the graveyard.
     *
     * Example: "Whenever one or more other creatures you control leave the battlefield without dying, draw a card."
     * → OneOrMoreLeaveWithoutDying(excludeSelf = true)
     */
    fun OneOrMoreLeaveWithoutDying(
        filter: GameObjectFilter = GameObjectFilter.Creature,
        excludeSelf: Boolean = false
    ): TriggerSpec = TriggerSpec(
        event = LeaveBattlefieldWithoutDyingEvent(filter = filter, excludeSelf = excludeSelf),
        binding = TriggerBinding.ANY
    )

    // =========================================================================
    // Death Batch Triggers
    // =========================================================================

    /**
     * Whenever one or more [other] creatures you control die.
     * Batching trigger — fires at most once per event batch, regardless of how many
     * creatures died simultaneously. This is the correct shape for "one or more
     * creatures died" payoffs: a per-creature [YourCreatureDies] trigger over-counts
     * on board wipes (one firing per creature), while this fires exactly once.
     *
     * Set [excludeSelf] for the "one or more *other* creatures you control die" wording —
     * the source's own death is excluded from the batch.
     *
     * Example: "Whenever one or more other creatures you control die, put a +1/+1 counter on this creature."
     * → OneOrMoreCreaturesYouControlDie(excludeSelf = true)   (Vengeful Townsfolk)
     */
    fun OneOrMoreCreaturesYouControlDie(
        filter: GameObjectFilter = GameObjectFilter.Creature,
        excludeSelf: Boolean = false
    ): TriggerSpec = TriggerSpec(
        event = CreaturesYouControlDiedEvent(filter = filter, excludeSelf = excludeSelf),
        binding = TriggerBinding.ANY
    )

    // =========================================================================
    // Enter Battlefield Batch Triggers
    // =========================================================================

    /**
     * Whenever one or more permanents matching a filter you control enter the battlefield.
     * Batching trigger — fires at most once per event batch.
     *
     * Example: "Whenever one or more noncreature, nonland permanents you control enter"
     * → OneOrMorePermanentsEnter(GameObjectFilter.Noncreature and GameObjectFilter.Nonland)
     */
    fun OneOrMorePermanentsEnter(filter: GameObjectFilter = GameObjectFilter.Any): TriggerSpec = TriggerSpec(
        event = PermanentsEnteredEvent(filter = filter),
        binding = TriggerBinding.ANY
    )

    // =========================================================================
    // Nth Spell Cast Triggers
    // =========================================================================

    /**
     * Whenever a player casts their Nth spell each turn.
     * Example: "Whenever a player casts their second spell each turn" → NthSpellCast(2)
     *
     * @param n The spell number (e.g., 2 for "second spell")
     * @param player Which player's spell count to track (default: any player)
     */
    fun NthSpellCast(n: Int, player: Player = Player.Each): TriggerSpec = TriggerSpec(
        event = NthSpellCastEvent(nthSpell = n, player = player),
        binding = TriggerBinding.ANY
    )

    /**
     * When you cast this spell — a "cast trigger" that fires on the spell's own cast while it is
     * still on the stack, distinct from [SpellCast] (which observes *other* spells from the
     * battlefield). Pair with `triggerCondition` for an intervening "if" (CR 603.4).
     *
     * Example: Sage of the Skies — "When you cast this spell, if you've cast another spell this
     * turn, copy this spell." (`triggerCondition = Conditions.YouCastSpellsThisTurn(atLeast = 2)`,
     * since the spell itself is already counted when its cast trigger is checked.)
     */
    fun WhenYouCastThisSpell(): TriggerSpec = TriggerSpec(
        event = CastThisSpellEvent,
        binding = TriggerBinding.SELF
    )

    // =========================================================================
    // Expend Triggers (Bloomburrow)
    // =========================================================================

    /**
     * Whenever you expend N (spend your Nth total mana on spells this turn).
     * Triggers at most once per turn.
     */
    fun Expend(threshold: Int): TriggerSpec = TriggerSpec(
        event = ExpendEvent(threshold),
        binding = TriggerBinding.ANY
    )

    // =========================================================================
    // The Ring Triggers (CR 701.54)
    // =========================================================================

    /**
     * Whenever the Ring tempts you (CR 701.54d). Fires when this ability's controller is tempted,
     * even if some or all of the tempt actions were impossible.
     */
    val RingTemptsYou: TriggerSpec = TriggerSpec(
        event = RingTemptedEvent(Player.You),
        binding = TriggerBinding.ANY
    )

    // =========================================================================
    // Scry Triggers (CR 701.18)
    // =========================================================================

    /**
     * Whenever you scry. Fires once per scry resolution, after the cards have been
     * placed on top/bottom. Pair with [com.wingedsheep.sdk.scripting.values.DynamicAmount.ContextProperty]
     * + [com.wingedsheep.sdk.scripting.values.ContextPropertyKey.TRIGGER_SCRY_COUNT]
     * to scale by "the number of cards looked at" (Celeborn the Wise, Elrond Master of Healing).
     */
    val WheneverYouScry: TriggerSpec = TriggerSpec(
        event = ScriedEvent(Player.You),
        binding = TriggerBinding.ANY
    )
}
