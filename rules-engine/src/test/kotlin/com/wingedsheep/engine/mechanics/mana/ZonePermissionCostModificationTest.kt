package com.wingedsheep.engine.mechanics.mana

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * `MayCastSelfFromZones.costModification` — a cast-from-zone permission that changes the price of
 * the cast it authorizes ("You may cast this creature from your graveyard if you pay {1} more to
 * cast it for each other creature card in your graveyard", Risen Executioner). The change is a
 * cost increase (CR 601.2f: increases first, then reductions), owed only by a cast through the
 * permission, and its count reads the card itself as the source, so "other" skips it.
 */
class ZonePermissionCostModificationTest : FunSpec({
    val taxed = card("Taxed Graveyard Creature") {
        manaCost = "{B}"; typeLine = "Creature — Zombie"; power = 2; toughness = 2
        staticAbility {
            ability = MayCastSelfFromZones(
                zones = listOf(Zone.GRAVEYARD),
                costModification = CostModification.IncreaseGenericBy(
                    CostReductionSource.Dynamic(
                        DynamicAmounts.count(Player.You, Zone.GRAVEYARD, GameObjectFilter.Creature.notSourceItself())
                    )
                ),
            )
        }
    }
    val filler = card("Graveyard Filler Creature") { manaCost = "{1}"; typeLine = "Creature — Elf"; power = 1; toughness = 1 }
    val sorcery = card("Graveyard Filler Sorcery") { manaCost = "{1}"; typeLine = "Sorcery" }
    val reducer = card("Creature Discounter") {
        typeLine = "Creature — Wizard"; power = 1; toughness = 1
        staticAbility {
            ability = ModifySpellCost(SpellCostTarget.YouCast(GameObjectFilter.Creature), CostModification.ReduceGeneric(1))
        }
    }

    fun setup() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(taxed, filler, sorcery, reducer))
        initMirrorMatch(Deck.of("Swamp" to 30))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.offer(p: EntityId, id: EntityId) =
        legalActions(p).single { (it.action as? CastSpell)?.cardId == id }

    test("a graveyard cast pays {1} more for each other creature card there") {
        val d = setup(); val p = d.activePlayer!!
        val id = d.putCardInGraveyard(p, taxed.name)
        repeat(2) { d.putCardInGraveyard(p, filler.name) }
        d.giveMana(p, Color.BLACK, 3)
        d.offer(p, id).manaCostString shouldBe "{2}{B}"
        d.submit(CastSpell(p, id)).error shouldBe null
        d.state.stack.contains(id) shouldBe true
    }

    test("the cast fails when only the printed cost plus less than the tax is available") {
        val d = setup(); val p = d.activePlayer!!
        val id = d.putCardInGraveyard(p, taxed.name)
        repeat(2) { d.putCardInGraveyard(p, filler.name) }
        d.giveMana(p, Color.BLACK, 2)
        d.offer(p, id).affordable shouldBe false
        d.submit(CastSpell(p, id)).error shouldNotBe null
        d.getGraveyard(p).contains(id) shouldBe true
    }

    test("the card itself and noncreature cards don't count") {
        val d = setup(); val p = d.activePlayer!!
        val id = d.putCardInGraveyard(p, taxed.name)
        d.putCardInGraveyard(p, sorcery.name)
        d.giveMana(p, Color.BLACK, 1)
        d.offer(p, id).manaCostString shouldBe "{B}"
        d.submit(CastSpell(p, id)).error shouldBe null
    }

    test("a hand cast owes nothing, however full the graveyard") {
        val d = setup(); val p = d.activePlayer!!
        repeat(3) { d.putCardInGraveyard(p, filler.name) }
        val id = d.putCardInHand(p, taxed.name)
        d.giveMana(p, Color.BLACK, 1)
        d.submit(CastSpell(p, id)).error shouldBe null
        d.state.stack.contains(id) shouldBe true
    }

    test("cost reductions apply after the increase (CR 601.2f)") {
        val d = setup(); val p = d.activePlayer!!
        d.putCreatureOnBattlefield(p, reducer.name)
        val id = d.putCardInGraveyard(p, taxed.name)
        repeat(2) { d.putCardInGraveyard(p, filler.name) }
        d.giveMana(p, Color.BLACK, 2)
        d.offer(p, id).manaCostString shouldBe "{1}{B}"
        d.submit(CastSpell(p, id)).error shouldBe null
    }
})
