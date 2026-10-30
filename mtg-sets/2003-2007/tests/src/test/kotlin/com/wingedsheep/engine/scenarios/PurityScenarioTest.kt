package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Purity (LRW #37) — noncombat damage to you is prevented, and you gain that much life as part of
 * the same prevention.
 */
class PurityScenarioTest : ScenarioTestBase() {
    init {
        fun base() = scenario().withPlayers("P1", "P2")
            .withCardOnBattlefield(1, "Purity")
            .withActivePlayer(2).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

        test("a burn spell's damage is prevented and becomes life") {
            val game = base().withCardInHand(2, "Lightning Bolt")
                .withLandsOnBattlefield(2, "Mountain", 1).build()
            game.castSpellTargetingPlayer(2, "Lightning Bolt", 1).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(1) shouldBe 23
        }

        test("an ability's noncombat damage is prevented too") {
            val game = base().withCardOnBattlefield(2, "Prodigal Sorcerer").build()
            game.execute(ActivateAbility(
                game.player2Id, game.findPermanent("Prodigal Sorcerer")!!,
                cardRegistry.getCard("Prodigal Sorcerer")!!.activatedAbilities.single().id,
                targets = listOf(ChosenTarget.Player(game.player1Id))
            )).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(1) shouldBe 21
        }

        test("damage that can't be prevented is dealt and gains no life") {
            // CR 615.12: the prevention still applies, prevents 0, and so gains 0 life.
            val game = base().withCardInHand(2, "Banefire")
                .withLandsOnBattlefield(2, "Mountain", 6).build()
            val banefire = game.findCardsInHand(2, "Banefire").single()
            game.execute(CastSpell(game.player2Id, banefire, listOf(ChosenTarget.Player(game.player1Id)), 5))
                .error shouldBe null
            game.resolveStack()
            game.getLifeTotal(1) shouldBe 15
        }

        test("combat damage is dealt normally") {
            val game = base().withCardOnBattlefield(2, "Grizzly Bears").build()
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 1)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareNoBlockers().error shouldBe null
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
            game.getLifeTotal(1) shouldBe 18
        }

        test("damage to a creature you control is not prevented") {
            val game = base().withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(2, "Lightning Bolt")
                .withLandsOnBattlefield(2, "Mountain", 1).build()
            game.castSpell(2, "Lightning Bolt", game.findPermanent("Grizzly Bears")!!).error shouldBe null
            game.resolveStack()
            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            game.getLifeTotal(1) shouldBe 20
        }

        test("destroyed Purity shuffles into its owner's library") {
            val game = base().withCardInHand(2, "Terminate")
                .withLandsOnBattlefield(2, "Swamp", 1)
                .withLandsOnBattlefield(2, "Mountain", 1).build()
            game.castSpell(2, "Terminate", game.findPermanent("Purity")!!).error shouldBe null
            game.resolveStack()
            game.findCardsInLibrary(1, "Purity").size shouldBe 1
        }
    }
}
