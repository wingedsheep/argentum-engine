package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Urgent Necropsy (MKM) — {2}{B}{G} instant.
 *
 * "As an additional cost to cast this spell, collect evidence X, where X is the total mana value of
 * the permanents this spell targets. Destroy up to one target artifact, up to one target creature,
 * up to one target enchantment, and up to one target planeswalker."
 *
 * The only collect-evidence cost in print whose threshold is *derived*. What these tests pin down
 * is the pricing rule and its consequences, from the card's rulings and CR 601.2:
 *
 *  - X is the summed mana value of the targets the caster **actually chose** (601.2c), determined
 *    before the cost is paid (601.2f → 601.2h);
 *  - a payment that doesn't reach X is refused outright — per the printed ruling a caster who
 *    can't exile that much "can't choose to collect evidence at all", making such a cast illegal
 *    (601.2e) rather than cheaper;
 *  - overpaying is legal, because the threshold is a floor on total mana value (CR 701.59a);
 *  - and with no targets chosen X is 0, which collects evidence 0 and exiles nothing.
 *
 * The board is priced so every number in the assertions is checkable by hand:
 * Bonesplitter {1} = 1, Grizzly Bears {1}{G} = 2, Mass Hysteria {R} = 1, Liliana of the Veil
 * {1}{B}{B} = 3 — 7 in total. The graveyard holds Air Elemental (5), Hill Giant (4) and a second
 * Grizzly Bears (2), so 5 + 2 pays exactly 7 and 4 + 2 falls one short.
 */
class UrgentNecropsyScenarioTest : ScenarioTestBase() {

    private fun board() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Urgent Necropsy")
        .withLandsOnBattlefield(1, "Swamp", 2)
        .withLandsOnBattlefield(1, "Forest", 2)
        // The four target types, all under the opponent.
        .withCardOnBattlefield(2, "Bonesplitter")
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withCardOnBattlefield(2, "Mass Hysteria")
        .withCardOnBattlefield(2, "Liliana of the Veil")
        // Evidence to spend: 5 + 4 + 2.
        .withCardInGraveyard(1, "Air Elemental")
        .withCardInGraveyard(1, "Hill Giant")
        .withCardInGraveyard(1, "Grizzly Bears")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

