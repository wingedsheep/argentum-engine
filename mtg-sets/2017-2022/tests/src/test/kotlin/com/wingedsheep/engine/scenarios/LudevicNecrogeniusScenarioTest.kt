package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.DoubleFacedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Supertype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/**
 * Ludevic, Necrogenius // Olag, Ludevic's Hubris (MID #233).
 *
 * The transform cost exiles X creature cards linked to Ludevic; as it transforms, Olag becomes a copy
 * of one of them — "its name is Olag, Ludevic's Hubris, it's 4/4, and it's a legendary blue and black
 * Zombie in addition to its other colors and types" — and gets a +1/+1 counter per creature card
 * exiled with it. The rulings pinned here: the copy is chosen as it transforms, it lasts after the
 * card leaves exile, Olag with nothing exiled is its plain 4/4 self, and a later transform turns the
 * card over without undoing the copy.
 */
class LudevicNecrogeniusScenarioTest : ScenarioTestBase() {

    private val transformAbilityId
        get() = cardRegistry.getCard("Ludevic, Necrogenius")!!.script.activatedAbilities[0].id

    private fun builder() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Ludevic, Necrogenius")
        .withLandsOnBattlefield(1, "Island", 3)
        .withLandsOnBattlefield(1, "Swamp", 3)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

    private fun TestGame.card(id: EntityId): CardComponent = state.getEntity(id)!!.get<CardComponent>()!!

