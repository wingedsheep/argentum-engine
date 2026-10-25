package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.war.cards.BolassCitadel
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CreatureStats
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Bolas's Citadel (WAR #79) — the first *printed* (rather than durationally granted)
 * `PlayFromTopWithAlternativeCost`: the top spell is cast for life equal to its mana value with no
 * mana, the top land is played with the turn's land drop, and the last ability sacrifices ten
 * nonland permanents — the Citadel itself may be one of them.
 */
class BolassCitadelScenarioTest : FunSpec({

    val topBeast = CardDefinition(
        name = "Top Beast",
        manaCost = ManaCost.parse("{4}{G}"),
        typeLine = TypeLine.parse("Creature — Beast"),
        oracleText = "",
        creatureStats = CreatureStats(3, 3),
    )

    val trinket = CardDefinition(
        name = "Test Trinket",
        manaCost = ManaCost.parse("{1}"),
        typeLine = TypeLine.parse("Artifact"),
        oracleText = "",
    )

    fun newGame(): Triple<GameTestDriver, EntityId, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(BolassCitadel, topBeast, trinket))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!
        val opponent = driver.state.turnOrder.first { it != you }
        return Triple(driver, you, opponent)
    }

    fun resolveStack(driver: GameTestDriver) {
        var g = 0
        while (g++ < 30 && driver.state.stack.isNotEmpty() && !driver.isPaused) driver.bothPass()
    }

    fun libraryActions(driver: GameTestDriver, playerId: EntityId) =
        LegalActionEnumerator.create(driver.cardRegistry).enumerate(driver.state, playerId)
            .filter { it.sourceZone == "LIBRARY" }

    test("the top spell is cast for life equal to its mana value, with no mana") {
        val (driver, you, _) = newGame()
        val beast = driver.putCardOnTopOfLibrary(you, "Top Beast")
        libraryActions(driver, you).any { (it.action as? CastSpell)?.cardId == beast } shouldBe false

        driver.putPermanentOnBattlefield(you, "Bolas's Citadel")
        libraryActions(driver, you).any { (it.action as? CastSpell)?.cardId == beast } shouldBe true

        val lifeBefore = driver.getLifeTotal(you)
        driver.submit(CastSpell(playerId = you, cardId = beast, paymentStrategy = PaymentStrategy.FromPool))
            .error shouldBe null
        resolveStack(driver)

        driver.getLifeTotal(you) shouldBe lifeBefore - 5
        driver.findPermanent(you, "Top Beast") shouldNotBe null
    }

    test("a land on top is played with the land drop, and only once") {
        val (driver, you, _) = newGame()
        driver.putPermanentOnBattlefield(you, "Bolas's Citadel")
        val lifeBefore = driver.getLifeTotal(you)

        val swamp = driver.putCardOnTopOfLibrary(you, "Swamp")
        driver.playLand(you, swamp).error shouldBe null
        driver.getController(swamp) shouldBe you
        driver.getLifeTotal(you) shouldBe lifeBefore

        val second = driver.putCardOnTopOfLibrary(you, "Swamp")
        driver.submitExpectFailure(PlayLand(you, second))
    }

    test("tap and sacrifice ten nonland permanents, the Citadel among them: each opponent loses 10") {
        val (driver, you, opponent) = newGame()
        val citadel = driver.putPermanentOnBattlefield(you, "Bolas's Citadel")
        val trinkets = (1..9).map { driver.putPermanentOnBattlefield(you, "Test Trinket") }
        val opponentLife = driver.getLifeTotal(opponent)

        driver.submit(
            ActivateAbility(
                playerId = you,
                sourceId = citadel,
                abilityId = BolassCitadel.activatedAbilities.single().id,
                costPayment = AdditionalCostPayment(sacrificedPermanents = trinkets + citadel),
            )
        ).error shouldBe null
        resolveStack(driver)

        driver.getLifeTotal(opponent) shouldBe opponentLife - 10
        driver.findPermanent(you, "Bolas's Citadel") shouldBe null
        driver.findPermanent(you, "Test Trinket") shouldBe null
    }

    test("the last ability can't be activated with fewer than ten nonland permanents") {
        val (driver, you, _) = newGame()
        val citadel = driver.putPermanentOnBattlefield(you, "Bolas's Citadel")
        val trinkets = (1..8).map { driver.putPermanentOnBattlefield(you, "Test Trinket") }

        driver.submitExpectFailure(
            ActivateAbility(
                playerId = you,
                sourceId = citadel,
                abilityId = BolassCitadel.activatedAbilities.single().id,
                costPayment = AdditionalCostPayment(sacrificedPermanents = trinkets + citadel),
            )
        )
    }
})
