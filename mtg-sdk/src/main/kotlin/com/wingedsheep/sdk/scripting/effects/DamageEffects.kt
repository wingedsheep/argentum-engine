package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.text.TextReplacer
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =============================================================================
// Damage Effects
// =============================================================================

/**
 * Deal damage to a target.
 * Supports both fixed amounts and dynamic amounts (e.g., X value, creature count).
 *
 * Examples:
 * - Lightning Bolt: DealDamageEffect(3, target)
 * - Blaze: DealDamageEffect(DynamicAmount.XValue, target)
 * - Final Strike: DealDamageEffect(DynamicAmounts.sacrificedPower(), target)
 */
@SerialName("DealDamage")
@Serializable
data class DealDamageEffect(
    val amount: DynamicAmount,
    val target: EffectTarget,
    val cantBePrevented: Boolean = false,
    /**
     * Optional override for the damage source. When null the engine attributes the damage to the
     * resolving spell/ability's source (the trigger's source for triggered abilities, the spell
     * itself for instants/sorceries). For LTB triggers on tokens, the source has already been
     * SBA-swept (CR 704.5d) by the time the trigger resolves — the engine reads it via
     * last-known-information, so leaving this null on a token's LTB damage clause works
     * (e.g. Munitions' "When this token leaves the battlefield, it deals 2 damage to any target").
     */
    val damageSource: EffectTarget? = null,
    /**
     * When true and the target is a creature, damage in excess of lethal (CR 120.4a) is dealt to
     * that creature's controller instead (Gandalf's Sanction).
     */
    val excessToController: Boolean = false,
    /**
     * When set, the excess damage (CR 120.4a) this effect deals to its single permanent target —
     * above lethal for a creature, above loyalty for a planeswalker, above defense for a battle —
     * is stored into this pipeline number variable for a following effect to read via
     * `DynamicAmount.VariableReference`. 0 when no excess was dealt (including when the damage was
     * prevented or the target is gone). Violent Echoes: "If excess damage was dealt to that
     * permanent this way, empower Jace X, where X is that excess damage."
     */
    val excessDamageVariable: String? = null
) : Effect {
    /** Convenience constructor for fixed amounts */
    constructor(amount: Int, target: EffectTarget, cantBePrevented: Boolean = false, damageSource: EffectTarget? = null)
        : this(DynamicAmount.Fixed(amount), target, cantBePrevented, damageSource)

    override val description: String = buildString {
        if (damageSource != null) {
            append("${damageSource.description} deals ${amount.description} damage to ${target.description}")
        } else {
            append("Deal ${amount.description} damage to ${target.description}")
        }
        if (cantBePrevented) append(". This damage can't be prevented")
    }

    override fun runtimeDescription(resolver: (DynamicAmount) -> Int?): String = buildString {
        // Undeterminable amount ("damage equal to target's power", pre-choice) reads by name.
        val resolved: String = resolver(amount)?.toString() ?: amount.description
        if (damageSource != null) {
            append("${damageSource.description} deals $resolved damage to ${target.description}")
        } else {
            append("Deal $resolved damage to ${target.description}")
        }
        if (cantBePrevented) append(". This damage can't be prevented")
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newAmount = amount.applyTextReplacement(replacer)
        return if (newAmount !== amount) copy(amount = newAmount) else this
    }
}

/**
 * Install a turn-duration (until end of turn) replacement effect that adds [bonus] to every damage
 * instance matching [appliesTo] for the rest of the turn (CR 616 — a damage-amount replacement:
 * "it deals that much damage plus N instead").
 *
 * [appliesTo] carries the whole scope — `source`, `recipient`, `damageType` and `amount` — matched by
 * the same shared matchers as [com.wingedsheep.sdk.scripting.ModifyDamageAmount], with the effect's
 * controller as "you" (the source that installed it answers "this permanent"). The bonus is resolved
 * once at resolution (typically [DynamicAmount.XValue] from an `{X}` cost) and baked into the floating
 * effect, so it reads the same amount for every damage instance the rest of the turn and outlives the
 * source that created it (CR 611.2c). Multiple installs stack — each adds its own bonus.
 *
 * - Taii Wakeen, Perfect Shot: "{X}, {T}: If a source you control would deal noncombat damage to a
 *   permanent or player this turn, it deals that much damage plus X instead." —
 *   `appliesTo = DamageEvent(source = Any.youControl(), damageType = NonCombat)`.
 * - Rankle and Torbran: "If a source would deal damage to a player or battle this turn, it deals that
 *   much damage plus 2 instead." — `appliesTo = DamageEvent(recipient = Recipient.AnyPlayerOrBattle)`.
 *
 * A floating effect rather than the permanent-hosted [com.wingedsheep.sdk.scripting.ModifyDamageAmount]
 * because it is turn-duration and bakes its bonus at resolution.
 */
