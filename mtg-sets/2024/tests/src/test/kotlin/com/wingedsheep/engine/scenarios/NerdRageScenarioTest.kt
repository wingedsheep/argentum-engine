package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Nerd Rage (PIP #34) — {2}{U} Enchantment — Aura.
 *
 *   Enchant creature
 *   When this Aura enters, draw two cards.
 *   Enchanted creature has "You have no maximum hand size" and "Whenever this creature attacks,
 *   if you have ten or more cards in hand, it gets +10/+10 until end of turn."
 */
class NerdRageScenarioTest : ScenarioTestBase() {

    init {
        test("entering draws two cards") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Nerd Rage")
                .withLandsOnBattlefield(1, "Island", 3)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Nerd Rage", bears).error shouldBe null
            game.resolveStack()

            (game.findPermanent("Nerd Rage") != null) shouldBe true
            game.handSize(1) shouldBe 2
        }

        test("the enchanted creature's controller keeps a hand over seven through cleanup") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardAttachedTo(1, "Nerd Rage", "Grizzly Bears")
                .withCardsInHand(1, "Forest", 9)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)

            game.hasPendingDecision() shouldBe false
            game.state.activePlayerId shouldBe game.player2Id
            game.handSize(1) shouldBe 9
        }

        test("on an opponent's creature, the opponent has no maximum hand size") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardAttachedTo(1, "Nerd Rage", "Grizzly Bears")
                .withCardsInHand(2, "Forest", 9)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(2)
                .withPriorityPlayer(2)
                .inPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)

            game.hasPendingDecision() shouldBe false
            game.handSize(2) shouldBe 9
        }

        test("attacking with ten or more cards in hand gives it +10/+10") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardAttachedTo(1, "Nerd Rage", "Grizzly Bears")
                .withCardsInHand(1, "Forest", 10)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
            game.resolveStack()

            game.state.projectedState.getPower(bears) shouldBe 12
            game.state.projectedState.getToughness(bears) shouldBe 12
        }

        test("attacking with nine cards in hand does nothing") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardAttachedTo(1, "Nerd Rage", "Grizzly Bears")
                .withCardsInHand(1, "Forest", 9)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
            game.resolveStack()

            game.state.projectedState.getPower(bears) shouldBe 2
        }
    }
}
