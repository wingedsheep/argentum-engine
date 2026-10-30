package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Dawn Evangel (THB #8) — {2}{W} Enchantment Creature — Human Cleric, 2/3.
 *
 * "Whenever a creature dies, if an Aura you controlled was attached to it, return target creature
 *  card with mana value 2 or less from your graveyard to your hand."
 *
 * Pins the intervening "if" against last-known Aura controllers: any player's creature counts when
 * *your* Aura was on it, an opponent's Aura alone doesn't, and the dead creature itself is a legal
 * target (ruling).
 */
class DawnEvangelScenarioTest : ScenarioTestBase() {

    private fun builder() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Dawn Evangel")
        .withCardInGraveyard(1, "Hill Giant")
        .withLandsOnBattlefield(1, "Swamp", 3)
        .withCardInHand(1, "Murder")
        .withCardInLibrary(1, "Swamp")
        .withCardInLibrary(2, "Swamp")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

    init {
        context("Dawn Evangel") {

            test("your Aura on your dying creature returns a creature card with mana value 2 or less — the creature itself") {
                val game = builder()
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardAttachedTo(1, "Holy Strength", "Grizzly Bears")
                    .build()

                game.castSpell(1, "Murder", game.findPermanent("Grizzly Bears")).error shouldBe null
                game.resolveStack()

                val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()
                val giant = game.findCardsInGraveyard(1, "Hill Giant").single()
                val decision = game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
                val legal = decision.legalTargets.values.flatten()
                withClue("the dead Bears is a legal target; the mana value 4 Hill Giant is not") {
                    (bears in legal) shouldBe true
                    (giant in legal) shouldBe false
                }
                game.selectTargets(listOf(bears)).error shouldBe null
                game.resolveStack()

                game.isInHand(1, "Grizzly Bears") shouldBe true
            }

            test("your Aura on an opponent's creature also counts") {
                val game = builder()
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardAttachedTo(1, "Pacifism", "Grizzly Bears")
                    .withCardInGraveyard(1, "Savannah Lions")
                    .build()

                game.castSpell(1, "Murder", game.findPermanent("Grizzly Bears")).error shouldBe null
                game.resolveStack()

                val lions = game.findCardsInGraveyard(1, "Savannah Lions").single()
                val decision = game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
                withClue("only your own graveyard is searched for a target") {
                    decision.legalTargets.values.flatten().toSet() shouldBe setOf(lions)
                }
                game.selectTargets(listOf(lions)).error shouldBe null
                game.resolveStack()

                game.isInHand(1, "Savannah Lions") shouldBe true
            }

            test("a creature enchanted only by an opponent's Aura doesn't trigger it") {
                val game = builder()
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardAttachedTo(2, "Pacifism", "Grizzly Bears")
                    .build()

                game.castSpell(1, "Murder", game.findPermanent("Grizzly Bears")).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                withClue("the intervening \"if\" is false, so the ability never triggers") {
                    game.hasPendingDecision() shouldBe false
                    game.state.stack.isEmpty() shouldBe true
                }
            }
        }
    }
}
