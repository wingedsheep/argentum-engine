package com.wingedsheep.engine.mechanics.mana

import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.engine.core.NumberChosenResponse
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.ManaExpiry
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * `AddDynamicManaEffect.expiry` — "add X mana in any combination of … Until end of turn, you don't
 * lose this mana as steps and phases end" (Klauth, Unrivaled Ancient). The executor colors mana
 * three ways (one color in bulk, a two-color split prompt, a pip-by-pip prompt for 3+ colors);
 * each must stamp the expiry on every unit so the mana survives combat's step boundaries into the
 * postcombat main phase, and is still lost once the turn ends.
 */
class DynamicManaExpiryTest : FunSpec({

    fun attackManaCard(name: String, colors: Set<Color>) = card(name) {
        manaCost = "{2}"
        typeLine = "Creature — Test"
        power = 1
        toughness = 1
        oracleText = "Whenever this creature attacks, add three mana in any combination of the allowed colors. " +
            "Until end of turn, you don't lose this mana as steps and phases end."
        triggeredAbility {
            trigger = Triggers.self.attacks()
            effect = Effects.AddManaInAnyCombination(
                DynamicAmount.Fixed(3), colors, expiry = ManaExpiry.KEPT_UNTIL_END_OF_TURN
            )
        }
    }

    val mono = attackManaCard("Kept Mono Attacker", setOf(Color.RED))
    val duo = attackManaCard("Kept Duo Attacker", setOf(Color.RED, Color.GREEN))
    val rainbow = attackManaCard("Kept Rainbow Attacker", Color.entries.toSet())

    fun GameTestDriver.kept(playerId: EntityId): Map<Color?, Int> =
        (state.getEntity(playerId)?.get<ManaPoolComponent>()?.restrictedMana ?: emptyList())
            .filter { it.expiry == ManaExpiry.KEPT_UNTIL_END_OF_TURN }
            .groupingBy { it.color }.eachCount()

    fun attack(name: String): Pair<GameTestDriver, EntityId> {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(mono, duo, rainbow))
        d.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val you = d.activePlayer!!
        val attacker = d.putCreatureOnBattlefield(you, name)
        d.removeSummoningSickness(attacker)
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(you, listOf(attacker), d.getOpponent(you))
        return d to you
    }

    test("a single allowed color adds the whole amount as kept mana") {
        val (d, you) = attack("Kept Mono Attacker")
        d.bothPass()
        d.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        d.kept(you) shouldBe mapOf(Color.RED to 3)
    }

    test("the two-color split keeps both halves") {
        val (d, you) = attack("Kept Duo Attacker")
        d.bothPass()
        d.submitDecision(you, NumberChosenResponse(d.pendingDecision!!.id, 1)).error shouldBe null
        d.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        d.kept(you).values.sum() shouldBe 3
        d.kept(you).keys shouldBe setOf(Color.RED, Color.GREEN)
    }

    test("pip-by-pip coloring keeps every pip, and the mana is lost once the turn ends") {
        val (d, you) = attack("Kept Rainbow Attacker")
        d.bothPass()
        for (color in listOf(Color.WHITE, Color.BLUE, Color.BLUE)) {
            d.submitDecision(you, ColorChosenResponse(d.pendingDecision!!.id, color)).error shouldBe null
        }
        d.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        d.kept(you) shouldBe mapOf(Color.WHITE to 1, Color.BLUE to 2)

        d.passPriorityUntil(Step.UPKEEP)
        d.state.getEntity(you)?.get<ManaPoolComponent>()?.isEmpty shouldBe true
    }
})