@SerialName("AmplifyDamageThisTurn")
@Serializable
data class AmplifyDamageThisTurnEffect(
    val bonus: DynamicAmount,
    val appliesTo: EventPattern.DamageEvent,
) : Effect {
    override val description: String = describe(bonus.description)

    override fun runtimeDescription(resolver: (DynamicAmount) -> Int?): String =
        describe(resolver(bonus)?.toString() ?: bonus.description)

    private fun describe(amount: String): String =
        "Until end of turn, if ${appliesTo.description}, it's that much damage plus $amount instead"

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newBonus = bonus.applyTextReplacement(replacer)
        return if (newBonus !== bonus) copy(bonus = newBonus) else this
    }
}

/**
 * Install a duration-bounded replacement effect that doubles *all* damage — from any source, combat
 * or noncombat — that would be dealt to a chosen player and to any permanent that player controls
 * (CR 616, a damage-amount replacement). The affected player is resolved once at resolution from
 * [target] (a player reference) and baked into a floating effect scoped to that player, so it keeps
 * doubling every damage instance to them and their permanents for the whole [duration], even if the
 * source that installed it has since left the battlefield.
 *
 * Lightning, Army of One — "Stagger — Whenever Lightning deals combat damage to a player, until your
 * next turn, if a source would deal damage to that player or a permanent that player controls, it
 * deals double that damage instead." ([target] = [Player.TriggeringPlayer], the just-damaged player;
 * [duration] = [Duration.UntilYourNextTurn].)
 *
 * Modelled as its own effect rather than the permanent-tied static [com.wingedsheep.sdk.scripting.DoubleDamage]
 * replacement because it (a) is created by a resolving ability and outlives its source, (b) is
 * duration-bounded, and (c) is scoped to a *specific* player captured at resolution — not "you" / "an
 * opponent" relative to a battlefield host. Read directly during damage resolution by
 * `DamageUtils.applyStaticDamageAmplification`, which runs for both combat (per already-assigned
 * recipient, so trample/assignment happens before doubling) and noncombat damage.
 */
@SerialName("DoubleDamageToPlayer")
@Serializable
data class DoubleDamageToPlayerEffect(
    val target: EffectTarget,
    val duration: Duration = Duration.UntilYourNextTurn
) : Effect {
    override val description: String = buildString {
        if (duration.description.isNotEmpty()) {
            append(duration.description.replaceFirstChar { it.uppercase() })
            append(", ")
        }
        append("if a source would deal damage to ${target.description} or a permanent that player ")
        append("controls, it deals double that damage instead")
    }
}

/**
 * Turn-scoped "Damage can't be prevented this turn" (CR 615.6 — a prevention effect can't apply
 * to damage that can't be prevented). While active for the current turn, all damage-prevention
 * shields, prevention/replacement-of-damage effects, and protection's damage-prevention clause are
 * ignored when damage is dealt.
 *
 * Unlike the static [com.wingedsheep.sdk.scripting.DamageCantBePrevented] replacement effect (which
 * lives on a battlefield permanent like Sunspine Lynx), this is a one-shot effect a spell or ability
 * can apply for the rest of the turn — e.g. Fear, Fire, Foes!: "Damage can't be prevented this turn."
 * The engine sets a turn-scoped flag on the game state that is cleared at the next turn boundary.
 */
@SerialName("DamageCantBePreventedThisTurn")
@Serializable
data object DamageCantBePreventedThisTurnEffect : Effect {
    override val description: String = "Damage can't be prevented this turn"
}

/**
 * "Damage that would be dealt to [target] this turn can't be prevented or dealt instead to another
 * permanent or player." — the *per-recipient* form of [DamageCantBePreventedThisTurnEffect]
 * (Whippoorwill).
 *
 * Both halves of the printed clause come from one marker on the recipient: prevention shields,
 * prevention replacements and protection's prevention clause stop applying to damage aimed at it,
 * and redirection ("dealt instead to another permanent or player") is skipped for it too.
 *
 * Scoped rather than global on purpose — the global effect would blank every prevention effect in
 * the game for the turn, which is a very different card.
 */
@SerialName("DamageToTargetCantBePreventedThisTurn")
@Serializable
data class DamageToTargetCantBePreventedThisTurnEffect(
    val target: EffectTarget = EffectTarget.ContextTarget(0)
) : Effect {
    override val description: String =
        "Damage that would be dealt to ${target.description} this turn can't be prevented or " +
            "dealt instead to another permanent or player"
}

/**
 * Deal damage to multiple targets, dividing the total as you choose.
 * Used for cards like Forked Lightning ("4 damage divided among 1-3 targets").
 *
 * [totalDamage] is the fixed total. [dynamicTotal], when non-null, is evaluated at resolution and
 * overrides [totalDamage] — used by effects whose total is computed when they resolve (Ureni, the
 * Song Unending: "X damage divided as you choose ..., where X is the number of lands you control").
 * The set of targets among which the total is divided comes from the ability's target requirement
 * (e.g. `TargetObject(unlimited = true, filter = CreatureOrPlaneswalker opponent controls)`), so
 * "any number of target" forms divide the total only among the targets actually chosen.
 */
