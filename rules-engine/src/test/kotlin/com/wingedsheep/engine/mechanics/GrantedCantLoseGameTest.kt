package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantCantLoseGame
import com.wingedsheep.sdk.scripting.GrantOpponentsCantWinGame
import com.wingedsheep.sdk.scripting.GrantStaticAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe

/**
 * "Equipped/enchanted creature has 'You can't lose the game and your opponents can't win the
 * game'" (Cloudsteel Kirin). The printed form (Platinum Angel) is stamped as a marker at entry;
 * a *granted* one only exists in projection, so these pin that every loss/win reader sees it:
 * the creature's controller is "you", the lock follows the grant, and it ends with it.
 */
class GrantedCantLoseGameTest : FunSpec({

    val ward = card("Lockward Aura") {
        manaCost = "{0}"
        typeLine = "Enchantment — Aura"
        auraTarget = TargetObject(filter = TargetFilter.Creature)
        staticAbility { ability = GrantStaticAbility(GrantCantLoseGame, filter = Filters.EnchantedCreature) }
        staticAbility { ability = GrantStaticAbility(GrantOpponentsCantWinGame, filter = Filters.EnchantedCreature) }
    }

    val host = card("Lockward Host") {
        manaCost = "{0}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }

    val loseThirty = card("Lockward Lifeloss") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell { effect = Effects.LoseLife(30, EffectTarget.Controller) }
    }

    val victory = card("Lockward Victory") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell { effect = Effects.WinGame(EffectTarget.Controller) }
    }

    val disenchant = card("Lockward Disenchant") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell {
            val t = target(TargetFilter(GameObjectFilter.Enchantment))
            effect = Effects.Destroy(t)
        }
    }

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(ward, host, loseThirty, victory, disenchant))
        initMirrorMatch(deck = Deck.of("Plains" to 40))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.cast(player: EntityId, name: String, targets: List<EntityId> = emptyList()) {
        val holder = state.priorityPlayerId
        if (holder != null && holder != player) passPriority(holder)
        castSpell(player, putCardInHand(player, name), targets).outcome shouldBe Outcome.Done
        var guard = 0
        while (!state.gameOver && state.stack.isNotEmpty() && guard++ < 10) bothPass()
    }

    test("the enchanted creature's controller can't lose, and stops being protected when the grant ends") {
        val d = driver()
        val creature = d.putCreatureOnBattlefield(d.player1, "Lockward Host")
        d.cast(d.player1, "Lockward Aura", listOf(creature))

        d.cast(d.player1, "Lockward Lifeloss")
        d.getLifeTotal(d.player1) shouldBe -10
        d.state.gameOver.shouldBeFalse()

        val aura = d.state.getBattlefield().single { d.state.getEntity(it)?.get<CardComponent>()?.name == "Lockward Aura" }
        d.cast(d.player2, "Lockward Disenchant", listOf(aura))
        d.state.gameOver.shouldBeTrue()
        d.state.winnerId shouldBe d.player2
    }

    test("'you' is the holder's controller, not the granting permanent's") {
        val d = driver()
        val theirs = d.putCreatureOnBattlefield(d.player2, "Lockward Host")
        d.cast(d.player1, "Lockward Aura", listOf(theirs))

        // player1 controls the Aura but not the creature: no protection for them.
        d.cast(d.player1, "Lockward Lifeloss")
        d.state.gameOver.shouldBeTrue()
        d.state.winnerId shouldBe d.player2
    }

    test("an opponent of the holder's controller can't win the game") {
        val d = driver()
        val creature = d.putCreatureOnBattlefield(d.player1, "Lockward Host")
        d.cast(d.player1, "Lockward Aura", listOf(creature))

        d.cast(d.player2, "Lockward Victory")
        d.state.gameOver.shouldBeFalse()
        d.state.winnerId shouldBe null
    }
})
