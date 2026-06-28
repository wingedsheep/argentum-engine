package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsRevealedEvent
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.tmt.cards.SharkShredderKillerClone
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Shark Shredder, Killer Clone (TMT #73) — "Whenever Shark Shredder deals combat damage to a
 * player, put up to one target creature card from that player's graveyard onto the battlefield
 * under your control. It enters tapped and attacking that player."
 *
 * Reanimates the damaged player's graveyard creature under the attacker's control.
 */
class SharkShredderKillerCloneTest : FunSpec({

    val bear = card("Test Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }

    test("combat damage reanimates a creature from the damaged player's graveyard under your control") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(SharkShredderKillerClone, bear))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 30), startingLife = 20)
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)

        val shark = driver.putCreatureOnBattlefield(player, "Shark Shredder, Killer Clone")
        driver.removeSummoningSickness(shark)
        val bearInGy = driver.putCardInGraveyard(opponent, "Test Bear")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(player, listOf(shark), opponent)
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareBlockers(opponent, emptyMap())

        // Resolve the combat-damage trigger, choosing the Bear; stop the moment it's reanimated so
        // the assertions run while combat is still ongoing (end-of-combat removes attackers, CR 511.3).
        var guard = 0
        while (guard++ < 40) {
            val decision = driver.pendingDecision
            val holder = driver.state.priorityPlayerId
            if (decision is ChooseTargetsDecision) {
                driver.submitTargetSelection(decision.playerId, listOf(bearInGy))
            } else if (driver.state.stack.isNotEmpty()) {
                driver.bothPass()
            } else if (driver.findPermanent(player, "Test Bear") != null) {
                break // reanimation resolved, still mid-combat
            } else if (holder != null) {
                driver.passPriority(holder)
            } else {
                break
            }
        }

        // The Bear left the opponent's graveyard and is now a permanent under the player's control.
        driver.getGraveyard(opponent).contains(bearInGy) shouldBe false
        val reanimated = driver.findPermanent(player, "Test Bear")
        reanimated shouldNotBe null

        // "It enters tapped and attacking that player" — verify the placement, not just control.
        driver.state.getEntity(reanimated!!)?.has<TappedComponent>() shouldBe true
        val attacking = driver.state.getEntity(reanimated)?.get<AttackingComponent>()
        attacking shouldNotBe null
        attacking!!.defenderId shouldBe opponent

        // The reanimation move emits a CardsRevealedEvent whose revealer is the *controller* doing
        // the reanimation (the attacker), not the card's owner. With revealToSelf = false, that
        // surfaces the overlay to the opponent (whose card was stolen onto your board), not to you.
        // Anchoring on owner here would mislabel the move as the opponent reanimating their own card.
        val reveal = driver.events.filterIsInstance<CardsRevealedEvent>()
            .last { it.cardIds.contains(bearInGy) }
        reveal.revealingPlayerId shouldBe player
        reveal.revealToSelf shouldBe false
    }
})
