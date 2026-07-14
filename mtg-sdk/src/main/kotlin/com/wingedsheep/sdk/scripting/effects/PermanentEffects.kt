package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.text.TextReplacer
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =============================================================================
// Permanent State Transformation Effects
// (animation, morph, equipment, exile-on-leave)
// =============================================================================

/**
 * Target land becomes an X/Y creature until end of turn. It's still a land.
 * Used for Kamahl, Fist of Krosa: "{G}: Target land becomes a 1/1 creature until end of turn. It's still a land."
 *
 * Creates two floating effects:
 * 1. Layer.TYPE + AddType("Creature") - adds the Creature type
 * 2. Layer.POWER_TOUGHNESS + Sublayer.SET_VALUES + SetPowerToughness - sets base P/T
 *
 * @property target The land to animate
 * @property power The base power to set
 * @property toughness The base toughness to set
 * @property duration How long the effect lasts
 */
@SerialName("AnimateLand")
@Serializable
data class AnimateLandEffect(
    val target: EffectTarget,
    val power: Int = 1,
    val toughness: Int = 1,
    val duration: Duration = Duration.EndOfTurn
) : Effect {
    override val description: String = buildString {
        append("${target.description} becomes a $power/$toughness creature")
        if (duration.description.isNotEmpty()) append(" ${duration.description}")
        append(". It's still a land.")
    }
}

/**
 * Target permanent becomes a creature with specified characteristics until end of turn.
 * More general than AnimateLandEffect — can also remove types (e.g., Planeswalker),
 * grant keywords, set subtypes, and change color.
 *
 * Used for Sarkhan, the Dragonspeaker's +1: "becomes a legendary 4/4 red Dragon creature
 * with flying, indestructible, and haste."
 *
 * Creates floating effects across multiple layers:
 * - Layer.TYPE: AddType("CREATURE"), RemoveType for each removeType, SetCreatureSubtypes
 * - Layer.COLOR: ChangeColor if colors specified
 * - Layer.ABILITY: GrantKeyword for each keyword
 * - Layer.POWER_TOUGHNESS + Sublayer.SET_VALUES: SetPowerToughness
 *
 * @property target The permanent to animate
 * @property power The base power to set (a [DynamicAmount]; use `DynamicAmount.Fixed(n)` for a
 *   constant — Sarkhan's "4/4 Dragon" — or a live amount like `X plus 1` for Fractalize's Fractal).
 *   The amount is evaluated once when the effect resolves and stamped as a fixed base-P/T floating
 *   effect (CR 613.4c — the value is locked in when the effect starts applying), so it does not keep
 *   recomputing if the underlying amount changes later.
 * @property toughness The base toughness to set (a [DynamicAmount]; same evaluation as [power]).
 * @property keywords Keywords to grant (e.g., flying, indestructible, haste)
 * @property creatureTypes Creature subtypes to *set*, replacing all existing subtypes (e.g.,
 *   "Dragon"; "Fractal" loses all other creature types — Layer 4 set effect)
 * @property removeTypes Types to remove (e.g., "PLANESWALKER")
 * @property colors Colors to set, replacing all existing colors (null = keep existing). An explicit
 *   set loses all other colors (e.g. green+blue for a Fractal).
 * @property imageUri Optional *display-only* card-image override shown for the animated permanent
 *   while the effect lasts (e.g. a token's art for a creature that becomes that token type —
 *   Fractalize's Fractal). Purely cosmetic: it changes no characteristic, only what the client
 *   renders, and reverts together with the rest of the animate when [duration] expires. `null`
 *   keeps the permanent's own art.
 * @property duration How long the effect lasts
 */
