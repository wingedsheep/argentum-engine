package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.one.cards.NissaAscendedAnimist
import com.wingedsheep.mtg.sets.definitions.one.cards.VraskasFall
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Vraska's Fall (ONE #116) — {2}{B} Instant.
 *
 * "Each opponent sacrifices a creature or planeswalker of their choice and gets a poison counter."
 */
class VraskasFallScenarioTest : FunSpec({

    fun poisonOf(driver: GameTestDriver, player: com.wingedsheep.sdk.model.EntityId): Int =
        driver.state.getEntity(player)?.get<CountersComponent>()?.getCount(CounterType.POISON) ?: 0

    test("each opponent sacrifices their creature and gets a poison counter; caster is untouched") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(VraskasFall))
        val players = driver.initMultiplayer(
            decks = List(3) { Deck.of("Swamp" to 40) },
            skipMulligans = true,
            startingPlayer = 0,
        )
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val (you, oppA, oppB) = players
        val mine = driver.putCreatureOnBattlefield(you, "Grizzly Bears")
        driver.putCreatureOnBattlefield(oppA, "Grizzly Bears")
        driver.putCreatureOnBattlefield(oppB, "Hill Giant")

        val spell = driver.putCardInHand(you, "Vraska's Fall")
        driver.giveMana(you, Color.BLACK, 3)
        driver.castSpell(you, spell).error shouldBe null
        while (driver.state.stack.isNotEmpty()) driver.passPriority(driver.state.priorityPlayerId!!)

        driver.getGraveyardCardNames(oppA) shouldContain "Grizzly Bears"
        driver.getGraveyardCardNames(oppB) shouldContain "Hill Giant"
        driver.findPermanent(you, "Grizzly Bears") shouldBe mine
        poisonOf(driver, oppA) shouldBe 1
        poisonOf(driver, oppB) shouldBe 1
        poisonOf(driver, you) shouldBe 0
    }

    test("a planeswalker is a legal sacrifice") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(VraskasFall, NissaAscendedAnimist))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val walker = driver.putPermanentOnBattlefield(driver.player2, "Nissa, Ascended Animist")
        driver.addComponent(walker, CountersComponent(mapOf(CounterType.LOYALTY to 7)))
        driver.findPermanent(driver.player2, "Nissa, Ascended Animist") shouldNotBe null

        val spell = driver.putCardInHand(driver.player1, "Vraska's Fall")
        driver.giveMana(driver.player1, Color.BLACK, 3)
        driver.castSpell(driver.player1, spell).error shouldBe null
        driver.bothPass()

        driver.getGraveyardCardNames(driver.player2) shouldContain "Nissa, Ascended Animist"
        poisonOf(driver, driver.player2) shouldBe 1
    }

    test("an opponent with nothing to sacrifice still gets a poison counter") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(VraskasFall))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.addComponent(driver.player2, CountersComponent(mapOf(CounterType.POISON to 2)))

        val spell = driver.putCardInHand(driver.player1, "Vraska's Fall")
        driver.giveMana(driver.player1, Color.BLACK, 3)
        driver.castSpell(driver.player1, spell).error shouldBe null
        driver.bothPass()

        poisonOf(driver, driver.player2) shouldBe 3
        poisonOf(driver, driver.player1) shouldBe 0
    }
})
