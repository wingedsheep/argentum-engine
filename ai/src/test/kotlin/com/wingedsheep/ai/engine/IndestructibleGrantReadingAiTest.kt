package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.HoldPolicy
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.TimingVerdict
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * "Target permanent gains indestructible until end of turn. Create a 1/1 colorless Servo artifact
 * creature token." (Renegade's Getaway): protection spent only if the permanent would be destroyed
 * before cleanup, with a permanent rider beside it.
 *
 * Unread, the Servo paid for casting it in a main phase on an empty stack, throwing the
 * indestructible away. Held, it answers removal on the stack and still makes the Servo.
 */
class IndestructibleGrantReadingAiTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    init {
        test("Renegade's Getaway is held on an empty stack in our main phase, and answers Murder on the stack") {
            val idle = scenario().withPlayers()
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Renegade's Getaway")
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(2, "Swamp")
                .build().advanceToPriority(1, Step.PRECOMBAT_MAIN)
            val idleBears = idle.findPermanent("Grizzly Bears")!!
            val idleCast = CastSpell(
                idle.player1Id, idle.findCardsInHand(1, "Renegade's Getaway").first(),
                listOf(ChosenTarget.Permanent(idleBears)),
            )
            withClue("nothing can destroy the Bears before cleanup, so the indestructible would be thrown away") {
                HoldPolicy(intents, refuseUnspendableGrants = true)
                    .verdictFor(idle.state, idle.player1Id, "Renegade's Getaway", cast = idleCast) shouldBe TimingVerdict.NoWindow
            }

            val threatened = scenario().withPlayers()
                .withActivePlayer(2)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Renegade's Getaway")
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withCardInHand(2, "Murder")
                .withLandsOnBattlefield(2, "Swamp", 3)
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(2, "Swamp")
                .build().advanceToPriority(2, Step.PRECOMBAT_MAIN)
            val bears = threatened.findPermanent("Grizzly Bears")!!
            threatened.castSpell(2, "Murder", bears).error shouldBe null
            val cast = CastSpell(
                threatened.player1Id, threatened.findCardsInHand(1, "Renegade's Getaway").first(),
                listOf(ChosenTarget.Permanent(bears)),
            )
            withClue("Murder on the stack is the window the indestructible answers") {
                HoldPolicy(intents, refuseUnspendableGrants = true)
                    .verdictFor(threatened.state, threatened.player1Id, "Renegade's Getaway", cast = cast) shouldNotBe
                    TimingVerdict.NoWindow
            }
        }
    }
}
