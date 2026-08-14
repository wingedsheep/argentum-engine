package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseColorDecision
import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Giant's Boulder (HOB #173) — {1} Artifact.
 *
 * "When this artifact enters, scry 2.
 *  {1}, {T}: Add one mana of any color.
 *  {7}, {T}, Sacrifice this artifact: Destroy target permanent."
 *
 * All three abilities are exercised: the ETB scry (bottoming both cards actually reorders the
 * library), the any-color mana ability, and the sacrifice-destroy — which must both eat the
 * Boulder and destroy the target.
 */
class GiantsBoulderScenarioTest : ScenarioTestBase() {

    init {
        context("Giant's Boulder") {

            test("its ETB scries 2 — bottoming both cards changes the top of the library") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Giant's Boulder")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.cardNameAtTop(1) shouldBe "Mountain"

                game.castSpell(1, "Giant's Boulder").error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                val scry = game.getPendingDecision()
                withClue("the ETB raises a scry decision") {
                    (scry is SelectCardsDecision) shouldBe true
                }
                val scryOptions = (scry as SelectCardsDecision).options
                withClue("scry 2 looks at the top two cards") { scryOptions.size shouldBe 2 }
                // Put both on the bottom.
                game.selectCards(scryOptions).error shouldBe null
                game.resolveStack()

                withClue("the library is the same size — scry only reorders") {
                    game.librarySize(1) shouldBe 3
                }
                withClue("both looked-at cards went to the bottom, surfacing the Island") {
                    game.cardNameAtTop(1) shouldBe "Island"
                }
                game.isOnBattlefield("Giant's Boulder") shouldBe true
            }

            test("{1}, {T} adds one mana of the chosen color") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Giant's Boulder")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val boulder = game.findPermanent("Giant's Boulder")!!
                val manaAbility = cardRegistry.requireCard("Giant's Boulder")
                    .activatedAbilities.first { it.isManaAbility }

                game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = boulder, abilityId = manaAbility.id)
                ).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                val colorChoice = game.getPendingDecision()
                withClue("'any color' asks which color to add") {
                    (colorChoice is ChooseColorDecision) shouldBe true
                }
                game.submitDecision(ColorChosenResponse(colorChoice!!.id, Color.GREEN)).error shouldBe null

                withClue("one green mana is in the pool") {
                    game.state.getEntity(game.player1Id)!!.get<ManaPoolComponent>()!!.green shouldBe 1
                }
            }

            test("{7}, {T}, Sacrifice: destroys target permanent and the Boulder is gone") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Giant's Boulder")
                    .withLandsOnBattlefield(1, "Plains", 7)
                    .withCardOnBattlefield(2, "Centaur Courser")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val boulder = game.findPermanent("Giant's Boulder")!!
                val courser = game.findPermanent("Centaur Courser")!!
                val destroy = cardRegistry.requireCard("Giant's Boulder")
                    .activatedAbilities.first { !it.isManaAbility }

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id, sourceId = boulder, abilityId = destroy.id,
                        targets = listOf(ChosenTarget.Permanent(courser))
                    )
                ).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("the sacrifice cost ate the Boulder") {
                    game.findPermanent("Giant's Boulder") shouldBe null
                    game.isInGraveyard(1, "Giant's Boulder") shouldBe true
                }
                withClue("the targeted permanent was destroyed") {
                    game.findPermanent("Centaur Courser") shouldBe null
                    game.isInGraveyard(2, "Centaur Courser") shouldBe true
                }
            }

            test("the destroy ability is unaffordable on six lands") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Giant's Boulder")
                    .withLandsOnBattlefield(1, "Plains", 6)
                    .withCardOnBattlefield(2, "Centaur Courser")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val boulder = game.findPermanent("Giant's Boulder")!!
                val courser = game.findPermanent("Centaur Courser")!!
                val destroy = cardRegistry.requireCard("Giant's Boulder")
                    .activatedAbilities.first { !it.isManaAbility }

                withClue("{7} needs seven mana") {
                    game.execute(
                        ActivateAbility(
                            playerId = game.player1Id, sourceId = boulder, abilityId = destroy.id,
                            targets = listOf(ChosenTarget.Permanent(courser))
                        )
                    ).error shouldNotBe null
                }
                withClue("nothing was sacrificed or destroyed") {
                    game.isOnBattlefield("Giant's Boulder") shouldBe true
                    game.isOnBattlefield("Centaur Courser") shouldBe true
                }
            }
        }
    }

    private fun TestGame.libraryIds(playerNumber: Int): List<com.wingedsheep.sdk.model.EntityId> =
        state.getLibrary(if (playerNumber == 1) player1Id else player2Id)

    private fun TestGame.cardNameAtTop(playerNumber: Int): String? =
        state.getEntity(libraryIds(playerNumber).first())?.get<CardComponent>()?.name

    private fun TestGame.cardNameAtBottom(playerNumber: Int): String? =
        state.getEntity(libraryIds(playerNumber).last())?.get<CardComponent>()?.name
}
