package com.wingedsheep.engine.mechanics.combat

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.player.ControlCombatDeclarationsExecutor
import com.wingedsheep.engine.state.components.player.CombatDeclarationControlComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.CombatDeclaration
import com.wingedsheep.sdk.scripting.effects.ControlCombatDeclarationsEffect
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * "You choose which creatures block this combat and how those creatures block" (Odric, Master
 * Tactician): [ControlCombatDeclarationsEffect] with `duration = Duration.EndOfCombat`.
 *
 * The rules pinned:
 *  - the grant covers the block declarations of the combat it was created in;
 *  - it ends with that combat phase, so a later combat phase this turn is declared as usual, while a
 *    "this turn" grant held alongside it survives;
 *  - once the later "this combat" grant is gone, an earlier "this turn" grant decides again;
 *  - "this turn" / "this combat" are the only durations the effect takes.
 */
class CombatScopedDeclarationControlTest : FunSpec({

    val bear = card("Test Odric Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }

    val chooseBlocksThisCombat = ControlCombatDeclarationsEffect(
        declarations = setOf(CombatDeclaration.BLOCKERS),
        duration = Duration.EndOfCombat
    )

    fun driverWith(seats: Int): Pair<GameTestDriver, List<EntityId>> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + bear)
        val players = driver.initMultiplayer(decks = List(seats) { Deck.of("Forest" to 40) }, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver to players
    }

    fun GameTestDriver.resolve(controller: EntityId, effect: ControlCombatDeclarationsEffect) {
        replaceState(ControlCombatDeclarationsExecutor().execute(state, effect, EffectContext(sourceId = null, controllerId = controller)).state)
    }

    fun GameTestDriver.readyBear(owner: EntityId): EntityId =
        putCreatureOnBattlefield(owner, "Test Odric Bear").also { removeSummoningSickness(it) }

    test("you declare the defender's blocks this combat, and the grant is gone once combat ends") {
        val (driver, p) = driverWith(2)
        val attacker = driver.readyBear(p[0])
        val blocker = driver.readyBear(p[1])

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(p[0], mapOf(attacker to p[1])).error shouldBe null
        driver.resolve(p[0], chooseBlocksThisCombat)
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)

        CombatDeclarationControl.declarerFor(driver.state, p[1]) shouldBe p[0]
        driver.declareBlockers(p[1], mapOf(blocker to listOf(attacker))).error shouldBe null

        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        withClue("a later combat phase this turn is declared as usual") {
            CombatDeclarationControl.controllerOf(driver.state, CombatDeclaration.BLOCKERS, p[1]) shouldBe null
        }
        driver.state.getEntity(p[0])?.get<CombatDeclarationControlComponent>() shouldBe null
    }

    test("a 'this turn' grant outlives a 'this combat' one, and decides again once it ends") {
        val (driver, p) = driverWith(3)
        val attacker = driver.readyBear(p[0])
        driver.readyBear(p[1])
        // P3 took over every block declaration this turn (Master Warcraft); P1's Odric then takes
        // this combat's.
        driver.resolve(p[2], ControlCombatDeclarationsEffect(setOf(CombatDeclaration.BLOCKERS), Player.Each))

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(p[0], mapOf(attacker to p[1])).error shouldBe null
        driver.resolve(p[0], chooseBlocksThisCombat)
        withClue("the later grant decides this combat") {
            CombatDeclarationControl.controllerOf(driver.state, CombatDeclaration.BLOCKERS, p[1]) shouldBe p[0]
        }
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        CombatDeclarationControl.declarerFor(driver.state, p[1]) shouldBe p[0]
        driver.declareNoBlockers(p[1]).error shouldBe null

        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        withClue("P3's 'this turn' grant is in force again for a later combat this turn") {
            CombatDeclarationControl.controllerOf(driver.state, CombatDeclaration.BLOCKERS, p[1]) shouldBe p[2]
        }
    }

    test("lasts this turn or this combat only") {
        shouldThrow<IllegalArgumentException> {
            ControlCombatDeclarationsEffect(duration = Duration.UntilYourNextTurn)
        }
        chooseBlocksThisCombat.description shouldBe
            "You choose which creatures block this combat and how those creatures block"
    }
})
