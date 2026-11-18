package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.mechanics.mana.CostCalculator
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.engine.view.LegalActionEnricher
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
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.AnyTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * A per-target cost modifier that counts only the targets matching a filter —
 * `CostReductionSource.ChosenTargets(filter = …)`, Battlefield Thaumaturge's "costs {1} less to
 * cast for each creature it targets". A player or a creature *card* in a graveyard is a target but
 * not a creature, so it earns nothing. With a filtered modifier in play the client's price table
 * is keyed by different targets *and* matching ones, and the action names the objects that match.
 */
class FilteredPerTargetSpellCostTest : FunSpec({

    val creatureDiscounter = card("Creature Target Discounter") {
        manaCost = "{2}"
        typeLine = "Artifact"
        oracleText = "Each instant and sorcery spell you cast costs {1} less to cast for each creature it targets."
        staticAbility {
            ability = ModifySpellCost(
                target = SpellCostTarget.YouCast(GameObjectFilter.InstantOrSorcery),
                modification = CostModification.ReduceGenericBy(
                    CostReductionSource.ChosenTargets(filter = GameObjectFilter.Creature.onBattlefield()),
                ),
            )
        }
    }

    val anyTaxer = card("Any Target Taxer") {
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

    val twinZap = card("Twin Zap") {
        manaCost = "{3}{R}"
        typeLine = "Sorcery"
        oracleText = "Twin Zap deals 1 damage to any target and 1 damage to any target."
        spell {
            effect = Effects.DealDamage(1, target(AnyTarget())) then Effects.DealDamage(1, target(AnyTarget()))
        }
    }

    val graveLook = card("Grave Look") {
        manaCost = "{2}{B}"
        typeLine = "Sorcery"
        oracleText = "Exile target creature card from a graveyard."
        spell {
            effect = Effects.Exile(target(TargetObject(filter = TargetFilter.CreatureInGraveyard)))
        }
    }

    fun newGame(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(creatureDiscounter, anyTaxer, twinZap, graveLook))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun key(distinct: Int, matched: Int) = CostCalculator.targetPriceKey(distinct, matched)

    fun GameTestDriver.castAction(me: EntityId, spell: EntityId) =
        legalActions(me).firstOrNull { (it.action as? CastSpell)?.cardId == spell }

    test("only creature targets earn the discount") {
        val driver = newGame()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.putPermanentOnBattlefield(me, "Creature Target Discounter")
        val bears = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        val spell = driver.putCardInHand(me, "Twin Zap")

        // A creature and a player: {3}{R} less {1} is {2}{R}, so {R}{R} is short.
        driver.giveMana(me, Color.RED, 2)
        val targets = listOf(ChosenTarget.Permanent(bears), ChosenTarget.Player(opponent))
        driver.submit(CastSpell(me, spell, targets)).outcome shouldNotBe Outcome.Done
        driver.giveMana(me, Color.RED, 1)
        driver.submit(CastSpell(me, spell, targets)).outcome shouldBe Outcome.Done
    }

    test("two different creature targets earn {1} each") {
        val driver = newGame()
        val me = driver.activePlayer!!
        driver.putPermanentOnBattlefield(me, "Creature Target Discounter")
        val bears = driver.putCreatureOnBattlefield(driver.getOpponent(me), "Grizzly Bears")
        val lions = driver.putCreatureOnBattlefield(driver.getOpponent(me), "Savannah Lions")
        val spell = driver.putCardInHand(me, "Twin Zap")

        driver.giveMana(me, Color.RED, 2)
        driver.submit(CastSpell(me, spell, listOf(ChosenTarget.Permanent(bears), ChosenTarget.Permanent(lions))))
            .outcome shouldBe Outcome.Done
    }

    test("a creature card in a graveyard is not a creature") {
        val driver = newGame()
        val me = driver.activePlayer!!
        driver.putPermanentOnBattlefield(me, "Creature Target Discounter")
        val corpse = driver.putCardInGraveyard(driver.getOpponent(me), "Grizzly Bears")
        val spell = driver.putCardInHand(me, "Grave Look")

        driver.giveMana(me, Color.BLACK, 2)
        driver.submit(CastSpell(me, spell, listOf(ChosenTarget.Card(corpse, driver.getOpponent(me), com.wingedsheep.sdk.core.Zone.GRAVEYARD))))
            .outcome shouldNotBe Outcome.Done
        driver.giveMana(me, Color.BLACK, 1)
        driver.submit(CastSpell(me, spell, listOf(ChosenTarget.Card(corpse, driver.getOpponent(me), com.wingedsheep.sdk.core.Zone.GRAVEYARD))))
            .outcome shouldBe Outcome.Done
    }

    test("affordability prices the most creature targets, and the table is keyed by both counts") {
        val driver = newGame()
        val me = driver.activePlayer!!
        driver.putPermanentOnBattlefield(me, "Creature Target Discounter")
        val bears = driver.putCreatureOnBattlefield(driver.getOpponent(me), "Grizzly Bears")
        val lions = driver.putCreatureOnBattlefield(me, "Savannah Lions")
        val spell = driver.putCardInHand(me, "Twin Zap")
        repeat(2) { driver.putLandOnBattlefield(me, "Mountain") }

        val action = driver.castAction(me, spell).shouldNotBeNull()
        action.affordable shouldBe true
        ManaCost.parse(action.manaCostString!!) shouldBe ManaCost.parse("{1}{R}")
        // Keyed by (different targets, creatures among them): one or two targets — two phrases may
        // share one — with up to that many creatures; each creature takes {1} off {3}{R}.
        action.manaCostByTargetCount.shouldNotBeNull().mapValues { ManaCost.parse(it.value).cmc } shouldBe mapOf(
            key(1, 0) to 4, key(1, 1) to 3,
            key(2, 0) to 4, key(2, 1) to 3, key(2, 2) to 2,
        )

        val info = LegalActionEnricher(driver.services.manaSolver, driver.cardRegistry, driver.services.costCalculator)
            .enrich(listOf(action), driver.state, me).single()
        // Every creature in play counts — and no player, though both players are legal targets.
        info.targetPriceCountedIds.shouldNotBeNull() shouldContainExactlyInAnyOrder listOf(bears, lions)
    }

    test("beside a modifier counting every target, the table prices both counts") {
        val driver = newGame()
        val me = driver.activePlayer!!
        driver.putPermanentOnBattlefield(me, "Creature Target Discounter")
        driver.putPermanentOnBattlefield(driver.getOpponent(me), "Any Target Taxer")
        val bears = driver.putCreatureOnBattlefield(driver.getOpponent(me), "Grizzly Bears")
        val spell = driver.putCardInHand(me, "Twin Zap")
        repeat(6) { driver.putLandOnBattlefield(me, "Mountain") }

        // {3}{R}, {1} more per target, {1} less per creature: a creature and a player is {4}{R},
        // two creature-free targets {5}{R} — the count of different targets alone can't say which.
        val action = driver.castAction(me, spell).shouldNotBeNull()
        action.manaCostByTargetCount.shouldNotBeNull().mapValues { ManaCost.parse(it.value).cmc } shouldBe mapOf(
            key(1, 0) to 5, key(1, 1) to 4,
            key(2, 0) to 6, key(2, 1) to 5,
        )
        LegalActionEnricher(driver.services.manaSolver, driver.cardRegistry, driver.services.costCalculator)
            .enrich(listOf(action), driver.state, me).single()
            .targetPriceCountedIds shouldBe listOf(bears)
    }

    test("a creature targeted twice earns {1} once") {
        val driver = newGame()
        val me = driver.activePlayer!!
        driver.putPermanentOnBattlefield(me, "Creature Target Discounter")
        val bears = driver.putCreatureOnBattlefield(driver.getOpponent(me), "Grizzly Bears")
        val spell = driver.putCardInHand(me, "Twin Zap")

        val twice = listOf(ChosenTarget.Permanent(bears), ChosenTarget.Permanent(bears))
        driver.giveMana(me, Color.RED, 2)
        driver.submit(CastSpell(me, spell, twice)).outcome shouldNotBe Outcome.Done
        driver.giveMana(me, Color.RED, 1)
        driver.submit(CastSpell(me, spell, twice)).outcome shouldBe Outcome.Done
    }

    test("with no filtered modifier in play the table stays keyed by different targets") {
        val driver = newGame()
        val me = driver.activePlayer!!
        driver.putPermanentOnBattlefield(driver.getOpponent(me), "Any Target Taxer")
        driver.putCreatureOnBattlefield(driver.getOpponent(me), "Grizzly Bears")
        val spell = driver.putCardInHand(me, "Twin Zap")
        repeat(6) { driver.putLandOnBattlefield(me, "Mountain") }

        val action = driver.castAction(me, spell).shouldNotBeNull()
        action.manaCostByTargetCount.shouldNotBeNull().mapValues { ManaCost.parse(it.value).cmc } shouldBe
            mapOf(1 to 5, 2 to 6)
        LegalActionEnricher(driver.services.manaSolver, driver.cardRegistry, driver.services.costCalculator)
            .enrich(listOf(action), driver.state, me).single()
            .targetPriceCountedIds.shouldBeNull()
    }
})
