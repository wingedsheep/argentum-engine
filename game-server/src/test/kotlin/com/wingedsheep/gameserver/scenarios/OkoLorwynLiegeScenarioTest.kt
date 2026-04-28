package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import java.util.concurrent.atomic.AtomicLong

/**
 * Scenario tests for Oko, Lorwyn Liege // Oko, Shadowmoor Scion.
 *
 * Coverage:
 *  - Front +1: target creature gets -2/-0 until your next turn
 *  - Front +2: target creature gains all creature types (Changeling, Permanent)
 *  - Back  -1: mill three; may put a permanent card from among them into your hand
 *  - Back  -3: create two 3/3 green Elk creature tokens
 *  - Back  -6: emblem grants +3/+3, vigilance, hexproof to creatures of chosen type you control
 */
class OkoLorwynLiegeScenarioTest : ScenarioTestBase() {

    private val helperEntityIdCounter = AtomicLong(50_000)

    init {
        context("Oko, Lorwyn Liege // Oko, Shadowmoor Scion") {

            test("+1 ability: target creature gets -2/-0") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Oko, Lorwyn Liege")
                    .withCardOnBattlefield(2, "Alpine Grizzly") // 4/2
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val okoId = game.findPermanent("Oko, Lorwyn Liege")!!
                val grizzlyId = game.findPermanent("Alpine Grizzly")!!
                game.setLoyalty(okoId, 3)

                val cardDef = cardRegistry.getCard("Oko, Lorwyn Liege")!!
                // ordering: +2 ability is first, +1 ability is second
                val plusOne = cardDef.script.activatedAbilities[1]

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = okoId,
                        abilityId = plusOne.id,
                        targets = listOf(
                            com.wingedsheep.engine.state.components.stack.ChosenTarget.Permanent(grizzlyId)
                        )
                    )
                )
                withClue("+1 activation should succeed: ${result.error}") { result.error shouldBe null }
                game.resolveStack()

