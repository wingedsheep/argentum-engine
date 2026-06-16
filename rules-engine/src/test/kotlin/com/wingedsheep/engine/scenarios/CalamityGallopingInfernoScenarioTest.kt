package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SaddleMount
import com.wingedsheep.engine.state.components.battlefield.SaddledComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.CalamityGallopingInferno
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Calamity, Galloping Inferno (OTJ rare Horse Mount), {4}{R}{R}, 4/6.
 *
 * Haste
 * Whenever Calamity attacks while saddled, choose a nonlegendary creature that saddled it this
 * turn and create a tapped and attacking token that's a copy of it. Sacrifice that token at the
 * beginning of the next end step. Repeat this process once.
 * Saddle 1
 *
 * Exercises the "choose a nonlegendary creature that saddled it" selection (twice, for "repeat
 * once"), the tapped+attacking token copy, and the end-step sacrifice cleanup.
 */
class CalamityGallopingInfernoScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(CalamityGallopingInferno)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.isSaddled(id: EntityId): Boolean =
        state.getEntity(id)?.has<SaddledComponent>() == true

    fun GameTestDriver.tokenCopiesOf(playerId: EntityId, name: String): List<EntityId> =
        getCreatures(playerId).filter {
            getCardName(it) == name && state.getEntity(it)?.has<TokenComponent>() == true
        }

    test("attacks while saddled: two tapped attacking token copies of the nonlegendary saddler") {
        val driver = createDriver()
        val me = driver.player1
        val calamity = driver.putCreatureOnBattlefield(me, "Calamity, Galloping Inferno")
        val saddler = driver.putCreatureOnBattlefield(me, "Grizzly Bears") // nonlegendary, power 2 >= saddle 1
        driver.removeSummoningSickness(calamity)

        // Saddle Calamity with the bear.
        driver.submitSuccess(SaddleMount(me, calamity, listOf(saddler)))
        driver.bothPass()
        driver.isSaddled(calamity) shouldBe true

        // Attack with Calamity.
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(calamity), driver.player2)

        // The attacks-while-saddled trigger resolves twice ("repeat once"), each iteration choosing
        // the lone nonlegendary saddler (the bear). With a single legal choice the engine selects
        // it without prompting; if a choice is surfaced (ChooseTargetsDecision), answer it with the
        // bear. Drive combat forward until the two token copies have been created.
        var guard = 0
        while (driver.tokenCopiesOf(me, "Grizzly Bears").size < 2 && guard++ < 30) {
            val decision = driver.state.pendingDecision
            if (decision is ChooseTargetsDecision) {
                driver.submitTargetSelection(me, listOf(saddler))
            } else if (decision != null) {
                driver.autoResolveDecision()
            } else {
                driver.bothPass()
            }
            if (driver.state.step == Step.POSTCOMBAT_MAIN) break
        }

        // Two token copies of the bear exist, both tapped.
        val tokens = driver.tokenCopiesOf(me, "Grizzly Bears")
        tokens.size shouldBe 2
        tokens.all { driver.isTapped(it) } shouldBe true

        // Advance past the next end step; the tokens are sacrificed.
        driver.passPriorityUntil(Step.END)
        driver.passPriorityUntil(Step.UPKEEP) // next turn — past the end step the tokens die at
        driver.tokenCopiesOf(me, "Grizzly Bears").size shouldBe 0
    }

    test("attacks while NOT saddled: no token copies are created") {
        val driver = createDriver()
        val me = driver.player1
        val calamity = driver.putCreatureOnBattlefield(me, "Calamity, Galloping Inferno")
        driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        driver.removeSummoningSickness(calamity)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(calamity), driver.player2)
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        driver.tokenCopiesOf(me, "Grizzly Bears").size shouldBe 0
    }
})
