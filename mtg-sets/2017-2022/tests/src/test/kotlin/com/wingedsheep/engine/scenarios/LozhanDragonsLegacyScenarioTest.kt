package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.identity.CommanderComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.clb.cards.LozhanDragonsLegacy
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Lozhan, Dragons' Legacy (CLB #281): "Whenever you cast an Adventure or Dragon spell, Lozhan deals
 * damage equal to that spell's mana value to any target that isn't a commander."
 *
 * Rulings: an Adventure's mana value is the Adventure's cost, not the creature's; a non-Dragon
 * adventurer cast as its creature doesn't trigger; players and non-commander permanents are legal.
 */
class LozhanDragonsLegacyScenarioTest : FunSpec({

    // {3}{G}{G} Giant whose Adventure costs {1}{R}.
    val storyGiant = card("Story Giant") {
        manaCost = "{3}{G}{G}"
        typeLine = "Creature — Giant"
        power = 5
        toughness = 5
        adventure("Tall Tale") {
            manaCost = "{1}{R}"
            typeLine = "Sorcery — Adventure"
            oracleText = "Draw a card."
            spell { effect = Effects.DrawCards(1) }
        }
    }
    val smallDragon = card("Small Dragon") {
        manaCost = "{2}{R}"
        typeLine = "Creature — Dragon"
        power = 2
        toughness = 2
    }

    fun setUp(): Triple<GameTestDriver, EntityId, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(LozhanDragonsLegacy, storyGiant, smallDragon))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40, "Forest" to 20), skipMulligans = true)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putCreatureOnBattlefield(player, "Lozhan, Dragons' Legacy")
        return Triple(driver, player, driver.getOpponent(player))
    }

    test("an Adventure spell deals damage equal to the Adventure's mana value") {
        val (driver, player, opponent) = setUp()
        val giant = driver.putCardInHand(player, "Story Giant")
        driver.giveMana(player, Color.RED, 2)

        driver.submit(CastSpell(player, giant, faceIndex = 0, paymentStrategy = PaymentStrategy.FromPool))
        driver.submitTargetSelection(player, listOf(opponent))
        driver.bothPass()

        driver.getLifeTotal(opponent) shouldBe 18
    }

    test("a Dragon spell deals damage equal to its mana value") {
        val (driver, player, opponent) = setUp()
        val dragon = driver.putCardInHand(player, "Small Dragon")
        driver.giveMana(player, Color.RED, 3)

        driver.submit(CastSpell(player, dragon, paymentStrategy = PaymentStrategy.FromPool))
        driver.submitTargetSelection(player, listOf(opponent))
        driver.bothPass()

        driver.getLifeTotal(opponent) shouldBe 17
    }

    test("a non-Dragon adventurer cast as its creature doesn't trigger") {
        val (driver, player, _) = setUp()
        val giant = driver.putCardInHand(player, "Story Giant")
        driver.giveMana(player, Color.GREEN, 5)

        driver.submitSuccess(CastSpell(player, giant, paymentStrategy = PaymentStrategy.FromPool))

        driver.isPaused shouldBe false
        driver.stackSize shouldBe 1
    }

    test("a commander can't be targeted; a non-commander creature can") {
        val (driver, player, opponent) = setUp()
        val commander = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        driver.addComponent(commander, CommanderComponent(ownerId = opponent))
        val bears = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        val dragon = driver.putCardInHand(player, "Small Dragon")
        driver.giveMana(player, Color.RED, 3)

        driver.submit(CastSpell(player, dragon, paymentStrategy = PaymentStrategy.FromPool))

        val decision = driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        val legal = decision.legalTargets.getValue(0)
        legal shouldNotContain commander
        legal shouldContain bears
        legal shouldContain opponent
    }
})
