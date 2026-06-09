package com.wingedsheep.engine.legalactions

import com.wingedsheep.engine.core.TurnManager
import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.legalactions.utils.CastPermissionUtils
import com.wingedsheep.engine.legalactions.utils.CostEnumerationUtils
import com.wingedsheep.engine.legalactions.utils.TargetEnumerationUtils
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.mechanics.mana.CostCalculator
import com.wingedsheep.engine.mechanics.mana.GrantedKeywordResolver
import com.wingedsheep.engine.mechanics.mana.ManaSource
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.CantActivateLoyaltyAbilitiesComponent
import com.wingedsheep.engine.state.components.player.CantCastSpellsComponent
import com.wingedsheep.engine.state.components.player.LandDropsComponent
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.model.EntityId

/**
 * Controls what data is computed during enumeration.
 *
 * [FULL] computes everything including auto-tap previews for the UI.
 * [ACTIONS_ONLY] skips auto-tap preview solve() calls — used by MCTS/simulation
 * where only the GameAction matters, not which lands to highlight.
 */
enum class EnumerationMode {
    FULL,
    ACTIONS_ONLY
}

/**
 * Shared precomputed state for a single enumeration pass.
 *
 * Cross-cutting values are computed lazily at most once. All enumerators
 * share this context, avoiding redundant computation.
 */
class EnumerationContext(
    val state: GameState,
    val playerId: EntityId,
    val cardRegistry: CardRegistry,
    val manaSolver: ManaSolver,
    val costCalculator: CostCalculator,
    val predicateEvaluator: PredicateEvaluator,
    val conditionEvaluator: ConditionEvaluator,
    val turnManager: TurnManager,
    val mode: EnumerationMode = EnumerationMode.FULL
) {
    val skipAutoTapPreview: Boolean get() = mode == EnumerationMode.ACTIONS_ONLY
    // Granted-keyword resolver (e.g., convoke granted by Eirdu via GrantKeywordToOwnSpells)
    val grantedKeywordResolver by lazy { GrantedKeywordResolver(cardRegistry) }
    // Utility classes (lazy initialized)
    val targetUtils by lazy { TargetEnumerationUtils(predicateEvaluator) }
    val costUtils by lazy { CostEnumerationUtils(manaSolver, costCalculator, predicateEvaluator, cardRegistry) }
    val castPermissionUtils by lazy { CastPermissionUtils(cardRegistry, predicateEvaluator, conditionEvaluator) }

    // Projected state
    val projected: ProjectedState by lazy { state.projectedState }

    // Battlefield permanents controlled by player (via projected state)
    val battlefieldPermanents: List<EntityId> by lazy {
        projected.getBattlefieldControlledBy(playerId)
    }

    // Available mana sources (cached per enumeration pass — avoids redundant battlefield scans)
    val availableManaSources: List<ManaSource> by lazy {
        manaSolver.findAvailableManaSources(state, playerId)
    }

    // Timing flags
    val canPlaySorcerySpeed: Boolean by lazy {
        state.step.isMainPhase && state.stack.isEmpty() && state.activePlayerId == playerId
    }

    // Land drop availability (accounts for static ability bonuses like GrantAdditionalLandDrop)
    val canPlayLand: Boolean by lazy {
        val landDrops = state.getEntity(playerId)?.get<LandDropsComponent>()
        val remaining = landDrops?.remaining ?: 0
        val staticBonus = castPermissionUtils.getAdditionalLandDrops(state, playerId)
        canPlaySorcerySpeed && (remaining + staticBonus > 0)
    }

    // Cast restrictions — blanket, spell-independent locks (a Silence-style CantCastSpellsComponent
    // or a RestrictSpellsCastPerTurn per-turn limit). Cached once per enumeration pass.
    val cantCastSpells: Boolean by lazy {
        state.getEntity(playerId)?.has<CantCastSpellsComponent>() == true ||
            castPermissionUtils.hasReachedSpellCastLimit(state, playerId)
    }

    // Loyalty-activation restriction (Revel in Silence etc.)
    val cantActivateLoyaltyAbilities: Boolean by lazy {
        state.getEntity(playerId)?.has<CantActivateLoyaltyAbilitiesComponent>() == true
    }

    // Whether any per-spell cast restriction (Mana Maze, PlayersCantCastSpells) is in play at all —
    // a cheap guard so cantCastSpell() stays O(1) when none exists (the common case).
    private val perSpellCastRestrictionPresent: Boolean by lazy {
        castPermissionUtils.anyPerSpellCastRestrictionPresent(state)
    }

    /**
     * Whether this player can't cast the specific card [cardId] right now — the blanket lock, the
     * CR 205.4e legendary-instant/sorcery restriction, OR a per-spell static restriction (Mana
     * Maze's color sharing, a filtered [PlayersCantCastSpells]). Every cast-enumeration site that
     * has a candidate card consults this, so the prohibitions are honoured across every casting zone.
     */
    fun cantCastSpell(cardId: EntityId): Boolean =
        cantCastSpells ||
            castPermissionUtils.lacksLegendaryControlForLegendarySpell(state, playerId, cardId) ||
            (perSpellCastRestrictionPresent &&
                castPermissionUtils.spellSpecificallyRestricted(state, playerId, cardId))

    // A battlefield [MayCastWithoutPayingManaCost] source is granting the player permission to
    // cast a spell without paying its mana cost (e.g., Weftwalking on the first spell of the
    // player's own turn). Surfaced as its own [CastSpell.useWithoutPayingManaCost] action
    // variant in CastSpellEnumerator, distinct from [alternativeCastingCosts] so the player can
    // choose between this free cast and any Jodah-style alternative (CR 118.9a — only one
    // alternative cost can apply to a cast, and which one is the player's choice).
    val freeCastPermissionAvailable: Boolean by lazy {
        costCalculator.hasFreeCastPermission(state, playerId)
    }

    /**
     * Whether a battlefield [MayCastWithoutPayingManaCost] source grants this player permission to
     * cast the specific card [cardId] for free. Unlike [freeCastPermissionAvailable] (a generic
     * "any free-cast source exists?" probe), this honors a source's spell filter — e.g.
     * Dracogenesis only frees Dragon spells.
     */
    fun freeCastPermissionFor(cardId: EntityId): Boolean {
        val cardDef = state.getEntity(cardId)
            ?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()
            ?.let { cardRegistry.getCard(it.cardDefinitionId) }
            ?: return freeCastPermissionAvailable
        return costCalculator.hasFreeCastPermission(state, playerId, cardDef)
    }

    // Alternative casting costs from battlefield permanents (e.g., Jodah's WUBRG).
    val alternativeCastingCosts: List<ManaCost> by lazy {
        costCalculator.findAlternativeCastingCosts(state, playerId)
    }

    // Cycling prevention
    val cyclingPrevented by lazy {
        castPermissionUtils.isCyclingPrevented(state)
    }
}
