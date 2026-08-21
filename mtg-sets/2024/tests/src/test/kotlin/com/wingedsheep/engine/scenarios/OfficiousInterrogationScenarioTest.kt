package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Officious Interrogation (MKM #222) — {W}{U} Instant.
 *
 * "This spell costs {W}{U} more to cast for each target beyond the first.
 *  Choose any number of target players. Investigate X times, where X is the total number of
 *  creatures those players control."
 *
 * Two engine additions carry this card, and each has a way of failing silently that these tests are
 * built to catch:
 *
 *  - **the per-target tax** (`CostModification.IncreaseColoredPerUnit` over
 *    `CostReductionSource.ChosenTargetsBeyondTheFirst`). A tax that never applied would look exactly
 *    like a working card in a test that only checks the Clue count, so the price is pinned from both
 *    sides: two targets must be *rejected* on one-target mana, and accepted on two-target mana.
 *  - **the plural target reference** (`Player.EachTargetedPlayer`). `Player.TargetPlayer` resolves to
 *    a *single* targeted player, so a card written with it would still make Clues — just the wrong
 *    number. The asymmetric board (one creature vs two) is what tells the two apart: the total is 3,
 *    which is neither player's own count.
 *
 * The client contract is checked too — the enumerated action must advertise the *one-target minimum*
 * as its `manaCostString` plus the increment as `manaCostPerExtraTarget`, because that pair is what
 * lets the web client defer its mana-source step past targeting instead of under-tapping.
 */
class OfficiousInterrogationScenarioTest : ScenarioTestBase() {

    init {
        context("Officious Interrogation") {

            /** Cast it from player 1's hand at [targetPlayerIds], with no manual payment plan. */
            fun TestGame.interrogate(targetPlayerIds: List<EntityId> = emptyList()) = execute(
                CastSpell(
                    playerId = player1Id,
                    cardId = state.getHand(player1Id).first { id ->
                        state.getEntity(id)?.get<CardComponent>()?.name == "Officious Interrogation"
                    },
                    targets = targetPlayerIds.map { ChosenTarget.Player(it) }
                )
            )

            test("one target costs the printed {W}{U} and counts that player's creatures") {
                val game = scenario()
                    .withPlayers("Detective", "Suspect")
                    .withCardInHand(1, "Officious Interrogation")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Savannah Lions")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.interrogate(listOf(game.player2Id))
                withClue("a single target owes nothing extra: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()

                withClue("two creatures under the one target → two Clues") {
                    game.findPermanents("Clue").size shouldBe 2
                }
            }

            test("two targets are rejected on one-target mana") {
                val game = scenario()
                    .withPlayers("Detective", "Suspect")
                    .withCardInHand(1, "Officious Interrogation")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.interrogate(listOf(game.player1Id, game.player2Id))
                withClue("the second target owes another {W}{U}, which this board can't pay") {
                    cast.error.shouldNotBeNull()
                }
                withClue("a rejected cast leaves the spell in hand") {
                    game.handSize(1) shouldBe 1
                }
            }

            test("two targets on two-target mana investigate for the combined total") {
                val game = scenario()
                    .withPlayers("Detective", "Suspect")
                    .withCardInHand(1, "Officious Interrogation")
                    // Asymmetric on purpose: 1 + 2 = 3 is neither player's own count, so a
                    // single-player reference could not produce it.
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Savannah Lions")
                    .withCardOnBattlefield(2, "Centaur Courser")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.interrogate(listOf(game.player1Id, game.player2Id))
                withClue("{W}{U}{W}{U} is payable here: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                withClue("the total across BOTH targeted players, not either one alone") {
                    game.findPermanents("Clue").size shouldBe 3
                }
                withClue("the Clues belong to the caster, not the targeted players") {
                    game.findPermanents("Clue").all { clue ->
                        game.state.projectedState.getController(clue) == game.player1Id
                    } shouldBe true
                }
            }

            test("no targets is a legal cast that investigates zero times") {
                val game = scenario()
                    .withPlayers("Detective", "Suspect")
                    .withCardInHand(1, "Officious Interrogation")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.interrogate()
                withClue("\"any number of target players\" includes none: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()

                withClue("nobody targeted, nothing counted") {
                    game.findPermanents("Clue").size shouldBe 0
                }
            }

            test("the enumerated action advertises the one-target minimum plus the increment") {
                val game = scenario()
                    .withPlayers("Detective", "Suspect")
                    .withCardInHand(1, "Officious Interrogation")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val action = game.getLegalActions(1)
                    .firstOrNull { it.description.contains("Officious Interrogation") }
                action.shouldNotBeNull()

                withClue("enumeration prices it with no targets, i.e. the cheapest legal cast") {
                    action.manaCostString shouldBe "{W}{U}"
                }
                withClue("the client needs the increment to price its own mana-source step") {
                    action.manaCostPerExtraTarget shouldBe "{W}{U}"
                }
            }
        }
    }
}
