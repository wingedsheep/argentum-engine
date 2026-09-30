package com.wingedsheep.engine.mechanics.mana

import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.ClassLevelComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.PayLifeForColoredMana

/**
 * The engine half of [PayLifeForColoredMana] — "For each {B} in a cost, you may pay 2 life rather
 * than pay that mana" (K'rrik, Son of Yawgmoth).
 *
 * [apply] is the one lowering every cost seam calls once the cost is final (after every increase
 * and reduction): it rewrites the payer's substitutable symbols into Phyrexian ones
 * ([ManaCost.withLifePayable]), after which the ordinary Phyrexian payment path does the rest. It is
 * idempotent, so a seam that re-lowers an already-lowered cost changes nothing.
 */
object LifePayableMana {

    /** The colors whose mana symbols [playerId] may pay with life, from statics they control. */
    fun colors(state: GameState, cardRegistry: CardRegistry, playerId: EntityId): Set<Color> {
        var result: MutableSet<Color>? = null
        val projected = state.projectedState
        for (entityId in state.controlledBattlefield(playerId)) {
            val container = state.getEntity(entityId) ?: continue
            if (container.has<FaceDownComponent>()) continue
            if (projected.hasLostAllAbilities(entityId)) continue
            val card = container.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
            val classLevel = container.get<ClassLevelComponent>()?.currentLevel
            for (ability in cardDef.script.effectiveStaticAbilities(classLevel)) {
                if (ability is PayLifeForColoredMana) {
                    (result ?: mutableSetOf<Color>().also { result = it }).add(ability.color)
                }
            }
        }
        return result ?: emptySet()
    }

    /** [cost] with every symbol [payerId] may pay with life rewritten into its Phyrexian form. */
    fun apply(state: GameState, cardRegistry: CardRegistry, payerId: EntityId, cost: ManaCost): ManaCost {
        if (cost.symbols.isEmpty()) return cost
        val colors = colors(state, cardRegistry, payerId)
        if (colors.isEmpty()) return cost
        return colors.fold(cost) { acc, color -> acc.withLifePayable(color) }
    }

    /**
     * How many of [printed]'s symbols a life substitution would turn Phyrexian for [payerId] — the
     * pips whose life payment is *not* a payment of a Phyrexian symbol. Compleated (CR 702.150a)
     * counts only Phyrexian symbols paid with life, and the payer chooses which pip each life
     * payment covers, so these are attributed first.
     */
    fun substitutedPipCount(state: GameState, cardRegistry: CardRegistry, payerId: EntityId, printed: ManaCost): Int {
        val colors = colors(state, cardRegistry, payerId)
        if (colors.isEmpty()) return 0
        val lowered = colors.fold(printed) { acc, color -> acc.withLifePayable(color) }
        return lowered.phyrexianSymbols.size - printed.phyrexianSymbols.size
    }
}
