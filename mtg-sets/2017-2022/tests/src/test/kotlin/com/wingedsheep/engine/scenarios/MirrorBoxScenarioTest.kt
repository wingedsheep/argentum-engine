package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.neo.cards.MirrorBox
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Mirror Box (NEO #250).
 *
 * Pins the first projection-time use of `sharingNameWith(AffectedEntity)`: each nontoken creature
 * counts the *other* same-named creatures you control (tokens included, opponents' excluded,
 * face-down ones nameless), tokens themselves are never pumped, legendary creatures get +1/+1 on
 * top, and the legend rule is switched off for your permanents.
 */
class MirrorBoxScenarioTest : FunSpec({

    val legend = card("Test Mirrored Legend") {
        manaCost = "{1}{G}"
        typeLine = "Legendary Creature — Human"
        power = 2
        toughness = 2
    }

    fun newGame(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(MirrorBox, legend))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver to driver.activePlayer!!
    }

    fun GameTestDriver.makeToken(id: EntityId) =
        replaceState(state.updateEntity(id) { it.with(TokenComponent) })

    fun GameTestDriver.pt(id: EntityId) =
        state.projectedState.let { it.getPower(id) to it.getToughness(id) }

    test("nontoken creatures get +1/+1 per other same-named creature you control; tokens count but aren't pumped") {
        val (driver, you) = newGame()
        val opponent = driver.getOpponent(you)
        driver.putPermanentOnBattlefield(you, "Mirror Box")
        val bearA = driver.putCreatureOnBattlefield(you, "Grizzly Bears")
        val bearB = driver.putCreatureOnBattlefield(you, "Grizzly Bears")
        val bearToken = driver.putCreatureOnBattlefield(you, "Grizzly Bears").also { driver.makeToken(it) }
        driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        val giant = driver.putCreatureOnBattlefield(you, "Hill Giant")

        withClue("two other Bears you control (one a token); the opponent's doesn't count") {
            driver.pt(bearA) shouldBe (4 to 4)
            driver.pt(bearB) shouldBe (4 to 4)
        }
        withClue("the token gets no bonus from the last ability") { driver.pt(bearToken) shouldBe (2 to 2) }
        withClue("no other Hill Giant") { driver.pt(giant) shouldBe (3 to 3) }
    }

    test("a face-down creature has no name, so it neither counts nor is counted") {
        val (driver, you) = newGame()
        driver.putPermanentOnBattlefield(you, "Mirror Box")
        val bear = driver.putCreatureOnBattlefield(you, "Grizzly Bears")
        val hidden = driver.putCreatureOnBattlefield(you, "Grizzly Bears")
        driver.replaceState(driver.state.updateEntity(hidden) { it.with(FaceDownComponent) })

        driver.pt(bear) shouldBe (2 to 2)
        driver.pt(hidden) shouldBe (2 to 2)
    }

    test("the legend rule doesn't apply; each copy of a legend gets +1/+1 plus +1/+1 per other copy") {
        val (driver, you) = newGame()
        driver.putPermanentOnBattlefield(you, "Mirror Box")
        val first = driver.putCreatureOnBattlefield(you, "Test Mirrored Legend")

        driver.giveMana(you, Color.GREEN, 2)
        val second = driver.putCardInHand(you, "Test Mirrored Legend")
        driver.castSpell(you, second).error shouldBe null
        var guard = 0
        while (guard++ < 30 && driver.state.stack.isNotEmpty() && !driver.isPaused) driver.bothPass()

        withClue("no legend-rule choice and both copies remain") {
            (driver.pendingDecision == null) shouldBe true
            driver.state.getBattlefield().count {
                driver.state.getEntity(it)?.get<CardComponent>()?.name == "Test Mirrored Legend"
            } shouldBe 2
        }
        withClue("2/2 base, +1/+1 legendary, +1/+1 for the other copy") { driver.pt(first) shouldBe (4 to 4) }
    }
})
