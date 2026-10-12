package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ManaSourcesSelectedResponse
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.plc.cards.SimianSpiritGuide
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Simian Spirit Guide (PLC #122) — "Exile this card from your hand: Add {R}."
 *
 * A mana ability that functions from the hand (CR 605.1a). It is activated like any mana ability —
 * at priority, or while a payment asks for mana (CR 605.3a) — never on the stack, and never by
 * auto-pay: spending a card is the player's call. On the battlefield the card has no usable ability.
 */
class SimianSpiritGuideScenarioTest : ScenarioTestBase() {

    private val guideAbility = SimianSpiritGuide.activatedAbilities.single().id

    init {
        context("Simian Spirit Guide") {
            fun TestGame.redFloating(): Int =
                state.getEntity(player1Id)?.get<ManaPoolComponent>()?.red ?: 0

            test("exiling it from hand adds {R} without using the stack, and that mana casts a spell") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Simian Spirit Guide")
                    .withCardInHand(1, "Lightning Bolt")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val guide = game.findCardsInHand(1, "Simian Spirit Guide").single()

                withClue("offered from the hand, flagged as a mana ability") {
                    game.getLegalActions(1).single { it.action == ActivateAbility(game.player1Id, guide, guideAbility) }
                        .isManaAbility shouldBe true
                }

                game.execute(ActivateAbility(game.player1Id, guide, guideAbility)).error shouldBe null
                game.state.stack.shouldBeEmpty()
                game.isInExile(1, "Simian Spirit Guide") shouldBe true
                game.redFloating() shouldBe 1
                game.state.priorityPlayerId shouldBe game.player1Id

                game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
                game.resolveStack()
                game.getLifeTotal(2) shouldBe 17
            }

            test("casting a spell it alone can pay opens a payment window instead of auto-exiling it") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Simian Spirit Guide")
                    .withCardInHand(1, "Lightning Bolt")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val guide = game.findCardsInHand(1, "Simian Spirit Guide").single()

                withClue("the hand ability counts toward affordability") {
                    services.manaSolver.canPay(game.state, game.player1Id, ManaCost.parse("{R}")) shouldBe true
                    services.manaSolver.canPay(game.state, game.player1Id, ManaCost.parse("{R}{R}")) shouldBe false
                }

                game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
                val window = game.state.pendingDecision as SelectManaSourcesDecision
                withClue("auto-pay never spends the card") {
                    game.isInHand(1, "Simian Spirit Guide") shouldBe true
                }
                services.legalActionEnumerator.enumerateManaAbilities(game.state, game.player1Id)
                    .any { it.action == ActivateAbility(game.player1Id, guide, guideAbility) } shouldBe true

                game.execute(ActivateAbility(game.player1Id, guide, guideAbility)).error shouldBe null
                game.state.pendingDecision!!.id shouldBe window.id
                game.submitDecision(ManaSourcesSelectedResponse(window.id)).error shouldBe null
                game.state.stack.size shouldBe 1
                game.resolveStack()
                game.getLifeTotal(2) shouldBe 17
                game.isInExile(1, "Simian Spirit Guide") shouldBe true
            }

            test("it pays a ward cost from inside the payment window") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardsInHand(1, "Simian Spirit Guide", 2)
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardOnBattlefield(2, "Tomakul Honor Guard")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val guard = game.findPermanent("Tomakul Honor Guard")!!

                game.castSpell(1, "Lightning Bolt", guard).error shouldBe null
                // Ward triggers; resolving it asks for {2} straight away.
                game.resolveStack()
                val window = game.state.pendingDecision as SelectManaSourcesDecision
                for (guide in game.findCardsInHand(1, "Simian Spirit Guide")) {
                    game.execute(ActivateAbility(game.player1Id, guide, guideAbility)).error shouldBe null
                    game.state.pendingDecision!!.id shouldBe window.id
                }
                game.submitDecision(ManaSourcesSelectedResponse(window.id)).error shouldBe null
                game.resolveStack()
                withClue("ward paid, so Bolt resolves") {
                    game.findPermanent("Tomakul Honor Guard") shouldBe null
                }
            }

            test("on the battlefield it has no usable mana ability") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Simian Spirit Guide")
                    .withCardInHand(1, "Lightning Bolt")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val guide = game.findPermanent("Simian Spirit Guide")!!

                game.getLegalActions(1).none { it.action == ActivateAbility(game.player1Id, guide, guideAbility) } shouldBe true
                services.manaSolver.canPay(game.state, game.player1Id, ManaCost.parse("{R}")) shouldBe false
                game.execute(ActivateAbility(game.player1Id, guide, guideAbility)).error shouldNotBe null
                game.findPermanent("Simian Spirit Guide") shouldBe guide
            }
        }
    }
}
