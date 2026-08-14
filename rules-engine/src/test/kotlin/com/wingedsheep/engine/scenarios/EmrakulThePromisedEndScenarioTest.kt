package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.mana.CostCalculator
import com.wingedsheep.engine.state.components.player.PlayerTurnHijackedComponent
import com.wingedsheep.engine.state.components.player.SkipNextTurnComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Emrakul, the Promised End (EMN #6) — {13} Legendary Creature — Eldrazi, 13/13.
 *
 * "This spell costs {1} less to cast for each card type among cards in your graveyard.
 *  When you cast this spell, you gain control of target opponent during that player's next turn.
 *  After that turn, that player takes an extra turn.
 *  Flying, trample, protection from instants"
 *
 * Three pieces of new engine behaviour to pin down:
 *  - `CostReductionSource.CardTypesInYourGraveyard` counts distinct card *types*, so the
 *    many-cards-one-type case is the one that separates it from
 *    `CardsInGraveyardMatchingFilter` — a card-counting implementation passes the mixed graveyard
 *    test and fails that one.
 *  - A *printed* `ProtectionScope.CardType` now reaches the projection as
 *    `PROTECTION_FROM_CARDTYPE_INSTANT`. Before this change `CardEntityFactory` dropped the scope
 *    on the floor, so the keyword never existed and instants could target Emrakul freely.
 *  - The cast trigger has to feed the *same* targeted opponent to both halves (hijack + extra
 *    turn); `TakeExtraTurn` defaults to the controller, so a dropped target silently hands the
 *    caster the extra turn instead.
 */
class EmrakulThePromisedEndScenarioTest : ScenarioTestBase() {

    init {
        context("Emrakul, the Promised End — cost reduction per card type in your graveyard") {

            test("empty graveyard → full {13}") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Emrakul, the Promised End")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cost = CostCalculator(cardRegistry).calculateEffectiveCost(
                    game.state,
                    cardRegistry.requireCard("Emrakul, the Promised End"),
                    game.player1Id,
                )

                withClue("nothing in the graveyard, so the generic component stays at 13") {
                    cost.genericAmount shouldBe 13
                }
            }

            test("six card types in the graveyard → {7}") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Emrakul, the Promised End")
                    .withCardInGraveyard(1, "Grizzly Bears")   // creature
                    .withCardInGraveyard(1, "Mountain")        // land
                    .withCardInGraveyard(1, "Lightning Bolt")  // instant
                    .withCardInGraveyard(1, "Wrath of God")    // sorcery
                    .withCardInGraveyard(1, "Pacifism")        // enchantment
                    .withCardInGraveyard(1, "Sol Ring")        // artifact
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cost = CostCalculator(cardRegistry).calculateEffectiveCost(
                    game.state,
                    cardRegistry.requireCard("Emrakul, the Promised End"),
                    game.player1Id,
                )

                withClue("six distinct card types shave {6} off the {13}") {
                    cost.genericAmount shouldBe 7
                }
            }

            test("many cards of one type still only reduce by {1}") {
                // The case that distinguishes counting *types* from counting *cards*: three creature
                // cards are one card type, so the discount is {1} and not {3}.
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Emrakul, the Promised End")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInGraveyard(1, "Hill Giant")
                    .withCardInGraveyard(1, "Savannah Lions")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cost = CostCalculator(cardRegistry).calculateEffectiveCost(
                    game.state,
                    cardRegistry.requireCard("Emrakul, the Promised End"),
                    game.player1Id,
                )

                withClue("three creature cards are a single card type → {12}") {
                    cost.genericAmount shouldBe 12
                }
            }

            test("only your own graveyard counts") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Emrakul, the Promised End")
                    .withCardInGraveyard(1, "Grizzly Bears")   // creature — ours
                    .withCardInGraveyard(2, "Mountain")        // land — the opponent's
                    .withCardInGraveyard(2, "Lightning Bolt")  // instant — the opponent's
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cost = CostCalculator(cardRegistry).calculateEffectiveCost(
                    game.state,
                    cardRegistry.requireCard("Emrakul, the Promised End"),
                    game.player1Id,
                )

                withClue("\"your graveyard\" excludes the opponent's two extra types → {12}") {
                    cost.genericAmount shouldBe 12
                }
            }
        }

        context("Emrakul, the Promised End — protection from instants") {

            test("an instant spell can't target Emrakul on the battlefield") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Emrakul, the Promised End")
                    .withCardInHand(2, "Lightning Bolt")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val emrakul = game.findPermanent("Emrakul, the Promised End")!!

                withClue("the printed protection scope reaches the projection") {
                    game.state.projectedState
                        .hasKeyword(emrakul, "PROTECTION_FROM_CARDTYPE_INSTANT") shouldBe true
                }

                val bolt = game.castSpell(2, "Lightning Bolt", emrakul)

                withClue("Lightning Bolt is an instant, so targeting Emrakul is illegal") {
                    bolt.error shouldNotBe null
                }
            }

            test("a noninstant spell may still target Emrakul") {
                // Protection is from instants only — a sorcery-source removal spell is unaffected.
                // Pinning this down guards against the keyword being read as "protection from
                // everything" at the enforcement sites.
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Emrakul, the Promised End")
                    .withCardInHand(2, "Pacifism")
                    .withLandsOnBattlefield(2, "Plains", 2) // Pacifism is {1}{W}
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val emrakul = game.findPermanent("Emrakul, the Promised End")!!
                val aura = game.castSpell(2, "Pacifism", emrakul)

                withClue("Pacifism is an enchantment, not an instant: ${aura.error}") {
                    aura.error shouldBe null
                }
            }
        }

        context("Emrakul, the Promised End — cast trigger") {

            test("hijacks the targeted opponent's next turn and hands them an extra turn after it") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Emrakul, the Promised End")
                    .withLandsOnBattlefield(1, "Mountain", 13)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpellTargetingPlayer(1, "Emrakul, the Promised End", 2)
                withClue("Emrakul is castable off 13 lands: ${cast.error}") { cast.error shouldBe null }

                // The cast trigger goes on the stack above Emrakul and resolves first (CR 603.2).
                game.resolveStack()

                val hijack = game.state.getEntity(game.player2Id)?.get<PlayerTurnHijackedComponent>()
                withClue("Player2's next turn is scheduled to be controlled by Player1") {
                    hijack shouldNotBe null
                    hijack!!.controllerId shouldBe game.player1Id
                    hijack.state shouldBe PlayerTurnHijackedComponent.HijackState.SCHEDULED
                }

                withClue("the extra turn goes to the targeted opponent, so the *caster* skips a turn") {
                    game.state.getEntity(game.player1Id)?.get<SkipNextTurnComponent>()?.turns shouldBe 1
                    game.state.getEntity(game.player2Id)?.get<SkipNextTurnComponent>() shouldBe null
                }
            }
        }
    }
}
