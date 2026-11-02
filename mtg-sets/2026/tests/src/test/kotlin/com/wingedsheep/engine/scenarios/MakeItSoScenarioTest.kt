package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Make It So ({3}{U} Sorcery), choose one —
 *   • Untap target permanent you control. Target player draws two cards.
 *   • Create a 1/1 red Officer creature token and draw two cards.
 */
class MakeItSoScenarioTest : ScenarioTestBase() {

    init {
        test("mode 1 untaps your permanent and the targeted player draws two") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Make It So")
                .withLandsOnBattlefield(1, "Island", 4)
                .withCardOnBattlefield(1, "Grizzly Bears", tapped = true)
                .withCardInLibrary(2, "Forest")
                .withCardInLibrary(2, "Forest")
                .withCardInLibrary(1, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            val spell = game.state.getHand(game.player1Id).first {
                game.state.getEntity(it)?.get<CardComponent>()?.name == "Make It So"
            }
            val targets = listOf(ChosenTarget.Permanent(bears), ChosenTarget.Player(game.player2Id))
            game.execute(
                CastSpell(game.player1Id, spell, targets, chosenModes = listOf(0), modeTargetsOrdered = listOf(targets))
            ).error shouldBe null
            game.resolveStack()

            game.state.getEntity(bears)?.has<TappedComponent>() shouldBe false
            game.handSize(2) shouldBe 2
            game.handSize(1) shouldBe 0
        }

        test("mode 2 creates a 1/1 red Officer token and you draw two") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Make It So")
                .withLandsOnBattlefield(1, "Island", 4)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpellWithMode(1, "Make It So", modeIndex = 1).error shouldBe null
            game.resolveStack()

            game.handSize(1) shouldBe 2
            val projected = game.state.projectedState
            val token = projected.getBattlefieldControlledBy(game.player1Id).single { projected.hasSubtype(it, "Officer") }
            projected.getPower(token) shouldBe 1
            projected.getToughness(token) shouldBe 1
            projected.getColors(token) shouldBe setOf(Color.RED.name)
        }
    }
}
