package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.handlers.continuations.entityIdToChosenTarget
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.hou.cards.EndlessSands
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Endless Sands — "{2}, {T}: Exile target creature you control. {4}, {T}, Sacrifice this land:
 * Return each creature card exiled with this land to the battlefield under its owner's control."
 *
 * The payoff sacrifices the land *as a cost*, so the linked-exile pile has to be read off a source
 * that is already in the graveyard by the time the ability resolves. The test banks two creatures —
 * one of them borrowed from the opponent — and checks both come back, each under its owner's
 * control.
 */
class EndlessSandsScenarioTest : FunSpec({

    val exileAbilityId = EndlessSands.activatedAbilities[1].id
    val returnAbilityId = EndlessSands.activatedAbilities[2].id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(EndlessSands)
        return driver
    }

    fun exile(driver: GameTestDriver, me: EntityId, sands: EntityId, creature: EntityId) {
        driver.untapPermanent(sands)
        driver.giveColorlessMana(me, 2)
        driver.submit(
            ActivateAbility(
                playerId = me,
                sourceId = sands,
                abilityId = exileAbilityId,
                targets = listOf(entityIdToChosenTarget(driver.state, creature)),
            )
        ).outcome shouldBe Outcome.Done
        driver.bothPass()
    }

    test("exile two, sacrifice the land, both return under their owners' control") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true)
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val sands = driver.putPermanentOnBattlefield(me, "Endless Sands")
        val bear = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        // A creature the opponent owns but I control (as if stolen).
        val courser = driver.putCreatureOnBattlefield(opponent, "Centaur Courser")
        driver.replaceState(driver.state.updateEntity(courser) { it.with(ControllerComponent(me)) })

        exile(driver, me, sands, bear)
        exile(driver, me, sands, courser)

        withClue("both creatures are banked in their owners' exile") {
            driver.getExileCardNames(me) shouldContain "Grizzly Bears"
            driver.getExileCardNames(opponent) shouldContain "Centaur Courser"
            driver.findPermanent(me, "Grizzly Bears") shouldBe null
            driver.findPermanent(me, "Centaur Courser") shouldBe null
        }

        driver.untapPermanent(sands)
        driver.giveColorlessMana(me, 4)
        driver.submit(ActivateAbility(playerId = me, sourceId = sands, abilityId = returnAbilityId))
            .outcome shouldBe Outcome.Done

        withClue("the land was sacrificed as part of the cost") {
            driver.getPermanents(me) shouldNotContain sands
        }

        driver.bothPass()

        withClue("each creature returns under its owner's control") {
            val returnedBear = driver.findPermanent(me, "Grizzly Bears")
            returnedBear shouldNotBe null
            driver.getController(returnedBear!!) shouldBe me

            val returnedCourser = driver.findPermanent(opponent, "Centaur Courser")
            returnedCourser shouldNotBe null
            driver.getController(returnedCourser!!) shouldBe opponent

            driver.getExileCardNames(me) shouldNotContain "Grizzly Bears"
            driver.getExileCardNames(opponent) shouldNotContain "Centaur Courser"
        }
    }
})
