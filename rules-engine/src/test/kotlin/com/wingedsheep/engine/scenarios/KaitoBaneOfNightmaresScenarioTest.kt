package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Kaito, Bane of Nightmares (DSK #220).
 *
 *  Ninjutsu {1}{U}{B}
 *  During your turn, as long as Kaito has one or more loyalty counters on him, he's a 3/4 Ninja
 *  creature and has hexproof.
 *  +1: You get an emblem with "Ninjas you control get +1/+1."
 *  0: Surveil 2. Then draw a card for each opponent who lost life this turn.
 *  −2: Tap target creature. Put two stun counters on it.
 *
 * Exercises the two new behaviors: the conditional self-animation (a type-*setting* static that
 * makes the planeswalker a creature on your turn — he stops being a planeswalker, CR ruling
 * 2024-09-20) and the new Ninjutsu keyword (shared declare-blockers alternative-cost pipeline:
 * return an unblocked attacker, enter tapped and attacking).
 */
class KaitoBaneOfNightmaresScenarioTest : ScenarioTestBase() {

    init {
        // A vanilla creature that declares as the unblocked attacker returned to pay ninjutsu.
        cardRegistry.register(
            CardDefinition.creature(
                name = "Test Attacker",
                manaCost = ManaCost.parse("{1}"),
                subtypes = emptySet(),
                power = 2,
                toughness = 2
            )
        )

        context("Kaito, Bane of Nightmares") {

            test("animation: on your turn with loyalty, Kaito is a 3/4 Ninja creature with hexproof and isn't a planeswalker") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Kaito, Bane of Nightmares")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Kaito, Bane of Nightmares")
                withClue("Casting Kaito should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                val kaito = game.findPermanent("Kaito, Bane of Nightmares")!!
                val projected = game.state.projectedState
                withClue("Kaito is a creature on your turn") { projected.isCreature(kaito) shouldBe true }
                withClue("Kaito stops being a planeswalker while animated") {
                    projected.isPlaneswalker(kaito) shouldBe false
                }
                withClue("Kaito is a 3/4") {
                    projected.getPower(kaito) shouldBe 3
                    projected.getToughness(kaito) shouldBe 4
                }
                withClue("Kaito is a Ninja") {
                    projected.getSubtypes(kaito).any { it.equals("Ninja", ignoreCase = true) } shouldBe true
                }
                withClue("Kaito has hexproof") {
                    projected.hasKeyword(kaito, Keyword.HEXPROOF) shouldBe true
                }
                withClue("Kaito still entered with his starting loyalty") {
                    game.state.getEntity(kaito)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) shouldBe 4
                }
            }

            test("animation: on the opponent's turn Kaito is a planeswalker, not a creature") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Kaito, Bane of Nightmares")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val kaito = game.findPermanent("Kaito, Bane of Nightmares")!!
                // Seed his loyalty counters (withCardOnBattlefield doesn't seed planeswalker loyalty).
                game.state = game.state.updateEntity(kaito) { c ->
                    c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(CounterType.LOYALTY, 4))
                }

                val projected = game.state.projectedState
                withClue("Kaito is not a creature on the opponent's turn") {
                    projected.isCreature(kaito) shouldBe false
                }
                withClue("Kaito is a planeswalker on the opponent's turn") {
                    projected.isPlaneswalker(kaito) shouldBe true
                }
            }

            test("ninjutsu: return an unblocked attacker to put Kaito onto the battlefield tapped and attacking") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Kaito, Bane of Nightmares")
                    .withCardOnBattlefield(1, "Test Attacker", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val attacker = game.findPermanent("Test Attacker")!!
                // Declare the Test Attacker attacking the opponent, then leave it unblocked.
                game.declareAttackers(mapOf("Test Attacker" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers().error shouldBe null
                // Hand priority to the active player so they can cast during the declare-blockers
                // window (CR 509.1 — APNAP gives the active player priority first).
                var guard = 0
                while (game.state.priorityPlayerId != null && game.state.priorityPlayerId != game.player1Id &&
                    game.state.step == Step.DECLARE_BLOCKERS && guard++ < 4
                ) {
                    game.passPriority()
                }

                // Cast Kaito for his ninjutsu cost: pay {1}{U}{B} (auto-tap lands) and return the
                // unblocked attacker to hand.
                val kaitoCardId = game.state.getHand(game.player1Id).first { id ->
                    game.state.getEntity(id)
                        ?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()
                        ?.name == "Kaito, Bane of Nightmares"
                }
                val cast = game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = kaitoCardId,
                        useAlternativeCost = true,
                        alternativeCostType = AlternativeCostType.SNEAK,
                        additionalCostPayment = AdditionalCostPayment(bouncedPermanents = listOf(attacker))
                    )
                )
                withClue("Ninjutsu cast should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                // The returned attacker is back in its owner's hand.
                withClue("The unblocked attacker was returned to hand") {
                    game.findPermanent("Test Attacker") shouldBe null
                    game.state.getHand(game.player1Id).any { id ->
                        game.state.getEntity(id)
                            ?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()
                            ?.name == "Test Attacker"
                    } shouldBe true
                }

                // Kaito entered tapped and attacking the same defender (the opponent), as a creature.
                val kaito = game.findPermanent("Kaito, Bane of Nightmares")
                kaito shouldNotBe null
                kaito!!
                withClue("Kaito entered tapped") {
                    game.state.getEntity(kaito)?.has<TappedComponent>() shouldBe true
                }
                val attacking = game.state.getEntity(kaito)?.get<AttackingComponent>()
                withClue("Kaito entered attacking the opponent") {
                    attacking shouldNotBe null
                    attacking!!.defenderId shouldBe game.player2Id
                }
                withClue("Kaito is a creature as he enters via ninjutsu (so he can be attacking)") {
                    game.state.projectedState.isCreature(kaito) shouldBe true
                }
            }
        }
    }
}
