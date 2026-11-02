package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Undercity Uprising (GRN #210) — {2}{B}{G} Sorcery.
 *   Creatures you control gain deathtouch until end of turn. Then target creature you control
 *   fights target creature you don't control.
 *
 * Pins the ordering: the deathtouch grant applies before the fight, so a 2-power creature kills a
 * 3-toughness one; every creature you control (not just the fighter) gains deathtouch.
 */
class UndercityUprisingScenarioTest : ScenarioTestBase() {

    init {
        test("creatures you control gain deathtouch before the fight, so the small fighter kills the big one") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Undercity Uprising")
                .withCardOnBattlefield(1, "Grizzly Bears") // 2/2 fighter
                .withCardOnBattlefield(1, "Horned Turtle") // bystander
                .withCardOnBattlefield(2, "Hill Giant") // 3/3
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withLandsOnBattlefield(1, "Forest", 2)
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(2, "Swamp")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val spell = game.findCardsInHand(1, "Undercity Uprising").single()
            val bears = game.findPermanent("Grizzly Bears")!!
            val turtle = game.findPermanent("Horned Turtle")!!
            val giant = game.findPermanent("Hill Giant")!!

            game.execute(
                CastSpell(
                    game.player1Id,
                    spell,
                    listOf(ChosenTarget.Permanent(bears), ChosenTarget.Permanent(giant)),
                )
            ).error shouldBe null
            game.resolveStack()

            withClue("2 deathtouch damage is lethal to the 3/3 Hill Giant") {
                game.isInGraveyard(2, "Hill Giant") shouldBe true
            }
            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            withClue("the non-fighting creature you control also gained deathtouch") {
                game.state.projectedState.hasKeyword(turtle, Keyword.DEATHTOUCH) shouldBe true
            }
        }
    }
}
