package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.aer.cards.AjanisAid
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Ajani's Aid (AER #188) — {2}{G}{W} Enchantment.
 *
 * When this enchantment enters, you may search your library and/or graveyard for a card named Ajani,
 * Valiant Protector, reveal it, and put it into your hand. If you search your library this way, shuffle.
 * Sacrifice this enchantment: Prevent all combat damage a creature of your choice would deal this turn.
 *
 * The sacrifice ability doesn't target (2017-02-09 ruling): the creature is chosen as it resolves.
 * Two attackers make the choice do work — only the chosen one's combat damage is prevented.
 */
class AjanisAidScenarioTest : ScenarioTestBase() {

    private val sacAbility = AjanisAid.activatedAbilities.single().id

    init {
        test("enters: search the graveyard for Ajani, Valiant Protector") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Ajani's Aid")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withLandsOnBattlefield(1, "Forest", 2)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Island")
                .withCardInGraveyard(1, "Ajani, Valiant Protector")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.castSpell(1, "Ajani's Aid").error shouldBe null
            game.resolveStack()

            val zones = game.getPendingDecision().shouldBeInstanceOf<ChooseOptionDecision>()
            game.submitDecision(OptionChosenResponse(zones.id, zones.options.indexOf("Search your graveyard")))
                .error shouldBe null
            game.resolveStack()

            withClue("the only Ajani in the graveyard is the search's single match and goes to hand") {
                game.isInHand(1, "Ajani, Valiant Protector") shouldBe true
                game.isInGraveyard(1, "Ajani, Valiant Protector") shouldBe false
            }
        }

        test("sacrifice: the chosen attacker deals no combat damage, the other still connects") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Savannah Lions")
                .withCardOnBattlefield(2, "Ajani's Aid")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()
            game.declareAttackers(mapOf("Grizzly Bears" to 2, "Savannah Lions" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareNoBlockers().error shouldBe null

            val aid = game.findPermanent("Ajani's Aid")!!
            game.execute(ActivateAbility(game.player2Id, aid, sacAbility)).error shouldBe null
            game.resolveStack()
            val choice = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            val bears = game.findPermanent("Grizzly Bears")!!
            val lions = game.findPermanent("Savannah Lions")!!
            withClue("both attacking creatures can be chosen") {
                choice.options shouldContainExactlyInAnyOrder listOf(bears, lions)
            }
            game.selectCards(listOf(bears)).error shouldBe null
            game.resolveStack()
            game.isInGraveyard(2, "Ajani's Aid") shouldBe true

            game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)
            withClue("the Bears' 2 is prevented, the Lions' 1 lands") {
                game.getLifeTotal(2) shouldBe 19
            }
        }
    }
}
