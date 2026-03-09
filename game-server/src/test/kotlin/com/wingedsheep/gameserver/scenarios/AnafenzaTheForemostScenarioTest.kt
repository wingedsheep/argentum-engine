package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.engine.state.components.identity.CardComponent
import io.kotest.matchers.shouldBe

class AnafenzaTheForemostScenarioTest : ScenarioTestBase() {

    init {
        context("Anafenza, the Foremost - replacement effect (exile instead of graveyard)") {

            test("opponent's creature that dies is exiled instead of going to graveyard") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Anafenza, the Foremost")
                    .withCardInHand(1, "Bring Low")
                    .withCardOnBattlefield(1, "Mountain")
                    .withCardOnBattlefield(1, "Mountain")
                    .withCardOnBattlefield(1, "Mountain")
                    .withCardOnBattlefield(1, "Mountain")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Bring Low targeting Hill Giant (deals 3 damage to 3/3 creature)
                val hillGiantId = game.findPermanent("Hill Giant")!!
                game.castSpell(1, "Bring Low", hillGiantId)
                game.resolveStack()

                // Hill Giant should be exiled, not in graveyard
                game.isInGraveyard(2, "Hill Giant") shouldBe false
                val exile = game.state.getExile(game.player2Id)
                exile.any { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Hill Giant"
                } shouldBe true
            }

            test("controller's own creature dying goes to graveyard normally") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Anafenza, the Foremost")
                    .withCardInHand(1, "Bring Low")
                    .withCardOnBattlefield(1, "Mountain")
                    .withCardOnBattlefield(1, "Mountain")
                    .withCardOnBattlefield(1, "Mountain")
                    .withCardOnBattlefield(1, "Mountain")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Bring Low targeting own Hill Giant
                val hillGiantId = game.findPermanent("Hill Giant")!!
                game.castSpell(1, "Bring Low", hillGiantId)
                game.resolveStack()

                // Own creature should go to graveyard normally
                game.isInGraveyard(1, "Hill Giant") shouldBe true
            }
        }

        context("Anafenza, the Foremost - attack trigger") {

            test("puts +1/+1 counter on another tapped creature when attacking") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Anafenza, the Foremost")
                    .withCardOnBattlefield(1, "Hill Giant", tapped = true)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Move to combat and declare Anafenza as attacker
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Anafenza, the Foremost" to 2))

                // Triggered ability fires - select tapped Hill Giant as target
                val hillGiantId = game.findPermanent("Hill Giant")!!
                game.selectTargets(listOf(hillGiantId))
                game.resolveStack()

                // Hill Giant should now have a +1/+1 counter (4/4 instead of 3/3)
                val projected = game.state.projectedState
                projected.getPower(hillGiantId) shouldBe 4
                projected.getToughness(hillGiantId) shouldBe 4
            }
        }
    }
}
