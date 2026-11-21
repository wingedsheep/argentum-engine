package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.battlefield.ChoiceValue
import com.wingedsheep.engine.state.components.battlefield.chosenCardName
import com.wingedsheep.engine.state.components.battlefield.withCastChoice
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.dgm.cards.CouncilOfTheAbsolute
import com.wingedsheep.mtg.sets.definitions.dom.cards.Divination
import com.wingedsheep.mtg.sets.definitions.inv.cards.Opt
import com.wingedsheep.mtg.sets.definitions.vis.cards.Inspiration
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.ChoiceSlot
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Council of the Absolute (DGM #62) — {2}{W}{U} Creature — Human Advisor 2/4.
 *
 * "As this creature enters, choose a noncreature, nonland card name. / Your opponents can't cast
 * spells with the chosen name. / Spells with the chosen name you cast cost {2} less to cast."
 *
 * Covers the `CardNamePool.NONCREATURE_NONLAND` offer (no creature or land names), the {2}
 * discount on the controller's own spell with the chosen name, and the opponent's cast ban.
 */
class CouncilOfTheAbsoluteScenarioTest : ScenarioTestBase() {

    init {
        cardRegistry.register(CouncilOfTheAbsolute)
        cardRegistry.register(Divination)
        cardRegistry.register(Inspiration)
        cardRegistry.register(Opt)

        test("offers only noncreature, nonland card names as it enters") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Council of the Absolute")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withLandsOnBattlefield(1, "Island", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Council of the Absolute").error shouldBe null
            game.resolveStack()
            val decision = game.getPendingDecision()
            withClue("Council of the Absolute asks for a card name as it enters") {
                (decision is ChooseOptionDecision) shouldBe true
            }
            decision as ChooseOptionDecision
            decision.options shouldContain "Divination"
            withClue("creature and land names are not offered") {
                decision.options shouldNotContain "Council of the Absolute"
                decision.options shouldNotContain "Island"
            }
            game.submitDecision(OptionChosenResponse(decision.id, decision.options.indexOf("Divination")))

            val council = game.findPermanent("Council of the Absolute")!!
            game.state.getEntity(council)?.chosenCardName() shouldBe "Divination"
        }

        test("your spell with the chosen name costs {2} less; an opponent can't cast it") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Council of the Absolute")
                .withCardInHand(1, "Inspiration")
                .withLandsOnBattlefield(1, "Island", 2)
                .withCardInHand(2, "Inspiration")
                .withCardInHand(2, "Opt")
                .withLandsOnBattlefield(2, "Island", 5)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val council = game.findPermanent("Council of the Absolute")!!
            game.state = game.state.updateEntity(council) {
                it.withCastChoice(ChoiceSlot.CARD_NAME, ChoiceValue.TextChoice("Inspiration"))
            }

            withClue("Inspiration costs {2} less: two Islands pay {1}{U}") {
                game.castSpellTargetingPlayer(1, "Inspiration", 1).error shouldBe null
            }
            game.resolveStack()
            game.handSize(1) shouldBe 2

            game.passPriority()
            withClue("an opponent can't cast a spell with the chosen name") {
                game.castSpellTargetingPlayer(2, "Inspiration", 2).error shouldNotBe null
            }
            withClue("the opponent can still cast a spell with another name") {
                game.castSpell(2, "Opt").error shouldBe null
            }
        }
    }
}
