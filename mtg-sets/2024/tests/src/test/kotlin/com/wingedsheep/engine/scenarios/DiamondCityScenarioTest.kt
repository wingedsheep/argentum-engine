package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.pip.cards.DiamondCity
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Diamond City — enters with a shield counter, and its move ability is gated on two or more
 * creatures having *entered* under your control this turn (the per-turn entry log, not the
 * creatures still on the battlefield).
 */
class DiamondCityScenarioTest : FunSpec({
    fun setup() = GameTestDriver().apply {
        registerCards(TestCards.all + DiamondCity)
        initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun resolve(d: GameTestDriver) {
        var guard = 0
        while (d.pendingDecision == null && d.state.stack.isNotEmpty() && guard++ < 40)
            d.passPriority(d.state.priorityPlayerId!!).error shouldBe null
    }
    fun shields(d: GameTestDriver, id: EntityId) =
        d.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.SHIELD) ?: 0
    fun castLions(d: GameTestDriver) {
        val lions = d.putCardInHand(d.player1, "Savannah Lions")
        d.giveMana(d.player1, Color.WHITE, 1)
        d.castSpell(d.player1, lions).error shouldBe null
        resolve(d)
    }

    val moveAbility = DiamondCity.activatedAbilities[1].id

    test("enters with a shield counter; the move ability needs two creatures to have entered this turn") {
        val d = setup()
        val cityCard = d.putCardInHand(d.player1, DiamondCity.name)
        d.playLand(d.player1, cityCard).error shouldBe null
        val city = d.findPermanent(d.player1, DiamondCity.name).shouldNotBeNull()
        shields(d, city) shouldBe 1

        val bears = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        fun activate() = d.submit(
            ActivateAbility(d.player1, city, moveAbility, targets = listOf(ChosenTarget.Permanent(bears)))
        )

        activate().error.shouldNotBeNull()
        castLions(d)
        activate().error.shouldNotBeNull()
        castLions(d)

        // A creature that entered and has since left still counts — the entry happened.
        d.moveToGraveyard(d.findPermanent(d.player1, "Savannah Lions").shouldNotBeNull())

        activate().error shouldBe null
        resolve(d)
        shields(d, city) shouldBe 0
        shields(d, bears) shouldBe 1
    }
})
