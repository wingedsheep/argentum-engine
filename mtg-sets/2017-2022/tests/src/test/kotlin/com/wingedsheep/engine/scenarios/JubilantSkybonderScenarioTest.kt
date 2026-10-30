package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Jubilant Skybonder (IKO #223; {1}{W/U}{W/U}, 2/2).
 *
 *   Flying
 *   Creatures you control with flying have "Spells your opponents cast that target this creature
 *   cost {2} more to cast."
 *
 * Each flyer holds its own copy of the granted tax, so a spell targeting two of them costs {4} more.
 */
class JubilantSkybonderScenarioTest : ScenarioTestBase() {

    init {
        context("Jubilant Skybonder") {

            test("an opponent's spell targeting the Skybonder itself costs {2} more") {
                val short = boltBoard(mountains = 2)
                withClue("{R} + {2} tax can't be paid with two Mountains") {
                    short.castSpell(1, "Lightning Bolt", short.findPermanent("Jubilant Skybonder")!!).error shouldNotBe null
                }

                val paid = boltBoard(mountains = 3)
                paid.castSpell(1, "Lightning Bolt", paid.findPermanent("Jubilant Skybonder")!!).error shouldBe null
                paid.resolveStack()
                paid.isInGraveyard(2, "Jubilant Skybonder") shouldBe true
            }

            test("another flyer you control is taxed too, a creature without flying is not") {
                val drake = boltBoard(mountains = 2)
                withClue("Wind Drake has flying, so it has the granted tax") {
                    drake.castSpell(1, "Lightning Bolt", drake.findPermanent("Wind Drake")!!).error shouldNotBe null
                }

                val bears = boltBoard(mountains = 1)
                bears.castSpell(1, "Lightning Bolt", bears.findPermanent("Grizzly Bears")!!).error shouldBe null
                bears.resolveStack()
                bears.isInGraveyard(2, "Grizzly Bears") shouldBe true
            }

            test("a spell targeting two flyers pays each one's tax — {4} more") {
                val short = seedsBoard(extraForests = 3)
                withClue("{G}{W} + {4} can't be paid with five lands") {
                    short.castSeedsAtAllThree().error shouldNotBe null
                }

                val paid = seedsBoard(extraForests = 4)
                paid.castSeedsAtAllThree().error shouldBe null
            }

            test("the Skybonder's controller casts spells at their own flyers untaxed") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Jubilant Skybonder")
                    .withCardInHand(1, "Giant Growth")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Giant Growth", game.findPermanent("Jubilant Skybonder")!!).error shouldBe null
            }

            test("the grant ends when the Skybonder leaves the battlefield") {
                val game = boltBoard(mountains = 2, skybonder = false)
                game.castSpell(1, "Lightning Bolt", game.findPermanent("Wind Drake")!!).error shouldBe null
            }
        }
    }

    private fun boltBoard(mountains: Int, skybonder: Boolean = true): TestGame {
        val builder = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(2, "Wind Drake")
            .withCardOnBattlefield(2, "Grizzly Bears")
            .withCardInHand(1, "Lightning Bolt")
            .withLandsOnBattlefield(1, "Mountain", mountains)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        if (skybonder) builder.withCardOnBattlefield(2, "Jubilant Skybonder")
        return builder.build()
    }

    private fun seedsBoard(extraForests: Int): TestGame =
        scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(2, "Jubilant Skybonder")
            .withCardOnBattlefield(2, "Wind Drake")
            .withCardOnBattlefield(2, "Grizzly Bears")
            .withCardInHand(1, "Seeds of Strength")
            .withLandsOnBattlefield(1, "Plains", 1)
            .withLandsOnBattlefield(1, "Forest", 1 + extraForests)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

    /** Seeds of Strength at the Skybonder, Wind Drake and Grizzly Bears: two taxed flyers, one untaxed creature. */
    private fun TestGame.castSeedsAtAllThree() = execute(
        CastSpell(
            player1Id,
            state.getHand(player1Id).first { state.getEntity(it)?.get<CardComponent>()?.name == "Seeds of Strength" },
            listOf("Jubilant Skybonder", "Wind Drake", "Grizzly Bears").map { ChosenTarget.Permanent(findPermanent(it)!!) },
        )
    )
}
