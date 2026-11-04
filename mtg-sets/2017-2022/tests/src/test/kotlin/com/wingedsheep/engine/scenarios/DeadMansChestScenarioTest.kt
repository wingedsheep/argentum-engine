package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Dead Man's Chest (RIX #66) — {1}{B} Enchantment — Aura.
 *
 * "Enchant creature an opponent controls. When enchanted creature dies, exile cards equal to its
 *  power from the top of its owner's library. You may cast spells from among those cards for as
 *  long as they remain exiled, and mana of any type can be spent to cast them."
 *
 * Proves X is the creature's last-known power (a pumped Hill Giant exiles six, not three), the
 * cards come off the *owner's* library, and an exiled green creature can be cast with Swamps.
 */
class DeadMansChestScenarioTest : ScenarioTestBase() {

    init {
        context("Dead Man's Chest") {

            test("exiles last-known-power cards from the owner's library, castable with any type of mana") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardAttachedTo(1, "Dead Man's Chest", "Hill Giant")
                    .withCardInHand(1, "Giant Growth")
                    .withCardInHand(1, "Doom Blade")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withCardInLibrary(1, "Plains")
                    // Opponent's library, top first.
                    .withCardInLibrary(2, "Grizzly Bears")
                    .withCardInLibrary(2, "Forest")
                    .withCardInLibrary(2, "Raging Goblin")
                    .withCardInLibrary(2, "Island")
                    .withCardInLibrary(2, "Island")
                    .withCardInLibrary(2, "Island")
                    .withCardInLibrary(2, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant")!!

                game.castSpell(1, "Giant Growth", giant).error shouldBe null
                game.resolveStack()
                game.state.projectedState.getPower(giant) shouldBe 6

                game.castSpell(1, "Doom Blade", giant).error shouldBe null
                game.resolveStack()

                withClue("Hill Giant died and its last-known power of 6 exiled six cards from its owner's library") {
                    game.isInGraveyard(2, "Hill Giant") shouldBe true
                    game.state.getExile(game.player2Id).size shouldBe 6
                    game.librarySize(2) shouldBe 2
                    game.librarySize(1) shouldBe 1
                }

                val exiledBears = game.state.getExile(game.player2Id)
                    .first { game.state.getEntity(it)?.get<CardComponent>()?.name == "Grizzly Bears" }
                val exiledForest = game.state.getExile(game.player2Id)
                    .first { game.state.getEntity(it)?.get<CardComponent>()?.name == "Forest" }

                withClue("the cast permission lasts as long as the cards stay exiled and is cast-only") {
                    val permission = game.state.mayPlayPermissions.first { exiledBears in it.cardIds }
                    permission.controllerId shouldBe game.player1Id
                    permission.permanent shouldBe true
                    permission.withAnyManaType shouldBe true
                    permission.nonLandOnly shouldBe true
                    (exiledForest in permission.cardIds) shouldBe true
                }

                // Only two untapped Swamps remain: cast the {1}{G} Bears with black mana.
                val result = game.execute(
                    CastSpell(game.player1Id, exiledBears, paymentStrategy = PaymentStrategy.AutoPay)
                )
                withClue("the exiled green creature is castable spending Swamp mana") {
                    result.error shouldBe null
                }
                var guard = 0
                while ((game.getPendingDecision() != null || game.state.stack.isNotEmpty()) && guard++ < 40) {
                    when (game.getPendingDecision()) {
                        is SelectManaSourcesDecision -> game.submitManaSourcesAutoPay()
                        null -> game.resolveStack()
                        else -> error("Unexpected decision casting from exile: ${game.getPendingDecision()}")
                    }
                }

                val bears = game.findPermanent("Grizzly Bears")
                bears.shouldNotBeNull()
                game.state.projectedState.getController(bears) shouldBe game.player1Id
            }
        }
    }
}
