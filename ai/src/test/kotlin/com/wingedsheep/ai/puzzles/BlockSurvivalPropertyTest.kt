package com.wingedsheep.ai.puzzles

import com.wingedsheep.ai.engine.AIPlayer
import com.wingedsheep.ai.engine.AiProfile
import com.wingedsheep.engine.core.DeclareBlockers
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.collections.shouldBeEmpty

/**
 * One property, swept over ~900 randomly generated combats: **when some legal set of blocks
 * survives the turn, the AI declares one.**
 *
 * A curated puzzle asks whether the AI makes the move a good player makes; this asks the much
 * weaker question of whether it stays alive when staying alive was on offer, and it asks it of
 * boards nobody chose. That is the difference that mattered here — `blocking-07` and `blocking-08`
 * are two positions this sweep found, and neither is a shape anyone would have thought to write
 * down. Both had the AI spending its blockers on the trades that read best (a gang block that eats
 * the biggest attacker, a free kill on a 2/2) while a creature it could have covered walked in for
 * exactly lethal.
 *
 * **Vanilla creatures only**, which is what makes the oracle trustworthy: with no trample, flying,
 * menace or deathtouch in the pool every blocker can block every attacker and each one it blocks
 * absorbs that attacker's whole hit. So the least damage any legal plan can take is
 * `total power − the N biggest powers`, N = our creature count, and the check needs no search of
 * its own to be sure it is right. Keywords are the puzzles' job.
 *
 * Seeds are pinned: a CI gate cannot be seeded from the clock.
 */
class BlockSurvivalPropertyTest : ScenarioTestBase() {

    private companion object {
        /** `ScenarioBuilder` seeds itself from the clock otherwise; a gate cannot. */
        const val SCENARIO_SEED = 20260727L
    }

    init {
        val pool = listOf("Grizzly Bears", "Hill Giant", "Craw Wurm", "Ordinary Bear")
        val seeds = listOf(20260811L, 4242L, 777L)
        val positionsPerSeed = 300

        test("the AI survives every combat that a legal block could survive") {
            val deaths = mutableListOf<String>()
            var survivable = 0

            for (seed in seeds) {
                val rng = kotlin.random.Random(seed)
                repeat(positionsPerSeed) { index ->
                    val attackerNames = List(rng.nextInt(2, 5)) { pool.random(rng) }
                    val blockerNames = List(rng.nextInt(1, 4)) { pool.random(rng) }
                    val life = rng.nextInt(3, 12)

                    var builder = scenario().withRngSeed(SCENARIO_SEED).withPlayers()
                    attackerNames.forEach { builder = builder.withCardOnBattlefield(1, it) }
                    blockerNames.forEach { builder = builder.withCardOnBattlefield(2, it) }
                    val game = builder.withLifeTotal(2, life).build()
                        .advanceToDeclaration(1, Step.DECLARE_ATTACKERS)
                        .also { g -> g.declareAttackers(attackerNames.distinct().associateWith { 2 }) }
                        .advanceToDeclaration(2, Step.DECLARE_BLOCKERS)

                    val state = game.state
                    val aiId = game.seatId(2)
                    // A mis-built position scores as a pass otherwise. Skip rather than measure it.
                    if (state.pendingDecision != null || state.priorityPlayerId != aiId) return@repeat

                    val projected = state.projectedState
                    val attackers = state.getBattlefield()
                        .filter { state.getEntity(it)?.has<AttackingComponent>() == true }
                    val powers = attackers.map { projected.getPower(it) ?: 0 }
                    val unavoidable = powers.sum() - powers.sortedDescending().take(blockerNames.size).sum()
                    if (unavoidable >= life) return@repeat // dead whatever we do; not this test's business
                    survivable++

                    val action = AIPlayer.create(cardRegistry, aiId, AiProfile.PRODUCTION).chooseAction(state)
                    val blocks = (action as? DeclareBlockers)?.blockers.orEmpty()
                    val blocked = blocks.values.flatten().toSet()
                    val taken = attackers.filter { it !in blocked }.sumOf { projected.getPower(it) ?: 0 }
                    if (taken < life) return@repeat

                    val name = { id: EntityId -> state.getEntity(id)?.get<CardComponent>()?.name ?: id.value }
                    deaths += "seed=$seed[$index] at $life life, took $taken (could have taken $unavoidable) — " +
                        "attackers ${attackers.joinToString { "${name(it)} ${projected.getPower(it)}/${projected.getToughness(it)}" }}; " +
                        "blockers $blockerNames; " +
                        "chose ${blocks.entries.joinToString { (b, a) -> "${name(b)} → ${a.joinToString { at -> name(at) }}" }}"
                }
            }

            println("block-survival sweep: $survivable survivable positions, ${deaths.size} avoidable deaths")
            deaths.shouldBeEmpty()
        }
    }
}
