package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Floodtide Serpent (BNG #41) — {4}{U} 4/4 Serpent.
 * "This creature can't attack unless you return an enchantment you control to its owner's hand.
 *  (This cost is paid as attackers are declared.)"
 */
class FloodtideSerpentScenarioTest : ScenarioTestBase() {

    init {
        context("Floodtide Serpent") {

            test("can't attack without an enchantment you control to return") {
                val game = scenario()
                    .withPlayers("Tide", "Shore")
                    .withCardOnBattlefield(1, "Floodtide Serpent")
                    .withCardOnBattlefield(2, "Glorious Anthem")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val result = game.declareAttackers(mapOf("Floodtide Serpent" to 2))
                withClue("an opponent's enchantment can't pay the cost, so the declaration is illegal") {
                    result.error shouldNotBe null
                }
            }

            test("an opponent's enchantment submitted as payment is rejected") {
                val game = scenario()
                    .withPlayers("Tide", "Shore")
                    .withCardOnBattlefield(1, "Floodtide Serpent")
                    .withCardOnBattlefield(1, "Glorious Anthem")
                    .withCardOnBattlefield(2, "Glorious Anthem")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val theirAnthem = game.findPermanents("Glorious Anthem")
                    .single { game.state.getBattlefield(game.player2Id).contains(it) }

                game.declareAttackers(mapOf("Floodtide Serpent" to 2)).error shouldBe null
                val decision = game.state.pendingDecision
                decision.shouldBeInstanceOf<SelectCardsDecision>()

                val result = game.submitDecision(CardsSelectedResponse(decision.id, listOf(theirAnthem)))
                withClue("the cost is an enchantment *you control*") {
                    result.error shouldNotBe null
                    game.findPermanents("Glorious Anthem").size shouldBe 2
                }
            }

            test("two Serpents can't both attack on one enchantment") {
                val game = scenario()
                    .withPlayers("Tide", "Shore")
                    .withCardOnBattlefield(1, "Floodtide Serpent")
                    .withCardOnBattlefield(1, "Floodtide Serpent")
                    .withCardOnBattlefield(1, "Glorious Anthem")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val serpents = game.findPermanents("Floodtide Serpent")
                val defender = game.player2Id
                val result = game.execute(
                    com.wingedsheep.engine.core.DeclareAttackers(game.player1Id, serpents.associateWith { defender })
                )
                withClue("each cost is payable alone, but not both together, so the declaration is refused") {
                    result.error shouldNotBe null
                    game.state.pendingDecision shouldBe null
                }
            }

            test("an enchantment creature declared as an attacker can pay, and then doesn't attack") {
                val game = scenario()
                    .withPlayers("Tide", "Shore")
                    .withCardOnBattlefield(1, "Floodtide Serpent")
                    .withCardOnBattlefield(1, "Nyx-Fleece Ram")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val serpent = game.findPermanent("Floodtide Serpent")!!
                val ram = game.findPermanent("Nyx-Fleece Ram")!!
                game.declareAttackers(mapOf("Floodtide Serpent" to 2, "Nyx-Fleece Ram" to 2)).error shouldBe null
                val decision = game.state.pendingDecision
                decision.shouldBeInstanceOf<SelectCardsDecision>()
                decision.options shouldContainExactly listOf(ram)

                game.submitDecision(CardsSelectedResponse(decision.id, listOf(ram))).error shouldBe null

                withClue("the Ram went back to hand and is not attacking; the Serpent is") {
                    game.isInHand(1, "Nyx-Fleece Ram") shouldBe true
                    game.state.getEntity(serpent)?.has<AttackingComponent>() shouldBe true
                }
            }

            test("attacking pauses to return one of your enchantments to hand") {
                val game = scenario()
                    .withPlayers("Tide", "Shore")
                    .withCardOnBattlefield(1, "Floodtide Serpent")
                    .withCardOnBattlefield(1, "Glorious Anthem")
                    .withCardOnBattlefield(2, "Glorious Anthem")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val serpent = game.findPermanent("Floodtide Serpent")!!
                val ownAnthem = game.findPermanents("Glorious Anthem")
                    .single { game.state.getBattlefield(game.player1Id).contains(it) }

                game.declareAttackers(mapOf("Floodtide Serpent" to 2)).error shouldBe null

                val decision = game.state.pendingDecision
                decision.shouldNotBeNull()
                decision.shouldBeInstanceOf<SelectCardsDecision>()
                withClue("only your own enchantment is eligible — not the Island, not the opponent's Anthem") {
                    decision.options shouldContainExactly listOf(ownAnthem)
                    decision.minSelections shouldBe 1
                    decision.maxSelections shouldBe 1
                }

                game.submitDecision(CardsSelectedResponse(decision.id, listOf(ownAnthem)))

                withClue("the Anthem went back to its owner's hand, not the graveyard") {
                    game.isInHand(1, "Glorious Anthem") shouldBe true
                    game.isInGraveyard(1, "Glorious Anthem") shouldBe false
                    game.findPermanents("Glorious Anthem").size shouldBe 1
                }
                withClue("and the Serpent is attacking") {
                    game.state.getEntity(serpent)?.has<AttackingComponent>() shouldBe true
                }
            }
        }
    }
}
