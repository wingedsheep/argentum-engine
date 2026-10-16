package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.ReduceEquipCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Cost reductions scoped to what the spell or ability targets, read relative to the reducing
 * permanent:
 *  - `ReduceEquipCost.targetFilter` — "Equip abilities you activate that target <filter> cost {N}
 *    less". Exact at payment (the chosen target must match); at enumeration it is offered only when a
 *    creature the player controls matches.
 *  - `FixedIfAnyTargetMatches` on a permanent's `ModifySpellCost` — the filter is evaluated with the
 *    permanent as its source, so `sourceItself()` / `attachedToBySource()` resolve, both when
 *    pricing a cast and when enumeration decides affordability.
 *
 * CR 601.2f: the total cost is locked after targets are chosen (CR 601.2c), so the reduction reads
 * the chosen targets.
 */
class TargetScopedCostReductionTest : FunSpec({

    val smallForge = card("Small Forge") {
        manaCost = "{1}"
        typeLine = "Artifact"
        oracleText = "Equip abilities you activate that target a creature with power 2 or less cost {2} less to activate."
        staticAbility {
            ability = ReduceEquipCost(amount = 2, targetFilter = GameObjectFilter.Creature.powerAtMost(2))
        }
    }

    val blade = card("Scope Test Blade") {
        manaCost = "{1}"
        typeLine = "Artifact — Equipment"
        oracleText = "Equip {3}"
        equipAbility("{3}")
    }
    val equipId = blade.activatedAbilities.first().id

    val beacon = card("Beacon Totem") {
        manaCost = "{2}"
        typeLine = "Artifact Creature — Construct"
        power = 1
        toughness = 1
        oracleText = "Spells you cast that target this creature cost {2} less to cast."
        staticAbility {
            ability = ModifySpellCost(
                target = SpellCostTarget.YouCast(GameObjectFilter.Any),
                modification = CostModification.ReduceGenericBy(
                    CostReductionSource.FixedIfAnyTargetMatches(2, GameObjectFilter.Any.sourceItself())
                )
            )
        }
    }

    val boost = card("Scope Boost") {
        manaCost = "{2}{G}"
        typeLine = "Instant"
        oracleText = "Target creature gets +1/+1 until end of turn."
        spell { effect = Effects.ModifyStats(1, 1, target(TargetFilter.Creature)) }
    }

    fun newGame(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(smallForge, blade, beacon, boost))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("equip discount applies when the chosen target matches the filter") {
        val driver = newGame()
        val me = driver.activePlayer!!
        driver.putPermanentOnBattlefield(me, "Small Forge")
        val bear = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val equipment = driver.putPermanentOnBattlefield(me, "Scope Test Blade")

        driver.giveColorlessMana(me, 1)
        driver.submit(
            ActivateAbility(me, equipment, equipId, targets = listOf(ChosenTarget.Permanent(bear)))
        ).outcome shouldBe Outcome.Done
    }

    test("equip discount does not apply when the chosen target misses the filter") {
        val driver = newGame()
        val me = driver.activePlayer!!
        driver.putPermanentOnBattlefield(me, "Small Forge")
        driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val courser = driver.putCreatureOnBattlefield(me, "Centaur Courser")
        val equipment = driver.putPermanentOnBattlefield(me, "Scope Test Blade")

        driver.giveColorlessMana(me, 1)
        driver.submit(
            ActivateAbility(me, equipment, equipId, targets = listOf(ChosenTarget.Permanent(courser)))
        ).outcome shouldNotBe Outcome.Done
    }

    test("enumeration offers the equip discount only when a controlled creature matches") {
        val driver = newGame()
        val me = driver.activePlayer!!
        driver.putPermanentOnBattlefield(me, "Small Forge")
        driver.putCreatureOnBattlefield(me, "Centaur Courser")
        val equipment = driver.putPermanentOnBattlefield(me, "Scope Test Blade")
        driver.putLandOnBattlefield(me, "Forest")

        fun equipAffordable() = driver.legalActions(me)
            .filter { (it.action as? ActivateAbility)?.sourceId == equipment }
            .any { it.affordable }

        equipAffordable() shouldBe false
        // An opponent's matching creature is no equip target, so it doesn't unlock the discount.
        driver.putCreatureOnBattlefield(driver.getOpponent(me), "Grizzly Bears")
        equipAffordable() shouldBe false
        driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        equipAffordable() shouldBe true
    }

    test("a permanent's target-conditional spell discount reads the permanent as its source") {
        val driver = newGame()
        val me = driver.activePlayer!!
        val totem = driver.putCreatureOnBattlefield(me, "Beacon Totem")
        driver.putLandOnBattlefield(me, "Forest")
        val spell = driver.putCardInHand(me, "Scope Boost")

        driver.legalActions(me)
            .filter { (it.action as? CastSpell)?.cardId == spell }
            .any { it.affordable } shouldBe true

        driver.giveMana(me, Color.GREEN, 1)
        driver.castSpell(me, spell, listOf(totem)).outcome shouldBe Outcome.Done
    }

    test("the spell discount does not apply to a spell targeting something else") {
        val driver = newGame()
        val me = driver.activePlayer!!
        driver.putCreatureOnBattlefield(me, "Beacon Totem")
        val bear = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val spell = driver.putCardInHand(me, "Scope Boost")

        driver.giveMana(me, Color.GREEN, 1)
        driver.castSpell(me, spell, listOf(bear)).outcome shouldNotBe Outcome.Done
    }
})
