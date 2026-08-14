package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.TurnFaceUp
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mkm.cards.EssenceOfAntiquity
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Essence of Antiquity (MKM #15) — {3}{W}{W} 1/10 Artifact Creature — Golem with Disguise {2}{W}.
 *
 * "When this creature is turned face up, creatures you control gain hexproof until end of turn.
 *  Untap them."
 *
 * The trigger is one affected set doing two things, so both are checked on the same creatures: the
 * hexproof grant and the untap. The opponent's board is the control — an "each creature" reading of
 * the group filter would untap their attackers too, which is the failure this catches.
 */
class EssenceOfAntiquityScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(EssenceOfAntiquity))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    /** Cast the Golem face down for {3} and return the resulting face-down permanent. */
    fun castFaceDown(driver: GameTestDriver, player: EntityId): EntityId {
        val card = driver.putCardInHand(player, "Essence of Antiquity")
        driver.giveColorlessMana(player, 3)
        driver.submit(
            CastSpell(
                playerId = player,
                cardId = card,
                castFaceDown = true,
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).error shouldBe null
        driver.bothPass()
        return driver.getPermanents(player).single {
            driver.state.getEntity(it)?.has<FaceDownComponent>() == true
        }
    }

    fun flipFaceUp(driver: GameTestDriver, player: EntityId, golem: EntityId) {
        driver.giveColorlessMana(player, 2)
        driver.giveMana(player, Color.WHITE, 1)
        driver.submit(
            TurnFaceUp(
                playerId = player,
                sourceId = golem,
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).error shouldBe null
        repeat(2) { if (!driver.isPaused && driver.stackSize > 0) driver.bothPass() }
    }

    test("flipping it untaps your creatures and gives them hexproof, leaving the opponent's alone") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val opponent = driver.state.turnOrder.first { it != player }

        val bears = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        val theirBears = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        driver.tapPermanent(bears)
        driver.tapPermanent(theirBears)

        val golem = castFaceDown(driver, player)
        driver.tapPermanent(golem)

        withClue("nothing has happened yet") {
            driver.state.projectedState.hasKeyword(bears, Keyword.HEXPROOF) shouldBe false
            driver.isTapped(bears) shouldBe true
        }

        flipFaceUp(driver, player, golem)

        withClue("your creatures — including the Golem itself — are untapped and hexproof") {
            driver.isTapped(bears) shouldBe false
            driver.isTapped(golem) shouldBe false
            driver.state.projectedState.hasKeyword(bears, Keyword.HEXPROOF) shouldBe true
            driver.state.projectedState.hasKeyword(golem, Keyword.HEXPROOF) shouldBe true
        }
        withClue("the opponent's creature is untouched by both halves") {
            driver.isTapped(theirBears) shouldBe true
            driver.state.projectedState.hasKeyword(theirBears, Keyword.HEXPROOF) shouldBe false
        }
    }

    test("face down it is a vanilla 2/2; face up it is the printed 1/10") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        val golem = castFaceDown(driver, player)

        driver.state.projectedState.getPower(golem) shouldBe 2
        driver.state.projectedState.getToughness(golem) shouldBe 2

        flipFaceUp(driver, player, golem)

        driver.state.projectedState.getPower(golem) shouldBe 1
        driver.state.projectedState.getToughness(golem) shouldBe 10
    }
})
