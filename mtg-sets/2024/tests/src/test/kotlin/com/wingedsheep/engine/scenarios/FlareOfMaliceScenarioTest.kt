package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mh3.cards.FlareOfMalice
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Flare of Malice {2}{B}{B} — Instant (MH3).
 *
 * "You may sacrifice a nontoken black creature rather than pay this spell's mana cost.
 *  Each opponent sacrifices a creature or planeswalker with the greatest mana value among
 *  creatures and planeswalkers they control."
 */
class FlareOfMaliceScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(FlareOfMalice))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun castBySacrificing(driver: GameTestDriver, player: EntityId, flare: EntityId, fodder: EntityId) =
        driver.submit(
            CastSpell(
                playerId = player,
                cardId = flare,
                useAlternativeCost = true,
                alternativeCostType = AlternativeCostType.SELF_ALTERNATIVE,
                additionalCostPayment = AdditionalCostPayment(sacrificedPermanents = listOf(fodder)),
            )
        )

    test("sacrificing a nontoken black creature makes the opponent sacrifice their greatest-mana-value creature") {
        val driver = newDriver()
        val me = driver.player1
        val opponent = driver.player2

        val fodder = driver.putCreatureOnBattlefield(me, "Black Creature")
        val courser = driver.putCreatureOnBattlefield(opponent, "Centaur Courser")
        val lions = driver.putCreatureOnBattlefield(opponent, "Savannah Lions")
        val flare = driver.putCardInHand(me, "Flare of Malice")

        castBySacrificing(driver, me, flare, fodder).error shouldBe null
        withClue("the black creature paid the alternative cost") {
            driver.findPermanent(me, "Black Creature") shouldBe null
        }
        driver.bothPass()

        withClue("the three-drop is sacrificed, the one-drop survives") {
            driver.state.getBattlefield().contains(courser) shouldBe false
            driver.state.getBattlefield().contains(lions) shouldBe true
        }
    }

    test("with a tie, the opponent chooses which tied creature to sacrifice") {
        val driver = newDriver()
        val me = driver.player1
        val opponent = driver.player2

        val first = driver.putCreatureOnBattlefield(opponent, "Centaur Courser")
        val second = driver.putCreatureOnBattlefield(opponent, "Centaur Courser")
        val lions = driver.putCreatureOnBattlefield(opponent, "Savannah Lions")
        val flare = driver.putCardInHand(me, "Flare of Malice")
        driver.giveMana(me, Color.BLACK, 4)
        driver.castSpell(me, flare).error shouldBe null
        driver.bothPass()

        val decision = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        withClue("the opponent makes the choice") { decision.playerId shouldBe opponent }
        withClue("only the tied creatures are offered") {
            decision.options.shouldContainExactlyInAnyOrder(first, second)
        }
        driver.submitCardSelection(opponent, listOf(second)).error shouldBe null

        driver.state.getBattlefield().contains(second) shouldBe false
        driver.state.getBattlefield().contains(first) shouldBe true
        driver.state.getBattlefield().contains(lions) shouldBe true
    }

    test("a non-black creature can't pay the alternative cost") {
        val driver = newDriver()
        val me = driver.player1

        driver.putCreatureOnBattlefield(me, "Island Walker")
        val walker = driver.findPermanent(me, "Island Walker")!!
        val flare = driver.putCardInHand(me, "Flare of Malice")

        castBySacrificing(driver, me, flare, walker).error shouldNotBe null
        driver.findPermanent(me, "Island Walker") shouldNotBe null
    }
})
