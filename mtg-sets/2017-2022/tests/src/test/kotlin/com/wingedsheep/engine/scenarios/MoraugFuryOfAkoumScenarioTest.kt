package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.player.AdditionalPhasesComponent
import com.wingedsheep.engine.state.components.player.InAdditionalCombatPhaseComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

/**
 * Moraug, Fury of Akoum — "Each creature you control gets +1/+0 for each time it has attacked this
 * turn. Landfall — Whenever a land you control enters, if it's your main phase, there's an
 * additional combat phase after this phase. At the beginning of that combat, untap all creatures
 * you control."
 */
class MoraugFuryOfAkoumScenarioTest : FunSpec({

    /** An instant-speed way to make a land enter outside a main phase. */
    val LandRecall = card("Test Land Recall") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell {
            val t = target(TargetFilter(GameObjectFilter.Land.ownedByYou(), zone = Zone.GRAVEYARD))
            effect = Effects.PutOntoBattlefieldFromGraveyard(t)
        }
    }

    fun newGame(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all)
        initMirrorMatch(Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.power(id: EntityId): Int? = state.projectedState.getPower(id)

    fun GameTestDriver.inAddedCombat(): Boolean =
        state.getEntity(player1)?.has<InAdditionalCombatPhaseComponent>() == true

    /** Play a Mountain from hand and resolve whatever it triggered. */
    fun GameTestDriver.playMountain() {
        val land = putCardInHand(player1, "Mountain")
        playLand(player1, land).error shouldBe null
        while (state.stack.isNotEmpty()) bothPass()
    }

    fun GameTestDriver.enterNextCombatResolvingTriggers(): Int {
        passPriorityUntil(Step.BEGIN_COMBAT)
        val triggered = state.stack.size
        while (state.stack.isNotEmpty()) bothPass()
        return triggered
    }

    test("landfall in the precombat main phase: an extra combat first that untaps, the pump grows per attack") {
        val d = newGame()
        d.registerCard(LandRecall)
        val moraug = d.putCreatureOnBattlefield(d.player1, "Moraug, Fury of Akoum")
        val courser = d.putCreatureOnBattlefield(d.player1, "Centaur Courser")
        d.removeSummoningSickness(moraug)
        d.removeSummoningSickness(courser)
        d.tapPermanent(courser)
        d.power(courser) shouldBe 3

        d.playMountain()

        withClue("The extra combat comes before the regular one and untaps creatures you control") {
            d.enterNextCombatResolvingTriggers() shouldBe 1
            d.inAddedCombat() shouldBe true
            d.isTapped(courser) shouldBe false
        }

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, listOf(courser), d.player2).error shouldBe null
        withClue("Attacked once: +1/+0, already for this combat's damage") {
            d.power(courser) shouldBe 4
            d.power(moraug) shouldBe 6
        }
        d.passPriorityUntil(Step.END_COMBAT)
        d.getLifeTotal(d.player2) shouldBe 16

        withClue("The regular combat follows; nothing untaps at its beginning") {
            d.enterNextCombatResolvingTriggers() shouldBe 0
            d.inAddedCombat() shouldBe false
            d.isTapped(courser) shouldBe true
        }

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, listOf(moraug), d.player2).error shouldBe null
        d.power(moraug) shouldBe 7
        d.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        d.getLifeTotal(d.player2) shouldBe 9
        withClue("The courser keeps its bonus after combat") { d.power(courser) shouldBe 4 }

        withClue("Landfall in the postcombat main phase adds a combat after it; a second attack is +2/+0") {
            // The land drop is used: a land entering by an effect is landfall too.
            val land = d.putCardInGraveyard(d.player1, "Mountain")
            val recall = d.putCardInHand(d.player1, "Test Land Recall")
            d.castSpellWithTargets(
                d.player1, recall, listOf(ChosenTarget.Card(land, d.player1, Zone.GRAVEYARD))
            ).error shouldBe null
            while (d.state.stack.isNotEmpty()) d.bothPass()
            d.enterNextCombatResolvingTriggers() shouldBe 1
            d.inAddedCombat() shouldBe true
            d.isTapped(courser) shouldBe false
            d.passPriorityUntil(Step.DECLARE_ATTACKERS)
            d.declareAttackers(d.player1, listOf(courser), d.player2).error shouldBe null
            d.power(courser) shouldBe 5
            d.passPriorityUntil(Step.END_COMBAT)
            d.getLifeTotal(d.player2) shouldBe 4
        }
    }

    test("landfall outside your main phase adds no combat") {
        val d = newGame()
        d.registerCard(LandRecall)
        d.putCreatureOnBattlefield(d.player1, "Moraug, Fury of Akoum")
        val courser = d.putCreatureOnBattlefield(d.player1, "Centaur Courser")
        d.removeSummoningSickness(courser)
        d.tapPermanent(courser)
        val land = d.putCardInGraveyard(d.player1, "Mountain")
        val recall = d.putCardInHand(d.player1, "Test Land Recall")

        // A land entering during combat — the declare attackers step is not a main phase.
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.castSpellWithTargets(
            d.player1, recall, listOf(ChosenTarget.Card(land, d.player1, Zone.GRAVEYARD))
        ).error shouldBe null
        d.bothPass()
        withClue("The land entered, but the intervening if kept landfall from triggering") {
            d.getLands(d.player1).contains(land) shouldBe true
            d.state.stack.shouldBeEmpty()
        }

        d.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        withClue("No extra combat was queued and nothing was untapped") {
            d.inAddedCombat() shouldBe false
            d.isTapped(courser) shouldBe true
            d.state.getEntity(d.player1)?.has<AdditionalPhasesComponent>() shouldBe false
        }
    }
})
