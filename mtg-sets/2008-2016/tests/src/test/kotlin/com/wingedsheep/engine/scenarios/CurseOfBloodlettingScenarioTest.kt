package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.dka.cards.CurseOfBloodletting
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Curse of Bloodletting (DKA #85) — {3}{R}{R} Enchantment — Aura Curse.
 *
 * "Enchant player. If a source would deal damage to enchanted player, it deals double that damage
 * to that player instead."
 *
 * The doubling is scoped to the player the Curse is attached to: not its controller, and not the
 * permanents the enchanted player controls.
 */
class CurseOfBloodlettingScenarioTest : ScenarioTestBase() {

    init {
        cardRegistry.register(listOf(CurseOfBloodletting))

        /** The Curse on player 1's battlefield, enchanting player 2; player 1 holds a Bolt. */
        fun cursed(): TestGame {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Curse of Bloodletting")
                .withCardOnBattlefield(2, "Force of Nature")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val curse = game.findPermanent("Curse of Bloodletting")!!
            game.state = game.state.updateEntity(curse) { it.with(AttachedToComponent(game.player2Id)) }
            return game
        }

        test("cast on an opponent, it enchants that player") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Curse of Bloodletting")
                .withLandsOnBattlefield(1, "Mountain", 5)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpellTargetingPlayer(1, "Curse of Bloodletting", 2).error shouldBe null
            if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
            game.resolveStack()

            val curse = game.findPermanent("Curse of Bloodletting")!!
            game.state.getEntity(curse)?.get<AttachedToComponent>()?.targetId shouldBe game.player2Id
        }

        test("damage to the enchanted player is doubled") {
            val game = cursed()
            game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
            if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
            game.resolveStack()
            game.getLifeTotal(2) shouldBe 14
        }

        test("damage to the Curse's controller is not doubled") {
            val game = cursed()
            game.castSpellTargetingPlayer(1, "Lightning Bolt", 1).error shouldBe null
            if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
            game.resolveStack()
            game.getLifeTotal(1) shouldBe 17
        }

        test("damage to a creature the enchanted player controls is not doubled") {
            val game = cursed()
            val force = game.findPermanent("Force of Nature")!!
            game.castSpell(1, "Lightning Bolt", force).error shouldBe null
            if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
            game.resolveStack()
            withClue("only the enchanted player is covered") {
                game.state.getEntity(force)?.get<DamageComponent>()?.amount shouldBe 3
            }
            game.getLifeTotal(2) shouldBe 20
        }
    }
}
