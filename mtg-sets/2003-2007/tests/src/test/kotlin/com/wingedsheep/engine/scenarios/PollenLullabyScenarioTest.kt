package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.player.SkipUntapComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lrw.cards.PollenLullaby
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Pollen Lullaby fogs unconditionally and, on a clash win, freezes the *clash* opponent's creatures.
 *
 * The freeze names a player the card never targeted — it is the opponent chosen by the clash, read
 * back through `Player.ChosenOpponent` — so these tests are the evidence for that read, and for the
 * fog resolving whether or not the clash is won.
 */
class PollenLullabyScenarioTest : FunSpec({
    val boulder = card("Lullaby Boulder") { manaCost = "{5}"; typeLine = "Artifact"; oracleText = "" }

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(PollenLullaby, boulder))
        initMirrorMatch(Deck.of("Plains" to 40), startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    /** Answer both clash top-or-bottom prompts, keeping each revealed card on top. */
    fun GameTestDriver.resolveClashDecisions() {
        repeat(2) {
            val decision = pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
            submitCardSelection(decision.playerId, emptyList()).error shouldBe null
        }
    }

    for (win in listOf(true, false)) {
        test("clash win=$win — the clash opponent's creatures are frozen only on a win") {
            val d = driver()
            d.putCardOnTopOfLibrary(d.player1, if (win) "Lullaby Boulder" else "Plains")
            d.putCardOnTopOfLibrary(d.player2, if (win) "Plains" else "Lullaby Boulder")

            val spell = d.putCardInHand(d.player1, "Pollen Lullaby")
            d.giveMana(d.player1, Color.WHITE, 2)
            d.castSpell(d.player1, spell, emptyList()).error shouldBe null
            d.bothPass().error shouldBe null
            d.resolveClashDecisions()
            d.pendingDecision shouldBe null

            val marker = d.state.getEntity(d.player2)?.get<SkipUntapComponent>()
            if (win) {
                // The freeze lands on the clash opponent, whom nothing targeted, and it is the
                // creatures-only axis — the printed clause never mentions lands.
                marker?.permanents shouldBe listOf(com.wingedsheep.sdk.scripting.GameObjectFilter.Creature)
            } else {
                marker.shouldBeNull()
            }
            // Either way the caster is untouched — "that player" is the opponent, not both.
            d.state.getEntity(d.player1)?.get<SkipUntapComponent>().shouldBeNull()
        }
    }

    test("the fog resolves even when the clash is lost") {
        val d = driver()
        val attacker = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        d.removeSummoningSickness(attacker)
        d.putCardOnTopOfLibrary(d.player1, "Plains")
        d.putCardOnTopOfLibrary(d.player2, "Lullaby Boulder")

        val spell = d.putCardInHand(d.player1, "Pollen Lullaby")
        d.giveMana(d.player1, Color.WHITE, 2)
        d.castSpell(d.player1, spell, emptyList()).error shouldBe null
        d.bothPass().error shouldBe null
        d.resolveClashDecisions()
        d.pendingDecision shouldBe null

        val startingLife = d.getLifeTotal(d.player2)
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, listOf(attacker), d.player2).error shouldBe null
        d.passPriorityUntil(Step.END_COMBAT)

        d.getLifeTotal(d.player2) shouldBe startingLife
    }
})
