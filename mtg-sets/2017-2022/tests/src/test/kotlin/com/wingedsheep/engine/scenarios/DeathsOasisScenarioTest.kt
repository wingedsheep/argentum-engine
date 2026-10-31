package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.iko.cards.DeathsOasis
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Death's Oasis (IKO #182) — {W}{B}{G} Enchantment.
 *
 *   Whenever a nontoken creature you control dies, mill two cards. Then return a creature card
 *   with lesser mana value than the creature that died from your graveyard to your hand.
 *   {1}, Sacrifice this enchantment: You gain life equal to the greatest mana value among
 *   creatures you control.
 *
 * Covers the non-targeted, post-mill return pool capped by the dead creature's mana value, and the
 * sacrifice ability reading the greatest mana value among creatures you control.
 */
class DeathsOasisScenarioTest : ScenarioTestBase() {

    init {
        test("a dying creature mills two, then returns a lesser-mana-value creature card, milled ones included") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, "Death's Oasis")
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardInHand(1, "Murder")
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withCardInLibrary(1, "Raging Goblin")
                .withCardInLibrary(1, "Craw Wurm")
                .withCardInGraveyard(1, "Grizzly Bears")
                .withCardInLibrary(2, "Swamp")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Murder", game.findPermanent("Hill Giant")!!).error shouldBe null
            game.resolveStack()

            val decision = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            val goblin = game.findCardsInGraveyard(1, "Raging Goblin").single()
            val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()
            withClue("only creature cards with mana value below Hill Giant's 4 are offered") {
                decision.options shouldContainExactlyInAnyOrder listOf(goblin, bears)
            }

            game.selectCards(listOf(goblin)).error shouldBe null
            game.resolveStack()

            game.isInHand(1, "Raging Goblin") shouldBe true
            game.isInGraveyard(1, "Craw Wurm") shouldBe true
            game.isInGraveyard(1, "Hill Giant") shouldBe true
            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
        }

        test("sacrificing it gains life equal to the greatest mana value among your creatures") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, "Death's Oasis")
                .withCardOnBattlefield(1, "Craw Wurm")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withLandsOnBattlefield(1, "Forest", 1)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = game.findPermanent("Death's Oasis")!!,
                    abilityId = DeathsOasis.activatedAbilities.single().id,
                )
            ).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Death's Oasis") shouldBe false
            game.getLifeTotal(1) shouldBe 26
        }
    }
}
