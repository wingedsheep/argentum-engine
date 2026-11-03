package com.wingedsheep.engine.state.permissions

import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.ClassLevelComponent
import com.wingedsheep.engine.state.components.battlefield.MayCastFromLinkedExileUsedThisTurnComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.emblemStaticAbilitiesOf
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.MayPlayCardsFromExile

/**
 * Live [MayPlayPermission]s derived from [MayPlayCardsFromExile] static abilities on the
 * battlefield and on emblems — "During your turn, you may play cards you don't own with stash counters on them
 * from exile" (Tinybones, Bauble Burglar), or "you may play a permanent card you own from outside the
 * game or from your graveyard" (The Holodeck).
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

    /**
     * Every static grant that authorizes [playerId] to play [cardId] from where it is right now.
     *
     * Ordered cheapest-check-first, because the permission read path runs per card over every exile
     * and graveyard zone on each legal-action enumeration: the zone test comes before the battlefield
     * scan for granting statics (so an opponent's graveyard card costs only a zone lookup), each
     * grant's [MayPlayCardsFromExile.zones] is checked next, and the filter is evaluated last.
     */
    fun forCard(
        state: GameState,
        cardId: EntityId,
        playerId: EntityId,
        cardRegistry: CardRegistry,
        predicateEvaluator: PredicateEvaluator
    ): List<MayPlayPermission> {
        // The grant covers only cards still in a zone it reaches — a stash-countered card that has
        // since been played or moved on is no longer playable this way. Exile is any player's; a
        // graveyard or sideboard grant ("a card you own") reaches only the granted player's own.
        val zone = when {
            state.turnOrder.any { ownerId -> cardId in state.getExile(ownerId) } -> Zone.EXILE
            cardId in state.getGraveyard(playerId) -> Zone.GRAVEYARD
            cardId in state.getZone(ZoneKey(playerId, Zone.SIDEBOARD)) -> Zone.SIDEBOARD
            else -> return emptyList()
        }

        val grants = grantsFor(state, playerId, cardRegistry)
        if (grants.isEmpty()) return emptyList()

        val projected = state.projectedState
        return grants.mapNotNull { (granterId, ability) ->
            if (zone !in ability.zones) return@mapNotNull null
            // "Once each turn" (Evelyn, the Covetous; The Holodeck): a granter whose allowance is
            // spent offers nothing until its marker is cleared at end of turn.
            if (allowanceSpent(state, granterId, ability)) return@mapNotNull null
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
                withAnyManaColor = ability.withAnyManaColor,
                spendAsAnyColor = ability.spendAsAnyColor,
                oncePerTurn = ability.oncePerTurn,
                // Derived permissions are never stored, so end-of-turn cleanup never sees them;
                // `permanent` keeps any code that inspects the flag from treating them as expiring.
                permanent = true,
                timestamp = 0L,
            )
        }
    }

    /**
     * True when [playerId] has a static grant reaching [zone] whose allowance isn't spent this turn.
     * The enumerator asks this before walking a sideboard, which a Limited game fills with the whole
     * unplayed pool — so the walk only happens while a grant (The Holodeck) could play from it. The
     * grant's own gate (its condition) is left to the per-card read.
     */
    fun coversZone(
        state: GameState,
        playerId: EntityId,
        zone: Zone,
        cardRegistry: CardRegistry,
    ): Boolean = grantsFor(state, playerId, cardRegistry).any { (granterId, ability) ->
        zone in ability.zones && !allowanceSpent(state, granterId, ability)
    }

    private fun allowanceSpent(state: GameState, granterId: EntityId, ability: MayPlayCardsFromExile): Boolean =
        ability.oncePerTurn &&
            state.getEntity(granterId)?.has<MayCastFromLinkedExileUsedThisTurnComponent>() == true

    /**
     * The [MayPlayCardsFromExile] abilities [playerId] controls, paired with the permanent that
     * grants them (or the emblem that has them). Empty unless [playerId] controls such a permanent
     * or has such an emblem — their conditions are gated later, by the shared permission read path.
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
        // An emblem the player has (Tibalt, Cosmic Impostor's "You may play cards exiled with
        // Tibalt") grants from outside every zone; the emblem entity stands in for the granter.
        for ((emblemId, ability) in state.emblemStaticAbilitiesOf(playerId)) {
            if (ability is MayPlayCardsFromExile) grants.add(emblemId to ability)
        }
        return grants
    }
}
