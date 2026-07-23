package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lci.cards.ContestedGameBall
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

/**
 * Contested Game Ball (LCI #251) — {2} Artifact.
 *
 *   Whenever you're dealt combat damage, the attacking player gains control of this artifact and
 *   untaps it.
 *   {2}, {T}: Draw a card and put a point counter on this artifact. Then if it has five or more
 *   point counters on it, sacrifice it and create a Treasure token.
 *
 * Pins: (1) the defensive combat-damage trigger hands control of the artifact to the attacking
 * player and untaps it; (2) the activated ability draws + adds a point counter below threshold; and
 * (3) the fifth point counter sacrifices the artifact and creates a Treasure.
 */
class ContestedGameBallScenarioTest : FunSpec({

    val abilityId = ContestedGameBall.activatedAbilities.single().id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ContestedGameBall) + PredefinedTokens.allTokens)
        return driver
    }

    fun pointCounters(driver: GameTestDriver, id: EntityId): Int =
        driver.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.POINT) ?: 0

    test("attacking player gains control of the artifact and untaps it when it deals you combat damage") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)

        val attacker = driver.activePlayer!!          // whose turn it is -> the attacking player
        val defender = driver.getOpponent(attacker)   // starts controlling the Game Ball

        // Defender controls the Game Ball, tapped (so we can prove it gets untapped).
        val ball = driver.putPermanentOnBattlefield(defender, "Contested Game Ball")
        driver.tapPermanent(ball)
        driver.getController(ball) shouldBe defender
        driver.isTapped(ball) shouldBe true

        // Attacker swings with a creature that connects with the defender.
        val bears = driver.putCreatureOnBattlefield(attacker, "Grizzly Bears")
        driver.removeSummoningSickness(bears)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(attacker, listOf(bears), defender).isSuccess shouldBe true
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareNoBlockers(defender).isSuccess shouldBe true

        // Combat damage step: defender takes 2, the trigger resolves.
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        // The attacking player now controls the Game Ball and it is untapped. "Gains control" is a
        // continuous control effect (a Layer.CONTROL floating effect), so it shows in projected
        // state — the base ControllerComponent stays the owner. Read the projected controller, as
        // every other control-change scenario does.
        driver.state.projectedState.getController(ball) shouldBe attacker
        driver.isTapped(ball) shouldBe false
    }

    test("a single activation draws, adds one point counter, and does not sacrifice") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val player = driver.activePlayer!!
        val ball = driver.putPermanentOnBattlefield(player, "Contested Game Ball")
        val handBefore = driver.getHand(player).size

        driver.giveMana(player, Color.BLUE, 2) // pays the {2}
        driver.submitSuccess(ActivateAbility(playerId = player, sourceId = ball, abilityId = abilityId))
        driver.bothPass()

        pointCounters(driver, ball) shouldBe 1
        driver.getHand(player).size shouldBe handBefore + 1
        driver.getCardName(ball) shouldBe "Contested Game Ball" // still on the battlefield
    }

    test("the fifth point counter sacrifices the artifact and creates a Treasure") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val player = driver.activePlayer!!
        val ball = driver.putPermanentOnBattlefield(player, "Contested Game Ball")
        // Seed four point counters so this activation reaches the five-counter threshold.
        driver.addComponent(ball, CountersComponent(mapOf(CounterType.POINT to 4)))

        driver.giveMana(player, Color.BLUE, 2)
        driver.submitSuccess(ActivateAbility(playerId = player, sourceId = ball, abilityId = abilityId))
        driver.bothPass()

        // Sacrificed: no longer on the battlefield.
        driver.findPermanent(player, "Contested Game Ball").shouldBeNull()
        // Exactly one Treasure token was created.
        driver.getPermanents(player).count { driver.getCardName(it) == "Treasure" } shouldBe 1
    }
})
