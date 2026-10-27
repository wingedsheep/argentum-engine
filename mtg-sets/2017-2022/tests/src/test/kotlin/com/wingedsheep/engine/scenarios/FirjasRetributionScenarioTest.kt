package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.SagaComponent
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
 * Firja's Retribution (KHM #210).
 *
 *   II — Until end of turn, Angels you control gain "{T}: Destroy target creature with power less
 *        than this creature's power."
 *   III — Angels you control gain double strike until end of turn.
 *
 * Pins the granted ability's self-relative target restriction: "this creature" is the Angel the
 * ability was granted to, and "less than" is strict. Only Angels you control get the grants.
 */
class FirjasRetributionScenarioTest : ScenarioTestBase() {

    init {
        fun builder() = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Firja's Retribution")
            .withCardOnBattlefield(1, "Serra Angel")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withActivePlayer(1)
            .inPhase(Phase.BEGINNING, Step.UPKEEP)
            .apply { repeat(4) { withCardInLibrary(1, "Plains"); withCardInLibrary(2, "Plains") } }

        /** Set the Saga one counter short of [chapter] and advance to the main phase so it triggers. */
        fun TestGame.playChapter(chapter: Int) {
            val saga = findPermanent("Firja's Retribution")!!
            state = state.updateEntity(saga) { container ->
                container.with(CountersComponent().withAdded(CounterType.LORE, chapter - 1))
                    .with(SagaComponent(triggeredChapters = (1 until chapter).toSet()))
            }
            passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            resolveStack()
        }

        fun TestGame.grantsOn(id: EntityId) = state.grantedActivatedAbilities.filter { it.entityId == id }

        context("chapter II") {
            test("an Angel taps to destroy a creature with less power") {
                val game = builder()
                    .withCardOnBattlefield(2, "Hill Giant")
                    .build()
                val angel = game.findPermanent("Serra Angel")!!
                val giant = game.findPermanent("Hill Giant")!!

                game.playChapter(2)

                val grant = game.grantsOn(angel).single()
                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = angel,
                        abilityId = grant.ability.id,
                        targets = listOf(ChosenTarget.Permanent(giant)),
                    )
                ).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(2, "Hill Giant") shouldBe true
            }

            test("a creature with equal power is not a legal target") {
                val game = builder()
                    .withCardOnBattlefield(2, "Serra Angel")
                    .build()
                val angel = game.findAllPermanents("Serra Angel").first { game.state.projectedState.getController(it) == game.player1Id }
                val theirAngel = game.findAllPermanents("Serra Angel").first { it != angel }

                game.playChapter(2)

                val grant = game.grantsOn(angel).single()
                withClue("4 is not less than 4") {
                    game.execute(
                        ActivateAbility(
                            playerId = game.player1Id,
                            sourceId = angel,
                            abilityId = grant.ability.id,
                            targets = listOf(ChosenTarget.Permanent(theirAngel)),
                        )
                    ).error shouldNotBe null
                }
                withClue("the opposing Angel is untouched") { game.findAllPermanents("Serra Angel").size shouldBe 2 }
            }

            test("only Angels you control gain the ability") {
                val game = builder()
                    .withCardOnBattlefield(2, "Serra Angel")
                    .build()
                val angels = game.findAllPermanents("Serra Angel")
                val mine = angels.first { game.state.projectedState.getController(it) == game.player1Id }
                val theirs = angels.first { it != mine }
                val bears = game.findPermanent("Grizzly Bears")!!

                game.playChapter(2)

                game.grantsOn(mine).size shouldBe 1
                withClue("the opponent's Angel gets nothing") { game.grantsOn(theirs).size shouldBe 0 }
                withClue("a non-Angel gets nothing") { game.grantsOn(bears).size shouldBe 0 }
            }
        }

        test("chapter III gives your Angels double strike, and nothing else") {
            val game = builder().build()
            val angel = game.findPermanent("Serra Angel")!!
            val bears = game.findPermanent("Grizzly Bears")!!

            game.playChapter(3)

            game.state.projectedState.hasKeyword(angel, Keyword.DOUBLE_STRIKE) shouldBe true
            game.state.projectedState.hasKeyword(bears, Keyword.DOUBLE_STRIKE) shouldBe false
            withClue("the Saga is sacrificed after chapter III") {
                game.findPermanent("Firja's Retribution") shouldBe null
            }
        }
    }
}
