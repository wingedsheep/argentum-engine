package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Warg Tactics (HOB) — {1}{G} Instant.
 *
 * "Choose one —
 *  • Destroy target creature with flying.
 *  • Put a +1/+1 counter on target creature you control. It gains trample and hexproof until end
 *    of turn."
 *
 * Mode 0's target filter is the point of the mode — a ground creature must be rejected. Mode 1
 * bundles a permanent counter with two granted keywords, and the hexproof is shown to actually
 * block an opponent's spell.
 */
class WargTacticsScenarioTest : ScenarioTestBase() {

    init {
        context("Warg Tactics") {

            test("mode 0 — destroys a creature with flying") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Warg Tactics")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    // Old Thrush is a 1/2 flier.
                    .withCardOnBattlefield(2, "Old Thrush")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val thrush = game.findPermanent("Old Thrush")!!
                game.castSpellWithMode(1, "Warg Tactics", modeIndex = 0, targetId = thrush)
                    .error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("the flier was destroyed") {
                    game.findPermanent("Old Thrush") shouldBe null
                    game.isInGraveyard(2, "Old Thrush") shouldBe true
                }
            }

            test("mode 0 — a creature without flying is not a legal target") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Warg Tactics")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                withClue("the mode reads 'target creature with flying'") {
                    game.castSpellWithMode(1, "Warg Tactics", modeIndex = 0, targetId = bears)
                        .error shouldNotBe null
                }
                game.isOnBattlefield("Grizzly Bears") shouldBe true
            }

            test("mode 1 — a +1/+1 counter plus trample and hexproof") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Warg Tactics")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardOnBattlefield(1, "Centaur Courser")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val courser = game.findPermanent("Centaur Courser")!!
                game.castSpellWithMode(1, "Warg Tactics", modeIndex = 1, targetId = courser)
                    .error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("a real +1/+1 counter, not a temporary pump") {
                    game.state.getEntity(courser)?.get<CountersComponent>()
                        ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
                    game.state.projectedState.getPower(courser) shouldBe 4
                    game.state.projectedState.getToughness(courser) shouldBe 4
                }
                withClue("both keywords granted") {
                    game.state.projectedState.hasKeyword(courser, Keyword.TRAMPLE) shouldBe true
                    game.state.projectedState.hasKeyword(courser, Keyword.HEXPROOF) shouldBe true
                }
            }

            test("mode 1 — the granted hexproof stops an opponent's removal") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Warg Tactics")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardOnBattlefield(1, "Centaur Courser")
                    .withCardInHand(2, "Lightning Bolt")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val courser = game.findPermanent("Centaur Courser")!!
                game.castSpellWithMode(1, "Warg Tactics", modeIndex = 1, targetId = courser)
                    .error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("hexproof makes it an illegal target for the opponent") {
                    game.castSpell(2, "Lightning Bolt", targetId = courser).error shouldNotBe null
                }
                game.isOnBattlefield("Centaur Courser") shouldBe true
            }

            test("mode 1 — an opponent's creature is not a legal target") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Warg Tactics")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val theirs = game.findPermanent("Grizzly Bears")!!
                withClue("the mode reads 'target creature you control'") {
                    game.castSpellWithMode(1, "Warg Tactics", modeIndex = 1, targetId = theirs)
                        .error shouldNotBe null
                }
            }
        }
    }
}