    private fun TestGame.plusOnes(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun TestGame.moveExiledToLibrary(cardId: EntityId) {
        state = state
            .removeFromZone(ZoneKey(player1Id, Zone.EXILE), cardId)
            .addToZone(ZoneKey(player1Id, Zone.LIBRARY), cardId)
    }

    private fun TestGame.activateTransform(x: Int, exiled: List<EntityId>, beforeResolving: () -> Unit = {}) {
        val result = execute(ActivateAbility(
            playerId = player1Id,
            sourceId = findPermanent("Ludevic, Necrogenius")!!,
            abilityId = transformAbilityId,
            xValue = x,
            costPayment = AdditionalCostPayment(exiledCards = exiled),
        ))
        withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
        beforeResolving()
        resolveStack()
    }

    init {
        context("Ludevic, Necrogenius") {

            test("X can't be 0") {
                val game = builder().withCardInGraveyard(1, "Grizzly Bears").build()
                val offer = game.getLegalActions(1).single {
                    (it.action as? ActivateAbility)?.abilityId == transformAbilityId
                }
                offer.minX shouldBe 1
            }

            test("as it transforms it becomes a copy of the chosen exiled creature card, with Olag's exceptions") {
                val game = builder()
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInGraveyard(1, "Hill Giant")
                    .build()
                val ludevic = game.findPermanent("Ludevic, Necrogenius")!!
                val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()
                val giant = game.findCardsInGraveyard(1, "Hill Giant").single()

                game.activateTransform(2, listOf(bears, giant))

                withClue("both exiled cards are linked to Ludevic") {
                    game.state.getEntity(ludevic)!!.get<LinkedExileComponent>()!!.exiledIds shouldContainExactlyInAnyOrder listOf(bears, giant)
                }
                val decision = game.getPendingDecision() as SelectCardsDecision
                withClue("its controller picks one of the creature cards exiled with it") {
                    decision.options shouldContainExactlyInAnyOrder listOf(bears, giant)
                    decision.minSelections shouldBe 1
                    decision.maxSelections shouldBe 1
                }
                game.selectCards(listOf(giant)).error shouldBe null

                val olag = game.card(ludevic)
                olag.name shouldBe "Olag, Ludevic's Hubris"
                olag.typeLine.supertypes shouldContainAll listOf(Supertype.LEGENDARY)
                olag.typeLine.subtypes.map { it.value } shouldContainExactlyInAnyOrder listOf("Giant", "Zombie")
                olag.colors shouldContainExactlyInAnyOrder listOf(Color.RED, Color.BLUE, Color.BLACK)
                withClue("4/4 plus a +1/+1 counter for each of the two creature cards exiled with it") {
                    game.plusOnes(ludevic) shouldBe 2
                    game.state.projectedState.getPower(ludevic) shouldBe 6
                    game.state.projectedState.getToughness(ludevic) shouldBe 6
                }
            }

            test("with a single exiled card it copies that card without asking, and keeps the copy after the card leaves exile") {
                val game = builder().withCardInGraveyard(1, "Wind Drake").build()
                val ludevic = game.findPermanent("Ludevic, Necrogenius")!!
                val drake = game.findCardsInGraveyard(1, "Wind Drake").single()

                game.activateTransform(1, listOf(drake))

                game.hasPendingDecision() shouldBe false
                game.card(ludevic).name shouldBe "Olag, Ludevic's Hubris"
                withClue("the copied Wind Drake's flying comes along") {
                    game.state.projectedState.hasKeyword(ludevic, com.wingedsheep.sdk.core.Keyword.FLYING) shouldBe true
                }
                game.state.projectedState.getPower(ludevic) shouldBe 5

                // The copied card leaves exile (shuffled back into its owner's library).
                game.moveExiledToLibrary(drake)
                withClue("Olag remains a copy of the card even after it leaves exile") {
                    game.state.projectedState.hasKeyword(ludevic, com.wingedsheep.sdk.core.Keyword.FLYING) shouldBe true
                    game.card(ludevic).name shouldBe "Olag, Ludevic's Hubris"
                }
            }

            test("a card that leaves exile in response is neither a copy choice nor counted") {
                val game = builder()
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInGraveyard(1, "Hill Giant")
                    .build()
                val ludevic = game.findPermanent("Ludevic, Necrogenius")!!
                val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()
                val giant = game.findCardsInGraveyard(1, "Hill Giant").single()

                // X = 2, but the Hill Giant leaves exile while the ability is on the stack.
                game.activateTransform(2, listOf(bears, giant)) { game.moveExiledToLibrary(giant) }

                withClue("only the Bears is still exiled with it, so it is copied without asking") {
                    game.hasPendingDecision() shouldBe false
                    game.card(ludevic).typeLine.subtypes.map { it.value } shouldContainExactlyInAnyOrder listOf("Bear", "Zombie")
                }
                withClue("one creature card exiled with it: one +1/+1 counter, not X") {
                    game.plusOnes(ludevic) shouldBe 1
                    game.state.projectedState.getPower(ludevic) shouldBe 5
                }
            }

            test("transformed another way with nothing exiled with it, Olag is its plain 4/4 self with no counters") {
                val game = builder()
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardInHand(1, "Moonmist")
                    .build()
                val ludevic = game.findPermanent("Ludevic, Necrogenius")!!

                game.castSpell(1, "Moonmist").error shouldBe null
                game.resolveStack()

                game.card(ludevic).name shouldBe "Olag, Ludevic's Hubris"
                game.plusOnes(ludevic) shouldBe 0
                game.state.projectedState.getPower(ludevic) shouldBe 4
                game.card(ludevic).typeLine.subtypes.map { it.value } shouldBe listOf("Zombie")
            }

            test("a later transform turns the card over but the copy keeps setting its characteristics") {
                val game = builder()
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withCardInGraveyard(1, "Elite Vanguard")
                    .withCardInHand(1, "Moonmist")
                    .withCardInHand(1, "Moonmist")
                    .build()
                val ludevic = game.findPermanent("Ludevic, Necrogenius")!!
                val vanguard = game.findCardsInGraveyard(1, "Elite Vanguard").single()

                game.activateTransform(1, listOf(vanguard))
                game.card(ludevic).name shouldBe "Olag, Ludevic's Hubris"
                withClue("as a copy of Elite Vanguard Olag is a Human, so Moonmist transforms it") {
                    game.card(ludevic).typeLine.subtypes.map { it.value } shouldContainExactlyInAnyOrder listOf("Human", "Soldier", "Zombie")
                }

                game.castSpell(1, "Moonmist").error shouldBe null
                game.resolveStack()

                withClue("it turned over to its front face …") {
                    game.state.getEntity(ludevic)!!.get<DoubleFacedComponent>()!!.currentFace shouldBe DoubleFacedComponent.Face.FRONT
                }
                withClue("… but doesn't become Ludevic again: still Olag's copy of Elite Vanguard, 4/4 plus its counter") {
                    game.card(ludevic).name shouldBe "Olag, Ludevic's Hubris"
                    game.state.projectedState.getPower(ludevic) shouldBe 5
                    game.state.projectedState.getToughness(ludevic) shouldBe 5
                }

                game.castSpell(1, "Moonmist").error shouldBe null
                game.resolveStack()

                withClue("turning back to Olag under the copy doesn't apply Olag's as-transforms copy again") {
                    game.state.getEntity(ludevic)!!.get<DoubleFacedComponent>()!!.currentFace shouldBe DoubleFacedComponent.Face.BACK
                    game.hasPendingDecision() shouldBe false
                    game.plusOnes(ludevic) shouldBe 1
                    game.card(ludevic).typeLine.subtypes.map { it.value } shouldContainExactlyInAnyOrder listOf("Human", "Soldier", "Zombie")
                    game.state.projectedState.getPower(ludevic) shouldBe 5
                }
            }
        }
    }
}
