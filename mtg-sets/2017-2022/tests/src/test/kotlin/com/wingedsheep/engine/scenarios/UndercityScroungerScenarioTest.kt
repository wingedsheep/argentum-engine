package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.mtg.sets.definitions.neo.cards.UndercityScrounger
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Undercity Scrounger — {2}{B} Artifact Creature — Human Rogue 1/4
 * {T}: Create a Treasure token. Activate only if a creature died this turn.
 */
class UndercityScroungerScenarioTest : FunSpec({

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + UndercityScrounger)
        registerCards(PredefinedTokens.allTokens)
        initMirrorMatch(deck = Deck.of("Mountain" to 30))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun activate(d: GameTestDriver, scrounger: EntityId) = d.submit(
        ActivateAbility(d.player1, scrounger, UndercityScrounger.activatedAbilities.single().id)
    )

    fun treasures(d: GameTestDriver) = d.getPermanents(d.player1).count { id ->
        d.state.projectedState.hasSubtype(id, "Treasure")
    }

    test("can't be activated before a creature has died this turn") {
        val d = driver()
        val scrounger = d.putCreatureOnBattlefield(d.player1, UndercityScrounger.name)
        d.removeSummoningSickness(scrounger)

        activate(d, scrounger).error shouldNotBe null
        treasures(d) shouldBe 0
    }

    test("creates a Treasure once any creature has died this turn") {
        val d = driver()
        val scrounger = d.putCreatureOnBattlefield(d.player1, UndercityScrounger.name)
        d.removeSummoningSickness(scrounger)
        // An opponent's creature dying counts too.
        val victim = d.putCreatureOnBattlefield(d.player2, "Savannah Lions")
        val bolt = d.putCardInHand(d.player1, "Lightning Bolt")
        d.giveMana(d.player1, Color.RED, 1)
        d.castSpell(d.player1, bolt, targets = listOf(victim)).outcome shouldBe Outcome.Done
        d.bothPass()

        activate(d, scrounger).outcome shouldBe Outcome.Done
        d.bothPass()
        treasures(d) shouldBe 1
    }
})
