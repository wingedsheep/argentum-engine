package com.wingedsheep.ai.engine

import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.drk.cards.GoblinRockSled
import com.wingedsheep.mtg.sets.definitions.rav.cards.BlazingArchon
import com.wingedsheep.sdk.core.AttackMode
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.maps.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

/**
 * Per-defender attack restrictions in a pod (CR 508.1b–c). Each attacker picks its own defender,
 * so "can't attack unless defending player controls a Mountain" (Goblin Rock Sled) and "creatures
 * can't attack you" (Blazing Archon) rule out one defender for one creature — never the attack.
 *
 * The engine publishes each attacker's legal defenders on the DeclareAttackers legal action
 * (`attackTargetsByAttacker`), and the AI picks from it. Before, the AI aimed every attacker at
 * the lowest-life opponent; one restricted creature got the whole declaration rejected and the
 * seat attacked with nothing (4-player AI-vs-AI log, 2026-10-12: six voided attacks in one game).
 */
class MultiplayerAttackDefenderLegalityTest : FunSpec({

    val cards = TestCards.all + GoblinRockSled + BlazingArchon

    fun setup(mode: AttackMode): Triple<GameTestDriver, List<EntityId>, CombatAdvisor> {
        val driver = GameTestDriver()
        driver.registerCards(cards)
        val players = driver.initMultiplayer(decks = List(4) { Deck.of("Plains" to 40) }, attackMode = mode)
        val registry = CardRegistry().apply { register(cards) }
        return Triple(driver, players, CombatAdvisor(GameSimulator(registry), AIPlayer.defaultEvaluator()))
    }

    fun GameTestDriver.readyAttacker(player: EntityId, name: String): EntityId =
        putCreatureOnBattlefield(player, name).also { removeSummoningSickness(it) }

    fun GameTestDriver.declareAction(player: EntityId): LegalAction =
        legalActions(player).single { it.actionType == "DeclareAttackers" }

    test("MULTIPLE: Goblin Rock Sled goes at the seat with a Mountain, the rest at the weakest seat") {
        val (driver, p, advisor) = setup(AttackMode.MULTIPLE)
        val (me, weak, mountainSeat) = p
        val sled = driver.readyAttacker(me, "Goblin Rock Sled")
        val giant = driver.readyAttacker(me, "Hill Giant")
        driver.putLandOnBattlefield(mountainSeat, "Mountain")
        driver.replaceState(driver.state.withLifeTotal(weak, 5))
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        val declare = driver.declareAction(me)
        declare.attackTargetsByAttacker?.get(sled) shouldBe listOf(mountainSeat)
        declare.attackTargetsByAttacker?.get(giant).shouldBeNull()

        val plan = advisor.chooseAttackers(driver.state, declare, me) as DeclareAttackers
        plan.attackers shouldBe mapOf(sled to mountainSeat, giant to weak)
        driver.submitSuccess(plan)
    }

    test("MULTIPLE: the AI attacks around a low-life seat behind Blazing Archon") {
        val (driver, p, advisor) = setup(AttackMode.MULTIPLE)
        val (me, archonSeat) = p
        driver.readyAttacker(me, "Hill Giant")
        driver.readyAttacker(me, "Hill Giant")
        driver.putCreatureOnBattlefield(archonSeat, "Blazing Archon")
        driver.replaceState(driver.state.withLifeTotal(archonSeat, 2))
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        val declare = driver.declareAction(me)
        declare.attackTargetsByAttacker.orEmpty().values.forEach { it shouldNotContain archonSeat }

        val plan = advisor.chooseAttackers(driver.state, declare, me) as DeclareAttackers
        plan.attackers.shouldNotBeEmpty()
        plan.attackers.values shouldNotContain archonSeat
        driver.submitSuccess(plan)
    }

    test("LEFT: a Rock Sled whose left opponent has no Mountain stays home; the rest still attack") {
        val (driver, p, advisor) = setup(AttackMode.LEFT)
        val (me, left, across) = p
        val sled = driver.readyAttacker(me, "Goblin Rock Sled")
        val giant = driver.readyAttacker(me, "Hill Giant")
        // Another opponent has a Mountain, so no single rule restricts the Sled from every
        // opponent — but under attack-left it may only attack [left], who has none.
        driver.putLandOnBattlefield(across, "Mountain")
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        val declare = driver.declareAction(me)
        declare.validAttackers.orEmpty() shouldNotContain sled

        val plan = advisor.chooseAttackers(driver.state, declare, me) as DeclareAttackers
        plan.attackers shouldBe mapOf(giant to left)
        driver.submitSuccess(plan)
    }

    test("LEFT: with Blazing Archon on the left nothing can attack, and the empty declaration is legal") {
        val (driver, p, advisor) = setup(AttackMode.LEFT)
        val (me, left) = p
        driver.readyAttacker(me, "Hill Giant")
        driver.putCreatureOnBattlefield(left, "Blazing Archon")
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        val declare = driver.declareAction(me)
        declare.validAttackers.orEmpty().shouldBeEmpty()

        val plan = advisor.chooseAttackers(driver.state, declare, me) as DeclareAttackers
        plan.attackers.shouldBeEmpty()
        driver.submitSuccess(plan)
    }

    test("the legalizer never lets one restricted attacker void the declaration") {
        val (driver, p, _) = setup(AttackMode.MULTIPLE)
        val (me, weak, mountainSeat) = p
        val sled = driver.readyAttacker(me, "Goblin Rock Sled")
        val giant = driver.readyAttacker(me, "Hill Giant")
        driver.putLandOnBattlefield(mountainSeat, "Mountain")
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        val declare = driver.declareAction(me)
        // The old plan shape: everything at one seat.
        val legal = CombatSeed.legalize(driver.state, declare, mapOf(sled to weak, giant to weak), weak)
        legal shouldBe mapOf(sled to mountainSeat, giant to weak)
        driver.submitSuccess(DeclareAttackers(me, legal))
    }
})
