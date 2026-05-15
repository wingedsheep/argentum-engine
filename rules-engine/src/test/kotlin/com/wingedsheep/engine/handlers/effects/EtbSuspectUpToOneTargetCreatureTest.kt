package com.wingedsheep.engine.handlers.effects

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Step 4 — BDD test: ETB trigger suspects a chosen target creature, granting menace and no-block.
 *
 * GIVEN a controller has a permanent whose ETB triggered ability invokes Suspect
 *       with "up to one target creature"
 * AND   an opponent controls two untapped creatures eligible to be targeted
 * AND   the trigger is on the stack with one of the opponent's creatures chosen as the target
 * WHEN  the ETB trigger resolves, applying the Suspect effect to the targeted creature
 * THEN  the targeted creature is marked as suspected (has menace, can't block)
 * AND   the non-targeted opponent creature is not suspected, does not gain menace,
 *       and may still block normally
 */
class EtbSuspectUpToOneTargetCreatureTest : FunSpec({

    // Minimal creature with ETB "up to one target creature becomes suspected."
    val EtbSuspectSource = card("Suspicious Witness") {
        manaCost = "{1}{W}"
        colorIdentity = "W"
        typeLine = "Creature — Human"
        power = 1
        toughness = 1
        oracleText = "When Suspicious Witness enters the battlefield, " +
            "up to one target creature becomes suspected."

        triggeredAbility {
            trigger = Triggers.EntersBattlefield
            optional = true
            val t = target("target creature", Targets.Creature)
            effect = Effects.Suspect(t)
        }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(EtbSuspectSource))
        return driver
    }

    test("ETB trigger applies Suspect to the targeted creature granting menace and no-block") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20, "Swamp" to 20),
            startingLife = 20
        )

        val controller = driver.activePlayer!!
        val opponent = driver.getOpponent(controller)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // GIVEN — two untapped opponent creatures eligible to be targeted
        val targetedCreature = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        val otherCreature = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        // Cast the ETB-Suspect source creature
        val witnessId = driver.putCardInHand(controller, "Suspicious Witness")
        driver.giveMana(controller, Color.WHITE, 2)
        driver.castSpell(controller, witnessId).isSuccess shouldBe true

        // Both players pass → spell resolves, ETB trigger fires, game pauses for target selection
        driver.bothPass()

        // GIVEN — trigger is on the stack with one opponent creature chosen as target
        driver.submitTargetSelection(controller, listOf(targetedCreature))

        // WHEN — both players pass, trigger resolves applying the Suspect effect
        driver.bothPass()

        val projected = StateProjector().project(driver.state)

        // THEN — the targeted creature carries the suspected status (CR 701.60a/b)
        withClue("suspected creature should report isSuspected = true") {
            projected.isSuspected(targetedCreature) shouldBe true
        }

        // AND — gains menace (CR 701.60c)
        withClue("suspected creature should have menace") {
            projected.hasKeyword(targetedCreature, Keyword.MENACE) shouldBe true
        }

        // AND — cannot be declared as a blocker (CR 701.60c)
        withClue("suspected creature cannot be declared as a blocker") {
            projected.cantBlock(targetedCreature) shouldBe true
        }

        // AND — the non-targeted creature is completely unaffected
        withClue("non-targeted creature must not be suspected") {
            projected.isSuspected(otherCreature) shouldBe false
        }
        withClue("non-targeted creature must not have menace") {
            projected.hasKeyword(otherCreature, Keyword.MENACE) shouldBe false
        }
        withClue("non-targeted creature can still block normally") {
            projected.cantBlock(otherCreature) shouldBe false
        }
    }
})
