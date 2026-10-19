package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.neo.cards.TameshiRealityArchitect
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tameshi, Reality Architect — "Whenever one or more noncreature permanents are returned to hand,
 * draw a card. This ability triggers only once each turn. / {X}{W}, Return a land you control to its
 * owner's hand: Return target artifact or enchantment card with mana value X or less from your
 * graveyard to the battlefield. Activate only as a sorcery."
 *
 * The land returned as a cost is a noncreature permanent going to hand, so it fires the draw; the
 * once-each-turn cap stops a second activation that turn from drawing again; and the announced X
 * bounds the graveyard target's mana value.
 */
class TameshiRealityArchitectScenarioTest : FunSpec({

    val ench2 = card("Test MV2 Enchantment") { manaCost = "{1}{W}"; typeLine = "Enchantment" }
    val art3 = card("Test MV3 Artifact") { manaCost = "{3}"; typeLine = "Artifact" }

    val abilityId = TameshiRealityArchitect.activatedAbilities.single().id

    fun setup(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(TameshiRealityArchitect, ench2, art3))
        d.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.activate(you: EntityId, tameshi: EntityId, x: Int, land: EntityId, target: EntityId) =
        submit(
            ActivateAbility(
                playerId = you,
                sourceId = tameshi,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Card(target, you, Zone.GRAVEYARD)),
                costPayment = AdditionalCostPayment(bouncedPermanents = listOf(land)),
                xValue = x,
                paymentStrategy = PaymentStrategy.FromPool,
            )
        )

    test("returning a land as the cost draws a card, and the target comes back") {
        val d = setup()
        val you = d.activePlayer!!
        val tameshi = d.putCreatureOnBattlefield(you, "Tameshi, Reality Architect")
        val land = d.putLandOnBattlefield(you, "Island")
        val gy2 = d.putCardInGraveyard(you, "Test MV2 Enchantment")
        val handBefore = d.getHandSize(you)
        d.giveMana(you, Color.WHITE, 1)
        d.giveColorlessMana(you, 2)

        d.activate(you, tameshi, x = 2, land = land, target = gy2).error shouldBe null
        d.bothPass() // the draw trigger
        d.bothPass() // the ability

        withClue("the bounced Island plus one drawn card") {
            d.getHandSize(you) shouldBe handBefore + 2
        }
        d.state.getZone(ZoneKey(you, Zone.BATTLEFIELD)).contains(gy2) shouldBe true
    }

    test("the draw triggers only once each turn") {
        val d = setup()
        val you = d.activePlayer!!
        val tameshi = d.putCreatureOnBattlefield(you, "Tameshi, Reality Architect")
        val land1 = d.putLandOnBattlefield(you, "Island")
        val land2 = d.putLandOnBattlefield(you, "Island")
        val first = d.putCardInGraveyard(you, "Test MV2 Enchantment")
        val second = d.putCardInGraveyard(you, "Test MV2 Enchantment")
        val handBefore = d.getHandSize(you)

        d.giveMana(you, Color.WHITE, 1)
        d.giveColorlessMana(you, 2)
        d.activate(you, tameshi, x = 2, land = land1, target = first).error shouldBe null
        d.bothPass()
        d.bothPass()

        d.giveMana(you, Color.WHITE, 1)
        d.giveColorlessMana(you, 2)
        d.activate(you, tameshi, x = 2, land = land2, target = second).error shouldBe null
        d.bothPass()

        withClue("two Islands returned, but only one card drawn this turn") {
            d.getHandSize(you) shouldBe handBefore + 3
        }
        d.state.getZone(ZoneKey(you, Zone.BATTLEFIELD)).contains(second) shouldBe true
    }

    test("a target with mana value greater than X is rejected") {
        val d = setup()
        val you = d.activePlayer!!
        val tameshi = d.putCreatureOnBattlefield(you, "Tameshi, Reality Architect")
        val land = d.putLandOnBattlefield(you, "Island")
        val gy3 = d.putCardInGraveyard(you, "Test MV3 Artifact")
        d.giveMana(you, Color.WHITE, 1)
        d.giveColorlessMana(you, 2)

        d.activate(you, tameshi, x = 2, land = land, target = gy3).error shouldNotBe null
        d.state.getZone(ZoneKey(you, Zone.BATTLEFIELD)).contains(land) shouldBe true
        d.state.getZone(ZoneKey(you, Zone.GRAVEYARD)).contains(gy3) shouldBe true
    }
})
