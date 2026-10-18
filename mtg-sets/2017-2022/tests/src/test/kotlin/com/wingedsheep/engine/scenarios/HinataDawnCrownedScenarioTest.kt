package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Hinata, Dawn-Crowned (NEO #222) — {1}{U}{R}{W} Legendary Creature — Kirin Spirit 4/4.
 *
 * "Flying, trample. Spells you cast cost {1} less to cast for each target. Spells your opponents
 *  cast cost {1} more to cast for each target."
 *
 * Each price is pinned from both sides — the cast that must fit, and the one a land short that must
 * be rejected — because a modifier that never applied would pass a test that only checks success.
 * The ruling's "different targets, not uses of the word" is pinned with Seeds of Strength, whose
 * three "target creature" phrases may all name one creature. Enumeration is checked too: the offered
 * cost is the cheapest reachable target count, and the per-count table the client prices a manual
 * mana pick from.
 */
class HinataDawnCrownedScenarioTest : ScenarioTestBase() {

    init {
        context("Hinata, Dawn-Crowned") {

            fun TestGame.cast(playerNumber: Int, name: String, targets: List<ChosenTarget>) = execute(
                CastSpell(
                    playerId = if (playerNumber == 1) player1Id else player2Id,
                    cardId = state.getHand(if (playerNumber == 1) player1Id else player2Id).first { id ->
                        state.getEntity(id)?.get<CardComponent>()?.name == name
                    },
                    targets = targets,
                )
            )

            fun TestGame.player(id: EntityId) = listOf(ChosenTarget.Player(id))

            test("your one-target spell costs {1} less") {
                val game = scenario()
                    .withPlayers("Hinata", "Opponent")
                    .withCardOnBattlefield(1, "Hinata, Dawn-Crowned")
                    .withCardInHand(1, "Lightning Strike")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.cast(1, "Lightning Strike", game.player(game.player2Id))
                withClue("{1}{R} less {1} for its one target is {R}: ${cast.error}") { cast.error.shouldBeNull() }
                game.resolveStack()
                game.getLifeTotal(2) shouldBe 17
            }

            test("your discount is generic-only, so it can't touch a spell's colored pips") {
                fun board(plains: Int) = scenario()
                    .withPlayers("Hinata", "Opponent")
                    .withCardOnBattlefield(1, "Hinata, Dawn-Crowned")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Savannah Lions")
                    .withCardOnBattlefield(2, "Centaur Courser")
                    .withCardInHand(1, "Seeds of Strength")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withLandsOnBattlefield(1, "Plains", plains)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                fun TestGame.threeCreatures() = listOf("Grizzly Bears", "Savannah Lions", "Centaur Courser")
                    .map { ChosenTarget.Permanent(findPermanent(it)!!) }

                val short = board(0)
                withClue("three targets take {3} off {G}{W}, which has no generic to lose") {
                    short.cast(1, "Seeds of Strength", short.threeCreatures()).error.shouldNotBeNull()
                }
                val paid = board(1)
                val cast = paid.cast(1, "Seeds of Strength", paid.threeCreatures())
                withClue("{G}{W} is still owed in full: ${cast.error}") { cast.error.shouldBeNull() }
            }

            test("your spell with no target costs full price") {
                val game = scenario()
                    .withPlayers("Hinata", "Opponent")
                    .withCardOnBattlefield(1, "Hinata, Dawn-Crowned")
                    .withCardInHand(1, "Divination")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                withClue("Divination {2}{U} has no target to discount") {
                    game.cast(1, "Divination", emptyList()).error.shouldNotBeNull()
                }
            }

            test("an opponent's one-target spell costs {1} more") {
                fun board(mountains: Int) = scenario()
                    .withPlayers("Hinata", "Opponent")
                    .withCardOnBattlefield(1, "Hinata, Dawn-Crowned")
                    .withCardInHand(2, "Lightning Strike")
                    .withLandsOnBattlefield(2, "Mountain", mountains)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val short = board(2)
                withClue("{1}{R} plus {1} is three mana") {
                    short.cast(2, "Lightning Strike", short.player(short.player1Id)).error.shouldNotBeNull()
                }
                val paid = board(3)
                val cast = paid.cast(2, "Lightning Strike", paid.player(paid.player1Id))
                withClue("three Mountains pay the tax: ${cast.error}") { cast.error.shouldBeNull() }
            }

            test("one creature named by three target phrases is one target") {
                fun board(lands: Int) = scenario()
                    .withPlayers("Hinata", "Opponent")
                    .withCardOnBattlefield(1, "Hinata, Dawn-Crowned")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Savannah Lions")
                    .withCardInHand(2, "Seeds of Strength")
                    .withLandsOnBattlefield(2, "Forest", 1)
                    .withLandsOnBattlefield(2, "Plains", lands - 1)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val same = board(3)
                val bears = same.findPermanent("Grizzly Bears")!!
                val cast = same.cast(2, "Seeds of Strength", List(3) { ChosenTarget.Permanent(bears) })
                withClue("three phrases, one creature: {G}{W} plus {1}: ${cast.error}") { cast.error.shouldBeNull() }

                val split = board(3)
                val twoCreatures = listOf(
                    split.findPermanent("Grizzly Bears")!!, split.findPermanent("Grizzly Bears")!!,
                    split.findPermanent("Savannah Lions")!!,
                ).map { ChosenTarget.Permanent(it) }
                withClue("two different creatures are two targets, owing {2} more") {
                    split.cast(2, "Seeds of Strength", twoCreatures).error.shouldNotBeNull()
                }
            }

            test("enumeration offers the discounted cast and prices each target count") {
                val game = scenario()
                    .withPlayers("Hinata", "Opponent")
                    .withCardOnBattlefield(1, "Hinata, Dawn-Crowned")
                    .withCardInHand(1, "Lightning Strike")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val action = game.getLegalActions(1).firstOrNull { it.description.contains("Lightning Strike") }
                withClue("one Mountain casts it once Hinata's discount for its one target is counted") {
                    action.shouldNotBeNull()
                    action.isAffordable shouldBe true
                }
                action!!.manaCostString shouldBe "{R}"
                val byCount = action.manaCostByTargetCount.shouldNotBeNull()
                byCount.keys shouldBe setOf(1)
                ManaCost.parse(byCount.getValue(1)) shouldBe ManaCost.parse("{R}")
            }

            test("enumeration withholds an opponent's targeted spell the tax makes unaffordable") {
                val game = scenario()
                    .withPlayers("Hinata", "Opponent")
                    .withCardOnBattlefield(1, "Hinata, Dawn-Crowned")
                    .withCardInHand(2, "Lightning Strike")
                    .withLandsOnBattlefield(2, "Mountain", 2)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val strike = game.getLegalActions(2).firstOrNull { it.description.contains("Lightning Strike") }
                withClue("its one required target owes {1} more than two Mountains make") {
                    (strike == null || !strike.isAffordable) shouldBe true
                }
                strike?.manaCostString?.let { it shouldBe "{2}{R}" }
            }
        }
    }
}
