package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Murder Investigation (GTC #21, reprinted in J22) — {1}{W} Enchantment — Aura.
 *
 * "Enchant creature you control
 *  When enchanted creature dies, create X 1/1 white Soldier creature tokens, where X is its power."
 *
 * X is the enchanted creature's power as it last existed on the battlefield (2013-01-24 ruling),
 * so a pump that was active when it died counts.
 */
class MurderInvestigationScenarioTest : ScenarioTestBase() {

    init {
        context("Murder Investigation") {

            test("creates Soldier tokens equal to the dead creature's power") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardAttachedTo(1, "Murder Investigation", "Hill Giant")
                    .withCardInHand(2, "Doom Blade")
                    .withLandsOnBattlefield(2, "Swamp", 2)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant")!!
                game.castSpell(2, "Doom Blade", giant).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(1, "Hill Giant") shouldBe true
                val soldiers = game.findPermanents("Soldier Token")
                withClue("Hill Giant had power 3") {
                    soldiers.size shouldBe 3
                }
                val player1 = game.state.turnOrder[0]
                soldiers.forEach { id ->
                    val card = game.state.getEntity(id)!!.get<CardComponent>()!!
                    card.baseStats?.basePower shouldBe 1
                    card.baseStats?.baseToughness shouldBe 1
                    card.colors shouldBe setOf(Color.WHITE)
                    game.state.getEntity(id)!!.get<ControllerComponent>()!!.playerId shouldBe player1
                }
            }

            test("uses last-known power, including a pump active when it died") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardAttachedTo(1, "Murder Investigation", "Hill Giant")
                    .withCardInHand(1, "Giant Growth")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardInHand(1, "Doom Blade")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant")!!
                game.castSpell(1, "Giant Growth", giant).error shouldBe null
                game.resolveStack()
                game.castSpell(1, "Doom Blade", giant).error shouldBe null
                game.resolveStack()

                withClue("3 base power + 3 from Giant Growth") {
                    game.findPermanents("Soldier Token").size shouldBe 6
                }
            }

            test("can only enchant a creature you control") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardInHand(1, "Murder Investigation")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val opposingGiant = game.findPermanent("Hill Giant")!!
                game.castSpell(1, "Murder Investigation", opposingGiant).error shouldNotBe null

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Murder Investigation", bears).error shouldBe null
                game.resolveStack()
                game.isOnBattlefield("Murder Investigation") shouldBe true
            }
        }
    }
}
