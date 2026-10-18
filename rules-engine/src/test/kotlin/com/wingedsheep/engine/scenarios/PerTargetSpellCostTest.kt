package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Spell costs priced per target — [CostReductionSource.ChosenTargets], "for each target"
 * (Hinata, Dawn-Crowned) and "for each target beyond the first" (Fireball).
 *
 * CR 601.2c announces targets before CR 601.2f locks the total cost, so the count is the spell's
 * chosen targets. Hinata's ruling: the count is of *different* players and objects, not of uses of
 * the word "target". Before targets exist, enumeration prices the cheapest reachable count — the
 * most different legal targets under a reduction, the fewest required under an increase — and
 * prices every count for the client. An alternative cost is priced with the targets too.
 */
class PerTargetSpellCostTest : FunSpec({

    val discounter = card("Target Discounter") {
        manaCost = "{2}"
        typeLine = "Artifact"
        oracleText = "Spells you cast cost {1} less to cast for each target."
        staticAbility {
            ability = ModifySpellCost(
                target = SpellCostTarget.YouCast(GameObjectFilter.Any),
                modification = CostModification.ReduceGenericBy(CostReductionSource.ChosenTargets()),
            )
        }
    }

    val taxer = card("Target Taxer") {
        manaCost = "{2}"
        typeLine = "Artifact"
        oracleText = "Spells your opponents cast cost {1} more to cast for each target."
        staticAbility {
            ability = ModifySpellCost(
                target = SpellCostTarget.OpponentsCast(GameObjectFilter.Any),
                modification = CostModification.IncreaseGenericBy(CostReductionSource.ChosenTargets()),
            )
        }
    }

    val twinBoost = card("Twin Boost") {
        manaCost = "{3}{G}"
        typeLine = "Instant"
        oracleText = "Target creature gets +1/+1 until end of turn. Target creature gets +1/+1 until end of turn."
        spell {
            effect = Effects.ModifyStats(1, 1, target(TargetFilter.Creature)) then
                Effects.ModifyStats(1, 1, target(TargetFilter.Creature))
        }
    }

    val spreadBoost = card("Spread Boost") {
        manaCost = "{3}{G}"
        typeLine = "Sorcery"
        oracleText = "Up to three target creatures each get +1/+1 until end of turn."
        spell {
            effect = Effects.ModifyStats(
                1, 1, target(TargetObject(count = 3, optional = true, filter = TargetFilter.Creature))
            )
        }
    }

    val flashbackBoost = card("Flashback Boost") {
        manaCost = "{2}{G}"
        typeLine = "Instant"
        oracleText = "Target creature gets +1/+1 until end of turn.\nFlashback {2}{G}"
        spell { effect = Effects.ModifyStats(1, 1, target(TargetFilter.Creature)) }
        keywordAbility(KeywordAbility.flashback("{2}{G}"))
    }

    fun newGame(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(discounter, taxer, twinBoost, spreadBoost, flashbackBoost))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.castOn(me: EntityId, spell: EntityId, targets: List<EntityId>) =
        submit(CastSpell(me, spell, targets.map { ChosenTarget.Permanent(it) }))

    fun GameTestDriver.castAction(me: EntityId, spell: EntityId) =
        legalActions(me).firstOrNull { (it.action as? CastSpell)?.cardId == spell }

    /** Offered and affordable — an unaffordable cast is not offered at all. */
    fun GameTestDriver.canCast(me: EntityId, spell: EntityId) = castAction(me, spell)?.affordable == true

    test("a reduction counts each different target") {
        val driver = newGame()
        val me = driver.activePlayer!!
        driver.putPermanentOnBattlefield(me, "Target Discounter")
        val bears = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val lions = driver.putCreatureOnBattlefield(me, "Savannah Lions")
        val spell = driver.putCardInHand(me, "Twin Boost")

        driver.giveMana(me, Color.GREEN, 2)
        driver.castOn(me, spell, listOf(bears, lions)).outcome shouldBe Outcome.Done
    }

    test("one object named by two target phrases is one target") {
        val driver = newGame()
        val me = driver.activePlayer!!
        driver.putPermanentOnBattlefield(me, "Target Discounter")
        val bears = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        driver.putCreatureOnBattlefield(me, "Savannah Lions")
        val spell = driver.putCardInHand(me, "Twin Boost")

        driver.giveMana(me, Color.GREEN, 2)
        driver.castOn(me, spell, listOf(bears, bears)).outcome shouldNotBe Outcome.Done
        driver.giveMana(me, Color.GREEN, 1)
        driver.castOn(me, spell, listOf(bears, bears)).outcome shouldBe Outcome.Done
    }

    test("an opponent's increase counts each different target") {
        val driver = newGame()
        val me = driver.activePlayer!!
        driver.putPermanentOnBattlefield(driver.getOpponent(me), "Target Taxer")
        val bears = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val lions = driver.putCreatureOnBattlefield(me, "Savannah Lions")
        val spell = driver.putCardInHand(me, "Twin Boost")

        driver.giveMana(me, Color.GREEN, 5)
        driver.castOn(me, spell, listOf(bears, lions)).outcome shouldNotBe Outcome.Done
        driver.giveMana(me, Color.GREEN, 1)
        driver.castOn(me, spell, listOf(bears, lions)).outcome shouldBe Outcome.Done
    }

    test("a reduction is priced at the most different legal targets for affordability") {
        val driver = newGame()
        val me = driver.activePlayer!!
        driver.putPermanentOnBattlefield(me, "Target Discounter")
        driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val spell = driver.putCardInHand(me, "Spread Boost")
        driver.putLandOnBattlefield(me, "Forest")
        driver.putLandOnBattlefield(me, "Forest")

        // One creature: {3}{G} less {1} is {2}{G} — two Forests can't.
        driver.canCast(me, spell) shouldBe false
        driver.putCreatureOnBattlefield(me, "Savannah Lions")
        driver.putCreatureOnBattlefield(driver.getOpponent(me), "Centaur Courser")
        // Three creatures reachable: {3}{G} less {3} is {G}.
        val action = driver.castAction(me, spell).shouldNotBeNull()
        action.affordable shouldBe true
        ManaCost.parse(action.manaCostString!!) shouldBe ManaCost.parse("{G}")
    }

    test("every reachable target count is priced for the client") {
        val driver = newGame()
        val me = driver.activePlayer!!
        driver.putPermanentOnBattlefield(me, "Target Discounter")
        driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        driver.putCreatureOnBattlefield(me, "Savannah Lions")
        val spell = driver.putCardInHand(me, "Spread Boost")
        repeat(4) { driver.putLandOnBattlefield(me, "Forest") }

        val byCount = driver.castAction(me, spell).shouldNotBeNull().manaCostByTargetCount.shouldNotBeNull()
        byCount.mapValues { ManaCost.parse(it.value).cmc } shouldBe mapOf(0 to 4, 1 to 3, 2 to 2)
    }

    test("an increase is priced at the fewest targets the spell requires") {
        val driver = newGame()
        val me = driver.activePlayer!!
        driver.putPermanentOnBattlefield(driver.getOpponent(me), "Target Taxer")
        driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        driver.putCreatureOnBattlefield(me, "Savannah Lions")
        val spell = driver.putCardInHand(me, "Twin Boost")
        repeat(4) { driver.putLandOnBattlefield(me, "Forest") }

        // Both phrases may name one creature, so one target is the floor: {3}{G} plus {1}.
        driver.canCast(me, spell) shouldBe false
        driver.putLandOnBattlefield(me, "Forest")
        val action = driver.castAction(me, spell).shouldNotBeNull()
        action.affordable shouldBe true
        ManaCost.parse(action.manaCostString!!).cmc shouldBe 5
    }

    test("with no legal target the reduction has nothing to count") {
        val driver = newGame()
        val me = driver.activePlayer!!
        driver.putPermanentOnBattlefield(me, "Target Discounter")
        val spell = driver.putCardInHand(me, "Spread Boost")
        repeat(4) { driver.putLandOnBattlefield(me, "Forest") }

        val action = driver.castAction(me, spell).shouldNotBeNull()
        ManaCost.parse(action.manaCostString!!).cmc shouldBe 4
        action.manaCostByTargetCount shouldBe mapOf(0 to action.manaCostString)
    }

    test("an alternative cost is priced with the chosen targets") {
        val driver = newGame()
        val me = driver.activePlayer!!
        driver.putPermanentOnBattlefield(me, "Target Discounter")
        val bears = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val spell = driver.putCardInGraveyard(me, "Flashback Boost")

        driver.giveMana(me, Color.GREEN, 2)
        driver.submit(
            CastSpell(
                me, spell, listOf(ChosenTarget.Permanent(bears)),
                useAlternativeCost = true, alternativeCostType = AlternativeCostType.FLASHBACK,
            )
        ).outcome shouldBe Outcome.Done
    }
})
