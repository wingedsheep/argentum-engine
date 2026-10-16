package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * `Player.EnchantedPlayer` once the enchant-player Aura has left the battlefield: "enchanted
 * player" is the player it was attached to as it last existed there (CR 608.2h). Destroying the
 * Aura in response to its own enters trigger must not turn the payoff into a no-op — on both the
 * player-reference path (`EffectTarget.PlayerRef`) and the predicate path
 * (`controlledByEnchantedPlayer()` group filters).
 */
class EnchantedPlayerLastKnownTest : FunSpec({

    val drainAura = card("Remembered Drain Aura") {
        manaCost = "{W}"; typeLine = "Enchantment — Aura"
        auraTarget = Targets.Opponent
        triggeredAbility {
            trigger = Triggers.self.enters()
            effect = Effects.LoseLife(3, EffectTarget.PlayerRef(Player.EnchantedPlayer))
        }
    }
    val wrathAura = card("Remembered Wrath Aura") {
        manaCost = "{W}"; typeLine = "Enchantment — Aura"
        auraTarget = Targets.Opponent
        triggeredAbility {
            trigger = Triggers.self.enters()
            effect = Effects.DestroyAll(GameObjectFilter.Creature.controlledByEnchantedPlayer())
        }
    }
    val shatter = card("Aura Shatter") {
        manaCost = "{W}"; typeLine = "Instant"
        spell {
            val t = target(TargetFilter(GameObjectFilter.Enchantment))
            effect = Effects.Destroy(t)
        }
    }

    fun driver() = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(drainAura, wrathAura, shatter))
        it.initMirrorMatch(Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    /** Cast [auraName] on player2, then destroy it in response to its enters trigger. */
    fun GameTestDriver.castAndShatter(auraName: String): EntityId {
        val aura = putCardInHand(player1, auraName)
        giveMana(player1, Color.WHITE, 1)
        castSpell(player1, aura, targets = listOf(player2)).error shouldBe null
        bothPass() // the Aura resolves; its enters trigger goes on the stack
        stackSize shouldBe 1
        val instant = putCardInHand(player1, "Aura Shatter")
        giveMana(player1, Color.WHITE, 1)
        castSpell(player1, instant, targets = listOf(aura)).error shouldBe null
        bothPass() // Aura Shatter resolves
        withClue("the Aura has left the battlefield") { findPermanent(player1, auraName) shouldBe null }
        stackSize shouldBe 1
        bothPass() // the enters trigger resolves
        return aura
    }

    test("a player reference reads the player the departed Aura enchanted") {
        val d = driver()
        val before = d.getLifeTotal(d.player2)
        d.castAndShatter("Remembered Drain Aura")
        d.getLifeTotal(d.player2) shouldBe before - 3
        d.getLifeTotal(d.player1) shouldBe before
    }

    test("an enchanted-player group filter reads the player the departed Aura enchanted") {
        val d = driver()
        d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        d.castAndShatter("Remembered Wrath Aura")
        withClue("the enchanted opponent's creature is destroyed") {
            d.findPermanent(d.player2, "Grizzly Bears") shouldBe null
        }
        withClue("the Aura controller's creature is untouched") {
            (d.findPermanent(d.player1, "Grizzly Bears") != null) shouldBe true
        }
    }
})
