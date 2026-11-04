package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.hou.cards.StewardOfSolidarity
import com.wingedsheep.mtg.sets.definitions.hou.cards.VizierOfTheTrue
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Vizier of the True (HOU) — "You may exert this creature as it attacks. Whenever you exert a
 * creature, tap target creature an opponent controls."
 */
class VizierOfTheTrueScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + VizierOfTheTrue + StewardOfSolidarity)
        d.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    test("exerting it as it attacks taps a creature an opponent controls") {
        val d = driver()
        val active = d.activePlayer!!
        val opponent = d.getOpponent(active)
        val vizier = d.putCreatureOnBattlefield(active, "Vizier of the True")
        val blocker = d.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        d.removeSummoningSickness(vizier)

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(active, listOf(vizier), opponent)
        d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        d.submitCardSelection(active, listOf(vizier))
        d.submitTargetSelection(active, listOf(blocker))
        d.bothPass()

        d.isTapped(blocker) shouldBe true
    }

    test("exerting another creature to pay an ability cost also taps an opposing creature") {
        val d = driver()
        val active = d.activePlayer!!
        val opponent = d.getOpponent(active)
        d.putCreatureOnBattlefield(active, "Vizier of the True")
        val steward = d.putCreatureOnBattlefield(active, "Steward of Solidarity")
        val blocker = d.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        d.removeSummoningSickness(steward)

        // The exert trigger's target is chosen as it goes on the stack, so activation pauses.
        d.submit(
            ActivateAbility(playerId = active, sourceId = steward, abilityId = StewardOfSolidarity.activatedAbilities.single().id)
        ).error shouldBe null
        d.submitTargetSelection(active, listOf(blocker))
        d.bothPass()

        d.isTapped(blocker) shouldBe true
    }
})
