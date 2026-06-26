package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.battlefield.SagaComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.fin.cards.CrystalFragments
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.TimingRule
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Crystal Fragments // Summon: Alexander (FIN #13).
 *
 * Exercises the two engine capabilities this card needed:
 *   1. Equipment ↔ Saga-creature transform — the front is an *Equipment* (not the creature // creature
 *      case [CardDefinition.doubleFacedCreature] handles), so it is built with
 *      [com.wingedsheep.sdk.model.CardDefinition.doubleFacedPermanent] and flips via the face-agnostic
 *      [com.wingedsheep.sdk.scripting.effects.ExileAndReturnTransformedEffect].
 *   2. Recipient-group damage prevention — chapters I, II apply
 *      [com.wingedsheep.sdk.dsl.Effects.PreventAllDamageToGroup] over "creatures you control".
 */
class CrystalFragmentsScenarioTest : FunSpec({

    val projector = StateProjector()

    fun resolveStack(driver: GameTestDriver) {
        var guard = 0
        while (guard++ < 40 && driver.state.stack.isNotEmpty() && !driver.isPaused) driver.bothPass()
    }

    fun advanceToNextTurnMain(driver: GameTestDriver) {
        driver.passPriorityUntil(Step.END, maxPasses = 300)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN, maxPasses = 300)
        resolveStack(driver)
    }

    fun newGame(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(CrystalFragments))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver to driver.activePlayer!!
    }

    fun GameTestDriver.markedDamage(id: EntityId): Int =
        state.getEntity(id)?.get<DamageComponent>()?.amount ?: 0

    val transformAbilityId = CrystalFragments.activatedAbilities.first { !it.isEquipAbility }.id
    val equipAbilityId = CrystalFragments.activatedAbilities.first { it.isEquipAbility }.id

    test("the transform ability is sorcery-speed (CR: 'activate only as a sorcery')") {
        CrystalFragments.activatedAbilities.first { !it.isEquipAbility }.timing shouldBe
            TimingRule.SorcerySpeed
    }

    test("equips a creature for +1/+1, then transforms into Summon: Alexander as a new object") {
        val (driver, you) = newGame()
        val courser = driver.putCreatureOnBattlefield(you, "Centaur Courser") // 3/3
        val fragments = driver.putPermanentOnBattlefield(you, "Crystal Fragments")

        // Equip {1}: equipped creature gets +1/+1 -> 4/4.
        driver.giveColorlessMana(you, 1)
        driver.submit(
            ActivateAbility(you, fragments, equipAbilityId, targets = listOf(ChosenTarget.Permanent(courser)))
        ).isSuccess shouldBe true
        driver.bothPass() // resolve the equip ability off the stack
        resolveStack(driver)
        projector.project(driver.state).getPower(courser) shouldBe 4
        projector.project(driver.state).getToughness(courser) shouldBe 4

        // {5}{W}{W}: exile and return transformed (sorcery speed).
        driver.giveMana(you, Color.WHITE, 2)
        driver.giveColorlessMana(you, 5)
        driver.submit(ActivateAbility(you, fragments, transformAbilityId)).isSuccess shouldBe true
        driver.bothPass()
        resolveStack(driver)

        // Same entity id, now the back face: a 4/3 Flying Enchantment-Creature Saga with a fresh
        // lore counter (it re-entered as a brand-new object).
        val container = driver.state.getEntity(fragments)!!
        container.get<CardComponent>()!!.name shouldBe "Summon: Alexander"
        val projected = projector.project(driver.state)
        projected.isCreature(fragments) shouldBe true
        projected.hasType(fragments, "Saga") shouldBe true
        projected.getPower(fragments) shouldBe 4
        projected.getToughness(fragments) shouldBe 3
        projected.hasKeyword(fragments, Keyword.FLYING) shouldBe true
        container.get<SagaComponent>() shouldNotBe null
        container.get<CountersComponent>()!!.getCount(CounterType.LORE) shouldBe 1

        // The Equipment fell off Courser when it left as a new object: back to base 3/3.
        projector.project(driver.state).getPower(courser) shouldBe 3
    }

    test("chapter I prevents all damage to creatures you control, but not to opponents'") {
        val (driver, you) = newGame()
        val opponent = driver.state.turnOrder.first { it != you }
        val myCreature = driver.putCreatureOnBattlefield(you, "Centaur Courser") // 3/3
        val theirCreature = driver.putCreatureOnBattlefield(opponent, "Centaur Courser") // 3/3
        val fragments = driver.putPermanentOnBattlefield(you, "Crystal Fragments")

        // Transform -> Summon: Alexander; chapter I "prevent all damage to creatures you control"
        // triggers on entry and resolves, leaving a turn-long shield.
        driver.giveMana(you, Color.WHITE, 2)
        driver.giveColorlessMana(you, 5)
        driver.submit(ActivateAbility(you, fragments, transformAbilityId)).isSuccess shouldBe true
        driver.bothPass()
        resolveStack(driver)
        driver.state.getEntity(fragments)!!.get<CardComponent>()!!.name shouldBe "Summon: Alexander"

        // Bolt my own creature -> prevented (no marked damage, survives).
        driver.giveMana(you, Color.RED, 1)
        val bolt1 = driver.putCardInHand(you, "Lightning Bolt")
        driver.castSpellWithTargets(you, bolt1, listOf(ChosenTarget.Permanent(myCreature)))
        driver.bothPass()
        resolveStack(driver)
        driver.markedDamage(myCreature) shouldBe 0
        driver.state.getEntity(myCreature) shouldNotBe null

        // Bolt the Saga itself (also a creature I control) -> prevented.
        driver.giveMana(you, Color.RED, 1)
        val bolt2 = driver.putCardInHand(you, "Lightning Bolt")
        driver.castSpellWithTargets(you, bolt2, listOf(ChosenTarget.Permanent(fragments)))
        driver.bothPass()
        resolveStack(driver)
        driver.markedDamage(fragments) shouldBe 0

        // Bolt the opponent's creature -> NOT prevented (it dies to 3 damage).
        driver.giveMana(you, Color.RED, 1)
        val bolt3 = driver.putCardInHand(you, "Lightning Bolt")
        driver.castSpellWithTargets(you, bolt3, listOf(ChosenTarget.Permanent(theirCreature)))
        driver.bothPass()
        resolveStack(driver)
        driver.state.getBattlefield().contains(theirCreature) shouldBe false
    }

    test("chapter III taps all creatures opponents control") {
        val (driver, you) = newGame()
        val opponent = driver.state.turnOrder.first { it != you }
        val theirCreature = driver.putCreatureOnBattlefield(opponent, "Centaur Courser")
        val fragments = driver.putPermanentOnBattlefield(you, "Crystal Fragments")

        driver.giveMana(you, Color.WHITE, 2)
        driver.giveColorlessMana(you, 5)
        driver.submit(ActivateAbility(you, fragments, transformAbilityId)).isSuccess shouldBe true
        driver.bothPass()
        resolveStack(driver)

        // Accrue lore to 3: entered with lore 1 (chapter I), +1 after each of my draw steps.
        advanceToNextTurnMain(driver) // opp turn
        advanceToNextTurnMain(driver) // my turn -> lore 2 (chapter II, prevention again)
        driver.state.getEntity(fragments)!!.get<CountersComponent>()!!.getCount(CounterType.LORE) shouldBe 2
        advanceToNextTurnMain(driver) // opp turn
        advanceToNextTurnMain(driver) // my turn -> lore 3 (chapter III taps opponents' creatures)
        resolveStack(driver)

        driver.state.getEntity(theirCreature)!!.get<TappedComponent>() shouldNotBe null
    }
})
