package com.wingedsheep.sdk.scripting.conditions

import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.text.TextReplacer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Condition: "the [entity] matches [filter]".
 *
 * The single "an entity matches a filter" primitive. It names *which* entity via the shared
 * [EffectTarget] vocabulary and *what* to check via a [GameObjectFilter], replacing the four
 * near-clones that differed only in the entity role:
 *
 * | Old condition                  | EntityMatches form                            |
 * |--------------------------------|-----------------------------------------------|
 * | `SourceMatches(f)`             | `EntityMatches(EffectTarget.Self, f)`         |
 * | `EnchantedPermanentMatches(f)` | `EntityMatches(EffectTarget.EnchantedPermanent, f)` |
 * | `TargetMatchesFilter(f, i)`    | `EntityMatches(EffectTarget.ContextTarget(i), f)`   |
 * | `TriggeringSpellMatchesFilter(f)` | `EntityMatches(EffectTarget.TriggeringEntity, f)`|
 *
 * Card authors should prefer the `Conditions.*` facade helpers (`Conditions.SourceMatches`,
 * `Conditions.SourceIs*`, `Conditions.EnchantedPermanentMatches`, `Conditions.TargetMatchesFilter`,
 * `Conditions.TriggeringSpellMatches`), which build the appropriate [entity]/[filter] pair.
 *
 * **Evaluation by role** (`ConditionEvaluator`): the entity is resolved and matched against
 * projected state. The role also fixes *when* the condition can be answered:
 * - [EffectTarget.Self] — the source permanent; **dual-mode** (resolution *and* static-ability
 *   projection).
 * - [EffectTarget.EnchantedPermanent] / [EffectTarget.EnchantedCreature] /
 *   [EffectTarget.EquippedCreature] — the source's attachment; **dual-mode**.
 * - [EffectTarget.ContextTarget] — a chosen target; **resolution-only** (false under projection,
 *   and false when the chosen target is a player rather than a game object).
 * - [EffectTarget.TriggeringEntity] — the triggering spell; **resolution-only**, matched by its
 *   static cast characteristics (CR 603.4 re-check) so the answer stays correct even after the
 *   spell has left the stack.
 *
 * Deliberately **not**: a player check (use `Conditions.TargetIsPlayer`) nor a numeric/tracker
 * check (use `Compare` over a `DynamicAmount`). Entity roles other than those listed above are
 * unsupported: the `CardLinter` rejects them at card load
 * (`CardValidationError.UnsupportedEntityMatchesRole`), and the evaluator answers `false` as the
 * defense-in-depth backstop.
 *
 * @property entity Which entity to test.
 * @property filter The filter the entity must match.
 */
@SerialName("EntityMatches")
@Serializable
data class EntityMatches(
    val entity: EffectTarget,
    val filter: GameObjectFilter
) : Condition {
    override val description: String = when (entity) {
        is EffectTarget.Self ->
            if (filter == GameObjectFilter.Any) "if this permanent matches" else "if this ${filter.description}"
        is EffectTarget.EnchantedPermanent, is EffectTarget.EnchantedCreature, is EffectTarget.EquippedCreature ->
            "if ${entity.description} is ${filter.description}"
        is EffectTarget.TriggeringEntity ->
            "if it's ${filter.description.ifEmpty { "a matching" }} spell"
        else -> "if ${entity.description} matches ${filter.description}"
    }

    override fun applyTextReplacement(replacer: TextReplacer): Condition {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}
