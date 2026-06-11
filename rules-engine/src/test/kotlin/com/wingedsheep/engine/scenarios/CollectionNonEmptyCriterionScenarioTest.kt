package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.LoseLifeEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.SuccessCriterion
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.matchers.shouldBe

/**
 * [SuccessCriterion.CollectionNonEmpty] must gate a `Gate.DoAction` on the actual pipeline
 * collection the action stored — both when the action drains through continuations (the
 * select pauses for a decision) and on the declined path. Before the collection-propagation
 * seam ([com.wingedsheep.engine.handlers.continuations.exposeCollectionsToNextFrame]) the
 * criterion silently fell back to the Auto zone-probe's "it happened" default, so the
 * declined path below would have (wrongly) drawn a card instead of losing life.
 */
class CollectionNonEmptyCriterionScenarioTest : ScenarioTestBase() {

    // "Exile up to one card from your graveyard. If you do, draw a card.
    //  Otherwise, you lose 1 life."
    private val scavengersBargain = card("Scavenger's Bargain") {
        manaCost = "{B}"
        typeLine = "Sorcery"
        oracleText = "Exile up to one card from your graveyard. If you do, draw a card. Otherwise, you lose 1 life."
        spell {
            effect = Effects.IfYouDo(
                action = Effects.Composite(
                    GatherCardsEffect(CardSource.FromZone(Zone.GRAVEYARD), storeAs = "gathered"),
                    SelectFromCollectionEffect(
                        from = "gathered",
                        selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                        storeSelected = "exiled",
                    ),
                    MoveCollectionEffect(from = "exiled", destination = CardDestination.ToZone(Zone.EXILE)),
                ),
                ifYouDo = Effects.DrawCards(1),
                ifYouDont = LoseLifeEffect(DynamicAmount.Fixed(1), EffectTarget.Controller),
                successCriterion = SuccessCriterion.CollectionNonEmpty("exiled"),
            )
        }
    }

    init {
        cardRegistry.register(scavengersBargain)

        fun buildGame() = scenario()
            .withPlayers("Scavenger", "Opponent")
            .withCardInGraveyard(1, "Grizzly Bears")
            .withCardInHand(1, "Scavenger's Bargain")
            .withCardInLibrary(1, "Forest")
            .withLandsOnBattlefield(1, "Swamp", 1)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        test("exiling a card satisfies CollectionNonEmpty — the payoff runs") {
            val game = buildGame()
            val bearsId = game.state.getGraveyard(game.player1Id).single()

            game.castSpell(1, "Scavenger's Bargain")
            game.resolveStack()
            game.selectCards(listOf(bearsId))

            game.state.getExile(game.player1Id).size shouldBe 1
            game.state.getHand(game.player1Id).size shouldBe 1 // drew the Forest
            game.state.getEntity(game.player1Id)!!.get<LifeTotalComponent>()!!.life shouldBe 20
        }

        test("declining leaves the collection empty — the otherwise branch runs") {
            val game = buildGame()

            game.castSpell(1, "Scavenger's Bargain")
            game.resolveStack()
            game.skipSelection()

            game.state.getExile(game.player1Id).size shouldBe 0
            game.state.getHand(game.player1Id).size shouldBe 0 // no draw
            game.state.getEntity(game.player1Id)!!.get<LifeTotalComponent>()!!.life shouldBe 19
        }
    }
}
