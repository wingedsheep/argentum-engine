package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.mtg.sets.definitions.dsk.cards.InsidiousFungus
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Insidious Fungus (DSK #186).
 *
 * Insidious Fungus — {G} Creature — Fungus, 1/2
 *   "{2}, Sacrifice this creature: Choose one —
 *    • Destroy target artifact.
 *    • Destroy target enchantment.
 *    • Draw a card. Then you may put a land card from your hand onto the battlefield tapped."
 *
 * Exercises the modal *activated* ability: paying the cost (mana + sacrifice), the
 * resolution-time mode choice, and each mode's effect. The sacrifice cost moves the Fungus
 * to the graveyard regardless of which mode is chosen.
 */
class InsidiousFungusScenarioTest : ScenarioTestBase() {

    private val abilityId = InsidiousFungus.activatedAbilities.first().id

    init {
        context("Insidious Fungus — modal activated ability") {

            test("mode 1 destroys target artifact; the Fungus is sacrificed") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Insidious Fungus")
                    .withCardOnBattlefield(2, "Bonesplitter")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val fungus = game.findPermanent("Insidious Fungus")!!
                val artifact = game.findPermanent("Bonesplitter")!!

                val act = game.execute(ActivateAbility(game.player1Id, fungus, abilityId))
                withClue("Activating the ability should succeed: ${act.error}") {
                    act.error shouldBe null
                }
                game.resolveStack()

                val modeDecision = game.state.pendingDecision as? ChooseOptionDecision
                    ?: error("expected a ChooseOptionDecision; got ${game.state.pendingDecision}")
                game.submitDecision(OptionChosenResponse(modeDecision.id, optionIndex = 0))

                val targetDecision = game.state.pendingDecision as? ChooseTargetsDecision
                    ?: error("expected a ChooseTargetsDecision after mode pick; got ${game.state.pendingDecision}")
                game.submitDecision(TargetsResponse(targetDecision.id, mapOf(0 to listOf(artifact))))
                game.resolveStack()

                withClue("The artifact is destroyed") {
                    game.isInGraveyard(2, "Bonesplitter") shouldBe true
                }
                withClue("The Fungus was sacrificed as a cost") {
                    game.isInGraveyard(1, "Insidious Fungus") shouldBe true
                }
            }

            test("mode 2 destroys target enchantment") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Insidious Fungus")
                    // A non-Aura enchantment: an unattached Aura would be put into its owner's
                    // graveyard by the state-based check (CR 704.5m) before the ability resolves.
                    .withCardOnBattlefield(2, "Glorious Anthem")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val fungus = game.findPermanent("Insidious Fungus")!!
                val anthem = game.findPermanent("Glorious Anthem")!!

                game.execute(ActivateAbility(game.player1Id, fungus, abilityId)).error shouldBe null
                game.resolveStack()

                val modeDecision = game.state.pendingDecision as? ChooseOptionDecision
                    ?: error("expected a ChooseOptionDecision; got ${game.state.pendingDecision}")
                game.submitDecision(OptionChosenResponse(modeDecision.id, optionIndex = modeDecision.options.indexOfFirst { it.contains("enchantment", ignoreCase = true) }))

                val targetDecision = game.state.pendingDecision as? ChooseTargetsDecision
                    ?: error("expected a ChooseTargetsDecision after mode pick")
                game.submitDecision(TargetsResponse(targetDecision.id, mapOf(0 to listOf(anthem))))
                game.resolveStack()

                withClue("The enchantment is destroyed") {
                    game.isInGraveyard(2, "Glorious Anthem") shouldBe true
                }
            }

            test("mode 3 draws a card and may put a land from hand onto the battlefield tapped") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Insidious Fungus")
                    .withCardInHand(1, "Mountain")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val fungus = game.findPermanent("Insidious Fungus")!!
                val handBefore = game.handSize(1)

                game.execute(ActivateAbility(game.player1Id, fungus, abilityId)).error shouldBe null
                game.resolveStack()

                val modeDecision = game.state.pendingDecision as? ChooseOptionDecision
                    ?: error("expected a ChooseOptionDecision; got ${game.state.pendingDecision}")
                game.submitDecision(OptionChosenResponse(modeDecision.id, optionIndex = modeDecision.options.indexOfFirst { it.contains("Draw", ignoreCase = true) }))
                game.resolveStack()

                // Drew Grizzly Bears (+1 to hand); the optional land put surfaces a selection.
                if (game.hasPendingDecision()) {
                    val land = game.findCardsInHand(1, "Mountain").firstOrNull()
                    if (land != null) game.selectCards(listOf(land)) else game.skipSelection()
                    game.resolveStack()
                }

                withClue("Drew a card (Grizzly Bears in hand)") {
                    game.isInHand(1, "Grizzly Bears") shouldBe true
                }
                withClue("The Mountain was put onto the battlefield tapped") {
                    game.isOnBattlefield("Mountain") shouldBe true
                }
                // hand: -Mountain (put to battlefield) +Grizzly Bears = net unchanged
                game.handSize(1) shouldBe handBefore
            }
        }
    }
}
