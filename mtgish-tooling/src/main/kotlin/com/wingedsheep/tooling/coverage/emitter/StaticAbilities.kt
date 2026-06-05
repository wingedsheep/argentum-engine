package com.wingedsheep.tooling.coverage.emitter

import com.wingedsheep.tooling.coverage.strField
import com.wingedsheep.tooling.coverage.subtypes
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * PermanentRuleEffect → `flags()` / `staticAbility { ability = ... }`. These classes live outside the
 * effects registry, so the capability gate is vacuous for them; the generated static is best-effort
 * and (like every draft) flagged for rules-text review.
 */
internal fun EmitCtx.staticBlock(rule: JsonObject): List<String>? {
    val rules = mutableListOf<JsonObject>()
    fun collect(n: JsonElement?) {
        when (n) {
            is JsonObject -> { if (n.strField("_PermanentRule") != null) rules.add(n); n.values.forEach { collect(it) } }
            is JsonArray -> n.forEach { collect(it) }
            else -> {}
        }
    }
    collect(rule)
    if (rules.isEmpty()) { reasons.add("PermanentRuleEffect"); return null }
    val lines = mutableListOf<String>()
    for (r in rules) {
        val name = r.strField("_PermanentRule")!!
        if (name == "CantBeBlocked") {
            used.add("AbilityFlag"); lines.add("    flags(AbilityFlag.CANT_BE_BLOCKED)"); continue
        }
        val dsl = staticAbilityDsl(name, r) ?: run { reasons.add(name); return null }
        lines.addAll(listOf("    staticAbility {", "        ability = $dsl", "    }"))
    }
    return lines
}

private fun EmitCtx.staticAbilityDsl(ruleName: String, ruleNode: JsonObject): String? {
    when (ruleName) {
        "CantBlock" -> { used.add("CantBlock"); return "CantBlock()" }
        "CantBeBlockedByMoreThanOne" -> { used.add("CantBeBlockedByMoreThan"); return "CantBeBlockedByMoreThan(maxBlockers = 1)" }
        "CanBlockOnly" -> {
            used.addAll(listOf("CanOnlyBlockCreaturesWith", "GameObjectFilter"))
            val kw = keywordOf(ruleNode)
            val bf = if (kw != null) "GameObjectFilter.Creature.withKeyword(Keyword.$kw)" else "GameObjectFilter.Creature"
            if (kw != null) used.add("Keyword")
            return "CanOnlyBlockCreaturesWith(blockerFilter = $bf)"
        }
        "CantBeBlockedExceptByDefenders", "CantBeBlockedByDefenders" -> {
            if (oracleText?.contains("defender", ignoreCase = true) == true) {
                used.addAll(listOf("CantBeBlockedExceptBy", "GameObjectFilter", "Keyword"))
                return "CantBeBlockedExceptBy(blockerFilter = GameObjectFilter.Creature.withKeyword(Keyword.DEFENDER))"
            }
            val blockerFilter = gameObjectFilterDsl(ruleNode["args"]) ?: return null
            used.add("CantBeBlockedBy")
            return "CantBeBlockedBy(blockerFilter = $blockerFilter)"
        }
        "CantAttackUnlessDefendingPlayer" -> {  // Deep-Sea Serpent: defender must control an Island
            val subs = subtypes(ruleNode)
            if (subs.isEmpty()) return null
            used.addAll(listOf("CantAttackUnless", "Conditions"))
            return "CantAttackUnless(Conditions.OpponentControlsLandType(\"${subs[0]}\"))"
        }
        "MustBlockAttacker" -> { used.add("MustBlock"); return "MustBlock()" }
        "MustAttackPlayer" -> { used.add("MustAttack"); return "MustAttack()" }
    }
    return null
}
