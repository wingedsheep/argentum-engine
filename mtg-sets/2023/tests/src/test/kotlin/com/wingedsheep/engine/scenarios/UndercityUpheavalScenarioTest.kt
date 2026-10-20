package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Undercity Upheaval (MAT #25) — "Distribute X +1/+1 counters among any number of target creatures
 * you control, where X is the number of creature cards in your graveyard as you cast this spell.
 * Creatures you control gain vigilance until end of turn."
 *
 * X is frozen as the spell is cast (CR 601.2i) and the division is announced at cast (CR 601.2d):
 * a target that turns illegal loses its share instead of passing it on (per the rulings).
 */
class UndercityUpheavalScenarioTest : ScenarioTestBase() {

    init {
        context("Undercity Upheaval") {

            // Three creature cards and one non-creature card in the graveyard: X = 3.
            fun board() = scenario()
                .withPlayers("Alice", "Bob")
                .withCardInHand(1, "Undercity Upheaval")
                .withCardInHand(1, "Shock")
                .withLandsOnBattlefield(1, "Forest", 4)
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardOnBattlefield(1, "Llanowar Elves")
                .withCardOnBattlefield(1, "Tormod's Crypt")
                .withCardOnBattlefield(2, "Centaur Courser")
                .withCardInGraveyard(1, "Grizzly Bears")
                .withCardInGraveyard(1, "Hill Giant")
                .withCardInGraveyard(1, "Ornithopter")
                .withCardInGraveyard(1, "Forest")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            fun TestGame.plusOnes(id: EntityId): Int =
                state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

            fun TestGame.mine(name: String): EntityId = findPermanent(name)!!

            fun TestGame.cast(targets: List<EntityId>, division: Map<EntityId, Int>? = null) = execute(
                CastSpell(
                    player1Id,
                    findCardsInHand(1, "Undercity Upheaval").single(),
                    targets.map { ChosenTarget.Permanent(it) },
                    damageDistribution = division
                )
            )

            fun TestGame.exileOwnGraveyardWithCrypt() {
                val crypt = findPermanent("Tormod's Crypt")!!
                val abilityId = cardRegistry.getCard("Tormod's Crypt")!!.activatedAbilities.single().id
                execute(
                    ActivateAbility(
                        playerId = player1Id,
                        sourceId = crypt,
                        abilityId = abilityId,
                        targets = listOf(ChosenTarget.Player(player1Id)),
                    )
                ).error shouldBe null
            }

            test("announced division of X counters is placed and all its creatures gain vigilance") {
                val game = board()
                val bears = game.mine("Grizzly Bears")
                val giant = game.mine("Hill Giant")
                val elves = game.mine("Llanowar Elves")
                val courser = game.findPermanent("Centaur Courser")!!

                game.cast(listOf(bears, giant), mapOf(bears to 2, giant to 1)).error shouldBe null
                withClue("the stack shows the announced split as counters, not damage") {
                    val spell = game.state.stack.single()
                    val view = stateTransformer.transform(game.state, game.player2Id).cards.getValue(spell)
                    view.damageDistribution shouldBe mapOf(bears to 2, giant to 1)
                    view.damageDistributionUnit shouldBe "+1/+1 counter"
                }
                game.resolveStack()

                game.hasPendingDecision() shouldBe false
                game.plusOnes(bears) shouldBe 2
                game.plusOnes(giant) shouldBe 1
                game.plusOnes(elves) shouldBe 0
                val projected = game.state.projectedState
                for (id in listOf(bears, giant, elves)) projected.hasKeyword(id, Keyword.VIGILANCE) shouldBe true
                withClue("the opponent's creature is unaffected") {
                    projected.hasKeyword(courser, Keyword.VIGILANCE) shouldBe false
                }
            }

            test("the announced division must sum to X and give each target at least one") {
                val game = board()
                val bears = game.mine("Grizzly Bears")
                val giant = game.mine("Hill Giant")
                val elves = game.mine("Llanowar Elves")

                withClue("two counters is not X = 3") {
                    game.cast(listOf(bears, giant), mapOf(bears to 1, giant to 1)).error shouldNotBe null
                }
                withClue("a target given zero counters") {
                    game.cast(listOf(bears, giant), mapOf(bears to 3, giant to 0)).error shouldNotBe null
                }
                withClue("a single target can't be announced more than X") {
                    game.cast(listOf(bears), mapOf(bears to 99)).error shouldNotBe null
                }
                withClue("the non-creature card doesn't count, so one counter each for three targets is exactly X") {
                    game.cast(listOf(bears, giant, elves), mapOf(bears to 1, giant to 1, elves to 1)).error shouldBe null
                }
            }

            test("X is frozen as the spell is cast — exiling the graveyard in response doesn't shrink it") {
                val game = board()
                val bears = game.mine("Grizzly Bears")

                game.cast(listOf(bears)).error shouldBe null
                game.exileOwnGraveyardWithCrypt()
                game.resolveStack()

                game.isInExile(1, "Hill Giant") shouldBe true
                game.plusOnes(bears) shouldBe 3
            }

            test("a target that becomes illegal loses its share; the other keeps exactly its own") {
                val game = board()
                val bears = game.mine("Grizzly Bears")
                val giant = game.mine("Hill Giant")

                game.cast(listOf(bears, giant), mapOf(bears to 2, giant to 1)).error shouldBe null
                game.castSpell(1, "Shock", bears).error shouldBe null
                game.resolveStack()

                game.findPermanents("Grizzly Bears").none { it == bears } shouldBe true
                game.plusOnes(giant) shouldBe 1
                game.state.projectedState.hasKeyword(giant, Keyword.VIGILANCE) shouldBe true
            }

            test("with no creature cards in the graveyard it can't target, but still grants vigilance") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardInHand(1, "Undercity Upheaval")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val bears = game.mine("Grizzly Bears")

                game.cast(listOf(bears)).error shouldNotBe null
                game.cast(emptyList()).error shouldBe null
                game.resolveStack()
                game.plusOnes(bears) shouldBe 0
                game.state.projectedState.hasKeyword(bears, Keyword.VIGILANCE) shouldBe true
            }
        }
    }
}
