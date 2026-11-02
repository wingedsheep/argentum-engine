package com.wingedsheep.ai.puzzles

import com.wingedsheep.ai.engine.AIPlayer
import com.wingedsheep.ai.engine.AiProfile
import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.core.DeclareBlockers
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.collections.shouldBeEmpty

/**
 * The live AI's combat trades, swept over ~900 random boards and checked for plays that are
 * **strictly dominated** — worse than an alternative whatever the opponent does, so no evaluation
 * can justify them. The sibling of [BlockSurvivalPropertyTest] and [AttackRacePropertyTest], which
 * ask whether the AI stays alive; this asks whether it throws creatures away.
 *
 * **Blocking**, at a life total where taking every attack twice still leaves us alive (so no block
 * is buying life we need):
 * - no blocker dies without taking its attacker with it (a pointless chump);
 * - no single block trades a creature for one at least two mana cheaper — against deathtouch every
 *   block is a trade, so which body pays is left to the evaluator;
 * - no gang block loses two creatures, worth more mana between them, to kill one;
 * - no attacker walks in unblocked while an idle blocker could kill it and survive.
 *
 * **Attacking**: never declare an attack in which the defender can block every attacker with its
 * own creature that kills it and survives. That attack deals nothing and loses everything it sent.
 *
 * Who dies is read off the engine for the blocks, and computed by [fight] for the attack oracle,
 * which plays one-on-one combat out step by step (first strike, double strike, deathtouch). The pool
 * is vanilla bodies plus those keywords, flying, reach, trample, menace and lifelink — no abilities
 * beyond combat keywords, so nothing outside the fight changes what the right play is. Libraries
 * are stocked so the draw step never decides a line.
 *
 * Every failure class here was a live bug when this test was written — see
 * [AiProfile.crackBackAfterCleanup], [AiProfile.soundBlockMath], [AiProfile.blockSearchTriesSwaps]
 * and [com.wingedsheep.ai.engine.evaluation.CreatureValuation.thresholdsWholeBoard].
 *
 * Seeds are pinned: a CI gate cannot be seeded from the clock.
 */
class CombatTradePropertyTest : ScenarioTestBase() {

    private data class Body(val power: Int, val toughness: Int, val mv: Int, val kw: Set<String> = emptySet())

    private companion object {
        const val SCENARIO_SEED = 20261102L
        val POOL = mapOf(
            "Grizzly Bears" to Body(2, 2, 2),
            "Savannah Lions" to Body(2, 1, 1),
            "Pensive Minotaur" to Body(2, 3, 3),
            "Hill Giant" to Body(3, 3, 4),
            "Trained Armodon" to Body(3, 3, 3),
            "Spined Wurm" to Body(5, 4, 5),
            "Craw Wurm" to Body(6, 4, 6),
            "Typhoid Rats" to Body(1, 1, 1, setOf("DT")),
            "Wind Drake" to Body(2, 2, 3, setOf("FLY")),
            "Giant Spider" to Body(2, 4, 4, setOf("REACH")),
            "Youthful Knight" to Body(2, 1, 2, setOf("FS")),
            "Serra Angel" to Body(4, 4, 5, setOf("FLY", "VIG")),
            "Alpine Grizzly" to Body(4, 2, 3),
            "Colossal Dreadmaw" to Body(6, 6, 6, setOf("TRAMPLE")),
            "Boggart Brute" to Body(3, 2, 3, setOf("MENACE")),
            "Child of Night" to Body(2, 1, 2, setOf("LIFELINK")),
            "Fencing Ace" to Body(1, 1, 2, setOf("DS")),
            "Raging Redcap" to Body(1, 2, 3, setOf("DS")),
            "Vampire Nighthawk" to Body(2, 3, 3, setOf("FLY", "DT", "LIFELINK")),
            "Skyhunter Patrol" to Body(2, 3, 4, setOf("FLY", "FS")),
        )
        val NAMES = POOL.keys.toList()

        /** Whether [b] can block [a] on its own — evasion, and menace needing a second body. */
        fun canBlock(a: Body, b: Body) =
            "MENACE" !in a.kw && ("FLY" !in a.kw || "FLY" in b.kw || "REACH" in b.kw)

        /** Combat damage an attacker deals over the whole combat, for the "safe life" bound. */
        fun hits(a: Body) = if ("DS" in a.kw) 2 * a.power else a.power

        /**
         * (attacker dies, blocker dies) in a one-on-one fight: a first-strike step for first and
         * double strikers, then a regular step for every survivor without first strike.
         */
        fun fight(a: Body, b: Body): Pair<Boolean, Boolean> {
            var aDamage = 0
            var bDamage = 0
            var aDead = false
            var bDead = false
            fun step(aDeals: Boolean, bDeals: Boolean) {
                if (aDeals && !aDead && a.power > 0) { bDamage += a.power; if ("DT" in a.kw) bDamage = maxOf(bDamage, b.toughness) }
                if (bDeals && !bDead && b.power > 0) { aDamage += b.power; if ("DT" in b.kw) aDamage = maxOf(aDamage, a.toughness) }
                aDead = aDamage >= a.toughness
                bDead = bDamage >= b.toughness
            }
            val aFirst = "FS" in a.kw || "DS" in a.kw
            val bFirst = "FS" in b.kw || "DS" in b.kw
            if (aFirst || bFirst) step(aFirst, bFirst)
            step("FS" !in a.kw, "FS" !in b.kw)
            return aDead to bDead
        }

        /**
         * [b] blocks [a] alone, kills it and survives — and [a] gets nothing out of the fight. A
         * lifelinker still gains its damage from the blocker, so it is never eaten for nothing.
         */
        fun freeEat(a: Body, b: Body) = "LIFELINK" !in a.kw && canBlock(a, b) &&
            fight(a, b).let { (aDies, bDies) -> aDies && !bDies }
    }

