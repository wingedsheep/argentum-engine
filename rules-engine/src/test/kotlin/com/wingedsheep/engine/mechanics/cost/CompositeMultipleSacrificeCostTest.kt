package com.wingedsheep.engine.mechanics.cost

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * A composite cost holding several [com.wingedsheep.sdk.scripting.costs.CostAtom.Sacrifice] atoms of
 * different filters — "Sacrifice a Swamp and a Forest" (Jarad, Golgari Lich Lord). The atoms are paid
 * by one disjoint assignment ([SacrificeCostAssignment]): every atom gets its own permanent, and a
 * Swamp Forest dual can pay either atom but never both (Jarad's 2012-10-01 ruling). Pinned here:
 *
 *  - the ability is offered only when a disjoint assignment exists — a lone dual is not enough,
 *    nor are two Swamps;
 *  - payment sacrifices one permanent per atom, auto-picking forced atoms (including the case where
 *    only the *assignment* is forced: Swamp + dual must put the dual on the Forest slot);
 *  - a real choice is asked atom by atom, offering only permanents that leave the later atoms payable;
 *  - a pre-filled wrong assignment (the dual twice, out of order, too few) is rejected with nothing paid;
 *  - the same holds for an ability activated from the graveyard.
 */
class CompositeMultipleSacrificeCostTest : FunSpec({

    val swampForest = card("Joint Sac Swamp Forest") {
        typeLine = "Land — Swamp Forest"
    }
    val swamp = card("Joint Sac Swamp") {
        typeLine = "Land — Swamp"
    }
    val forest = card("Joint Sac Forest") {
        typeLine = "Land — Forest"
    }
    val jointCost = Costs.Composite(
        Costs.Sacrifice(GameObjectFilter.Land.withSubtype(Subtype.SWAMP)),
        Costs.Sacrifice(GameObjectFilter.Land.withSubtype(Subtype.FOREST)),
    )
    val outlet = card("Joint Sac Outlet") {
        manaCost = "{1}"
        typeLine = "Artifact"
        oracleText = "Sacrifice a Swamp and a Forest: Draw a card."
        activatedAbility {
            cost = jointCost
            effect = Effects.DrawCards(1)
            timing = TimingRule.InstantSpeed
        }
    }
    val recursion = card("Joint Sac Recursion") {
        manaCost = "{2}"
        typeLine = "Creature — Zombie"
        power = 2
        toughness = 2
        oracleText = "Sacrifice a Swamp and a Forest: Return this card from your graveyard to your hand."
        activatedAbility {
            cost = jointCost
            effect = Effects.ReturnToHandFromGraveyard(EffectTarget.Self)
            activateFromZone = Zone.GRAVEYARD
        }
    }

    fun newDriver(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(swampForest, swamp, forest, outlet, recursion))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver to me
    }

    val outletAbility = outlet.activatedAbilities.single().id
    val recursionAbility = recursion.activatedAbilities.single().id

    fun GameTestDriver.offered(player: EntityId, sourceId: EntityId) =
        legalActions(player).any { (it.action as? ActivateAbility)?.sourceId == sourceId && it.affordable }

    fun GameTestDriver.onBattlefield(id: EntityId) = id in state.getBattlefield()

    test("offered only when each atom can get its own land — a lone dual or two Swamps is not enough") {
        val (driver, me) = newDriver()
        val source = driver.putPermanentOnBattlefield(me, "Joint Sac Outlet")
        driver.putLandOnBattlefield(me, "Joint Sac Swamp Forest")
        withClue("one Swamp Forest pays one atom, not both") { driver.offered(me, source) shouldBe false }

        driver.putLandOnBattlefield(me, "Joint Sac Forest")
        withClue("dual on the Swamp slot, Forest on the Forest slot") { driver.offered(me, source) shouldBe true }
    }

    test("two lands of the same type pay only one atom") {
        val (driver, me) = newDriver()
        val source = driver.putPermanentOnBattlefield(me, "Joint Sac Outlet")
        driver.putLandOnBattlefield(me, "Joint Sac Swamp")
        driver.putLandOnBattlefield(me, "Joint Sac Swamp")
        driver.offered(me, source) shouldBe false
        withClue("the handler refuses it too") {
            driver.submit(ActivateAbility(me, source, outletAbility)).error shouldNotBe null
        }
    }

    test("the legal action carries no single sacrifice picker — the engine asks atom by atom") {
        val (driver, me) = newDriver()
        val source = driver.putPermanentOnBattlefield(me, "Joint Sac Outlet")
        driver.putLandOnBattlefield(me, "Joint Sac Swamp")
        driver.putLandOnBattlefield(me, "Joint Sac Forest")
        val action = driver.legalActions(me).single { (it.action as? ActivateAbility)?.sourceId == source }
        action.additionalCostInfo?.validSacrificeTargets shouldBe null
    }

    test("forced lands are sacrificed without a prompt, one per atom") {
        val (driver, me) = newDriver()
        val source = driver.putPermanentOnBattlefield(me, "Joint Sac Outlet")
        val s = driver.putLandOnBattlefield(me, "Joint Sac Swamp")
        val f = driver.putLandOnBattlefield(me, "Joint Sac Forest")
        val handBefore = driver.getHandSize(me)

        driver.submitSuccess(ActivateAbility(me, source, outletAbility))
        driver.pendingDecision shouldBe null
        driver.onBattlefield(s) shouldBe false
        driver.onBattlefield(f) shouldBe false
        driver.bothPass()
        driver.getHandSize(me) shouldBe handBefore + 1
    }

    test("a forced assignment puts the dual on the slot only it can fill") {
        val (driver, me) = newDriver()
        val source = driver.putPermanentOnBattlefield(me, "Joint Sac Outlet")
        val s = driver.putLandOnBattlefield(me, "Joint Sac Swamp")
        val dual = driver.putLandOnBattlefield(me, "Joint Sac Swamp Forest")

        driver.submitSuccess(ActivateAbility(me, source, outletAbility))
        withClue("both Swamp-slot candidates match, but only the plain Swamp leaves a Forest — no real choice") {
            driver.pendingDecision shouldBe null
        }
        driver.onBattlefield(s) shouldBe false
        driver.onBattlefield(dual) shouldBe false
    }

    test("a real choice is asked per atom, and only lands that keep the rest payable are offered") {
        val (driver, me) = newDriver()
        val source = driver.putPermanentOnBattlefield(me, "Joint Sac Outlet")
        val s1 = driver.putLandOnBattlefield(me, "Joint Sac Swamp")
        val s2 = driver.putLandOnBattlefield(me, "Joint Sac Swamp")
        val dual = driver.putLandOnBattlefield(me, "Joint Sac Swamp Forest")

        driver.submit(ActivateAbility(me, source, outletAbility)).error shouldBe null
        val swampPick = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        withClue("the dual is the only Forest, so it is not offered for the Swamp") {
            swampPick.options shouldContainExactlyInAnyOrder listOf(s1, s2)
            swampPick.options shouldNotContain dual
        }
        swampPick.minSelections shouldBe 1
        swampPick.maxSelections shouldBe 1
        driver.submitCardSelection(me, listOf(s2))

        withClue("the Forest atom then has exactly one candidate left — forced, no second prompt") {
            driver.pendingDecision shouldBe null
        }
        driver.onBattlefield(s1) shouldBe true
        driver.onBattlefield(s2) shouldBe false
        driver.onBattlefield(dual) shouldBe false
    }

    test("two real choices are asked in cost order") {
        val (driver, me) = newDriver()
        val source = driver.putPermanentOnBattlefield(me, "Joint Sac Outlet")
        val s1 = driver.putLandOnBattlefield(me, "Joint Sac Swamp")
        val s2 = driver.putLandOnBattlefield(me, "Joint Sac Swamp")
        val f1 = driver.putLandOnBattlefield(me, "Joint Sac Forest")
        val f2 = driver.putLandOnBattlefield(me, "Joint Sac Forest")

        driver.submit(ActivateAbility(me, source, outletAbility)).error shouldBe null
        val swampPick = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        swampPick.options shouldContainExactlyInAnyOrder listOf(s1, s2)
        driver.submitCardSelection(me, listOf(s1))

        val forestPick = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        forestPick.options shouldContainExactlyInAnyOrder listOf(f1, f2)
        driver.submitCardSelection(me, listOf(f2))

        driver.pendingDecision shouldBe null
        listOf(s1, f2).forEach { driver.onBattlefield(it) shouldBe false }
        listOf(s2, f1).forEach { driver.onBattlefield(it) shouldBe true }
    }

    test("a pre-filled wrong assignment is rejected and nothing is sacrificed") {
        val (driver, me) = newDriver()
        val source = driver.putPermanentOnBattlefield(me, "Joint Sac Outlet")
        val s = driver.putLandOnBattlefield(me, "Joint Sac Swamp")
        val dual = driver.putLandOnBattlefield(me, "Joint Sac Swamp Forest")
        fun paying(ids: List<EntityId>) =
            ActivateAbility(me, source, outletAbility, costPayment = AdditionalCostPayment(sacrificedPermanents = ids))

        withClue("the dual can't pay both atoms") { driver.submit(paying(listOf(dual, dual))).error shouldNotBe null }
        withClue("segments follow cost order: a plain Swamp can't pay the Forest atom") {
            driver.submit(paying(listOf(dual, s))).error shouldNotBe null
        }
        withClue("one land for two atoms") { driver.submit(paying(listOf(dual))).error shouldNotBe null }
        driver.onBattlefield(s) shouldBe true
        driver.onBattlefield(dual) shouldBe true

        driver.submitSuccess(paying(listOf(s, dual)))
        driver.onBattlefield(s) shouldBe false
        driver.onBattlefield(dual) shouldBe false
    }

    test("activated from the graveyard: same disjoint gate, and it returns the card") {
        val (driver, me) = newDriver()
        val card = driver.putCardInGraveyard(me, "Joint Sac Recursion")
        driver.putLandOnBattlefield(me, "Joint Sac Swamp Forest")
        withClue("a lone dual doesn't make the graveyard ability payable") { driver.offered(me, card) shouldBe false }
        driver.submit(ActivateAbility(me, card, recursionAbility)).error shouldNotBe null

        val f = driver.putLandOnBattlefield(me, "Joint Sac Forest")
        driver.offered(me, card) shouldBe true
        driver.submitSuccess(ActivateAbility(me, card, recursionAbility))
        driver.pendingDecision shouldBe null
        driver.onBattlefield(f) shouldBe false
        driver.bothPass()
        driver.getHand(me) shouldContain card
    }
})
