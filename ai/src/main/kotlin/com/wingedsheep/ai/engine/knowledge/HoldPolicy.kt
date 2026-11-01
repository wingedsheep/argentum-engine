package com.wingedsheep.ai.engine.knowledge

import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.ai.engine.evaluation.SynergyPresence
import com.wingedsheep.ai.engine.evaluation.EvaluationWeights
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.combat.BlockingComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.TargetsComponent
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.ModalEffect

/**
 * "Is this the window for this card?" — the timing half of Phase 6.
 *
 * A one-ply evaluator has no notion of a window. It scores the board right after the spell
 * resolves, and a combat trick cast in your own main phase scores exactly as well there as one cast
 * after blockers, because `ThreatAssessment` reads the pumped power either way. The result is an AI
 * that dumps its instants at the first opportunity.
 *
 * Before Phase 6 the only correction was one hardcoded line in `Strategist`: `passScore - 1.5` on
 * the opponent's end step, applied to *every* candidate. That is both too blunt — it *encourages*
 * dumping a pump that is about to wear off in cleanup, exactly as much as it encourages a removal
 * spell that would otherwise rot in hand — and too narrow, since it says nothing about our own main
 * phase, which is where the mistake actually happens. This replaces it with a per-card verdict
 * driven by [CardIntent].
 *
 * A [TimingVerdict.Adjust] is in the board score's own units, so a point here is a point of board
 * value. A [TimingVerdict.NoWindow] is a different kind of claim; see there.
 */
