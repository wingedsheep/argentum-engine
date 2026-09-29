package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/** Invasion of Ikoria // Zilortha, Apex of Ikoria. */
class InvasionOfIkoriaScenarioTest : ScenarioTestBase() {
    init {
        test("front: ETB searches library and graveyard for a non-Human creature with mana value X or less") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Invasion of Ikoria")
                .withLandsOnBattlefield(1, "Forest", 5)
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Hill Giant")
                .withCardInGraveyard(1, "Wall of Granite")
                .withCardInGraveyard(1, "Elite Vanguard")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castXSpell(1, "Invasion of Ikoria", xValue = 3).error shouldBe null
            game.resolveStack()

            val decision = game.getPendingDecision()
            decision.shouldBeInstanceOf<SelectCardsDecision>()
            withClue("Hill Giant is over X; Elite Vanguard is a Human") {
                decision.options.map { game.state.getEntity(it)!!.get<CardComponent>()!!.name }
                    .shouldContainExactlyInAnyOrder("Grizzly Bears", "Wall of Granite")
            }
            val wall = decision.options.single {
                game.state.getEntity(it)!!.get<CardComponent>()!!.name == "Wall of Granite"
            }
            game.selectCards(listOf(wall)).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Wall of Granite") shouldBe true
            game.isInGraveyard(1, "Elite Vanguard") shouldBe true
        }

        test("back: each blocked non-Human creature you control may assign its damage as though unblocked") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Zilortha, Apex of Ikoria")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Elite Vanguard")
                .withCardOnBattlefield(2, "Wall of Granite")
                .withCardOnBattlefield(2, "Craw Wurm")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val zilortha = game.findPermanent("Zilortha, Apex of Ikoria")!!
            game.state.projectedState.hasKeyword(zilortha, Keyword.REACH) shouldBe true

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2, "Elite Vanguard" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(
                mapOf("Wall of Granite" to listOf("Grizzly Bears"), "Craw Wurm" to listOf("Elite Vanguard"))
            ).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)

            withClue("only the non-Human Bears is asked about") {
                val d = game.getPendingDecision()
                d.shouldBeInstanceOf<YesNoDecision>()
                d.context.sourceName shouldBe "Grizzly Bears"
            }
            game.answerYesNo(true).error shouldBe null
            withClue("no second question for the Human Elite Vanguard") {
                (game.getPendingDecision() is YesNoDecision) shouldBe false
            }
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

            withClue("Bears' 2 damage goes to the opponent; Elite Vanguard's goes to its blocker") {
                game.getLifeTotal(2) shouldBe 18
            }
        }

        test("back: declining assigns to the blocker as normal") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Zilortha, Apex of Ikoria")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Wall of Granite")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Wall of Granite" to listOf("Grizzly Bears"))).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)

            game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(false).error shouldBe null
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

            game.getLifeTotal(2) shouldBe 20
        }

        test("back: an opponent's Zilortha doesn't cover your creatures") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(2, "Zilortha, Apex of Ikoria", tapped = true)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Wall of Granite")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Wall of Granite" to listOf("Grizzly Bears"))).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)

            (game.getPendingDecision() is YesNoDecision) shouldBe false
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
            game.getLifeTotal(2) shouldBe 20
        }

        test("back: a Zilortha that lost all abilities covers nothing") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Zilortha, Apex of Ikoria")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Lignify")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withCardOnBattlefield(2, "Wall of Granite")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Lignify", game.findPermanent("Zilortha, Apex of Ikoria")!!).error shouldBe null
            game.resolveStack()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Wall of Granite" to listOf("Grizzly Bears"))).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)

            (game.getPendingDecision() is YesNoDecision) shouldBe false
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
            game.getLifeTotal(2) shouldBe 20
        }
    }
}