@SerialName("BecomeCreature")
@Serializable
data class BecomeCreatureEffect(
    val target: EffectTarget = EffectTarget.Self,
    val power: DynamicAmount,
    val toughness: DynamicAmount,
    val keywords: Set<Keyword> = emptySet(),
    val creatureTypes: Set<String> = emptySet(),
    val removeTypes: Set<String> = emptySet(),
    /**
     * Additional card types to grant alongside CREATURE (added, not replaced — the permanent keeps
     * its existing types). Use for "becomes a 2/2 Assembly-Worker **artifact** creature. It's still
     * a land" (Mishra's Factory): `addTypes = setOf("ARTIFACT")`. The CREATURE type is always
     * granted; list any other type here.
     */
    val addTypes: Set<String> = emptySet(),
    val colors: Set<String>? = null,
    val imageUri: String? = null,
    val duration: Duration = Duration.EndOfTurn,
    /**
     * Optional dynamic base power. When non-null, the Layer 7b base-P/T floating effect uses
     * [SerializableModification.SetPowerToughnessDynamic] with this amount instead of the fixed
     * [power], recomputed continuously at projection. Use `DynamicAmount.EntityProperty(
     * EntityReference.AffectedEntity, EntityNumericProperty.ManaValue)` for "power equal to its
     * mana value" (Xenic Poltergeist). Both [dynamicPower] and [dynamicToughness] must be supplied
     * together; the fixed [power]/[toughness] then serve only as the rules-text display.
     */
    val dynamicPower: DynamicAmount? = null,
    /** Optional dynamic base toughness — see [dynamicPower]. */
    val dynamicToughness: DynamicAmount? = null
) : Effect {
    override val description: String = buildString {
        val ptText = if (dynamicPower != null && dynamicToughness != null) "*/*"
            else "${power.description}/${toughness.description}"
        append("${target.description} becomes a $ptText ")
        if (addTypes.isNotEmpty()) append("${addTypes.joinToString(" ") { it.lowercase() }} ")
        append("creature")
        if (creatureTypes.isNotEmpty()) append(" ${creatureTypes.joinToString("/")}")
        if (keywords.isNotEmpty()) append(" with ${keywords.joinToString(", ") { it.name.lowercase() }}")
        if (duration.description.isNotEmpty()) append(" ${duration.description}")
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newPower = power.applyTextReplacement(replacer)
        val newToughness = toughness.applyTextReplacement(replacer)
        return if (newPower !== power || newToughness !== toughness)
            copy(power = newPower, toughness = newToughness) else this
    }
}

/**
 * Target permanent becomes an artifact with a fixed set of card types and subtypes, optionally
 * losing all other card types and abilities and gaining a single activated ability — modeled as
 * continuous floating effects keyed to that entity (Layer 4 type/subtype, Layer 5 color, Layer 6
 * "lose all abilities"), plus a [com.wingedsheep.sdk.scripting.ActivatedAbility] grant.
 *
 * The general "becomes a Treasure/Food/Clue/artifact" transform — name the mechanic, not the
 * card. Vraska, the Silencer uses it to make a returned dead creature "a Treasure artifact with
 * '{T}, Sacrifice this artifact: Add one mana of any color', and it loses all other card types"
 * with [duration] = `Duration.Permanent`. Pair with a graveyard→battlefield return.
 *
 * Differs from [BecomeCreatureEffect] (which adds CREATURE + sets P/T): this *replaces* all card
 * types with [cardTypes] and all subtypes with [subtypes] (CR 613.4 Layer 4 set effects), so the
 * resulting permanent is exactly the named artifact, nothing more.
 *
 * @property target The permanent to transform
 * @property cardTypes Card types to set, replacing all existing ones (e.g. `setOf("ARTIFACT")`).
 *   `null` keeps the permanent's existing card types unchanged — used when only subtypes/abilities
 *   change (Ultima's blighted land "loses all land types and abilities" but stays a land and keeps
 *   any other card types such as artifact, per its ruling).
 * @property subtypes Subtypes to set, replacing all existing ones (e.g. `setOf("Treasure")`;
 *   `emptySet()` strips all subtypes — "loses all land types")
 * @property colors Colors to set (`emptySet()` = colorless, the default; `null` = keep existing)
 * @property loseAllAbilities Whether the permanent loses all printed/granted-via-projection abilities
 * @property grantedAbility A single activated ability the transformed permanent gains (e.g. the
 *   Treasure sac-for-mana ability). Granted via the durable granted-activated-ability record, so it
 *   survives [loseAllAbilities] (which only strips projected abilities).
 * @property duration How long the transform lasts (`Duration.Permanent` for an indefinite change
 *   that ends only when the permanent leaves the battlefield)
 */
