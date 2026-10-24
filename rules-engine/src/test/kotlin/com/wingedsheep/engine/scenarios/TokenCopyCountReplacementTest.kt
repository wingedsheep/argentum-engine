package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.ModifyTokenCount
import com.wingedsheep.sdk.scripting.MultiplyTokenCreation
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Token-count replacements ("it creates twice that many of those tokens instead" — Doubling
 * Season, Parallel Lives; "that many plus one" — Mondrak's siblings) apply to *token copies*
 * exactly as to any other token creation: a token copy is still a token being created. Covers
 * both counted copy paths — a copy of a target (`CreateTokenCopyOfTarget`) and a copy of the
 * source (`CreateTokenCopyOfSelf`) — plus "under your control" scoping and stacking.
 */
class TokenCopyCountReplacementTest : ScenarioTestBase() {

    private val doubler = card("Copy Test Doubler") {
        typeLine = "Enchantment"
        replacementEffect(MultiplyTokenCreation())
    }
    private val plusOne = card("Copy Test Plus One") {
        typeLine = "Enchantment"
        replacementEffect(ModifyTokenCount(1))
    }
    private val copyTarget = card("Copy Test Replicate") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell {
            val t = target(TargetFilter.CreatureYouControl)
            effect = Effects.CreateTokenCopyOfTarget(t, count = 2)
        }
    }
    private val selfCopier = card("Copy Test Self Copier") {
        manaCost = "{1}"
        typeLine = "Creature — Shapeshifter"
        power = 1
        toughness = 1
        activatedAbility {
            cost = Costs.Tap
            effect = Effects.CreateTokenCopyOfSelf()
        }
    }

    init {
        cardRegistry.register(listOf(doubler, plusOne, copyTarget, selfCopier))

        fun base() = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardInHand(1, "Copy Test Replicate")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

        fun TestGame.replicateBears() {
            castSpell(1, "Copy Test Replicate", findPermanent("Grizzly Bears")!!).error shouldBe null
            resolveStack()
        }

        test("a doubler doubles a token copy of a target") {
            val game = base().withCardOnBattlefield(1, "Copy Test Doubler").build()
            game.replicateBears()
            withClue("the original plus 2 × 2 copies") {
                game.findPermanents("Grizzly Bears") shouldHaveSize 5
            }
        }

        test("an opponent's doubler does not double your token copies") {
            val game = base().withCardOnBattlefield(2, "Copy Test Doubler").build()
            game.replicateBears()
            game.findPermanents("Grizzly Bears") shouldHaveSize 3
        }

        test("a count modifier runs before the doubler on token copies") {
            val game = base()
                .withCardOnBattlefield(1, "Copy Test Doubler")
                .withCardOnBattlefield(1, "Copy Test Plus One")
                .build()
            game.replicateBears()
            withClue("(2 + 1) × 2 copies plus the original") {
                game.findPermanents("Grizzly Bears") shouldHaveSize 7
            }
        }

        test("a doubler doubles a token copy of the source") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Copy Test Self Copier")
                .withCardOnBattlefield(1, "Copy Test Doubler")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val abilityId = cardRegistry.getCard("Copy Test Self Copier")!!.script.activatedAbilities.single().id
            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = game.findPermanent("Copy Test Self Copier")!!,
                    abilityId = abilityId,
                )
            ).error shouldBe null
            game.resolveStack()
            game.findPermanents("Copy Test Self Copier") shouldHaveSize 3
        }
    }
}
