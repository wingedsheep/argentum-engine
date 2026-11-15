package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain

/**
 * Corpseweft (DTK #92) — {1}{B}, Exile one or more creature cards from your graveyard: Create a
 * tapped X/X black Zombie Horror creature token, where X is twice the number of cards exiled this way.
 *
 * The load-bearing claim is "one or more": the exile-X cost has no `{X}` mana, so X is the size of
 * the graveyard selection, floored at one by `minimumXValue = 1`. With no creature card in the
 * graveyard the ability isn't offered at all, the selection can't be empty, and a single candidate
 * still prompts (X is bound by the pick, not silently paid as zero).
 */
class CorpseweftScenarioTest : ScenarioTestBase() {

    private val abilityId
        get() = cardRegistry.getCard("Corpseweft")!!.script.activatedAbilities[0].id

    /** Whether the ability is offered as payable (an unpayable one is listed greyed out). */
    private fun ScenarioTestBase.TestGame.corpseweftOffered() = getLegalActions(1).any {
        (it.action as? ActivateAbility)?.sourceId == findPermanent("Corpseweft") && it.isAffordable
    }

    private fun ScenarioTestBase.TestGame.newTokens(before: Set<com.wingedsheep.sdk.model.EntityId>) =
        state.getBattlefield().filter { it !in before }

    init {
        context("Corpseweft") {

            test("X is twice the creature cards exiled; only creature cards are offered; the token enters tapped") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Corpseweft")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInGraveyard(1, "Centaur Courser")
                    .withCardInGraveyard(1, "Swamp") // not a creature card — never offered
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val before = game.state.getBattlefield().toSet()
                game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = game.findPermanent("Corpseweft")!!, abilityId = abilityId)
                ).error shouldBe null

                val decision = game.getPendingDecision() as SelectCardsDecision
                val bears = game.findCardsInGraveyard(1, "Grizzly Bears")
                withClue("creature cards only, at least one") {
                    decision.options.size shouldBe 3
                    decision.options.toSet() shouldBe (bears + game.findCardsInGraveyard(1, "Centaur Courser")).toSet()
                    decision.minSelections shouldBe 1
                    decision.maxSelections shouldBe 3
                }

                game.selectCards(bears).error shouldBe null
                game.resolveStack()

                val tokens = game.newTokens(before)
                tokens.size shouldBe 1
                val token = tokens.single()
                withClue("two cards exiled → a 4/4") {
                    game.state.projectedState.getPower(token) shouldBe 4
                    game.state.projectedState.getToughness(token) shouldBe 4
                }
                withClue("the token enters tapped") {
                    game.state.getEntity(token)!!.has<TappedComponent>() shouldBe true
                }
                game.findCardsInGraveyard(1, "Grizzly Bears").size shouldBe 0
                game.isInGraveyard(1, "Centaur Courser") shouldBe true
            }

            test("with no creature card in the graveyard the ability can't be activated") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Corpseweft")
                    .withCardInGraveyard(1, "Swamp")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                withClue("not offered as payable") { game.corpseweftOffered() shouldBe false }
                val result = game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = game.findPermanent("Corpseweft")!!, abilityId = abilityId)
                )
                withClue("an engine-direct activation is rejected too") {
                    result.error shouldNotBe null
                    result.error!! shouldContain "Not enough"
                }
            }

            test("a single creature card still prompts and makes a 2/2 — X is never silently zero") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Corpseweft")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                withClue("offered with one creature card") { game.corpseweftOffered() shouldBe true }
                val before = game.state.getBattlefield().toSet()
                game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = game.findPermanent("Corpseweft")!!, abilityId = abilityId)
                ).error shouldBe null
                val decision = game.getPendingDecision() as SelectCardsDecision
                decision.minSelections shouldBe 1
                decision.maxSelections shouldBe 1
                game.selectCards(decision.options).error shouldBe null
                game.resolveStack()

                val token = game.newTokens(before).single()
                game.state.projectedState.getPower(token) shouldBe 2
                game.state.projectedState.getToughness(token) shouldBe 2
            }

            test("a submitted empty exile selection with X = 0 is rejected") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Corpseweft")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id, sourceId = game.findPermanent("Corpseweft")!!, abilityId = abilityId,
                        xValue = 0,
                        costPayment = AdditionalCostPayment(exiledCards = emptyList()),
                    )
                )
                result.error shouldNotBe null
                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            }
        }
    }
}
