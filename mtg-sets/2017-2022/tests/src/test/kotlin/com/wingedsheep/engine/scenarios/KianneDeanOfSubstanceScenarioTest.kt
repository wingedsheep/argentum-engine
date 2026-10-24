package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.stx.cards.KianneDeanOfSubstance
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Kianne, Dean of Substance // Imbraham, Dean of Theory (STX #152) — a modal DFC with two creature
 * faces sharing the study counter, a mark on cards in exile.
 *
 * Kianne's {T} exiles the top card and sends a land on to your hand, marking anything else; her
 * {4}{G} Fractal counts the *different* mana values among nonland study cards you own in exile.
 * Imbraham's {X}{U}{U}, {T} marks the top X as it exiles them, then offers any study card you own
 * in exile — and still offers one at X = 0 (ruling).
 */
class KianneDeanOfSubstanceScenarioTest : ScenarioTestBase() {

    private val bearA = card("Kianne Test Bear A") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }
    private val bearB = card("Kianne Test Bear B") {
        manaCost = "{G}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }
    private val giant = card("Kianne Test Giant") {
        manaCost = "{3}{G}"
        typeLine = "Creature — Giant"
        power = 4
        toughness = 4
    }
    private val unmarked = card("Kianne Test Wurm") {
        manaCost = "{6}{G}"
        typeLine = "Creature — Wurm"
        power = 6
        toughness = 6
    }

    init {
        listOf(bearA, bearB, giant, unmarked).forEach { cardRegistry.register(it) }

        fun TestGame.exiledNamed(name: String): EntityId =
            state.getExile(player1Id).first { state.getEntity(it)!!.get<CardComponent>()!!.name == name }

        fun TestGame.studyCounters(id: EntityId): Int =
            state.getEntity(id)!!.get<CountersComponent>()?.getCount(CounterType.STUDY) ?: 0

        fun TestGame.activate(face: String, abilityIndex: Int, xValue: Int? = null) {
            val source = findPermanent(face)!!
            val definition = if (face.startsWith("Imbraham")) KianneDeanOfSubstance.backFace!! else KianneDeanOfSubstance
            val abilityId = definition.activatedAbilities[abilityIndex].id
            execute(ActivateAbility(player1Id, source, abilityId, xValue = xValue)).error shouldBe null
            resolveStack()
        }

        context("Kianne — {T}: exile the top card") {

            test("a nonland card stays in exile with a study counter") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Kianne, Dean of Substance")
                    .withCardInLibrary(1, "Kianne Test Bear A")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.activate("Kianne, Dean of Substance", 0)

                val bear = game.exiledNamed("Kianne Test Bear A")
                withClue("the exiled bear carries one study counter") {
                    game.studyCounters(bear) shouldBe 1
                }
                game.isInHand(1, "Kianne Test Bear A") shouldBe false
            }

            test("a land card is put into your hand, without a counter") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Kianne, Dean of Substance")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Kianne Test Bear A")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.activate("Kianne, Dean of Substance", 0)

                game.isInHand(1, "Forest") shouldBe true
                game.isInExile(1, "Forest") shouldBe false
                withClue("the next card is untouched") {
                    game.isInExile(1, "Kianne Test Bear A") shouldBe false
                }
                val inHand = game.state.getHand(game.player1Id).first {
                    game.state.getEntity(it)!!.get<CardComponent>()!!.name == "Forest"
                }
                game.studyCounters(inHand) shouldBe 0
            }
        }

        context("Imbraham and Kianne's Fractal") {

            fun deansBoard() = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Kianne, Dean of Substance")
                .withCardOnBattlefield(1, "Imbraham, Dean of Theory")
                .withLandsOnBattlefield(1, "Island", 6)
                .withLandsOnBattlefield(1, "Forest", 5)
                // Top → bottom: MV 2, MV 2, MV 4, a land, then one card that stays.
                .withCardInLibrary(1, "Kianne Test Bear A")
                .withCardInLibrary(1, "Kianne Test Bear B")
                .withCardInLibrary(1, "Kianne Test Giant")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Island")
                // Exiled without a study counter: never counted, never offered.
                .withCardInExile(1, "Kianne Test Wurm")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            test("Imbraham's X ability is offered as a legal action") {
                val game = deansBoard()
                val imbraham = game.findPermanent("Imbraham, Dean of Theory")!!
                val abilityId = KianneDeanOfSubstance.backFace!!.activatedAbilities[0].id
                val offered = game.getLegalActions(1).map { it.action }
                    .filterIsInstance<ActivateAbility>()
                    .filter { it.sourceId == imbraham }
                withClue("legal actions for Imbraham: $offered") {
                    offered.any { it.abilityId == abilityId } shouldBe true
                }
            }

            test("Imbraham marks each of the top X cards, then you may decline to take one") {
                val game = deansBoard()
                game.activate("Imbraham, Dean of Theory", 0, xValue = 4)

                withClue("the study cards are offered") {
                    game.hasPendingDecision() shouldBe true
                }
                game.skipSelection()
                game.resolveStack()

                listOf("Kianne Test Bear A", "Kianne Test Bear B", "Kianne Test Giant", "Forest").forEach {
                    withClue("$it was exiled with a study counter") {
                        game.studyCounters(game.exiledNamed(it)) shouldBe 1
                    }
                }
                game.studyCounters(game.exiledNamed("Kianne Test Wurm")) shouldBe 0
                game.isInExile(1, "Island") shouldBe false
            }

            test("the Fractal gets one counter per different mana value among nonland study cards") {
                val game = deansBoard()
                game.activate("Imbraham, Dean of Theory", 0, xValue = 4)
                game.skipSelection()
                game.resolveStack()

                game.activate("Kianne, Dean of Substance", 1)

                val fractal = game.findPermanent("Fractal Token")!!
                withClue("MVs 2, 2 and 4 are two different values; the land and the unmarked wurm don't count") {
                    game.state.projectedState.getPower(fractal) shouldBe 2
                    game.state.projectedState.getToughness(fractal) shouldBe 2
                }
            }

            test("at X = 0 Imbraham exiles nothing but still returns a study card, which loses its counter") {
                val game = deansBoard()
                game.activate("Kianne, Dean of Substance", 0)
                val bear = game.exiledNamed("Kianne Test Bear A")
                game.studyCounters(bear) shouldBe 1

                game.activate("Imbraham, Dean of Theory", 0, xValue = 0)
                withClue("the bear Kianne marked is offered to Imbraham") {
                    game.hasPendingDecision() shouldBe true
                }
                game.selectCards(listOf(bear))
                game.resolveStack()

                game.isInHand(1, "Kianne Test Bear A") shouldBe true
                withClue("nothing else was exiled") {
                    game.isInExile(1, "Kianne Test Bear B") shouldBe false
                }
                val inHand = game.state.getHand(game.player1Id).first {
                    game.state.getEntity(it)!!.get<CardComponent>()!!.name == "Kianne Test Bear A"
                }
                game.studyCounters(inHand) shouldBe 0
            }
        }
    }
}
