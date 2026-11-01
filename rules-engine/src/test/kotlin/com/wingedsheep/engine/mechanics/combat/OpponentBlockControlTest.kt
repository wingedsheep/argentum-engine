package com.wingedsheep.engine.mechanics.combat

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.combat.MustBlockGroupExecutor
import com.wingedsheep.engine.handlers.effects.player.ControlCombatDeclarationsExecutor
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.CombatDeclaration
import com.wingedsheep.sdk.scripting.effects.ControlCombatDeclarationsThisTurnEffect
import com.wingedsheep.sdk.scripting.effects.MustBlockGroupEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Brutal Hordechief's "creatures your opponents control block this turn if able, and you choose how
 * those creatures block": [MustBlockGroupEffect] plus [ControlCombatDeclarationsThisTurnEffect]
 * narrowed to the opponents' block declarations.
 *
 * The rules pinned, from Brutal Hordechief's rulings and CR 509.1 / 611.2c:
 *  - the must-block requirement reaches opponents' creatures that arrive later, never yours;
 *  - only the opponents' *block* declarations move — not an opponent's attack declaration on their
 *    own turn, and not your own blocks;
 *  - the blocks you choose must still be legal for their controller (the requirement holds);
 *  - with several such effects in one turn, the last to resolve decides for the players it covers;
 *  - a narrower grant doesn't erase a wider one the same player already holds (Master Warcraft);
 *  - everything lapses with the turn.
 */
class OpponentBlockControlTest : FunSpec({

    val bear = card("Test Hordechief Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }

    val chooseOpponentsBlocks = ControlCombatDeclarationsThisTurnEffect(
        declarations = setOf(CombatDeclaration.BLOCKERS),
        declaringPlayers = Player.EachOpponent
    )

    fun driverWith(seats: Int, startingPlayer: Int = 0): Pair<GameTestDriver, List<EntityId>> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + bear)
        val players = driver.initMultiplayer(decks = List(seats) { Deck.of("Forest" to 40) }, startingPlayer = startingPlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver to players
    }

    fun GameTestDriver.resolve(controller: EntityId, effect: ControlCombatDeclarationsThisTurnEffect) {
        replaceState(ControlCombatDeclarationsExecutor().execute(state, effect, EffectContext(sourceId = null, controllerId = controller)).state)
    }

    fun GameTestDriver.mustBlock(controller: EntityId) {
        val effect = MustBlockGroupEffect(GroupFilter.AllCreaturesOpponentsControl)
        replaceState(MustBlockGroupExecutor(PredicateEvaluator(cardRegistry = null).amounts).execute(state, effect, EffectContext(sourceId = null, controllerId = controller)).state)
    }

    fun GameTestDriver.readyBear(owner: EntityId): EntityId =
        putCreatureOnBattlefield(owner, "Test Hordechief Bear").also { removeSummoningSickness(it) }

    test("must block reaches opponents' creatures, including one that arrives later, and none of yours") {
        val (driver, p) = driverWith(3)
        val mine = driver.readyBear(p[0])
        val theirs = driver.readyBear(p[1])
        driver.mustBlock(p[0])
        val later = driver.readyBear(p[2])

        val projected = driver.state.projectedState
        projected.mustBlock(theirs) shouldBe true
        projected.mustBlock(later) shouldBe true
        projected.mustBlock(mine) shouldBe false
    }

    test("you declare the opponent's blocks, and they must still block if able") {
        val (driver, p) = driverWith(2)
        val attacker = driver.readyBear(p[0])
        val blocker = driver.readyBear(p[1])
        driver.mustBlock(p[0])
        driver.resolve(p[0], chooseOpponentsBlocks)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        withClue("your own attack declaration is yours as usual") {
            CombatDeclarationControl.declarerFor(driver.state, p[0]) shouldBe null
        }
        driver.declareAttackers(p[0], mapOf(attacker to p[1])).error shouldBe null
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)

        CombatDeclarationControl.declarerFor(driver.state, p[1]) shouldBe p[0]
        withClue("the defender's input window is routed to you") {
            CombatDeclarationControl.inputActorFor(driver.state, p[1]) shouldBe p[0]
        }
        withClue("declaring no block breaks the must-block requirement") {
            driver.declareNoBlockers(p[1]).error shouldNotBe null
        }
        driver.declareBlockers(p[1], mapOf(blocker to listOf(attacker))).error shouldBe null
    }

    test("an opponent's attack declaration on their own turn stays theirs; their defenders' blocks move") {
        // P2 is active and attacks P3; P1 holds the grant.
        val (driver, p) = driverWith(3, startingPlayer = 1)
        val attacker = driver.readyBear(p[1])
        driver.readyBear(p[2])
        driver.resolve(p[0], chooseOpponentsBlocks)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        CombatDeclarationControl.declarerFor(driver.state, p[1]) shouldBe null
        driver.declareAttackers(p[1], mapOf(attacker to p[2])).error shouldBe null
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        CombatDeclarationControl.declarerFor(driver.state, p[2]) shouldBe p[0]
    }

    test("your own blocks stay yours") {
        // P2 is active and attacks P1, the grant's holder.
        val (driver, p) = driverWith(2, startingPlayer = 1)
        val attacker = driver.readyBear(p[1])
        driver.readyBear(p[0])
        driver.resolve(p[0], chooseOpponentsBlocks)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(p[1], mapOf(attacker to p[0])).error shouldBe null
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        CombatDeclarationControl.declarerFor(driver.state, p[0]) shouldBe null
    }

    test("the last such effect to resolve decides, for the players it covers") {
        val (driver, p) = driverWith(3)
        driver.resolve(p[0], chooseOpponentsBlocks)
        driver.resolve(p[1], chooseOpponentsBlocks)

        val blocks = CombatDeclaration.BLOCKERS
        withClue("P3 is covered by both; P2's resolved last") {
            CombatDeclarationControl.controllerOf(driver.state, blocks, p[2]) shouldBe p[1]
        }
        CombatDeclarationControl.controllerOf(driver.state, blocks, p[0]) shouldBe p[1]
        withClue("only P1's effect covers P2") {
            CombatDeclarationControl.controllerOf(driver.state, blocks, p[1]) shouldBe p[0]
        }
    }

    test("a narrower grant keeps a wider one the same player holds") {
        val (driver, p) = driverWith(2)
        driver.resolve(p[1], ControlCombatDeclarationsThisTurnEffect())
        driver.resolve(p[1], chooseOpponentsBlocks)

        withClue("P2's Master Warcraft still hands it P1's attack declaration") {
            CombatDeclarationControl.controllerOf(driver.state, CombatDeclaration.ATTACKERS, p[0]) shouldBe p[1]
        }
        CombatDeclarationControl.controllerOf(driver.state, CombatDeclaration.BLOCKERS, p[0]) shouldBe p[1]
    }

    test("lapses with the turn") {
        val (driver, p) = driverWith(2)
        val theirs = driver.readyBear(p[1])
        driver.mustBlock(p[0])
        driver.resolve(p[0], chooseOpponentsBlocks)

        driver.passPriorityUntil(Step.UPKEEP, p[1])
        CombatDeclarationControl.controllerOf(driver.state, CombatDeclaration.BLOCKERS, p[1]) shouldBe null
        driver.state.projectedState.mustBlock(theirs) shouldBe false
    }
})
