package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Bruna, Light of Alabaster (AVR #208) — "Whenever Bruna attacks or blocks, you may attach to it any
 * number of Auras on the battlefield and you may put onto the battlefield attached to it any number
 * of Aura cards that could enchant it from your graveyard and/or hand."
 */
class BrunaLightOfAlabasterScenarioTest : ScenarioTestBase() {

    private fun TestGame.attachedTo(id: EntityId): EntityId? =
        state.getEntity(id)?.get<AttachedToComponent>()?.targetId

    init {
        context("Bruna, Light of Alabaster") {

            test("on attack, steals an opponent's Aura and returns an Aura from the graveyard") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Bruna, Light of Alabaster", summoningSickness = false)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardAttachedTo(2, "Holy Strength", "Grizzly Bears")
                    .withCardInGraveyard(1, "Unholy Strength")
                    .withCardInGraveyard(1, "Erosion")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val bruna = game.findPermanent("Bruna, Light of Alabaster")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val holy = game.findPermanent("Holy Strength")!!
                val unholy = game.findCardsInGraveyard(1, "Unholy Strength").single()

                game.declareAttackers(mapOf("Bruna, Light of Alabaster" to 2)).error shouldBe null
                game.resolveStack()

                val fromBattlefield = game.getPendingDecision()
                fromBattlefield.shouldBeInstanceOf<SelectCardsDecision>()
                fromBattlefield.minSelections shouldBe 0
                fromBattlefield.options shouldContain holy
                game.selectCards(listOf(holy)).error shouldBe null
                game.resolveStack()

                val fromCards = game.getPendingDecision()
                fromCards.shouldBeInstanceOf<SelectCardsDecision>()
                withClue("only Aura cards that could enchant Bruna are offered — not Erosion (enchant land)") {
                    fromCards.options shouldContainExactlyInAnyOrder listOf(unholy)
                }
                game.selectCards(listOf(unholy)).error shouldBe null
                game.resolveStack()

                val returned = game.findPermanent("Unholy Strength")!!
                val projected = game.state.projectedState
                withClue("Holy Strength moved off the Bears onto Bruna; Unholy Strength entered attached") {
                    game.attachedTo(holy) shouldBe bruna
                    game.attachedTo(returned) shouldBe bruna
                    projected.getPower(bruna) shouldBe 8
                    projected.getToughness(bruna) shouldBe 8
                    projected.getPower(bears) shouldBe 2
                }
                withClue("the moved Aura keeps its controller") {
                    projected.getController(holy) shouldBe game.player2Id
                }
                game.isInGraveyard(1, "Erosion") shouldBe true
            }

            test("on block, puts an Aura from hand onto it; an Aura that can't enchant it stays put") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Bruna, Light of Alabaster")
                    .withCardOnBattlefield(1, "Plains")
                    .withCardAttachedTo(1, "Erosion", "Plains")
                    .withCardInHand(1, "Holy Strength")
                    .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(2)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val bruna = game.findPermanent("Bruna, Light of Alabaster")!!
                val plains = game.findPermanent("Plains")!!
                val erosion = game.findPermanent("Erosion")!!
                val holy = game.findCardsInHand(1, "Holy Strength").single()

                game.declareAttackers(mapOf("Grizzly Bears" to 1)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Bruna, Light of Alabaster" to listOf("Grizzly Bears"))).error shouldBe null
                game.resolveStack()

                game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                game.selectCards(listOf(erosion)).error shouldBe null
                game.resolveStack()

                val fromCards = game.getPendingDecision()
                fromCards.shouldBeInstanceOf<SelectCardsDecision>()
                fromCards.options shouldContainExactlyInAnyOrder listOf(holy)
                game.selectCards(listOf(holy)).error shouldBe null
                game.resolveStack()

                withClue("Erosion (enchant land) couldn't legally move to Bruna and stays on the Plains") {
                    game.attachedTo(erosion) shouldBe plains
                }
                val aura = game.findPermanent("Holy Strength")!!
                game.attachedTo(aura) shouldBe bruna
                game.state.projectedState.getPower(bruna) shouldBe 6
                game.state.projectedState.getToughness(bruna) shouldBe 7
            }

            test("declining both choices moves nothing") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Bruna, Light of Alabaster", summoningSickness = false)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardAttachedTo(2, "Holy Strength", "Grizzly Bears")
                    .withCardInHand(1, "Unholy Strength")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val holy = game.findPermanent("Holy Strength")!!

                game.declareAttackers(mapOf("Bruna, Light of Alabaster" to 2)).error shouldBe null
                game.resolveStack()
                game.skipSelection().error shouldBe null
                game.resolveStack()
                game.skipSelection().error shouldBe null
                game.resolveStack()

                game.attachedTo(holy) shouldBe bears
                game.isInHand(1, "Unholy Strength") shouldBe true
            }
        }
    }
}
