package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.dsk.cards.SayItsName
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Scenario coverage for the DSK spells batch, all composed from existing SDK primitives:
 *
 *  - Duskmourn's Domination ({4}{U}{U} Aura) — control the enchanted creature, it gets -3/-0 and
 *    loses all abilities. (ControlEnchantedPermanent + LoseAllAbilities + ModifyStats.)
 *  - Peer Past the Veil ({2}{R}{G} Instant) — discard your hand, then draw X = card types among
 *    cards in your graveyard. (Patterns.Hand.discardHand + DrawCards over AggregateZone DISTINCT_TYPES.)
 *  - Say Its Name ({1}{G} Sorcery) — mill three, then may return a creature/land card from your
 *    graveyard; plus a graveyard-activated "exile three Say Its Names → search for Altanak" ability.
 *  - Waltz of Rage ({3}{R}{R} Sorcery) — target creature you control deals damage equal to its
 *    power to each other creature, plus a turn-long delayed impulse-draw on each of your creatures' deaths.
 */
class DskSpellsBatchScenarioTest : ScenarioTestBase() {

    init {
        context("Duskmourn's Domination — control + -3/-0 + loses all abilities") {

            test("attaching the Aura steals control, applies -3/-0, and removes flying/vigilance") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(2, "Serra Angel", summoningSickness = false)
                    .withCardAttachedTo(1, "Duskmourn's Domination", "Serra Angel")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val angel = game.findPermanent("Serra Angel")!!

                withClue("Player 1 now controls the enchanted Serra Angel") {
                    game.state.projectedState.getController(angel) shouldBe game.player1Id
                }
                withClue("Serra Angel (4/4) gets -3/-0 → 1/4") {
                    game.state.projectedState.getPower(angel) shouldBe 1
                    game.state.projectedState.getToughness(angel) shouldBe 4
                }
                withClue("Serra Angel loses all abilities, including flying and vigilance") {
                    game.state.projectedState.hasKeyword(angel, Keyword.FLYING) shouldBe false
                    game.state.projectedState.hasKeyword(angel, Keyword.VIGILANCE) shouldBe false
                }
            }
        }

        context("Peer Past the Veil — discard hand, draw X = card types in graveyard") {

            test("discards the whole hand then draws one card per distinct card type in the graveyard") {
                // Graveyard seeded with three distinct card types: creature, instant, land.
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Peer Past the Veil")
                    .withCardsInHand(1, "Grizzly Bears", 2)
                    .withCardInGraveyard(1, "Grizzly Bears") // creature
                    .withCardInGraveyard(1, "Lightning Bolt") // instant
                    .withCardInGraveyard(1, "Forest") // land
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(1, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                withClue("Graveyard starts with 3 cards (creature, instant, land)") {
                    game.state.getGraveyard(game.player1Id!!).size shouldBe 3
                }

                game.castSpell(1, "Peer Past the Veil").error shouldBe null
                game.resolveStack()

                // After resolution the 2 Grizzly Bears are discarded (creature already present) and
                // Peer Past the Veil itself reaches the graveyard as an instant (already present). So the
                // card types among graveyard cards = {creature, instant, land} = 3, and 3 cards are drawn.
                withClue("Hand was discarded then refilled with 3 drawn cards (X = 3 card types)") {
                    game.state.getHand(game.player1Id!!).size shouldBe 3
                }
                withClue("Discarded Grizzly Bears are now in the graveyard") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                }
            }
        }

        context("Say Its Name — mill three, then may return a creature/land card") {

            test("mills three and returns a chosen creature card from the graveyard to hand") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Say Its Name")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Lightning Bolt")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Say Its Name").error shouldBe null
                game.resolveStack()

                // Mill 3 puts the top three library cards (Grizzly Bears, Lightning Bolt, Forest) into
                // the graveyard, then a Gather/Select(up to 1) over creature/land cards prompts.
                withClue("Say Its Name pauses for the optional creature/land return") {
                    game.hasPendingDecision() shouldBe true
                }
                val bearsInGy = game.findCardsInGraveyard(1, "Grizzly Bears")
                withClue("Milled Grizzly Bears is a legal return candidate") {
                    bearsInGy.isNotEmpty() shouldBe true
                }
                game.selectCards(listOf(bearsInGy.first())).error shouldBe null
                game.resolveStack()

                withClue("Grizzly Bears returned to hand") {
                    game.findCardsInHand(1, "Grizzly Bears").size shouldBe 1
                }
                withClue("Lightning Bolt (instant) stays milled in the graveyard") {
                    game.isInGraveyard(1, "Lightning Bolt") shouldBe true
                }
            }

            test("graveyard ability exiles three Say Its Names as its cost and searches for Altanak") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInGraveyard(1, "Say Its Name")
                    .withCardInGraveyard(1, "Say Its Name")
                    .withCardInGraveyard(1, "Say Its Name")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val copies = game.findCardsInGraveyard(1, "Say Its Name")
                withClue("Three Say Its Name copies seeded in the graveyard") {
                    copies.size shouldBe 3
                }
                val sourceId = copies.first()
                val abilityId = SayItsName.activatedAbilities.first().id

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id!!,
                        sourceId = sourceId,
                        abilityId = abilityId,
                        costPayment = AdditionalCostPayment(exiledCards = copies)
                    )
                )
                withClue("Activating the graveyard ability should succeed: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()
                // The multi-zone search may surface a (possibly empty) optional selection; skip it.
                if (game.hasPendingDecision()) {
                    game.skipSelection()
                    game.resolveStack()
                }

                // Altanak, the Thrice-Called isn't implemented, so the search finds nothing — but the
                // cost is still paid: all three Say Its Name copies are exiled from the graveyard.
                withClue("All three Say Its Name copies were exiled to pay the cost") {
                    game.findCardsInGraveyard(1, "Say Its Name").size shouldBe 0
                    game.state.getExile(game.player1Id!!).size shouldBeGreaterThanOrEqual 3
                }
            }
        }

        context("Waltz of Rage — chosen creature pings each other creature; deaths impulse-draw") {

            test("chosen creature deals power damage to each other creature and survives itself") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Waltz of Rage")
                    .withLandsOnBattlefield(1, "Mountain", 5)
                    .withCardOnBattlefield(1, "Hill Giant", summoningSickness = false) // 3/3
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false) // 2/2
                    .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false) // 2/2
                    .withCardInLibrary(1, "Lightning Bolt")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanents("Hill Giant").first()
                game.castSpell(1, "Waltz of Rage", giant).error shouldBe null
                game.resolveStack()

                // Hill Giant (power 3) deals 3 to each OTHER creature, so both 2/2 Grizzly Bears die,
                // while Hill Giant itself survives (it's excluded from "each other creature").
                withClue("Hill Giant survives (excluded from 'each other creature')") {
                    game.findPermanents("Hill Giant").size shouldBe 1
                }
                withClue("Both 2/2 Grizzly Bears took 3 damage and died") {
                    game.findAllPermanents("Grizzly Bears").size shouldBe 0
                }
                withClue("Player 1's creature death triggered the delayed impulse-draw to exile") {
                    game.state.getExile(game.player1Id!!).size shouldBeGreaterThanOrEqual 1
                }
            }
        }
    }
}
