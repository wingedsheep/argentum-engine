package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Rem Karolus, Stalwart Slayer (MID #235) — spells' damage to you and your other permanents is
 * prevented; spells' damage to an opponent or their permanents gets +1. Abilities are untouched.
 */
class RemKarolusStalwartSlayerScenarioTest : ScenarioTestBase() {
    init {
        fun base() = scenario().withPlayers("P1", "P2")
            .withCardOnBattlefield(1, "Rem Karolus, Stalwart Slayer")
            .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

        test("your burn spell at an opponent deals 1 more") {
            val game = base().withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 1).build()
            game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(2) shouldBe 16
        }

        test("a spell at a permanent an opponent controls deals 1 more") {
            // Hill Giant is 3/3: Shock's 2 becomes 3 and kills it.
            val game = base().withCardOnBattlefield(2, "Hill Giant")
                .withCardInHand(1, "Shock")
                .withLandsOnBattlefield(1, "Mountain", 1).build()
            game.castSpell(1, "Shock", game.findPermanent("Hill Giant")!!).error shouldBe null
            game.resolveStack()
            game.isInGraveyard(2, "Hill Giant") shouldBe true
        }

        test("an opponent's spell at you is prevented") {
            val game = base().withCardInHand(2, "Lightning Bolt")
                .withLandsOnBattlefield(2, "Mountain", 1).build()
            game.passPriority().error shouldBe null
            game.castSpellTargetingPlayer(2, "Lightning Bolt", 1).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(1) shouldBe 20
        }

        test("an opponent's spell at another permanent you control is prevented") {
            val game = base().withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(2, "Lightning Bolt")
                .withLandsOnBattlefield(2, "Mountain", 1).build()
            game.passPriority().error shouldBe null
            game.castSpell(2, "Lightning Bolt", game.findPermanent("Grizzly Bears")!!).error shouldBe null
            game.resolveStack()
            game.isInGraveyard(1, "Grizzly Bears") shouldBe false
        }

        test("Rem Karolus itself is not protected") {
            val game = base().withCardInHand(2, "Lightning Bolt")
                .withLandsOnBattlefield(2, "Mountain", 1).build()
            game.passPriority().error shouldBe null
            game.castSpell(2, "Lightning Bolt", game.findPermanent("Rem Karolus, Stalwart Slayer")!!)
                .error shouldBe null
            game.resolveStack()
            game.isInGraveyard(1, "Rem Karolus, Stalwart Slayer") shouldBe true
        }

        test("an ability's damage to an opponent gets no bonus") {
            val game = base().withCardOnBattlefield(1, "Prodigal Sorcerer").build()
            game.execute(
                ActivateAbility(
                    game.player1Id, game.findPermanent("Prodigal Sorcerer")!!,
                    cardRegistry.getCard("Prodigal Sorcerer")!!.activatedAbilities.single().id,
                    targets = listOf(ChosenTarget.Player(game.player2Id))
                )
            ).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(2) shouldBe 19
        }

        test("an opponent's ability at you is not prevented") {
            val game = base().withCardOnBattlefield(2, "Prodigal Sorcerer").build()
            game.passPriority().error shouldBe null
            game.execute(
                ActivateAbility(
                    game.player2Id, game.findPermanent("Prodigal Sorcerer")!!,
                    cardRegistry.getCard("Prodigal Sorcerer")!!.activatedAbilities.single().id,
                    targets = listOf(ChosenTarget.Player(game.player1Id))
                )
            ).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(1) shouldBe 19
        }
    }
}
