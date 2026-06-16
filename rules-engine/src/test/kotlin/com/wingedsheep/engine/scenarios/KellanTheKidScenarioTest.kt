package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.KellanTheKid
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.MayCastSelfFromZones
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Kellan, the Kid.
 *
 * Kellan, the Kid ({G}{W}{U}): Legendary Creature — Human Faerie Rogue, 3/3, Flying, lifelink.
 * "Whenever you cast a spell from anywhere other than your hand, you may cast a permanent spell
 *  with equal or lesser mana value from your hand without paying its mana cost. If you don't, you
 *  may put a land card from your hand onto the battlefield."
 *
 * Exercises the new SpellCastPredicate.CastFromZoneOtherThan trigger predicate and the
 * TRIGGERING_SPELL_MANA_VALUE dynamic amount.
 */
class KellanTheKidScenarioTest : FunSpec({

    // A {2}{R} sorcery (mana value 3) castable from the graveyard — the "cast from anywhere
    // other than your hand" enabler.
    val graveyardSpell = card("Kellan Test Graveyard Spell") {
        manaCost = "{2}{R}"
        typeLine = "Sorcery"
        oracleText = "Test sorcery castable from the graveyard."
        spell { effect = Effects.GainLife(1) }
        staticAbility { ability = MayCastSelfFromZones(zones = listOf(Zone.GRAVEYARD)) }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(KellanTheKid))
        driver.registerCard(graveyardSpell)
        return driver
    }

    test("casting a spell from the graveyard lets you free-cast a permanent of equal or lesser MV") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 30, "Forest" to 10))
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(player, "Kellan, the Kid")

        // Centaur Courser is a {2}{G} (MV 3) permanent — eligible (3 <= 3).
        driver.putCardInHand(player, "Centaur Courser")
        val gySpell = driver.putCardInGraveyard(player, "Kellan Test Graveyard Spell")
        repeat(3) { driver.putLandOnBattlefield(player, "Mountain") }

        val creaturesBefore = driver.getCreatures(player).size

        // Cast the sorcery from the graveyard — Kellan's trigger fires.
        driver.submit(
            CastSpell(playerId = player, cardId = gySpell, paymentStrategy = PaymentStrategy.AutoPay),
        )
        // Resolve the trigger (it goes on top of the sorcery).
        driver.passPriority(player)
        driver.passPriority(opponent)

        // Trigger resolution pauses to choose the permanent to free-cast.
        driver.submitCardSelection(player, listOf(driver.findCardInHand(player, "Centaur Courser")!!))
        // Let any free-cast / follow-up settle.
        driver.bothPass()

        // Centaur Courser was cast for free and is now a creature on the battlefield.
        driver.getCreatures(player).any {
            driver.state.getEntity(it)
                ?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name == "Centaur Courser"
        } shouldBe true
        driver.getCreatures(player).size shouldBe creaturesBefore + 1
    }

    test("declining the free cast lets you put a land from hand instead") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 30, "Forest" to 10))
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(player, "Kellan, the Kid")

        driver.putCardInHand(player, "Centaur Courser")
        val forest = driver.putCardInHand(player, "Forest")
        val gySpell = driver.putCardInGraveyard(player, "Kellan Test Graveyard Spell")
        repeat(3) { driver.putLandOnBattlefield(player, "Mountain") }

        driver.submit(
            CastSpell(playerId = player, cardId = gySpell, paymentStrategy = PaymentStrategy.AutoPay),
        )
        driver.passPriority(player)
        driver.passPriority(opponent)

        // Decline the free cast (choose no permanent).
        driver.submitCardSelection(player, emptyList())
        // Now the "if you don't" branch offers putting a land — choose the Forest.
        driver.submitCardSelection(player, listOf(forest))
        driver.bothPass()

        (forest in driver.state.getZone(ZoneKey(player, Zone.BATTLEFIELD))) shouldBe true
    }

    test("casting a spell from hand does NOT trigger Kellan") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 30, "Forest" to 10))
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(player, "Kellan, the Kid")
        driver.putCardInHand(player, "Centaur Courser")
        val handSpell = driver.putCardInHand(player, "Kellan Test Graveyard Spell")
        repeat(3) { driver.putLandOnBattlefield(player, "Mountain") }

        val creaturesBefore = driver.getCreatures(player).size

        // Cast the sorcery from hand — no trigger, so no free cast.
        driver.submit(
            CastSpell(playerId = player, cardId = handSpell, paymentStrategy = PaymentStrategy.AutoPay),
        )
        driver.bothPass()

        // No new permanent entered from a free cast.
        driver.getCreatures(player).size shouldBe creaturesBefore
    }
})
