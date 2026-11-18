package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Crystalline Nautilus: "When this permanent becomes the target of a spell or ability, sacrifice it.
 * Enchanted creature gets +4/+4 and has 'When this creature becomes the target of a spell or
 * ability, sacrifice it.'" Bestowed, the two triggers sacrifice different objects: targeting the
 * host sacrifices the host (the Nautilus falls off and stays as a creature), targeting the Aura
 * sacrifices only the Aura.
 */
class CrystallineNautilusScenarioTest : ScenarioTestBase() {

    private fun bestowedOnBears(extraCard: String): TestGame {
        val game = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
            .withCardInHand(1, "Crystalline Nautilus")
            .withCardInHand(1, extraCard)
            .withLandsOnBattlefield(1, "Island", 7)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        val bears = game.findPermanent("Grizzly Bears")!!
        game.execute(
            CastSpell(
                playerId = game.player1Id,
                cardId = game.findCardsInHand(1, "Crystalline Nautilus").single(),
                targets = listOf(ChosenTarget.Permanent(bears)),
                useAlternativeCost = true,
                alternativeCostType = AlternativeCostType.BESTOW,
            )
        ).error shouldBe null
        game.resolveStack()

        val nautilus = game.findPermanent("Crystalline Nautilus")!!
        game.state.getEntity(nautilus)?.get<AttachedToComponent>()?.targetId shouldBe bears
        game.state.projectedState.getPower(bears) shouldBe 6
        game.state.projectedState.getToughness(bears) shouldBe 6
        return game
    }

    init {
        test("targeting the enchanted creature sacrifices the creature before the spell resolves; the Nautilus stays as a 4/4") {
            val game = bestowedOnBears("Unsummon")
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Unsummon", bears).error shouldBe null
            game.resolveStack()

            game.findPermanent("Grizzly Bears") shouldBe null
            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            game.isInHand(1, "Grizzly Bears") shouldBe false
            val nautilus = game.findPermanent("Crystalline Nautilus")
            nautilus shouldNotBe null
            game.state.projectedState.isCreature(nautilus!!) shouldBe true
            game.state.projectedState.getPower(nautilus) shouldBe 4
        }

        test("targeting the bestowed Aura sacrifices only the Aura, before the spell resolves") {
            val game = bestowedOnBears("Boomerang")
            val nautilus = game.findPermanent("Crystalline Nautilus")!!

            game.castSpell(1, "Boomerang", nautilus).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Crystalline Nautilus") shouldBe true
            game.isInHand(1, "Crystalline Nautilus") shouldBe false
            val bears = game.findPermanent("Grizzly Bears")
            bears shouldNotBe null
            game.state.projectedState.getPower(bears!!) shouldBe 2
        }
    }
}
