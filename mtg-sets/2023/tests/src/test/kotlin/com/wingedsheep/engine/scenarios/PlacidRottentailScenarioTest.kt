package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mom.cards.PlacidRottentail
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Placid Rottentail — {G} Creature — Fungus Rabbit (1/1), vigilance.
 *
 * "{2}{G}, Exile this card from your graveyard: Put two +1/+1 counters on target creature.
 *  Activate only as a sorcery."
 */
class PlacidRottentailScenarioTest : FunSpec({

    val projector = StateProjector()
    val ability = PlacidRottentail.activatedAbilities.first().id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(PlacidRottentail)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("exiles itself from the graveyard to put two +1/+1 counters on any target creature") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        val rottentail = driver.putCardInGraveyard(me, "Placid Rottentail")
        // "target creature" — not restricted to creatures you control.
        val oppCreature = driver.putCreatureOnBattlefield(opp, "Centaur Courser") // 3/3

        driver.giveColorlessMana(me, 2)
        driver.giveMana(me, Color.GREEN, 1)
        driver.submit(
            ActivateAbility(
                playerId = me,
                sourceId = rottentail,
                abilityId = ability,
                targets = listOf(ChosenTarget.Permanent(oppCreature))
            )
        ).outcome shouldBe Outcome.Done

        driver.getExile(me).contains(rottentail) shouldBe true
        driver.getGraveyard(me).contains(rottentail) shouldBe false

        driver.bothPass()

        val counters = driver.state.getEntity(oppCreature)?.get<CountersComponent>()?.counters ?: emptyMap()
        counters[CounterType.PLUS_ONE_PLUS_ONE] shouldBe 2
        projector.getProjectedPower(driver.state, oppCreature) shouldBe 5
    }

    test("can't be activated at instant speed") {
        val driver = createDriver()
        val me = driver.activePlayer!!

        val rottentail = driver.putCardInGraveyard(me, "Placid Rottentail")
        val target = driver.putCreatureOnBattlefield(me, "Centaur Courser")

        driver.passPriorityUntil(Step.UPKEEP)
        driver.giveColorlessMana(me, 2)
        driver.giveMana(me, Color.GREEN, 1)
        driver.submit(
            ActivateAbility(
                playerId = me,
                sourceId = rottentail,
                abilityId = ability,
                targets = listOf(ChosenTarget.Permanent(target))
            )
        ).error shouldNotBe null
        driver.getGraveyard(me).contains(rottentail) shouldBe true
    }
})
