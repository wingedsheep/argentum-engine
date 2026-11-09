package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.continuations.entityIdToChosenTarget
import com.wingedsheep.engine.handlers.effects.DamageUtils
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mkc.cards.FiendishDuo
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Fiendish Duo (MKC #153).
 *
 * {4}{R}{R} Creature — Devil 5/5
 * First strike
 * If a source would deal damage to an opponent, it deals double that damage to that player instead.
 *
 * The doubling reads *any* source — the Duo's controller's or the opponent's own — but only an
 * opponent *player* as the recipient: permanents an opponent controls, and the Duo's controller,
 * are untouched.
 */
class FiendishDuoScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(FiendishDuo))
        return driver
    }

    test("a spell's damage to an opponent is doubled") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(active, "Fiendish Duo")

        val bolt = driver.putCardInHand(active, "Lightning Bolt")
        driver.giveMana(active, Color.RED, 1)
        driver.castSpellWithTargets(
            active,
            bolt,
            listOf(entityIdToChosenTarget(driver.state, opponent)),
        ).error shouldBe null
        driver.bothPass()

        withClue("Lightning Bolt's 3 damage doubled to 6") {
            driver.getLifeTotal(opponent) shouldBe 14
        }
    }

    test("a spell's damage to a creature an opponent controls is not doubled") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(active, "Fiendish Duo")
        // 5/5 — survives an unamplified Bolt, would die to a doubled one.
        val theirCreature = driver.putCreatureOnBattlefield(opponent, "Force of Nature")

        val bolt = driver.putCardInHand(active, "Lightning Bolt")
        driver.giveMana(active, Color.RED, 1)
        driver.castSpellWithTargets(
            active,
            bolt,
            listOf(entityIdToChosenTarget(driver.state, theirCreature)),
        ).error shouldBe null
        driver.bothPass()

        withClue("3 damage leaves the 5/5 alive") {
            driver.getGraveyardCardNames(opponent) shouldBe emptyList()
        }
    }

    test("scope: any source, but only an opponent player as the recipient") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(active, "Fiendish Duo")
        val myCreature = driver.putCreatureOnBattlefield(active, "Centaur Courser")
        val theirCreature = driver.putCreatureOnBattlefield(opponent, "Centaur Courser")

        fun amplified(targetId: EntityId, sourceId: EntityId) =
            DamageUtils.applyStaticDamageAmplification(
                driver.zones.cardRegistry,
                driver.state, targetId = targetId, amount = 3, sourceId = sourceId,
                predicateEvaluator = PredicateEvaluator(cardRegistry = null)
            )

        withClue("your source dealing 3 to an opponent → 6") {
            amplified(opponent, myCreature) shouldBe 6
        }
        withClue("the opponent's own source dealing 3 to them → 6") {
            amplified(opponent, theirCreature) shouldBe 6
        }
        withClue("your source dealing 3 to a creature an opponent controls is NOT doubled") {
            amplified(theirCreature, myCreature) shouldBe 3
        }
        withClue("an opponent's source dealing 3 to you is NOT doubled") {
            amplified(active, theirCreature) shouldBe 3
        }
        withClue("your source dealing 3 to you is NOT doubled") {
            amplified(active, myCreature) shouldBe 3
        }
        withClue("an opponent's source dealing 3 to your creature is NOT doubled") {
            amplified(myCreature, theirCreature) shouldBe 3
        }
    }
})
