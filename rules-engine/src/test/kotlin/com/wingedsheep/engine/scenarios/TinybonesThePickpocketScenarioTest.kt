package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.TinybonesThePickpocket
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe

/**
 * Tinybones, the Pickpocket (OTJ) — {B} Legendary Creature — Skeleton Rogue 1/1.
 *
 * "Deathtouch
 *  Whenever Tinybones deals combat damage to a player, you may cast target nonland permanent
 *  card from that player's graveyard, and mana of any type can be spent to cast that spell."
 *
 * Exercises the graveyard `withAnyManaType` may-play grant: after Tinybones connects, the
 * controller targets a nonland permanent card in the opponent's graveyard and gains permission to
 * cast it paying its (color-relaxed) cost from the graveyard. A land/instant in that graveyard is
 * not a legal target.
 */
class TinybonesThePickpocketScenarioTest : FunSpec({

    // A green creature card to be stolen from the opponent's graveyard. Its {G}{G} cost proves the
    // "mana of any type can be spent" relaxation — we pay it from Swamps (black mana).
    val greenCreature = card("Pickpocket Test Bear") {
        manaCost = "{G}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }
    // An instant — a nonpermanent card; must NOT be a legal target.
    val cheapInstant = card("Pickpocket Test Bolt") {
        manaCost = "{R}"
        typeLine = "Instant"
        oracleText = "Pickpocket Test Bolt deals 3 damage to any target."
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(TinybonesThePickpocket)
        driver.registerCard(greenCreature)
        driver.registerCard(cheapInstant)
        driver.initMirrorMatch(Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.castActionsFor(playerId: EntityId, cardId: EntityId): List<CastSpell> =
        LegalActionEnumerator.create(cardRegistry)
            .enumerate(state, playerId)
            .mapNotNull { it.action as? CastSpell }
            .filter { it.cardId == cardId }

    test("combat damage lets you cast a targeted nonland permanent card from the opponent's graveyard with any mana") {
        val driver = newDriver()
        val me = driver.player1
        val opponent = driver.player2

        // Tinybones (mine) attacks; a green creature sits in the opponent's graveyard.
        val tinybones = driver.putCreatureOnBattlefield(me, "Tinybones, the Pickpocket")
        driver.removeSummoningSickness(tinybones)
        val stolenCard = driver.putCardInGraveyard(opponent, "Pickpocket Test Bear")
        // Off-color (black) mana sources: tapping these for the {G}{G} cost proves "mana of any
        // type can be spent" — the permission relaxes the colored pips.
        repeat(2) { driver.putLandOnBattlefield(me, "Swamp") }

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(tinybones), opponent)

        // Drive combat damage + the Tinybones trigger to resolution. The trigger's target is the
        // green creature in the opponent's graveyard; choose it when the targeting decision appears.
        // Stop once the may-play permission has been granted.
        run {
            repeat(20) {
                if (driver.state.mayPlayPermissions.any { stolenCard in it.cardIds }) return@run
                when (driver.pendingDecision) {
                    is ChooseTargetsDecision -> driver.submitTargetSelection(me, listOf(stolenCard))
                    null -> driver.bothPass()
                    else -> driver.autoResolveDecision()
                }
            }
        }

        // The trigger granted a may-play permission for the stolen card (still in the graveyard).
        driver.state.mayPlayPermissions.any { stolenCard in it.cardIds } shouldBe true

        // Advance to my postcombat main phase, where this sorcery-speed creature can be cast.
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        // The granted permission relaxes the colored cost so any mana type pays it.
        driver.state.mayPlayPermissions.single { stolenCard in it.cardIds }.withAnyManaType shouldBe true

        // The card is now castable from the opponent's graveyard via the granted permission.
        driver.castActionsFor(me, stolenCard).isNotEmpty().shouldBeTrue()

        // Tap the Swamps (black mana) to pay the {G}{G} cost — possible only because mana of any
        // type can be spent to cast it.
        driver.castSpell(me, stolenCard).isSuccess shouldBe true
        repeat(4) {
            if (driver.pendingDecision != null) driver.autoResolveDecision() else driver.bothPass()
            if (driver.state.getZone(me, Zone.BATTLEFIELD).contains(stolenCard)) return@repeat
        }

        // The stolen creature resolved onto my battlefield (I control it).
        driver.state.getZone(me, Zone.BATTLEFIELD).contains(stolenCard).shouldBeTrue()
        driver.state.getEntity(stolenCard)?.get<CardComponent>()?.name shouldBe "Pickpocket Test Bear"
    }

    test("a nonpermanent card in the opponent's graveyard is not a legal target") {
        val driver = newDriver()
        val me = driver.player1
        val opponent = driver.player2

        val tinybones = driver.putCreatureOnBattlefield(me, "Tinybones, the Pickpocket")
        driver.removeSummoningSickness(tinybones)
        // Only an instant in the opponent's graveyard — no nonland permanent card.
        driver.putCardInGraveyard(opponent, "Pickpocket Test Bolt")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(tinybones), opponent)
        driver.bothPass() // combat damage

        // With no legal target, the triggered ability is removed (no target-selection decision
        // surfaces) and nothing becomes castable from the opponent's graveyard.
        driver.passPriorityUntil(Step.END)

        val instantId = driver.getGraveyard(opponent).first { id ->
            driver.state.getEntity(id)?.get<CardComponent>()?.name == "Pickpocket Test Bolt"
        }
        driver.castActionsFor(me, instantId) shouldBe emptyList()
    }
})
