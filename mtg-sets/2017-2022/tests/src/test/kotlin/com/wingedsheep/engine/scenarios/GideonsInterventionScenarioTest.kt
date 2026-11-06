package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.battlefield.ChoiceValue
import com.wingedsheep.engine.state.components.battlefield.chosenCardName
import com.wingedsheep.engine.state.components.battlefield.withCastChoice
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.akh.cards.GideonsIntervention
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.ChoiceSlot
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Gideon's Intervention (AKH #15) — {2}{W}{W} Enchantment.
 *
 * "As this enchantment enters, choose a card name. / Your opponents can't cast spells with the
 * chosen name. / Prevent all damage that would be dealt to you and permanents you control by
 * sources with the chosen name."
 *
 * First card to key `PlayersCantCastSpells` and a `PreventDamage` source filter off the durable
 * as-enters name (`NameEqualsChosenComponent`). Covers: the opponent-only cast prohibition, damage
 * from a same-named spell to a permanent you control (your own spell included — the prevention is
 * by source name, not controller), and combat damage to you from a same-named creature.
 */
class GideonsInterventionScenarioTest : ScenarioTestBase() {

    init {
        cardRegistry.register(GideonsIntervention)

        test("opponents can't cast the chosen name; you still can, and its damage to your permanents is prevented") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Gideon's Intervention")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Plains", 4)
                .withLandsOnBattlefield(1, "Mountain", 3)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(2, "Lightning Bolt")
                .withLandsOnBattlefield(2, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Gideon's Intervention").error shouldBe null
            game.resolveStack()
            val decision = game.getPendingDecision()
            withClue("Gideon's Intervention asks for a card name as it enters") {
                (decision is ChooseOptionDecision) shouldBe true
            }
            decision as ChooseOptionDecision
            decision.options shouldContain "Lightning Bolt"
            game.submitDecision(OptionChosenResponse(decision.id, decision.options.indexOf("Lightning Bolt")))

            val intervention = game.findPermanent("Gideon's Intervention")!!
            game.state.getEntity(intervention)?.chosenCardName() shouldBe "Lightning Bolt"

            val bears = game.findPermanent("Grizzly Bears")!!
            withClue("the controller isn't an opponent, so their own Lightning Bolt can be cast") {
                game.castSpell(1, "Lightning Bolt", bears).error shouldBe null
            }
            game.resolveStack()
            withClue("damage from a source with the chosen name to a permanent you control is prevented") {
                game.findPermanent("Grizzly Bears") shouldNotBe null
            }

            game.passPriority()
            withClue("an opponent can't cast a spell with the chosen name") {
                game.castSpellTargetingPlayer(2, "Lightning Bolt", 1).error shouldNotBe null
            }
        }

        test("combat damage to you from a creature with the chosen name is prevented, another name still hits") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Gideon's Intervention")
                .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                .withCardOnBattlefield(2, "Hill Giant", summoningSickness = false)
                .withActivePlayer(2)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()
            val intervention = game.findPermanent("Gideon's Intervention")!!
            game.state = game.state.updateEntity(intervention) {
                it.withCastChoice(ChoiceSlot.CARD_NAME, ChoiceValue.TextChoice("Grizzly Bears"))
            }
            val lifeBefore = game.getLifeTotal(1)

            game.declareAttackers(mapOf("Grizzly Bears" to 1, "Hill Giant" to 1)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
            game.resolveStack()
            if (game.state.pendingDecision != null) {
                game.submitDefaultCombatDamage()
                game.resolveStack()
            }

            withClue("only Hill Giant's 3 damage is dealt; Grizzly Bears' 2 is prevented") {
                game.getLifeTotal(1) shouldBe lifeBefore - 3
            }
        }
    }
}
