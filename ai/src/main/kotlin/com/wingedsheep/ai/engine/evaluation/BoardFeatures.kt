package com.wingedsheep.ai.engine.evaluation

import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.ai.engine.OpponentAggregate
import com.wingedsheep.ai.engine.PlaneswalkerThreat
import com.wingedsheep.ai.engine.knowledge.CardIntent
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.lifePoolsOf
import com.wingedsheep.ai.engine.sidesFor
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.LandDropsComponent
import com.wingedsheep.engine.state.components.player.PlayerTurnsTakenComponent
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId

// ═════════════════════════════════════════════════════════════════════════════
// Life Differential
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Non-linear life differential. Being at 3 life is exponentially worse
 * than being at 13 — each point near death is worth much more.
 *
 * Life is a resource, not a threat — an opponent sitting on 40 does not attack you — so the fold
 * over a pod is [OpponentAggregate.FIELD]: how the AI is doing against the table, not against its
 * scariest neighbour. Sides are valued per life *pool*, so a Two-Headed Giant team's shared 30
 * counts once (CR 810.4).
 */
object LifeDifferential : BoardFeature {
    override fun score(state: GameState, projected: ProjectedState, playerId: EntityId): Double {
        val sides = state.sidesFor(playerId) ?: return 0.0
        val mine = sideLifeValue(state, sides.mine)
        return sides.against(OpponentAggregate.FIELD) { opponent ->
            mine - sideLifeValue(state, opponent)
        }
    }

    private fun sideLifeValue(state: GameState, side: List<EntityId>): Double =
        state.lifePoolsOf(side).sumOf { lifeValue(it) }

