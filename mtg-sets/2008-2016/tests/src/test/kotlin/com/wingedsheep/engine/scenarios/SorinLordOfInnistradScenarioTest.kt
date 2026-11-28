package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dka.cards.SorinLordOfInnistrad
import com.wingedsheep.mtg.sets.definitions.m13.cards.LilianaOfTheDarkRealms
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Sorin, Lord of Innistrad {2}{W}{B} — Legendary Planeswalker — Sorin (loyalty 3)
 *   −2: You get an emblem with "Creatures you control get +1/+0."
 *   −6: Destroy up to three target creatures and/or other planeswalkers. Return each card put
 *       into a graveyard this way to the battlefield under your control.
 *
 * Pins the −6's destroy-then-return pipeline (cards come back under your control, a token
 * doesn't), its "other planeswalkers" targeting, and the −2 emblem's anthem.
 */
class SorinLordOfInnistradScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(SorinLordOfInnistrad, LilianaOfTheDarkRealms))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun putSorin(driver: GameTestDriver, playerId: EntityId, loyalty: Int): EntityId {
        val sorin = driver.putPermanentOnBattlefield(playerId, "Sorin, Lord of Innistrad")
        driver.addComponent(sorin, CountersComponent(mapOf(CounterType.LOYALTY to loyalty)))
        return sorin
    }

    val abilities = SorinLordOfInnistrad.script.activatedAbilities

    test("−6 destroys a creature, a planeswalker and a token; the two cards return under your control") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        val sorin = putSorin(driver, me, 7)
        val bears = driver.putCreatureOnBattlefield(opp, "Grizzly Bears")
        val liliana = driver.putPermanentOnBattlefield(opp, "Liliana of the Dark Realms")
        driver.addComponent(liliana, CountersComponent(mapOf(CounterType.LOYALTY to 3)))
        val token = driver.putCreatureOnBattlefield(opp, "Centaur Courser")
        driver.addComponent(token, TokenComponent)

        driver.submitSuccess(
            ActivateAbility(
                playerId = me,
                sourceId = sorin,
                abilityId = abilities[2].id,
                targets = listOf(
                    ChosenTarget.Permanent(bears),
                    ChosenTarget.Permanent(liliana),
                    ChosenTarget.Permanent(token),
                )
            )
        )
        driver.bothPass()

        withClue("Grizzly Bears returns under my control") {
            driver.findPermanent(me, "Grizzly Bears").shouldNotBeNull()
            driver.findPermanent(opp, "Grizzly Bears").shouldBeNull()
        }
        withClue("Liliana returns under my control") {
            driver.findPermanent(me, "Liliana of the Dark Realms").shouldNotBeNull()
            driver.findPermanent(opp, "Liliana of the Dark Realms").shouldBeNull()
        }
        withClue("the destroyed token doesn't come back") {
            driver.findPermanent(me, "Centaur Courser").shouldBeNull()
            driver.findPermanent(opp, "Centaur Courser").shouldBeNull()
        }
        driver.getGraveyardCardNames(opp).contains("Grizzly Bears") shouldBe false
        driver.state.getEntity(sorin)!!.get<CountersComponent>()!!.getCount(CounterType.LOYALTY) shouldBe 1
    }

    test("−6 can't target Sorin himself — only other planeswalkers") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val sorin = putSorin(driver, me, 7)

        driver.submitExpectFailure(
            ActivateAbility(
                playerId = me,
                sourceId = sorin,
                abilityId = abilities[2].id,
                targets = listOf(ChosenTarget.Permanent(sorin))
            )
        )
    }

    test("−2 emblem gives creatures you control +1/+0, and keeps working after Sorin is gone") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        val sorin = putSorin(driver, me, 2)
        val bears = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val theirBears = driver.putCreatureOnBattlefield(opp, "Grizzly Bears")

        driver.submitSuccess(ActivateAbility(playerId = me, sourceId = sorin, abilityId = abilities[1].id))
        driver.bothPass()

        withClue("Sorin at 0 loyalty is gone") { driver.findPermanent(me, "Sorin, Lord of Innistrad").shouldBeNull() }
        driver.state.projectedState.getPower(bears) shouldBe 3
        driver.state.projectedState.getToughness(bears) shouldBe 2
        driver.state.projectedState.getPower(theirBears) shouldBe 2
    }
})
