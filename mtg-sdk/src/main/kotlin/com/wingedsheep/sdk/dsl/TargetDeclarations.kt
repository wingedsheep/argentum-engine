package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetChooser
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.targets.TargetRequirement
import com.wingedsheep.sdk.scripting.targets.withId
import com.wingedsheep.sdk.scripting.targets.withOptional
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * The one way a card declares a target: every builder that can hold targets — a spell, an
 * activated / triggered / loyalty ability, a saga chapter, a modal mode — implements this, so the
 * spelling is the same everywhere.
 *
 * ```kotlin
 * spell {
 *     val t = target(TargetFilter(GameObjectFilter.Artifact or GameObjectFilter.Creature.tapped()))
 *     effect = Effects.Destroy(t) then Effects.GainLife(3)
 * }
 * ```
 *
 * - **An object target** — a permanent, a card in a zone, a spell or ability on the stack — is
 *   declared by its [TargetFilter]: `target(TargetFilter.Creature.youControl().other())`. The
 *   filter's zone says where to look; the parameters say how many.
 * - **Any other shape** — players, "any target", "creature or planeswalker" — is declared by its
 *   [Targets] preset: `target(Targets.Player)`, `target(Targets.Any)`.
 *
 * The handle a declaration returns is how effects read the chosen target. Its binding id is minted
 * here, unique across the card, so authors never name a target: the targeting prompt the player sees is
 * derived from the requirement itself ([TargetRequirement.description]), never from a name.
 */
interface TargetDeclarations {

    /** Record [requirement] and return the id its chosen target binds under. */
    fun declareTarget(requirement: TargetRequirement): String

    /** Declare one target of any shape — `target(Targets.Player)`, `target(Targets.Any)`. */
    fun target(requirement: TargetRequirement): EffectTarget.BoundVariable =
        EffectTarget.BoundVariable(
            declareTarget(requirement),
            requirement.description.takeIf { requirement.count == 1 && !requirement.unlimited }
        )

    /**
     * Declare one target of any shape that may be left unchosen — `target(Targets.CreatureOrPlaneswalker,
     * optional = true)` is "up to one target creature or planeswalker".
     */
    fun target(requirement: TargetRequirement, optional: Boolean): EffectTarget.BoundVariable =
        target(if (optional) requirement.withOptional() else requirement)

    /**
     * Declare one object target — "target creature you control" is
     * `target(TargetFilter.CreatureYouControl)`; `optional` makes it "up to one".
     */
    fun target(
        filter: TargetFilter,
        optional: Boolean = false,
        chooser: TargetChooser = TargetChooser.Controller,
    ): EffectTarget.BoundVariable =
        target(TargetObject(filter = filter, optional = optional, chooser = chooser))

    /**
     * Declare a multi-target requirement of any shape and get one handle per chosen target:
     * `val (first, second) = targets(Targets.Player.withCount(2))`.
     */
    fun targets(requirement: TargetRequirement): List<EffectTarget.BoundVariable> {
        val id = declareTarget(requirement)
        return (0 until requirement.count).map { i -> EffectTarget.BoundVariable("$id[$i]") }
    }

    /**
     * Declare several object targets under one requirement — "up to two target creatures" is
     * `targets(TargetFilter.Creature, count = 2, optional = true)`. Returns one handle per slot;
     * an ability that treats the targets uniformly ignores them and uses `Effects.ForEachTarget`.
     *
     * The cross-target constraints are [TargetObject]'s: see its fields for what each enforces.
     *
     * A count only known at cast or trigger time takes one of two spellings, and the Oracle text
     * says which: "**up to** X target creatures" is `optional = true, dynamicMaxCount = X`, and
     * "X target creatures" — exactly X, no fewer — is `exactly = X`
     * (`targets(TargetFilter.Creature, exactly = DynamicAmounts.xValue())`, Icy Blast). `exactly`
     * stands alone: it is the whole count, so it can't be combined with `dynamicMaxCount`,
     * `optional` or `unlimited`.
     */
    fun targets(
        filter: TargetFilter,
        count: Int = 1,
        minCount: Int = count,
        optional: Boolean = false,
        unlimited: Boolean = false,
        dynamicMaxCount: DynamicAmount? = null,
        exactly: DynamicAmount? = null,
        sameController: Boolean = false,
        sameOwner: Boolean = false,
        sameCreatureType: Boolean = false,
        sameCardType: Boolean = false,
        totalManaValueAtMost: DynamicAmount? = null,
        differentNames: Boolean = false,
        differentControllers: Boolean = false,
        onePerCardType: Boolean = false,
        chooser: TargetChooser = TargetChooser.Controller,
    ): List<EffectTarget.BoundVariable> {
        require(exactly == null || (dynamicMaxCount == null && !optional && !unlimited)) {
            "targets(exactly = …) is the whole count; don't combine it with dynamicMaxCount, optional or unlimited"
        }
        return targets(
            TargetObject(
                count = count,
                minCount = minCount,
                optional = optional,
                unlimited = unlimited,
                filter = filter,
                dynamicMaxCount = exactly ?: dynamicMaxCount,
                dynamicMinCount = exactly,
                sameController = sameController,
                sameOwner = sameOwner,
                sameCreatureType = sameCreatureType,
                sameCardType = sameCardType,
                totalManaValueAtMost = totalManaValueAtMost,
                differentNames = differentNames,
                differentControllers = differentControllers,
                onePerCardType = onePerCardType,
                chooser = chooser,
            )
        )
    }
}

/**
 * The target list behind a builder's [TargetDeclarations]: requirements in declaration order, each
 * stamped with a binding id (`t0`, `t1`, …) unique across the whole card being built — nested
 * blocks (a reflexive or delayed trigger inside an ability) resolve their targets alongside the
 * enclosing ones, so ids only unique per block would collide. Outside a card, ids count per list.
 */
class TargetList : TargetDeclarations {
    private val declared = mutableListOf<TargetRequirement>()

    override fun declareTarget(requirement: TargetRequirement): String {
        val id = "t${com.wingedsheep.sdk.scripting.AbilityIdScope.nextTargetSlot() ?: declared.size}"
        declared += requirement.withId(id)
        return id
    }

    /** The requirements declared so far, in declaration order. */
    val requirements: List<TargetRequirement> get() = declared.toList()

    fun isEmpty(): Boolean = declared.isEmpty()
}
