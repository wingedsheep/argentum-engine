package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Mission Briefing (GRN 44) — "Surveil 2, then choose an instant or sorcery card in your
 * graveyard. You may cast it this turn. If that spell would be put into your graveyard, exile it
 * instead."
 *
 * The choice is made on resolution after the surveil (a card just surveilled away is eligible),
 * only the chosen card gains the permission, it is cast for its mana cost, and it is exiled as it
 * resolves.
 */
class MissionBriefingScenarioTest : ScenarioTestBase() {

    init {
        test("choose a just-surveilled instant, cast it for its mana cost, and it is exiled") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Mission Briefing")
                .withLandsOnBattlefield(1, "Island", 2)
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardInGraveyard(1, "Shock")
                .withCardInLibrary(1, "Lightning Bolt")
                .withCardInLibrary(1, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Mission Briefing").error.shouldBeNull()
            game.resolveStack()

            val surveil = game.getPendingDecision()
            surveil.shouldBeInstanceOf<SelectCardsDecision>()
            val bolt = game.findCardsInLibrary(1, "Lightning Bolt").single()
            game.selectCards(listOf(bolt)).error.shouldBeNull()
            if (game.getPendingDecision() is ReorderLibraryDecision) game.keepLibraryOrder()

            val choice = game.getPendingDecision()
            choice.shouldBeInstanceOf<SelectCardsDecision>()
            val shock = game.findCardsInGraveyard(1, "Shock").single()
            withClue("the just-surveilled card and the one already there are both offered") {
                choice.options shouldContain bolt
                choice.options shouldContain shock
            }
            game.selectCards(listOf(bolt)).error.shouldBeNull()
            game.resolveStack()

            withClue("Mission Briefing itself goes to the graveyard as normal") {
                game.isInGraveyard(1, "Mission Briefing") shouldBe true
            }
            withClue("the card not chosen gains no permission") {
                game.castSpellFromGraveyard(1, "Shock").error.shouldNotBeNull()
            }

            game.execute(CastSpell(game.player1Id, bolt, listOf(ChosenTarget.Player(game.player2Id))))
                .error.shouldBeNull()
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 17
            withClue("the spell cast this way is exiled instead of going to the graveyard") {
                game.isInExile(1, "Lightning Bolt") shouldBe true
                game.isInGraveyard(1, "Lightning Bolt") shouldBe false
            }
        }
    }
}
