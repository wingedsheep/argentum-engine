package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.player.SkipNextUntapStepComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Shisato, Whispering Hunter (CHK #242) — "At the beginning of your upkeep, sacrifice a Snake. /
 * Whenever Shisato deals combat damage to a player, that player skips their next untap step."
 */
class ShisatoWhisperingHunterScenarioTest : ScenarioTestBase() {

    private fun TestGame.isTapped(name: String) =
        state.getEntity(findPermanent(name)!!)!!.has<TappedComponent>()

    init {
        context("Shisato, Whispering Hunter") {

            test("the damaged player skips their whole next untap step — every permanent type stays tapped") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Shisato, Whispering Hunter")
                    .withCardOnBattlefield(2, "Grizzly Bears", tapped = true)
                    .withCardOnBattlefield(2, "Forest", tapped = true)
                    .withCardOnBattlefield(2, "Ornithopter", tapped = true)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Shisato, Whispering Hunter" to 2)).error shouldBe null
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                game.resolveStack()

                withClue("Shisato hit Bob and the skip is pending, with its badge") {
                    game.getLifeTotal(2) shouldBe 18
                    game.state.getEntity(game.player2Id)!!.get<SkipNextUntapStepComponent>()?.steps shouldBe 1
                    game.getClientState(2).players.single { it.playerId == game.player2Id }
                        .activeEffects.map { it.effectId } shouldContain "skip_untap_step"
                }

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                withClue("it's Bob's turn") { game.state.activePlayerId shouldBe game.player2Id }
                withClue("creature, land and artifact all stayed tapped") {
                    game.isTapped("Grizzly Bears") shouldBe true
                    game.isTapped("Forest") shouldBe true
                    game.isTapped("Ornithopter") shouldBe true
                }
                withClue("the skip was used up") {
                    game.state.getEntity(game.player2Id)!!.has<SkipNextUntapStepComponent>() shouldBe false
                }

                // Alice's upkeep: Shisato is her only Snake, so it's sacrificed.
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveStack()
                withClue("Shisato sacrificed itself at Alice's upkeep") {
                    game.state.activePlayerId shouldBe game.player1Id
                    game.isInGraveyard(1, "Shisato, Whispering Hunter") shouldBe true
                }

                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                withClue("Bob's following untap step happens normally") {
                    game.state.activePlayerId shouldBe game.player2Id
                    game.isTapped("Grizzly Bears") shouldBe false
                    game.isTapped("Forest") shouldBe false
                    game.isTapped("Ornithopter") shouldBe false
                }
            }

            test("with another Snake around, Shisato's upkeep trigger can sacrifice that one instead") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Shisato, Whispering Hunter")
                    .withCardOnBattlefield(1, "Orochi Sustainer")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(2)
                    .inPhase(Phase.ENDING, Step.END)
                    .build()

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveStack()
                game.selectCards(listOf(game.findPermanent("Orochi Sustainer")!!)).error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Shisato, Whispering Hunter") shouldBe true
                game.isInGraveyard(1, "Orochi Sustainer") shouldBe true
            }
        }
    }
}