@SerialName("BecomeArtifact")
@Serializable
data class BecomeArtifactEffect(
    val target: EffectTarget = EffectTarget.ContextTarget(0),
    val cardTypes: Set<String>? = setOf("ARTIFACT"),
    val subtypes: Set<String> = emptySet(),
    val colors: Set<com.wingedsheep.sdk.core.Color>? = emptySet(),
    val loseAllAbilities: Boolean = true,
    val grantedAbility: com.wingedsheep.sdk.scripting.ActivatedAbility? = null,
    val duration: Duration = Duration.Permanent
) : Effect {
    override val description: String = buildString {
        append("${target.description} becomes ")
        if (colors?.isEmpty() == true) append("a colorless ")
        if (subtypes.isNotEmpty()) append(subtypes.joinToString(" "))
        if (!cardTypes.isNullOrEmpty()) {
            if (subtypes.isNotEmpty()) append(" ")
            append(cardTypes.joinToString(" ") { it.lowercase() })
        }
        grantedAbility?.let { append(" with \"${it.description}\"") }
        if (loseAllAbilities) append(" and loses all other card types and abilities")
        if (duration.description.isNotEmpty()) append(" ${duration.description}")
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newAbility = grantedAbility?.applyTextReplacement(replacer)
        return if (newAbility !== grantedAbility) copy(grantedAbility = newAbility) else this
    }
}

/**
 * One-shot: animate every permanent matching [filter] into a creature for [duration], setting its
 * base power and toughness to [power]/[toughness] (each a [DynamicAmount] evaluated per affected
 * permanent — e.g. `EntityProperty(AffectedEntity, ManaValue)` for "P/T equal to its mana value")
 * and (optionally) stripping all of its abilities. Implemented as floating continuous effects keyed
 * to the matched set, captured once at resolution time (CR 611.2c — the set of affected permanents
 * is locked in):
 * - Layer 4 (TYPE): AddType("CREATURE")
 * - Layer 6 (ABILITY): RemoveAllAbilities, when [loseAllAbilities]
 * - Layer 7b (POWER_TOUGHNESS, SET_VALUES): base P/T = [power]/[toughness]
 *
 * This is the one-shot, fixed-set companion to expressing the same effect *continuously* through
 * [com.wingedsheep.sdk.scripting.GrantCardType] + [com.wingedsheep.sdk.scripting.LoseAllAbilities] +
 * [com.wingedsheep.sdk.scripting.SetBasePowerToughnessDynamicStatic] group statics on a permanent
 * (which take the same [DynamicAmount] P/T). Use the statics for the while-on-battlefield behavior;
 * use this effect for the "this effect continues until end of turn" linger when the permanent that
 * generated those statics leaves — Titania's Song: "Each noncreature artifact ... becomes an
 * artifact creature with power and toughness each equal to its mana value. If this enchantment
 * leaves the battlefield, this effect continues until end of turn." Reusable for any "all X become
 * creatures with P/T [formula] (and lose their abilities) until end of turn" effect — name the
 * mechanic, not the card.
 *
 * @property filter Which permanents to animate (evaluated against the battlefield at resolution)
 * @property power Base power each animated permanent is set to (per-entity dynamic value)
 * @property toughness Base toughness each animated permanent is set to (per-entity dynamic value)
 * @property loseAllAbilities Whether the animated permanents also lose all abilities
 * @property duration How long the animation lasts (defaults to end of turn)
 */
@SerialName("MassAnimate")
@Serializable
data class MassAnimateEffect(
    val filter: GameObjectFilter,
    val power: DynamicAmount,
    val toughness: DynamicAmount,
    val loseAllAbilities: Boolean = true,
    val duration: Duration = Duration.EndOfTurn
) : Effect {
    override val description: String = buildString {
        append("Each ${filter.description}")
        if (loseAllAbilities) append(" loses all abilities and")
        append(" becomes a creature with power and toughness set to ${power.description}/${toughness.description}")
        if (duration.description.isNotEmpty()) append(" ${duration.description}")
    }
    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}

