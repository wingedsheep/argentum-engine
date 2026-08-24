package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.fem.cards.Heroism
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Heroism (Fallen Empires).
 *
 * The clause under test is who gets asked: the ransom is paid by each attacking creature's *own*
 * controller, not by Heroism's. A defender activating it against an attacking opponent is exactly
 * the board where the two differ, so that is the one used here.
 */
class HeroismScenarioTest : FunSpec({

    val abilityId = Heroism.activatedAbilities.first().id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(Heroism)
        return driver
    }

    test("an attacker whose controller can't pay deals no combat damage") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)

        // Alice is the attacker; Bob holds Heroism and sacrifices a white creature to it.
        val alice = driver.activePlayer!!
        val bob = driver.getOpponent(alice)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val attacker = driver.putCreatureOnBattlefield(alice, "Raging Goblin")
        driver.removeSummoningSickness(attacker)
        val heroism = driver.putPermanentOnBattlefield(bob, "Heroism")
        val fodder = driver.putCreatureOnBattlefield(bob, "Savannah Lions")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(alice, listOf(attacker), bob)
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareNoBlockers(bob)

        driver.submitSuccess(
            ActivateAbility(
                playerId = bob,
                sourceId = heroism,
                abilityId = abilityId,
                costPayment = com.wingedsheep.sdk.scripting.AdditionalCostPayment(
                    sacrificedPermanents = listOf(fodder)
                )
            )
        )
        // Alice controls no red mana, so the {2}{R} is unpayable and the prevention lands with no
        // prompt at all.
        driver.bothPass()

        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        withClue("the unblocked 1/1 dealt nothing") {
            driver.getLifeTotal(bob) shouldBe 20
        }
    }

    test("an attacker whose controller pays still connects") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)

        val alice = driver.activePlayer!!
        val bob = driver.getOpponent(alice)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val attacker = driver.putCreatureOnBattlefield(alice, "Raging Goblin")
        driver.removeSummoningSickness(attacker)
        val heroism = driver.putPermanentOnBattlefield(bob, "Heroism")
        val fodder = driver.putCreatureOnBattlefield(bob, "Savannah Lions")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(alice, listOf(attacker), bob)
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareNoBlockers(bob)

        driver.submitSuccess(
            ActivateAbility(
                playerId = bob,
                sourceId = heroism,
                abilityId = abilityId,
                costPayment = com.wingedsheep.sdk.scripting.AdditionalCostPayment(
                    sacrificedPermanents = listOf(fodder)
                )
            )
        )
        // The ransom is Alice's to pay, not Bob's — float it for her and say yes.
        driver.giveMana(alice, Color.RED, 3)
        driver.bothPass()
        driver.submitYesNo(alice, true)

        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        withClue("paying keeps the Goblin's damage") {
            driver.getLifeTotal(bob) shouldBe 19
        }
    }
})
