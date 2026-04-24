package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Kirol, Attentive First-Year.
 *
 * {1}{R/W}{R/W} — Legendary Creature — Vampire Cleric, 3/3
 * "Tap two untapped creatures you control: Copy target triggered ability you control.
 *  You may choose new targets for the copy. Activate only once each turn."
 */
class KirolAttentiveFirstYearScenarioTest : ScenarioTestBase() {

    // Test creature with a simple non-targeted ETB trigger (gain 2 life) that we'll
    // use to put a copyable triggered ability on the stack.
    private val lifegainCaller = card("Lifegain Caller") {
        manaCost = "{W}"
        typeLine = "Creature — Human Cleric"
        power = 1
        toughness = 1

        triggeredAbility {
            trigger = Triggers.EntersBattlefield
            effect = Effects.GainLife(2)
            description = "When Lifegain Caller enters, you gain 2 life."
        }
    }

    // Plain vanilla body with no triggers or static abilities — used to pay Kirol's
    // tap-two cost without polluting the stack with other triggers.
    private val tapFodder = card("Tap Fodder") {
        manaCost = "{W}"
        typeLine = "Creature — Human"
        power = 1
        toughness = 1
    }

    init {
        cardRegistry.register(lifegainCaller)
        cardRegistry.register(tapFodder)

        context("Kirol, Attentive First-Year — copy triggered ability") {

            test("copies a non-targeted ETB trigger; both resolve for double effect") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Kirol, Attentive First-Year")
                    .withCardOnBattlefield(1, "Tap Fodder")
                    .withCardOnBattlefield(1, "Tap Fodder")
                    .withCardInHand(1, "Lifegain Caller")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val startingLife = game.getLifeTotal(1)

                // Cast Lifegain Caller, then pass priority twice so the spell resolves
                // and the ETB trigger lands on the stack without being resolved yet.
                game.castSpell(1, "Lifegain Caller")
                game.passPriority()
                game.passPriority()

                withClue(
                    "Lifegain Caller's ETB trigger should be on the stack after the spell resolves. " +
                        "Stack size: ${game.state.stack.size}"
                ) {
                    game.state.stack.size shouldBe 1
                }
                val triggerEntityId = game.state.stack.last()

                val kirolId = game.findPermanent("Kirol, Attentive First-Year")!!
                val kirolDef = cardRegistry.getCard("Kirol, Attentive First-Year")!!
                val copyAbility = kirolDef.script.activatedAbilities[0]

                val fodderIds = game.findAllPermanents("Tap Fodder")

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = kirolId,
                        abilityId = copyAbility.id,
                        targets = listOf(
                            com.wingedsheep.engine.state.components.stack.ChosenTarget.Spell(triggerEntityId)
                        ),
                        costPayment = AdditionalCostPayment(tappedPermanents = fodderIds.take(2))
                    )
                )
                withClue("Kirol's copy ability should succeed: ${result.error}") {
                    result.error shouldBe null
                }

                // Two triggers should now be on the stack: the original + the copy.
                withClue("Stack should have the original ETB trigger plus a copy") {
                    game.state.stack.size shouldBe 2
                }

                // Resolve both triggers.
                game.resolveStack()

                // 2 life gained from original + 2 from copy = 4 life above starting.
                withClue("Player 1 should have gained 4 life (2 from original + 2 from copy)") {
                    game.getLifeTotal(1) shouldBe startingLife + 4
                }
            }

            test("cannot activate without two untapped creatures") {
                // Only Kirol is on the battlefield. Lifegain Caller arrives untapped, but
                // that's still only one untapped creature other than Kirol — but even
                // tapping Kirol himself + the caller, the caller was just-cast and even
                // if it counted, we try to pass an empty tapped-permanents list to test
                // the validator. Specifically here: no partner creature exists when the
                // activation is attempted with an empty cost payment list, so cost fails.
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Kirol, Attentive First-Year", tapped = true)
                    .withCardOnBattlefield(1, "Tap Fodder", tapped = true)
                    .withCardInHand(1, "Lifegain Caller")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Lifegain Caller")
                game.passPriority()
                game.passPriority()
                val triggerEntityId = game.state.stack.last()

                val kirolId = game.findPermanent("Kirol, Attentive First-Year")!!
                val kirolDef = cardRegistry.getCard("Kirol, Attentive First-Year")!!
                val copyAbility = kirolDef.script.activatedAbilities[0]

                // Find the only untapped creature (Lifegain Caller); supplying just one
                // should fail the "tap 2" cost validation.
                val lifegainId: EntityId? = game.state.entities.entries.firstOrNull { (_, container) ->
                    container.get<CardComponent>()?.name == "Lifegain Caller" &&
                        container.get<ControllerComponent>()?.playerId == game.player1Id
                }?.key

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = kirolId,
                        abilityId = copyAbility.id,
                        targets = listOf(
                            com.wingedsheep.engine.state.components.stack.ChosenTarget.Spell(triggerEntityId)
                        ),
                        costPayment = AdditionalCostPayment(
                            tappedPermanents = listOfNotNull(lifegainId)
                        )
                    )
                )
                withClue("Activation must fail with only one untapped creature available") {
                    result.error shouldNotBe null
                }
            }

            test("once-per-turn restriction blocks a second activation") {
                // Four untapped creatures so the cost is payable twice.
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Kirol, Attentive First-Year")
                    .withCardOnBattlefield(1, "Tap Fodder")
                    .withCardOnBattlefield(1, "Tap Fodder")
                    .withCardOnBattlefield(1, "Tap Fodder")
                    .withCardInHand(1, "Lifegain Caller")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Lifegain Caller")
                game.passPriority()
                game.passPriority()

                val firstTrigger = game.state.stack.last()
                val kirolId = game.findPermanent("Kirol, Attentive First-Year")!!
                val kirolDef = cardRegistry.getCard("Kirol, Attentive First-Year")!!
                val copyAbility = kirolDef.script.activatedAbilities[0]
                val fodderIds = game.findAllPermanents("Tap Fodder")

                val first = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = kirolId,
                        abilityId = copyAbility.id,
                        targets = listOf(
                            com.wingedsheep.engine.state.components.stack.ChosenTarget.Spell(firstTrigger)
                        ),
                        costPayment = AdditionalCostPayment(tappedPermanents = fodderIds.take(2))
                    )
                )
                withClue("First activation should succeed: ${first.error}") {
                    first.error shouldBe null
                }

                // Try activating again with the remaining untapped creature (Kirol + 1 fodder),
                // targeting the still-on-stack original trigger. The source ability is now
                // blocked by OncePerTurn regardless of payment.
                val second = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = kirolId,
                        abilityId = copyAbility.id,
                        targets = listOf(
                            com.wingedsheep.engine.state.components.stack.ChosenTarget.Spell(firstTrigger)
                        ),
                        costPayment = AdditionalCostPayment(
                            tappedPermanents = listOf(kirolId, fodderIds[2])
                        )
                    )
                )
                withClue("Second activation in the same turn should be rejected") {
                    second.error shouldNotBe null
                }
            }
        }
    }
}
