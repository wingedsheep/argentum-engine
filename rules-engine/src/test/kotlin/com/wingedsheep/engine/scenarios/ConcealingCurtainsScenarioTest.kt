package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.vow.cards.ConcealingCurtains
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Concealing Curtains // Revealing Eye (VOW #101).
 *
 *   Front — Concealing Curtains (0/4) — Defender. {2}{B}: Transform this creature. Sorcery speed.
 *   Back  — Revealing Eye (3/4) — Menace. When this transforms into Revealing Eye, target opponent
 *           reveals their hand. You may choose a nonland card from it. If you do, that player
 *           discards that card, then draws a card.
 *
 * Exercises the {2}{B} sorcery-speed transform and the transforms-into trigger's Duress pipeline:
 * a nonland card is chosen from the targeted opponent's hand, they discard it, then draw.
 */
class ConcealingCurtainsScenarioTest : ScenarioTestBase() {

    private val transformAbilityId = ConcealingCurtains
        .activatedAbilities.first { !it.isManaAbility }.id

    init {
        context("Concealing Curtains") {

            test("{2}{B} transforms Curtains and its trigger discards a chosen nonland card then draws") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Concealing Curtains", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    // The opponent's hand: one nonland to be discarded, plus a land they keep.
                    .withCardInHand(2, "Lightning Bolt")
                    .withCardInHand(2, "Forest")
                    .withCardInLibrary(2, "Island") // a card for them to draw
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val curtains = game.findPermanent("Concealing Curtains")!!
                val oppHandBefore = game.handSize(2)

                game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = curtains, abilityId = transformAbilityId)
                ).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()

                // Resolve the transform + transforms-into trigger. The trigger targets the opponent,
                // reveals their hand, and prompts the controller to choose a nonland card.
                var guard = 0
                while (guard++ < 20) {
                    val decision = game.getPendingDecision()
                    when {
                        decision is SelectCardsDecision -> {
                            // Choose the sole nonland card (Lightning Bolt) to discard.
                            val bolt = decision.options.first {
                                game.state.getEntity(it)?.get<CardComponent>()?.name == "Lightning Bolt"
                            }
                            game.selectCards(listOf(bolt))
                        }
                        game.state.stack.isNotEmpty() -> game.resolveStack()
                        else -> break
                    }
                }

                withClue("Curtains flipped to Revealing Eye") {
                    game.state.getEntity(curtains)!!.get<CardComponent>()!!.name shouldBe "Revealing Eye"
                }
                withClue("the opponent discarded the chosen nonland card") {
                    game.isInGraveyard(2, "Lightning Bolt") shouldBe true
                }
                withClue("the opponent then drew a card — net hand size unchanged (discard 1, draw 1)") {
                    game.handSize(2) shouldBe oppHandBefore
                }
                withClue("the kept land is still in hand") {
                    game.isInHand(2, "Forest") shouldBe true
                }
            }

            test("cannot activate the transform at instant speed on the opponent's turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Concealing Curtains", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withActivePlayer(2) // opponent's turn — sorcery-speed activation is illegal
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val curtains = game.findPermanent("Concealing Curtains")!!
                val result = game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = curtains, abilityId = transformAbilityId)
                )

                withClue("sorcery-speed ability is illegal on the opponent's turn") {
                    (result.error != null).shouldBe(true)
                }
                withClue("it stays Concealing Curtains") {
                    game.state.getEntity(curtains)!!.get<CardComponent>()!!.name shouldBe "Concealing Curtains"
                }
            }
        }
    }
}
