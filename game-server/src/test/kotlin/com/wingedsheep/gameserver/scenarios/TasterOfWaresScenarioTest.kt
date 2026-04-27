package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.MayPlayFromExileComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain

/**
 * Scenario tests for Taster of Wares.
 *
 * {2}{B} Creature — Goblin Warlock 3/2
 * When this creature enters, target opponent reveals X cards from their hand,
 * where X is the number of Goblins you control. You choose one of those cards.
 * That player exiles it. If an instant or sorcery card is exiled this way, you
 * may cast it for as long as you control this creature, and mana of any type
 * can be spent to cast that spell.
 */
class TasterOfWaresScenarioTest : ScenarioTestBase() {

    init {
        context("Taster of Wares — stack display") {

            test("triggered ability on stack uses the description override and exposes X") {
                // 2 Goblins on the battlefield + Taster (a Goblin Warlock) entering = X = 3.
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Taster of Wares")
                    .withCardOnBattlefield(1, "Raging Goblin")
                    .withCardOnBattlefield(1, "Goblin Bully")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardInLibrary(1, "Swamp")
                    .withCardInHand(2, "Volcanic Hammer")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castResult = game.castSpell(1, "Taster of Wares")
                withClue("Cast should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Resolve Taster the spell so the ETB trigger goes on the stack, but don't
                // resolve the trigger itself yet — we want to inspect its on-stack state.
                game.passPriority()
                game.passPriority()
                val triggerOnStack: TriggeredAbilityOnStackComponent? =
                    game.state.stack.firstNotNullOfOrNull { id ->
                        game.state.getEntity(id)?.get<TriggeredAbilityOnStackComponent>()
                    }
                withClue("Triggered ability should be on the stack after Taster resolves") {
                    triggerOnStack shouldNotBe null
                }
                withClue("descriptionOverride should be propagated from the card") {
                    triggerOnStack!!.descriptionOverride shouldNotBe null
                    triggerOnStack.descriptionOverride!! shouldContain "reveals X cards"
                    triggerOnStack.descriptionOverride!! shouldContain "mana of any type"
                }
                withClue("X should be exposed as the goblin count (Raging + Bully + Taster = 3)") {
                    triggerOnStack!!.xValue shouldBe 3
                }
            }
        }

        context("Taster of Wares — mana of any type") {

            test("exiled instant/sorcery can be cast paying its colored cost with off-color mana") {
                // Player 1 has only Swamps. After casting Taster of Wares ({2}{B}), the
                // exiled Volcanic Hammer ({1}{R}) must be castable using the remaining
                // Swamps thanks to the "mana of any type can be spent" clause.
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Taster of Wares")
                    .withLandsOnBattlefield(1, "Swamp", 5)
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInHand(2, "Volcanic Hammer")
                    .withCardInLibrary(2, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castResult = game.castSpell(1, "Taster of Wares")
                withClue("Cast should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                // Opponent picks the (only) card to reveal.
                if (game.hasPendingDecision()) {
                    val cardsInHand = game.state.getHand(game.player2Id)
                    game.selectCards(cardsInHand)
                    game.resolveStack()
                }

                // Controller picks the card to exile.
                if (game.hasPendingDecision()) {
                    val volcanicHammer = game.findCardsInHand(2, "Volcanic Hammer")
                    game.selectCards(volcanicHammer)
                    game.resolveStack()
                }

                val exile = game.state.getExile(game.player2Id)
                val volcanicHammerExiled = exile.find { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Volcanic Hammer"
                }
                withClue("Volcanic Hammer should be in opponent's exile") {
                    volcanicHammerExiled shouldNotBe null
                }
                val mayPlay = game.state.getEntity(volcanicHammerExiled!!)
                    ?.get<MayPlayFromExileComponent>()
                withClue("MayPlayFromExileComponent should be granted to player 1") {
                    mayPlay shouldNotBe null
                    mayPlay!!.controllerId shouldBe game.player1Id
                }
                withClue("withAnyManaType should be true on the granted permission") {
                    mayPlay!!.withAnyManaType shouldBe true
                }

                // The casting player only has Swamp mana available — without the relaxation,
                // {1}{R} cannot be paid. With it, two Swamps satisfy the cost.
                val initialOpponentLife = game.getLifeTotal(2)
                val castFromExile = game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = volcanicHammerExiled,
                        targets = listOf(ChosenTarget.Player(game.player2Id))
                    )
                )
                withClue("Casting Volcanic Hammer from exile with off-color mana should succeed: ${castFromExile.error}") {
                    castFromExile.error shouldBe null
                }
                game.resolveStack()

                withClue("Volcanic Hammer should resolve and deal 3 damage") {
                    game.getLifeTotal(2) shouldBe initialOpponentLife - 3
                }
            }
        }

    }
}
