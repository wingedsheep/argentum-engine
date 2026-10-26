package com.wingedsheep.engine.handlers.effects.stack

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.stack.EntryKeywordGrant
import com.wingedsheep.engine.state.components.stack.SpellGrantedKeywordsComponent
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.sdk.scripting.effects.GrantKeywordToSpellEffect
import kotlin.reflect.KClass

/**
 * Executor for [GrantKeywordToSpellEffect].
 *
 * Resolves the target to a stack entity (typically the triggering spell) and adds the
 * keyword to its [SpellGrantedKeywordsComponent]. Damage code paths consult this
 * component when checking source keywords (e.g. wither, lifelink) so granted keywords
 * apply for the rest of the spell's time on the stack.
 *
 * With a `permanentDuration` the keyword is also frozen onto the spell's
 * [SpellOnStackComponent.entryKeywordGrants], which `PermanentEntry` turns into a grant on the
 * permanent the spell becomes ("it gains haste until end of turn" — Tyvar Kell's emblem). A spell
 * or ability that never becomes a permanent simply never reads it.
 */
class GrantKeywordToSpellExecutor : EffectExecutor<GrantKeywordToSpellEffect> {

    override val effectType: KClass<GrantKeywordToSpellEffect> = GrantKeywordToSpellEffect::class

    override fun execute(
        state: GameState,
        effect: GrantKeywordToSpellEffect,
        context: EffectContext
    ): EffectResult {
        val targetId = context.resolveTarget(effect.target, state)
            ?: return EffectResult.success(state)

        val container = state.getEntity(targetId)
            ?: return EffectResult.success(state)

        val existing = container.get<SpellGrantedKeywordsComponent>()
        val updated = SpellGrantedKeywordsComponent((existing?.keywords ?: emptySet()) + effect.keyword)
        val newState = state.updateEntity(targetId) { c ->
            val onStack = c.get<SpellOnStackComponent>()
            val duration = effect.permanentDuration
            if (onStack == null || duration == null) c.with(updated)
            else c.with(updated).with(
                onStack.copy(entryKeywordGrants = onStack.entryKeywordGrants + EntryKeywordGrant(effect.keyword, duration, context.sourceId))
            )
        }

        return EffectResult.success(newState)
    }
}
