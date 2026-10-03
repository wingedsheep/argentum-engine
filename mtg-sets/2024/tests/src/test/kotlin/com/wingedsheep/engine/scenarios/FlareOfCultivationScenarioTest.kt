package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mh3.cards.FlareOfCultivation
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Flare of Cultivation {1}{G}{G} — Sorcery (MH3).
 *
 * "You may sacrifice a nontoken green creature rather than pay this spell's mana cost.
 *  Search your library for up to two basic land cards, reveal those cards, put one onto the
 *  battlefield tapped and the other into your hand, then shuffle."
 */
class FlareOfCultivationScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(FlareOfCultivation))
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20, "Plains" to 15, "Centaur Courser" to 5),
            skipMulligans = true,
            startingPlayer = 0
        )
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

    test("sacrificing a green creature: one basic enters tapped, the other goes to hand") {
        val driver = newDriver()
        val me = driver.player1

        val courser = driver.putCreatureOnBattlefield(me, "Centaur Courser")
        val flare = driver.putCardInHand(me, "Flare of Cultivation")
        val handBefore = driver.getHand(me).map { driver.getCardName(it) }

        castBySacrificing(driver, me, flare, courser).error shouldBe null
        withClue("the green creature paid the alternative cost") {
            driver.findPermanent(me, "Centaur Courser") shouldBe null
        }
        driver.bothPass()

        val search = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        withClue("only basic lands are searchable, up to two") {
            search.options.map { driver.getCardName(it) }.toSet() shouldBe setOf("Forest", "Plains")
            search.maxSelections shouldBe 2
            search.minSelections shouldBe 0
        }
        val forest = search.options.first { driver.getCardName(it) == "Forest" }
        val plains = search.options.first { driver.getCardName(it) == "Plains" }
        driver.submitCardSelection(me, listOf(forest, plains)).error shouldBe null

        val split = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        split.options shouldContainAll listOf(forest, plains)
        driver.submitCardSelection(me, listOf(plains)).error shouldBe null

        withClue("the chosen Plains entered the battlefield tapped") {
            driver.getPermanents(me).contains(plains) shouldBe true
            driver.isTapped(plains) shouldBe true
        }
        withClue("the Forest went to hand") {
            driver.getHand(me).contains(forest) shouldBe true
            driver.getPermanents(me).contains(forest) shouldBe false
        }
        withClue("the spell itself left the hand; only the Forest was added") {
            driver.getHand(me).map { driver.getCardName(it) }.sortedBy { it } shouldBe
                (handBefore - "Flare of Cultivation" + "Forest").sortedBy { it }
        }
    }

    test("a white creature can't pay the alternative cost") {
        val driver = newDriver()
        val me = driver.player1

        val lions = driver.putCreatureOnBattlefield(me, "Savannah Lions")
        val flare = driver.putCardInHand(me, "Flare of Cultivation")

        castBySacrificing(driver, me, flare, lions).error shouldNotBe null
        driver.findPermanent(me, "Savannah Lions") shouldNotBe null
    }
})
