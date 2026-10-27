package com.wingedsheep.engine.hidden

import com.wingedsheep.engine.core.DistributeDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.permanent.counters.DistributeCountersFromSelfExecutor
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.player.SkipUntapComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.DistributeCountersFromSelfEffect
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * A face-down permanent has only its face-down characteristics — a 2/2 creature with no name, no
 * subtypes and no text (CR 708.2) — whatever card it is. Every site here used to read the card
 * underneath instead, which is both a rules bug and a leak: the visible outcome told the other
 * players what the card was (#3340 group D). Each test turns a permanent face down and checks the
 * site treats it as the face-down creature, not as its card.
 */
class FaceDownCharacteristicsTest : ScenarioTestBase() {

    private val legendarySpider = card("Test Legendary Spider") {
        manaCost = "{1}"
        typeLine = "Legendary Creature — Spider"
        power = 1
        toughness = 1
    }

    init {
        cardRegistry.register(listOf(legendarySpider))
    }

    private fun TestGame.turnFaceDown(name: String): EntityId {
        val id = findPermanent(name)!!
        state = state.updateEntity(id) { it.with(FaceDownComponent) }
        return id
    }

    init {
        test("a face-down Dueling Grounds doesn't cap blockers") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardOnBattlefield(2, "Dueling Grounds")
                .withCardOnBattlefield(2, "Centaur Courser")
                .withCardOnBattlefield(2, "Elvish Warrior")
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()
            game.turnFaceDown("Dueling Grounds")

            game.declareAttackers(mapOf("Grizzly Bears" to 2, "Hill Giant" to 2)).error.shouldBeNull()
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(
                mapOf("Centaur Courser" to listOf("Grizzly Bears"), "Elvish Warrior" to listOf("Hill Giant"))
            ).error.shouldBeNull()
        }

        test("a face-down Propaganda taxes no attack") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Propaganda")
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()
            game.turnFaceDown("Propaganda")

            // P1 has no mana: a live Propaganda would refuse the attack.
            game.declareAttackers(mapOf("Grizzly Bears" to 2)).error.shouldBeNull()
        }

        test("a face-down Spider-Verse exempts no legend from the legend rule") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Spider-Verse")
                .withCardOnBattlefield(1, "Test Legendary Spider")
                .withCardOnBattlefield(1, "Test Legendary Spider")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.checkStateBasedActions()
            game.state.pendingDecision.shouldBeNull()

            game.turnFaceDown("Spider-Verse")
            game.checkStateBasedActions()
            game.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        }

        test("Forgotten Ancient can move counters onto a face-down permanent whatever card it is") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Forgotten Ancient")
                .withCardOnBattlefield(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.BEGINNING, Step.UPKEEP)
                .build()
            val ancient = game.findPermanent("Forgotten Ancient")!!
            game.state = game.state.updateEntity(ancient) { it.with(CountersComponent().withAdded(CounterType.PLUS_ONE_PLUS_ONE, 2)) }
            val forest = game.turnFaceDown("Forest")

            val result = DistributeCountersFromSelfExecutor().execute(
                game.state,
                DistributeCountersFromSelfEffect(CounterType.PLUS_ONE_PLUS_ONE),
                EffectContext(sourceId = ancient, controllerId = game.player1Id),
            )

            val decision = result.state.pendingDecision.shouldBeInstanceOf<DistributeDecision>()
            decision.targets shouldContain forest
        }

        test("a skip-untap for lands only untaps a face-down land, which is a creature") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Forest", tapped = true)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(2)
                .inPhase(Phase.ENDING, Step.END)
                .build()
            val forest = game.turnFaceDown("Forest")
            game.state = game.state.updateEntity(game.player1Id) {
                it.with(SkipUntapComponent(listOf(com.wingedsheep.sdk.scripting.GameObjectFilter.Land)))
            }

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)

            game.state.activePlayerId shouldBe game.player1Id
            game.state.getEntity(forest)!!.has<TappedComponent>() shouldBe false
        }
    }
}
