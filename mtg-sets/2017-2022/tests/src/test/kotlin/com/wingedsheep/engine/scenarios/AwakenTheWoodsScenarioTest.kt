package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AbilityId
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class AwakenTheWoodsScenarioTest : ScenarioTestBase() {
    init {
        test("X creates green nonbasic Forest Dryad land creatures with BRO token art") {
            val game = scenario().withPlayers()
                .withCardInHand(1, "Awaken the Woods")
                .withCardInHand(1, "Forest")
                .withLandsOnBattlefield(1, "Forest", 5)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castXSpell(1, "Awaken the Woods", xValue = 3).error shouldBe null
            game.resolveStack()

            val tokens = game.findAllPermanents("Forest Dryad")
            tokens shouldHaveSize 3
            val projected = game.state.projectedState
            tokens.forEach { token ->
                projected.getController(token) shouldBe game.player1Id
                projected.isCreature(token) shouldBe true
                projected.hasType(token, "LAND") shouldBe true
                projected.hasSubtype(token, "Forest") shouldBe true
                projected.hasSubtype(token, "Dryad") shouldBe true
                projected.getColors(token) shouldBe setOf("GREEN")
                projected.getPower(token) shouldBe 1
                projected.getToughness(token) shouldBe 1
                val entity = game.state.getEntity(token)!!
                entity.has<TokenComponent>() shouldBe true
                entity.has<TappedComponent>() shouldBe false
                entity.get<CardComponent>()!!.typeLine.isBasicLand shouldBe false
                entity.get<CardComponent>()!!.imageUri shouldBe
                    "https://cards.scryfall.io/normal/front/7/4/74de70f2-93b6-4fc5-8c4d-464f880d3c54.jpg?1783919910"
            }
            // Creating lands doesn't consume the turn's land play.
            game.getLegalActions(1).map { it.action }.filterIsInstance<PlayLand>() shouldHaveSize 1
        }

        test("X zero creates no tokens or landfall triggers") {
            val game = scenario().withPlayers()
                .withCardInHand(1, "Awaken the Woods")
                .withCardOnBattlefield(1, "Courser of Kruphix")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withCardInLibrary(1, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castXSpell(1, "Awaken the Woods", xValue = 0).error shouldBe null
            game.resolveStack()
            game.findAllPermanents("Forest Dryad") shouldHaveSize 0
            game.getLifeTotal(1) shouldBe 20
        }

        test("each created land fires landfall") {
            val game = scenario().withPlayers()
                .withCardInHand(1, "Awaken the Woods")
                .withCardOnBattlefield(1, "Courser of Kruphix")
                .withLandsOnBattlefield(1, "Forest", 4)
                .withCardInLibrary(1, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castXSpell(1, "Awaken the Woods", xValue = 2).error shouldBe null
            game.resolveStack()
            game.findAllPermanents("Forest Dryad") shouldHaveSize 2
            game.getLifeTotal(1) shouldBe 22
        }

        test("token doubling applies to the evaluated X count") {
            val game = scenario().withPlayers()
                .withCardInHand(1, "Awaken the Woods")
                .withCardOnBattlefield(1, "Doubling Season")
                .withLandsOnBattlefield(1, "Forest", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castXSpell(1, "Awaken the Woods", xValue = 2).error shouldBe null
            game.resolveStack()
            game.findAllPermanents("Forest Dryad") shouldHaveSize 4
        }

        test("haste permits the new Forest Dryad's mana ability immediately") {
            val game = scenario().withPlayers()
                .withCardInHand(1, "Awaken the Woods")
                .withCardOnBattlefield(1, "Concordant Crossroads")
                .withLandsOnBattlefield(1, "Forest", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castXSpell(1, "Awaken the Woods", xValue = 1).error shouldBe null
            game.resolveStack()
            val token = game.findAllPermanents("Forest Dryad").single()
            game.getLegalActions(1).filter { it.isAffordable }.map { it.action }.filterIsInstance<ActivateAbility>()
                .any { it.sourceId == token } shouldBe true
            game.execute(ActivateAbility(game.player1Id, token, AbilityId.intrinsicMana('G'))).error shouldBe null
            game.state.getEntity(game.player1Id)!!.get<ManaPoolComponent>()!!.green shouldBe 1
        }

        test("Forest mana is summoning sick then becomes usable on the next own turn") {
            val game = scenario().withPlayers()
                .withCardInHand(1, "Awaken the Woods")
                .withLandsOnBattlefield(1, "Forest", 3)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castXSpell(1, "Awaken the Woods", xValue = 1).error shouldBe null
            game.resolveStack()
            val token = game.findAllPermanents("Forest Dryad").single()
            game.state.getEntity(token)!!.has<SummoningSicknessComponent>() shouldBe true
            game.state.projectedState.isCreature(token) shouldBe true
            game.state.projectedState.hasKeyword(token, com.wingedsheep.sdk.core.Keyword.HASTE) shouldBe false
            val tap = ActivateAbility(game.player1Id, token, AbilityId.intrinsicMana('G'))
            game.getLegalActions(1).filter { it.isAffordable }.map { it.action }.filterIsInstance<ActivateAbility>()
                .any { it.sourceId == token } shouldBe false
            game.execute(tap).error shouldNotBe null
            game.state.getEntity(token)!!.has<TappedComponent>() shouldBe false

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            game.state.activePlayerId shouldBe game.player2Id
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            game.state.activePlayerId shouldBe game.player1Id
            game.getLegalActions(1).filter { it.isAffordable }.map { it.action }.filterIsInstance<ActivateAbility>()
                .any { it.sourceId == token } shouldBe true
            game.execute(tap).error shouldBe null
            game.state.getEntity(token)!!.has<TappedComponent>() shouldBe true
            game.state.getEntity(game.player1Id)!!.get<ManaPoolComponent>()!!.green shouldBe 1
        }
    }
}
