package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Shadowfax, Lord of Horses (LTR #227) — {3}{R}{W} Legendary Horse, 4/4.
 *
 * - Horses you control have haste. (Keyword-anthem lord static over
 *   `Creature.withSubtype(Horse).youControl()`.)
 * - Whenever Shadowfax attacks, you may put a creature card with lesser power from your hand
 *   onto the battlefield tapped and attacking. ("lesser power" = strict
 *   `powerLessThanEntity(Source)` over hand cards; tapped+attacking entry via
 *   `Patterns.Hand.putFromHand(entersAttacking = true)`.)
 */
class ShadowfaxLordOfHorsesScenarioTest : ScenarioTestBase() {

    init {
        // A summoning-sick Horse to prove the haste anthem lets it attack the turn it enters.
        cardRegistry.register(
            CardDefinition.creature(
                name = "Test Pony",
                manaCost = ManaCost.parse("{1}{W}"),
                subtypes = setOf(Subtype("Horse")),
                power = 2,
                toughness = 2
            )
        )
        // Power-3 creature card: eligible (3 < 4) to be put in via Shadowfax's attack trigger.
        cardRegistry.register(
            CardDefinition.creature(
                name = "Lesser Beast",
                manaCost = ManaCost.parse("{2}{G}"),
                subtypes = setOf(Subtype("Bear")),
                power = 3,
                toughness = 3
            )
        )
        // Power-5 creature card: NOT eligible (5 >= 4).
        cardRegistry.register(
            CardDefinition.creature(
                name = "Greater Beast",
                manaCost = ManaCost.parse("{4}{G}"),
                subtypes = setOf(Subtype("Bear")),
                power = 5,
                toughness = 5
            )
        )

        context("Shadowfax, Lord of Horses") {

            test("Horses you control have haste — a Horse can attack the turn it enters") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Shadowfax, Lord of Horses", summoningSickness = false)
                    // Test Pony just entered (summoning sick) — only haste lets it attack now.
                    .withCardOnBattlefield(1, "Test Pony", summoningSickness = true)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val pony = game.findPermanent("Test Pony")!!
                withClue("Test Pony has haste from Shadowfax's anthem") {
                    game.state.projectedState.hasKeyword(pony, Keyword.HASTE) shouldBe true
                }

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                val attack = game.declareAttackers(mapOf("Test Pony" to 2))
                withClue("summoning-sick Horse with haste can be declared as an attacker: ${attack.error}") {
                    attack.error shouldBe null
                }
                withClue("Test Pony is attacking") {
                    game.state.getEntity(pony)?.has<AttackingComponent>() shouldBe true
                }
            }

            test("attack: may put a lesser-power creature from hand onto the battlefield tapped and attacking") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Shadowfax, Lord of Horses", summoningSickness = false)
                    .withCardInHand(1, "Lesser Beast")  // power 3 < 4: eligible
                    .withCardInHand(1, "Greater Beast") // power 5 >= 4: NOT eligible
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Shadowfax, Lord of Horses" to 2)).error shouldBe null

                // The attack trigger goes on the stack; resolving it asks the controller to choose
                // up to one eligible creature card from hand.
                game.resolveStack()

                withClue("attack trigger paused for the hand-card selection") {
                    game.hasPendingDecision() shouldBe true
                }

                // The power-5 Greater Beast must NOT be a selectable option (5 is not < 4); only
                // the power-3 Lesser Beast qualifies. Select it.
                val lesser = game.findCardsInHand(1, "Lesser Beast").single()
                game.selectCards(listOf(lesser)).error shouldBe null
                game.resolveStack()

                val beast = game.findPermanents("Lesser Beast").singleOrNull()
                    ?: error("Lesser Beast should be on the battlefield once, found ${game.findPermanents("Lesser Beast").size}")
                withClue("the put creature enters tapped") {
                    game.state.getEntity(beast)?.has<TappedComponent>() shouldBe true
                }
                withClue("the put creature enters attacking") {
                    game.state.getEntity(beast)?.has<AttackingComponent>() shouldBe true
                }
                withClue("Greater Beast (power 5) stayed in hand — not eligible") {
                    game.isInHand(1, "Greater Beast") shouldBe true
                }
            }

            test("the power-5 creature is not eligible to be put in even when it's the only option") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Shadowfax, Lord of Horses", summoningSickness = false)
                    .withCardInHand(1, "Greater Beast") // power 5 >= 4: NOT eligible
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Shadowfax, Lord of Horses" to 2)).error shouldBe null
                game.resolveStack()

                // No eligible card → the trigger finds nothing to put in. Greater Beast stays put.
                if (game.hasPendingDecision()) {
                    game.skipSelection()
                    game.resolveStack()
                }
                withClue("no power-5 creature was cheated onto the battlefield") {
                    game.findPermanents("Greater Beast").size shouldBe 0
                }
                withClue("Greater Beast remains in hand") {
                    game.isInHand(1, "Greater Beast") shouldBe true
                }
            }
        }
    }
}
