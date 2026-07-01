package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Aetherize (GTC #29) — {3}{U} Instant
 * "Return all attacking creatures to their owner's hand."
 *
 * Exercises [com.wingedsheep.sdk.dsl.Patterns.Group.returnAllToHand] over the
 * [com.wingedsheep.sdk.scripting.filters.unified.GroupFilter.AttackingCreatures] group — a
 * non-targeted mass bounce that only touches creatures currently attacking.
 */
class AetherizeScenarioTest : ScenarioTestBase() {

    init {
        context("Aetherize — bounce every attacking creature") {

            test("all attacking creatures go to their owners' hands; non-attackers stay") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Aetherize")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withCardOnBattlefield(1, "Grizzly Bears", tapped = false, summoningSickness = false)
                    .withCardOnBattlefield(1, "Hill Giant", tapped = false, summoningSickness = false)
                    // A creature that does not attack — must be left alone.
                    .withCardOnBattlefield(1, "Glory Seeker", tapped = false, summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Grizzly Bears and Hill Giant attack Player2; Glory Seeker holds back.
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2, "Hill Giant" to 2)).error shouldBe null

                val spellId = game.state.getHand(game.player1Id).first { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Aetherize"
                }
                val cast = game.execute(CastSpell(game.player1Id, spellId, emptyList()))
                withClue("Cast should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                fun onBattlefield(name: String) = game.state.getBattlefield(game.player1Id).any { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == name
                }
                fun inHand(name: String) = game.state.getHand(game.player1Id).any { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == name
                }

                withClue("both attackers returned to their owner's hand") {
                    onBattlefield("Grizzly Bears") shouldBe false
                    onBattlefield("Hill Giant") shouldBe false
                    inHand("Grizzly Bears") shouldBe true
                    inHand("Hill Giant") shouldBe true
                }
                withClue("the non-attacking creature is untouched") {
                    onBattlefield("Glory Seeker") shouldBe true
                }
            }
        }
    }
}
