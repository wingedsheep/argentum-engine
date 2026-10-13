package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.SearchLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Urza's Sylex (BRO #40) — {3} Legendary Artifact.
 *
 * "{2}{W}{W}, {T}, Exile Urza's Sylex: Each player chooses six lands they control. Destroy all
 * other permanents. Activate only as a sorcery.
 * When Urza's Sylex is put into exile from the battlefield, you may pay {2}. If you do, search your
 * library for a planeswalker card, reveal it, put it into your hand, then shuffle."
 *
 * Covers the composition that needs the engine to run it: the exile-as-cost trigger resolving
 * first (ruling 2022-10-14) and the per-player six-land keep feeding one destroy of everything else
 * on the battlefield; plus the declined payment and a player with fewer than six lands keeping all.
 */
class UrzasSylexScenarioTest : ScenarioTestBase() {

    private val sylexAbilityId =
        cardRegistry.getCard("Urza's Sylex")!!.activatedAbilities.first().id

    /**
     * Drive the stack and every decision: answer the may-pay with [pay], auto-pay its mana, take
     * [searchPick] from the library search, and keep the first six offered lands per player.
     */
    private fun TestGame.drive(pay: Boolean, searchPick: (List<EntityId>) -> List<EntityId>) {
        var guard = 0
        while ((state.stack.isNotEmpty() || state.pendingDecision != null) && guard++ < 40) {
            when (val d = state.pendingDecision) {
                null -> passPriority()
                is YesNoDecision -> answerYesNo(pay)
                is SelectManaSourcesDecision -> submitManaSourcesAutoPay()
                is SearchLibraryDecision -> submitDecision(CardsSelectedResponse(d.id, searchPick(d.options)))
                is SelectCardsDecision ->
                    // The land keep runs on the targeting UI; anything else is the library search.
                    if (d.useTargetingUI) submitDecision(CardsSelectedResponse(d.id, d.options.take(6)))
                    else submitDecision(CardsSelectedResponse(d.id, searchPick(d.options)))
                else -> error("Unexpected decision $d")
            }
        }
    }

    init {
        context("Urza's Sylex") {

            test("fetches a planeswalker first, then each player keeps six lands and the rest is destroyed") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Urza's Sylex", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Plains", 9)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(2, "Forest", 8)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInLibrary(1, "Jace Beleren")
                    .withCardInLibrary(1, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val activation = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = game.findPermanent("Urza's Sylex")!!,
                        abilityId = sylexAbilityId,
                    )
                )
                withClue("activation succeeds: ${activation.error}") { activation.error shouldBe null }
                withClue("the Sylex is exiled as part of the cost") {
                    game.isInExile(1, "Urza's Sylex") shouldBe true
                }

                var guard = 0
                while (game.state.pendingDecision == null && guard++ < 10) game.passPriority()
                withClue("the exile trigger resolves above the ability: its may-pay comes before any land keep") {
                    (game.state.pendingDecision is YesNoDecision) shouldBe true
                }

                game.drive(pay = true) { options -> options }
                withClue("the planeswalker was searched into hand") {
                    game.isInHand(1, "Jace Beleren") shouldBe true
                }
                withClue("each player kept exactly six lands") {
                    game.findPermanents("Plains").size shouldBe 6
                    game.findPermanents("Forest").size shouldBe 6
                }
                withClue("every other permanent was destroyed") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                    game.findCardsInGraveyard(1, "Plains").size shouldBe 3
                    game.findCardsInGraveyard(2, "Forest").size shouldBe 2
                }
            }

            test("declining the payment leaves the library alone; fewer than six lands are all kept") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Urza's Sylex", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Plains", 7)
                    .withLandsOnBattlefield(2, "Forest", 3)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInLibrary(1, "Jace Beleren")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = game.findPermanent("Urza's Sylex")!!,
                        abilityId = sylexAbilityId,
                    )
                ).error shouldBe null

                game.drive(pay = false) { error("no search should be offered after declining") }

                withClue("no search happened") { game.isInHand(1, "Jace Beleren") shouldBe false }
                withClue("a player with three lands keeps all three") {
                    game.findPermanents("Forest").size shouldBe 3
                }
                withClue("the active player keeps six of seven") {
                    game.findPermanents("Plains").size shouldBe 6
                }
                game.isOnBattlefield("Grizzly Bears") shouldBe false
            }
        }
    }
}
