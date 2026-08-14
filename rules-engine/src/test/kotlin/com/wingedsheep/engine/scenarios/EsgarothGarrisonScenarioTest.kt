package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.hob.cards.EsgarothGarrison
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Esgaroth Garrison (HOB #13) — {4}{W} Creature — Human Soldier, star/5.
 *
 * "Esgaroth Garrison's power is equal to the number of creatures you control."
 * "When this creature enters, recruit."
 *
 * The two abilities interact: the Garrison counts itself, and its own recruit trigger resolves
 * after it has entered, so a Soldier token minted that way immediately raises its power.
 */
class EsgarothGarrisonScenarioTest : ScenarioTestBase() {

    init {
        cardRegistry.register(EsgarothGarrison)

        context("Esgaroth Garrison") {

            test("power counts creatures you control; a recruited Soldier raises it") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Esgaroth Garrison")
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Forest")
                    .withLandsOnBattlefield(1, "Plains", 5)
                    // An opposing creature must not be counted — "creatures you control".
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Esgaroth Garrison").error shouldBe null
                game.resolveStack()

                val garrison = game.findPermanent("Esgaroth Garrison")!!
                withClue("alone on its side, the Garrison counts only itself") {
                    game.state.projectedState.getPower(garrison) shouldBe 1
                }
                withClue("toughness is printed, not dynamic") {
                    game.state.projectedState.getToughness(garrison) shouldBe 5
                }

                withClue("the enters trigger should pause for recruit's discard choice") {
                    game.hasPendingDecision() shouldBe true
                }
                val bears = game.findCardsInHand(1, "Grizzly Bears").single()
                game.selectCards(listOf(bears))
                game.resolveStack()

                withClue("the nonland discard mints one Human Soldier token") {
                    game.findAllPermanents("Human Soldier Token").size shouldBe 1
                }
                withClue("the Garrison plus its recruited Soldier is two creatures you control") {
                    game.state.projectedState.getPower(garrison) shouldBe 2
                }
            }
        }
    }
}