    init {
        test("X is the summed mana value of every chosen target, and all four are destroyed") {
            val game = board().build()

            val cast = game.castNecropsy(
                targets = game.allFourTargets(),
                evidence = listOf("Air Elemental", "Grizzly Bears"), // 5 + 2 = exactly 7
            )
            withClue("exiling exactly the total mana value of the targets pays the cost: ${cast.error}") {
                cast.error shouldBe null
            }
            game.resolveStack()

            withClue("every targeted permanent is destroyed") {
                game.findPermanent("Bonesplitter") shouldBe null
                game.findPermanent("Mass Hysteria") shouldBe null
                game.findPermanent("Liliana of the Veil") shouldBe null
                game.state.getBattlefield(game.player2Id).none {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Grizzly Bears"
                } shouldBe true
            }
            withClue("the two chosen evidence cards left the graveyard for exile") {
                game.graveyardNames(1).sorted() shouldBe listOf("Hill Giant", "Urgent Necropsy")
                game.exileNames(1).contains("Air Elemental") shouldBe true
                game.exileNames(1).contains("Grizzly Bears") shouldBe true
            }
        }

        test("a payment short of the targets' total mana value is refused") {
            val game = board().build()

            val cast = game.castNecropsy(
                targets = game.allFourTargets(),
                evidence = listOf("Hill Giant", "Grizzly Bears"), // 4 + 2 = 6, one short of 7
            )
            withClue("CR 601.2e — the cast is illegal, not discounted") {
                cast.error shouldNotBe null
            }
            withClue("nothing was exiled and nothing was destroyed") {
                game.graveyardNames(1).sorted() shouldBe
                    listOf("Air Elemental", "Grizzly Bears", "Hill Giant")
                game.findPermanent("Liliana of the Veil") shouldNotBe null
            }
        }

        test("only the chosen targets are counted, not every legal one") {
            val game = board().build()

            // Just the artifact — mana value 1 — even though 7 worth of targets is on the board.
            val cast = game.castNecropsy(
                targets = listOf(ChosenTarget.Permanent(game.oppPermanent("Bonesplitter"))),
                evidence = listOf("Grizzly Bears"), // mana value 2, which clears a threshold of 1
            )
            withClue("a cheap target makes a cheap cost: ${cast.error}") { cast.error shouldBe null }
            game.resolveStack()

            withClue("the targeted artifact dies") { game.findPermanent("Bonesplitter") shouldBe null }
            withClue("the untargeted permanents are untouched") {
                game.findPermanent("Mass Hysteria") shouldNotBe null
                game.findPermanent("Liliana of the Veil") shouldNotBe null
            }
            withClue("overpaying is legal — the whole 2-mana-value card is exiled for a cost of 1") {
                game.exileNames(1) shouldBe listOf("Grizzly Bears")
            }
        }

        test("with no targets chosen the cost is collect evidence 0 and nothing is exiled") {
            val game = board().build()

            val cast = game.castNecropsy(targets = emptyList(), evidence = emptyList())
            withClue("every target is 'up to one', so casting with none is legal: ${cast.error}") {
                cast.error shouldBe null
            }
            game.resolveStack()

            withClue("collecting evidence 0 exiles nothing") {
                game.exileNames(1) shouldBe emptyList()
                game.graveyardNames(1).sorted() shouldBe
                    listOf("Air Elemental", "Grizzly Bears", "Hill Giant", "Urgent Necropsy")
            }
            withClue("nothing was destroyed") {
                game.findPermanent("Bonesplitter") shouldNotBe null
                game.findPermanent("Liliana of the Veil") shouldNotBe null
            }
        }

        test("the legal action defers its evidence picker and ships a price per target") {
            val game = board().build()

            val cast = game.getLegalActions(1)
                .first { it.description.contains("Urgent Necropsy") }
            val cost = cast.additionalCostInfo
            withClue("the mandatory collect-evidence cost reaches the client at all") {
                cost shouldNotBe null
                cost!!.costType shouldBe "CollectEvidence"
            }
            withClue("the threshold is not knowable yet — it starts at 0") {
                cost!!.exileMinTotalWeight shouldBe 0
            }
            withClue("every legal target is priced so the client can total up its own selection") {
                val weights = cost!!.exileWeightPerTarget
                weights[game.oppPermanent("Bonesplitter")] shouldBe 1
                weights[game.oppPermanent("Grizzly Bears")] shouldBe 2
                weights[game.oppPermanent("Mass Hysteria")] shouldBe 1
                weights[game.oppPermanent("Liliana of the Veil")] shouldBe 3
            }
            withClue("the whole graveyard is spendable, priced by mana value") {
                cost!!.exileCardWeights.values.sorted() shouldBe listOf(2, 4, 5)
                cost.exileWeightUnit shouldBe "mana value"
            }
        }
    }

    // -- helpers ------------------------------------------------------------------------------

    private fun TestGame.oppPermanent(name: String): EntityId =
        state.getBattlefield(player2Id).first {
            state.getEntity(it)?.get<CardComponent>()?.name == name
        }

    /** The four requirements in printed order: artifact, creature, enchantment, planeswalker. */
    private fun TestGame.allFourTargets(): List<ChosenTarget> = listOf(
        ChosenTarget.Permanent(oppPermanent("Bonesplitter")),
        ChosenTarget.Permanent(oppPermanent("Grizzly Bears")),
        ChosenTarget.Permanent(oppPermanent("Mass Hysteria")),
        ChosenTarget.Permanent(oppPermanent("Liliana of the Veil")),
    )

    private fun TestGame.castNecropsy(
        targets: List<ChosenTarget>,
        evidence: List<String>,
    ) = execute(
        CastSpell(
            playerId = player1Id,
            cardId = state.getHand(player1Id).first {
                state.getEntity(it)?.get<CardComponent>()?.name == "Urgent Necropsy"
            },
            targets = targets,
            additionalCostPayment = AdditionalCostPayment(exiledCards = evidence.map { name ->
                state.getZone(ZoneKey(player1Id, Zone.GRAVEYARD)).first {
                    state.getEntity(it)?.get<CardComponent>()?.name == name
                }
            }),
        )
    )

    private fun TestGame.zoneNames(playerNumber: Int, zone: Zone): List<String> =
        state.getZone(ZoneKey(if (playerNumber == 1) player1Id else player2Id, zone))
            .mapNotNull { state.getEntity(it)?.get<CardComponent>()?.name }

    private fun TestGame.graveyardNames(playerNumber: Int) = zoneNames(playerNumber, Zone.GRAVEYARD)

    private fun TestGame.exileNames(playerNumber: Int) = zoneNames(playerNumber, Zone.EXILE)
}
