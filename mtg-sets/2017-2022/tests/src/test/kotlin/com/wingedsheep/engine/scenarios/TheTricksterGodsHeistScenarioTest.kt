package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.SagaComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * The Trickster-God's Heist (KHM #232).
 *
 *   I — You may exchange control of two target creatures.
 *   II — You may exchange control of two target nonbasic, noncreature permanents that share a card type.
 *   III — Target player loses 3 life and you gain 3 life.
 *
 * Pins chapter II's dependent targeting (nonbasic, noncreature, and the second must share a card
 * type with the first — any type, so an artifact land pairs with an artifact), the "you may"
 * consent on the exchanges, and chapter III's drain.
 */
class TheTricksterGodsHeistScenarioTest : ScenarioTestBase() {

    init {
        fun builder() = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "The Trickster-God's Heist")
            .withActivePlayer(1)
            .inPhase(Phase.BEGINNING, Step.UPKEEP)
            .apply { repeat(6) { withCardInLibrary(1, "Island"); withCardInLibrary(2, "Island") } }

        /** Set the Saga one counter short of [chapter] and advance to the main phase so it triggers. */
        fun TestGame.triggerChapter(chapter: Int) {
            val saga = findPermanent("The Trickster-God's Heist")!!
            state = state.updateEntity(saga) { container ->
                container.with(CountersComponent().withAdded(CounterType.LORE, chapter - 1))
                    .with(SagaComponent(triggeredChapters = (1 until chapter).toSet()))
            }
            passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
        }

        fun TestGame.controllerOf(name: String) = state.projectedState.getController(findPermanent(name)!!)

        context("chapter I") {
            test("exchanges control of two creatures when you choose to") {
                val game = builder()
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .build()

                game.triggerChapter(1)
                val decision = game.getPendingDecision()!!
                game.submitDecision(
                    TargetsResponse(
                        decision.id,
                        mapOf(
                            0 to listOf(game.findPermanent("Grizzly Bears")!!),
                            1 to listOf(game.findPermanent("Hill Giant")!!),
                        ),
                    )
                ).error shouldBe null
                game.resolveStack()
                game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                game.answerYesNo(true).error shouldBe null
                game.resolveStack()

                game.controllerOf("Grizzly Bears") shouldBe game.player2Id
                game.controllerOf("Hill Giant") shouldBe game.player1Id
            }

            test("declining leaves both creatures where they were") {
                val game = builder()
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .build()

                game.triggerChapter(1)
                val decision = game.getPendingDecision()!!
                game.submitDecision(
                    TargetsResponse(
                        decision.id,
                        mapOf(
                            0 to listOf(game.findPermanent("Grizzly Bears")!!),
                            1 to listOf(game.findPermanent("Hill Giant")!!),
                        ),
                    )
                ).error shouldBe null
                game.resolveStack()
                game.answerYesNo(false).error shouldBe null
                game.resolveStack()

                game.controllerOf("Grizzly Bears") shouldBe game.player1Id
                game.controllerOf("Hill Giant") shouldBe game.player2Id
            }
        }

        context("chapter II") {
            fun chapterTwoBoard() = builder()
                .withCardOnBattlefield(1, "Mind Stone")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Ancient Den")
                .withCardOnBattlefield(2, "Crusade")
                .withLandsOnBattlefield(2, "Forest", 1)
                .build()

            test("an artifact and an artifact land share a card type and are exchanged") {
                val game = chapterTwoBoard()

                game.triggerChapter(2)
                game.selectTargets(listOf(game.findPermanent("Mind Stone")!!)).error shouldBe null
                game.selectTargets(listOf(game.findPermanent("Ancient Den")!!)).error shouldBe null
                game.resolveStack()
                game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                game.answerYesNo(true).error shouldBe null
                game.resolveStack()

                game.controllerOf("Mind Stone") shouldBe game.player2Id
                game.controllerOf("Ancient Den") shouldBe game.player1Id
            }

            test("only nonbasic, noncreature permanents are offered first, the Saga itself included") {
                val game = chapterTwoBoard()

                game.triggerChapter(2)
                val decision = game.getPendingDecision() as ChooseTargetsDecision
                withClue("Forest is basic; Bears is a creature; Crusade pairs with the Saga, itself an enchantment") {
                    decision.legalTargets[0]!! shouldContainExactlyInAnyOrder listOf(
                        game.findPermanent("Mind Stone")!!,
                        game.findPermanent("Ancient Den")!!,
                        game.findPermanent("Crusade")!!,
                        game.findPermanent("The Trickster-God's Heist")!!,
                    )
                }
                withClue("a basic land is not a legal target") {
                    game.selectTargets(listOf(game.findPermanent("Forest")!!)).error shouldNotBe null
                }
            }

            test("the second target must share a card type with the first") {
                val game = builder()
                    .withCardOnBattlefield(1, "Mind Stone")
                    .withCardOnBattlefield(1, "Bad Moon")
                    .withCardOnBattlefield(2, "Darksteel Ingot")
                    .withCardOnBattlefield(2, "Crusade")
                    .build()

                game.triggerChapter(2)
                game.selectTargets(listOf(game.findPermanent("Mind Stone")!!)).error shouldBe null
                withClue("an enchantment shares no card type with an artifact") {
                    game.selectTargets(listOf(game.findPermanent("Crusade")!!)).error shouldNotBe null
                }
                game.selectTargets(listOf(game.findPermanent("Darksteel Ingot")!!)).error shouldBe null
                game.resolveStack()
                game.answerYesNo(true).error shouldBe null
                game.resolveStack()

                game.controllerOf("Mind Stone") shouldBe game.player2Id
                game.controllerOf("Darksteel Ingot") shouldBe game.player1Id
                game.controllerOf("Crusade") shouldBe game.player2Id
            }

            test("declining the exchange leaves control unchanged") {
                val game = chapterTwoBoard()

                game.triggerChapter(2)
                game.selectTargets(listOf(game.findPermanent("Mind Stone")!!)).error shouldBe null
                game.selectTargets(listOf(game.findPermanent("Ancient Den")!!)).error shouldBe null
                game.resolveStack()
                game.answerYesNo(false).error shouldBe null
                game.resolveStack()

                game.controllerOf("Mind Stone") shouldBe game.player1Id
                game.controllerOf("Ancient Den") shouldBe game.player2Id
            }
        }

        context("chapter III") {
            test("target player loses 3 life and you gain 3 life, then the Saga is sacrificed") {
                val game = builder().build()

                game.triggerChapter(3)
                game.selectTargets(listOf(game.player2Id)).error shouldBe null
                game.resolveStack()

                game.getLifeTotal(1) shouldBe 23
                game.getLifeTotal(2) shouldBe 17
                game.findPermanent("The Trickster-God's Heist") shouldBe null
            }
        }
    }
}
