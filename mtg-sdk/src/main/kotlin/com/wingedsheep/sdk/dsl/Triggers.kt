package com.wingedsheep.sdk.dsl

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
import com.wingedsheep.sdk.scripting.events.SpellTypeFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
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
        event = ZoneChangeEvent(from = Zone.BATTLEFIELD, to = Zone.GRAVEYARD),
        binding = TriggerBinding.ANY
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
        event = AttackEvent,
        binding = TriggerBinding.SELF
    )

    /**
     * When any creature attacks.
     */
    val AnyAttacks: TriggerSpec = TriggerSpec(
        event = AttackEvent,
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
     * When this creature blocks.
     */
    val Blocks: TriggerSpec = TriggerSpec(
        event = BlockEvent,
        binding = TriggerBinding.SELF
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
     * At the beginning of enchanted creature's controller's upkeep.
     * Used for auras that grant "At the beginning of your upkeep" to the enchanted creature.
     */
    val EnchantedCreatureControllerUpkeep: TriggerSpec = TriggerSpec(
        event = EnchantedCreatureControllerStepEvent(Step.UPKEEP),
        binding = TriggerBinding.ANY
    )

    /**
     * At the beginning of the end step of enchanted creature's controller.
     * Used for auras like Lingering Death that trigger on the enchanted creature's controller's end step.
     */
    val EnchantedCreatureControllerEndStep: TriggerSpec = TriggerSpec(
        event = EnchantedCreatureControllerStepEvent(Step.END),
        binding = TriggerBinding.ANY
    )

    /**
     * When this creature is turned face up.
     */
    val TurnedFaceUp: TriggerSpec = TriggerSpec(
        event = TurnFaceUpEvent,
        binding = TriggerBinding.SELF
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
        event = SpellCastEvent(spellType = SpellTypeFilter.CREATURE, player = Player.You),
        binding = TriggerBinding.ANY
    )

    /**
     * Whenever you cast a noncreature spell.
     */
    val YouCastNoncreature: TriggerSpec = TriggerSpec(
        event = SpellCastEvent(spellType = SpellTypeFilter.NONCREATURE, player = Player.You),
        binding = TriggerBinding.ANY
    )

    /**
     * Whenever you cast an instant or sorcery.
     */
    val YouCastInstantOrSorcery: TriggerSpec = TriggerSpec(
        event = SpellCastEvent(spellType = SpellTypeFilter.INSTANT_OR_SORCERY, player = Player.You),
        binding = TriggerBinding.ANY
    )

    /**
     * Whenever you cast an enchantment spell.
     */
    val YouCastEnchantment: TriggerSpec = TriggerSpec(
        event = SpellCastEvent(spellType = SpellTypeFilter.ENCHANTMENT, player = Player.You),
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
     * Whenever the enchanted creature is dealt damage.
     * Used for auras like Frozen Solid.
     */
    val EnchantedCreatureTakesDamage: TriggerSpec = TriggerSpec(
        event = EnchantedCreatureDamageReceivedEvent,
        binding = TriggerBinding.ANY
    )

    /**
     * Whenever the enchanted creature deals combat damage to a player.
     * Used for auras like One with Nature.
     */
    val EnchantedCreatureDealsCombatDamageToPlayer: TriggerSpec = TriggerSpec(
        event = EnchantedCreatureDealsCombatDamageToPlayerEvent,
        binding = TriggerBinding.ANY
    )

    /**
     * Whenever the enchanted creature attacks.
     * Used for auras like Extra Arms.
     */
    val EnchantedCreatureAttacks: TriggerSpec = TriggerSpec(
        event = EnchantedCreatureAttacksEvent,
        binding = TriggerBinding.ANY
    )

    /**
     * Whenever the enchanted creature deals damage (any type).
     * Used for auras like Guilty Conscience.
     */
    val EnchantedCreatureDealsDamage: TriggerSpec = TriggerSpec(
        event = EnchantedCreatureDealsDamageEvent,
        binding = TriggerBinding.ANY
    )

    /**
     * When the enchanted creature is turned face up.
     * Used for auras like Fatal Mutation.
     */
    val EnchantedCreatureTurnedFaceUp: TriggerSpec = TriggerSpec(
        event = EnchantedCreatureTurnedFaceUpEvent,
        binding = TriggerBinding.ANY
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
        event = EnchantedPermanentBecomesTappedEvent,
        binding = TriggerBinding.ANY
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
}
