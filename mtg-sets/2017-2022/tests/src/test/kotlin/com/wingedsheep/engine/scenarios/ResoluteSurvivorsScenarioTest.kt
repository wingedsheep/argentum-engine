package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.hou.cards.ResoluteSurvivors
import com.wingedsheep.mtg.sets.definitions.hou.cards.StewardOfSolidarity
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Resolute Survivors (HOU) — "You may exert this creature as it attacks. Whenever you exert a
 * creature, this creature deals 1 damage to each opponent and you gain 1 life."
 */
class ResoluteSurvivorsScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + ResoluteSurvivors + StewardOfSolidarity)
        d.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    test("exerting it as it attacks drains each opponent for 1") {
        val d = driver()
        val active = d.activePlayer!!
        val opponent = d.getOpponent(active)
        val survivors = d.putCreatureOnBattlefield(active, "Resolute Survivors")
        d.removeSummoningSickness(survivors)
        val (ours, theirs) = d.getLifeTotal(active) to d.getLifeTotal(opponent)

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(active, listOf(survivors), opponent)
        d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        d.submitCardSelection(active, listOf(survivors))
        d.bothPass()

        d.getLifeTotal(active) shouldBe ours + 1
        d.getLifeTotal(opponent) shouldBe theirs - 1
    }

    test("exerting another creature for an ability cost drains too") {
        val d = driver()
        val active = d.activePlayer!!
        val opponent = d.getOpponent(active)
        d.putCreatureOnBattlefield(active, "Resolute Survivors")
        val steward = d.putCreatureOnBattlefield(active, "Steward of Solidarity")
        d.removeSummoningSickness(steward)
        val (ours, theirs) = d.getLifeTotal(active) to d.getLifeTotal(opponent)

        d.submitSuccess(
            ActivateAbility(playerId = active, sourceId = steward, abilityId = StewardOfSolidarity.activatedAbilities.single().id)
        )
        d.bothPass()

        d.getLifeTotal(active) shouldBe ours + 1
        d.getLifeTotal(opponent) shouldBe theirs - 1
    }
})
