package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.identity.RingBearerComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Sauron, the Necromancer (LTR #106) — {3}{B}{B} Legendary Avatar Horror, 4/4.
 *
 * Menace
 * Whenever Sauron attacks, exile target creature card from your graveyard. Create a tapped and
 * attacking token that's a copy of that card, except it's a 3/3 black Wraith with menace. At the
 * beginning of the next end step, exile that token unless Sauron is your Ring-bearer.
 *
 * Exercises the `CreateTokenCopyOfTargetEffect.exileAtStep` / `exileUnlessSourceIsRingBearer`
 * additions (the exile sibling of the existing `sacrificeAtStep`, gated on the source being the
 * Ring-bearer), plus the token-copy overrides (P/T 3/3, black, Wraith, added menace), tapped, and
 * attacking. The copied card carries flying to prove the token inherits the source's copiable
 * keywords on top of the overrides.
 */
class SauronTheNecromancerScenarioTest : ScenarioTestBase() {

    init {
        cardRegistry.register(
            CardDefinition.creature(
                name = "Dead Goblin",
                manaCost = ManaCost.parse("{1}{R}"),
                subtypes = setOf(Subtype("Goblin")),
                power = 5,
                toughness = 1,
                keywords = setOf(Keyword.FLYING)
            )
        )

        context("Sauron, the Necromancer") {

            test("has menace") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Sauron, the Necromancer", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val sauron = game.findPermanent("Sauron, the Necromancer")!!
                withClue("Sauron has menace") {
                    game.state.projectedState.hasKeyword(sauron, Keyword.MENACE) shouldBe true
                }
            }

            test("attack exiles a graveyard creature card and makes a tapped attacking 3/3 black Wraith with menace") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Sauron, the Necromancer", tapped = false, summoningSickness = false)
                    .withCardInGraveyard(1, "Dead Goblin")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                val attack = game.declareAttackers(mapOf("Sauron, the Necromancer" to 2))
                withClue("Declaring Sauron as attacker should succeed: ${attack.error}") {
                    attack.error shouldBe null
                }

                // The attack trigger targets a creature card in the graveyard. With one legal
                // target the engine may auto-target; otherwise resolve the target decision.
                resolveTriggerTargetingDeadGoblin(game)
                game.resolveStack()

                // The Dead Goblin card was exiled from the graveyard.
                withClue("Dead Goblin is no longer in the graveyard") {
                    game.state.getGraveyard(game.player1Id)
                        .mapNotNull { game.cardName(it) }
                        .contains("Dead Goblin") shouldBe false
                }

                val token = game.findPermanents("Dead Goblin").singleOrNull()
                    ?: error("Expected exactly one token copy of Dead Goblin, found ${game.findPermanents("Dead Goblin").size}")

                withClue("token enters tapped") {
                    game.state.getEntity(token)?.has<TappedComponent>() shouldBe true
                }
                withClue("token enters attacking") {
                    game.state.getEntity(token)?.has<AttackingComponent>() shouldBe true
                }
                withClue("token is 3/3 (overridden P/T, not the card's 5/1)") {
                    game.state.projectedState.getPower(token) shouldBe 3
                    game.state.projectedState.getToughness(token) shouldBe 3
                }
                withClue("token is black") {
                    game.state.projectedState.getColors(token) shouldBe setOf("BLACK")
                }
                withClue("token is a Wraith (overridden subtype, not Goblin)") {
                    game.state.projectedState.getSubtypes(token) shouldBe setOf("Wraith")
                }
                withClue("token has menace (added keyword)") {
                    game.state.projectedState.hasKeyword(token, Keyword.MENACE) shouldBe true
                }
                withClue("token inherits the copied card's flying") {
                    game.state.projectedState.hasKeyword(token, Keyword.FLYING) shouldBe true
                }
            }

            test("token is exiled at the next end step when Sauron is NOT the Ring-bearer") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Sauron, the Necromancer", tapped = false, summoningSickness = false)
                    .withCardInGraveyard(1, "Dead Goblin")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Sauron, the Necromancer" to 2))
                resolveTriggerTargetingDeadGoblin(game)
                game.resolveStack()

                game.findPermanents("Dead Goblin").size shouldBe 1

                // Advance to the end step and drain the delayed "exile that token" trigger.
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("Sauron is not the Ring-bearer, so the token is exiled at the next end step") {
                    game.findPermanents("Dead Goblin").size shouldBe 0
                }
            }

            test("token survives the next end step when Sauron IS the Ring-bearer") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Sauron, the Necromancer", tapped = false, summoningSickness = false)
                    .withCardInGraveyard(1, "Dead Goblin")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Designate Sauron as Player 1's Ring-bearer.
                val sauron = game.findPermanent("Sauron, the Necromancer")!!
                game.state = game.state.updateEntity(sauron) { container ->
                    container.with(RingBearerComponent(game.player1Id))
                }

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Sauron, the Necromancer" to 2))
                resolveTriggerTargetingDeadGoblin(game)
                game.resolveStack()

                game.findPermanents("Dead Goblin").size shouldBe 1

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("Sauron is the Ring-bearer, so the token is NOT exiled and survives") {
                    game.findPermanents("Dead Goblin").size shouldBe 1
                }
            }
        }
    }

    /**
     * If the attack trigger paused for target selection, choose the Dead Goblin card in Player 1's
     * graveyard; otherwise (single legal target auto-chosen) do nothing.
     */
    private fun resolveTriggerTargetingDeadGoblin(game: TestGame) {
        if (game.hasPendingDecision()) {
            val deadGoblin = game.state.getGraveyard(game.player1Id)
                .first { game.cardName(it) == "Dead Goblin" }
            game.selectTargets(listOf(deadGoblin))
        }
    }
}

private fun ScenarioTestBase.TestGame.cardName(id: EntityId): String? =
    state.getEntity(id)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name
