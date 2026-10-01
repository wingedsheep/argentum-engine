package com.wingedsheep.engine

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.emerge
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantEmergeToOwnSpells
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Granted emerge ([GrantEmergeToOwnSpells], Herigast's "each creature spell you cast has emerge.
 * The emerge cost is equal to its mana cost"): the spell's own mana cost becomes its emerge cost,
 * reduced by the sacrificed creature's mana value on the generic portion only (CR 702.119a).
 * Card-level coverage lives in `HerigastEruptingNullkiteScenarioTest`.
 */
class GrantedEmergeTest : FunSpec({

    val Granter = card("Emerge Granter") {
        manaCost = "{3}{R}"
        typeLine = "Creature — Eldrazi"
        power = 3
        toughness = 3
        oracleText = "Each creature spell you cast has emerge. The emerge cost is equal to its mana cost."
        staticAbility { ability = GrantEmergeToOwnSpells(GameObjectFilter.Creature) }
    }

    val BigBeast = card("Granted Emerge Beast") {
        manaCost = "{5}{G}"
        typeLine = "Creature — Beast"
        power = 6
        toughness = 6
    }

    val GreenPips = card("Granted Emerge Pips") {
        manaCost = "{G}{G}{G}"
        typeLine = "Creature — Elf"
        power = 3
        toughness = 3
    }

    val PrintedEmerger = card("Granted Emerge Printed") {
        manaCost = "{7}{G}"
        typeLine = "Creature — Horror"
        power = 7
        toughness = 7
        oracleText = "Emerge {4}{G}"
        emerge("{4}{G}")
    }

    val Sorcery = card("Granted Emerge Sorcery") {
        manaCost = "{3}{G}"
        typeLine = "Sorcery"
        spell { effect = com.wingedsheep.sdk.dsl.Effects.DrawCards(1) }
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(Granter, BigBeast, GreenPips, PrintedEmerger, Sorcery))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 60), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun emergeOffers(driver: GameTestDriver, player: EntityId, cardId: EntityId) =
        driver.legalActions(player).filter {
            val action = it.action
            action is CastSpell && action.cardId == cardId && action.alternativeCostType == AlternativeCostType.EMERGE
        }

    fun castEmerge(driver: GameTestDriver, player: EntityId, cardId: EntityId, sacrifice: EntityId) =
        driver.submit(
            CastSpell(
                playerId = player,
                cardId = cardId,
                useAlternativeCost = true,
                alternativeCostType = AlternativeCostType.EMERGE,
                additionalCostPayment = AdditionalCostPayment(sacrificedPermanents = listOf(sacrifice))
            )
        )

    test("a creature spell gains emerge at its own mana cost, reduced by the sacrifice's mana value") {
        val driver = newDriver()
        val player = driver.activePlayer!!
        driver.putCreatureOnBattlefield(player, "Emerge Granter")
        val beast = driver.putCardInHand(player, "Granted Emerge Beast")
        val bears = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        repeat(4) { driver.giveMana(player, Color.GREEN) }

        val offer = emergeOffers(driver, player, beast).single()
        offer.manaCostString shouldBe "{5}{G}"
        withClue("{5}{G} less the Bears' mana value 2 is {3}{G}") {
            offer.additionalCostInfo!!.costAfterSacrifice!![bears] shouldBe "{3}{G}"
        }

        castEmerge(driver, player, beast, bears).outcome shouldBe Outcome.Done
        driver.getGraveyard(player) shouldContain bears
        driver.bothPass()
        driver.state.getBattlefield() shouldContain beast
    }

    test("the reduction never touches colored pips") {
        val driver = newDriver()
        val player = driver.activePlayer!!
        driver.putCreatureOnBattlefield(player, "Emerge Granter")
        val pips = driver.putCardInHand(player, "Granted Emerge Pips")
        val bears = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        repeat(3) { driver.giveMana(player, Color.GREEN) }

        emergeOffers(driver, player, pips).single().additionalCostInfo!!.costAfterSacrifice!![bears] shouldBe "{G}{G}{G}"
    }

    test("the granter itself may be sacrificed to pay a granted emerge cost") {
        val driver = newDriver()
        val player = driver.activePlayer!!
        val granter = driver.putCreatureOnBattlefield(player, "Emerge Granter")
        val beast = driver.putCardInHand(player, "Granted Emerge Beast")
        repeat(2) { driver.giveMana(player, Color.GREEN) }

        val info = emergeOffers(driver, player, beast).single().additionalCostInfo!!
        info.validSacrificeTargets shouldContainExactlyInAnyOrder listOf(granter)
        info.costAfterSacrifice!![granter] shouldBe "{1}{G}"

        castEmerge(driver, player, beast, granter).outcome shouldBe Outcome.Done
        driver.getGraveyard(player) shouldContain granter
        driver.bothPass()
        driver.state.getBattlefield() shouldContain beast
    }

    test("noncreature spells, and spells without a granter you control, get no emerge") {
        val driver = newDriver()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        driver.putCreatureOnBattlefield(opponent, "Emerge Granter")
        val beast = driver.putCardInHand(player, "Granted Emerge Beast")
        val bears = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        repeat(6) { driver.giveMana(player, Color.GREEN) }

        withClue("an opponent's granter grants nothing to you") {
            emergeOffers(driver, player, beast) shouldBe emptyList()
        }
        driver.submitExpectFailure(
            CastSpell(
                playerId = player,
                cardId = beast,
                useAlternativeCost = true,
                alternativeCostType = AlternativeCostType.EMERGE,
                additionalCostPayment = AdditionalCostPayment(sacrificedPermanents = listOf(bears))
            )
        )
        driver.state.getBattlefield() shouldContain bears

        driver.putCreatureOnBattlefield(player, "Emerge Granter")
        val sorcery = driver.putCardInHand(player, "Granted Emerge Sorcery")
        withClue("the grant covers creature spells only") {
            emergeOffers(driver, player, sorcery) shouldBe emptyList()
        }
    }

    test("a printed emerge wins over the granted one") {
        val driver = newDriver()
        val player = driver.activePlayer!!
        driver.putCreatureOnBattlefield(player, "Emerge Granter")
        val printed = driver.putCardInHand(player, "Granted Emerge Printed")
        val bears = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        repeat(3) { driver.giveMana(player, Color.GREEN) }

        val offer = emergeOffers(driver, player, printed).single()
        offer.manaCostString shouldBe "{4}{G}"
        offer.additionalCostInfo!!.costAfterSacrifice!![bears] shouldBe "{2}{G}"
        driver.state.getBattlefield() shouldNotContain printed
    }
})
