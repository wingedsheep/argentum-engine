package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Heliod, the Radiant Dawn // Heliod, the Warped Eclipse (MOM #17).
 *
 *   Front — "When Heliod enters, return target enchantment card that isn't a God from your
 *            graveyard to your hand." / "{3}{U/P}: Transform Heliod. Activate only as a sorcery."
 *   Back  — "You may cast spells as though they had flash." / "Spells you cast cost {1} less to
 *            cast for each card your opponents have drawn this turn."
 */
class HeliodTheRadiantDawnScenarioTest : ScenarioTestBase() {

    private fun transform(game: TestGame): EntityId {
        val heliod = game.findPermanent("Heliod, the Radiant Dawn")!!
        val abilityId = cardRegistry.getCard("Heliod, the Radiant Dawn")!!.activatedAbilities.first().id
        game.execute(
            ActivateAbility(playerId = game.player1Id, sourceId = heliod, abilityId = abilityId)
        ).error shouldBe null
        if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
        game.resolveStack()
        game.state.getEntity(heliod)!!.get<CardComponent>()!!.name shouldBe "Heliod, the Warped Eclipse"
        return heliod
    }

    init {
        context("Heliod, the Radiant Dawn") {

            test("ETB returns a non-God enchantment card; a God enchantment card is not a legal target") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Heliod, the Radiant Dawn")
                    .withCardInGraveyard(1, "Pacifism")
                    .withCardInGraveyard(1, "Heliod, the Radiant Dawn")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val pacifism = game.findCardsInGraveyard(1, "Pacifism").single()
                val deadGod = game.findCardsInGraveyard(1, "Heliod, the Radiant Dawn").single()
                val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()

                game.castSpell(1, "Heliod, the Radiant Dawn").error shouldBe null
                game.resolveStack()

                val decision = game.getPendingDecision() as ChooseTargetsDecision
                val legal = decision.legalTargets[0]!!
                legal shouldContain pacifism
                withClue("a God is excluded") { legal shouldNotContain deadGod }
                withClue("a non-enchantment is excluded") { legal shouldNotContain bears }

                game.selectTargets(listOf(pacifism)).error shouldBe null
                game.resolveStack()

                game.isInHand(1, "Pacifism") shouldBe true
                game.isInGraveyard(1, "Heliod, the Radiant Dawn") shouldBe true
            }

            test("transforms by paying 2 life for {U/P} into a 4/6 Heliod, the Warped Eclipse") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Heliod, the Radiant Dawn")
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val heliod = transform(game)
                game.getLifeTotal(1) shouldBe 18
                game.state.projectedState.getPower(heliod) shouldBe 4
                game.state.projectedState.getToughness(heliod) shouldBe 6
            }

            test("Warped Eclipse discounts spells by {1} per card opponents drew this turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Heliod, the Radiant Dawn")
                    .withLandsOnBattlefield(1, "Plains", 7)
                    .withCardInHand(1, "Serra Angel")
                    .withCardsDrawnThisTurn(2, 2)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                transform(game) // taps 3 Plains; 4 remain untapped

                // Serra Angel {3}{W}{W} costs {1}{W}{W} with two opponent draws — 3 Plains suffice.
                game.castSpell(1, "Serra Angel").error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()
                game.isOnBattlefield("Serra Angel") shouldBe true
                withClue("only three of the four remaining Plains were tapped") {
                    game.findPermanents("Plains").count { !game.state.getEntity(it)!!.has<com.wingedsheep.engine.state.components.battlefield.TappedComponent>() } shouldBe 1
                }
            }

            test("no discount when only you have drawn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Heliod, the Radiant Dawn")
                    .withLandsOnBattlefield(1, "Plains", 6)
                    .withCardInHand(1, "Serra Angel")
                    .withCardsDrawnThisTurn(1, 3)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                transform(game) // 3 Plains remain

                game.castSpell(1, "Serra Angel").error shouldNotBe null
            }

            test("Warped Eclipse lets you cast a sorcery-speed spell while the stack is non-empty") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Heliod, the Radiant Dawn")
                    .withLandsOnBattlefield(1, "Plains", 5)
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardInHand(1, "Savannah Lions")
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                transform(game)

                game.castSpell(1, "Savannah Lions").error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.state.stack.isEmpty() shouldBe false
                // With Savannah Lions still on the stack, a creature spell needs flash.
                game.castSpell(1, "Grizzly Bears").error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()
                game.isOnBattlefield("Grizzly Bears") shouldBe true
                game.isOnBattlefield("Savannah Lions") shouldBe true
            }

            test("front face grants no flash") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Heliod, the Radiant Dawn")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardInHand(1, "Savannah Lions")
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Savannah Lions").error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.castSpell(1, "Grizzly Bears").error shouldNotBe null
            }
        }
    }
}
