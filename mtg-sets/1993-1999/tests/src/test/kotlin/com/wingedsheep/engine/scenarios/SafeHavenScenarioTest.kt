package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.handlers.continuations.entityIdToChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.drk.cards.SafeHaven
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Safe Haven.
 *
 * "Each card exiled **with this land**" is the clause that matters: the return has to be the
 * Haven's own linked pile, not everything in exile. The second test puts an unrelated card in exile
 * first, so a return wired to the exile zone at large would drag it back and fail.
 */
private fun resolveUpkeepTrigger(driver: GameTestDriver, player: com.wingedsheep.sdk.model.EntityId, accept: Boolean) {
    var guard = 0
    while (guard++ < 12 && (driver.state.stack.isNotEmpty() || driver.pendingDecision != null)) {
        if (driver.pendingDecision != null) {
            driver.submitYesNo(player, accept)
        } else {
            driver.bothPass()
        }
    }
}

class SafeHavenScenarioTest : FunSpec({

    val exileAbilityId = SafeHaven.activatedAbilities.first().id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(SafeHaven)
        return driver
    }

    test("banked creatures come back when the Haven is sacrificed on upkeep") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)

        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val haven = driver.putPermanentOnBattlefield(me, "Safe Haven")
        val bear = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        driver.giveColorlessMana(me, 2)

        driver.submit(
            ActivateAbility(
                playerId = me,
                sourceId = haven,
                abilityId = exileAbilityId,
                targets = listOf(entityIdToChosenTarget(driver.state, bear)),
            )
        ).isSuccess shouldBe true
        driver.bothPass()

        withClue("the creature is banked in exile") {
            driver.findPermanent(me, "Grizzly Bears") shouldBe null
            driver.getExileCardNames(me) shouldBe listOf("Grizzly Bears")
        }

        // Round to my next upkeep, and take the trigger's offer.
        driver.passPriorityUntil(Step.END)
        driver.passPriorityUntil(Step.UPKEEP)
        driver.passPriorityUntil(Step.END)
        driver.passPriorityUntil(Step.UPKEEP)
        driver.activePlayer shouldBe me

        // The trigger goes on the stack at the upkeep; its "may" question only appears once the
        // trigger resolves, so pass first and then answer.
        resolveUpkeepTrigger(driver, me, accept = true)

        withClue("the Haven paid itself and gave the creature back") {
            driver.findPermanent(me, "Safe Haven") shouldBe null
            (driver.findPermanent(me, "Grizzly Bears") != null) shouldBe true
        }
    }

    test("declining the upkeep offer keeps the Haven and the pile") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)

        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val haven = driver.putPermanentOnBattlefield(me, "Safe Haven")
        val bear = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        driver.giveColorlessMana(me, 2)

        driver.submit(
            ActivateAbility(
                playerId = me,
                sourceId = haven,
                abilityId = exileAbilityId,
                targets = listOf(entityIdToChosenTarget(driver.state, bear)),
            )
        ).isSuccess shouldBe true
        driver.bothPass()

        driver.passPriorityUntil(Step.END)
        driver.passPriorityUntil(Step.UPKEEP)
        driver.passPriorityUntil(Step.END)
        driver.passPriorityUntil(Step.UPKEEP)

        resolveUpkeepTrigger(driver, me, accept = false)

        withClue("saying no costs nothing and returns nothing") {
            (driver.findPermanent(me, "Safe Haven") != null) shouldBe true
            driver.getExileCardNames(me) shouldBe listOf("Grizzly Bears")
        }
    }

    test("the upkeep trigger asks using the card's printed text") {
        // The "may" question used to be rendered from the effect tree, which reads bottom-up off
        // the pipeline steps: "You may sacrifice this creature. If you do, look at cards exiled by
        // this permanent. Put those cards onto the battlefield" — wrong noun for a land, a gather
        // described as a look, and no mention of whose control the cards return under. The prompt
        // is the authored ability text whenever the card supplies one.
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)

        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putPermanentOnBattlefield(me, "Safe Haven")

        driver.passPriorityUntil(Step.END)
        driver.passPriorityUntil(Step.UPKEEP)
        driver.passPriorityUntil(Step.END)
        driver.passPriorityUntil(Step.UPKEEP)

        var guard = 0
        while (guard++ < 12 && driver.pendingDecision == null) {
            driver.bothPass()
        }

        driver.pendingDecision?.prompt shouldBe
            "At the beginning of your upkeep, you may sacrifice this land. If you do, return each " +
            "card exiled with this land to the battlefield under its owner's control."
    }
})
