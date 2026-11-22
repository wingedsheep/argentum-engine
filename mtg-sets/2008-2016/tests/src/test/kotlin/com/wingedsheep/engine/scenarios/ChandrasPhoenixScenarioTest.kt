package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Chandra's Phoenix (M12 #126) — "Whenever an opponent is dealt damage by a red instant or sorcery
 * spell you control or by a red planeswalker you control, return this card from your graveyard to
 * your hand."
 *
 * A damage observer that functions only from the graveyard, with a two-branch source filter: these
 * pin that both branches (red spell, red planeswalker) return it, and that damage to a creature, a
 * red spell an opponent controls, or a red creature does not.
 */
class ChandrasPhoenixScenarioTest : ScenarioTestBase() {

    private fun setup(p2Bolt: Boolean = false): TestGame {
        var builder = scenario()
            .withPlayers("Player", "Opponent")
            .withCardInGraveyard(1, "Chandra's Phoenix")
            .withCardInHand(1, "Lightning Bolt")
            .withLandsOnBattlefield(1, "Mountain", 1)
            .withCardOnBattlefield(2, "Grizzly Bears")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        if (p2Bolt) {
            builder = builder.withCardInHand(2, "Lightning Bolt").withLandsOnBattlefield(2, "Mountain", 1)
        }
        repeat(5) { builder = builder.withCardInLibrary(1, "Mountain").withCardInLibrary(2, "Mountain") }
        return builder.build()
    }

    private fun TestGame.phoenixReturned(): Boolean =
        isInHand(1, "Chandra's Phoenix") && !isInGraveyard(1, "Chandra's Phoenix")

    init {
        test("a red sorcery or instant you control damaging an opponent returns it to hand") {
            val game = setup()
            game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 17
            game.phoenixReturned() shouldBe true
        }

        test("a red spell you control damaging only a creature does not trigger") {
            val game = setup()
            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Lightning Bolt", bears).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Chandra's Phoenix") shouldBe true
        }

        test("a red spell an opponent controls damaging that opponent does not trigger") {
            val game = setup(p2Bolt = true)
            game.passPriority()
            game.castSpellTargetingPlayer(2, "Lightning Bolt", 2).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 17
            game.isInGraveyard(1, "Chandra's Phoenix") shouldBe true
        }

        test("a red planeswalker you control damaging an opponent returns it to hand") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInGraveyard(1, "Chandra's Phoenix")
                .withCardOnBattlefield(1, "Chandra Nalaar")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val chandra = game.findPermanent("Chandra Nalaar")!!
            val plusOne = cardRegistry.getCard("Chandra Nalaar")!!.script.activatedAbilities[0]
            game.execute(
                ActivateAbility(game.player1Id, chandra, plusOne.id, targets = listOf(ChosenTarget.Player(game.player2Id)))
            ).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 19
            game.phoenixReturned() shouldBe true
        }

        test("a red creature dealing combat damage to an opponent does not trigger") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInGraveyard(1, "Chandra's Phoenix")
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            game.declareAttackers(mapOf("Hill Giant" to 2)).error shouldBe null
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

            game.getLifeTotal(2) shouldBe 17
            game.isInGraveyard(1, "Chandra's Phoenix") shouldBe true
        }
    }
}
