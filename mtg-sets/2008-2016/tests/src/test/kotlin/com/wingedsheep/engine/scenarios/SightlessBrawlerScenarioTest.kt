package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Sightless Brawler: "This creature can't attack alone. Enchanted creature gets +3/+2 and can't
 * attack alone." Bestowed, the restriction moves to the host; on its own, it binds the Brawler.
 */
class SightlessBrawlerScenarioTest : ScenarioTestBase() {

    init {
        test("as a creature it can't attack alone, but may alongside another creature") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Sightless Brawler", summoningSickness = false)
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            game.declareAttackers(mapOf("Sightless Brawler" to 2)).error shouldNotBe null
            game.declareAttackers(mapOf("Sightless Brawler" to 2, "Grizzly Bears" to 2)).error shouldBe null
        }

        test("bestowed, the host gets +3/+2 and can't attack alone") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withCardOnBattlefield(1, "Savannah Lions", summoningSickness = false)
                .withCardInHand(1, "Sightless Brawler")
                .withLandsOnBattlefield(1, "Plains", 5)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.execute(
                CastSpell(
                    playerId = game.player1Id,
                    cardId = game.findCardsInHand(1, "Sightless Brawler").single(),
                    targets = listOf(ChosenTarget.Permanent(bears)),
                    useAlternativeCost = true,
                    alternativeCostType = AlternativeCostType.BESTOW,
                )
            ).error shouldBe null
            game.resolveStack()

            val brawler = game.findPermanent("Sightless Brawler")!!
            game.state.getEntity(brawler)?.get<AttachedToComponent>()?.targetId shouldBe bears
            val projected = game.state.projectedState
            projected.isCreature(brawler) shouldBe false
            projected.getPower(bears) shouldBe 5
            projected.getToughness(bears) shouldBe 4

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldNotBe null
            game.declareAttackers(mapOf("Savannah Lions" to 2)).error shouldBe null
        }

        test("bestowed, the host attacks together with another creature") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withCardOnBattlefield(1, "Savannah Lions", summoningSickness = false)
                .withCardAttachedTo(1, "Sightless Brawler", "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            game.declareAttackers(mapOf("Grizzly Bears" to 2, "Savannah Lions" to 2)).error shouldBe null
        }
    }
}
