package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Illusory Gains (DTK #59, {3}{U}{U}, Enchantment — Aura).
 *
 *   Enchant creature
 *   You control enchanted creature.
 *   Whenever a creature an opponent controls enters, attach this Aura to that creature.
 *
 * Alice's Illusory Gains starts on Bob's Hill Giant, so Alice controls the Giant. Bob then casts a
 * creature. Pinned here: the Aura moves with no decision, Alice gains the newcomer and the Giant goes
 * back to Bob; hexproof doesn't stop the untargeted move; and a newcomer it can't legally enchant (protection from blue) leaves the Aura where it
 * is, with Alice keeping the Giant (2015-02-25 ruling, CR 701.3b).
 */
class IllusoryGainsScenarioTest : ScenarioTestBase() {

    init {
        context("Illusory Gains") {

            fun setup(newcomer: String) = scenario()
                .withPlayers("Alice", "Bob")
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardAttachedTo(1, "Illusory Gains", "Hill Giant")
                .withCardInHand(2, newcomer)
                .withLandsOnBattlefield(2, "Forest", 2)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            test("moves onto the opponent's entering creature, swapping which creature you control") {
                val game = setup("Grizzly Bears")
                val gains = game.findPermanent("Illusory Gains")!!
                val giant = game.findPermanent("Hill Giant")!!

                withClue("Alice starts out controlling the enchanted Hill Giant") {
                    game.state.projectedState.getController(giant) shouldBe game.player1Id
                }

                game.castSpell(2, "Grizzly Bears").error shouldBe null
                game.resolveStack()
                game.checkStateBasedActions()

                val bears = game.findPermanent("Grizzly Bears")!!
                withClue("the move is mandatory and asks nothing") {
                    game.hasPendingDecision() shouldBe false
                }
                withClue("Illusory Gains is now attached to the entering creature") {
                    game.state.getEntity(gains)!!.get<AttachedToComponent>()!!.targetId shouldBe bears
                }
                withClue("Alice gains control of the new creature") {
                    game.state.projectedState.getController(bears) shouldBe game.player1Id
                }
                withClue("the creature it left returns to Bob") {
                    game.state.projectedState.getController(giant) shouldBe game.player2Id
                }
            }

            test("hexproof doesn't stop the move: it doesn't target") {
                val game = setup("Gladecover Scout")
                val gains = game.findPermanent("Illusory Gains")!!

                game.castSpell(2, "Gladecover Scout").error shouldBe null
                game.resolveStack()
                game.checkStateBasedActions()

                val scout = game.findPermanent("Gladecover Scout")!!
                withClue("Illusory Gains moves onto the hexproof newcomer") {
                    game.state.getEntity(gains)!!.get<AttachedToComponent>()!!.targetId shouldBe scout
                }
                withClue("Alice gains control of it") {
                    game.state.projectedState.getController(scout) shouldBe game.player1Id
                }
            }

            test("stays put when it can't legally enchant the entering creature") {
                val game = setup("Karoo Meerkat")
                val gains = game.findPermanent("Illusory Gains")!!
                val giant = game.findPermanent("Hill Giant")!!

                game.castSpell(2, "Karoo Meerkat").error shouldBe null
                game.resolveStack()
                game.checkStateBasedActions()

                val meerkat = game.findPermanent("Karoo Meerkat")!!
                withClue("protection from blue keeps Illusory Gains on the Hill Giant") {
                    game.isOnBattlefield("Illusory Gains") shouldBe true
                    game.state.getEntity(gains)!!.get<AttachedToComponent>()!!.targetId shouldBe giant
                }
                withClue("Alice keeps the Hill Giant; Bob keeps the Meerkat") {
                    game.state.projectedState.getController(giant) shouldBe game.player1Id
                    game.state.projectedState.getController(meerkat) shouldBe game.player2Id
                }
            }
        }
    }
}
