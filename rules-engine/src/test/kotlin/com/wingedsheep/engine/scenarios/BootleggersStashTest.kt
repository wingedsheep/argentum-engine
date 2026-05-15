package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.legalactions.utils.CastPermissionUtils
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.blc.cards.BootleggersStash
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Bootleggers' Stash:
 * "Lands you control have '{T}: Create a Treasure token.'"
 *
 * Regression for a filter-fallthrough bug in [CastPermissionUtils.getStaticGrantedAbilitiesWithGranter]
 * (and its twins in ActivateAbilityHandler and ManaSolver). The old implementation only
 * matched `CardPredicate.IsCreature` and `CardPredicate.HasSubtype` from the filter and let
 * everything else fall through `else -> true`, *and* never checked `controllerPredicate`. So
 * `Land.youControl()` matched every permanent on the battlefield — Bootleggers' Stash itself
 * and opponents' lands included.
 */
class BootleggersStashTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(BootleggersStash)
        return driver
    }

    fun makeCastPermissionUtils(driver: GameTestDriver): CastPermissionUtils {
        val predicateEvaluator = PredicateEvaluator()
        val conditionEvaluator = ConditionEvaluator()
        return CastPermissionUtils(driver.cardRegistry, predicateEvaluator, conditionEvaluator)
    }

    test("granted '{T}: Create a Treasure token' only attaches to lands the granter's controller controls") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 40),
            skipMulligans = true
        )
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        val stash = driver.putPermanentOnBattlefield(activePlayer, "Bootleggers' Stash")
        val myForest = driver.putLandOnBattlefield(activePlayer, "Forest")
        val theirForest = driver.putLandOnBattlefield(opponent, "Forest")

        val utils = makeCastPermissionUtils(driver)

        // The granter (an Artifact) is not a Land — it must not receive its own grant.
        utils.getStaticGrantedAbilitiesWithGranter(stash, driver.state) shouldBe emptyList()

        // The active player's Forest is a Land they control — it receives the grant.
        val myGrants = utils.getStaticGrantedAbilitiesWithGranter(myForest, driver.state)
        myGrants.size shouldBe 1
        myGrants[0].granterId shouldBe stash

        // The opponent's Forest is a Land, but not "you control" relative to Stash's controller.
        utils.getStaticGrantedAbilitiesWithGranter(theirForest, driver.state) shouldBe emptyList()
    }
})