/**
 * Target permanent becomes saddled until end of turn (CR 702.171b). This is the resolving
 * effect of a Saddle ability: it stamps a transient "saddled" marker on the permanent (the
 * engine's `SaddledComponent`), which Mount payoffs read via `Conditions.SourceIsSaddled` /
 * `StatePredicate.IsSaddled`. The marker is engine state, not a copiable value, and is cleared
 * at end-of-turn cleanup or when the permanent leaves the battlefield.
 *
 * Defaults to [EffectTarget.Self] because a Saddle ability always saddles its own source.
 *
 * @property target The permanent to mark as saddled
 */
@SerialName("BecomeSaddled")
@Serializable
data class BecomeSaddledEffect(
    val target: EffectTarget = EffectTarget.Self
) : Effect {
    override val description: String = "${target.description} becomes saddled until end of turn"
}

/**
 * Make [target] become prepared (Secrets of Strixhaven). The target must be a permanent whose
 * card has the [com.wingedsheep.sdk.model.CardLayout.PREPARE] layout. Becoming prepared creates a
 * copy of its prepare spell in the controller's exile that may be cast (paying that spell's cost);
 * casting that copy unprepares the creature. A creature that is already prepared does not
 * re-prepare. Used by Leech Collector ("Whenever you gain life for the first time each turn, this
 * creature becomes prepared.").
 */
@SerialName("BecomePrepared")
@Serializable
data class BecomePreparedEffect(
    val target: EffectTarget = EffectTarget.Self
) : Effect {
    override val description: String = "${target.description} becomes prepared"
}

/**
 * Make [target] become unprepared (Secrets of Strixhaven) — the inverse of [BecomePreparedEffect].
 * Strips the target's prepared status: removes its `PreparedComponent` and the cast-from-exile
 * permission for its exile prepare-spell copy (the orphaned copy is then swept by the phantom-copy
 * state-based action). A creature that isn't prepared is unaffected. Used by Biblioplex Tomekeeper
 * ("Target creature becomes unprepared.").
 */
@SerialName("Unprepare")
@Serializable
data class UnprepareEffect(
    val target: EffectTarget = EffectTarget.Self
) : Effect {
    override val description: String = "${target.description} becomes unprepared"
}

/**
 * Turn target creature face down.
 * "Turn target creature with a morph ability face down."
 * Used for Backslide and similar effects.
 */
@SerialName("TurnFaceDown")
@Serializable
data class TurnFaceDownEffect(
    val target: EffectTarget = EffectTarget.ContextTarget(0)
) : Effect {
    override val description: String = "Turn ${target.description} face down"
}

/**
 * Turn target face-down creature face up.
 * "Turn target face-down creature an opponent controls face up."
 * Used for Break Open and similar effects.
 */
@SerialName("TurnFaceUp")
@Serializable
data class TurnFaceUpEffect(
    val target: EffectTarget = EffectTarget.ContextTarget(0)
) : Effect {
    override val description: String = "Turn ${target.description} face up"
}

/**
 * Reveal a face-down permanent: make the identity of the card under a face-down permanent
 * public (CR 708 — a face-down permanent's hidden card characteristics are shown to all players).
 * This is purely informational — it does not turn the permanent face up and does not change its
 * game state; it emits a reveal so opponents learn what the permanent is.
 *
 * Models "Reveal target face-down permanent" (Hauntwoods Shrieker). Pair with a
 * [com.wingedsheep.sdk.scripting.conditions.TargetIsCreatureCard] gate + an optional
 * [TurnFaceUpEffect] to express "...If it's a creature card, you may turn it face up." If the
 * target is somehow no longer face down by resolution, the reveal is a no-op.
 *
 * @property target The face-down permanent to reveal (default: this effect's first target).
 */
@SerialName("RevealFaceDownPermanent")
@Serializable
data class RevealFaceDownPermanentEffect(
    val target: EffectTarget = EffectTarget.ContextTarget(0)
) : Effect {
    override val description: String = "Reveal ${target.description}"
}