    private fun build(ours: List<String>, theirs: List<String>, ourLife: Int, theirLife: Int): TestGame {
        var builder = scenario().withRngSeed(SCENARIO_SEED).withPlayers()
            .withLifeTotal(1, theirLife).withLifeTotal(2, ourLife)
        theirs.forEach { builder = builder.withCardOnBattlefield(1, it) }
        ours.forEach { builder = builder.withCardOnBattlefield(2, it) }
        repeat(10) { builder = builder.withCardInLibrary(1, "Craw Wurm").withCardInLibrary(2, "Craw Wurm") }
        return builder.build()
    }

    init {
        test("blocks: no pointless chumps, trade-downs, losing gang blocks or missed free kills when safe") {
            val failures = mutableListOf<String>()
            var checked = 0
            for (seed in listOf(1L, 2L, 3L)) {
                val rng = kotlin.random.Random(seed)
                repeat(150) { index ->
                    val theirs = List(rng.nextInt(1, 5)) { NAMES.random(rng) }
                    val ours = List(rng.nextInt(1, 5)) { NAMES.random(rng) }
                    val ourLife = rng.nextInt(8, 21)
                    val game = build(ours, theirs, ourLife, 20).advanceToDeclaration(1, Step.DECLARE_ATTACKERS)
                    val attackerIds = game.state.projectedState.getBattlefieldControlledBy(game.seatId(1))
                    game.execute(DeclareAttackers(game.seatId(1), attackerIds.associateWith { game.seatId(2) }))
                    game.advanceToDeclaration(2, Step.DECLARE_BLOCKERS)
                    val state = game.state
                    val aiId = game.seatId(2)
                    if (state.pendingDecision != null || state.priorityPlayerId != aiId) return@repeat
                    val name = { id: EntityId -> state.getEntity(id)?.get<CardComponent>()?.name ?: id.value }
                    val body = { id: EntityId -> POOL.getValue(name(id)) }
                    val totalPower = attackerIds.sumOf { hits(body(it)) }
                    val safe = ourLife > 2 * totalPower
                    checked++

                    val action = AIPlayer.create(cardRegistry, aiId, AiProfile.LIVE).chooseAction(state)
                    val blocks = (action as? DeclareBlockers)?.blockers.orEmpty()
                    game.execute(action)
                    game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                    val alive = game.state.getBattlefield().toSet()
                    val where = "seed=$seed[$index] life $ourLife (safe=$safe) — attackers ${attackerIds.map(name)}, " +
                        "ours ${ours}; chose ${blocks.entries.joinToString { (b, a) -> "${name(b)}→${a.map(name)}" }}"

                    val byAttacker = blocks.entries.groupBy({ it.value.first() }, { it.key })
                    for ((attacker, blockers) in byAttacker) {
                        val lost = blockers.filter { it !in alive }
                        val killed = attacker !in alive
                        if (!safe) continue
                        if (lost.isNotEmpty() && !killed) failures += "CHUMP $where"
                        // Against deathtouch every block is a trade, so which body pays is the
                        // evaluator's judgement, not a dominated choice.
                        if (killed && lost.size == 1 && blockers.size == 1 && "DT" !in body(attacker).kw &&
                            body(lost[0]).mv > body(attacker).mv + 1
                        ) failures += "TRADE-DOWN $where"
                        if (killed && lost.size >= 2 && lost.sumOf { body(it).mv } > body(attacker).mv) {
                            failures += "GANG 2-FOR-1 $where"
                        }
                    }
                    val blockedAttackers = byAttacker.keys
                    val idle = state.projectedState.getBattlefieldControlledBy(aiId).filter { it !in blocks.keys }
                    for (attacker in attackerIds.filter { it !in blockedAttackers }) {
                        val eater = idle.firstOrNull { freeEat(body(attacker), body(it)) }
                        if (eater != null) failures += "MISSED FREE KILL (${name(eater)} on ${name(attacker)}) $where"
                    }
                }
            }
            println("block-trade sweep: $checked positions, ${failures.size} failures")
            failures.shouldBeEmpty()
        }

        test("attacks: never send attackers the defender can all eat for free") {
            val failures = mutableListOf<String>()
            var checked = 0
            for (seed in listOf(11L, 12L, 13L)) {
                val rng = kotlin.random.Random(seed)
                repeat(150) { index ->
                    val ours = List(rng.nextInt(1, 5)) { NAMES.random(rng) }
                    val theirs = List(rng.nextInt(1, 5)) { NAMES.random(rng) }
                    val ourLife = rng.nextInt(6, 21)
                    val theirLife = rng.nextInt(6, 21)
                    // seat 1 = AI attacking
                    var builder = scenario().withRngSeed(SCENARIO_SEED).withPlayers()
                        .withLifeTotal(1, ourLife).withLifeTotal(2, theirLife)
                    ours.forEach { builder = builder.withCardOnBattlefield(1, it) }
                    theirs.forEach { builder = builder.withCardOnBattlefield(2, it) }
                    repeat(10) { builder = builder.withCardInLibrary(1, "Craw Wurm").withCardInLibrary(2, "Craw Wurm") }
                    val game = builder.build().advanceToDeclaration(1, Step.DECLARE_ATTACKERS)
                    val state = game.state
                    val aiId = game.seatId(1)
                    if (state.pendingDecision != null || state.priorityPlayerId != aiId) return@repeat
                    checked++
                    val name = { id: EntityId -> state.getEntity(id)?.get<CardComponent>()?.name ?: id.value }
                    val body = { id: EntityId -> POOL.getValue(name(id)) }
                    val action = AIPlayer.create(cardRegistry, aiId, AiProfile.LIVE).chooseAction(state)
                    val attackers = (action as? DeclareAttackers)?.attackers?.keys.orEmpty().toList()
                    if (attackers.isEmpty()) return@repeat
                    val blockers = state.projectedState.getBattlefieldControlledBy(game.seatId(2))
                    val where = "seed=$seed[$index] life $ourLife vs $theirLife — ours $ours, theirs $theirs; " +
                        "attacked with ${attackers.map(name)}"
                    // Bipartite matching: can every attacker be eaten free by a distinct blocker?
                    val match = mutableMapOf<EntityId, EntityId>()
                    fun augment(a: EntityId, seen: MutableSet<EntityId>): Boolean {
                        for (b in blockers) {
                            if (!freeEat(body(a), body(b)) || !seen.add(b)) continue
                            val holder = match[b]
                            if (holder == null || augment(holder, seen)) { match[b] = a; return true }
                        }
                        return false
                    }
                    if (attackers.all { augment(it, mutableSetOf()) }) failures += "ALL EATEN $where"
                }
            }
            println("attack-trade sweep: $checked positions, ${failures.size} failures")
            failures.shouldBeEmpty()
        }
    }
}
