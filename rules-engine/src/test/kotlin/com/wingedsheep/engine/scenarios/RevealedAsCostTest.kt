package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.conditions.EntityMatches
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * `EffectTarget.RevealedAsCost` — "the revealed card", the card shown from hand to pay a spell's
 * additional reveal cost (`Costs.additional.RevealFromHand`). Titan's Presence reads its power.
 *
 * Pinned here:
 *  - a value read gives the revealed card's power while it sits in hand (CR 701.20b: revealing
 *    moves nothing);
 *  - Titan's Presence's ruling — if the card isn't in hand any more at resolution, use its power
 *    "as it last existed in your hand": a revealed card cast in response and pumped on the
 *    battlefield is still read at its in-hand power;
 *  - an `EntityMatches` over the revealed card;
 *  - with no reveal paid the reference names nothing, so the amount is 0.
 */
class RevealedAsCostTest : ScenarioTestBase() {

    init {
        val powerProbe = card("Reveal Probe") {
            manaCost = "{0}"
            typeLine = "Instant"
            oracleText = "As an additional cost to cast this spell, reveal a creature card from your hand. " +
                "You gain life equal to the revealed card's power."
            additionalCost(Costs.additional.RevealFromHand(filter = GameObjectFilter.Creature))
            spell {
                effect = Effects.GainLife(DynamicAmounts.powerOf(EffectTarget.RevealedAsCost()))
            }
        }
        val typeProbe = card("Reveal Sorter") {
            manaCost = "{0}"
            typeLine = "Instant"
            oracleText = "As an additional cost to cast this spell, reveal a card from your hand. " +
                "If the revealed card is an artifact card, you gain 5 life."
            additionalCost(Costs.additional.RevealFromHand())
            spell {
                effect = Effects.If(
                    condition = EntityMatches(EffectTarget.RevealedAsCost(), GameObjectFilter.Artifact),
                    then = Effects.GainLife(5),
                )
            }
        }
        val noRevealProbe = card("Unrevealing Probe") {
            manaCost = "{0}"
            typeLine = "Instant"
            oracleText = "You gain life equal to the revealed card's power."
            spell {
                effect = Effects.GainLife(DynamicAmounts.powerOf(EffectTarget.RevealedAsCost()))
            }
        }
        val flashGolem = card("Flash Golem") {
            manaCost = "{0}"
            typeLine = "Artifact Creature — Golem"
            power = 3
            toughness = 3
            keywords(Keyword.FLASH)
        }
        val banner = card("Golem Banner") {
            manaCost = "{0}"
            typeLine = "Artifact"
            staticAbility {
                ability = ModifyStats(
                    powerBonus = 2,
                    toughnessBonus = 0,
                    filter = GroupFilter(GameObjectFilter.Creature.youControl()),
                )
            }
        }
        val bear = card("Hand Bear") {
            manaCost = "{1}{G}"
            typeLine = "Creature — Bear"
            power = 2
            toughness = 2
        }
        listOf(powerProbe, typeProbe, noRevealProbe, flashGolem, banner, bear).forEach { cardRegistry.register(it) }

        fun TestGame.handCard(name: String): EntityId =
            state.getHand(player1Id).first { state.getEntity(it)?.get<CardComponent>()?.name == name }

        fun TestGame.castRevealing(spell: String, revealed: String) =
            execute(
                CastSpell(
                    playerId = player1Id,
                    cardId = handCard(spell),
                    additionalCostPayment = AdditionalCostPayment(revealedCards = listOf(handCard(revealed))),
                )
            )

        fun board(vararg hand: String, banner: Boolean = false): TestGame {
            var builder = scenario()
                .withPlayers("Player", "Opponent")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            hand.forEach { builder = builder.withCardInHand(1, it) }
            if (banner) builder = builder.withCardOnBattlefield(1, "Golem Banner")
            repeat(5) { builder = builder.withCardInLibrary(1, "Forest") }
            repeat(5) { builder = builder.withCardInLibrary(2, "Forest") }
            return builder.build()
        }

        test("reads the power of the card revealed, which stays in hand") {
            val game = board("Reveal Probe", "Flash Golem", "Hand Bear")
            val life = game.getLifeTotal(1)

            game.castRevealing("Reveal Probe", "Hand Bear").error shouldBe null
            game.resolveStack()

            game.getLifeTotal(1) shouldBe life + 2
            game.state.getHand(game.player1Id) shouldContain game.handCard("Hand Bear")
        }

        test("a revealed card that left the hand is read at its power as it last existed in hand") {
            val game = board("Reveal Probe", "Flash Golem", banner = true)
            val life = game.getLifeTotal(1)

            game.castRevealing("Reveal Probe", "Flash Golem").error shouldBe null
            // Cast the revealed Golem in response; it resolves first and is a 5/3 under the Banner.
            game.castSpell(1, "Flash Golem").error shouldBe null
            game.passPriority()
            game.passPriority()
            val golem = game.findPermanent("Flash Golem")!!
            withClue("the Golem is on the battlefield and pumped") {
                game.state.projectedState.getPower(golem) shouldBe 5
            }
            game.resolveStack()

            withClue("the in-hand power (3), not the battlefield power (5)") {
                game.getLifeTotal(1) shouldBe life + 3
            }
        }

        test("an EntityMatches reads the revealed card") {
            val artifact = board("Reveal Sorter", "Flash Golem")
            val before = artifact.getLifeTotal(1)
            artifact.castRevealing("Reveal Sorter", "Flash Golem").error shouldBe null
            artifact.resolveStack()
            artifact.getLifeTotal(1) shouldBe before + 5

            val nonArtifact = board("Reveal Sorter", "Hand Bear")
            val start = nonArtifact.getLifeTotal(1)
            nonArtifact.castRevealing("Reveal Sorter", "Hand Bear").error shouldBe null
            nonArtifact.resolveStack()
            nonArtifact.getLifeTotal(1) shouldBe start
        }

        test("with no reveal paid the revealed card is nothing and its power is 0") {
            val game = board("Unrevealing Probe", "Hand Bear")
            val life = game.getLifeTotal(1)
            game.castSpell(1, "Unrevealing Probe").error shouldBe null
            game.resolveStack()
            game.getLifeTotal(1) shouldBe life
        }
    }
}
