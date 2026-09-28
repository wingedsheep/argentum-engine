package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Quintorius, Loremaster (MOM #250).
 *
 *   "At the beginning of your end step, exile target noncreature, nonland card from your graveyard.
 *    Create a 3/2 red and white Spirit creature token.
 *    {1}{R}{W}, {T}, Sacrifice a Spirit: Choose target card exiled with Quintorius. You may cast that
 *    card this turn without paying its mana cost. If that spell would be put into a graveyard, put it
 *    on the bottom of its owner's library instead."
 *
 * Pins the linked exile (only cards Quintorius exiled are targetable), the lingering free cast, and
 * the bottom-of-library rider — `GrantFreeCastTargetFromExile(insteadOfGraveyard = BOTTOM_OF_LIBRARY)`.
 */
class QuintoriusLoremasterScenarioTest : ScenarioTestBase() {

    private val castAbility get() = cardRegistry.getCard("Quintorius, Loremaster")!!.activatedAbilities[0].id

    private fun TestGame.spirits(): List<EntityId> = state.getBattlefield().filter {
        state.getEntity(it)?.get<CardComponent>()?.typeLine?.subtypes?.any { s -> s.value == "Spirit" } == true
    }

    /** Your end step with Lightning Bolt in the graveyard; the trigger exiles it and makes a Spirit. */
    private fun endStepWithBoltExiled(
        extra: ScenarioBuilder.() -> ScenarioBuilder = { this }
    ): Pair<TestGame, EntityId> {
        val game = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Quintorius, Loremaster", summoningSickness = false)
            .withLandsOnBattlefield(1, "Mountain", 2)
            .withLandsOnBattlefield(1, "Plains", 1)
            .withCardInGraveyard(1, "Lightning Bolt")
            .withCardInGraveyard(1, "Grizzly Bears")
            // A non-empty library, so "bottom" is distinguishable from "top".
            .withCardInLibrary(1, "Mountain")
            .withCardInLibrary(1, "Plains")
            .withActivePlayer(1)
            .inPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
            .extra()
            .build()
        val bolt = game.findCardsInGraveyard(1, "Lightning Bolt").single()
        game.passUntilPhase(Phase.ENDING, Step.END)
        withClue("the end-step trigger asks for its graveyard target") {
            (game.getPendingDecision() is ChooseTargetsDecision) shouldBe true
        }
        game.selectTargets(listOf(bolt)).error shouldBe null
        game.resolveStack()
        return game to bolt
    }

    private fun TestGame.activate(target: EntityId) = execute(
        ActivateAbility(
            playerId = player1Id,
            sourceId = findPermanent("Quintorius, Loremaster")!!,
            abilityId = castAbility,
            targets = listOf(ChosenTarget.Card(target, player1Id, Zone.EXILE)),
            costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(spirits().first()))
        )
    ).also { if (getPendingDecision() is SelectManaSourcesDecision) submitManaSourcesAutoPay() }

    init {
        context("Quintorius, Loremaster") {

            test("end step: exiles the noncreature, nonland card and creates a 3/2 Spirit") {
                val (game, bolt) = endStepWithBoltExiled()

                withClue("the Bolt is exiled; the creature card was never a legal target") {
                    (bolt in game.state.getExile(game.player1Id)) shouldBe true
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                }
                game.spirits().size shouldBe 1
            }

            test("casts the exiled card for free, then it goes to the bottom of its owner's library") {
                val (game, bolt) = endStepWithBoltExiled()

                game.activate(bolt).error shouldBe null
                game.resolveStack()
                withClue("the Spirit was sacrificed as a cost") { game.spirits().size shouldBe 0 }

                game.execute(CastSpell(game.player1Id, bolt, listOf(ChosenTarget.Player(game.player2Id))))
                    .error shouldBe null
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 17
                withClue("the resolved Bolt is on the bottom of the library, not in the graveyard or exile") {
                    game.state.getLibrary(game.player1Id).last() shouldBe bolt
                    game.state.getLibrary(game.player1Id).first() shouldNotBe bolt
                    game.isInGraveyard(1, "Lightning Bolt") shouldBe false
                    (bolt in game.state.getExile(game.player1Id)) shouldBe false
                }
            }

            test("a countered spell goes to the bottom of the library too") {
                val (game, bolt) = endStepWithBoltExiled {
                    withCardInHand(2, "Counterspell").withLandsOnBattlefield(2, "Island", 2)
                }
                game.activate(bolt).error shouldBe null
                game.resolveStack()

                game.execute(CastSpell(game.player1Id, bolt, listOf(ChosenTarget.Player(game.player2Id))))
                    .error shouldBe null
                game.passPriority()
                game.castSpellTargetingStackSpell(2, "Counterspell", "Lightning Bolt").error shouldBe null
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 20
                withClue("the countered Bolt is on the bottom of the library") {
                    game.state.getLibrary(game.player1Id).last() shouldBe bolt
                    game.state.getLibrary(game.player1Id).first() shouldNotBe bolt
                    game.isInGraveyard(1, "Lightning Bolt") shouldBe false
                }
            }

            test("only cards exiled with Quintorius can be targeted") {
                val (game, _) = endStepWithBoltExiled { withCardInExile(1, "Shock") }
                val unlinked = game.state.getExile(game.player1Id).single {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Shock"
                }
                game.activate(unlinked).error shouldNotBe null
                withClue("the failed activation paid nothing") { game.spirits().size shouldBe 1 }
            }

            test("the permission lasts only this turn; an uncast card stays exiled") {
                val (game, bolt) = endStepWithBoltExiled()
                game.activate(bolt).error shouldBe null
                game.resolveStack()
                game.state.mayPlayPermissions.any { bolt in it.cardIds } shouldBe true

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                withClue("next turn the Bolt is exiled and no longer castable") {
                    (bolt in game.state.getExile(game.player1Id)) shouldBe true
                    game.state.mayPlayPermissions.any { bolt in it.cardIds } shouldBe false
                }
            }
        }
    }
}
