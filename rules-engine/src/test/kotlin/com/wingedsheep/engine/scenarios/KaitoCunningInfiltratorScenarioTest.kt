package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.fdn.cards.KaitoCunningInfiltrator
import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.engine.core.SelectCardsDecision
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Kaito, Cunning Infiltrator (FDN, {1}{U}{U}, Loyalty 3).
 *
 *   Whenever a creature you control deals combat damage to a player, put a loyalty counter on Kaito.
 *   +1: Up to one target creature you control can't be blocked this turn. Draw a card, then discard a card.
 *   −2: Create a 2/1 blue Ninja creature token.
 *   −9: You get an emblem with "Whenever a player casts a spell, you create a 2/1 blue Ninja creature token."
 *
 * Covers the passive loyalty trigger (and that it ignores creatures you *don't* control), the +1's
 * genuinely-optional target (both with and without a target chosen — the loot happens either way),
 * the −2 token's exact characteristics, and the −9 emblem: it outlives Kaito dying to the 0-loyalty
 * state-based action and fires for spells cast by *either* player.
 */
class KaitoCunningInfiltratorScenarioTest : ScenarioTestBase() {

    private val plusOne = KaitoCunningInfiltrator.activatedAbilities[0].id
    private val minusTwo = KaitoCunningInfiltrator.activatedAbilities[1].id
    private val minusNine = KaitoCunningInfiltrator.activatedAbilities[2].id

    init {
        context("Kaito, Cunning Infiltrator") {

            test("a creature you control connecting puts a loyalty counter on Kaito") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Kaito, Cunning Infiltrator")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val kaito = game.findPermanent("Kaito, Cunning Infiltrator")!!
                // The scenario builder drops permanents straight onto the battlefield without
                // running the "enters with its starting loyalty" step, so seed it explicitly.
                game.state = game.state.updateEntity(kaito) { c ->
                    c.with(CountersComponent().withAdded(CounterType.LOYALTY, 3))
                }
                loyalty(game, kaito) shouldBe 3

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)

                withClue("Bears dealt 2 combat damage to the opponent") {
                    game.getLifeTotal(2) shouldBe 18
                }
                withClue("the passive trigger added one loyalty counter") {
                    loyalty(game, kaito) shouldBe 4
                }
            }

            test("a creature an opponent controls connecting does NOT bump Kaito's loyalty") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Kaito, Cunning Infiltrator")
                    .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val kaito = game.findPermanent("Kaito, Cunning Infiltrator")!!
                // The scenario builder drops permanents straight onto the battlefield without
                // running the "enters with its starting loyalty" step, so seed it explicitly.
                game.state = game.state.updateEntity(kaito) { c ->
                    c.with(CountersComponent().withAdded(CounterType.LOYALTY, 3))
                }

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                // Attack the player, not Kaito, so no loyalty is removed by combat damage either.
                game.declareAttackers(mapOf("Grizzly Bears" to 1)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)

                withClue("the opponent's Bears connected") {
                    game.getLifeTotal(1) shouldBe 18
                }
                withClue("'a creature you control' filtered out the opponent's attacker") {
                    loyalty(game, kaito) shouldBe 3
                }
            }

            test("+1 makes the target unblockable and loots") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Kaito, Cunning Infiltrator")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(1, "Hill Giant")
                    .withCardInLibrary(1, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val kaito = game.findPermanent("Kaito, Cunning Infiltrator")!!
                // The scenario builder drops permanents straight onto the battlefield without
                // running the "enters with its starting loyalty" step, so seed it explicitly.
                game.state = game.state.updateEntity(kaito) { c ->
                    c.with(CountersComponent().withAdded(CounterType.LOYALTY, 3))
                }
                val bears = game.findPermanent("Grizzly Bears")!!

                withClue("Bears is blockable before the ability") {
                    game.state.projectedState.hasKeyword(bears, AbilityFlag.CANT_BE_BLOCKED) shouldBe false
                }

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = kaito,
                        abilityId = plusOne,
                        targets = listOf(ChosenTarget.Permanent(bears)),
                    )
                ).error shouldBe null
                game.resolveStack()

                // Draw a card, then discard a card — the discard asks which card to pitch.
                val discard = game.getPendingDecision()
                withClue("resolution paused on the loot's discard: $discard") {
                    (discard is SelectCardsDecision) shouldBe true
                }
                game.selectCards(listOf((discard as SelectCardsDecision).options.first())).error shouldBe null
                game.resolveStack()

                withClue("target can't be blocked this turn") {
                    game.state.projectedState.hasKeyword(bears, AbilityFlag.CANT_BE_BLOCKED) shouldBe true
                }
                withClue("drew one and discarded one: hand size unchanged, graveyard grew") {
                    game.handSize(1) shouldBe 1
                    game.graveyardSize(1) shouldBe 1
                    game.librarySize(1) shouldBe 0
                }
                withClue("+1 paid a loyalty counter onto Kaito") {
                    loyalty(game, kaito) shouldBe 4
                }
            }

            test("+1 with no target chosen still loots") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Kaito, Cunning Infiltrator")
                    .withCardInHand(1, "Hill Giant")
                    .withCardInLibrary(1, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val kaito = game.findPermanent("Kaito, Cunning Infiltrator")!!
                // The scenario builder drops permanents straight onto the battlefield without
                // running the "enters with its starting loyalty" step, so seed it explicitly.
                game.state = game.state.updateEntity(kaito) { c ->
                    c.with(CountersComponent().withAdded(CounterType.LOYALTY, 3))
                }

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = kaito,
                        abilityId = plusOne,
                        targets = emptyList(),
                    )
                ).error shouldBe null
                game.resolveStack()

                val discard = game.getPendingDecision()
                withClue("'up to one' with zero targets still reaches the loot: $discard") {
                    (discard is SelectCardsDecision) shouldBe true
                }
                game.selectCards(listOf((discard as SelectCardsDecision).options.first())).error shouldBe null
                game.resolveStack()

                withClue("drew one and discarded one") {
                    game.handSize(1) shouldBe 1
                    game.graveyardSize(1) shouldBe 1
                }
                loyalty(game, kaito) shouldBe 4
            }

            test("−2 creates a 2/1 blue Ninja token") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Kaito, Cunning Infiltrator")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val kaito = game.findPermanent("Kaito, Cunning Infiltrator")!!
                // The scenario builder drops permanents straight onto the battlefield without
                // running the "enters with its starting loyalty" step, so seed it explicitly.
                game.state = game.state.updateEntity(kaito) { c ->
                    c.with(CountersComponent().withAdded(CounterType.LOYALTY, 3))
                }

                game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = kaito, abilityId = minusTwo)
                ).error shouldBe null
                game.resolveStack()

                val token = game.findPermanent("Ninja Token")
                withClue("the −2 created a token") { token shouldNotBe null }
                val projected = game.state.projectedState
                withClue("2/1 blue Ninja") {
                    projected.getProjectedValues(token!!)?.power shouldBe 2
                    projected.getProjectedValues(token)?.toughness shouldBe 1
                    projected.isCreature(token) shouldBe true
                }
                withClue("−2 removed two loyalty counters") {
                    loyalty(game, kaito) shouldBe 1
                }
            }

            test("−9 emblem outlives Kaito and makes a Ninja whenever any player casts a spell") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Kaito, Cunning Infiltrator")
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardInHand(2, "Hill Giant")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withLandsOnBattlefield(2, "Mountain", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val kaito = game.findPermanent("Kaito, Cunning Infiltrator")!!
                game.state = game.state.updateEntity(kaito) { c ->
                    c.with(CountersComponent().withAdded(CounterType.LOYALTY, 9))
                }

                game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = kaito, abilityId = minusNine)
                ).error shouldBe null
                game.resolveStack()

                withClue("Kaito hit 0 loyalty and died, but the emblem is a global grant") {
                    game.findPermanent("Kaito, Cunning Infiltrator") shouldBe null
                    game.state.globalGrantedTriggeredAbilities.size shouldBe 1
                }

                // Your own spell triggers it.
                game.castSpell(1, "Grizzly Bears").error shouldBe null
                game.resolveStack()
                withClue("you cast a spell → one Ninja token") {
                    game.findPermanents("Ninja Token").size shouldBe 1
                }

                // So does an opponent's spell ("whenever a player casts a spell").
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                game.state = game.state.copy(activePlayerId = game.player2Id, priorityPlayerId = game.player2Id)
                game.castSpell(2, "Hill Giant").error shouldBe null
                game.resolveStack()
                withClue("the opponent casting a spell also makes you a Ninja") {
                    game.findPermanents("Ninja Token").size shouldBe 2
                }
                withClue("the emblem's tokens are yours, not the caster's") {
                    game.findPermanents("Ninja Token").forEach { token ->
                        controllerOf(game, token) shouldBe game.player1Id
                    }
                }
            }
        }
    }

    private fun loyalty(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

    private fun controllerOf(game: TestGame, id: EntityId): EntityId? =
        game.state.getEntity(id)
            ?.get<ControllerComponent>()
            ?.playerId
}