    /**
     * Public so a consumer that wants to price *anticipated* life loss can charge it at exactly
     * the rate the evaluator charges life already, instead of inventing a second constant.
     * [com.wingedsheep.ai.engine.CombatAdvisor]'s crack-back estimate is the caller.
     */
    fun lifeValue(life: Int): Double = when {
        life <= 0 -> -100.0
        life <= 3 -> life * 3.0
        life <= 7 -> 9.0 + (life - 3) * 2.0
        life <= 15 -> 17.0 + (life - 7) * 1.0
        else -> 25.0 + (life - 15) * 0.5
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Library Clock
// ═════════════════════════════════════════════════════════════════════════════

/**
 * How close each library is to running out — [com.wingedsheep.ai.engine.AiProfile.libraryClock].
 *
 * Drawing from an empty library loses the game (CR 704.5b), but only at the next draw, so a leaf
 * that has just exiled or milled its own library looks like any other board: game a7fc150e (Momir
 * Basic) played a land with Territorial Bruntar out, its landfall trigger exiled all 48 basics, and
 * the AI decked out at 20 life. A library is a clock of about one card a turn, so this prices it
 * the way [LifeDifferential] prices life: nothing until the last few cards, then steeply — an
 * empty library is most of the way to a loss. Symmetric, in life units, folded over the field like
 * life: milling ourselves out is bad, milling an opponent out is good.
 *
 * Zero from [SAFE_CARDS] up, so a 40- or 60-card game far from the end of its library scores
 * exactly as it did without it.
 */
object LibraryClock : BoardFeature {
    override fun score(state: GameState, projected: ProjectedState, playerId: EntityId): Double {
        val sides = state.sidesFor(playerId) ?: return 0.0
        val mine = sideValue(state, sides.mine)
        return sides.against(OpponentAggregate.FIELD) { opponent -> mine - sideValue(state, opponent) }
    }

    private fun sideValue(state: GameState, side: List<EntityId>): Double =
        side.sumOf { libraryValue(state.getLibrary(it).size) }

    /**
     * Cards left from which the library scores nothing. Three draws is three turns of game still
     * to play; scenario positions routinely hold libraries of four to six cards, and a search or a
     * cycle out of one is not the misplay this term exists for.
     */
    const val SAFE_CARDS = 3

    /** An empty library: one draw from a loss — most of [LifeDifferential]'s −100 for dead. */
    private const val EMPTY_LIBRARY = -60.0

    /** Each card left keeps this fraction of the penalty of one fewer: −60, −15, −3.75, then 0. */
    private const val DECAY = 0.25

    /** What a library of [cards] is worth to its owner, in life units; 0 from [SAFE_CARDS] up. */
    fun libraryValue(cards: Int): Double =
        if (cards >= SAFE_CARDS) 0.0 else EMPTY_LIBRARY * Math.pow(DECAY, cards.coerceAtLeast(0).toDouble())
}

// ═════════════════════════════════════════════════════════════════════════════
// Board Presence
// ═════════════════════════════════════════════════════════════════════════════

/**
 * The two corrections to `BoardPresence.creatureValue` that
 * [com.wingedsheep.ai.engine.AiProfile.PRODUCTION_RACECLOCK]'s KDoc itemized as the reason its
 * arena win was a trade rather than a straight gain. Both were masked while the race clock's
 * `99.0` no-attacker sentinel was out of scale, and both surfaced the moment it stopped being.
 *
 * One carrier rather than two boolean parameters because they travel the same path
 * ([BoardPresence.score] → `permanentValue` → `creatureValue`) and every intermediate signature
 * would otherwise grow twice. They stay two independent fields, each with its own
 * [com.wingedsheep.ai.engine.AiProfile] flag and its own attribution column, so either can be
 * backed out alone.
 *
 * **Only the evaluator passes one of these.** The other `permanentValue` callers — `TargetSelection`'s
 * heuristic rank, `CombatMath`, `DecisionResponder`, `RemovalPatience`'s fair-trade bar — keep
 * [LEGACY] on purpose, the same seam every profile flag here sits behind. `RemovalPatience` is the
 * one worth naming: its `FAIR_TRADE_VALUE_PER_MANA` is calibrated against *vanilla* creatures, which
 * neither correction touches, so moving it would change the bar's meaning rather than its inputs.
 */
data class CreatureValuation(
    /**
     * Stop discounting a creature for damage marked on it.
     *
     * Marked damage wears off at cleanup (CR 514.2), so a 3/3 with one damage on it is a 3/3 next
     * turn — exactly the case the end-of-turn subtraction in `creatureValue` exists
     * to avoid, arrived at from the other direction. The old `0.5 + 0.5 × healthFraction` made
     * *pinging* the opponent's 3/3 look like 0.7 of board progress, which is `activate-04`: one
     * damage at a creature it cannot kill, bought with the Sorcerer's whole turn.
     *
     * Off, one point of damage on a 3/3 costs it a sixth of its value; on, it costs nothing. There
     * is a real case in between — a creature already damaged *is* cheaper to finish off — but it is
     * a fact about a line, not about a leaf, and a one-ply evaluator that prices it ends up paying
     * for the first half of a two-card plan it will not remember to complete.
     */
    val markedDamageFadesAtCleanup: Boolean = false,
    /**
     * Price a projected "can't attack" the way DEFENDER is already priced — as the loss of the
     * power — instead of as a flat ×0.85.
     *
     * The two spellings of the same restriction disagreed by a factor of four. A Pacifism'd Craw
     * Wurm kept 0.85 of a 6/4 (and 0.72 once the can't-block multiplier landed too), so at 5.5 it
     * still outranked an untouched Hill Giant at 4.2 and drew the Murder — `removal-03`. A 6/4 with
     * DEFENDER printed on it, which can do exactly as much, scored 2.8.
     *
     * The multiplicative form is wrong in shape and not merely in size: it scales toughness down as
     * well, so it takes *more* value off a creature that can still block than off one that cannot,
     * and it lands hardest on the big creatures where the restriction is worth the most. Subtracting
     * the power leaves the body — a pacified 6/4 still walls a 3/3 — which is the thing the AI
     * actually gets to keep using.
     */
    val cantAttackCostsPower: Boolean = false,
    /**
     * Read an "until end of turn" P/T change only on a creature in a fight that has not dealt its
     * damage yet — everywhere else, read the stats it will have after cleanup.
     *
     * `BoardPresence` took the modification off its base stats already, but its power-scaled
     * keyword bonuses and the whole of `ThreatAssessment` read it whole, so a pump bought for
     * nothing scored as board progress. See [ExpiringStats] for the production game and for why
     * the simulation, not this term, covers the uses that matter (a pump in response to burn,
     * firebreathing once blockers are declared). Off is the historical reading.
     */
    val expiringStatsNeedAFight: Boolean = false,
    /**
     * Price a planeswalker continuously in its loyalty, net of the attack it faces before its
     * controller acts again ([PlaneswalkerThreat.boardValue] / [PlaneswalkerThreat.expectedDamage]),
     * instead of `max(4.0, prior + 0.8 × loyalty)`.
     *
     * Not a creature correction — it rides this carrier because it travels exactly the same
     * evaluator-only path to `permanentValue`. The flat floor hid every loyalty change below it,
     * so a walker about to die looked as healthy as a fresh one: nothing told the AI to spend its
     * loyalty first, to keep a blocker home for it, or that a −1 left it dead to the next attack.
     * [com.wingedsheep.ai.engine.AiProfile.defendPlaneswalkers] sets it.
     */
    val walkersFaceTheNextAttack: Boolean = false,
    /**
     * Read a planeswalker's loyalty abilities off the battlefield it stands on — its printed ones
     * plus any another permanent grants it (`GrantActivatedAbility`, the FRA "Way of the X" cards)
     * — when [walkersFaceTheNextAttack] asks whether it can only spend loyalty
     * ([com.wingedsheep.ai.engine.knowledge.IntentCatalog.loyaltyIsOnlyFuel]). A Jace token under
     * Way of the Pyromancer has a +1 and is priced as the engine it now is.
     * [com.wingedsheep.ai.engine.AiProfile.spendFuelWalkers] sets it.
     */
    val grantedLoyaltyAbilities: Boolean = false,
    /**
     * Under [walkersFaceTheNextAttack], price a planeswalker as the engine it is
     * ([PlaneswalkerThreat.engineBoardValue]): its abilities read where it stands (granted ones
     * included), an engine worth at least [PlaneswalkerThreat.ENGINE_VALUE] beyond its loyalty, its
     * ultimate a threat that grows as it climbs, and a minus-only walker's counters worth the share
     * of a card each one buys. [com.wingedsheep.ai.engine.AiProfile.walkersPricedAsEngines] sets it.
     */
    val walkersAsEngines: Boolean = false,
) {
    companion object {
        /** Neither correction: what every number published before 2026-08-10 was measured on. */
        val LEGACY = CreatureValuation()
    }
}

/**
 * Total effective board value. Creatures are scored by combat stats + keywords.
 * Non-creature permanents get type-appropriate values. Enchantments and artifacts
 * that aren't auras get a flat bonus (they're doing something even without P/T).
 *
 * A board is a threat, so a pod folds with [OpponentAggregate.THREAT] — the runaway leader
 * dominates. A team's board is the sum of its members' (CR 810: teammates attack and block as one
 * side, so their permanents defend each other).
 */
object BoardPresence : BoardFeature {
    /**
     * Score with no card knowledge — the pre-Phase-6 behaviour, and what every caller that has no
     * [IntentCatalog] to hand still gets.
     */
    override fun score(state: GameState, projected: ProjectedState, playerId: EntityId): Double =
        score(state, projected, playerId, IntentCatalog.NONE)

    /**
     * Score with [intents] supplying a per-card prior for non-creature permanents.
     *
     * `EvaluationWeights.toEvaluator(intents)` binds this into the evaluator; on
     * [IntentCatalog.NONE] it is exactly [score].
     */
    fun score(
        state: GameState,
        projected: ProjectedState,
        playerId: EntityId,
        intents: IntentCatalog,
        sequenceLandsByUsableMana: Boolean = false,
        creatureValuation: CreatureValuation = CreatureValuation.LEGACY,
        pendingLandDropIsMana: Boolean = false,
        handAtRoot: Set<EntityId>? = null,
        modalLandRoot: ModalLandRoot? = null,
    ): Double {
        val sides = state.sidesFor(playerId) ?: return 0.0
        val mine = boardValue(state, projected, sides.mine, intents, creatureValuation) +
            if (sequenceLandsByUsableMana) {
                landSequencing(state, projected, playerId, intents, pendingLandDropIsMana, handAtRoot, modalLandRoot)
            } else {
                0.0
            }
        return sides.against(OpponentAggregate.THREAT) { opponent ->
            mine - boardValue(state, projected, opponent, intents, creatureValuation)
        }
    }

    /**
     * [score] with its profile switches bound — the feature `EvaluationWeights.toEvaluator` puts in
     * the composite. It exists as a class rather than a lambda for [rootedAt]: the land-sequencing
     * refund needs the decision root's hand (`handAtRoot`), and only a feature that knows its own
     * parameters can hand back a copy carrying it.
     */
    class Bound(
        private val intents: IntentCatalog,
        private val sequenceLandsByUsableMana: Boolean,
        private val creatureValuation: CreatureValuation,
        private val pendingLandDropIsMana: Boolean,
        private val surplusAwareLands: Boolean = false,
        private val handAtRoot: Set<EntityId>? = null,
        private val modalLandRoot: ModalLandRoot? = null,
    ) : BoardFeature {
        override fun score(state: GameState, projected: ProjectedState, playerId: EntityId): Double =
            score(
                state, projected, playerId, intents, sequenceLandsByUsableMana, creatureValuation,
                pendingLandDropIsMana, handAtRoot, modalLandRoot,
            )

        override fun rootedAt(root: GameState, playerId: EntityId): BoardFeature =
            if (!sequenceLandsByUsableMana) this
            else Bound(
                intents, sequenceLandsByUsableMana, creatureValuation, pendingLandDropIsMana, surplusAwareLands,
                handAtRoot = root.getZone(playerId, Zone.HAND).toSet(),
                modalLandRoot = if (surplusAwareLands) ModalLandRoot.of(root, playerId) else null,
            )
    }

    private fun boardValue(
        state: GameState,
        projected: ProjectedState,
        side: List<EntityId>,
        intents: IntentCatalog,
        creatureValuation: CreatureValuation,
    ): Double {
        var total = 0.0
        for (playerId in side) {
            for (entityId in projected.getBattlefieldControlledBy(playerId)) {
                val card = state.getEntity(entityId)?.get<CardComponent>() ?: continue
                total += permanentValue(state, projected, entityId, card, intents, creatureValuation)
            }
        }
        return total
    }

    /**
     * The part of a land's worth [permanentValue] cannot see: not *whether* it is tapped, but
     * whether being tapped is costing anything **this turn**.
     *
     * [permanentValue] prices an untapped land [LAND_UNTAPPED] and a tapped one [LAND_TAPPED], flat,
     * always. That constant is the only thing in the evaluator that tells one land drop from
     * another, and it is right for the wrong reason: a tapped land is not worth less because it is
     * tapped — it untaps next turn — it is worth less when the mana it is not producing is mana you
     * would have spent. Two corrections follow, and they are the same idea seen from each end:
     *
     *  1. **Refund the charge when the mana was idle.** If nothing in hand becomes castable by
     *     untapping those lands, they cost their controller nothing this turn and are worth a full
     *     land each. This is what makes "play the tapland on the turn you have nothing to cast" fall
     *     out — and, deliberately, also stops charging the AI for lands it tapped to *cast* something,
     *     which was a standing tax on spending mana at all.
     *  2. **Charge the debt while it is still in hand.** A land that always enters tapped
     *     ([CardIntent.entersTapped]) owes a turn of mana whenever it is finally played. Holding it
     *     is therefore slightly worse than holding a basic, which is what breaks the tie in 1's
     *     favour: on a turn where both drops are free, get the tapland out of your hand.
     *
     * [TAPLAND_IN_HAND] is deliberately far below [IDLE_MANA_REFUND]: tidying your hand must never
     * outrank mana you can spend right now. `BoardPresenceLandSequencingTest` pins that ordering.
     *
     * **Own seat only, never a side or an opponent.** Both halves read *hand contents* — mana values
     * and which lands enter tapped — and the AI is only entitled to its own. Reading a teammate's or
     * an opponent's would be exactly the hidden-information cheat Phase 8's determinizer exists to
     * remove, bought for a tenth of a point.
     *
     * Two approximations, both deliberate. Castability is mana *value* against a land count, not a
     * real `ManaSolver` run — colours, alternative costs and timing are ignored, because this term
     * only has to rank two land drops against each other and a solver call per evaluated state is
     * not affordable. And the untap check counts lands, the same thing [Tempo] counts, not every
     * permanent that could make mana.
     *
     * **The land drop still to come is mana too** ([pendingLandDropIsMana]). Without it the window
     * "castable once the tapped lands untap" is read off the battlefield alone, so it slides up by
     * one the moment a land is played: tapped out on four lands with only 5-drops in hand, the four
     * tapped lands are refunded as idle; play the fifth land and the 5-drops fall into the window,
     * the refund (4 × [IDLE_MANA_REFUND]) vanishes, and the land drop scores *below* passing. On our
     * own turn, with a land drop left and a land in hand, that land counts as one more untapped
     * land — so playing an untapped land leaves the window exactly where it was and is worth its
     * full [LAND_UNTAPPED], while a tapland still pays for the mana it withholds.
     *
     * **A card the line itself put into hand never closes the window** ([handAtRoot]). The window is
     * a question about the lands — would their mana have been spent — so it is asked of the hand the
     * decision started from. Asked of the leaf's hand instead, any draw or tutor while tapped out
     * pays for itself twice: the card it fetches lands in the window, the refund (up to
     * tapped × [IDLE_MANA_REFUND]) vanishes, and on four tapped lands that outweighs the card. That
     * is how Goblin Matron declined its own search for a two-drop (2026-10-11 logs, game 3 turn 9).
     * The comparisons the term exists for are untouched: a land drop or a spell leaves the root's
     * other hand cards where they were, and those are exactly the ones still counted. `null` — no
     * root known, a leaf scored on its own — counts the whole hand, as before.
     */
    private fun landSequencing(
        state: GameState,
        projected: ProjectedState,
        playerId: EntityId,
        intents: IntentCatalog,
        pendingLandDropIsMana: Boolean,
        handAtRoot: Set<EntityId>? = null,
        modalLandRoot: ModalLandRoot? = null,
    ): Double {
        // Projection on the battlefield, base state in hand: an animated Mishra's Factory is still a
        // land and a Dryad Arbor is one from the moment it arrives, and only the projection knows.
        // A card in hand has no continuous effects on it, so its printed type line is the answer.
        val lands = projected.getBattlefieldControlledBy(playerId)
            .filter { projected.hasType(it, "LAND") }
        val untapped = lands.count { state.getEntity(it)?.has<TappedComponent>() != true }
        val tapped = lands.size - untapped

        val hand = state.getZone(playerId, Zone.HAND)
            .mapNotNull { state.getEntity(it)?.get<CardComponent>() }

        val pending = if (
            pendingLandDropIsMana &&
            state.activePlayerId == playerId &&
            state.getEntity(playerId)?.get<LandDropsComponent>()?.canPlayLand == true &&
            hand.any { it.isLand }
        ) 1 else 0

        val window = (untapped + pending + 1)..(lands.size + pending)
        val windowManaValues = state.getZone(playerId, Zone.HAND)
            .filter { handAtRoot == null || it in handAtRoot }
            .mapNotNull { state.getEntity(it)?.get<CardComponent>() }
            .filter { !it.isLand }
            .map { it.manaValue } +
            // A spell the line played away as its land face still asks the question. Throwing it
            // away did not make the mana idle — it removed what the mana was for, which the hand
            // count already charges. Without this, playing Pinnacle Monk as a seventh land while
            // tapped out refunded every tapped land (2026-10-12 logs, game 3 turn 13).
            modalLandRoot?.let { root -> root.playedAsLands(projected, playerId).map { root.frontManaValues.getValue(it) } }
                .orEmpty()

        var score = 0.0
        if (tapped > 0 && windowManaValues.none { it in window }) {
            score += tapped * IDLE_MANA_REFUND
        }
        score -= TAPLAND_IN_HAND * hand.count {
            it.isLand && intents.forName(it.name)?.entersTapped == true
        }
        return score
    }

    internal fun permanentValue(
        state: GameState,
        projected: ProjectedState,
        entityId: EntityId,
        card: CardComponent,
        intents: IntentCatalog = IntentCatalog.NONE,
        creatureValuation: CreatureValuation = CreatureValuation.LEGACY,
    ): Double {
        val container = state.getEntity(entityId) ?: return 0.0

        if (projected.isCreature(entityId)) {
            return creatureValue(state, projected, entityId, container, creatureValuation)
        }

        // Non-creature permanents
        if (card.isLand) {
            return if (container.has<TappedComponent>()) LAND_TAPPED else LAND_UNTAPPED
        }

        // What the structural analyzer makes of the card, if this agent has card knowledge on.
        // Phase 6: `intent` is a *floor*, never a ceiling. The shape-based values below are read
        // off live game state (this Aura is attached to something, this O-Ring is holding a card)
        // and know things a static prior cannot; the prior knows what the card *does*. Taking the
        // max keeps both, and guarantees no permanent is ever valued lower than it was before
        // Phase 6 — so a position the AI played correctly on the old numbers still scores at least
        // as well.
        val prior = priorValue(projected, entityId, container, card, intents)

        // Planeswalkers: the flat 4.0 priced a fresh Jace and a Jace at 1 loyalty identically.
        // Loyalty is both the walker's life total and its remaining activations, so it is the one
        // number that has to be in here; the intent prior carries what its abilities *do*.
        if (card.isPlaneswalker) {
            val loyalty = container.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0
            if (creatureValuation.walkersFaceTheNextAttack) {
                // Continuous in loyalty, net of the attack it faces before its controller acts
                // again — see [PlaneswalkerThreat.boardValue].
                val damage = projected.getController(entityId)
                    ?.let { PlaneswalkerThreat.expectedDamage(state, projected, it) } ?: 0
                if (creatureValuation.walkersAsEngines) {
                    val shape = PlaneswalkerThreat.shape(state, entityId, null, engines = intents)
                    return PlaneswalkerThreat.engineBoardValue(shape, prior, loyalty, damage)
                }
                val fuelOnly = if (creatureValuation.grantedLoyaltyAbilities) intents.loyaltyIsOnlyFuel(state, entityId)
                else intents.loyaltyIsOnlyFuel(card.name)
                return PlaneswalkerThreat.boardValue(!fuelOnly, prior, loyalty, damage)
            }
            return maxOf(4.0, prior + loyalty * LOYALTY_VALUE)
        }

        // Auras/equipment attached to something are valuable
        if (container.has<com.wingedsheep.engine.state.components.battlefield.AttachedToComponent>()) {
            return maxOf(1.5, prior)
        }

        // Enchantments/artifacts exiling something (O-Ring effects) are valuable
        if (container.has<com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent>()) {
            return maxOf(2.5, prior)
        }

        // General non-creature permanents. Without card knowledge this is a flat 0.5 regardless of
        // text — a signet, an Oblivion Ring and a Bitterblossom scoring the same number is the
        // blindness Phase 6 exists to remove.
        return maxOf(0.5, prior)
    }

    /**
     * The prior for the permanent [entityId] *as it currently stands* — which is not the same
     * question as what its card can do.
     *
     * [IntentCatalog.forPermanent] answers with one [CardIntent] per reading in force: one for an
     * ordinary permanent, one per **unlocked** door for a Room (CR 709.5). Reading a Room as a
     * whole card would price it the same however many doors are open, which is exactly why the AI
     * would cast a half and then never pay to unlock the other — a special action that moves no
     * evaluated number can never beat passing.
     *
     * The readings combine as a baseline plus what each one adds *over* it, rather than a plain
     * sum. `UNKNOWN.staticPriorValue` is the value of a permanent the analyzer cannot read, and a
     * Room with two blank halves is still one unreadable permanent, not two — summing raw would
     * make it outrank every other unreadable permanent for no reason at all.
     */
    private fun priorValue(
        projected: ProjectedState,
        entityId: EntityId,
        container: ComponentContainer,
        card: CardComponent,
        intents: IntentCatalog,
    ): Double {
        val inForce = intents.forPermanent(container, card.name)
        if (inForce.isEmpty()) return 0.0
        val unreadable = CardIntent.UNKNOWN.staticPriorValue
        return unreadable + inForce.sumOf {
            (intentValue(projected, entityId, it) - unreadable).coerceAtLeast(0.0)
        }
    }

    /**
     * A permanent's [CardIntent] prior, plus the one board-dependent term that is cheap to read.
     *
     * An anthem is the case where a static prior is genuinely not enough: "creatures you control
     * get +1/+1" behind an empty board and the same card behind five creatures are different cards.
     * The creatures' own [creatureValue] already contains the pumped stats, so this counts only
     * what killing the anthem would *take back* — a quarter point per point of P+T handed out,
     * which is a little over half the rate [creatureValue] itself pays for toughness.
     */
    private fun intentValue(
        projected: ProjectedState,
        entityId: EntityId,
        intent: CardIntent,
    ): Double {
        if (intent.anthemBonus <= 0) return intent.staticPriorValue
        val controller = projected.getController(entityId) ?: return intent.staticPriorValue
        val pumped = projected.getBattlefieldControlledBy(controller).count { projected.isCreature(it) }
        return intentValue(intent, pumped)
    }

    /**
     * The same prior for a permanent that is not on the battlefield yet, where the creature count
     * has to be supplied rather than read off the permanent's own controller.
     *
     * [spellValue] is the caller: an anthem *spell* is worth what it will pump, and that is the
     * caster's board, counted before it resolves.
     */
    private fun intentValue(intent: CardIntent, pumpedCreatures: Int): Double =
        intent.staticPriorValue + ANTHEM_VALUE_PER_STAT * intent.anthemBonus * pumpedCreatures

    /**
     * What a spell on the stack would be worth on the battlefield if it resolved, or **null** when
     * that question does not apply — the spell is an instant or a sorcery, or it is a permanent this
     * agent cannot read.
     *
     * A null is not "worth nothing", it is "not answerable this way", and the difference is the
     * whole reason [com.wingedsheep.ai.engine.knowledge.CounterPatience] can exist: an instant's or
     * a sorcery's worth *is* what it does to the board, which the leaf score already simulates, so
     * there is nothing for a prior to add. A permanent spell is the opposite case — the leaf sees it
     * only as one card leaving their hand, and what it will be once it lands is exactly the thing
     * the counterspell is trading for.
     *
     * The valuation is [permanentValue]'s, minus the parts that need a battlefield: no counters, no
     * summoning sickness, no marked damage, no attachment, and printed P/T and keywords rather than
     * projected ones. What is deliberately *kept* is the anthem term, because "creatures you control
     * get +1/+1" cast into an empty board and the same card cast into ten creatures are different
     * cards, and countering the second one is the whole point.
     *
     * @param casterId whose board the spell would join — the anthem term's creature count.
     */
    internal fun spellValue(
        projected: ProjectedState,
        card: CardComponent,
        casterId: EntityId,
        intents: IntentCatalog,
    ): Double? {
        // Lands are played, not cast; a land "spell" is a face the AI should not be pricing here.
        if (!card.isPermanent || card.isLand) return null

        if (card.isCreature) {
            val power = card.baseStats?.basePower ?: return null
            val toughness = card.baseStats?.baseToughness ?: return null
            val keywords = card.baseKeywords.mapTo(mutableSetOf()) { it.name }
            return creatureBodyValue(power, toughness, keywords).coerceAtLeast(0.1)
        }

        val intent = intents.forName(card.name) ?: return null
        val pumped = projected.getBattlefieldControlledBy(casterId).count { projected.isCreature(it) }
        // One card, one reading — [priorValue]'s "baseline plus what each reading adds over it"
        // collapses to a max when there is a single intent, and a Room is not castable as one spell.
        val prior = maxOf(CardIntent.UNKNOWN.staticPriorValue, intentValue(intent, pumped))

        // The same floors [permanentValue] applies, in the same order. A planeswalker's loyalty is
        // its card's starting loyalty, which the stack object does not carry, so it gets the flat
        // floor rather than the loyalty-scaled one; an Aura is priced as attached because resolving
        // is what attaches it.
        if (card.isPlaneswalker) return maxOf(4.0, prior)
        if (card.isAura) return maxOf(1.5, prior)
        return maxOf(0.5, prior)
    }

    /** Board value of one point of P/T an anthem hands to one creature. */
    private const val ANTHEM_VALUE_PER_STAT = 0.25

    /** Board value of one loyalty counter. Only reached with card knowledge on. */
    private const val LOYALTY_VALUE = 0.8

    /**
     * What a creature's **body** is worth — the half of [creatureValue] that reads only stats and
     * keywords, and so is the same question whether the creature is on the battlefield or still a
     * spell on the stack ([spellValue] is the other caller).
     *
     * The keyword bonuses scale with [power] rather than [settledPower] deliberately: a Giant Growth
     * on a flier really is buying evasive damage right now, even though the stats it buys are gone
     * at cleanup — when the flier is in a fight to deal it in, which is the reading
     * [CreatureValuation.expiringStatsNeedAFight] gives `creatureValue`'s caller. Off the battlefield there are no temporary modifications, so the two coincide and
     * the defaults are the printed numbers.
     */
    private fun creatureBodyValue(
        power: Int,
        toughness: Int,
        keywords: Set<String>,
        /** [power] and [toughness] with "until end of turn" modifications taken back off. */
        settledPower: Int = power,
        settledToughness: Int = toughness,
    ): Double {
        // Base: power matters more than toughness for winning
        var value = settledPower * 1.0 + settledToughness * 0.4

        // ── Evasion (the most important combat keyword category) ──
        if (Keyword.FLYING.name in keywords) value += 1.5 + power * 0.3
        if (Keyword.MENACE.name in keywords) value += 0.8
        if (Keyword.FEAR.name in keywords) value += 0.8
        if (Keyword.INTIMIDATE.name in keywords) value += 0.8
        if (Keyword.SHADOW.name in keywords) value += 1.0
        // Landwalk — contextual but often evasion
        if (Keyword.SWAMPWALK.name in keywords) value += 0.6
        if (Keyword.FORESTWALK.name in keywords) value += 0.6
        if (Keyword.ISLANDWALK.name in keywords) value += 0.6
        if (Keyword.MOUNTAINWALK.name in keywords) value += 0.6

        // ── Combat modifiers ──
        if (Keyword.TRAMPLE.name in keywords) value += 0.5 + power * 0.2
        if (Keyword.DEATHTOUCH.name in keywords) value += 2.0  // trades with anything
        if (Keyword.FIRST_STRIKE.name in keywords) value += 1.0 + power * 0.2
        if (Keyword.DOUBLE_STRIKE.name in keywords) value += 2.0 + power * 0.5
        if (Keyword.LIFELINK.name in keywords) value += 0.5 + power * 0.3
        if (Keyword.VIGILANCE.name in keywords) value += 0.8  // attacks without cost
        if (Keyword.PROVOKE.name in keywords) value += 0.5

        // ── Survivability ──
        if (Keyword.INDESTRUCTIBLE.name in keywords) value += 3.0
        if (Keyword.HEXPROOF.name in keywords) value += 1.5
        if (Keyword.SHROUD.name in keywords) value += 1.2
        if (Keyword.PROTECTION.name in keywords) value += 1.0

        // ── Drawbacks ──
        if (Keyword.DEFENDER.name in keywords) value -= power * 0.8 // can't attack, power mostly wasted

        return value
    }

    private fun creatureValue(
        state: GameState,
        projected: ProjectedState,
        entityId: EntityId,
        container: ComponentContainer,
        valuation: CreatureValuation = CreatureValuation.LEGACY,
    ): Double {
        val power = projected.getPower(entityId) ?: 0
        val toughness = projected.getToughness(entityId) ?: 0
        val keywords = projected.getKeywords(entityId)

        // Discount temporary P/T modifications. "Until end of turn" effects expire
        // at cleanup — evaluate using the permanent stats so killing a 2/2 with -2/-2
        // scores better than merely shrinking a 2/3 (which recovers next turn).
        val tempMod = ExpiringStats.temporaryModification(state, entityId)
        val settledPower = (power - tempMod.first).coerceAtLeast(0)
        val settledToughness = (toughness - tempMod.second).coerceAtLeast(0)

        // The keyword bonuses scale with the power this creature will actually hit with. Under
        // [CreatureValuation.expiringStatsNeedAFight] that is the pumped power only while it is in
        // a fight still ahead — a pumped flier at home deals no evasive damage before cleanup.
        val strikingPower = if (valuation.expiringStatsNeedAFight && !ExpiringStats.inFightStillAhead(state, entityId)) {
            settledPower
        } else {
            power
        }

        // Everything that is a property of the card's own body — stats, keywords, DEFENDER — is
        // [creatureBodyValue]; everything below is a property of this permanent in this position.
        var value = creatureBodyValue(strikingPower, toughness, keywords, settledPower, settledToughness)

        // ── Drawbacks ──
        // "Can't attack" hung on the creature by a Pacifism takes away the same thing DEFENDER
        // does: the power. [CreatureValuation.cantAttackCostsPower] is what makes the two spellings
        // agree — see its KDoc for why the flat multiplier they used to disagree over is the wrong
        // shape. DEFENDER itself is already paid inside [creatureBodyValue]; this is the other
        // spelling, and the guard is what keeps it from being charged twice.
        if (valuation.cantAttackCostsPower &&
            projected.cantAttack(entityId) &&
            Keyword.DEFENDER.name !in keywords
        ) {
            value -= power * 0.8 // can't attack, power mostly wasted
        }

        // ── Combat restrictions (Pacifism, "can't block this turn", etc.) ──
        // A creature that can't block has lost its defensive value; one that can't attack can't
        // pressure. Pricing these keeps the AI from stacking redundant "target creature can't
        // block / can't attack" effects on a creature already under that restriction: hitting a
        // fresh target strictly lowers the opponent's board value, re-hitting a restricted one
        // does not. (DEFENDER's can't-attack is already priced just above — don't double-count.)
        if (projected.cantBlock(entityId)) value *= 0.85
        if (!valuation.cantAttackCostsPower &&
            projected.cantAttack(entityId) &&
            Keyword.DEFENDER.name !in keywords
        ) {
            value *= 0.85
        }

        // ── Speed ──
        if ((Keyword.HASTE.name in keywords || projected.canAttackAsThoughHasty(entityId)) &&
            container.has<SummoningSicknessComponent>()) {
            value += 0.5 // haste is most valuable the turn it enters
        }

        // +1/+1 counters represent permanent investment
        val counters = container.get<CountersComponent>()
        val plusCounters = counters?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
        value += plusCounters * 0.5

        // Summoning sickness reduces immediate threat
        if (container.has<SummoningSicknessComponent>() && Keyword.HASTE.name !in keywords &&
            !projected.canAttackAsThoughHasty(entityId)) {
            value *= 0.85
        }

        // Tapped creatures can't block, but tapping is temporary — light discount
        if (container.has<TappedComponent>()) {
            value *= 0.9
        }

        // Damaged creatures are closer to dying — but only until cleanup, which is why
        // [CreatureValuation.markedDamageFadesAtCleanup] switches this off.
        val damage = container.get<DamageComponent>()?.amount ?: 0
        if (!valuation.markedDamageFadesAtCleanup && damage > 0 && toughness > 0) {
            val healthFraction = (toughness - damage).toDouble() / toughness
            value *= (0.5 + 0.5 * healthFraction) // half value at 1 toughness remaining
        }

        return value.coerceAtLeast(0.1)
    }

    /** What a land is worth on the battlefield. Historical constants; [landSequencing] refines them. */
    private const val LAND_UNTAPPED = 0.6
    private const val LAND_TAPPED = 0.3

    /** Exactly the charge [permanentValue] applied, so an idle tapped land nets out to a full land. */
    private const val IDLE_MANA_REFUND = LAND_UNTAPPED - LAND_TAPPED

    /**
     * What holding a land that always enters tapped costs, versus holding one that does not.
     *
     * Small on purpose, and its size is the whole design: it has to break a tie between two land
     * drops that are otherwise identical, and it must never come near [IDLE_MANA_REFUND], or the AI
     * would start dumping taplands on turns where the untapped mana was live — which is the mistake
     * this term exists to stop, in the opposite direction.
     */
    private const val TAPLAND_IN_HAND = 0.1
}

/**
 * What a decision root knew about the modal double-faced cards in its player's hand whose front face
 * is a spell — the cards a line can play as a land and so throw the spell away. Read by
 * [CardAdvantage]'s modal-face charge and [BoardPresence]'s land-sequencing window under
 * `AiProfile.surplusAwareLands`. Own seat only: it reads hand contents.
 *
 * @property frontManaValues each such card's front-face mana value, by entity.
 * @property holdsALand whether the root hand also held a real land card.
 * @property waived the card was the only land on offer and its controller is behind on lands — fewer
 *   in play than both the turns it has taken and the costliest spell in hand.
 */
class ModalLandRoot(
    val frontManaValues: Map<EntityId, Int>,
    val holdsALand: Boolean,
    val waived: Boolean,
) {
    /** The cards of [frontManaValues] that the line put onto [playerId]'s battlefield as a land. */
    fun playedAsLands(projected: ProjectedState, playerId: EntityId): List<EntityId> {
        if (frontManaValues.isEmpty()) return emptyList()
        val battlefield = projected.getBattlefieldControlledBy(playerId).toSet()
        return frontManaValues.keys.filter { it in battlefield && projected.hasType(it, "LAND") }
    }

    companion object {
        private val NONE = ModalLandRoot(emptyMap(), holdsALand = false, waived = false)

        fun of(root: GameState, playerId: EntityId): ModalLandRoot {
            val hand = root.getZone(playerId, Zone.HAND)
                .mapNotNull { id -> root.getEntity(id)?.get<CardComponent>()?.let { id to it } }
            // Off the battlefield a modal DFC shows its front face (CR 712.8a), so a double-faced
            // card in hand that is not a land is one whose land face, if it has one, is the back.
            val fronts = hand.filter { (_, card) -> card.isDoubleFaced && !card.isLand }
                .associate { (id, card) -> id to card.manaValue }
            if (fronts.isEmpty()) return NONE
            val holdsALand = hand.any { (_, card) -> card.isLand }
            val curveTop = hand.filter { (_, card) -> !card.isLand }.maxOfOrNull { it.second.manaValue } ?: 0
            val turnsTaken = root.getEntity(playerId)?.get<PlayerTurnsTakenComponent>()?.count ?: 0
            val projected = root.projectedState
            val landsInPlay = projected.getBattlefieldControlledBy(playerId).count { projected.hasType(it, "LAND") }
            val behind = landsInPlay < minOf(turnsTaken, curveTop)
            return ModalLandRoot(fronts, holdsALand, waived = !holdsALand && behind)
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Card Advantage
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Cards in hand with non-linear scaling. Empty hand (topdeck mode) is heavily
 * penalized. Excess cards have diminishing returns since you'll discard at cleanup.
 *
 * A resource, so a pod folds with [OpponentAggregate.FIELD]. Hands are per player even in a team
 * format (CR 810 pools life and poison, not cards), so a side's value is the sum over its members
 * — two teammates in topdeck mode really is twice the disaster.
 */
object CardAdvantage : BoardFeature {
    /** The historical empty-hand value. See [EvaluationWeights.topdeckPenalty] for why it moves. */
    const val LEGACY_TOPDECK_PENALTY = -3.0

    override fun score(state: GameState, projected: ProjectedState, playerId: EntityId): Double =
        score(state, projected, playerId, LEGACY_TOPDECK_PENALTY)

    fun score(
        state: GameState,
        projected: ProjectedState,
        playerId: EntityId,
        topdeckPenalty: Double,
        landDropIsNotCardLoss: Boolean = false,
        priceLandsInHandAsMana: Boolean = false,
        surplusAwareLands: Boolean = false,
        modalLandRoot: ModalLandRoot? = null,
    ): Double {
        val sides = state.sidesFor(playerId) ?: return 0.0
        // The earmark model only: the mana model already prices a flood on its own schedule.
        val surplus = surplusAwareLands && landDropIsNotCardLoss && !priceLandsInHandAsMana
        val mine = sideHandValue(
            state, projected, sides.mine, topdeckPenalty, landDropIsNotCardLoss, priceLandsInHandAsMana, surplus,
        ) - if (surplus && modalLandRoot != null) {
            modalFaceCharge(state, projected, playerId, topdeckPenalty, modalLandRoot)
        } else {
            0.0
        }
        return sides.against(OpponentAggregate.FIELD) { opponent ->
            mine - sideHandValue(
                state, projected, opponent, topdeckPenalty, landDropIsNotCardLoss, priceLandsInHandAsMana,
                surplus,
            )
        }
    }

    /**
     * [score] with its profile switches bound — the feature `EvaluationWeights.toEvaluator` puts in
     * the composite. A class rather than a lambda for [rootedAt], for the same reason as
     * [BoardPresence.Bound]: the modal-face charge asks what a line cost, so it has to know which
     * spells were in hand where the line started.
     */
    class Bound(
        private val topdeckPenalty: Double,
        private val landDropIsNotCardLoss: Boolean,
        private val priceLandsInHandAsMana: Boolean,
        private val surplusAwareLands: Boolean,
        private val modalLandRoot: ModalLandRoot? = null,
    ) : BoardFeature {
        override fun score(state: GameState, projected: ProjectedState, playerId: EntityId): Double =
            score(
                state, projected, playerId, topdeckPenalty, landDropIsNotCardLoss, priceLandsInHandAsMana,
                surplusAwareLands, modalLandRoot,
            )

        override fun rootedAt(root: GameState, playerId: EntityId): BoardFeature =
            if (!surplusAwareLands) this
            else Bound(
                topdeckPenalty, landDropIsNotCardLoss, priceLandsInHandAsMana, surplusAwareLands,
                ModalLandRoot.of(root, playerId),
            )
    }

    /**
     * What playing a modal double-faced card as its land face cost, on top of what the hand count
     * already moved: the spell's hand value, net.
     *
     * [heldCardCount] earmarks the land drop with a land in hand, and a modal DFC whose front is a
     * spell is not a land there. So with a real land in hand the earmark sits on that land, and
     * playing the modal card instead comes out exactly as card-neutral as playing the basic — the
     * spell it threw away was invisible. 2026-10-12 AI-vs-AI logs (`live`), game 3 turn 1: Drowner
     * of Truth played as its land face with Forest, Mountain and Shattered Landscape in hand.
     *
     * The intended total is one spell's worth (the marginal of [cardValue] at the leaf's count),
     * waived — zero — when the card was the only land on offer and its controller is behind on
     * lands, because that is the spot the land face is printed for. The hand count gets there on
     * its own in one case and overshoots in another, so this charges the difference:
     *  - another land was in hand: the count moved nothing, so charge the spell;
     *  - the only land, waived: the count already charged the spell as a lost card, so refund it;
     *  - the only land, not waived: the count already charged it, so add nothing.
     */
    private fun modalFaceCharge(
        state: GameState,
        projected: ProjectedState,
        playerId: EntityId,
        topdeckPenalty: Double,
        root: ModalLandRoot,
    ): Double {
        val played = root.playedAsLands(projected, playerId).size
        if (played == 0) return 0.0
        val sign = when {
            root.holdsALand -> 1.0
            root.waived -> -1.0
            else -> return 0.0
        }
        val held = heldCardCount(state, playerId, landDropIsNotCardLoss = true, surplusAwareLands = true)
        return sign * (cardValue(held + played, topdeckPenalty) - cardValue(held, topdeckPenalty))
    }

    private fun sideHandValue(
        state: GameState,
        projected: ProjectedState,
        side: List<EntityId>,
        topdeckPenalty: Double,
        landDropIsNotCardLoss: Boolean,
        priceLandsInHandAsMana: Boolean,
        surplusAwareLands: Boolean,
    ): Double =
        side.sumOf {
            handValue(
                state, projected, it, topdeckPenalty, landDropIsNotCardLoss, priceLandsInHandAsMana,
                surplusAwareLands,
            )
        }

    /**
     * What one player's hand is worth.
     *
     * Two models, and [priceLandsInHandAsMana] picks between them. Off, the historical one:
     * [cardValue] over a count, with [heldCardCount]'s earmark subtracting one land per land drop.
     * On, the curve prices **business** and lands are priced separately and lower — see
     * [priceLandsInHandAsMana]'s own KDoc on `AiProfile` for why that is the better shape.
     *
     * On, [landDropIsNotCardLoss] is not consulted at all. The earmark exists only to force the land
     * drop to be card-neutral, and under this model it does not need forcing: a land is simply worth
     * less in hand than on the battlefield, so playing it is positive by construction. Two mechanisms
     * for one fact would be one too many, and the newer one subsumes the older.
     */
    private fun handValue(
        state: GameState,
        projected: ProjectedState,
        playerId: EntityId,
        topdeckPenalty: Double,
        landDropIsNotCardLoss: Boolean,
        priceLandsInHandAsMana: Boolean,
        surplusAwareLands: Boolean = false,
    ): Double {
        if (!priceLandsInHandAsMana) {
            return cardValue(heldCardCount(state, playerId, landDropIsNotCardLoss, surplusAwareLands), topdeckPenalty)
        }
        val hand = state.getZone(playerId, Zone.HAND)
        val lands = hand.count { state.getEntity(it)?.get<CardComponent>()?.isLand == true }
        return cardValue(hand.size - lands, topdeckPenalty) +
            landsInHandValue(landsInPlay(state, projected, playerId), lands)
    }

    /**
     * What the lands in a hand are worth, given how much mana their controller already has.
     *
     * A land is not worth a fixed amount: **it is worth a lot when you are short of mana and almost
     * nothing when you are already rich.** The second land of the game makes every card in your hand
     * castable a turn sooner; the eleventh is a card you would happily pitch. That is the same shape
     * [Tempo] already prices on the battlefield side (2.0 a land through the third, 1.2 through the
     * sixth, 0.4 after), so this schedule is that curve read one zone earlier rather than a new
     * guess.
     *
     * Each land in hand is priced at the count it would actually arrive at — the first at today's
     * land count, the second as if the first had been played, and so on. That is what makes a hand of
     * seven lands on turn one score as the flood it is instead of seven times the first one's value,
     * without needing a second rule about hand contents.
     */
    private fun landsInHandValue(landsInPlay: Int, landsInHand: Int): Double =
        (0 until landsInHand).sumOf { landInHandValue(landsInPlay + it) }

    /**
     * What one more land is worth to a player who already has [landsAvailable] of them.
     *
     * Bounded, at every rung, by numbers already in the evaluator rather than chosen freely — which
     * is what makes a schedule defensible where a single constant was not:
     *
     *  - **Always below what the same land is worth on the battlefield**, so playing it is a gain at
     *    every stage and never needs [landDropIsNotCardLoss] to force it. A land drop pays
     *    `BoardPresence` 0.6 × 1.5 = 0.9 plus a [Tempo] marginal of 0.6 × the numbers above:
     *    **+2.1 early, +1.62 mid, +1.14 once mana-rich**, against 0.9 / 0.5 / 0.2 here.
     *  - **Below a real card's marginal on [cardValue]**, whose working band is 0.8–1.5, so even the
     *    top rung never lets a land pass for business.
     *
     * Deliberately *not* zero at any rung, which is what the earmark made the next land drop. A land
     * in hand is a card: it is a guaranteed drop next turn, and an opponent choosing what to strip
     * would take it — a Duress that costs its victim nothing is not a model anyone would defend.
     */
    private fun landInHandValue(landsAvailable: Int): Double = when {
        landsAvailable <= 2 -> 0.9 // still hitting drops; the next land is most of the game
        landsAvailable <= 5 -> 0.5 // curve filled out, more mana still buys turns
        else -> 0.2 // mana-rich: a card you would pitch, but not one worth nothing
    }

    /** Projected, per the battlefield-filter rule: an animated Mishra's Factory is still a land. */
    private fun landsInPlay(state: GameState, projected: ProjectedState, playerId: EntityId): Int =
        projected.getBattlefieldControlledBy(playerId).count { projected.hasType(it, "LAND") }

    /** The top rung, exposed for the test that pins the schedule against [Tempo]'s own curve. */
    internal fun landInHandValueAt(landsAvailable: Int): Double = landInHandValue(landsAvailable)

    /**
     * How many cards in [playerId]'s hand are *cards* rather than mana waiting to be tapped.
     *
     * With [landDropIsNotCardLoss] off this is the hand size, which is what every published number
     * before 2026-08-07 was measured on.
     *
     * On, it holds back one land per unused land drop. Playing a land does not spend a card — it
     * relocates one, from a zone this feature prices to the battlefield that [Tempo] and
     * [BoardPresence] price. Charging the move as card loss made it a strict debit: the land drop
     * paid 1.5–3.0 here to buy 2.1 there, and at the empty-hand cliff it did not cover the
     * difference, which is why the AI would sit on its last land for the rest of the game
     * (`sequencing-02`). Holding the drop back on *both* sides of the count makes the move exactly
     * card-neutral, so [Tempo] and [BoardPresence] decide it alone.
     *
     * `LandDropsComponent.remaining` rather than the enumerator's `canPlayLand`, which also demands
     * main phase / empty stack / your turn. Land drops reset for every player at cleanup, so
     * `remaining` reads 1 for whoever is not the active player too — the earmark is symmetric and
     * survives a turn boundary instead of flickering on and off within one. It also misses the
     * `GrantAdditionalLandDrop` statics that `LandDropUtils` adds on top, which needs a
     * `CardRegistry` this feature does not have: under an Exploration the second drop is still
     * charged as card loss, which is the old behaviour rather than a new error.
     *
     * ## Known limitation: the earmarked land is priced at zero, and it is not worth zero
     *
     * Subtracting it outright buys land-drop neutrality at the wrong end. The transition really is
     * neutral — that is the fix, and it works — but both sides of it are priced as the *empty-hand
     * disaster* rather than as "a resource in hand". So at `concave-hand-2`'s `-2.0`, a hand of one
     * Forest with the drop unused scores `-2.0`, exactly what an empty hand scores and 3.0 below a
     * hand holding one Grizzly Bears; `[Forest, Bears]` scores the same as `[Bears]` alone.
     *
     * A land in hand is a card and has value. The sharpest statement of that: an opponent handed the
     * choice of what to strip would happily take it, and this feature prices that Duress at **zero**.
     * The same zero is what made every card *drawn* inside a simulation free when the library was all
     * basic lands — see `PuzzleRunner.stockLibraries` for the day that cost.
     *
     * Only **one** land is affected (`minOf(lands, drops)`), so a hand full of lands is still counted;
     * this is not "lands are worthless", it is "the next land drop is worthless while it is still in
     * hand". Fixing it means restoring neutrality at the land's *realized* value rather than at zero,
     * and that is a new flag with its own attribution column and arena run — not an edit to a term
     * that has already shipped and been measured.
     *
     * ## [surplusAwareLands]: a flood is not a full hand
     *
     * The opposite end of the same count. Past the earmarked land every land was a whole card, so
     * three lands in hand scored like three spells and a land returned to hand was a card gained even
     * when two others were already queued for the next drops. With the flag on, a land past the drops
     * of this turn and the next [LOOKAHEAD_LAND_DROPS] ([surplusLands]) counts [SURPLUS_LAND_CARD] of
     * a card. A land drop stays exactly card-neutral: it spends one drop and one land, so the surplus
     * count does not move.
     */
    private fun heldCardCount(
        state: GameState,
        playerId: EntityId,
        landDropIsNotCardLoss: Boolean,
        surplusAwareLands: Boolean = false,
    ): Double {
        val hand = state.getZone(playerId, Zone.HAND)
        if (!landDropIsNotCardLoss) return hand.size.toDouble()
        val drops = state.getEntity(playerId)?.get<LandDropsComponent>()?.remaining ?: 0
        val lands = hand.count { state.getEntity(it)?.get<CardComponent>()?.isLand == true }
        val earmarked = if (drops <= 0) 0 else minOf(lands, drops)
        if (!surplusAwareLands) return (hand.size - earmarked).toDouble()
        return hand.size - earmarked - surplusLands(lands, drops) * (1.0 - SURPLUS_LAND_CARD)
    }

    /**
     * How many of [playerId]'s lands in hand no land drop in sight reaches: more than the drops left
     * this turn plus [lookahead] turns' worth after it.
     *
     * One land a turn is the cap, so a land past it is not mana the next turn or two can use — it
     * waits behind the others. Under `AiProfile.surplusAwareLands` the hand counts such a land as
     * [SURPLUS_LAND_CARD] of a card rather than a whole one ([LOOKAHEAD_LAND_DROPS]), and
     * `DecisionResponder` uses the one-turn reading to tell a free bounce of its own land from one
     * that costs a land drop.
     */
    fun surplusLands(state: GameState, playerId: EntityId, lookahead: Int = LOOKAHEAD_LAND_DROPS): Int {
        val drops = state.getEntity(playerId)?.get<LandDropsComponent>()?.remaining ?: 0
        val lands = state.getZone(playerId, Zone.HAND)
            .count { state.getEntity(it)?.get<CardComponent>()?.isLand == true }
        return surplusLands(lands, drops, lookahead)
    }

    private fun surplusLands(landsInHand: Int, dropsLeft: Int, lookahead: Int = LOOKAHEAD_LAND_DROPS): Int =
        (landsInHand - maxOf(dropsLeft, 0) - lookahead).coerceAtLeast(0)

    /** Turns of land drops after this one that a land in hand still counts for: "the next turn or two". */
    const val LOOKAHEAD_LAND_DROPS = 2

    /**
     * What a surplus land in hand is worth, as a fraction of a card. Near zero rather than zero: a
     * flood still has uses (discard fodder, a replacement for a destroyed land), but it is not a card
     * that does anything this game is likely to see.
     */
    const val SURPLUS_LAND_CARD = 0.1

    /**
     * The hand curve over a fractional count — piecewise linear, and equal to the historical integer
     * curve at every whole number, so a profile without `surplusAwareLands` scores exactly as before.
     * Below one card it runs straight from the empty-hand value to the first card's.
     */
    private fun cardValue(count: Double, topdeckPenalty: Double): Double = when {
        count <= 0.0 -> topdeckPenalty
        count <= 1.0 -> topdeckPenalty + count * (1.0 - topdeckPenalty)
        count <= 3.0 -> 1.0 + (count - 1) * 1.5
        count <= 7.0 -> 4.0 + (count - 3) * 0.8
        else -> 7.2 + (count - 7) * 0.2 // past 7 cards, you're discarding anyway
    }

    private fun cardValue(count: Int, topdeckPenalty: Double): Double = cardValue(count.toDouble(), topdeckPenalty)
}

// ═════════════════════════════════════════════════════════════════════════════
// Threat Assessment
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Measures how close the opponent is to killing you with creatures on board.
 * A negative score means they have enough power to threaten lethal.
 *
 * This is distinct from LifeDifferential — it measures *potential* damage, not
 * actual life totals. An opponent at 5 life with 15 power on board is more
 * dangerous than one at 5 life with 1 power.
 *
 * The whole race calculation runs once per opposing side and folds with
 * [OpponentAggregate.THREAT]: this is the feature that is *about* who is going to kill whom, so the
 * matchup the AI is losing has to dominate. A side's clock is the sum of its members' power
 * (CR 805.10 — teammates attack together) and its life is the *lowest* pool on that side, since a
 * team dies when any of its pools does.
 */
object ThreatAssessment : BoardFeature {
    /**
     * The scale the race is measured on: what a full turn of urgency — a clock that kills next
     * turn — is worth before the 2.0 / 1.5 slope.
     *
     * The historical race term is linear in **turns**: `(theirClock − ourClock) × 2.0` when we are
     * faster and `× 1.5` when we are not, with a side that has no attacker handed the sentinel
     * `99.0` turns. Two things are wrong with that and they are the same thing.
     *
     * The sentinel goes into the subtraction, so an empty board facing one 2/2 scores
     * `(99 − 10) × 1.5` — **−160 once weighted**, on a scale where a point of life is worth 1.0 and
     * that 2/2 is worth 3.6 of board presence. And even without the sentinel, a turn is a turn: the
     * difference between dying on turn 10 and turn 20 counts for as much as the difference between
     * dying next turn and the turn after. It is not worth as much. A lot happens in ten turns, and
     * almost nothing the evaluator can see reaches that far.
     *
     * So [discountedRaceClock] scores the race in **urgency** — `power / life`, the share of a life
     * total removed per turn — rather than in turns. Urgency is `1 / turns`, so it discounts a
     * distant clock exactly the way distance should: a 1-turn clock is 1.0, three turns 0.33, ten
     * turns 0.1, and no creatures at all is **0.0** with no sentinel and no special case. It also
     * makes the term linear in *power*, which the turns form got backwards — going 2 power → 4 was
     * worth 7.5 there and 4 → 8 only 3.75, though each adds the same damage.
     *
     * Urgency is capped at 1.0 — nothing kills you more than dead this turn — so the whole term
     * lands inside +8/−6 raw at this scale, alongside the +8/−10 lethal bonuses below it rather
     * than fifteen times them.
     *
     * **4.0 is measured, not derived.** Near a symmetric clock of `T` turns the urgency form is the
     * turns form divided by `T²`, so the value that would reproduce the old term at a typical
     * 3-turn race is ~10 — and 10 is *too strong*, because the old term was double-counting: it
     * charges for life that [LifeDifferential] already prices at 1.0 a point and for power that
     * `BoardPresence` already prices. Swept on the 83-puzzle suite against `production`:
     *
     * | scale | 0 | 2 | 4 | 6 | 10 | 15 | 20 | unbounded (today) |
     * |---|---|---|---|---|---|---|---|---|
     * | passes | 69 | 70 | **71** | 70 | 68 | 68 | 69 | 71 |
     *
     * A real optimum rather than an edge: 0 is the control that deletes the term, and losing two
     * puzzles to it is what says the race is worth scoring at all.
     */
    const val RACE_URGENCY_SCALE = 4.0

    /**
     * How much of [life] a side removes per turn, in `[0, 1]`. `0.0` for a side with no attacker —
     * which is the whole point: the no-clock case needs no sentinel. See [RACE_URGENCY_SCALE].
     */
    private fun urgency(life: Int, power: Int): Double =
        if (power <= 0 || life <= 0) 0.0 else minOf(power.toDouble() / life, 1.0)

    /**
     * What a race lead of [lead] urgency is worth, unweighted: the [RACE_URGENCY_SCALE] with the
     * same 2.0 / 1.5 slopes [score] applies. Public so a consumer pricing one side of the race on
     * its own — `CombatAdvisor`'s follow-up under `AiProfile.attacksReadTheRace` — uses this scale
     * rather than a second one.
     */
    fun raceLeadValue(lead: Double): Double = lead * RACE_URGENCY_SCALE * if (lead > 0) 2.0 else 1.5

    /** Score with the historical turns-linear race and its `99.0` sentinel. */
    override fun score(state: GameState, projected: ProjectedState, playerId: EntityId): Double =
        score(state, projected, playerId, discountedRaceClock = false)

    /** The pre-[com.wingedsheep.ai.engine.AiProfile.evasionAfterAttacking] signature. */
    fun score(
        state: GameState,
        projected: ProjectedState,
        playerId: EntityId,
        discountedRaceClock: Boolean,
    ): Double = score(state, projected, playerId, discountedRaceClock, evasionAfterAttacking = false)

    /** The pre-[CreatureValuation.expiringStatsNeedAFight] signature. */
    fun score(
        state: GameState,
        projected: ProjectedState,
        playerId: EntityId,
        discountedRaceClock: Boolean,
        evasionAfterAttacking: Boolean,
    ): Double = score(
        state, projected, playerId, discountedRaceClock, evasionAfterAttacking, expiringStatsNeedAFight = false,
    )

    /**
     * @param discountedRaceClock score the race in urgency rather than in turns, so a distant clock
     *   is discounted and an absent one is zero. `EvaluationWeights.toEvaluator` binds this from
     *   [com.wingedsheep.ai.engine.AiProfile.discountedRaceClock]; see [RACE_URGENCY_SCALE].
     * @param evasionAfterAttacking keep tapped attackers in evasive power and stop counting a
     *   can't-block flier as air cover; see
     *   [com.wingedsheep.ai.engine.AiProfile.evasionAfterAttacking].
     * @param expiringStatsNeedAFight read an until-end-of-turn P/T change only on a creature in a
     *   fight still ahead; see [CreatureValuation.expiringStatsNeedAFight]. Every term here is about
     *   the *next* combat, and for a creature not already in this one, that combat comes after
     *   cleanup has taken the bonus away.
     */
    fun score(
        state: GameState,
        projected: ProjectedState,
        playerId: EntityId,
        discountedRaceClock: Boolean,
        evasionAfterAttacking: Boolean,
        expiringStatsNeedAFight: Boolean,
    ): Double {
        val fight = expiringStatsNeedAFight
        val sides = state.sidesFor(playerId) ?: return 0.0

        val myLife = sideLife(state, sides.mine)

        // Calculate attack potential (power of untapped, non-sick creatures)
        val myAttackPower = attackPotential(state, projected, sides.mine, fight)

        // Calculate defense capability (total toughness of untapped creatures that can block)
        val myDefense = defensePotential(state, projected, sides.mine, fight)

        return sides.against(OpponentAggregate.THREAT) { opponent ->
            val theirLife = sideLife(state, opponent)
            val theirAttackPower = attackPotential(state, projected, opponent, fight)
            val theirDefense = defensePotential(state, projected, opponent, fight)

            // Score: positive if we're the faster clock
            var score = 0.0

            // Being closer to killing them is good.
            if (discountedRaceClock) {
                // Urgency, not turns — see [RACE_URGENCY_SCALE]. Same slopes, same sign, and the
                // no-clock sentinel simply does not exist on this path.
                val lead = urgency(theirLife, myAttackPower) - urgency(myLife, theirAttackPower)
                score += raceLeadValue(lead)
            } else {
                // How many turns until opponent kills us (if we can't block)
                val turnsUntilDead =
                    if (theirAttackPower > 0) myLife.toDouble() / theirAttackPower else 99.0
                val turnsUntilWeKill =
                    if (myAttackPower > 0) theirLife.toDouble() / myAttackPower else 99.0
                if (turnsUntilWeKill < turnsUntilDead) {
                    score += (turnsUntilDead - turnsUntilWeKill) * 2.0
                } else {
                    score -= (turnsUntilWeKill - turnsUntilDead) * 1.5
                }
            }

            // Lethal on board next turn is very valuable
            if (hasLethalOnBoard(myAttackPower, theirLife, theirDefense)) score += 8.0
            if (hasLethalOnBoard(theirAttackPower, myLife, myDefense)) score -= 10.0

            // Evasive damage (flying power they can't block)
            val theirEvasivePower = evasivePower(state, projected, opponent, sides.mine, evasionAfterAttacking, fight)
            val myEvasivePower = evasivePower(state, projected, sides.mine, opponent, evasionAfterAttacking, fight)
            score += (myEvasivePower - theirEvasivePower) * 0.5

            score
        }
    }

    /**
     * Whether an attacking side with [attackPower] kills a defending side at [life] behind
     * [defense] **this coming combat** — power enough to finish it, and more than the defender can
     * put in the way.
     *
     * Extracted so the two lethal bonuses in [score] and [lethalOnBoardAgainst] are the same claim
     * rather than the same expression written three times.
     */
    private fun hasLethalOnBoard(attackPower: Int, life: Int, defense: Int): Boolean =
        attackPower >= life && attackPower > defense

    /**
     * Whether some opposing side can kill [playerId] with the board exactly as it stands.
     *
     * The same claim [score]'s `−10.0` term makes, exposed because one consumer needs it as a
     * **veto** rather than as a number: [com.wingedsheep.ai.engine.knowledge.RemovalPatience] must
     * never talk the AI into holding a removal spell on a turn where doing nothing loses the game,
     * and "the board score will outvote the discount" is an argument about magnitudes rather than a
     * guarantee. This makes it one.
     *
     * Folded as "any opposing side", not [OpponentAggregate.THREAT]: in a pod, one player having
     * lethal on us is lethal on us however the other matchups are going.
     */
    fun lethalOnBoardAgainst(state: GameState, projected: ProjectedState, playerId: EntityId): Boolean {
        val sides = state.sidesFor(playerId) ?: return false
        val myLife = sideLife(state, sides.mine)
        val myDefense = defensePotential(state, projected, sides.mine)
        return sides.opponents.any { opponent ->
            hasLethalOnBoard(attackPotential(state, projected, opponent), myLife, myDefense)
        }
    }

    /** A side dies when its weakest life pool does, so the clock runs against the minimum. */
    private fun sideLife(state: GameState, side: List<EntityId>): Int =
        state.lifePoolsOf(side).minOrNull() ?: 20

    private fun attackPotential(
        state: GameState,
        projected: ProjectedState,
        side: List<EntityId>,
        needAFight: Boolean = false,
    ): Int {
        // Don't filter by TappedComponent — tapped creatures untap on the next
        // turn and can attack again. Filtering them out massively penalizes the
        // post-combat state (where our creatures are tapped from attacking),
        // making the AI think attacking reduced its clock to zero.
        return side.sumOf { playerId ->
            projected.getBattlefieldControlledBy(playerId)
                .filter { entityId ->
                    projected.isCreature(entityId) &&
                        !projected.cantAttack(entityId) &&
                        (state.getEntity(entityId)?.has<SummoningSicknessComponent>() != true ||
                            projected.hasKeyword(entityId, Keyword.HASTE) || projected.canAttackAsThoughHasty(entityId))
                }
                .sumOf { ExpiringStats.power(state, projected, it, needAFight) }
        }
    }

    private fun defensePotential(
        state: GameState,
        projected: ProjectedState,
        side: List<EntityId>,
        needAFight: Boolean = false,
    ): Int {
        return side.sumOf { playerId ->
            projected.getBattlefieldControlledBy(playerId)
                .filter { entityId ->
                    projected.isCreature(entityId) &&
                        !projected.cantBlock(entityId) &&
                        state.getEntity(entityId)?.has<TappedComponent>() != true
                }
                .sumOf { ExpiringStats.toughness(state, projected, it, needAFight) }
        }
    }

    /**
     * Power of creatures with flying/evasion that the defending side can't block.
     *
     * The defenders' fliers are counted only while untapped — a tapped one cannot block — but the
     * attackers' were too, which is the mistake [attackPotential] documents avoiding: the state an
     * attack plan is scored on is the post-combat one, where every flier that just attacked is
     * tapped, so attacking with an unblockable flier erased its own evasive power from the score.
     *
     * The same post-combat state exposes a second miscount: a flier that *can't block* (Aesthir
     * Glider) was counted as the defenders' air cover while it stood untapped at home, so sending it
     * in looked like it opened the skies to the opponent's fliers. It never covered them.
     *
     * [afterAttacking] fixes both; off, the historical reading.
     */
    private fun evasivePower(
        state: GameState,
        projected: ProjectedState,
        attackers: List<EntityId>,
        defenders: List<EntityId>,
        afterAttacking: Boolean,
        needAFight: Boolean = false,
    ): Int {
        val defenderHasFlyers = defenders.any { defenderId ->
            projected.getBattlefieldControlledBy(defenderId).any { entityId ->
                projected.isCreature(entityId) &&
                    state.getEntity(entityId)?.has<TappedComponent>() != true &&
                    !(afterAttacking && projected.cantBlock(entityId)) &&
                    (Keyword.FLYING.name in projected.getKeywords(entityId) ||
                        Keyword.REACH.name in projected.getKeywords(entityId))
            }
        }

        if (defenderHasFlyers) return 0 // they can block flyers

        return attackers.sumOf { attackerId ->
            projected.getBattlefieldControlledBy(attackerId)
                .filter { entityId ->
                    projected.isCreature(entityId) &&
                        Keyword.FLYING.name in projected.getKeywords(entityId) &&
                        (afterAttacking || state.getEntity(entityId)?.has<TappedComponent>() != true) &&
                        (state.getEntity(entityId)?.has<SummoningSicknessComponent>() != true ||
                            projected.hasKeyword(entityId, Keyword.HASTE) || projected.canAttackAsThoughHasty(entityId))
                }
                .sumOf { ExpiringStats.power(state, projected, it, needAFight) }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Tempo
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Measures mana development and efficiency. Having more available mana means
 * casting bigger spells and holding up responses. Counts lands (not mana pool)
 * since land count is the durable measure of mana development.
 *
 * A resource, so a pod folds with [OpponentAggregate.FIELD]. Mana is *not* pooled by team — CR 810
 * shares life and poison, never mana — so a side's value is the sum of its members' own land
 * curves, evaluated one player at a time.
 */
object Tempo : BoardFeature {
    override fun score(state: GameState, projected: ProjectedState, playerId: EntityId): Double {
        val sides = state.sidesFor(playerId) ?: return 0.0

        // Each land ahead is worth about 1.5 points (mana advantage = tempo)
        // But the first few lands matter more than later ones
        val mine = sideLandValue(state, projected, sides.mine)
        return sides.against(OpponentAggregate.FIELD) { opponent ->
            mine - sideLandValue(state, projected, opponent)
        }
    }

    private fun sideLandValue(state: GameState, projected: ProjectedState, side: List<EntityId>): Double =
        side.sumOf { landValue(countLands(state, projected, it)) }

    private fun countLands(state: GameState, projected: ProjectedState, playerId: EntityId): Int {
        return projected.getBattlefieldControlledBy(playerId).count { entityId ->
            val card = state.getEntity(entityId)?.get<CardComponent>()
            card?.isLand == true
        }
    }

    /**
     * Exposed so `CardAdvantageLandDropTest` can pin `CardAdvantage`'s land-in-hand schedule against
     * this curve directly, rather than against remembered copies of these numbers.
     */
    internal fun landValueAt(count: Int): Double = landValue(count)

    private fun landValue(count: Int): Double = when {
        count <= 0 -> -5.0  // no mana is terrible
        count <= 3 -> count * 2.0  // early mana is critical
        count <= 6 -> 6.0 + (count - 3) * 1.2  // mid-game mana
        else -> 9.6 + (count - 6) * 0.4  // diminishing returns for excess mana
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// One-shot mana
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Mana banked in a permanent that **sacrifices itself to make it** — Treasure, Gold, a Banana, an
 * Eldrazi Spawn, Lotus Petal. [com.wingedsheep.ai.engine.AiProfile.oneShotManaIsMana].
 *
 * [Tempo] counts lands and nothing else, and [BoardPresence] prices a non-land, non-creature
 * permanent by its intent prior — which reads a Treasure's mana ability as `RAMP`, the tag of a mana
 * rock, worth a flat 0.7 board points however many lands stand beside it and whatever the hand
 * holds. Without card knowledge, or with no definition registered for the token, it is the flat 0.5
 * of an unreadable permanent. Either way nothing scored a Treasure as *mana*: a 2026-10-31 review
 * (#4116) found a choose-N mode that makes Treasure moving the score by nothing worth having, and
 * Gala Greeters' "create a tapped Treasure" lost to "gain 2 life" at 20 life with a four-drop in
 * hand one land short.
 *
 * Two parts, on [Tempo]'s scale (and [Tempo]'s weight), so they are measured in land drops:
 *
 *  - **One mana, once.** Each source is worth [ONE_SHOT_SHARE] of the next land drop on its
 *    controller's land curve — the same marginal [Tempo] would pay for that land, capped at
 *    [MARGINAL_CAP] so a Treasure beside no lands is not priced as the first land. A land makes
 *    that mana every turn; a Treasure makes it once, so it is meaningfully less, and it shrinks as
 *    the land count climbs exactly as a land drop does.
 *  - **It closes a gap.** When a nonland spell in the controller's own hand costs more than the
 *    lands (plus a land drop still in hand) can make, but no more than those lands plus every
 *    one-shot source, the sources are the mana that casts it a turn early — worth one more
 *    [ONE_SHOT_SHARE] of the marginal land, once. Mana value against a land count, the same
 *    approximation [BoardPresence]'s land sequencing makes, and for the same reason: no solver run
 *    per evaluated state. **Own seat only**: it reads the hand.
 *
 * Read off the printed abilities of the permanent's definition and off abilities granted to it
 * (a `CreateTokenEffect` token's abilities are grants), so a token with no registered definition
 * still counts when its ability was granted. A permanent that has lost all its abilities, and a
 * land (which [Tempo] already counts), are not sources. Creatures are: an Eldrazi Spawn's 0/1 body
 * is priced by [BoardPresence], the mana it banks is priced here.
 *
 * Clue, Food and Blood are not here. They also sacrifice themselves for a resource, but the
 * resource is a card or life — priced by [CardAdvantage]'s and [LifeDifferential]'s curves, net of
 * an activation cost in mana — and a term that put all three on one scale would be guessing an
 * exchange rate between currencies the evaluator already prices separately.
 */
class OneShotMana(private val intents: IntentCatalog) : BoardFeature {

    /** Whether the definition [id] prints a sacrifice-self mana ability. Definitions never change. */
    private val printed = java.util.concurrent.ConcurrentHashMap<String, Boolean>()

    override fun score(state: GameState, projected: ProjectedState, playerId: EntityId): Double {
        val sides = state.sidesFor(playerId) ?: return 0.0
        val granted = state.grantedActivatedAbilities
            .filter { isOneShotMana(it.ability) }
            .mapTo(HashSet()) { it.entityId }
        val mine = sideValue(state, projected, sides.mine, playerId, granted)
        return sides.against(OpponentAggregate.FIELD) { opponent ->
            mine - sideValue(state, projected, opponent, null, granted)
        }
    }

    private fun sideValue(
        state: GameState,
        projected: ProjectedState,
        side: List<EntityId>,
        ownSeat: EntityId?,
        granted: Set<EntityId>,
    ): Double = side.sumOf { player ->
        val battlefield = projected.getBattlefieldControlledBy(player)
        val lands = battlefield.count { projected.hasType(it, "LAND") }
        val sources = battlefield.count { isSource(state, projected, it, granted) }
        if (sources == 0) 0.0
        else value(lands, sources) + if (player == ownSeat) gapValue(state, player, lands, sources) else 0.0
    }

    private fun isSource(state: GameState, projected: ProjectedState, id: EntityId, granted: Set<EntityId>): Boolean {
        if (projected.hasType(id, "LAND") || projected.hasLostAllAbilities(id)) return false
        if (id in granted) return true
        val card = state.getEntity(id)?.get<CardComponent>() ?: return false
        return printed.getOrPut(card.cardDefinitionId) {
            intents.definition(card.cardDefinitionId)?.script?.activatedAbilities
                ?.any(::isOneShotMana) == true
        }
    }

    private fun gapValue(state: GameState, player: EntityId, lands: Int, sources: Int): Double {
        val hand = state.getZone(player, Zone.HAND).mapNotNull { state.getEntity(it)?.get<CardComponent>() }
        val reach = lands + if (hand.any { it.isLand }) 1 else 0
        val closes = hand.any { !it.isLand && it.manaValue > reach && it.manaValue <= reach + sources }
        return if (closes) ONE_SHOT_SHARE * marginal(lands) else 0.0
    }

    companion object {
        /** A Treasure is this share of a land drop: the land's mana every turn against once. */
        const val ONE_SHOT_SHARE = 0.5

        /**
         * The steepest land drop a source is priced against. [Tempo]'s curve charges seven points
         * for the first land (no mana is "terrible"), which is the price of having no land, not
         * of a mana you would spend once.
         */
        private const val MARGINAL_CAP = 2.0

        /** [sources] one-shot mana sources beside [lands] lands, before any hand is read. */
        fun value(lands: Int, sources: Int): Double =
            (0 until sources).sumOf { ONE_SHOT_SHARE * marginal(lands + it) }

        private fun marginal(lands: Int): Double =
            (Tempo.landValueAt(lands + 1) - Tempo.landValueAt(lands)).coerceAtMost(MARGINAL_CAP)

        /** A mana ability that sacrifices its own source: one mana, once. */
        fun isOneShotMana(ability: com.wingedsheep.sdk.scripting.ActivatedAbility): Boolean =
            ability.isManaAbility && sacrificesSelf(ability.cost)

        private fun sacrificesSelf(cost: com.wingedsheep.sdk.scripting.AbilityCost): Boolean = when (cost) {
            com.wingedsheep.sdk.scripting.AbilityCost.SacrificeSelf -> true
            is com.wingedsheep.sdk.scripting.AbilityCost.Composite -> cost.costs.any(::sacrificesSelf)
            else -> false
        }
    }
}
