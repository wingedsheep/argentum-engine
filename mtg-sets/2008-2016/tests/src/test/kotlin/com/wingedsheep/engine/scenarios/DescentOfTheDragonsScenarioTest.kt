package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Descent of the Dragons (DTK #133) — {4}{R}{R} Sorcery.
 *
 *   Destroy any number of target creatures. For each creature destroyed this way, its controller
 *   creates a 4/4 red Dragon creature token with flying.
 *
 * Pins that each Dragon goes to the destroyed creature's controller (not the caster), and that a
 * target that isn't destroyed — indestructible here — makes no Dragon (2015-02-25 ruling).
 */
class DescentOfTheDragonsScenarioTest : ScenarioTestBase() {

    init {
        test("each destroyed creature's controller gets a Dragon; an indestructible target gives none") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Descent of the Dragons")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardOnBattlefield(2, "Glory Seeker")
                .withCardOnBattlefield(2, "Darksteel Myr")
                .withLandsOnBattlefield(1, "Mountain", 6)
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val myr = game.findPermanent("Darksteel Myr")!!
            val targets = listOf("Grizzly Bears", "Hill Giant", "Glory Seeker", "Darksteel Myr")
                .map { ChosenTarget.Permanent(game.findPermanent(it)!!) }
            val spell = game.state.getHand(game.player1Id).first {
                game.state.getEntity(it)?.get<CardComponent>()?.name == "Descent of the Dragons"
            }

            game.execute(CastSpell(game.player1Id, spell, targets)).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            game.isInGraveyard(2, "Hill Giant") shouldBe true
            game.isInGraveyard(2, "Glory Seeker") shouldBe true
            game.findPermanent("Darksteel Myr") shouldBe myr

            val dragons = game.findPermanents("Dragon Token")
            dragons.size shouldBe 3
            dragons.count { game.state.getEntity(it)?.get<ControllerComponent>()?.playerId == game.player1Id } shouldBe 1
            dragons.count { game.state.getEntity(it)?.get<ControllerComponent>()?.playerId == game.player2Id } shouldBe 2
            dragons.forEach { dragon ->
                game.state.projectedState.getPower(dragon) shouldBe 4
                game.state.projectedState.getToughness(dragon) shouldBe 4
                game.state.projectedState.hasKeyword(dragon, Keyword.FLYING) shouldBe true
            }
        }

        test("no targets: the spell resolves and creates nothing") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Descent of the Dragons")
                .withCardOnBattlefield(2, "Hill Giant")
                .withLandsOnBattlefield(1, "Mountain", 6)
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val spell = game.state.getHand(game.player1Id).first {
                game.state.getEntity(it)?.get<CardComponent>()?.name == "Descent of the Dragons"
            }

            game.execute(CastSpell(game.player1Id, spell, emptyList())).error shouldBe null
            game.resolveStack()

            game.findPermanents("Dragon Token").size shouldBe 0
            game.isInGraveyard(1, "Descent of the Dragons") shouldBe true
        }
    }
}
