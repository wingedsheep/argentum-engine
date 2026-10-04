package com.wingedsheep.engine.state

import com.wingedsheep.engine.core.ManaSpendingObligationsContinuation
import com.wingedsheep.engine.handlers.effects.mana.ManaProvenanceTracker
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.player.RestrictedManaEntry
import com.wingedsheep.engine.state.components.player.ManaSourceTag
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.ManaRestriction

fun GameState.activeManaSpendingScope(playerId: EntityId): ManaSpendingObligationsContinuation? =
    continuationStack.lastOrNull { it is ManaSpendingObligationsContinuation && it.playerId == playerId }
        as? ManaSpendingObligationsContinuation

fun GameState.remainingManaObligations(playerId: EntityId): Boolean = continuationStack.any {
    it is ManaSpendingObligationsContinuation && it.playerId == playerId && it.pendingIds.isNotEmpty()
}

/** Tag only the activated ability's production, before triggered mana bonuses are applied. */
fun tagManaObligationProduction(
    before: GameState,
    after: GameState,
    playerId: EntityId,
    sourceId: EntityId? = null,
    productionTag: ManaSourceTag? = null,
): GameState {
    if (after.activeManaSpendingScope(playerId) == null) return after
    val old = before.getEntity(playerId)?.get<ManaPoolComponent>() ?: ManaPoolComponent()
    val added = after.getEntity(playerId)?.get<ManaPoolComponent>() ?: ManaPoolComponent()
    val restrictedDelta = (added.restrictedMana.size - old.restrictedMana.size).coerceAtLeast(0)
    val colored = Color.entries.associateWith { (added.getAmount(it) - old.getAmount(it)).coerceAtLeast(0) }
    val colorless = (added.colorless - old.colorless).coerceAtLeast(0)
    // Zero output still creates an unsatisfied activation: it cannot silently count as a contribution.
    val (id, allocated) = after.newRoutingId()
    val tag = productionTag ?: sourceId?.let { ManaProvenanceTracker.sourceTag(before, it) }
    val entries = colored.flatMap { (color, count) -> List(count) {
        RestrictedManaEntry(color, ManaRestriction.AnySpend, source = tag, obligationIds = setOf(id))
    } } + List(colorless) {
        RestrictedManaEntry(null, ManaRestriction.AnySpend, source = tag, obligationIds = setOf(id))
    }
    val untaggedCount = added.restrictedMana.size - restrictedDelta
    val pool = added.copy(
        white = added.white - colored.getValue(Color.WHITE),
        blue = added.blue - colored.getValue(Color.BLUE),
        black = added.black - colored.getValue(Color.BLACK),
        red = added.red - colored.getValue(Color.RED),
        green = added.green - colored.getValue(Color.GREEN),
        colorless = added.colorless - colorless,
        // Those units now carry their exact snow/source metadata on the restricted entries.
        snowMana = old.snowMana,
        snowColorless = old.snowColorless,
        restrictedMana = added.restrictedMana.mapIndexed { index, entry ->
            if (index >= untaggedCount) entry.copy(
                source = entry.source?.takeUnless { productionTag != null && it.sourceId == sourceId } ?: tag,
                obligationIds = entry.obligationIds + id,
            ) else entry
        } + entries,
        // Preserve the preexisting approximate counters; moved units have exact entry tags.
        manaBySubtype = old.manaBySubtype,
        manaBySource = old.manaBySource,
        manaByCardType = old.manaByCardType,
    )
    return allocated.updateEntity(playerId) { it.with(pool) }.copy(
        continuationStack = allocated.continuationStack.map { frame ->
            if (frame is ManaSpendingObligationsContinuation && frame.playerId == playerId)
                frame.copy(pendingIds = frame.pendingIds + id) else frame
        }
    )
}

/** Exact multiset comparison: a same-color unit from another activation cannot discharge this one. */
fun settleManaObligationPayment(
    state: GameState,
    playerId: EntityId,
    before: List<RestrictedManaEntry>,
    after: List<RestrictedManaEntry>,
): GameState {
    if (state.activeManaSpendingScope(playerId) == null) return state
    val beforeCounts = before.flatMap { it.obligationIds }.groupingBy { it }.eachCount()
    val afterCounts = after.flatMap { it.obligationIds }.groupingBy { it }.eachCount()
    val spent = beforeCounts.filter { (id, count) -> count > afterCounts.getOrDefault(id, 0) }.keys
    if (spent.isEmpty()) return state
    return state.updateEntity(playerId) { container ->
        val pool = container.get<ManaPoolComponent>() ?: return@updateEntity container
        container.with(pool.copy(restrictedMana = pool.restrictedMana.map {
            it.copy(obligationIds = it.obligationIds - spent)
        }))
    }.copy(continuationStack = state.continuationStack.map { frame ->
        if (frame is ManaSpendingObligationsContinuation && frame.playerId == playerId)
            frame.copy(pendingIds = frame.pendingIds - spent) else frame
    })
}
