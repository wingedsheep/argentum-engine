package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Thorin's Last Stand (HOB) — {2}{W}{W} Instant.
 *
 * "Choose one —
 *  • Creatures you control get +2/+1 until end of turn.
 *  • Destroy target artifact or enchantment. You gain 2 life."
 *
 * Mode 0 is a one-sided team pump; mode 1 pairs a destruction with a life gain and must reject a
 * creature as its target.
 */
class ThorinsLastStandScenarioTest : ScenarioTestBase() {

    init {
        context("Thorin's Last Stand") {

            test("mode 0 — creatures you control get +2/+1, opponents' do not") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Thorin's Last Stand")
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withCardOnBattlefield(1, "Centaur Courser")
                    .withCardOnBattlefield(1, "Savannah Lions")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val courser = game.findPermanent("Centaur Courser")!!
                val lions = game.findPermanent("Savannah Lions")!!
                val theirs = game.findPermanent("Grizzly Bears")!!
                val spell = game.state.getHand(game.player1Id).single()

                game.execute(
                    CastSpell(game.player1Id, spell, emptyList(), chosenModes = listOf(0))
                ).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("+2/+1 on each of your creatures") {
                    game.state.projectedState.getPower(courser) shouldBe 5
                    game.state.projectedState.getToughness(courser) shouldBe 4
                    game.state.projectedState.getPower(lions) shouldBe 3
                    game.state.projectedState.getToughness(lions) shouldBe 2
                }
                withClue("the opponent's creature is untouched") {
                    game.state.projectedState.getPower(theirs) shouldBe 2
                    game.state.projectedState.getToughness(theirs) shouldBe 2
                }
                withClue("mode 0 gains no life") { game.getLifeTotal(1) shouldBe 20 }
            }

            test("mode 1 — destroys a targeted artifact and gains 2 life") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Thorin's Last Stand")
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withCardOnBattlefield(2, "Well-Worn Spatula")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val spatula = game.findPermanent("Well-Worn Spatula")!!
                game.castSpellWithMode(1, "Thorin's Last Stand", modeIndex = 1, targetId = spatula)
                    .error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("the artifact was destroyed into its owner's graveyard") {
                    game.findPermanent("Well-Worn Spatula") shouldBe null
                    game.isInGraveyard(2, "Well-Worn Spatula") shouldBe true
                }
                withClue("and the caster gained 2 life") {
                    game.getLifeTotal(1) shouldBe 22
                }
            }

            test("mode 1 — destroys a targeted enchantment too") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Thorin's Last Stand")
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withCardOnBattlefield(2, "Fateful Discovery")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val enchantment = game.findPermanent("Fateful Discovery")!!
                game.castSpellWithMode(1, "Thorin's Last Stand", modeIndex = 1, targetId = enchantment)
                    .error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                game.findPermanent("Fateful Discovery") shouldBe null
                game.getLifeTotal(1) shouldBe 22
            }

            test("mode 1 — a creature is not a legal target") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Thorin's Last Stand")
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                withClue("the mode reads 'artifact or enchantment'") {
                    game.castSpellWithMode(1, "Thorin's Last Stand", modeIndex = 1, targetId = bears)
                        .error shouldNotBe null
                }
                withClue("nothing happened") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                    game.getLifeTotal(1) shouldBe 20
                }
            }
        }
    }
}