/**
 * Attach this equipment to a target creature.
 * Detaches from the currently equipped creature (if any) before attaching to the new one.
 * "Attach to target creature you control."
 *
 * @property target The creature to attach to
 */
@SerialName("AttachEquipment")
@Serializable
data class AttachEquipmentEffect(
    val target: EffectTarget = EffectTarget.ContextTarget(0)
) : Effect {
    override val description: String = "Attach this equipment to ${target.description}"
}

/**
 * Attach a targeted Equipment to a targeted creature.
 * Unlike [AttachEquipmentEffect] which uses the source as the equipment,
 * this effect uses two explicit targets — one for the Equipment, one for the creature.
 * Used for Blacksmith's Talent Level 2: "attach target Equipment you control to up to one
 * target creature you control."
 *
 * @property equipmentTarget The Equipment to attach (e.g., ContextTarget(0))
 * @property creatureTarget The creature to attach it to (e.g., ContextTarget(1))
 */
@SerialName("AttachTargetEquipmentToCreature")
@Serializable
data class AttachTargetEquipmentToCreatureEffect(
    val equipmentTarget: EffectTarget = EffectTarget.ContextTarget(0),
    val creatureTarget: EffectTarget = EffectTarget.ContextTarget(1)
) : Effect {
    override val description: String = "Attach ${equipmentTarget.description} to ${creatureTarget.description}"
}

/**
 * Unattach an Aura/Equipment from its host without moving it to another zone (CR 701.3d). Removes
 * the [target] attachment's `AttachedToComponent` and drops it from the host's attachment list; a
 * no-op if [target] isn't currently attached to anything. The inverse of [AttachEquipmentEffect] /
 * [AttachTargetEquipmentToCreatureEffect] — used for "unattach it" riders such as Stolen Uniform's
 * reflexive "when you lose control of that Equipment, … unattach it".
 *
 * @property target The attachment to unattach (e.g. [EffectTarget.TriggeringEntity] for "that
 *   Equipment" inside a delayed trigger, or [EffectTarget.Self] when a permanent unattaches itself).
 */
@SerialName("UnattachEquipment")
@Serializable
data class UnattachEquipmentEffect(
    val target: EffectTarget = EffectTarget.Self
) : Effect {
    override val description: String = "Unattach ${target.description}"
}

/**
 * Put a targeted Aura or Equipment card onto the battlefield **attached to a permanent the
 * effect's controller chooses** at resolution. The card is the [target] (e.g. a targeted
 * Aura/Equipment in a graveyard); the host is chosen as the effect resolves and is therefore
 * NOT a target — only the host filter constrains it ([hostFilter], default "a creature you
 * control"). Models "Return target Aura or Equipment card from your graveyard to the
 * battlefield attached to a creature you control" (One Last Job, Brass Squire-shaped attaches).
 *
 * Unlike the [com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect] aura auto-attach
 * (Rule 303.4f), which is Aura-only and uses the Aura's own enchant target, this effect
 * works for both Auras and Equipment and lets the card restrict the host. Per the One Last
 * Job ruling, the Aura/Equipment must be legally attachable to the chosen host:
 *  - If a legal host exists, the controller chooses one and the card enters attached to it.
 *  - If no legal host exists, an Equipment enters the battlefield unattached, while an Aura
 *    can't enter (it stays in its current zone — Rule 303.4g).
 *
 * @property target The Aura or Equipment card to put onto the battlefield (e.g. a graveyard target).
 * @property hostFilter Which permanents are eligible hosts (default: a creature you control).
 */
@SerialName("PutOntoBattlefieldAttachedToChosen")
@Serializable
data class PutOntoBattlefieldAttachedToChosenEffect(
    val target: EffectTarget = EffectTarget.ContextTarget(0),
    val hostFilter: GameObjectFilter = GameObjectFilter.Creature.youControl()
) : Effect {
    override val description: String =
        "Put ${target.description} onto the battlefield attached to ${hostFilter.description}"
}

