package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Join the Maestros (SNC #85) — {4}{B} Sorcery.
 *
 *   Casualty 2. Create a 4/3 black Ogre Warrior creature token.
 *
 * Paying casualty with a power-2 creature copies the spell, so the cast makes two Ogres; a
 * power-1 creature is below the threshold and can't pay it.
 */
class JoinTheMaestrosScenarioTest : ScenarioTestBase() {

    private fun newGame(fodder: String) = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Join the Maestros")
        .withCardOnBattlefield(1, fodder)
        .withLandsOnBattlefield(1, "Swamp", 5)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("casualty with a power-2 creature copies the spell for two Ogre Warriors") {
            val game = newGame("Grizzly Bears")
            val spell = game.findCardsInHand(1, "Join the Maestros").single()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.execute(CastSpell(game.player1Id, spell, casualtyCreature = bears)).error shouldBe null
            withClue("the casualty creature is sacrificed as the spell is cast") {
                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            }
            game.resolveStack()

            val ogres = game.findPermanents("Ogre Warrior Token")
            ogres shouldHaveSize 2
            ogres.forEach { ogre ->
                val projected = game.state.projectedState
                projected.getPower(ogre) shouldBe 4
                projected.getToughness(ogre) shouldBe 3
            }
        }

        test("a power-1 creature can't pay casualty 2") {
            val game = newGame("Savannah Lions")
            val spell = game.findCardsInHand(1, "Join the Maestros").single()
            val lions = game.findPermanent("Savannah Lions")!!

            game.execute(CastSpell(game.player1Id, spell, casualtyCreature = lions)).error shouldNotBe null
        }
    }
}
