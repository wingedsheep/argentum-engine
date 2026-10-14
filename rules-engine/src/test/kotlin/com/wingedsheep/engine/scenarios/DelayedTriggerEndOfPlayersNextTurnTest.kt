package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

/**
 * `DelayedTriggerExpiry.EndOfPlayersNextTurn` — "until the end of [that player's] next turn,
 * whenever …" — and a cast watcher scoped to one caster through `watchedTarget`.
 *
 * The window: the current turn never counts, even when it is the named player's own; the trigger
 * survives the other players' turns and is removed in the cleanup step of the named player's next
 * turn (the same floor + active-player guard as `Duration.EndOfYourNextTurn`).
 */
class DelayedTriggerEndOfPlayersNextTurnTest : FunSpec({

    // "Until the end of your next turn, whenever you cast a spell, you gain 1 life."
    val selfWatch = card("Self Watch") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell {
            effect = Effects.CreateDelayedTrigger(
                trigger = Triggers.anyPlayer.casts(),
                watchedTarget = EffectTarget.PlayerRef(Player.You),
                expiry = DelayedTriggerExpiry.EndOfPlayersNextTurn(),
                effect = Effects.GainLife(1, EffectTarget.PlayerRef(Player.TriggeringPlayer))
            )
        }
    }
    val freeSpell = card("Free Spell") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell { effect = Effects.DrawCards(0) }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(selfWatch, freeSpell))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN, driver.player1)
        return driver
    }

    fun GameTestDriver.castAndResolve(player: com.wingedsheep.sdk.model.EntityId, name: String) {
        castSpell(player, putCardInHand(player, name)).error shouldBe null
        while (state.stack.isNotEmpty()) bothPass()
    }

    test("the trigger is floored past the current turn and bound to the named player") {
        val driver = createDriver()
        val turn = driver.state.turnNumber
        driver.castAndResolve(driver.player1, "Self Watch")

        val watcher = driver.state.delayedTriggers.single()
        watcher.expiryPlayerId shouldBe driver.player1
        watcher.expiresAfterTurn shouldBe turn + 1
        watcher.watchedEntityId shouldBe driver.player1
    }

    test("only the watched player's casts fire it") {
        val driver = createDriver()
        driver.castAndResolve(driver.player1, "Self Watch")

        val life = driver.getLifeTotal(driver.player1)
        driver.castAndResolve(driver.player1, "Free Spell")
        driver.getLifeTotal(driver.player1) shouldBe life + 1

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN, driver.player2)
        val opponentLife = driver.getLifeTotal(driver.player2)
        driver.castAndResolve(driver.player2, "Free Spell")
        driver.getLifeTotal(driver.player2) shouldBe opponentLife
        driver.getLifeTotal(driver.player1) shouldBe life + 1
    }

    test("lasts through the named player's next turn and ends in its cleanup step") {
        val driver = createDriver()
        driver.castAndResolve(driver.player1, "Self Watch")

        // Created on the named player's own turn: this turn doesn't count.
        driver.passPriorityUntil(Step.UPKEEP, driver.player2)
        driver.state.delayedTriggers.size shouldBe 1
        // The other player's turn doesn't end it either.
        driver.passPriorityUntil(Step.END, driver.player1)
        driver.state.delayedTriggers.size shouldBe 1
        val life = driver.getLifeTotal(driver.player1)
        driver.castAndResolve(driver.player1, "Free Spell")
        driver.getLifeTotal(driver.player1) shouldBe life + 1

        // Gone after the cleanup of that next turn.
        driver.passPriorityUntil(Step.UPKEEP, driver.player2)
        driver.state.delayedTriggers.shouldBeEmpty()
    }
})
