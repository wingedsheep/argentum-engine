package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Shatterskull Smashing (ZNR #161, {X}{R}{R}, Sorcery // Land).
 *
 *   Shatterskull Smashing deals X damage divided as you choose among up to two target creatures
 *   and/or planeswalkers. If X is 6 or more, Shatterskull Smashing deals twice X damage divided as
 *   you choose among them instead.
 *
 * The division is announced with X at cast (CR 601.2d), so the split must total X — or twice X once
 * X reaches 6 — and each of two targets must get at least 1.
 */
class ShatterskullSmashingScenarioTest : ScenarioTestBase() {

    private fun board(mountains: Int) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Shatterskull Smashing")
        .withCardOnBattlefield(2, "Craw Wurm") // 6/4
        .withCardOnBattlefield(2, "Hill Giant") // 3/3
        .withLandsOnBattlefield(1, "Mountain", mountains)
        .withActivePlayer(1)
        .withPriorityPlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun damageOn(game: TestGame, id: com.wingedsheep.sdk.model.EntityId) =
        game.state.getEntity(id)?.get<DamageComponent>()?.amount ?: 0

    init {
        context("Shatterskull Smashing") {

            test("X below 6 divides X damage between the two targets") {
                val game = board(mountains = 5)
                val spell = game.findCardsInHand(1, "Shatterskull Smashing").single()
                val wurm = game.findPermanent("Craw Wurm")!!
                val giant = game.findPermanent("Hill Giant")!!

                game.execute(
                    CastSpell(
                        game.player1Id, spell,
                        targets = listOf(ChosenTarget.Permanent(wurm), ChosenTarget.Permanent(giant)),
                        xValue = 3,
                        damageDistribution = mapOf(wurm to 1, giant to 2),
                    )
                ).error shouldBe null
                game.resolveStack()

                damageOn(game, wurm) shouldBe 1
                damageOn(game, giant) shouldBe 2
            }

            test("X of 6 or more divides twice X damage") {
                val game = board(mountains = 8)
                val spell = game.findCardsInHand(1, "Shatterskull Smashing").single()
                val wurm = game.findPermanent("Craw Wurm")!!
                val giant = game.findPermanent("Hill Giant")!!

                withClue("a split totalling X, not twice X, is rejected") {
                    game.execute(
                        CastSpell(
                            game.player1Id, spell,
                            targets = listOf(ChosenTarget.Permanent(wurm), ChosenTarget.Permanent(giant)),
                            xValue = 6,
                            damageDistribution = mapOf(wurm to 4, giant to 2),
                        )
                    ).error shouldNotBe null
                }

                game.execute(
                    CastSpell(
                        game.player1Id, spell,
                        targets = listOf(ChosenTarget.Permanent(wurm), ChosenTarget.Permanent(giant)),
                        xValue = 6,
                        damageDistribution = mapOf(wurm to 9, giant to 3),
                    )
                ).error shouldBe null
                game.resolveStack()

                withClue("12 damage kills both") {
                    game.findPermanent("Craw Wurm") shouldBe null
                    game.findPermanent("Hill Giant") shouldBe null
                }
            }

            test("a single target takes the whole total without a split") {
                val game = board(mountains = 8)
                val spell = game.findCardsInHand(1, "Shatterskull Smashing").single()
                val giant = game.findPermanent("Hill Giant")!!

                game.execute(
                    CastSpell(game.player1Id, spell, targets = listOf(ChosenTarget.Permanent(giant)), xValue = 2)
                ).error shouldBe null
                game.resolveStack()

                damageOn(game, giant) shouldBe 2
            }

            test("X of 1 can't be divided between two targets") {
                val game = board(mountains = 3)
                val spell = game.findCardsInHand(1, "Shatterskull Smashing").single()
                val wurm = game.findPermanent("Craw Wurm")!!
                val giant = game.findPermanent("Hill Giant")!!

                game.execute(
                    CastSpell(
                        game.player1Id, spell,
                        targets = listOf(ChosenTarget.Permanent(wurm), ChosenTarget.Permanent(giant)),
                        xValue = 1,
                        damageDistribution = mapOf(wurm to 1, giant to 0),
                    )
                ).error shouldNotBe null
            }
        }
    }
}