/**
 * Mark a permanent so that if it would leave the battlefield, it is exiled instead.
 * Used by Kheru Lich Lord, Whip of Erebos, Sneak Attack, and similar reanimation effects.
 *
 * @property target The permanent to mark
 */
@SerialName("GrantExileOnLeave")
@Serializable
data class GrantExileOnLeaveEffect(
    val target: EffectTarget = EffectTarget.ContextTarget(0)
) : Effect {
    override val description: String =
        "If ${target.description} would leave the battlefield, exile it instead of putting it anywhere else"
}

// =============================================================================
// Ability Resolution Tracking
// =============================================================================

/**
 * Increments the ability resolution count on the source permanent.
 * Used for cards that track "the Nth time this ability has resolved this turn"
 * (e.g., Harvestrite Host).
 */
@SerialName("IncrementAbilityResolutionCount")
@Serializable
data object IncrementAbilityResolutionCountEffect : Effect {
    override val description: String = "Track ability resolution"
}

/**
 * Stamps the source permanent as having returned via its Enduring ability (Duskmourn Glimmer
 * cycle). Composed after a return-to-battlefield move so the card's
 * [com.wingedsheep.sdk.scripting.ConditionalStaticAbility] (gated on
 * [com.wingedsheep.sdk.scripting.conditions.SourceReturnedAsEnchantment]) makes the returned
 * permanent an enchantment with no other card types or subtypes. No-op if the source is no
 * longer on the battlefield.
 */
@SerialName("MarkEnduringReturn")
@Serializable
data object MarkEnduringReturnEffect : Effect {
    override val description: String = "It's an enchantment. It's not a creature."
}

/**
 * Records the card most recently selected from the source's linked-exile pile as its "last chosen
 * card", stamping a `ChosenLinkedExileComponent` on the source. Reads the *first* entry of the
 * pipeline collection named [from] (populated by a preceding
 * [com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect]) and remembers that entity id.
 *
 * Backs the choose-from-your-exile step of Koh, the Face Stealer ("Pay 1 life: Choose a creature
 * card exiled with Koh"): a later [com.wingedsheep.sdk.scripting.HasAbilitiesOfChosenLinkedExiledCard]
 * static ability reads that stamp to grant the source the chosen card's activated and triggered
 * abilities. No-op if the source has left the battlefield or the collection is empty.
 *
 * @property from Name of the pipeline collection holding the selected card (its first element is used).
 */
@SerialName("RecordChosenLinkedExile")
@Serializable
data class RecordChosenLinkedExileEffect(
    val from: String,
) : Effect {
    override val description: String = "Note the chosen card"
}

// =============================================================================
// Explore
// =============================================================================

/**
 * Target creature explores.
 *
 * "Reveal the top card of your library. If it's a land card, put it into your hand.
 * Otherwise, put a +1/+1 counter on this creature, then put the card back on top of
 * your library or put it into your graveyard."
 *
 * The exploring player is the controller of the effect (the Map token's controller).
 * The exploring creature is [target].
 *
 * @property target The creature that is exploring.
 */
@SerialName("Explore")
@Serializable
data class ExploreEffect(
    val target: EffectTarget = EffectTarget.ContextTarget(0)
) : Effect {
    override val description: String = "${target.description} explores"
}

/**
 * Tail marker that emits the "a permanent explored" event (CR 701.44), mirroring
 * [EmitSurveiledEventEffect]. Deferred to the very end of the explore so the event lands in a
 * *completed* resolution batch — the nonland branch of an explore pauses for the top/graveyard
 * choice, and a game event emitted in that paused batch does not reliably fire watcher triggers.
 * The land and empty-library branches emit the event inline (they don't pause), so this marker is
 * appended only to the nonland branch's post-decision effects.
 *
 * @property target The permanent that explored (the trigger's subject).
 * @property revealedCardWasLand `true` land / `false` nonland / `null` no reveal (empty library).
 */
@SerialName("EmitExploredEvent")
@Serializable
data class EmitExploredEventEffect(
    val target: EffectTarget = EffectTarget.Self,
    val revealedCardWasLand: Boolean? = null
) : Effect {
    override val description: String = "${target.description} explored"
}
