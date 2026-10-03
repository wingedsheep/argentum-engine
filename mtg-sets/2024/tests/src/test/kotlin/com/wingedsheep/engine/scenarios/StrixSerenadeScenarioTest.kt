package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Strix Serenade (MH3): "Counter target artifact, creature, or planeswalker spell. Its controller
 * creates a 2/2 blue Bird creature token with flying."
 */
class StrixSerenadeScenarioTest : ScenarioTestBase() {

    init {
        context("Strix Serenade") {
            test("counters a creature spell; its controller creates a 2/2 flying Bird") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Strix Serenade")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withCardInHand(2, "Grizzly Bears")
                    .withLandsOnBattlefield(2, "Forest", 2)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(2, "Grizzly Bears").error shouldBe null
                game.passPriority()

                game.castSpellTargetingStackSpell(1, "Strix Serenade", "Grizzly Bears").error shouldBe null
                game.resolveStack()

                withClue("Grizzly Bears is countered") {
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                    game.findPermanent("Grizzly Bears") shouldBe null
                }

                val birds = game.findAllPermanents("Bird Token")
                withClue("the countered spell's controller (Player 2) gets one Bird") {
                    birds.size shouldBe 1
                    val bird = birds.single()
                    game.state.getEntity(bird)?.get<ControllerComponent>()?.playerId shouldBe game.player2Id
                    game.state.projectedState.getPower(bird) shouldBe 2
                    game.state.projectedState.getToughness(bird) shouldBe 2
                    game.state.projectedState.hasKeyword(bird, Keyword.FLYING) shouldBe true
                }
            }

            test("can target an artifact spell") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Strix Serenade")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withCardInHand(2, "Ornithopter")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(2, "Ornithopter").error shouldBe null
                game.passPriority()
                game.castSpellTargetingStackSpell(1, "Strix Serenade", "Ornithopter").error shouldBe null
                game.resolveStack()

                game.isInGraveyard(2, "Ornithopter") shouldBe true
                game.findAllPermanents("Bird Token").size shouldBe 1
            }

            test("a spell that can't be countered still gives its controller a Bird") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Strix Serenade")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withCardInHand(2, "Carnage Tyrant")
                    .withLandsOnBattlefield(2, "Forest", 6)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(2, "Carnage Tyrant").error shouldBe null
                game.passPriority()
                game.castSpellTargetingStackSpell(1, "Strix Serenade", "Carnage Tyrant").error shouldBe null
                game.resolveStack()

                withClue("Carnage Tyrant resolves") {
                    game.findPermanent("Carnage Tyrant") shouldNotBe null
                }
                val birds = game.findAllPermanents("Bird Token")
                birds.size shouldBe 1
                game.state.getEntity(birds.single())?.get<ControllerComponent>()?.playerId shouldBe game.player2Id
            }

            test("cannot target an instant spell") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Strix Serenade")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withCardInHand(2, "Lightning Bolt")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(2, "Lightning Bolt", 1).error shouldBe null
                game.passPriority()
                game.castSpellTargetingStackSpell(1, "Strix Serenade", "Lightning Bolt").error shouldNotBe null
            }
        }
    }
}
