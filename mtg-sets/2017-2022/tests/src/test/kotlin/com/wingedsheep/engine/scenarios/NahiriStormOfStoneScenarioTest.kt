package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.war.cards.NahiriStormOfStone
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EquipAbilitiesAtInstantSpeed
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Nahiri, Storm of Stone {2}{R/W}{R/W} — WAR #233.
 *
 * Pins the "during your turn" gate on both halves of the static — first strike for creatures you
 * control and the {1} equip discount (a [com.wingedsheep.sdk.scripting.ReduceEquipCost] wrapped in
 * an IsYourTurn condition, evaluated at activation) — and the −X's tapped-creature target.
 */
class NahiriStormOfStoneScenarioTest : FunSpec({

    // Equip {2}, so the {1} discount shows as a {1} payment.
    val testBlade = card("Test Blade") {
        manaCost = "{1}"
        typeLine = "Artifact — Equipment"
        oracleText = "Equipped creature gets +1/+0.\nEquip {2}"
        equipAbility("{2}")
    }
    // Lets the equip be activated on the opponent's turn, where Nahiri's discount must not apply.
    val instantEquipper = card("Test Instant Equipper") {
        manaCost = "{1}"
        typeLine = "Artifact"
        oracleText = "You may activate equip abilities any time you could cast an instant."
        staticAbility { ability = EquipAbilitiesAtInstantSpeed }
    }
    val equipId = testBlade.activatedAbilities.first().id
    val minusX = NahiriStormOfStone.script.activatedAbilities.single().id

    data class Board(val d: GameTestDriver, val you: EntityId, val opp: EntityId, val nahiri: EntityId)

    fun setup(): Board {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(NahiriStormOfStone, testBlade, instantEquipper))
        d.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = d.activePlayer!!
        val nahiri = d.putPermanentOnBattlefield(you, "Nahiri, Storm of Stone").also {
            d.addComponent(it, CountersComponent(mapOf(CounterType.LOYALTY to 6)))
        }
        return Board(d, you, d.getOpponent(you), nahiri)
    }

    test("creatures you control have first strike only during your turn") {
        val (d, you, opp, _) = setup()
        val bear = d.putCreatureOnBattlefield(you, "Grizzly Bears")
        val theirBear = d.putCreatureOnBattlefield(opp, "Grizzly Bears")

        d.state.projectedState.hasKeyword(bear, Keyword.FIRST_STRIKE) shouldBe true
        d.state.projectedState.hasKeyword(theirBear, Keyword.FIRST_STRIKE) shouldBe false

        d.passPriorityUntil(Step.PRECOMBAT_MAIN, activePlayer = opp)
        d.state.projectedState.hasKeyword(bear, Keyword.FIRST_STRIKE) shouldBe false
    }

    test("equip costs {1} less during your turn") {
        val (d, you, _, _) = setup()
        val bear = d.putCreatureOnBattlefield(you, "Grizzly Bears")
        val blade = d.putPermanentOnBattlefield(you, "Test Blade")

        d.giveColorlessMana(you, 1)
        d.submit(
            ActivateAbility(you, blade, equipId, targets = listOf(ChosenTarget.Permanent(bear)))
        ).outcome shouldBe Outcome.Done
        d.bothPass()
        d.state.getEntity(blade)?.get<AttachedToComponent>()?.targetId shouldBe bear
    }

    test("equip is not discounted on the opponent's turn") {
        val (d, you, opp, _) = setup()
        val bear = d.putCreatureOnBattlefield(you, "Grizzly Bears")
        val blade = d.putPermanentOnBattlefield(you, "Test Blade")
        d.putPermanentOnBattlefield(you, "Test Instant Equipper")

        d.passPriorityUntil(Step.PRECOMBAT_MAIN, activePlayer = opp)
        d.passPriority(opp)
        d.priorityPlayer shouldBe you

        d.giveColorlessMana(you, 1)
        d.submit(
            ActivateAbility(you, blade, equipId, targets = listOf(ChosenTarget.Permanent(bear)))
        ).outcome shouldNotBe Outcome.Done
        d.state.getEntity(blade)?.get<AttachedToComponent>().shouldBeNull()
    }

    test("−X deals X damage to a tapped creature and can't target an untapped one") {
        val (d, you, opp, nahiri) = setup()
        val untapped = d.putCreatureOnBattlefield(opp, "Grizzly Bears")
        val tapped = d.putCreatureOnBattlefield(opp, "Grizzly Bears")
        d.tapPermanent(tapped)

        d.submit(
            ActivateAbility(you, nahiri, minusX, targets = listOf(ChosenTarget.Permanent(untapped)), xValue = 2)
        ).outcome shouldNotBe Outcome.Done

        d.submitSuccess(
            ActivateAbility(you, nahiri, minusX, targets = listOf(ChosenTarget.Permanent(tapped)), xValue = 1)
        )
        d.state.getEntity(nahiri)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) shouldBe 5
        d.bothPass()
        d.state.getEntity(tapped)?.get<DamageComponent>()?.amount shouldBe 1
        d.getGraveyard(opp).contains(untapped) shouldBe false
    }
})