class HoldPolicy(
    private val intents: IntentCatalog,
    /**
     * [AiProfile.combatTricksWaitForBlocks][com.wingedsheep.ai.engine.AiProfile.combatTricksWaitForBlocks]
     * — narrow the combat window to the steps where blocks are already in.
     */
    private val tricksWaitForBlocks: Boolean = false,
    /**
     * [AiProfile.holdRemovalForBetterTargets][com.wingedsheep.ai.engine.AiProfile.holdRemovalForBetterTargets]
     * — charge a removal spell for pointing at a target below a fair trade. See [RemovalPatience],
     * which is where the whole idea lives.
     */
    private val holdRemovalForBetterTargets: Boolean = false,
    /**
     * [com.wingedsheep.ai.engine.AiProfile.payoffsReadTheirFuel]: under [holdRemovalForBetterTargets],
     * a payoff's fuel counts toward whether it is worth the removal spell.
     */
    private val payoffsReadTheirFuel: Boolean = false,
    /**
     * [AiProfile.holdCountersForBetterSpells][com.wingedsheep.ai.engine.AiProfile.holdCountersForBetterSpells]
     * — charge a counterspell for answering a spell smaller than what the caster can still deploy.
     * See [CounterPatience].
     */
    private val holdCountersForBetterSpells: Boolean = false,
    /**
     * [AiProfile.cashCantripsInTheEndStep][com.wingedsheep.ai.engine.AiProfile.cashCantripsInTheEndStep]
     * — hand an instant-speed draw spell the end-step window this policy already hands removal.
     */
    private val cashCantripsInTheEndStep: Boolean = false,
    /**
     * [AiProfile.holdFlashPermanentsForAmbush][com.wingedsheep.ai.engine.AiProfile.holdFlashPermanentsForAmbush]
     * — stop deploying a flash creature on our own turn when the ambush window is still ahead. See
     * [AmbushWindow], which is where the whole idea lives.
     */
    private val holdFlashPermanentsForAmbush: Boolean = false,
    /**
     * [AiProfile.holdExpiringGrantsForCombat][com.wingedsheep.ai.engine.AiProfile.holdExpiringGrantsForCombat]
     * — stop paying for an activated ability whose whole payoff expires at cleanup, in a window
     * where nothing can spend it. See [ExpiringGrantWindow], which is where the whole idea lives.
     */
    private val holdExpiringGrantsForCombat: Boolean = false,
    /**
     * [AiProfile.refuseUnspendableGrants][com.wingedsheep.ai.engine.AiProfile.refuseUnspendableGrants]
     * — refuse an until-end-of-turn payoff that nothing left this turn can spend: an activation
     * ([ExpiringGrantWindow]) or a combat trick aimed at a creature sitting the fight out
     * ([trickTargetSitsOutTheFight]).
     */
    private val refuseUnspendableGrants: Boolean = false,
    /**
     * [AiProfile.refuseDeadSearches][com.wingedsheep.ai.engine.AiProfile.refuseDeadSearches]
     * — refuse an activation whose whole payoff is a library search our library cannot satisfy.
     * See [DeadSearch], which is where the whole idea lives.
     */
    private val refuseDeadSearches: Boolean = false,
    /**
     * [AiProfile.refuseEmptyPayoffs][com.wingedsheep.ai.engine.AiProfile.refuseEmptyPayoffs]
     * — refuse an activation whose whole payoff lands on nothing: a group effect over an empty
     * group, an attach to the current host, another Equipment move after combat, or a move before
     * combat onto no better an attacker. See
     * [EmptyPayoff], which is where the whole idea lives.
     */
    private val refuseEmptyPayoffs: Boolean = false,
    /**
     * [AiProfile.holdUnusablePumps][com.wingedsheep.ai.engine.AiProfile.holdUnusablePumps] — read
     * an untargeted team pump ([CardIntent.expiringGroupPump]) as a combat trick, refuse it once the
     * fight has left every creature of ours out, and defer any trick out of our own
     * beginning-of-combat step to the declare-attackers window.
     */
    private val holdUnusablePumps: Boolean = false,
    /**
     * [AiProfile.drawWithIdleMana][com.wingedsheep.ai.engine.AiProfile.drawWithIdleMana] — spend
     * the last main phase's idle mana on a sorcery-speed draw spell ([idleDrawWindow]), and keep an
     * instant-speed draw activation for the opponent's end step ([drawActivationVerdict]).
     */
    private val drawWithIdleMana: Boolean = false,
    /**
     * [AiProfile.holdInstantSpeedDeploys][com.wingedsheep.ai.engine.AiProfile.holdInstantSpeedDeploys]
     * — read a flash permanent's payoffs by what it can cash in on arrival, and give an instant
     * that only deploys ([AmbushWindow.holdsInstantDeploy]) the same hold.
     */
    private val holdInstantSpeedDeploys: Boolean = false,
    /**
     * [AiProfile.taxCountersReadTheirMana][com.wingedsheep.ai.engine.AiProfile.taxCountersReadTheirMana]
     * — read a "counter unless they pay {N}" against the mana its target's caster has left. See
     * [CounterTax], which is where the whole idea lives.
     */
    private val taxCountersReadTheirMana: Boolean = false,
    /**
     * [AiProfile.refuseWastedPayoffs][com.wingedsheep.ai.engine.AiProfile.refuseWastedPayoffs] —
     * refuse a permanent whose entry trigger has nothing to target ([TargetlessEntry]), an
     * end-of-turn shrink on a creature with no fight left ([ExpiringGrantWindow]), an end-of-combat
     * sweep keyed on blocks its creature will never make ([IdleBlockWatcher]) and a spells-only
     * mana grant nothing castable would spend ([UnspendableManaGrant]).
     */
    private val refuseWastedPayoffs: Boolean = false,
    /**
     * [AiProfile.lifelinkWaitsForBlocks][com.wingedsheep.ai.engine.AiProfile.lifelinkWaitsForBlocks]
     * — [ExpiringGrantWindow]'s deferral holds a lifelink-only grant until blockers are declared.
     */
    private val lifelinkWaitsForBlocks: Boolean = false,
    /**
     * [AiProfile.tapToGrowAtTheirEndStep][com.wingedsheep.ai.engine.AiProfile.tapToGrowAtTheirEndStep]
     * — hold a creature's "{T}: put counters on this creature" for the window where the tap costs
     * no attack and no block. See [SelfTapGrowthWindow], which is where the whole idea lives.
     */
    private val tapToGrowAtTheirEndStep: Boolean = false,
    /**
     * [AiProfile.borrowedCreaturesGoHome][com.wingedsheep.ai.engine.AiProfile.borrowedCreaturesGoHome]
     * — refuse an until-end-of-turn steal once the turn has no fight left for the borrowed creature
     * ([ExpiringGrantWindow], `PayoffKind.BORROW`).
     */
    private val borrowedCreaturesGoHome: Boolean = false,
    /**
     * The profile's `EvaluationWeights.boardPresence`, so [RemovalPatience] can quote its discount
     * in the same currency as the board value it compares against. The default is the compiled
     * fallback's, which is what every profile that does not opt in would have used anyway.
     */
    private val boardPresenceWeight: Double = EvaluationWeights.DEFAULT.boardPresence,
    /**
     * The engine's legal actions for a player in a position — what [UnspendableManaGrant] asks of
     * the leaf a mana grant resolves to. Null where no engine is wired, and that reader then
     * declines.
     */
    private val legalActions: ((GameState, EntityId) -> List<LegalAction>)? = null,
) {
    private val synergy = SynergyPresence(intents)

    /** Whether this policy can say anything. False when the agent has no card knowledge. */
    val isEnabled: Boolean get() = intents.isEnabled

    /**
     * The policy's reading of casting [cardName] here — the window it is in, plus what it is being
     * pointed at.
     *
     * The two halves are deliberately asymmetric about speed. **Windows** only judge instant-speed
     * cards: a sorcery has no window to wait for, and penalizing one would just make the AI pass
     * with a full hand. **[RemovalPatience]** judges any removal at all, because "is this creature
     * worth a card?" is the same question for a Pacifism as for a Doom Blade — and the Aura is the
     * case that motivated it.
     *
     * @param cast the materialized spell, when this action is one. Null for an activated ability,
     *   which spends no card and so is never charged for patience.
     * @param activation the materialized activation, when this action is one. The window half below
     *   reads the *card*, which for an activation is its source permanent — a different and much
     *   broader question than what the ability does, and the reason [activationVerdictFor] exists.
     */
    fun verdictFor(
        state: GameState,
        playerId: EntityId,
        cardName: String,
        cast: CastSpell? = null,
        activation: ActivateAbility? = null,
        /**
         * The position the action resolves to, for the readings a card cannot answer — what a
         * token it makes can do ([AmbushWindow.holdsInstantDeploy]). Null where there is none.
         */
        leaf: GameState? = null,
    ): TimingVerdict {
        // Asked first. It returns [TimingVerdict.Neutral] with every flag off and on any action
        // that is not an activation, so everything below is reached exactly as it was.
        activationVerdictFor(state, playerId, cardName, activation)
            .takeIf { it !is TimingVerdict.Neutral }
            ?.let { return it }

        // A modal cast is read off the modes it chose, not the union the card carries in hand.
        val intent = (if (cast != null) intents.forCast(cardName, cast) else intents.forName(cardName))
            ?: return TimingVerdict.Neutral

        // A permanent whose entry has nothing to point at — or an Aura whose entry is gated on a
        // creature it is not being cast onto — does nothing in any window. Nor does a sweep over
        // what a creature blocked when it can block nothing more, or borrowed spell mana with no
        // spell to spend it on.
        if (refuseWastedPayoffs && cast != null) {
            val definition = intents.definition(cardName)
            val predicates = intents.predicates
            if (definition != null && predicates != null &&
                (TargetlessEntry.holds(state, playerId, definition, cast.cardId, predicates) ||
                    UnmetAuraEntry.holds(state, playerId, definition, cast.cardId, cast.targets, predicates) ||
                    IdleBlockWatcher.holds(state, definition, cast.targets) ||
                    UnspendableManaGrant.holds(state, playerId, definition, leaf, legalActions))
            ) {
                return TimingVerdict.NoWindow
            }
            // An end-of-turn shrink with no fight left to shrink — the spell twin of the
            // activation reading in [activationVerdictFor].
            val resolves = definition?.let { castEffects(it, cast) }
            if (resolves != null && ExpiringGrantWindow.castBuysNothing(
                    state, playerId, resolves, cast.targets + cast.modeTargetsOrdered.flatten(), intents,
                )
            ) {
                return TimingVerdict.NoWindow
            }
        }
        // A creature borrowed until end of turn after the turn's last fight goes home unused.
        if (borrowedCreaturesGoHome && cast != null) {
            val resolves = intents.definition(cardName)?.let { castEffects(it, cast) }
            if (resolves != null && ExpiringGrantWindow.castBuysNothing(
                    state, playerId, resolves, cast.targets + cast.modeTargetsOrdered.flatten(), intents,
                    readBorrows = true, readShrinks = refuseWastedPayoffs,
                )
            ) {
                return TimingVerdict.NoWindow
            }
        }

        val window = windowVerdictFor(state, playerId, intent, cast, leaf)
        // A card that accomplishes nothing here is already floored below passing; there is no
        // target trade left to price on top of that.
        if (window is TimingVerdict.NoWindow) return window

        // A tax counter is read against the mana they have left: futile, or the first of a stack.
        val stackCredit = if (taxCountersReadTheirMana && cast != null) {
            when (val reading = CounterTax.read(state, playerId, intent, cast, intents)) {
                CounterTax.Reading.Futile -> return TimingVerdict.NoWindow
                is CounterTax.Reading.FirstOfAStack -> boardPresenceWeight * reading.spellWorth
                CounterTax.Reading.Silent -> 0.0
            }
        } else {
            0.0
        }

        val patience = patienceFor(state, playerId, intent, cast)
        if (patience <= 0.0 && stackCredit == 0.0) return window

        val windowDelta = (window as? TimingVerdict.Adjust)?.delta ?: 0.0
        return TimingVerdict.Adjust(
            windowDelta + stackCredit - patience.coerceAtLeast(0.0),
            reason = if (stackCredit > 0.0) "first of stacked tax counters" else "patience",
        )
    }

    /**
     * What [cast] resolves: the spell's effect, or for a modal spell the modes it chose. Null for a
     * shape this does not read — a split or adventure face, a fused cast, or a modal spell cast
     * without its modes named.
     */
    private fun castEffects(definition: CardDefinition, cast: CastSpell): List<Effect>? {
        if (cast.faceIndex != null || cast.castFused || definition.cardFaces.isNotEmpty()) return null
        val effect = definition.script.spellEffect ?: return null
        if (effect !is ModalEffect) return listOf(effect)
        if (cast.chosenModes.isEmpty()) return null
        return cast.chosenModes.map { effect.modes.getOrNull(it)?.effect ?: return null }
    }

    /**
     * The patience discount for [cast], or `0.0` when the flags are off or the shape doesn't fit.
     *
     * The two bars are summed rather than chosen between, which costs nothing: each declines on the
     * other's shape — [RemovalPatience] wants a single opposing *permanent* target and
     * [CounterPatience] a single opposing *spell* — so at most one of them is ever non-zero.
     */
    private fun patienceFor(
        state: GameState,
        playerId: EntityId,
        intent: CardIntent,
        cast: CastSpell?,
    ): Double {
        if (cast == null) return 0.0
        val card = state.getEntity(cast.cardId)?.get<CardComponent>() ?: return 0.0

        val removal = if (holdRemovalForBetterTargets) {
            RemovalPatience.discount(
                state, playerId, intent, card, cast.targets, boardPresenceWeight,
                synergy = if (payoffsReadTheirFuel) synergy else null,
            )
        } else {
            0.0
        }
        val counter = if (holdCountersForBetterSpells) {
            CounterPatience.discount(state, playerId, intent, cast.targets, intents, boardPresenceWeight)
        } else {
            0.0
        }
        return removal + counter
    }

    /**
     * The window half for an **activated ability**, which the card-driven one below cannot ask.
     *
     * Kept as its own entry point rather than as a branch in [windowVerdictFor] because the two
     * read different objects. That one reads the card, and for an activation the card is the source
     * permanent — [CardIntentAnalyzer] types anything with a non-mana activated ability as
     * [Speed.ACTIVATED], which the window half declines outright, so no branch there has ever seen
     * an activation. This reads the *ability*: what it does, how long it lasts, and whether it can
     * be paid for again later.
     *
     * A null lookup — the flag off, no card knowledge, or an ability granted by something other
     * than the card itself — is "no information" and returns [TimingVerdict.Neutral], leaving the
     * leaf score in charge exactly as before.
     */
    private fun activationVerdictFor(
        state: GameState,
        playerId: EntityId,
        cardName: String,
        activation: ActivateAbility?,
    ): TimingVerdict {
        if (activation == null) return TimingVerdict.Neutral
        if (!(holdExpiringGrantsForCombat || refuseUnspendableGrants || refuseDeadSearches ||
                refuseEmptyPayoffs || drawWithIdleMana || refuseWastedPayoffs || tapToGrowAtTheirEndStep)
        ) {
            return TimingVerdict.Neutral
        }
        val ability = intents.activatedAbility(cardName, activation.abilityId)
            ?: return TimingVerdict.Neutral
        // A search that can find nothing is a lost permanent and a shuffle, in every window.
        if (refuseDeadSearches) {
            val predicates = intents.predicates
            if (predicates != null && DeadSearch.holds(state, playerId, ability, activation, predicates)) {
                return TimingVerdict.NoWindow
            }
        }
        // An activation that changes nothing is a waste of its cost, in every window.
        if (refuseEmptyPayoffs && EmptyPayoff.holds(state, playerId, ability, activation, intents.predicates)) {
            return TimingVerdict.NoWindow
        }
        if (drawWithIdleMana) drawActivationVerdict(state, playerId, ability)?.let { return it }
        // A tap that costs an attack or a block, for counters the opponent's end step buys for free.
        if (tapToGrowAtTheirEndStep && SelfTapGrowthWindow.holds(state, playerId, ability, activation)) {
            return TimingVerdict.NoWindow
        }
        if (!(holdExpiringGrantsForCombat || refuseUnspendableGrants || refuseWastedPayoffs || borrowedCreaturesGoHome)) {
            return TimingVerdict.Neutral
        }
        val holds = ExpiringGrantWindow.holds(
            state, playerId, ability, intents,
            activation = activation,
            deferToLaterWindow = holdExpiringGrantsForCombat,
            refuseUnspendable = refuseUnspendableGrants,
            readShrinks = refuseWastedPayoffs,
            readRestrictions = refuseWastedPayoffs,
            lifelinkWaitsForBlocks = lifelinkWaitsForBlocks,
            readBorrows = borrowedCreaturesGoHome,
        )
        return if (holds) TimingVerdict.NoWindow else TimingVerdict.Neutral
    }

    /**
     * An instant-speed activation that only draws, scries or surveils ([ACTIVATION_FLOW_TAGS]), judged under
     * [drawWithIdleMana]: cashed at the opponent's end step, held everywhere before it while an
     * instant-speed card in hand wants the mana. Null when the ability is not that shape or the hold does not apply.
     *
     * The misplay (2026-10-11 logs, game 2 turn 11): Omashu City's "{4}, {T}, Sacrifice this land:
     * Draw a card" activated in our precombat main, tapping five of six lands while Pillar Launch
     * and Cunning Maneuver sat in hand — and a creature the trick could have saved traded on the
     * opponent's turn. The leaf prices a card drawn now exactly as one drawn at their end step, so
     * nothing told the AI the mana had a better use in between.
     *
     * The hold is a floor, like [ExpiringGrantWindow]'s, on the same argument: an ability cannot be
     * stripped from hand, so it is still there at the opponent's end step at the same cost, and the
     * mana it would have spent covered every trick until then. What casting it now buys that the end
     * step cannot — playing a land or a sorcery it draws — is real but small, and only argued for
     * when there is no instant-speed card holding the mana up; without one the leaf decides as
     * before. A spell on the stack, or one of [Patience.releasedOutright]'s releases, hands the
     * decision back too.
     */
    private fun drawActivationVerdict(
        state: GameState,
        playerId: EntityId,
        ability: ActivatedAbility,
    ): TimingVerdict? {
        if (ability.isManaAbility || ability.timing != TimingRule.InstantSpeed) return null
        val tags = CardIntentAnalyzer.analyzeEffect(ability.effect).tags
        if (tags.isEmpty() || !tags.all { it in ACTIVATION_FLOW_TAGS }) return null
        // A copy-and-cast (Isochron Scepter) also reads as DRAW, but the copy is a spell that wants
        // its own window — an imprinted Fog in the opponent's combat — not the end step.
        if (EffectWalker.payoffLeaves(ability.effect).any {
                it is com.wingedsheep.sdk.scripting.effects.CopyCardIntoCollectionEffect ||
                    it is com.wingedsheep.sdk.scripting.effects.CopyCollectionIntoCollectionEffect
            }
        ) return null
        // A look at the top is worth about nothing on the board, so the window's bonus would decide
        // alone: it may only spend what empties at cleanup anyway, never life or a permanent
        // (Doom Whisperer's 2 life, Zahur's sacrifice). A draw is a card, worth its own price.
        if (IntentTag.DRAW !in tags && !costsOnlyUntilCleanup(ability.cost)) return null
        if (isOpponentEndStep(state, playerId)) {
            return TimingVerdict.Adjust(END_STEP_WINDOW, reason = "card flow at their end step")
        }
        if (state.stack.isNotEmpty()) return null
        if (!holdsInstantSpeedCard(state, playerId)) return null
        if (Patience.releasedOutright(state, state.projectedState, playerId)) return null
        return TimingVerdict.NoWindow
    }

    /** Whether [cost] spends only mana and untapped permanents — what the turn's cleanup takes back anyway. */
    private fun costsOnlyUntilCleanup(cost: AbilityCost): Boolean = when (cost) {
        AbilityCost.Free, AbilityCost.Tap -> true
        is AbilityCost.Atom -> cost.atom is CostAtom.Mana || cost.atom is CostAtom.TapPermanents
        is AbilityCost.Composite -> cost.costs.all(::costsOnlyUntilCleanup)
        else -> false
    }

    /**
     * Whether casting [cast] — a sorcery-speed draw or card-selection spell — spends mana nothing
     * else will: our postcombat main on an empty stack, with no instant-speed card in hand that the
     * mana could still be held up for. Under [drawWithIdleMana].
     *
     * The misplay (2026-10-11 logs, game 3 turns 1–13): Sleight of Hand held for seven turns, on
     * turn 11 with five untapped lands and nothing else to do. Its own board value is about zero —
     * a card found against a card spent — so on a tie the AI passes, and [cashCantripsInTheEndStep]'s
     * window is one a sorcery can never reach. Our last main phase is that window's sorcery-speed
     * twin: the mana empties at the end of the turn whatever we do.
     *
     * "Nothing else will" has a second half this cannot see, which is the rest of the candidates:
     * the bonus is [TimingVerdict.Adjust.onlyWhenIdle], so `Strategist` drops it whenever another
     * action beats passing on its own — a threat, or a land drop, goes first.
     */
    private fun idleDrawWindow(
        state: GameState,
        playerId: EntityId,
        intent: CardIntent,
        cast: CastSpell?,
    ): Boolean {
        if (cast == null) return false
        if (!intent.tags.all { it in CARD_FLOW_TAGS }) return false
        // A scry beside an unread payoff (a bounce, a discard) is a rider, not the spell's purpose.
        if ((intent.tags - IntentTag.CARD_SELECTION).isEmpty() && !intent.selectsIntoHand) return false
        val card = state.getEntity(cast.cardId)?.get<CardComponent>() ?: return false
        if (card.typeLine.isPermanent) return false
        if (intent.wishFilters.isNotEmpty() && !holdsWishableCard(state, playerId, intent)) return false
        return state.isActiveTurnFor(playerId) && state.step == Step.POSTCOMBAT_MAIN &&
            state.stack.isEmpty() && !holdsInstantSpeedCard(state, playerId)
    }

    /**
     * Whether a wish has anything to take: our sideboard holds a card one of its gathers matches.
     *
     * The leaf prices a wish as a card spent and a card gained — the same tie as a cantrip, which is
     * why it earns [idleDrawWindow]'s nudge at all (2026-10-14, Reality Fracture draft: Extrapolate
     * the Impossible held all game with two Vraskas, a Jace and an Edgar outside the game). A wish
     * into a sideboard with nothing it can take is a card thrown away, and the nudge must not
     * outvote that. Reading the sideboard is honest: it is our own, and we know what is in it. An
     * unreadable filter is "it might match", as in [DeadSearch].
     */
    private fun holdsWishableCard(state: GameState, playerId: EntityId, intent: CardIntent): Boolean {
        val sideboard = state.getZone(playerId, Zone.SIDEBOARD)
        if (sideboard.isEmpty()) return false
        val predicates = intents.predicates ?: return true
        val context = PredicateContext(controllerId = playerId)
        val projected = state.projectedState
        return intent.wishFilters.any { filter ->
            sideboard.any { cardId ->
                runCatching { predicates.matches(state, projected, cardId, filter, context) }.getOrDefault(true)
            }
        }
    }

    /** Whether our hand holds an instant-speed card — something mana left untapped can still buy. */
    private fun holdsInstantSpeedCard(state: GameState, playerId: EntityId): Boolean =
        state.getZone(playerId, Zone.HAND).any { id ->
            val name = state.getEntity(id)?.get<CardComponent>()?.name ?: return@any false
            intents.forName(name)?.speed == Speed.INSTANT
        }

    /** The window half — "is this the moment?", which only an instant-speed card can get wrong. */
    private fun windowVerdictFor(
        state: GameState,
        playerId: EntityId,
        intent: CardIntent,
        cast: CastSpell?,
        leaf: GameState? = null,
    ): TimingVerdict {
        // The one sorcery-speed exception: a draw spell in our last main phase, where the mana is
        // otherwise about to empty unused. See [idleDrawWindow].
        if (drawWithIdleMana && intent.speed == Speed.SORCERY && idleDrawWindow(state, playerId, intent, cast)) {
            return TimingVerdict.Adjust(END_STEP_WINDOW, reason = "draw with idle mana", onlyWhenIdle = true)
        }
        if (intent.speed != Speed.INSTANT) return TimingVerdict.Neutral

        val stackHasSomething = state.stack.isNotEmpty()

        return when {
            // A counterspell with nothing to counter is not a play, it is a discard.
            IntentTag.COUNTERSPELL in intent.tags && !stackHasSomething -> TimingVerdict.NoWindow

            // A pump wears off at cleanup, so it is worth casting only when something will use it
            // before then: a fight in combat, or a spell on the stack it can save the creature
            // from — which is [responseWindowFor]'s question, not "is the stack non-empty".
            // Anywhere else — our own main phase, and specifically the opponent's end step, where
            // the old blanket `passScore - 1.5` discount actively *encouraged* dumping it — it buys
            // nothing at all.
            //
            // "Something will use it" is about the *target*, not the step. Once the fight is set, a
            // pump on a creature that is not in it has no combat to spend the stats in, whatever
            // step it is cast in — Acrobatic Leap on a Market Gnome in our own declare-blockers
            // step, with only the Guardian attacking. So that shape gets the same reading as any
            // window outside combat: only a spell on the stack can still make it worth a card.
            //
            // [holdUnusablePumps] widens the branch to an untargeted team pump (Rabbit Response),
            // whose whole team sits the fight out when no creature of ours is in it. It also moves
            // a trick out of our own beginning-of-combat step: we get priority again once attackers
            // are declared, and a pump spent before then is one the attack may not use at all.
            IntentTag.COMBAT_TRICK in intent.tags || (holdUnusablePumps && intent.expiringGroupPump) -> when {
                (refuseUnspendableGrants || holdUnusablePumps) && cast != null &&
                    trickTargetSitsOutTheFight(state, playerId, cast, intent) ->
                    responseWindowFor(state, playerId, intent)
                holdUnusablePumps && state.isActiveTurnFor(playerId) && state.step == Step.BEGIN_COMBAT ->
                    responseWindowFor(state, playerId, intent)
                state.step in combatWindow -> TimingVerdict.Adjust(COMBAT_WINDOW)
                else -> responseWindowFor(state, playerId, intent)
            }

            // A permanent with flash, deployed where the ambush window is still ahead of us.
            //
            // This is the one branch that returns a verdict on the *wrong* window rather than a
            // bonus on the right one, and [AmbushWindow] carries the argument for why the removal
            // branch below does not transfer: a bonus three steps later cannot correct a decision
            // made now, and unlike "hold the removal" the claim here is provable.
            //
            // It sits **above** the removal branch on purpose, and the reason is a bug it has to
            // route around. `hitsAnotherPermanent` reads "does this take someone else's permanent
            // off the battlefield" off the effect target alone, which for a bound target carries no
            // filter and falls through to true — so Restoration Angel, whose ETB blinks a creature
            // *we control*, is tagged `REMOVAL, EXILE_REMOVAL`. Below the removal branch this
            // branch would be unreachable for the exact card it was written for.
            //
            // Ceding to it costs nothing, because [AmbushWindow]'s own guard is strictly the more
            // precise of the two: it asks the same question *and* which side of the table the
            // targeting points at. A flash creature that really does answer their board fails that
            // guard, this branch's condition goes false, and the removal branch below picks it up
            // unchanged. Nothing without flash reaches here at all.
            holdFlashPermanentsForAmbush && AmbushWindow.holds(
                state, playerId, intent,
                readArrival = holdInstantSpeedDeploys,
                castCardId = cast?.cardId,
                intents = intents,
            ) -> TimingVerdict.NoWindow

            // The same hold for an instant whose whole payoff is the body or walker it leaves
            // behind. Its own guard declines anything that draws, removes, pumps or counters, so it
            // takes nothing from the branches below.
            holdInstantSpeedDeploys && cast != null &&
                AmbushWindow.holdsInstantDeploy(state, leaf, playerId, intent, cast.cardId, intents) ->
                TimingVerdict.NoWindow

            // Instant-speed removal: reward the windows that are strictly better than now, and
            // charge nothing for casting it early.
            //
            // The symmetric *window* penalty — "hold it, our own main phase is the wrong time" — is
            // what the plan proposed, and it was built, measured and removed. Holding removal is a
            // preference between two futures, not a provable loss, and a constant cannot price one:
            // the one large enough to change behaviour was large enough to veto casting the removal
            // at all, which is the exact blindness this phase exists to fix.
            //
            // What survives that verdict is [RemovalPatience], and the difference is what it
            // charges *for*. Not the window — the **target**, by how far it falls short of a fair
            // trade, so a 1/1 is charged and the artifact the Disenchant is aimed at is not charged
            // at all. It is applied outside this `when`, to sorcery-speed removal too.
            intent.tags.any { it in REMOVAL_TAGS } -> when {
                stackHasSomething -> TimingVerdict.Adjust(RESPONSE_WINDOW)
                isOpponentEndStep(state, playerId) -> TimingVerdict.Adjust(END_STEP_WINDOW)

                else -> TimingVerdict.Neutral
            }

            // The same end-step window, for the other card that is strictly better cast there.
            //
            // A cantrip's own board value is about zero — a card drawn against a card spent — so on
            // a tie the AI passes, and it passed *every* window, right through to cleanup, where the
            // mana it was saving evaporates. Casting at the opponent's end step costs nothing this
            // spell was going to use and buys a turn of extra information about what to keep.
            //
            // Phase 6 used to get this for free from `Strategist`'s blanket `passScore - 1.5` at the
            // end step, and correctly switched it off for every agent with card knowledge: the same
            // constant also paid for dumping a pump that expires in cleanup. That is `instants-06`,
            // and it stays answered — an expiring pump is [IntentTag.COMBAT_TRICK] and is caught by
            // the branch above this one, whatever else it draws. What is restored here is only the
            // half of the old constant that was right, which is why this is a window on a tag rather
            // than a discount on a step.
            //
            // No symmetric penalty for casting a cantrip in our own main phase, for the reason the
            // removal branch gives at length: rewarding the better window is a comparison, and
            // charging the worse one is a preference a constant cannot price.
            cashCantripsInTheEndStep && IntentTag.DRAW in intent.tags &&
                isOpponentEndStep(state, playerId) -> TimingVerdict.Adjust(END_STEP_WINDOW)

            else -> TimingVerdict.Neutral
        }
    }

    /**
     * Whether [cast] pumps only creatures of ours that the fight has already left out — the
     * combat-trick half of [AiProfile.refuseUnspendableGrants][com.wingedsheep.ai.engine.AiProfile.refuseUnspendableGrants].
     *
     * "Already left out" is read off the declarations, so it is only ever true once they are in:
     * on our turn once attackers are declared (we only get priority in that step afterwards), on
     * theirs once blockers are. Before that a creature not yet in combat can still be put in it,
     * and the window is the ordinary one. From the combat damage step on, the damage is dealt
     * before anyone gets priority (CR 510.2–510.3), so no creature is still in a fight a pump can
     * change.
     *
     * Every target must be one of ours, untapped and out of combat. An opposing target, a tapped
     * one (an "untap it" rider is a lasting payoff), or a target that is attacking or blocking
     * keeps the ordinary window — this only fires where the trick provably buys nothing.
     *
     * An untargeted team pump ([CardIntent.expiringGroupPump], under [holdUnusablePumps]) names no
     * targets; it sits the fight out when none of our creatures is attacking or blocking — Rabbit
     * Response cast in our declare-attackers step after we declared no attack (2026-10-10 logs,
     * game 1 turn 13).
     */
    private fun trickTargetSitsOutTheFight(
        state: GameState,
        playerId: EntityId,
        cast: CastSpell,
        intent: CardIntent,
    ): Boolean {
        val fightSet = if (state.isActiveTurnFor(playerId)) {
            state.step in ATTACKERS_DECLARED_STEPS
        } else {
            state.step in BLOCKERS_DECLARED_STEPS
        }
        if (!fightSet) return false
        val damageDealt = state.step == Step.COMBAT_DAMAGE || state.step == Step.END_COMBAT

        val projected = state.projectedState
        if (cast.targets.isEmpty() && holdUnusablePumps && intent.expiringGroupPump) {
            return damageDealt || projected.getBattlefieldControlledBy(playerId).none { id ->
                val entity = state.getEntity(id)
                projected.isCreature(id) && entity != null &&
                    (entity.has<AttackingComponent>() || entity.has<BlockingComponent>())
            }
        }
        if (!refuseUnspendableGrants) return false
        val targets = cast.targets.map { (it as? ChosenTarget.Permanent)?.entityId ?: return false }
        if (targets.isEmpty()) return false
        return targets.all { id ->
            val entity = state.getEntity(id) ?: return false
            projected.getController(id) == playerId &&
                !entity.has<TappedComponent>() &&
                (damageDealt || (!entity.has<AttackingComponent>() && !entity.has<BlockingComponent>()))
        }
    }

    /** The last window before our own turn — the deadline every "hold it" argument runs out at. */
    private fun isOpponentEndStep(state: GameState, playerId: EntityId): Boolean =
        state.activePlayerId != playerId && state.step == Step.END

    /**
     * What a spell already on the stack is worth to a pump — the "last chance" window.
     *
     * A stack object is not by itself a reason to cast a trick. The bonus was flat for any
     * non-empty stack, which paid the AI to answer a Murder with Giant Growth (a 5/5 is destroyed
     * exactly as fast as a 2/2) and to pump a 6/4 that was already walking off a Bolt. Both are
     * `lastchance` puzzles, and both are the *same* mistake: reading "there is a deadline" as "this
     * card meets it".
     *
     * So the window has to be earned. It is real when something on the stack would kill a creature
     * we control **by size** — damage, or -N/-N — and the extra toughness carries it out of range.
     * That is one comparison, and it separates the three positions the suite pairs: Bolt on a 2/2
     * (dying, +3/+3 saves it → window), Bolt on a 6/4 (not dying → nothing to buy), Murder on
     * anything (no reach, so no amount of toughness answers it).
     *
     * **Silence is not a veto.** [TimingVerdict.NoWindow] floors the candidate below passing, which
     * is only honest where "does nothing" is structurally certain, so a stack object this policy
     * cannot read — an unknown card, or a fight, whose reach is the other creature's power and not
     * a property of the card — keeps the old bonus rather than earning a veto. Which makes it
     * load-bearing that [IntentCatalog.forStackObject] reads *abilities* too, since a trigger on the
     * stack is the commonest stack object there is.
     */
    private fun responseWindowFor(state: GameState, playerId: EntityId, pump: CardIntent): TimingVerdict {
        val projected = state.projectedState
        var unreadable = false

        for (stackId in state.stack) {
            val container = state.getEntity(stackId) ?: continue
            val threat = intents.forStackObject(container)
            if (threat == null || IntentTag.FIGHT in threat.tags) {
                unreadable = true
                continue
            }

            // A sweeper names no targets; everything we control is under it.
            val victims = if (IntentTag.SWEEPER in threat.tags) {
                projected.getBattlefieldControlledBy(playerId)
            } else {
                container.get<TargetsComponent>()?.targets.orEmpty()
                    .mapNotNull { (it as? ChosenTarget.Permanent)?.entityId }
                    .filter { projected.getController(it) == playerId }
            }

            // Null reach on a card we *did* read is destruction, exile or bounce — toughness is no
            // defence against any of them — or a dynamic amount (X burn, a -X/-X) whose size the
            // static reading cannot bound; see [CardIntent.removalReach]. No pump is priced against either.
            val reach = threat.removalReach ?: continue
            if (victims.any { savedByPump(state, projected, it, reach, pump) }) {
                return TimingVerdict.Adjust(RESPONSE_WINDOW)
            }
        }

        return if (unreadable) TimingVerdict.Adjust(RESPONSE_WINDOW) else TimingVerdict.NoWindow
    }

    /**
     * Whether [creature] is dying to [reach] damage and [pump] is enough to change that.
     *
     * Both halves are load-bearing: a creature already out of range buys nothing (there is nothing
     * to save), and neither does one the pump cannot lift out of range (a +1/+0 trick against three
     * damage). Damage already marked counts, because that is what the state-based action will
     * compare against (CR 704.5g).
     */
    private fun savedByPump(
        state: GameState,
        projected: ProjectedState,
        creature: EntityId,
        reach: Int,
        pump: CardIntent,
    ): Boolean {
        if (!projected.isCreature(creature)) return false
        val toughness = projected.getToughness(creature) ?: return false
        val remaining = toughness - (state.getEntity(creature)?.get<DamageComponent>()?.amount ?: 0)
        return remaining <= reach && remaining + pump.pumpToughness > reach
    }

    /**
     * The steps where a combat trick is worth its bonus.
     *
     * [COMBAT_STEPS] is the historical answer and it is one window too wide: it pays the trick in
     * `BEGIN_COMBAT` and `DECLARE_ATTACKERS`, both of which are *before* blocks. Spending a trick
     * there is worse than spending it late for a reason no board evaluation can see — it hands the
     * defender the information. A 2/2 that would have gone unblocked gets chump-blocked once it is
     * visibly a 5/5, and the pump that was exactly lethal buys nothing.
     *
     * [BLOCKS_IN_STEPS] is the constant matching [COMBAT_WINDOW]'s own comment. Every step in it is
     * one where blocks are already declared, whichever side we are on: on our turn we only receive
     * priority in `DECLARE_BLOCKERS` after the defender has declared, and on theirs we are the one
     * who just declared. Before that a trick falls through to [responseWindowFor], which pays it
     * only for something on the stack it can actually answer — so "hold it until blocks are in"
     * costs the AI no legitimate response.
     */
    private val combatWindow: Set<Step> get() = if (tricksWaitForBlocks) BLOCKS_IN_STEPS else COMBAT_STEPS

    private companion object {
        /** Blockers are in; a trick decides the fight. */
        const val COMBAT_WINDOW = 1.0

        /** Something on the stack this card can actually answer — see [responseWindowFor]. */
        const val RESPONSE_WINDOW = 1.0

        /**
         * Their end step: the last moment holding it is still free. Replaces the blanket
         * `passScore - 1.5`, but only for the cards that actually want the window.
         */
        const val END_STEP_WINDOW = 1.5

        val COMBAT_STEPS = setOf(
            Step.BEGIN_COMBAT, Step.DECLARE_ATTACKERS, Step.DECLARE_BLOCKERS,
            Step.FIRST_STRIKE_COMBAT_DAMAGE, Step.COMBAT_DAMAGE,
        )

        /** [COMBAT_STEPS] minus the two windows that come before blocks are declared. */
        val BLOCKS_IN_STEPS = setOf(
            Step.DECLARE_BLOCKERS, Step.FIRST_STRIKE_COMBAT_DAMAGE, Step.COMBAT_DAMAGE,
        )

        val REMOVAL_TAGS = setOf(IntentTag.REMOVAL, IntentTag.EXILE_REMOVAL, IntentTag.SWEEPER)

        /** Tags of a spell whose whole payoff is card flow — cards into hand, or a scry — see [idleDrawWindow]. */
        val CARD_FLOW_TAGS = setOf(IntentTag.DRAW, IntentTag.TUTOR, IntentTag.CARD_SELECTION)

        /** What [drawActivationVerdict] cashes at the opponent's end step: a draw, or a look at the top (scry, surveil). */
        val ACTIVATION_FLOW_TAGS = setOf(IntentTag.DRAW, IntentTag.CARD_SELECTION)

        /** Our turn, attackers already declared — see [trickTargetSitsOutTheFight]. */
        val ATTACKERS_DECLARED_STEPS = setOf(
            Step.DECLARE_ATTACKERS, Step.DECLARE_BLOCKERS, Step.FIRST_STRIKE_COMBAT_DAMAGE,
            Step.COMBAT_DAMAGE, Step.END_COMBAT,
        )

        /** Their turn, blockers already declared — see [trickTargetSitsOutTheFight]. */
        val BLOCKERS_DECLARED_STEPS = setOf(
            Step.DECLARE_BLOCKERS, Step.FIRST_STRIKE_COMBAT_DAMAGE, Step.COMBAT_DAMAGE, Step.END_COMBAT,
        )
    }
}

