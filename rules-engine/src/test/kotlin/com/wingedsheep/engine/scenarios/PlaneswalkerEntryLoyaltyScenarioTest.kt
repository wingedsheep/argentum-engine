package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.handlers.effects.ZoneEntryOptions
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * CR 306.5b — "A planeswalker has the intrinsic ability 'This permanent enters with a number of
 * loyalty counters on it equal to its printed loyalty number.' This ability creates a replacement
 * effect (see rule 614.1c)."
 *
 * Because it is a replacement effect on *entering*, it applies however the planeswalker gets onto
 * the battlefield — not only when a planeswalker spell resolves. A planeswalker that entered with
 * no loyalty counters would immediately be put into its owner's graveyard by state-based actions
 * (CR 704.5i, "If a planeswalker has loyalty 0, it's put into its owner's graveyard").
 *
 * The engine places these counters in two spots, matching the split that already exists for Saga
 * lore counters: the cast pipeline does it in `StackResolver` (it adds permanents to the
 * battlefield zone directly), and every other entry gets it from
 * `ZoneMovementUtils.applyPlaneswalkerEntryIfNeeded` via `ZoneTransitionService.moveToZone`. Both
 * end up in the same `EntersWithReplacements.placeEntryCounters` call, so counter-placement
 * modifiers apply identically either way. These tests pin down both, the face-down exclusion, and
 * that parity. Token copies are covered by [TokenCopyOfPlaneswalkerEntryScenarioTest].
 */
class PlaneswalkerEntryLoyaltyScenarioTest : ScenarioTestBase() {

    private fun loyaltyOf(entityId: EntityId, state: com.wingedsheep.engine.state.GameState): Int =
        state.getEntity(entityId)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

    /** A creature that flips onto a planeswalker back face — the "returns transformed" entry. */
    private val squire: CardDefinition = CardDefinition.doubleFacedPermanent(
        frontFace = card("Test Squire") {
            manaCost = "{W}"
            colorIdentity = "W"
            typeLine = "Creature — Human Soldier"
            power = 1
            toughness = 1
            oracleText = ""
        },
        backFace = card("Test Squire Ascended") {
            manaCost = ""
            colorIndicator = "W"
            typeLine = "Legendary Planeswalker — Squire"
            startingLoyalty = 3
            oracleText = "+1: You gain 1 life."
            loyaltyAbility(+1) { effect = Effects.GainLife(1) }
        },
    )

    /** "Exile target creature, then return it to the battlefield transformed." */
    private val ascension = card("Test Ascension") {
        manaCost = "{W}"
        colorIdentity = "W"
        typeLine = "Instant"
        oracleText = "Exile target creature, then return it to the battlefield transformed."
        spell {
            target = Targets.Creature
            effect = Effects.ExileAndReturnTransformed(EffectTarget.ContextTarget(0))
        }
    }

