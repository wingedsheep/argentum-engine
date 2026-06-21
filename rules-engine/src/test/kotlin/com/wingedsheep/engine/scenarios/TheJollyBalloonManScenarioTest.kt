package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dsk.cards.TheJollyBalloonMan
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * The Jolly Balloon Man (DSK #219) — {1}{R}{W} 1/4 Legendary Creature — Human Clown, Haste.
 *
 * "{1}, {T}: Create a token that's a copy of another target creature you control, except it's a
 *  1/1 red Balloon creature in addition to its other colors and types and it has flying and haste.
 *  Sacrifice it at the beginning of the next end step. Activate only as a sorcery."
 *
 * Exercises the new `addedColors` modifier on `CreateTokenCopyOfTargetEffect` — the copy gains red
 * *in addition to* the copied creature's colors (Grizzly Bears is green → token is red + green),
 * is fixed at 1/1, gains the Balloon subtype, and gains flying + haste. It is then sacrificed at
 * the next end step.
 */
class TheJollyBalloonManScenarioTest : FunSpec({

    val abilityId = TheJollyBalloonMan.activatedAbilities.first().id

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun tokenCopyOf(driver: GameTestDriver, name: String, original: EntityId): EntityId =
        driver.state.getBattlefield(driver.player1).first {
            it != original &&
                driver.state.getEntity(it)?.has<TokenComponent>() == true
        }

    test("copy is a 1/1 red Balloon with flying + haste, keeping the copied creature's green color") {
        val driver = newDriver()
        val jolly = driver.putCreatureOnBattlefield(driver.player1, "The Jolly Balloon Man")
        driver.removeSummoningSickness(jolly)
        val bears = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")

        // {1} needs a mana source — give the player an untapped Mountain.
        val mountain = driver.putPermanentOnBattlefield(driver.player1, "Mountain")

        val result = driver.submit(
            ActivateAbility(
                playerId = driver.player1,
                sourceId = jolly,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Permanent(bears)),
                costPayment = null,
            )
        )
        result.error shouldBe null
        driver.bothPass()

        val token = tokenCopyOf(driver, "Grizzly Bears", bears)
        val projected = driver.state.projectedState
        projected.getPower(token) shouldBe 1
        projected.getToughness(token) shouldBe 1
        projected.hasColor(token, com.wingedsheep.sdk.core.Color.RED) shouldBe true
        projected.hasColor(token, com.wingedsheep.sdk.core.Color.GREEN) shouldBe true
        projected.getSubtypes(token).map { it.lowercase() } shouldContain "balloon"
        projected.hasKeyword(token, Keyword.FLYING) shouldBe true
        projected.hasKeyword(token, Keyword.HASTE) shouldBe true
    }

    test("the token copy is sacrificed at the next end step") {
        val driver = newDriver()
        val jolly = driver.putCreatureOnBattlefield(driver.player1, "The Jolly Balloon Man")
        driver.removeSummoningSickness(jolly)
        val bears = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")
        driver.putPermanentOnBattlefield(driver.player1, "Mountain")

        driver.submit(
            ActivateAbility(
                playerId = driver.player1,
                sourceId = jolly,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Permanent(bears)),
                costPayment = null,
            )
        )
        driver.bothPass()
        val token = tokenCopyOf(driver, "Grizzly Bears", bears)

        // Advance to the end step; the delayed sacrifice trigger should remove the token.
        driver.passPriorityUntil(Step.END)
        driver.bothPass()

        driver.state.getBattlefield(driver.player1).contains(token) shouldBe false
    }
})
