package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Unleash the Inferno (SNC #229).
 *
 * "Unleash the Inferno deals 7 damage to target creature or planeswalker. When it deals excess
 * damage this way, destroy target artifact or enchantment an opponent controls with mana value less
 * than or equal to that amount of excess damage."
 *
 * The reflexive trigger (CR 603.12) is only created when excess damage (CR 120.4a) is dealt, and
 * its target is chosen as it goes on the stack, capped at the excess.
 */
class UnleashTheInfernoScenarioTest : ScenarioTestBase() {

    private fun game(victim: String, vararg opponentArtifacts: String) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Unleash the Inferno")
        .withLandsOnBattlefield(1, "Swamp", 1)
        .withLandsOnBattlefield(1, "Mountain", 2)
        .withLandsOnBattlefield(1, "Forest", 1)
        .withCardOnBattlefield(1, "Ornithopter")
        .withCardOnBattlefield(2, victim)
        .apply { opponentArtifacts.forEach { withCardOnBattlefield(2, it) } }
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.ornithopters() =
        state.getBattlefield().count { state.getEntity(it)?.get<CardComponent>()?.name == "Ornithopter" }

    init {
        context("Unleash the Inferno") {

            test("4 excess damage lets it destroy an opponent's artifact with mana value 4 or less") {
                val game = game("Hill Giant", "Mind Stone", "Juggernaut", "Steel Hellkite")
                val giant = game.findPermanent("Hill Giant").shouldNotBeNull()
                game.castSpell(1, "Unleash the Inferno", giant).error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Hill Giant") shouldBe false
                val decision = game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
                decision.legalTargets.getValue(0) shouldContainExactlyInAnyOrder listOf(
                    game.findPermanent("Mind Stone").shouldNotBeNull(),
                    game.findPermanent("Juggernaut").shouldNotBeNull(),
                )

                game.selectTargets(listOf(game.findPermanent("Juggernaut")!!)).error shouldBe null
                game.resolveStack()
                game.isOnBattlefield("Juggernaut") shouldBe false
                game.isOnBattlefield("Mind Stone") shouldBe true
                game.isOnBattlefield("Steel Hellkite") shouldBe true
                game.isOnBattlefield("Ornithopter") shouldBe true
            }

            test("1 excess damage caps the target at mana value 1") {
                val game = game("Colossal Dreadmaw", "Ornithopter", "Mind Stone")
                val dreadmaw = game.findPermanent("Colossal Dreadmaw").shouldNotBeNull()
                game.castSpell(1, "Unleash the Inferno", dreadmaw).error shouldBe null
                game.resolveStack()
                // Only the opponent's Ornithopter (mana value 0) is legal; ours and Mind Stone aren't.
                val decision = game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
                val theirs = decision.legalTargets.getValue(0)
                theirs.size shouldBe 1
                game.state.getEntity(theirs.single())?.get<CardComponent>()?.name shouldBe "Ornithopter"
                game.selectTargets(theirs).error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Colossal Dreadmaw") shouldBe false
                game.isOnBattlefield("Mind Stone") shouldBe true
                // The opponent's Ornithopter is destroyed; ours (not a legal target) survives.
                game.ornithopters() shouldBe 1
            }

            test("no excess damage, no trigger: indestructible 11/11 is dealt 7 and nothing is destroyed") {
                val game = game("Darksteel Colossus", "Ornithopter")
                val colossus = game.findPermanent("Darksteel Colossus").shouldNotBeNull()
                game.castSpell(1, "Unleash the Inferno", colossus).error shouldBe null
                game.resolveStack()

                game.getPendingDecision() shouldBe null
                game.state.stack shouldBe emptyList()
                game.isOnBattlefield("Darksteel Colossus") shouldBe true
                game.ornithopters() shouldBe 2
            }
        }
    }
}
