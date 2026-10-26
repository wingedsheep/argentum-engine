package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.ManaExpiry
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Klauth, Unrivaled Ancient — "Whenever Klauth attacks, add X mana in any combination of colors,
 * where X is the total power of attacking creatures. Spend this mana only to cast spells. Until end
 * of turn, you don't lose this mana as steps and phases end."
 */
class KlauthUnrivaledAncientScenarioTest : FunSpec({

    fun GameTestDriver.keptSpellMana(playerId: EntityId) =
        (state.getEntity(playerId)?.get<ManaPoolComponent>()?.restrictedMana ?: emptyList())
            .filter { it.expiry == ManaExpiry.KEPT_UNTIL_END_OF_TURN && it.restriction == ManaRestriction.SpellsOnly }

    test("X is the total power of attackers, colored pip by pip, and the mana pays for a spell after combat") {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        d.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val you = d.activePlayer!!
        val opponent = d.getOpponent(you)
        val klauth = d.putCreatureOnBattlefield(you, "Klauth, Unrivaled Ancient")
        val bears = d.putCreatureOnBattlefield(you, "Grizzly Bears")
        d.removeSummoningSickness(bears)

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        // Haste: Klauth attacks the turn it arrives.
        d.declareAttackers(you, listOf(klauth, bears), opponent).error shouldBe null
        d.bothPass()

        val picks = listOf(Color.RED, Color.RED, Color.GREEN, Color.BLUE, Color.WHITE, Color.BLACK)
        for (color in picks) {
            d.submitDecision(you, ColorChosenResponse(d.pendingDecision!!.id, color)).error shouldBe null
        }
        d.keptSpellMana(you).size shouldBe 4 + 2

        d.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        d.keptSpellMana(you).size shouldBe 6
        d.getLifeTotal(opponent) shouldBe 20 - 4 - 2

        val bolt = d.putCardInHand(you, "Lightning Bolt")
        d.castSpell(you, bolt, listOf(opponent)).error shouldBe null
        d.bothPass()
        d.keptSpellMana(you).size shouldBe 5
        d.getLifeTotal(opponent) shouldBe 20 - 6 - 3

        d.passPriorityUntil(Step.UPKEEP)
        d.state.getEntity(you)?.get<ManaPoolComponent>()?.isEmpty shouldBe true
    }
})
