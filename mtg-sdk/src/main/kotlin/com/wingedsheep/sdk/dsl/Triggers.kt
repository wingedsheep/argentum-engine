package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.GameEvent.*
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.events.RecipientFilter
import com.wingedsheep.sdk.scripting.events.SourceFilter

import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Facade object providing convenient access to trigger specifications.
 *
 * Each constant returns a [TriggerSpec] that bundles a [GameEvent] with a [TriggerBinding].
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

    /**
     * When this permanent enters the battlefield.
     */
    val EntersBattlefield: TriggerSpec = TriggerSpec(
        event = ZoneChangeEvent(to = Zone.BATTLEFIELD),
        binding = TriggerBinding.SELF
    )

    /**
     * When any permanent enters the battlefield.
     */
    val AnyEntersBattlefield: TriggerSpec = TriggerSpec(
        event = ZoneChangeEvent(to = Zone.BATTLEFIELD),
        binding = TriggerBinding.ANY
    )

    /**
     * When a face-down creature enters the battlefield (any controller).
     * Compose with `.youControl()` filter for "you control" variant.
     */
    val FaceDownCreatureEnters: TriggerSpec = TriggerSpec(
        event = ZoneChangeEvent(
            filter = GameObjectFilter.Creature.faceDown(),
            to = Zone.BATTLEFIELD
        ),
        binding = TriggerBinding.ANY
    )

    /**
     * When another creature you control enters the battlefield.
     */
    val OtherCreatureEnters: TriggerSpec = TriggerSpec(
        event = ZoneChangeEvent(
            filter = GameObjectFilter.Creature.youControl(),
            to = Zone.BATTLEFIELD
        ),
        binding = TriggerBinding.OTHER
    )

    /**
     * When another creature enters the battlefield (any controller).
     */
    val AnyOtherCreatureEnters: TriggerSpec = TriggerSpec(
        event = ZoneChangeEvent(
            filter = GameObjectFilter.Creature,
            to = Zone.BATTLEFIELD
        ),
        binding = TriggerBinding.OTHER
    )

    /**
     * When another permanent you control enters the battlefield.
     */
    val OtherPermanentYouControlEnters: TriggerSpec = TriggerSpec(
        event = ZoneChangeEvent(
            filter = GameObjectFilter.Any.youControl(),
            to = Zone.BATTLEFIELD
        ),
        binding = TriggerBinding.OTHER
    )

    /**
     * Landfall — Whenever a land you control enters the battlefield.
     */
    val LandYouControlEnters: TriggerSpec = TriggerSpec(
        event = ZoneChangeEvent(
            filter = GameObjectFilter.Land.youControl(),
            to = Zone.BATTLEFIELD
        ),
        binding = TriggerBinding.OTHER
    )

    /**
     * Eerie (part 1) — Whenever an enchantment you control enters the battlefield.
     * Fires for any enchantment you control, including the source if it's an enchantment.
     */
    val AnyEnchantmentYouControlEnters: TriggerSpec = TriggerSpec(
        event = ZoneChangeEvent(
            filter = GameObjectFilter.Enchantment.youControl(),
            to = Zone.BATTLEFIELD
        ),
        binding = TriggerBinding.ANY
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
     * When this permanent leaves the battlefield.
     */
    val LeavesBattlefield: TriggerSpec = TriggerSpec(
        event = ZoneChangeEvent(from = Zone.BATTLEFIELD),
        binding = TriggerBinding.SELF
    )

    /**
     * When this creature dies.
     */
    val Dies: TriggerSpec = TriggerSpec(
        event = ZoneChangeEvent(from = Zone.BATTLEFIELD, to = Zone.GRAVEYARD),
        binding = TriggerBinding.SELF
    )

    /**
     * When any creature dies.
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
     * When another creature dies (any controller).
     */
    val AnyOtherCreatureDies: TriggerSpec = TriggerSpec(
        event = ZoneChangeEvent(
            filter = GameObjectFilter.Creature,
            from = Zone.BATTLEFIELD,
            to = Zone.GRAVEYARD
        ),
        binding = TriggerBinding.OTHER
    )

    /**
     * When a creature you control dies.
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
     * When this creature or another creature you control leaves the battlefield without dying.
     * "Leaves without dying" = zone change from battlefield to any zone except graveyard.
     */
    val YourCreatureLeavesBattlefieldWithoutDying: TriggerSpec = TriggerSpec(
        event = ZoneChangeEvent(
            filter = GameObjectFilter.Creature.youControl(),
            from = Zone.BATTLEFIELD,
            excludeTo = Zone.GRAVEYARD
        ),
        binding = TriggerBinding.ANY
    )

    /**
     * When this is put into the graveyard from the battlefield.
     */
    val PutIntoGraveyardFromBattlefield: TriggerSpec = TriggerSpec(
        event = ZoneChangeEvent(from = Zone.BATTLEFIELD, to = Zone.GRAVEYARD),
        binding = TriggerBinding.SELF
    )

    /**
     * When another creature with a specific subtype dies.
     */
    fun OtherCreatureWithSubtypeDies(subtype: Subtype): TriggerSpec = TriggerSpec(
        event = ZoneChangeEvent(
            filter = GameObjectFilter.Creature.withSubtype(subtype),
            from = Zone.BATTLEFIELD,
            to = Zone.GRAVEYARD
        ),
        binding = TriggerBinding.OTHER
    )

    // =========================================================================
    // Combat Triggers
    // =========================================================================

    /**
     * When this creature attacks.
     */
    val Attacks: TriggerSpec = TriggerSpec(
        event = AttackEvent(),
        binding = TriggerBinding.SELF
    )

    /**
     * When this creature attacks alone (is the only declared attacker).
     */
    val AttacksAlone: TriggerSpec = TriggerSpec(
        event = AttackEvent(alone = true),
        binding = TriggerBinding.SELF
    )

    /**
     * When any creature attacks.
     */
    val AnyAttacks: TriggerSpec = TriggerSpec(
        event = AttackEvent(),
        binding = TriggerBinding.ANY
    )

    /**
     * When a nontoken creature you control attacks.
     * Creates one trigger per nontoken attacker controlled by the ability's controller.
     */
    val NontokenCreatureYouControlAttacks: TriggerSpec = TriggerSpec(
        event = AttackEvent(
            filter = GameObjectFilter(
                cardPredicates = listOf(CardPredicate.IsCreature, CardPredicate.IsNontoken),
                controllerPredicate = ControllerPredicate.ControlledByYou
            )
        ),
        binding = TriggerBinding.ANY
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
     * When this creature blocks.
     */
    val Blocks: TriggerSpec = TriggerSpec(
        event = BlockEvent(),
        binding = TriggerBinding.SELF
    )

    /**
     * When a creature you control blocks.
     * Creates one trigger per matching blocker controlled by the ability's controller.
     */
    val CreatureYouControlBlocks: TriggerSpec = TriggerSpec(
        event = BlockEvent(
            filter = GameObjectFilter(
                cardPredicates = listOf(CardPredicate.IsCreature),
                controllerPredicate = ControllerPredicate.ControlledByYou
            )
        ),
        binding = TriggerBinding.ANY
    )

    /**
     * When this creature becomes blocked.
     */
    val BecomesBlocked: TriggerSpec = TriggerSpec(
        event = BecomesBlockedEvent(),
        binding = TriggerBinding.SELF
    )

    /**
     * When a creature you control becomes blocked.
     */
    val CreatureYouControlBecomesBlocked: TriggerSpec = TriggerSpec(
        event = BecomesBlockedEvent(),
        binding = TriggerBinding.ANY
    )

    /**
     * Whenever a creature matching the filter becomes blocked (any controller).
     * Used for cards like Berserk Murlodont: "Whenever a Beast becomes blocked..."
     */
    fun FilteredBecomesBlocked(filter: GameObjectFilter): TriggerSpec = TriggerSpec(
        event = BecomesBlockedEvent(filter = filter),
        binding = TriggerBinding.ANY
    )

    /**
     * When this creature blocks or becomes blocked by a creature matching the filter.
     * TriggerContext.triggeringEntityId = the combat partner.
     */
    fun BlocksOrBecomesBlockedBy(filter: GameObjectFilter): TriggerSpec = TriggerSpec(
        event = BlocksOrBecomesBlockedByEvent(partnerFilter = filter),
        binding = TriggerBinding.SELF
    )

    /**
     * When this creature deals damage.
     */
    val DealsDamage: TriggerSpec = TriggerSpec(
        event = DealsDamageEvent(),
        binding = TriggerBinding.SELF
    )

    /**
     * When this creature deals combat damage.
     */
    val DealsCombatDamage: TriggerSpec = TriggerSpec(
        event = DealsDamageEvent(damageType = DamageType.Combat),
        binding = TriggerBinding.SELF
    )

    /**
     * When this creature deals combat damage to a player.
     */
    val DealsCombatDamageToPlayer: TriggerSpec = TriggerSpec(
        event = DealsDamageEvent(damageType = DamageType.Combat, recipient = RecipientFilter.AnyPlayer),
        binding = TriggerBinding.SELF
    )

    /**
     * Whenever a creature you control deals combat damage to a player.
     * Uses ANY binding — fires for each creature you control that connects.
     */
    val CreatureYouControlDealsCombatDamageToPlayer: TriggerSpec = TriggerSpec(
        event = DealsDamageEvent(
            damageType = DamageType.Combat,
            recipient = RecipientFilter.AnyPlayer,
            sourceFilter = GameObjectFilter.Creature.youControl()
        ),
        binding = TriggerBinding.ANY
    )

    /**
     * When this creature deals combat damage to a creature.
     */
    val DealsCombatDamageToCreature: TriggerSpec = TriggerSpec(
        event = DealsDamageEvent(damageType = DamageType.Combat, recipient = RecipientFilter.AnyCreature),
        binding = TriggerBinding.SELF
    )

    /**
     * Whenever a creature dealt damage by this creature this turn dies.
     * Used for Soul Collector and similar cards.
     */
    val CreatureDealtDamageByThisDies: TriggerSpec = TriggerSpec(
        event = CreatureDealtDamageBySourceDiesEvent,
        binding = TriggerBinding.SELF
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
     * At the beginning of enchanted creature's controller's upkeep.
     * Used for auras that grant "At the beginning of your upkeep" to the enchanted creature.
     */
    val EnchantedCreatureControllerUpkeep: TriggerSpec = TriggerSpec(
        event = StepEvent(Step.UPKEEP, Player.You),
        binding = TriggerBinding.ATTACHED
    )

    /**
     * At the beginning of the end step of enchanted creature's controller.
     * Used for auras like Lingering Death that trigger on the enchanted creature's controller's end step.
     */
    val EnchantedCreatureControllerEndStep: TriggerSpec = TriggerSpec(
        event = StepEvent(Step.END, Player.You),
        binding = TriggerBinding.ATTACHED
    )

    /**
     * When this creature is turned face up.
     */
    val TurnedFaceUp: TriggerSpec = TriggerSpec(
        event = TurnFaceUpEvent,
        binding = TriggerBinding.SELF
    )

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
     * Whenever any player draws a card.
     */
    val AnyPlayerDraws: TriggerSpec = TriggerSpec(
        event = DrawEvent(Player.Each),
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

    // =========================================================================
    // Spell Triggers
    // =========================================================================

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
     * Whenever you cast a noncreature or [subtype] spell.
     * Uses OR logic: triggers on noncreature spells OR spells with the given subtype.
     */
    fun YouCastNoncreatureOrSubtype(subtype: Subtype): TriggerSpec = TriggerSpec(
        event = SpellCastEvent(
            spellFilter = GameObjectFilter.Noncreature or GameObjectFilter.Any.withSubtype(subtype),
            player = Player.You
        ),
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
     * Example: "Whenever you cast a Lizard spell" → YouCastSubtype(Subtype.LIZARD)
     */
    fun YouCastSubtype(subtype: Subtype): TriggerSpec = TriggerSpec(
        event = SpellCastEvent(spellFilter = GameObjectFilter.Any.withSubtype(subtype), player = Player.You),
        binding = TriggerBinding.ANY
    )

    /**
     * Whenever you cast a kicked spell.
     */
    val YouCastKickedSpell: TriggerSpec = TriggerSpec(
        event = SpellCastEvent(kicked = true, player = Player.You),
        binding = TriggerBinding.ANY
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

    // =========================================================================
    // Damage Triggers
    // =========================================================================

    /**
     * Whenever this creature is dealt damage.
     */
    val TakesDamage: TriggerSpec = TriggerSpec(
        event = DamageReceivedEvent(),
        binding = TriggerBinding.SELF
    )

    /**
     * Whenever the enchanted creature dies.
     * Used for auras like Demonic Vigor.
     */
    val EnchantedCreatureDies: TriggerSpec = TriggerSpec(
        event = ZoneChangeEvent(from = Zone.BATTLEFIELD, to = Zone.GRAVEYARD),
        binding = TriggerBinding.ATTACHED
    )

    /**
     * When the enchanted permanent leaves the battlefield.
     * Used for auras like Curator's Ward.
     */
    val EnchantedPermanentLeavesBattlefield: TriggerSpec = TriggerSpec(
        event = ZoneChangeEvent(from = Zone.BATTLEFIELD),
        binding = TriggerBinding.ATTACHED
    )

    /**
     * Whenever the enchanted creature is dealt damage.
     * Used for auras like Frozen Solid.
     */
    val EnchantedCreatureTakesDamage: TriggerSpec = TriggerSpec(
        event = DamageReceivedEvent(),
        binding = TriggerBinding.ATTACHED
    )

    /**
     * Whenever the enchanted creature deals combat damage to a player.
     * Used for auras like One with Nature.
     */
    val EnchantedCreatureDealsCombatDamageToPlayer: TriggerSpec = TriggerSpec(
        event = DealsDamageEvent(damageType = DamageType.Combat, recipient = RecipientFilter.AnyPlayer),
        binding = TriggerBinding.ATTACHED
    )

    /**
     * Whenever the enchanted creature attacks.
     * Used for auras like Extra Arms.
     */
    val EnchantedCreatureAttacks: TriggerSpec = TriggerSpec(
        event = AttackEvent(),
        binding = TriggerBinding.ATTACHED
    )

    /**
     * Whenever the equipped creature attacks.
     * Used for equipment like Heart-Piercer Bow.
     */
    val EquippedCreatureAttacks: TriggerSpec = TriggerSpec(
        event = AttackEvent(),
        binding = TriggerBinding.ATTACHED
    )

    /**
     * Whenever the equipped creature dies.
     * Used for equipment like Forebear's Blade.
     * Equipment stays on the battlefield; detected via aurasByTarget index
     * on the dying creature's zone change event.
     */
    val EquippedCreatureDies: TriggerSpec = TriggerSpec(
        event = ZoneChangeEvent(from = Zone.BATTLEFIELD, to = Zone.GRAVEYARD),
        binding = TriggerBinding.ATTACHED
    )

    /**
     * Whenever the enchanted creature deals damage (any type).
     * Used for auras like Guilty Conscience.
     */
    val EnchantedCreatureDealsDamage: TriggerSpec = TriggerSpec(
        event = DealsDamageEvent(),
        binding = TriggerBinding.ATTACHED
    )

    /**
     * When the enchanted creature is turned face up.
     * Used for auras like Fatal Mutation.
     */
    val EnchantedCreatureTurnedFaceUp: TriggerSpec = TriggerSpec(
        event = TurnFaceUpEvent,
        binding = TriggerBinding.ATTACHED
    )

    /**
     * Whenever a creature deals damage to this creature.
     */
    val DamagedByCreature: TriggerSpec = TriggerSpec(
        event = DamageReceivedEvent(source = SourceFilter.Creature),
        binding = TriggerBinding.SELF
    )

    /**
     * Whenever a spell deals damage to this creature.
     */
    val DamagedBySpell: TriggerSpec = TriggerSpec(
        event = DamageReceivedEvent(source = SourceFilter.Spell),
        binding = TriggerBinding.SELF
    )

    // =========================================================================
    // Tap/Untap Triggers
    // =========================================================================

    /**
     * Whenever this permanent becomes tapped.
     */
    val BecomesTapped: TriggerSpec = TriggerSpec(
        event = TapEvent,
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
     * When the enchanted permanent becomes tapped.
     * Used for auras like Uncontrolled Infestation.
     */
    val EnchantedPermanentBecomesTapped: TriggerSpec = TriggerSpec(
        event = TapEvent,
        binding = TriggerBinding.ATTACHED
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
     * Whenever you cycle a card.
     */
    val YouCycle: TriggerSpec = TriggerSpec(
        event = CycleEvent(Player.You),
        binding = TriggerBinding.ANY
    )

    /**
     * Whenever a player cycles a card.
     */
    val AnyPlayerCycles: TriggerSpec = TriggerSpec(
        event = CycleEvent(Player.Each),
        binding = TriggerBinding.ANY
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
     */
    fun CreatureYouControlBecomesTargetByOpponent(filter: GameObjectFilter = GameObjectFilter.Creature): TriggerSpec = TriggerSpec(
        event = BecomesTargetEvent(targetFilter = filter.youControl(), byOpponent = true),
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
}
