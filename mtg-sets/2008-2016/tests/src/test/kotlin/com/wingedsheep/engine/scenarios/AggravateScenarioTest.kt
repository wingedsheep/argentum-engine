package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.state.components.combat.MustAttackThisTurnComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Aggravate (AVR #125, {3}{R}{R}, Instant).
 *
 *   Aggravate deals 1 damage to each creature target player controls. Each creature dealt damage
 *   this way attacks this turn if able.
 *
 * Only creatures the damage actually reached are forced to attack: Silver Knight's protection from
 * red prevents its damage, so it stays free to sit home. Cast during the opponent's precombat main
 * so the requirement binds in their declare-attackers step.
 */
class AggravateScenarioTest : ScenarioTestBase() {

    private fun board() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Aggravate")
        .withLandsOnBattlefield(1, "Mountain", 5)
        .withCardOnBattlefield(1, "Craw Wurm")
        .withCardOnBattlefield(2, "Grizzly Bears")   // 2/2: survives, must attack
        .withCardOnBattlefield(2, "Silver Knight")   // protection from red: damage prevented
        .withCardInLibrary(1, "Mountain").withCardInLibrary(1, "Mountain")
        .withCardInLibrary(2, "Forest").withCardInLibrary(2, "Forest")
        .withActivePlayer(2)
        .withPriorityPlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("creatures dealt damage must attack; a creature whose damage was prevented need not") {
            val game = board()
            val aggravate = game.findCardsInHand(1, "Aggravate").single()
            val bears = game.findPermanent("Grizzly Bears")!!
            val knight = game.findPermanent("Silver Knight")!!
            val wurm = game.findPermanent("Craw Wurm")!!

            game.execute(
                CastSpell(game.player1Id, aggravate, targets = listOf(ChosenTarget.Player(game.player2Id)))
            ).error shouldBe null
            game.resolveStack()

            withClue("the damaged Bears is marked to attack") {
                game.state.getEntity(bears)!!.has<MustAttackThisTurnComponent>() shouldBe true
            }
            withClue("protection from red prevented the Knight's damage, so no requirement") {
                game.state.getEntity(knight)!!.has<MustAttackThisTurnComponent>() shouldBe false
            }
            withClue("the caster's own creatures are untouched") {
                game.state.getEntity(wurm)!!.has<MustAttackThisTurnComponent>() shouldBe false
            }

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            withClue("declaring no attackers breaks the Bears' requirement") {
                game.execute(DeclareAttackers(game.player2Id, emptyMap())).error shouldNotBe null
            }
            game.declareAttackers(mapOf("Grizzly Bears" to 1)).error shouldBe null
        }
    }
}
