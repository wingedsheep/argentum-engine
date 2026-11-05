package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Refuse // Cooperate (HOU #156).
 *
 *   Refuse {3}{R}: Refuse deals damage to target spell's controller equal to that spell's mana value.
 *
 * Refuse reads the mana value off the targeted spell and damages that spell's *controller* — not
 * the caster — and it does not counter the spell.
 */
class RefuseCooperateScenarioTest : ScenarioTestBase() {

    init {
        context("Refuse // Cooperate") {

            test("Refuse deals the target spell's mana value to its controller and the spell still resolves") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardInHand(1, "Refuse // Cooperate")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withCardInHand(2, "Hill Giant")
                    .withLandsOnBattlefield(2, "Mountain", 4)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(2, "Hill Giant").error shouldBe null
                game.execute(PassPriority(game.player2Id)).error shouldBe null

                val giantSpell = game.state.stack.first {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Hill Giant"
                }
                val refuse = game.state.getHand(game.player1Id).first {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Refuse // Cooperate"
                }
                game.execute(
                    CastSpell(game.player1Id, refuse, listOf(ChosenTarget.Spell(giantSpell)), faceIndex = 0)
                ).error shouldBe null

                game.resolveStack()

                withClue("Hill Giant's controller takes damage equal to its mana value (4)") {
                    game.getLifeTotal(2) shouldBe 16
                }
                withClue("Refuse's caster takes nothing") { game.getLifeTotal(1) shouldBe 20 }
                withClue("Refuse does not counter the spell") { game.isOnBattlefield("Hill Giant") shouldBe true }
                withClue("Refuse, cast from hand, goes to the graveyard for a later Cooperate") {
                    game.state.getGraveyard(game.player1Id).contains(refuse) shouldBe true
                }
            }
        }
    }
}
