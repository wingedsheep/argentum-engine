package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
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
 * Fireball (LEA #149) — {X}{R} Sorcery.
 *
 * "This spell costs {1} more to cast for each target beyond the first.
 *  Fireball deals X damage divided evenly, rounded down, among any number of targets."
 *
 * What these pin, each from its 2017-11-17 ruling:
 *  - the generic per-target tax (X=5 at three targets costs {7}{R}, not {5}{R});
 *  - the zero-target cast is legal and resolves rather than fizzling;
 *  - the divisor is the number of targets still legal *on resolution*, not the number chosen —
 *    and it must be frozen before the per-target loop, where the context holds one target;
 *  - more legal targets than X deals no damage at all.
 */
class FireballScenarioTest : ScenarioTestBase() {

    init {
        context("Fireball") {

            fun TestGame.fireball(x: Int, targets: List<ChosenTarget>) = execute(
                CastSpell(
                    playerId = player1Id,
                    cardId = state.getHand(player1Id).first { id ->
                        state.getEntity(id)?.get<CardComponent>()?.name == "Fireball"
                    },
                    targets = targets,
                    xValue = x
                )
            )

            fun TestGame.damageOn(id: EntityId): Int =
                state.getEntity(id)?.get<DamageComponent>()?.amount ?: 0

            fun board(mountains: Int) = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Fireball")
                .withCardOnBattlefield(2, "Craw Wurm")
                .withCardOnBattlefield(2, "Hill Giant")
                .withLandsOnBattlefield(1, "Mountain", mountains)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

            test("X=5 at three targets costs {7}{R} — seven lands are not enough") {
                val game = board(mountains = 7).build()
                val wurm = game.findPermanent("Craw Wurm")!!
                val giant = game.findPermanent("Hill Giant")!!

                val cast = game.fireball(
                    5,
                    listOf(ChosenTarget.Permanent(wurm), ChosenTarget.Permanent(giant), ChosenTarget.Player(game.player2Id))
                )
                withClue("two extra targets owe {2} on top of {5}{R}") { cast.error.shouldNotBeNull() }
                game.handSize(1) shouldBe 1
            }

            test("X=5 at three targets on eight lands deals 1 to each") {
                val game = board(mountains = 8).build()
                val wurm = game.findPermanent("Craw Wurm")!!
                val giant = game.findPermanent("Hill Giant")!!

                val cast = game.fireball(
                    5,
                    listOf(ChosenTarget.Permanent(wurm), ChosenTarget.Permanent(giant), ChosenTarget.Player(game.player2Id))
                )
                withClue("{7}{R} is payable: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                withClue("floor(5 / 3) = 1 to each target") {
                    game.damageOn(wurm) shouldBe 1
                    game.damageOn(giant) shouldBe 1
                    game.getLifeTotal(2) shouldBe 19
                }
                game.getLifeTotal(1) shouldBe 20
            }

            test("a single target takes the full X at the printed cost") {
                val game = board(mountains = 6).build()

                val cast = game.fireball(5, listOf(ChosenTarget.Player(game.player2Id)))
                withClue("{5}{R} on six lands: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 15
            }

            test("zero targets is a legal cast that resolves and deals no damage") {
                val game = board(mountains = 4).build()
                val wurm = game.findPermanent("Craw Wurm")!!

                val cast = game.fireball(3, emptyList())
                withClue("\"any number of targets\" includes none: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                withClue("resolved to the graveyard, not stuck on the stack") {
                    game.state.stack.isEmpty() shouldBe true
                    game.isInGraveyard(1, "Fireball") shouldBe true
                }
                game.getLifeTotal(1) shouldBe 20
                game.getLifeTotal(2) shouldBe 20
                game.damageOn(wurm) shouldBe 0
            }

            test("a target that becomes illegal is left out of the division") {
                val game = board(mountains = 8)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInHand(1, "Lightning Bolt")
                    .build()
                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!

                // X=4 at three targets: {4}{R} + {2} = 7 lands, leaving one for the Bolt.
                val cast = game.fireball(
                    4,
                    listOf(ChosenTarget.Permanent(bears), ChosenTarget.Permanent(giant), ChosenTarget.Player(game.player2Id))
                )
                withClue("cast: ${cast.error}") { cast.error shouldBe null }
                val bolt = game.castSpell(1, "Lightning Bolt", bears)
                withClue("Bolt in response: ${bolt.error}") { bolt.error shouldBe null }
                game.resolveStack()

                withClue("the Bears died to the Bolt before Fireball resolved") {
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                }
                withClue("two targets still legal: floor(4 / 2) = 2 each, not floor(4 / 3) = 1") {
                    game.damageOn(giant) shouldBe 2
                    game.getLifeTotal(2) shouldBe 18
                }
            }

            test("more legal targets than X deals no damage to any of them") {
                val game = board(mountains = 5).build()
                val giant = game.findPermanent("Hill Giant")!!

                // X=2 at three targets: {2}{R} + {2} = 5 lands.
                val cast = game.fireball(
                    2,
                    listOf(ChosenTarget.Permanent(giant), ChosenTarget.Player(game.player1Id), ChosenTarget.Player(game.player2Id))
                )
                withClue("cast: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                withClue("floor(2 / 3) = 0") {
                    game.damageOn(giant) shouldBe 0
                    game.getLifeTotal(1) shouldBe 20
                    game.getLifeTotal(2) shouldBe 20
                }
                game.isInGraveyard(1, "Fireball") shouldBe true
            }

            test("the enumerated action prices each target count {1} apart beyond the first") {
                val game = board(mountains = 4).build()

                val action = game.getLegalActions(1).firstOrNull { it.description.contains("Fireball") }
                action.shouldNotBeNull()
                val byCount = action.manaCostByTargetCount.shouldNotBeNull()
                val cmc = byCount.mapValues { com.wingedsheep.sdk.core.ManaCost.parse(it.value).cmc }
                cmc.getValue(1) shouldBe cmc.getValue(0)
                cmc.getValue(2) shouldBe cmc.getValue(1) + 1
                cmc.getValue(3) shouldBe cmc.getValue(1) + 2
            }
        }
    }
}
