package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Sword of Once and Future — {3} Artifact — Equipment:
 *   "Equipped creature gets +2/+2 and has protection from blue and from black.
 *    Whenever equipped creature deals combat damage to a player, surveil 2. Then you may cast an
 *    instant or sorcery spell with mana value 2 or less from your graveyard without paying its mana
 *    cost. If that spell would be put into your graveyard, exile it instead.
 *    Equip {2}"
 *
 * Covers the static grant, a free cast of a card just surveiled into the graveyard (MV 3 excluded,
 * the spell is exiled after resolving), and declining the optional cast.
 */
class SwordOfOnceAndFutureScenarioTest : ScenarioTestBase() {

    private val equipAbilityId by lazy {
        cardRegistry.requireCard("Sword of Once and Future").activatedAbilities[0].id
    }

    init {
        cardRegistry.register(
            CardDefinition.sorcery(
                name = "Two Drop Draw",
                manaCost = ManaCost.parse("{1}{U}"),
                oracleText = "Draw a card.",
                script = CardScript(spellEffect = Effects.DrawCards(1))
            )
        )
        cardRegistry.register(
            CardDefinition.sorcery(
                name = "Three Drop Draw",
                manaCost = ManaCost.parse("{2}{U}"),
                oracleText = "Draw a card.",
                script = CardScript(spellEffect = Effects.DrawCards(1))
            )
        )

        fun setUp(): TestGame {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardOnBattlefield(1, "Sword of Once and Future")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withCardInGraveyard(1, "Three Drop Draw")
                .withCardInLibrary(1, "Two Drop Draw")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val giant = game.findPermanent("Hill Giant")!!
            val sword = game.findPermanent("Sword of Once and Future")!!
            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = sword,
                    abilityId = equipAbilityId,
                    targets = listOf(ChosenTarget.Permanent(giant))
                )
            ).error shouldBe null
            game.resolveStack()
            return game
        }

        fun attackAndSurveilTwoDropIntoGraveyard(game: TestGame) {
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Hill Giant" to 2))
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareNoBlockers()
            game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
            game.resolveStack()

            withClue("Opponent took 5 combat damage from the 5/5 equipped Giant") {
                game.getLifeTotal(2) shouldBe 15
            }

            // Surveil 2 — put Two Drop Draw into the graveyard, keep the other on top.
            val surveil = game.getPendingDecision()
            surveil.shouldBeInstanceOf<SelectCardsDecision>()
            surveil.options.size shouldBe 2
            val twoDrop = surveil.options.single { game.state.getEntity(it)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name == "Two Drop Draw" }
            game.selectCards(listOf(twoDrop)).error shouldBe null
            if (game.getPendingDecision() is ReorderLibraryDecision) game.keepLibraryOrder()
        }

        test("equipped creature gets +2/+2 and protection from blue and from black") {
            val game = setUp()
            val giant = game.findPermanent("Hill Giant")!!
            game.state.projectedState.getPower(giant) shouldBe 5
            game.state.projectedState.getToughness(giant) shouldBe 5
            game.state.projectedState.hasKeyword(giant, "PROTECTION_FROM_BLUE") shouldBe true
            game.state.projectedState.hasKeyword(giant, "PROTECTION_FROM_BLACK") shouldBe true
            game.state.projectedState.hasKeyword(giant, "PROTECTION_FROM_RED") shouldBe false
        }

        test("free-casts a surveiled MV-2 sorcery from the graveyard, then exiles it") {
            val game = setUp()
            attackAndSurveilTwoDropIntoGraveyard(game)

            val choice = game.getPendingDecision()
            choice.shouldBeInstanceOf<SelectCardsDecision>()
            withClue("only the MV <= 2 card is offered (Three Drop Draw is excluded)") {
                choice.options.size shouldBe 1
                choice.minSelections shouldBe 0
            }
            val handBefore = game.state.getHand(game.player1Id).size
            game.selectCards(choice.options).error shouldBe null
            game.resolveStack()

            withClue("the free-cast sorcery resolved (drew a card) and was exiled instead of graveyarded") {
                game.state.getHand(game.player1Id).size shouldBe handBefore + 1
                game.isInExile(1, "Two Drop Draw") shouldBe true
                game.isInGraveyard(1, "Two Drop Draw") shouldBe false
                game.isInGraveyard(1, "Three Drop Draw") shouldBe true
            }
            withClue("no mana was spent") {
                game.state.getBattlefield().count { id ->
                    game.state.getEntity(id)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name == "Plains" &&
                        game.state.getEntity(id)?.has<com.wingedsheep.engine.state.components.battlefield.TappedComponent>() == true
                } shouldBe 2 // tapped for equip only
            }
        }

        test("the cast is optional — declining leaves the card in the graveyard") {
            val game = setUp()
            attackAndSurveilTwoDropIntoGraveyard(game)

            game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            game.skipSelection().error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Two Drop Draw") shouldBe true
            game.isInExile(1, "Two Drop Draw") shouldBe false
        }
    }
}
