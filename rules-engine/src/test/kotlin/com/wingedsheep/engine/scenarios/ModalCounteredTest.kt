package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.khans.cards.Cancel
import com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards.BrigidsCommand
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests C1 from [`backlog/modal-cast-time-choices-plan.md`]: once modes and targets are
 * chosen at cast time (601.2b–c), an opponent holding priority must be able to counter
 * the choose-N spell before it resolves. Without this, the whole point of the cast-time
 * flow — making choose-N spells visible so opponents can respond knowingly — would fail.
 *
 * Scenario: P1 casts Brigid's Command with modes [2, 3] pre-chosen. Priority passes to
 * P2. P2 casts Cancel targeting the Command. Both resolve; the Command is countered and
 * no mode effects fire.
 */
class ModalCounteredTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        d.registerCard(BrigidsCommand)
        d.registerCard(Cancel)
        return d
    }

    fun nameOf(d: GameTestDriver, id: EntityId): String? =
        d.state.getEntity(id)?.get<CardComponent>()?.name

    test("C1 — P2 counters P1's choose-N Brigid's Command; no modes resolve") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Forest" to 20, "Plains" to 20))
        val p1 = d.activePlayer!!
        val p2 = d.getOpponent(p1)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // P1 controls a buffable creature; P2 controls a fightable creature. With modes
        // [2, 3] pre-chosen, Brigid's Command would buff the Courser and have it fight the
        // Goblin — unless countered.
        d.putCreatureOnBattlefield(p1, "Centaur Courser")
        d.putCreatureOnBattlefield(p2, "Goblin Guide")

        // P1's cast cost: {1}{G}{W}.
        d.giveMana(p1, Color.WHITE, 1)
        d.giveMana(p1, Color.GREEN, 1)
        d.giveColorlessMana(p1, 1)
        val command = d.putCardInHand(p1, "Brigid's Command")

        // P2's counter cost: {1}{U}{U}.
        d.giveMana(p2, Color.BLUE, 2)
        d.giveColorlessMana(p2, 1)
        val cancel = d.putCardInHand(p2, "Cancel")

        val centaur = d.getCreatures(p1).first { nameOf(d, it) == "Centaur Courser" }
        val goblin = d.getCreatures(p2).first { nameOf(d, it) == "Goblin Guide" }

        d.submit(
            CastSpell(
                playerId = p1,
                cardId = command,
                targets = listOf(
                    ChosenTarget.Permanent(centaur),
                    ChosenTarget.Permanent(centaur),
                    ChosenTarget.Permanent(goblin)
                ),
                chosenModes = listOf(2, 3),
                modeTargetsOrdered = listOf(
                    listOf(ChosenTarget.Permanent(centaur)),
                    listOf(ChosenTarget.Permanent(centaur), ChosenTarget.Permanent(goblin))
                )
            )
        ).isSuccess shouldBe true

        // Command sits on the stack; per Rule 117.3c the active player retains priority
        // immediately after casting a spell. P1 passes so P2 gets priority.
        d.state.stack.size shouldBe 1
        d.state.priorityPlayerId shouldBe p1
        d.passPriority(p1)
        d.state.priorityPlayerId shouldBe p2

        // P2 counters it targeting the Command spell itself.
        val commandOnStackId = d.state.stack.first()
        d.submit(
            CastSpell(
                playerId = p2,
                cardId = cancel,
                targets = listOf(ChosenTarget.Spell(commandOnStackId))
            )
        ).isSuccess shouldBe true

        // Stack order: Cancel on top, Command below.
        d.state.stack.size shouldBe 2
        d.state.stack.last() shouldNotBe commandOnStackId

        // Both resolve in LIFO order: Cancel first, then Command (which should be countered
        // rather than resolving). `bothPass` handles each priority round.
        d.bothPass()   // resolve Cancel
        d.bothPass()   // resolve / validate Command (countered, no effects)

        // Stack is empty.
        d.state.stack.size shouldBe 0

        // Neither mode's effect fired: Courser has no +3/+3 (still on battlefield with
        // baseline stats), Goblin was never fought, no creature in the graveyard from this
        // interaction (the Courser and Goblin are both still on their respective battlefields).
        d.findPermanent(p1, "Centaur Courser") shouldNotBe null
        d.findPermanent(p2, "Goblin Guide") shouldNotBe null

        // Command ended up in P1's graveyard (countered spells go to the graveyard per 701.5b).
        val commandInGrave = d.getGraveyard(p1).any { id ->
            nameOf(d, id) == "Brigid's Command"
        }
        commandInGrave shouldBe true

        // Command is NOT still on the stack or in the battlefield.
        d.findPermanent(p1, "Brigid's Command").shouldBeNull()
    }
})
