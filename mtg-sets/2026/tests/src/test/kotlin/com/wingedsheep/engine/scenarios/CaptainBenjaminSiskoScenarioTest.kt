package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Captain Benjamin Sisko (TRK #6) — {1}{W} Legendary Creature — Human Officer 2/2.
 *
 *   At the beginning of your end step, create a 1/1 red Officer creature token for each dilemma
 *   you've faced this turn.
 *   {2}{W}{U}{B}{R}{G}, {T}: Exile the top five cards of your library. You may cast any number of
 *   spells with mana value less than or equal to the number of Officers you control from among
 *   those cards without paying their mana costs.
 *
 * Each Make It So cast with its token mode is one dilemma faced (and makes one Officer token of
 * its own), so two casts mean two more Officers at end step.
 */
class CaptainBenjaminSiskoScenarioTest : ScenarioTestBase() {

    private fun officerTokens(game: TestGame): List<EntityId> {
        val projected = game.state.projectedState
        return projected.getBattlefieldControlledBy(game.player1Id).filter {
            projected.hasSubtype(it, "Officer") &&
                game.state.getEntity(it)?.get<CardComponent>()?.name != "Captain Benjamin Sisko"
        }
    }

    init {
        context("Captain Benjamin Sisko") {

            test("two dilemmas faced this turn make two Officer tokens at your end step") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Captain Benjamin Sisko")
                    .withCardInHand(1, "Make It So")
                    .withCardInHand(1, "Make It So")
                    .withLandsOnBattlefield(1, "Island", 8)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellWithMode(1, "Make It So", modeIndex = 1).error shouldBe null
                game.resolveStack()
                game.castSpellWithMode(1, "Make It So", modeIndex = 1).error shouldBe null
                game.resolveStack()

                withClue("each Make It So made one Officer of its own") {
                    officerTokens(game).size shouldBe 2
                }

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                val officers = officerTokens(game)
                withClue("Sisko's end-step trigger added one Officer per dilemma faced") {
                    officers.size shouldBe 4
                }
                val projected = game.state.projectedState
                officers.forEach { token ->
                    projected.getPower(token) shouldBe 1
                    projected.getToughness(token) shouldBe 1
                    projected.getColors(token) shouldBe setOf(Color.RED.name)
                }
            }

            test("no dilemmas faced means no tokens") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Captain Benjamin Sisko")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                officerTokens(game).size shouldBe 0
            }

            test("the activated ability casts a spell with mana value up to your Officer count for free") {
                // Officers you control: Sisko + Starfleet Crew = 2. Grizzly Bears (MV 2) is castable,
                // Hill Giant (MV 4) is not, lands never are.
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Captain Benjamin Sisko")
                    .withCardOnBattlefield(1, "Starfleet Crew")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Hill Giant")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val sisko = game.findPermanent("Captain Benjamin Sisko")!!
                val ability = cardRegistry.getCard("Captain Benjamin Sisko")!!.script.activatedAbilities.single().id
                game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = sisko, abilityId = ability)
                ).error shouldBe null
                game.resolveStack()

                withClue("the top five cards were exiled") {
                    game.state.getExile(game.player1Id).size shouldBe 5
                }
                val decision = game.getPendingDecision()
                withClue("Sisko pauses for a cast-from-exile choice") {
                    (decision is SelectCardsDecision) shouldBe true
                }
                decision as SelectCardsDecision
                val offered = decision.options.mapNotNull { game.state.getEntity(it)?.get<CardComponent>()?.name }
                withClue("only the nonland spell with mana value <= 2 Officers is offered") {
                    offered.toSet() shouldBe setOf("Grizzly Bears")
                }

                game.selectCards(decision.options)
                game.resolveStack()

                game.isOnBattlefield("Grizzly Bears") shouldBe true
                withClue("Hill Giant and the lands stay in exile") {
                    game.state.getExile(game.player1Id).size shouldBe 4
                }
            }
        }
    }
}
