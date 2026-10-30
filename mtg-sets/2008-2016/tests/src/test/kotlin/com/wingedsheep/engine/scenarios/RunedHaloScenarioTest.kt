package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.battlefield.ChoiceValue
import com.wingedsheep.engine.state.components.battlefield.chosenCardName
import com.wingedsheep.engine.state.components.battlefield.withCastChoice
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.shm.cards.RunedHalo
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.ChoiceSlot
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Runed Halo (SHM #21; reprinted M21) — {W}{W} Enchantment.
 *
 * "As this enchantment enters, choose a card name. / You have protection from the chosen card name.
 * (You can't be targeted, dealt damage, or enchanted by anything with that name.)"
 *
 * The as-enters choice is `EntersWithChoice(ChoiceType.CARD_NAME)`; the protection is
 * `GrantProtectionToController(ProtectionScope.ChosenCardName())`, read off the Halo's durable
 * choice. Covers the cast-and-choose flow, the targeting leg (a spell with the chosen name can't
 * target you, but still targets your opponent), and the damage leg (combat damage from a creature
 * with the chosen name is prevented; a differently named one still connects).
 */
class RunedHaloScenarioTest : ScenarioTestBase() {

    init {
        cardRegistry.register(RunedHalo)

        test("choosing a name stops spells with that name from targeting you") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Runed Halo")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withCardInHand(2, "Lightning Bolt")
                .withLandsOnBattlefield(2, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Runed Halo").error shouldBe null
            game.resolveStack()
            val decision = game.getPendingDecision()
            withClue("Runed Halo asks for a card name as it enters") {
                (decision is ChooseOptionDecision) shouldBe true
            }
            decision as ChooseOptionDecision
            decision.options shouldContain "Lightning Bolt"
            game.submitDecision(OptionChosenResponse(decision.id, decision.options.indexOf("Lightning Bolt")))

            val halo = game.findPermanent("Runed Halo")!!
            game.state.getEntity(halo)?.chosenCardName() shouldBe "Lightning Bolt"

            game.passPriority()
            withClue("a Lightning Bolt can't target the Halo's controller") {
                game.castSpellTargetingPlayer(2, "Lightning Bolt", 1).error shouldNotBe null
            }
            withClue("but it can still target its own caster — the protection is the Halo controller's") {
                game.castSpellTargetingPlayer(2, "Lightning Bolt", 2).error shouldBe null
            }
        }

        test("combat damage from a creature with the chosen name is prevented, another name still hits") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Runed Halo")
                .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                .withCardOnBattlefield(2, "Hill Giant", summoningSickness = false)
                .withActivePlayer(2)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()
            val halo = game.findPermanent("Runed Halo")!!
            game.state = game.state.updateEntity(halo) {
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