    init {
        cardRegistry.register(squire)
        cardRegistry.register(ascension)

        context("CR 306.5b — a planeswalker enters with its printed loyalty however it enters") {

            test("a planeswalker returned by an 'exile until this leaves' aura comes back with its printed loyalty") {
                // The reported repro: Sheltered by Ghosts (DSK) exiles the opponent's planeswalker,
                // then hands it back when the Aura is destroyed. The returning object is a new
                // permanent entering the battlefield, so CR 306.5b applies to it — it must not
                // arrive on 0 loyalty and die to CR 704.5i on the spot.
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Sheltered by Ghosts")
                    .withCardInHand(1, "Disenchant")
                    // {1}{W} for the Aura, {1}{W} for the Disenchant.
                    .withLandsOnBattlefield(1, "Plains", 4)
                    // Something for the Aura to enchant (it enchants a creature you control).
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    // The opponent's planeswalker the ETB will exile.
                    .withCardOnBattlefield(2, "Ajani, Outland Chaperone")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val ajani = game.findPermanent("Ajani, Outland Chaperone")!!

                val cast = game.castSpell(1, "Sheltered by Ghosts", bears)
                withClue("casting the Aura should succeed: ${cast.error}") { cast.error shouldBe null }

                // Resolve the Aura; its enters trigger asks for the nonland permanent to exile.
                var guard = 0
                while (game.state.pendingDecision !is ChooseTargetsDecision && guard < 20) {
                    game.resolveStack(); guard++
                }
                val targetDecision = game.state.pendingDecision as? ChooseTargetsDecision
                    ?: error("expected a ChooseTargetsDecision for the exile trigger; got ${game.state.pendingDecision}")
                game.submitDecision(TargetsResponse(targetDecision.id, mapOf(0 to listOf(ajani))))
                game.resolveStack()

                withClue("the planeswalker is exiled by the Aura's enters trigger") {
                    game.isOnBattlefield("Ajani, Outland Chaperone") shouldBe false
                    game.isInGraveyard(2, "Ajani, Outland Chaperone") shouldBe false
                }

                // Destroy the Aura — its leaves trigger returns the exiled card to the battlefield.
                val disenchant = game.castSpell(1, "Disenchant", game.findPermanent("Sheltered by Ghosts")!!)
                withClue("casting Disenchant should succeed: ${disenchant.error}") {
                    disenchant.error shouldBe null
                }
                game.resolveStack()

                withClue("the planeswalker is back on the battlefield, not in the graveyard") {
                    game.isOnBattlefield("Ajani, Outland Chaperone") shouldBe true
                    game.isInGraveyard(2, "Ajani, Outland Chaperone") shouldBe false
                }
                val returned = game.findPermanent("Ajani, Outland Chaperone")
                    ?: error("the returned planeswalker is not on the battlefield")

                withClue("it entered with its printed loyalty (CR 306.5b), so SBAs leave it alone") {
                    loyaltyOf(returned, game.state) shouldBe 3
                }
                withClue("it returns under its owner's control") {
                    game.state.getZone(game.player2Id, Zone.BATTLEFIELD).contains(returned) shouldBe true
                }
            }

            test("a planeswalker reanimated from a graveyard enters with its printed loyalty") {
                // A second, unrelated entry path: Perennation's PutOntoBattlefield (a targeted
                // graveyard → battlefield move) rather than the linked-exile return pipeline.
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Perennation")
                    .withCardInGraveyard(1, "Ajani, Outland Chaperone")
                    // {3}{W}{B}{G}
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpellTargetingGraveyardCard(
                    1, "Perennation", 1, "Ajani, Outland Chaperone"
                )
                withClue("casting Perennation should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                val ajani = game.findPermanent("Ajani, Outland Chaperone")
                    ?: error("the reanimated planeswalker is not on the battlefield")

                withClue("it entered with its printed loyalty rather than 0") {
                    loyaltyOf(ajani, game.state) shouldBe 3
                }
                withClue("Perennation's own keyword counters land on the same object") {
                    val counters = game.state.getEntity(ajani)?.get<CountersComponent>()
                    counters?.getCount(CounterType.HEXPROOF) shouldBe 1
                    counters?.getCount(CounterType.INDESTRUCTIBLE) shouldBe 1
                }
            }

            test("casting a planeswalker still places its printed loyalty exactly once") {
                // Guards the cast pipeline against double-applying: it owns its own copy of the
                // CR 306.5b step because it adds permanents to the battlefield zone directly
                // instead of going through ZoneTransitionService.moveToZone.
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Ajani, Outland Chaperone")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Ajani, Outland Chaperone")
                withClue("casting the planeswalker should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()

                val ajani = game.findPermanent("Ajani, Outland Chaperone")
                    ?: error("the cast planeswalker is not on the battlefield")
                withClue("exactly the printed loyalty — not doubled by a second entry pass") {
                    loyaltyOf(ajani, game.state) shouldBe 3
                }
            }

            test("a planeswalker put onto the battlefield face down gets no loyalty counters") {
                // A face-down permanent is a nameless 2/2 creature with no card types beyond
                // creature (CR 708.2), so there is no printed loyalty number to place. Driven
                // through ZoneTransitionService directly: manifesting a planeswalker is legal but
                // needs a whole manifest-dread pipeline to reach through a card.
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInGraveyard(1, "Ajani, Outland Chaperone")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val ajani = game.state.getGraveyard(game.player1Id).first { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Ajani, Outland Chaperone"
                }

                val faceUp = ZoneTransitionService.moveToZone(
                    game.state, ajani, Zone.BATTLEFIELD,
                    ZoneEntryOptions(controllerId = game.player1Id)
                )
                withClue("sanity: the same move face up does place the loyalty counters") {
                    loyaltyOf(ajani, faceUp.state) shouldBe 3
                }

                val faceDown = ZoneTransitionService.moveToZone(
                    game.state, ajani, Zone.BATTLEFIELD,
                    ZoneEntryOptions(
                        controllerId = game.player1Id,
                        faceDown = true,
                        faceDownMode = FaceDownMode.MANIFEST
                    )
                )
                withClue("a face-down entry has no printed loyalty to place") {
                    loyaltyOf(ajani, faceDown.state) shouldBe 0
                }
            }

            test("a non-planeswalker entering the battlefield gets no loyalty counters") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.state.getGraveyard(game.player1Id).first { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Grizzly Bears"
                }

                val result = ZoneTransitionService.moveToZone(
                    game.state, bears, Zone.BATTLEFIELD,
                    ZoneEntryOptions(controllerId = game.player1Id)
                )
                loyaltyOf(bears, result.state) shouldBe 0
            }

            test("a double-faced card returning transformed enters with its back face's loyalty") {
                // The back face's printed loyalty, not the front face's (a creature has none):
                // returnDfcFace flips the CardComponent before handing the entity to moveToZone,
                // so the entry step reads the planeswalker face's definition.
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Test Squire")
                    .withCardInHand(1, "Test Ascension")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val squirePermanent = game.findPermanent("Test Squire")!!
                val cast = game.castSpell(1, "Test Ascension", squirePermanent)
                withClue("casting the transform instant should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()

                val ascended = game.findPermanent("Test Squire Ascended")
                    ?: error("the transformed planeswalker is not on the battlefield")
                withClue("it entered on its planeswalker face with that face's printed loyalty") {
                    loyaltyOf(ascended, game.state) shouldBe 3
                }
            }
        }

        context("CR 306.5b entry counters honour counter-placement modifiers (CR 614)") {

            // Doubling Season: "If an effect would put one or more counters on a permanent you
            // control, it puts twice that many of those counters on that permanent instead." The
            // entry loyalty counters are placed by a replacement effect like any other, so they
            // double — the printed ruling, and the reason both entry paths route through
            // EntersWithReplacements.placeEntryCounters instead of writing counters directly.

            test("Doubling Season doubles the entry loyalty of a cast planeswalker") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Ajani, Outland Chaperone")
                    .withCardOnBattlefield(1, "Doubling Season")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Ajani, Outland Chaperone")
                withClue("casting the planeswalker should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()

                val ajani = game.findPermanent("Ajani, Outland Chaperone")!!
                loyaltyOf(ajani, game.state) shouldBe 6
            }

            test("Doubling Season doubles the entry loyalty of a reanimated planeswalker") {
                // The non-cast path has to reach the same modifiers, or the two pipelines disagree
                // about the same card.
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Perennation")
                    .withCardInGraveyard(1, "Ajani, Outland Chaperone")
                    .withCardOnBattlefield(1, "Doubling Season")
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpellTargetingGraveyardCard(
                    1, "Perennation", 1, "Ajani, Outland Chaperone"
                )
                withClue("casting Perennation should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                val ajani = game.findPermanent("Ajani, Outland Chaperone")!!
                loyaltyOf(ajani, game.state) shouldBe 6
            }
        }
    }
}