                val projected = game.state.projectedState
                withClue("Alpine Grizzly should drop to 2 power") {
                    projected.getPower(grizzlyId) shouldBe 2
                }
                withClue("Toughness should be unchanged") {
                    projected.getToughness(grizzlyId) shouldBe 2
                }
            }

            test("+2 ability: target creature gains all creature types via Changeling") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Oko, Lorwyn Liege")
                    .withCardOnBattlefield(2, "Alpine Grizzly") // Bear
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val okoId = game.findPermanent("Oko, Lorwyn Liege")!!
                val grizzlyId = game.findPermanent("Alpine Grizzly")!!
                game.setLoyalty(okoId, 3)

                val cardDef = cardRegistry.getCard("Oko, Lorwyn Liege")!!
                val plusTwo = cardDef.script.activatedAbilities[0]

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = okoId,
                        abilityId = plusTwo.id,
                        targets = listOf(
                            com.wingedsheep.engine.state.components.stack.ChosenTarget.Permanent(grizzlyId)
                        )
                    )
                )
                withClue("+2 activation should succeed: ${result.error}") { result.error shouldBe null }
                game.resolveStack()

                val projected = game.state.projectedState
                withClue("Alpine Grizzly should now have Changeling") {
                    projected.getKeywords(grizzlyId).contains(Keyword.CHANGELING.name) shouldBe true
                }
                val subtypes = projected.getSubtypes(grizzlyId)
                withClue("Alpine Grizzly should keep its Bear subtype") { subtypes shouldContain "Bear" }
                withClue("Alpine Grizzly should also have Goblin (every creature type)") {
                    subtypes shouldContain "Goblin"
                }
                withClue("Alpine Grizzly should also have Elf (every creature type)") {
                    subtypes shouldContain "Elf"
                }
            }

            test("back -3 ability: creates two 3/3 green Elk creature tokens") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Oko, Shadowmoor Scion")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val okoId = game.findPermanent("Oko, Shadowmoor Scion")!!
                game.setLoyalty(okoId, 3)

                val cardDef = cardRegistry.getCard("Oko, Shadowmoor Scion")!!
                // back face activated abilities ordering: -1, -3, -6
                val minusThree = cardDef.script.activatedAbilities[1]

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = okoId,
                        abilityId = minusThree.id
                    )
                )
                withClue("-3 activation should succeed: ${result.error}") { result.error shouldBe null }
                game.resolveStack()

                val elks = game.findAllPermanents("Elk Token")
                withClue("Two Elk tokens should be created") { elks.size shouldBe 2 }
                val projected = game.state.projectedState
                for (elk in elks) {
                    withClue("Each Elk should be 3/3") {
                        projected.getPower(elk) shouldBe 3
                        projected.getToughness(elk) shouldBe 3
                    }
                }
            }

            test("back -1 ability: mill three, may put a permanent card into your hand") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Oko, Shadowmoor Scion")
                    .withCardInLibrary(1, "Alpine Grizzly")  // permanent (creature)
                    .withCardInLibrary(1, "Blaze")           // not a permanent (sorcery)
                    .withCardInLibrary(1, "Mountain")        // permanent (land)
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val okoId = game.findPermanent("Oko, Shadowmoor Scion")!!
                game.setLoyalty(okoId, 1)

                val cardDef = cardRegistry.getCard("Oko, Shadowmoor Scion")!!
                val minusOne = cardDef.script.activatedAbilities[0]

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = okoId,
                        abilityId = minusOne.id
                    )
                )
                withClue("-1 activation should succeed: ${result.error}") { result.error shouldBe null }
                game.resolveStack()

                val decision = game.getPendingDecision()
                check(decision is SelectCardsDecision) {
                    "Expected SelectCardsDecision for mill+keep, got ${decision?.javaClass?.simpleName}"
                }
                val grizzlyOption = decision.options.first { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Alpine Grizzly"
                }
                game.selectCards(listOf(grizzlyOption))

                withClue("Grizzly should be in hand") {
                    game.isInHand(1, "Alpine Grizzly") shouldBe true
                }
                withClue("Blaze should be in graveyard") {
                    game.isInGraveyard(1, "Blaze") shouldBe true
                }
                withClue("Mountain should be in graveyard (not selected)") {
                    game.isInGraveyard(1, "Mountain") shouldBe true
                }
            }

            test("back -6 emblem: creatures of chosen type you control get +3/+3, vigilance, hexproof") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Oko, Shadowmoor Scion")
                    .withCardOnBattlefield(1, "Alpine Grizzly") // Bear, 4/2
                    .withCardOnBattlefield(2, "Alpine Grizzly") // opponent's Bear should NOT get bonus
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val okoId = game.findPermanent("Oko, Shadowmoor Scion")!!
                val ownGrizzlyId = game.findAllPermanents("Alpine Grizzly")
                    .first { game.state.getEntity(it)?.get<ControllerComponent>()?.playerId == game.player1Id }
                val opponentGrizzlyId = game.findAllPermanents("Alpine Grizzly")
                    .first { game.state.getEntity(it)?.get<ControllerComponent>()?.playerId == game.player2Id }

                game.setLoyalty(okoId, 6)

                val cardDef = cardRegistry.getCard("Oko, Shadowmoor Scion")!!
                val minusSix = cardDef.script.activatedAbilities[2]

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = okoId,
                        abilityId = minusSix.id
                    )
                )
                withClue("-6 activation should succeed: ${result.error}") { result.error shouldBe null }
                game.resolveStack()

                // Choose creature type: Bear
                val typeDecision = game.getPendingDecision()
                check(typeDecision is ChooseOptionDecision) {
                    "Expected creature-type choice, got ${typeDecision?.javaClass?.simpleName}"
                }
                val bearIndex = typeDecision.options.indexOf("Bear")
                check(bearIndex >= 0) { "Bear must be a valid creature type option" }
                game.submitDecision(OptionChosenResponse(typeDecision.id, bearIndex))
                game.resolveStack()

                val projected = game.state.projectedState

                withClue("Player 1's Alpine Grizzly (Bear) should get +3/+3") {
                    projected.getPower(ownGrizzlyId) shouldBe 7
                    projected.getToughness(ownGrizzlyId) shouldBe 5
                }
                withClue("Player 1's Alpine Grizzly should gain vigilance") {
                    projected.getKeywords(ownGrizzlyId).contains(Keyword.VIGILANCE.name) shouldBe true
                }
                withClue("Player 1's Alpine Grizzly should gain hexproof") {
                    projected.getKeywords(ownGrizzlyId).contains(Keyword.HEXPROOF.name) shouldBe true
                }
                withClue("Opponent's Alpine Grizzly should NOT get the bonus") {
                    projected.getPower(opponentGrizzlyId) shouldBe 4
                    projected.getToughness(opponentGrizzlyId) shouldBe 2
                    projected.getKeywords(opponentGrizzlyId).contains(Keyword.VIGILANCE.name) shouldBe false
                }

                // A new Bear entering later should also pick up the bonus (dynamic re-evaluation)
                val newBearId = game.addBattlefieldCreature(game.player1Id, "Alpine Grizzly")
                val projectedAfter = game.state.projectedState
                withClue("Newly entered Bear should also get +3/+3 from the emblem") {
                    projectedAfter.getPower(newBearId) shouldBe 7
                    projectedAfter.getToughness(newBearId) shouldBe 5
                }
            }
        }
    }

    private fun TestGame.setLoyalty(entityId: EntityId, count: Int) {
        state = state.updateEntity(entityId) { c ->
            val counters = c.get<CountersComponent>() ?: CountersComponent()
            c.with(counters.withAdded(CounterType.LOYALTY, count))
        }
    }

    private fun TestGame.addBattlefieldCreature(ownerId: EntityId, cardName: String): EntityId {
        val cardDef = cardRegistry.getCard(cardName) ?: error("Card not found: $cardName")
        val cardId = EntityId.of("test-card-${helperEntityIdCounter.incrementAndGet()}")
        val cardComponent = CardComponent(
            cardDefinitionId = cardDef.name,
            name = cardDef.name,
            manaCost = cardDef.manaCost,
            typeLine = cardDef.typeLine,
            oracleText = cardDef.oracleText,
            colors = cardDef.colors,
            baseKeywords = cardDef.keywords,
            baseFlags = cardDef.flags,
            baseStats = cardDef.creatureStats,
            ownerId = ownerId,
            spellEffect = cardDef.spellEffect,
            imageUri = cardDef.metadata.imageUri
        )
        val container = ComponentContainer.of(
            cardComponent,
            OwnerComponent(ownerId),
            ControllerComponent(ownerId)
        )
        state = state.withEntity(cardId, container)
        state = state.addToZone(ZoneKey(ownerId, Zone.BATTLEFIELD), cardId)
        return state.let { _ -> cardId }
    }
}
