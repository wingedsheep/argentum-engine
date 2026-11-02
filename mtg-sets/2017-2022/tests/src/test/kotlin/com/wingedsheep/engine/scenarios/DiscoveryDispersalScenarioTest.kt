package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Discovery // Dispersal (GRN 223).
 *
 * Dispersal: "Each opponent returns a nonland permanent they control with the greatest mana value
 * among permanents they control to its owner's hand, then discards a card." The opponent — not the
 * caster — breaks a tie for greatest mana value, and an opponent with only lands still discards
 * (2018-10-05 ruling).
 */
class DiscoveryDispersalScenarioTest : ScenarioTestBase() {

    init {
        test("the opponent chooses among their tied greatest-mana-value permanents, then discards") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Discovery // Dispersal")
                .withLandsOnBattlefield(1, "Island", 3)
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withCardOnBattlefield(2, "Serra Angel")
                .withCardOnBattlefield(2, "Air Elemental")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withLandsOnBattlefield(2, "Plains", 1)
                .withCardInHand(2, "Lightning Bolt")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val cardId = game.findCardsInHand(1, "Discovery // Dispersal").first()
            game.execute(CastSpell(game.player1Id, cardId, faceIndex = 1)).error.shouldBeNull()
            game.resolveStack()

            val bounce = game.getPendingDecision()
            bounce.shouldBeInstanceOf<SelectCardsDecision>()
            withClue("the opponent makes the choice") { bounce.playerId shouldBe game.player2Id }
            val angel = game.findPermanent("Serra Angel").shouldNotBeNull()
            val elemental = game.findPermanent("Air Elemental").shouldNotBeNull()
            withClue("only the two mana-value-5 permanents are offered") {
                bounce.options shouldContainExactlyInAnyOrder listOf(angel, elemental)
            }
            game.selectCards(listOf(elemental)).error.shouldBeNull()

            val discard = game.getPendingDecision()
            discard.shouldBeInstanceOf<SelectCardsDecision>()
            discard.playerId shouldBe game.player2Id
            val bolt = game.findCardsInHand(2, "Lightning Bolt").single()
            game.selectCards(listOf(bolt)).error.shouldBeNull()
            game.resolveStack()

            withClue("Air Elemental returned to its owner's hand") { game.isInHand(2, "Air Elemental") shouldBe true }
            game.findPermanent("Serra Angel").shouldNotBeNull()
            game.findPermanent("Grizzly Bears").shouldNotBeNull()
            game.isInGraveyard(2, "Lightning Bolt") shouldBe true
        }

        test("an opponent who controls only lands returns nothing but still discards") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Discovery // Dispersal")
                .withLandsOnBattlefield(1, "Island", 3)
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withLandsOnBattlefield(2, "Plains", 2)
                .withCardInHand(2, "Lightning Bolt")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val cardId = game.findCardsInHand(1, "Discovery // Dispersal").first()
            game.execute(CastSpell(game.player1Id, cardId, faceIndex = 1)).error.shouldBeNull()
            game.resolveStack()
            game.getPendingDecision()?.let { game.selectCards(game.findCardsInHand(2, "Lightning Bolt")) }
            game.resolveStack()

            game.findPermanents("Plains").size shouldBe 2
            game.isInGraveyard(2, "Lightning Bolt") shouldBe true
        }
    }
}
