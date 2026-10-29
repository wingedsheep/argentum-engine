package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Feline Sovereign (M21 #180, {2}{G}, Creature — Cat 2/3).
 *
 *   Other Cats you control get +1/+1 and have protection from Dogs.
 *   Whenever one or more Cats you control deal combat damage to a player, destroy up to one target
 *   artifact or enchantment that player controls.
 *
 * The lord excludes the Sovereign itself and only reaches Cats you control. The trigger is the
 * batched combat-damage trigger scoped to *your* Cats, with "that player" bound to the damaged
 * player — so the target prompt offers only that player's artifacts/enchantments, and an
 * opponent's Cats connecting with you never fire it.
 */
class FelineSovereignScenarioTest : ScenarioTestBase() {

    private fun TestGame.permanentOf(playerId: EntityId, name: String): EntityId =
        state.getZone(ZoneKey(playerId, Zone.BATTLEFIELD)).first {
            state.getEntity(it)?.get<CardComponent>()?.name == name
        }

    /** Push an already-declared attack through to combat damage, settling any damage-assignment prompt. */
    private fun TestGame.throughCombatDamage() {
        passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
        resolveStack()
        if (hasPendingDecision() && getPendingDecision() !is ChooseTargetsDecision) {
            submitDefaultCombatDamage()
            resolveStack()
        }
    }

    init {
        context("Feline Sovereign") {

            test("other Cats you control get +1/+1; the Sovereign and an opponent's Cats don't") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Feline Sovereign")
                    .withCardOnBattlefield(1, "Savannah Lions")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Savannah Lions")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val projected = game.state.projectedState
                val sovereign = game.findPermanent("Feline Sovereign")!!
                val myLions = game.permanentOf(game.player1Id, "Savannah Lions")
                val theirLions = game.permanentOf(game.player2Id, "Savannah Lions")
                val bears = game.findPermanent("Grizzly Bears")!!

                withClue("your other Cat is pumped to 2/2 (the test registry's Savannah Lions is a 1/1)") {
                    projected.getPower(myLions) shouldBe 2
                    projected.getToughness(myLions) shouldBe 2
                }
                withClue("the Sovereign doesn't pump itself") {
                    projected.getPower(sovereign) shouldBe 2
                    projected.getToughness(sovereign) shouldBe 3
                }
                withClue("an opponent's Cat is not pumped") {
                    projected.getPower(theirLions) shouldBe 1
                    projected.getToughness(theirLions) shouldBe 1
                }
                withClue("a non-Cat is not pumped") {
                    projected.getPower(bears) shouldBe 2
                    projected.getToughness(bears) shouldBe 2
                }
            }

            test("your Cat connecting lets you destroy an artifact or enchantment the damaged player controls") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Feline Sovereign")
                    .withCardOnBattlefield(1, "Savannah Lions", summoningSickness = false)
                    .withCardOnBattlefield(1, "Ornithopter")
                    .withCardOnBattlefield(2, "Ornithopter")
                    .withCardOnBattlefield(2, "Glorious Anthem")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val myThopter = game.permanentOf(game.player1Id, "Ornithopter")
                val theirThopter = game.permanentOf(game.player2Id, "Ornithopter")
                val anthem = game.permanentOf(game.player2Id, "Glorious Anthem")

                game.declareAttackers(mapOf("Savannah Lions" to 2)).error shouldBe null
                game.throughCombatDamage()

                withClue("the pumped Lions dealt 2") { game.getLifeTotal(2) shouldBe 18 }

                val decision = game.getPendingDecision()
                withClue("the trigger asks for an optional target; got $decision") {
                    decision.shouldBeInstanceOf<ChooseTargetsDecision>()
                    decision.targetRequirements.single().minTargets shouldBe 0
                }
                val legal = (decision as ChooseTargetsDecision).legalTargets[0].orEmpty()
                withClue("the damaged player's artifact and enchantment are legal targets") {
                    legal shouldContain theirThopter
                    legal shouldContain anthem
                }
                withClue("your own artifact is not — it must be one that player controls") {
                    legal shouldNotContain myThopter
                }

                game.selectTargets(listOf(theirThopter)).error shouldBe null
                game.resolveStack()

                withClue("Bob's Ornithopter is destroyed") { game.isInGraveyard(2, "Ornithopter") shouldBe true }
                withClue("Alice's Ornithopter survives") { game.isInGraveyard(1, "Ornithopter") shouldBe false }
                withClue("the untargeted Glorious Anthem survives") { game.isOnBattlefield("Glorious Anthem") shouldBe true }
            }

            test("an opponent's Cats dealing combat damage to you don't trigger your Sovereign") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Feline Sovereign", tapped = true)
                    .withCardOnBattlefield(1, "Ornithopter", tapped = true)
                    .withCardOnBattlefield(2, "Savannah Lions", summoningSickness = false)
                    .withCardOnBattlefield(2, "Ornithopter")
                    .withActivePlayer(2)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Savannah Lions" to 1)).error shouldBe null
                game.throughCombatDamage()

                withClue("Bob's unpumped Lions dealt 1 to Alice") { game.getLifeTotal(1) shouldBe 19 }
                withClue("no Feline Sovereign target prompt") {
                    (game.getPendingDecision() is ChooseTargetsDecision) shouldBe false
                }
                withClue("nothing went on the stack") { game.state.stack.isEmpty() shouldBe true }
                withClue("no artifact was destroyed") {
                    game.isInGraveyard(1, "Ornithopter") shouldBe false
                    game.isInGraveyard(2, "Ornithopter") shouldBe false
                }
            }
        }
    }
}
