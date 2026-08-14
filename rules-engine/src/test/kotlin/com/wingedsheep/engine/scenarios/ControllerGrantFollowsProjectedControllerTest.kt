package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.targeting.ControllerShroud
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.GrantShroudToController
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * A controller grant ("You have shroud") follows whoever *currently* controls the permanent.
 *
 * Control change is a layer-2 continuous effect (`EffectApplicator`'s `ChangeController` branch
 * writes `projectedValues[..].controllerId`), so a stolen permanent's base `ControllerComponent`
 * still names its original controller for as long as the theft lasts. `ControllerGrants` therefore
 * resolves the granting permanent's controller through the projection — the same way it already
 * resolved "you" when evaluating an "as long as …" gate, and the same rule
 * [ReplacementFollowsProjectedControllerTest] pins for replacement effects.
 *
 * Reading the base component instead is silent and inverted rather than merely absent: the grant
 * keeps protecting the player who lost the permanent and never protects the one who gained it.
 * Both halves are asserted, because a fix that only stops the first would leave a stolen True
 * Believer granting shroud to nobody.
 *
 * Shroud stands in for all nine markers here — they share the one battlefield scan in
 * `ControllerGrants.anyGranting`, so the controller resolution is proven once for the family.
 * `ConditionalControllerGrantsTest` covers the gate; this covers who the gate is resolved for.
 */
class ControllerGrantFollowsProjectedControllerTest : FunSpec({

    val trueBeliever = card("True Believer") {
        manaCost = "{W}{W}"
        typeLine = "Creature — Human Cleric"
        oracleText = "You have shroud."
        power = 2
        toughness = 2
        staticAbility {
            ability = GrantShroudToController
        }
    }

    test("a stolen True Believer grants shroud to its thief, not to its owner") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(trueBeliever)
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)

        val owner = driver.player1
        val thief = driver.getOpponent(owner)

        val believer = driver.putCreatureOnBattlefield(owner, "True Believer")

        withClue("Control: before the theft the grant protects its owner") {
            ControllerShroud.appliesTo(driver.state, owner) shouldBe true
            ControllerShroud.appliesTo(driver.state, thief) shouldBe false
        }

        // Advance to the thief's precombat main so they can cast a sorcery-speed aura.
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        withClue("Setup: the thief should be the active player") {
            (driver.state.activePlayerId == thief) shouldBe true
        }

        driver.giveMana(thief, Color.BLUE, 4)
        val controlMagic = driver.putCardInHand(thief, "Control Magic")
        val cast = driver.castSpell(thief, controlMagic, targets = listOf(believer))
        withClue("Control Magic should resolve: ${cast.error}") { cast.error shouldBe null }
        driver.bothPass()

        withClue("Setup: the theft is a projection-only change, so the base component is stale") {
            driver.state.projectedState.getController(believer) shouldBe thief
            driver.state.getEntity(believer)?.get<ControllerComponent>()?.playerId shouldBe owner
        }

        withClue("The grant moved with the permanent, so it must stop protecting its owner") {
            ControllerShroud.appliesTo(driver.state, owner) shouldBe false
        }
        withClue("...and must start protecting whoever controls it now") {
            ControllerShroud.appliesTo(driver.state, thief) shouldBe true
        }
    }
})