@SerialName("DividedDamage")
@Serializable
data class DividedDamageEffect(
    val totalDamage: Int,
    val minTargets: Int = 1,
    val maxTargets: Int = 3,
    val dynamicTotal: com.wingedsheep.sdk.scripting.values.DynamicAmount? = null
) : Effect {
    override val description: String =
        if (dynamicTotal != null)
            "Deal damage equal to ${dynamicTotal.description} divided as you choose among the targets"
        else
            "Deal $totalDamage damage divided as you choose among $minTargets to $maxTargets target creatures"
}

/**
 * [damageSource] deals [amount] damage divided among the permanents in pipeline collection
 * [collectionName] — **not targets** — with the division made at resolution by [chooser].
 *
 * The non-targeted sibling of [DividedDamageEffect]: because nothing in the collection is
 * targeted, nothing is announced on the stack (CR 601.2d covers only targets), so the split is
 * chosen as the effect resolves (CR 608.2d). Only collection members still on the battlefield are
 * eligible; "among any number of those" means a member may be left out (`minPerTarget = 0`), but
 * the whole amount is dealt while any member remains. With exactly one eligible member it takes all
 * of it and nothing is asked. A missing [damageSource] or a non-positive [amount] deals nothing.
 *
 * Master of the Wild Hunt: "That creature deals damage equal to its power divided as its controller
 * chooses among any number of those Wolves" =
 * `DistributeDamageAmongCollection(powerOf(target), wolves, damageSource = target,
 * chooser = Chooser.ControllerOfTarget)`.
 */
@SerialName("DistributeDamageAmongCollection")
@Serializable
data class DistributeDamageAmongCollectionEffect(
    val amount: DynamicAmount,
    val collectionName: String,
    val damageSource: EffectTarget,
    val chooser: Chooser = Chooser.Controller
) : Effect {
    override val description: String =
        "${damageSource.description} deals damage equal to ${amount.description} divided among those permanents"
}

/**
 * Two creatures fight — each deals damage equal to its power to the other.
 * Used for fight abilities like Contested Cliffs and the fight keyword action.
 *
 * @property target1 First creature (e.g., Beast you control)
 * @property target2 Second creature (e.g., creature opponent controls)
 */
@SerialName("Fight")
@Serializable
data class FightEffect(
    val target1: EffectTarget,
    val target2: EffectTarget,
    /**
     * When set, the executor stores the *excess* damage (CR 120.4a — damage past lethal,
     * deathtouch-aware) that [target1] dealt to [target2] into this pipeline number variable,
     * so a following effect can read it via `DynamicAmount.VariableReference`. Only the damage
     * dealt **to [target2]** is captured (the "creature an opponent controls" half of the fight),
     * not the excess dealt back to [target1]. `null` (the default) records nothing — every
     * ordinary fight card leaves it unset. Built for The Last Agni Kai: "If the creature the
     * opponent controls is dealt excess damage this way, add that much {R}."
     */
    val excessDamageVariable: String? = null
) : Effect {
    override val description: String = "${target1.description} fights ${target2.description}"
}

/**
 * Deal damage to a target for each entity from a tracked collection that is still in a given zone.
 * Used for Dragonhawk-style "deal 2 damage to each opponent for each of those cards that are still exiled."
 *
 * At definition time, [collectionName] references a pipeline collection (e.g., "exiledCards").
 * When a delayed trigger is created, [CreateDelayedTriggerExecutor] resolves the collection
 * to concrete [entityIds] so the delayed trigger can check zone membership without the original context.
 *
 * @property entityIds Concrete entity IDs to check (populated at delayed trigger creation time)
 * @property collectionName Pipeline collection name to resolve into entityIds (used at definition time)
 * @property zone The zone to check for remaining entities
 * @property damagePerEntity Damage dealt per entity still in the zone
 * @property target Who receives the damage (e.g., PlayerRef(Player.EachOpponent))
 * @property damageSource Optional override for the damage source
 */
@SerialName("DealDamagePerEntityInZone")
@Serializable
data class DealDamagePerEntityInZoneEffect(
    val entityIds: List<EntityId> = emptyList(),
    val collectionName: String? = null,
    val zone: Zone = Zone.EXILE,
    val damagePerEntity: Int = 1,
    val target: EffectTarget = EffectTarget.PlayerRef(Player.EachOpponent),
    val damageSource: EffectTarget? = null
) : Effect {
    override val description: String =
        "Deal $damagePerEntity damage to ${target.description} for each card still in ${zone.name.lowercase()}"
}
