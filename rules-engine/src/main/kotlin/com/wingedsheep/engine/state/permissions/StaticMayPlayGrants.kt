package com.wingedsheep.engine.state.permissions

import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.ClassLevelComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.MayPlayCardsFromExile

/**
 * Live [MayPlayPermission]s derived from [MayPlayCardsFromExile] static abilities on the
 * battlefield — "During your turn, you may play cards you don't own with stash counters on them
 * from exile" (Tinybones, Bauble Burglar).
 *
 * These permissions are *not* stored on [GameState]: a filter-defined static grant has to be
 * recomputed on every read, because the playable set changes as cards gain the filter's property,
 * as the granting permanent enters or leaves, and as the turn changes. Deriving them here — rather
 * than stamping a permission per card at exile time — is what makes the grant survive its granter
 * being replaced by a fresh copy, per Tinybones' ruling: the permission covers every stash-countered
 * card "regardless of whether they were put there by the Tinybones you currently control or a
 * Tinybones that was previously on the battlefield."
 *
 * The ability's own gate (`MayPlayCardsFromExile.condition`, e.g. "During your turn") is carried onto
 * the derived permission and evaluated by the shared [gateOpen] pass in [activeMayPlayFor], so there
 * is no second, divergent place where the gate is checked. Ids are derived from the granter rather
 * than generated, so repeated reads of the same state produce identical permissions.
 *
 * Read through [activeMayPlayFor] / [hasMayPlayFor], which union these with the stored permissions;
 * no call site should consult this object directly.
 */
internal object StaticMayPlayGrants {

    private val predicateEvaluator = PredicateEvaluator()

    /**
     * Every static grant that authorizes [playerId] to play [cardId] from exile right now.
     *
     * Ordered cheapest-check-first, because the permission read path runs per card over every exile
     * and graveyard zone on each legal-action enumeration: the zone test comes before the battlefield
     * scan for granting statics (so graveyard cards cost only a zone lookup), and the filter is
     * evaluated last.
     */
    fun forCard(
        state: GameState,
        cardId: EntityId,
        playerId: EntityId,
        cardRegistry: CardRegistry,
    ): List<MayPlayPermission> {
        // The grant only covers cards in exile — a stash-countered card that has since been played or
        // moved on is no longer playable this way.
        if (state.turnOrder.none { ownerId -> cardId in state.getExile(ownerId) }) return emptyList()

        val grants = grantsFor(state, playerId, cardRegistry)
        if (grants.isEmpty()) return emptyList()

        val projected = state.projectedState
        return grants.mapNotNull { (granterId, ability) ->
            val matches = predicateEvaluator.matches(
                state, projected, cardId, ability.filter,
                PredicateContext(controllerId = playerId, sourceId = granterId),
            )
            if (!matches) return@mapNotNull null
            MayPlayPermission(
                id = EntityId("static-may-play-from-exile:$granterId"),
                cardIds = setOf(cardId),
                controllerId = playerId,
                sourceId = granterId,
                // The ability's gate ("During your turn") rides the permission, so the shared
                // `gateOpen` re-evaluation covers it — with sourceId set, source-keyed conditions
                // resolve against the granting permanent rather than the exiled card.
                condition = ability.condition,
                withAnyManaType = ability.withAnyManaType,
                // Derived permissions are never stored, so end-of-turn cleanup never sees them;
                // `permanent` keeps any code that inspects the flag from treating them as expiring.
                permanent = true,
                timestamp = 0L,
            )
        }
    }

    /**
     * The [MayPlayCardsFromExile] abilities [playerId] controls, paired with the permanent that
     * grants them. Empty unless [playerId] controls such a permanent — their conditions are gated
     * later, by the shared permission read path.
     */
    private fun grantsFor(
        state: GameState,
        playerId: EntityId,
        cardRegistry: CardRegistry,
    ): List<Pair<EntityId, MayPlayCardsFromExile>> {
        val grants = mutableListOf<Pair<EntityId, MayPlayCardsFromExile>>()
        val projected = state.projectedState
        for (granterId in state.getBattlefield()) {
            val granter = state.getEntity(granterId) ?: continue
            // A face-down permanent has no text, so it grants nothing (CR 708.2a).
            if (granter.has<FaceDownComponent>()) continue
            if (projected.getController(granterId) != playerId) continue
            val card = granter.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
            val classLevel = granter.get<ClassLevelComponent>()?.currentLevel
            for (ability in cardDef.script.effectiveStaticAbilities(classLevel)) {
                if (ability !is MayPlayCardsFromExile) continue
                grants.add(granterId to ability)
            }
        }
        return grants
    }
}
