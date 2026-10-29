package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Summon: Esper Valigarmanda (FIC #60).
 *
 *   I — Exile an instant or sorcery card from each graveyard.
 *   II, III, IV — Add {R} for each lore counter on this Saga. You may cast an instant or sorcery
 *       card exiled with this Saga, and mana of any type can be spent to cast that spell.
 *
 * Pins: chapter I takes exactly one instant or sorcery from every graveyard, the Saga's controller
 * choosing; chapter II adds {R}{R} and lets that red mana pay a blue cost during resolution; the
 * cast is optional, and a declined card stays exiled for a later chapter; an opponent's exiled card
 * is cast by — and controlled by — the Saga's controller.
 */
class SummonEsperValigarmandaScenarioTest : ScenarioTestBase() {

    init {
        fun builder() = scenario()
            .withPlayers("Summoner", "Opponent")
            .withCardInHand(1, "Summon: Esper Valigarmanda")
            .withLandsOnBattlefield(1, "Mountain", 4)
            .withCardInGraveyard(1, "Divination")
            .withCardInGraveyard(1, "Opt")
            .withCardInGraveyard(1, "Grizzly Bears")
            .withCardInGraveyard(2, "Lightning Bolt")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .apply { repeat(10) { withCardInLibrary(1, "Mountain"); withCardInLibrary(2, "Mountain") } }

        /** Cast the Saga and answer chapter I, exiling [pick] from the graveyard that offers it, the first option elsewhere. */
        fun TestGame.castAndExile(pick: EntityId): Int {
            castSpell(1, "Summon: Esper Valigarmanda").error shouldBe null
            resolveStack()
            var prompts = 0
            while (true) {
                val decision = getPendingDecision() as? SelectCardsDecision ?: break
                prompts++
                withClue("the Saga's controller makes every graveyard's pick") {
                    decision.playerId shouldBe player1Id
                }
                val choice = if (pick in decision.options) pick else decision.options.first()
                selectCards(listOf(choice)).error shouldBe null
            }
            resolveStack()
            return prompts
        }

        /** Through the opponent's turn to our next precombat main, where chapter II triggers. */
        fun TestGame.advanceToNextOwnMain() {
            passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            state.activePlayerId shouldBe player1Id
        }

        fun TestGame.redInPool(): Int = state.getEntity(player1Id)?.get<ManaPoolComponent>()?.red ?: 0

        /** Resolve chapter II up to its cast prompt and return that prompt. */
        fun TestGame.resolveToCastPrompt(): SelectCardsDecision {
            resolveStack()
            return getPendingDecision().shouldNotBeNull() as SelectCardsDecision
        }

        test("chapter I exiles one instant or sorcery card from each graveyard") {
            val game = builder().build()
            val divination = game.findCardsInGraveyard(1, "Divination").single()
            val prompts = game.castAndExile(divination)

            withClue("your graveyard asks which of its two instants and sorceries to exile") { prompts shouldBe 1 }
            game.isInExile(1, "Divination") shouldBe true
            withClue("only one card per graveyard") { game.isInGraveyard(1, "Opt") shouldBe true }
            withClue("a creature card is never taken") { game.isInGraveyard(1, "Grizzly Bears") shouldBe true }
            withClue("the opponent's graveyard loses its only instant too") {
                game.isInExile(2, "Lightning Bolt") shouldBe true
            }
        }

        test("chapter II adds {R}{R} and that red mana casts the exiled blue sorcery") {
            val game = builder().build()
            val divination = game.findCardsInGraveyard(1, "Divination").single()
            val bolt = game.findCardsInGraveyard(2, "Lightning Bolt").single()
            game.castAndExile(divination)
            game.advanceToNextOwnMain()
            val handBefore = game.handSize(1)

            val prompt = game.resolveToCastPrompt()
            withClue("both cards exiled with the Saga are offered") {
                prompt.options shouldContainExactlyInAnyOrder listOf(
                    divination, bolt,
                )
            }
            withClue("the chapter's mana is already in the pool") { game.redInPool() shouldBe 2 }

            withClue("{2}{U} cast on resolution with only red mana available") {
                game.selectCards(listOf(divination)).error shouldBe null
            }
            game.state.stack shouldContain divination
            game.resolveStack()

            game.handSize(1) shouldBe handBefore + 2
            game.isInGraveyard(1, "Divination") shouldBe true
        }

        test("declining the cast leaves the card exiled, castable by a later chapter") {
            val game = builder().build()
            val divination = game.findCardsInGraveyard(1, "Divination").single()
            game.castAndExile(divination)
            game.advanceToNextOwnMain()

            game.resolveToCastPrompt()
            game.skipSelection().error shouldBe null
            game.resolveStack()

            game.isInExile(1, "Divination") shouldBe true
            game.redInPool() shouldBe 2
            withClue("the cast was only offered during resolution: it can't be cast now") {
                game.execute(CastSpell(game.player1Id, divination, emptyList())).error shouldNotBe null
            }

            // Chapter III, next turn: three lore counters, {R}{R}{R}, and the pile still holds both.
            game.advanceToNextOwnMain()
            val prompt = game.resolveToCastPrompt()
            game.redInPool() shouldBe 3
            prompt.options shouldContain divination
        }

        test("you cast and control an opponent's exiled instant") {
            val game = builder().build()
            val divination = game.findCardsInGraveyard(1, "Divination").single()
            val bolt = game.findCardsInGraveyard(2, "Lightning Bolt").single()
            game.castAndExile(divination)
            game.advanceToNextOwnMain()

            game.resolveToCastPrompt()
            game.selectCards(listOf(bolt)).error shouldBe null
            game.selectTargets(listOf(game.player2Id)).error shouldBe null
            game.state.stack shouldContain bolt
            withClue("the opponent's card is your spell") {
                game.state.getEntity(bolt)?.get<SpellOnStackComponent>()?.casterId shouldBe game.player1Id
            }
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 17
            withClue("the cast spent one of the chapter's red mana") { game.redInPool() shouldBe 1 }
            game.isInGraveyard(2, "Lightning Bolt") shouldBe true
        }
    }
}
