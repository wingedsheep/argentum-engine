package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.LaughingJasperFlint
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Laughing Jasper Flint — {1}{B}{R} Legendary Creature — Lizard Rogue 4/3.
 *
 * - "Creatures you control but don't own are Mercenaries in addition to their other types."
 * - "At the beginning of your upkeep, exile the top X cards of target opponent's library, where X
 *   is the number of outlaws you control. Until end of turn, you may cast spells from among those
 *   cards, and mana of any type can be spent to cast those spells."
 */
class LaughingJasperFlintScenarioTest : FunSpec({

    // A plain non-outlaw creature to be stolen / re-typed.
    val PlainBear = card("Jasper Test Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }
    // A Pirate (outlaw) to bump the outlaw count.
    val PirateGoon = card("Jasper Test Pirate") {
        manaCost = "{1}{B}"
        typeLine = "Creature — Human Pirate"
        power = 2
        toughness = 1
    }

    fun createDriver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        d.registerCard(LaughingJasperFlint)
        d.registerCard(PlainBear)
        d.registerCard(PirateGoon)
        return d
    }

    test("a creature you control but an opponent owns becomes a Mercenary") {
        val d = createDriver()
        d.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = d.activePlayer!!
        val opponent = d.state.turnOrder.first { it != me }

        d.putCreatureOnBattlefield(me, "Laughing Jasper Flint")
        // A Bear I control but opponent owns (control-changed): controller = me, owner = opponent.
        // Owner is read from CardComponent.ownerId, so update that alongside OwnerComponent.
        val stolenBear = d.putCreatureOnBattlefield(me, "Jasper Test Bear")
        d.replaceState(d.state.updateEntity(stolenBear) { c ->
            val card = c.get<CardComponent>()!!
            c.with(card.copy(ownerId = opponent)).with(OwnerComponent(opponent))
        })

        val projected = StateProjector().project(d.state)
        projected.hasSubtype(stolenBear, "Mercenary") shouldBe true
        // It keeps its original subtype too ("in addition to").
        projected.hasSubtype(stolenBear, "Bear") shouldBe true
    }

    test("a creature you control AND own does not gain Mercenary") {
        val d = createDriver()
        d.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = d.activePlayer!!

        d.putCreatureOnBattlefield(me, "Laughing Jasper Flint")
        val ownBear = d.putCreatureOnBattlefield(me, "Jasper Test Bear")

        StateProjector().project(d.state).hasSubtype(ownBear, "Mercenary") shouldBe false
    }

    test("upkeep ability exiles top X of target opponent's library where X = outlaws you control") {
        val d = createDriver()
        d.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        val me = d.activePlayer!!
        val opponent = d.state.turnOrder.first { it != me }

        // Jasper (Rogue) + a Pirate = 2 outlaws I control.
        d.putCreatureOnBattlefield(me, "Laughing Jasper Flint")
        d.putCreatureOnBattlefield(me, "Jasper Test Pirate")

        // Seed the opponent's library top with identifiable cards.
        d.putCardOnTopOfLibrary(opponent, "Jasper Test Bear")
        d.putCardOnTopOfLibrary(opponent, "Jasper Test Pirate")
        d.putCardOnTopOfLibrary(opponent, "Jasper Test Bear")

        val opponentExileBefore = d.getExile(opponent).size

        // Advance to my next upkeep (turn 3) so the trigger fires on my beginning-of-upkeep.
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.passPriorityUntil(Step.UPKEEP, maxPasses = 300)   // opponent's upkeep
        d.passPriorityUntil(Step.UPKEEP, maxPasses = 300)   // back to my upkeep — trigger fires

        // Resolve the upkeep trigger. Single opponent → "target opponent" auto-targets; pass
        // priority to let the triggered ability resolve. If any decision surfaces, auto-resolve it.
        repeat(6) {
            if (d.state.pendingDecision != null) d.autoResolveDecision() else d.bothPass()
            if (d.getExile(opponent).size > opponentExileBefore) return@repeat
        }

        // X = 2 outlaws → 2 cards exiled from the opponent's library.
        d.getExile(opponent).size shouldBe (opponentExileBefore + 2)
    }
})
