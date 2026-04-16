package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards.BrigidsCommand
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Phase 5: pre-chosen choose-N modal spells iterate every chosen mode in order,
 * each with its own per-mode targets pulled from SpellOnStackComponent.modeTargetsOrdered.
 *
 * The legacy ModalEffectExecutor (chosenModes.first only) dropped every mode but the first
 * when the spell landed on the stack with a non-empty chosenModes list. These tests pin the
 * new behavior against Brigid's Command (choose two of four) so regressions surface quickly.
 */
class ChooseNModalPreChosenTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        d.registerCard(BrigidsCommand)
        return d
    }

    fun nameOf(d: GameTestDriver, id: com.wingedsheep.sdk.model.EntityId): String? =
        d.state.getEntity(id)?.get<CardComponent>()?.name

    test("choose-2 executes every chosen mode in order with per-mode targets") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Forest" to 20, "Plains" to 20), startingLife = 20)
        val p1 = d.activePlayer!!
        val p2 = d.getOpponent(p1)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // P1 controls Centaur Courser (3/3). P2 controls Goblin Guide (2/1).
        // Choose modes [2, 3]: buff Centaur +3/+3 to 6/6, then Centaur fights Goblin.
        // Expected: Goblin dies (Centaur deals 6); Centaur takes 2 damage, survives.
        d.putCreatureOnBattlefield(p1, "Centaur Courser")
        d.putCreatureOnBattlefield(p2, "Goblin Guide")

        d.giveMana(p1, Color.WHITE, 1)
        d.giveMana(p1, Color.GREEN, 1)
        d.giveColorlessMana(p1, 1)
        val command = d.putCardInHand(p1, "Brigid's Command")

        val centaur = d.getCreatures(p1).first { nameOf(d, it) == "Centaur Courser" }
        val goblin = d.getCreatures(p2).first { nameOf(d, it) == "Goblin Guide" }

        val result = d.submit(CastSpell(
            playerId = p1,
            cardId = command,
            targets = listOf(ChosenTarget.Permanent(centaur), ChosenTarget.Permanent(centaur), ChosenTarget.Permanent(goblin)),
            chosenModes = listOf(2, 3),
            modeTargetsOrdered = listOf(
                listOf(ChosenTarget.Permanent(centaur)),                                  // mode 2: +3/+3 on Centaur
                listOf(ChosenTarget.Permanent(centaur), ChosenTarget.Permanent(goblin))   // mode 3: Centaur fights Goblin
            )
        ))
        result.isSuccess shouldBe true

        val spell = d.state.stack.first().let { id -> d.state.getEntity(id)?.get<SpellOnStackComponent>() }
        spell?.chosenModes shouldBe listOf(2, 3)
        spell?.modeTargetsOrdered?.size shouldBe 2
        spell?.modeTargetsOrdered?.get(0) shouldBe listOf(ChosenTarget.Permanent(centaur))
        spell?.modeTargetsOrdered?.get(1) shouldBe listOf(ChosenTarget.Permanent(centaur), ChosenTarget.Permanent(goblin))

        // Resolve — both modes should fire in order. No decision should prompt for mode selection.
        d.bothPass()
        d.pendingDecision shouldBe null

        // Goblin (2/1, dealt 6) dies; Centaur (3+3=6/6, dealt 2) survives.
        d.findPermanent(p2, "Goblin Guide") shouldBe null
        d.findPermanent(p1, "Centaur Courser") shouldNotBe null
    }

    test("mode 3 (fight) uses its own targets rather than spilling over from mode 2") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Forest" to 20, "Plains" to 20), startingLife = 20)
        val p1 = d.activePlayer!!
        val p2 = d.getOpponent(p1)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        d.putCreatureOnBattlefield(p1, "Centaur Courser")
        d.putCreatureOnBattlefield(p1, "Savannah Lions")
        d.putCreatureOnBattlefield(p2, "Goblin Guide")

        d.giveMana(p1, Color.WHITE, 1)
        d.giveMana(p1, Color.GREEN, 1)
        d.giveColorlessMana(p1, 1)
        val command = d.putCardInHand(p1, "Brigid's Command")

        val centaur = d.getCreatures(p1).first { nameOf(d, it) == "Centaur Courser" }
        val lions = d.getCreatures(p1).first { nameOf(d, it) == "Savannah Lions" }
        val goblin = d.getCreatures(p2).first()

        // Cast modes [2, 3]. Mode 2 buffs Centaur, Mode 3 fights Lions vs Goblin.
        // Lions (1/1) vs Goblin (2/1): both deal lethal to each other; both die.
        // Centaur (buffed to 6/6) is not touched by the fight.
        val result = d.submit(CastSpell(
            playerId = p1,
            cardId = command,
            targets = listOf(
                ChosenTarget.Permanent(centaur),
                ChosenTarget.Permanent(lions),
                ChosenTarget.Permanent(goblin)
            ),
            chosenModes = listOf(2, 3),
            modeTargetsOrdered = listOf(
                listOf(ChosenTarget.Permanent(centaur)),
                listOf(ChosenTarget.Permanent(lions), ChosenTarget.Permanent(goblin))
            )
        ))
        result.isSuccess shouldBe true

        d.bothPass()

        // Both Lions and Goblin died; Centaur survives.
        d.findPermanent(p1, "Savannah Lions") shouldBe null
        d.findPermanent(p2, "Goblin Guide") shouldBe null
        d.findPermanent(p1, "Centaur Courser") shouldNotBe null
    }

    test("mode whose single target has left the battlefield before resolution fizzles just that mode") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Forest" to 20, "Plains" to 20), startingLife = 20)
        val p1 = d.activePlayer!!
        val p2 = d.getOpponent(p1)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        d.putCreatureOnBattlefield(p1, "Savannah Lions")
        d.putCreatureOnBattlefield(p1, "Centaur Courser")
        d.putCreatureOnBattlefield(p2, "Goblin Guide")

        d.giveMana(p1, Color.WHITE, 1)
        d.giveMana(p1, Color.GREEN, 1)
        d.giveColorlessMana(p1, 1)
        val command = d.putCardInHand(p1, "Brigid's Command")

        val lions = d.getCreatures(p1).first { nameOf(d, it) == "Savannah Lions" }
        val centaur = d.getCreatures(p1).first { nameOf(d, it) == "Centaur Courser" }
        val goblin = d.getCreatures(p2).first()

        // Modes [2, 3]: mode 2 targets lions (+3/+3); mode 3 has Centaur fight Goblin.
        val castResult = d.submit(CastSpell(
            playerId = p1,
            cardId = command,
            targets = listOf(ChosenTarget.Permanent(lions), ChosenTarget.Permanent(centaur), ChosenTarget.Permanent(goblin)),
            chosenModes = listOf(2, 3),
            modeTargetsOrdered = listOf(
                listOf(ChosenTarget.Permanent(lions)),
                listOf(ChosenTarget.Permanent(centaur), ChosenTarget.Permanent(goblin))
            )
        ))
        castResult.isSuccess shouldBe true

        // Manually remove Lions from the battlefield (simulate it being destroyed between cast and
        // resolution — easier than scheduling another spell). Use replaceState to move it to the
        // graveyard zone directly.
        val beforeResolution = d.state
        val lionsOwner = beforeResolution.getEntity(lions)?.get<com.wingedsheep.engine.state.components.identity.OwnerComponent>()?.playerId ?: p1
        val battlefieldZoneKey = com.wingedsheep.engine.state.ZoneKey(p1, com.wingedsheep.sdk.core.Zone.BATTLEFIELD)
        val graveyardZoneKey = com.wingedsheep.engine.state.ZoneKey(lionsOwner, com.wingedsheep.sdk.core.Zone.GRAVEYARD)
        val relocated = beforeResolution
            .removeFromZone(battlefieldZoneKey, lions)
            .addToZone(graveyardZoneKey, lions)
        d.replaceState(relocated)

        // Now resolve — mode 2 should fizzle (Lions not on battlefield), mode 3 should still fire.
        d.bothPass()

        // Mode 3 resolved: Centaur (3/3) fights Goblin (2/1). Both die (Centaur takes 2, becomes 3/1;
        // actually 3 toughness with 2 damage survives. Goblin takes 3, dies). So Centaur survives,
        // Goblin dies.
        d.findPermanent(p2, "Goblin Guide") shouldBe null
        d.findPermanent(p1, "Centaur Courser") shouldNotBe null
    }
})
