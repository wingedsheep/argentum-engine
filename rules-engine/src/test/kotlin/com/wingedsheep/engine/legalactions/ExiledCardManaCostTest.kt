package com.wingedsheep.engine.legalactions

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.costs.CostAtom
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * `AbilityCost.ExiledCardManaCost` — "exile a creature card from your graveyard **and pay its mana
 * cost**" (Back from the Brink). The price is the printed cost of the card the payer picks while
 * determining the total cost (CR 601.2f), so these tests pin:
 *  - the legal-action picker offers only the candidates whose cost is affordable, and the ability
 *    is unaffordable when none is;
 *  - a bare activation (the AI's path) asks which card even when only one qualifies, offering only
 *    affordable ones;
 *  - the price is the chosen card's cost, merged with any mana the rest of the cost names;
 *  - an {X} in that cost is 0 (CR 107.3h), and a card with no mana cost is never payable (CR 118.6).
 */
class ExiledCardManaCostTest : FunSpec({

    // "{1}, Exile a creature card from your graveyard and pay its mana cost: You gain 1 life."
    val taxedEngine = card("Test Taxed Brink") {
        manaCost = "{3}"
        typeLine = "Artifact"
        oracleText = "{1}, Exile a creature card from your graveyard and pay its mana cost: You gain 1 life."
        activatedAbility {
            cost = AbilityCost.Composite(
                listOf(
                    AbilityCost.Atom(CostAtom.Mana(com.wingedsheep.sdk.core.ManaCost.parse("{1}"))),
                    AbilityCost.Atom(CostAtom.ExileFrom(Zone.GRAVEYARD, GameObjectFilter.Creature, 1)),
                    AbilityCost.ExiledCardManaCost,
                )
            )
            effect = Effects.GainLife(1)
        }
    }

    // "Exile a creature card from your graveyard and pay its mana cost: You gain 1 life."
    val plainEngine = card("Test Plain Brink") {
        manaCost = "{3}"
        typeLine = "Artifact"
        oracleText = "Exile a creature card from your graveyard and pay its mana cost: You gain 1 life."
        activatedAbility {
            cost = Costs.ExileAndPayItsManaCost(GameObjectFilter.Creature)
            effect = Effects.GainLife(1)
        }
    }

    val xCreature = card("Test X Ooze") {
        manaCost = "{X}{G}"
        typeLine = "Creature — Ooze"
        power = 1
        toughness = 1
    }

    val manalessCreature = card("Test Manaless Dryad") {
        typeLine = "Creature — Dryad"
        power = 1
        toughness = 1
    }

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(taxedEngine, plainEngine, xCreature, manalessCreature))
        d.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.offer(playerId: EntityId, sourceId: EntityId): LegalAction =
        legalActions(playerId).single {
            it.actionType == "ActivateAbility" && (it.action as? ActivateAbility)?.sourceId == sourceId
        }

    fun GameTestDriver.poolTotal(playerId: EntityId): Int =
        state.getEntity(playerId)?.get<ManaPoolComponent>()?.let { it.green + it.colorless } ?: 0

    test("the picker offers only the creature cards whose mana cost you can pay") {
        val d = driver()
        val me = d.activePlayer!!
        val engine = d.putPermanentOnBattlefield(me, "Test Plain Brink")
        val bears = d.putCardInGraveyard(me, "Grizzly Bears") // {1}{G}
        d.putCardInGraveyard(me, "Force of Nature") // {3}{G}{G}
        d.giveMana(me, Color.GREEN, 2)

        val offer = d.offer(me, engine)
        offer.affordable shouldBe true
        offer.additionalCostInfo?.validExileTargets shouldBe listOf(bears)
    }

    test("with no affordable creature card the ability is offered greyed out") {
        val d = driver()
        val me = d.activePlayer!!
        val engine = d.putPermanentOnBattlefield(me, "Test Plain Brink")
        d.putCardInGraveyard(me, "Force of Nature")
        d.giveMana(me, Color.GREEN, 2)

        d.offer(me, engine).affordable shouldBe false
    }

    test("the rest of the cost's mana is priced on top of the card's") {
        val d = driver()
        val me = d.activePlayer!!
        val engine = d.putPermanentOnBattlefield(me, "Test Taxed Brink")
        d.putCardInGraveyard(me, "Grizzly Bears") // {1}{G} + {1} = 3
        d.giveMana(me, Color.GREEN, 2)
        withClue("{1} plus Grizzly Bears' {1}{G} is three mana") {
            d.offer(me, engine).affordable shouldBe false
        }
        d.giveMana(me, Color.GREEN, 1)
        d.offer(me, engine).affordable shouldBe true
    }

    test("activating with a chosen card pays exactly that card's cost, merged with the rest") {
        val d = driver()
        val me = d.activePlayer!!
        val engine = d.putPermanentOnBattlefield(me, "Test Taxed Brink")
        val bears = d.putCardInGraveyard(me, "Grizzly Bears")
        d.giveMana(me, Color.GREEN, 4)

        d.submitSuccess(
            ActivateAbility(
                playerId = me, sourceId = engine, abilityId = taxedEngine.activatedAbilities.single().id,
                costPayment = AdditionalCostPayment(exiledCards = listOf(bears)),
            )
        )
        withClue("three of the four green were spent: {1} + {1}{G}") { d.poolTotal(me) shouldBe 1 }
        d.getExile(me) shouldBe listOf(bears)
    }

    test("a bare activation asks which card even when one qualifies, offering only affordable ones") {
        val d = driver()
        val me = d.activePlayer!!
        val engine = d.putPermanentOnBattlefield(me, "Test Plain Brink")
        val bears = d.putCardInGraveyard(me, "Grizzly Bears")
        d.putCardInGraveyard(me, "Force of Nature")
        d.giveMana(me, Color.GREEN, 2)

        d.submit(ActivateAbility(me, engine, plainEngine.activatedAbilities.single().id)).error shouldBe null
        val decision = d.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        decision.options shouldBe listOf(bears)

        d.submitCardSelection(me, listOf(bears))
        d.state.pendingDecision shouldBe null
        d.poolTotal(me) shouldBe 0
        d.getExile(me) shouldBe listOf(bears)
        d.getGraveyardCardNames(me) shouldBe listOf("Force of Nature")
    }

    test("a bare activation with nothing affordable is refused rather than paid for nothing") {
        val d = driver()
        val me = d.activePlayer!!
        val engine = d.putPermanentOnBattlefield(me, "Test Plain Brink")
        d.putCardInGraveyard(me, "Force of Nature")
        d.giveMana(me, Color.GREEN, 2)

        d.submit(ActivateAbility(me, engine, plainEngine.activatedAbilities.single().id)).error shouldNotBe null
        d.getGraveyardCardNames(me) shouldBe listOf("Force of Nature")
    }

    test("an {X} in the exiled card's cost is 0 (CR 107.3h)") {
        val d = driver()
        val me = d.activePlayer!!
        val engine = d.putPermanentOnBattlefield(me, "Test Plain Brink")
        val ooze = d.putCardInGraveyard(me, "Test X Ooze")
        d.giveMana(me, Color.GREEN, 1)

        d.offer(me, engine).additionalCostInfo?.validExileTargets shouldBe listOf(ooze)
        d.submitSuccess(
            ActivateAbility(
                playerId = me, sourceId = engine, abilityId = plainEngine.activatedAbilities.single().id,
                costPayment = AdditionalCostPayment(exiledCards = listOf(ooze)),
            )
        )
        d.poolTotal(me) shouldBe 0
    }

    test("a creature card with no mana cost can't be exiled for it (CR 118.6)") {
        val d = driver()
        val me = d.activePlayer!!
        val engine = d.putPermanentOnBattlefield(me, "Test Plain Brink")
        val dryad = d.putCardInGraveyard(me, "Test Manaless Dryad")
        val bears = d.putCardInGraveyard(me, "Grizzly Bears")
        d.giveMana(me, Color.GREEN, 5)

        d.offer(me, engine).additionalCostInfo?.validExileTargets
            .orEmpty() shouldContainExactlyInAnyOrder listOf(bears)
        d.submit(
            ActivateAbility(
                playerId = me, sourceId = engine, abilityId = plainEngine.activatedAbilities.single().id,
                costPayment = AdditionalCostPayment(exiledCards = listOf(dryad)),
            )
        ).error shouldNotBe null
        d.getGraveyard(me) shouldContainExactlyInAnyOrder listOf(dryad, bears)
    }
})