/** What [HoldPolicy] makes of casting a particular card in a particular window. */
sealed interface TimingVerdict {
    /** Timing says nothing here; the board score stands as it is. */
    data object Neutral : TimingVerdict

    /**
     * A nudge in the board score's own units.
     *
     * [reason] is for the local testing mode's decision panel and nothing else: a candidate the AI
     * passed over despite a strong board score is only explicable if the panel can name which half
     * of the policy moved it. Null means the window half, which is what an `Adjust` used to be.
     */
    data class Adjust(
        val delta: Double,
        val reason: String? = null,
        /**
         * The nudge is for spending mana that would otherwise go unused, so `Strategist` counts it
         * only when no other candidate beats passing without one. See `HoldPolicy.idleDrawWindow`.
         */
        val onlyWhenIdle: Boolean = false,
    ) : TimingVerdict

    /**
     * The card **cannot accomplish anything** in this window — a counterspell with an empty stack,
     * a pump that will wear off before any combat.
     *
     * This is a floor, not a penalty, and the difference matters. A penalty is a constant racing an
     * unbounded number, and it loses: `ThreatAssessment` reads a Giant Growth's +3/+3 as a
     * permanently faster clock and pays about 10 points for it, so no defensible literal could
     * outvote it. A verdict says the thing worth saying instead — *whatever the simulation reports,
     * this is not better than passing* — and the Strategist scores it just below the pass score.
     * Reserved for cases where "does nothing" is structurally certain, never for a preference.
     */
    data object NoWindow : TimingVerdict
}
