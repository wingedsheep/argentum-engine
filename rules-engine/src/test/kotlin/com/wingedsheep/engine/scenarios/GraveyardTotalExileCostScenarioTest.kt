package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.costs.CardMeasure
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * `CostAtom.ExileFromGraveyardForTotal` — "exile any number of `<filter>` cards from your graveyard
 * whose totalled `<measure>` is N or more" — as a primitive, independent of the one card that prints it.
 *
 * It is the unnamed, **filtered** generalization of collect evidence (CR 701.59a, the same shape
 * hard-wired to "any card" and "total mana value"), and both route through the one shared
 * `GraveyardTotalExileResolver`. What these tests pin is the two axes that are genuinely new plus the
 * invariants the shape turns on:
 *
 *  - **the floor is on the summed measure, never on the card count** — a fistful of zero-weight cards
 *    is not a payment, and overpaying is the payer's right;
 *  - **it fails closed** — a graveyard that can't reach the floor makes the ability *absent* from the
 *    menu, not offered and then refused, exactly as CR 701.59b requires of collect evidence;
 *  - **the filter narrows the pool** — non-matching graveyard cards are never offered and never help;
 *  - **a client-supplied selection that doesn't pay is rejected**, never silently replaced with the
 *    engine's own pick (every `GameAction` field is client-supplied);
 *  - **pip counting follows CR 107.4e/f** — hybrid and Phyrexian symbols are symbols of their colour.
 */
class GraveyardTotalExileCostScenarioTest : ScenarioTestBase() {

    // --- Graveyard fodder, priced in black pips ------------------------------------------------

    /** Three black pips. Black. */
    private val trio = card("Test Pip Trio") {
        manaCost = "{B}{B}{B}"
        typeLine = "Creature — Zombie"
        power = 1
        toughness = 1
    }

    /** Two black pips. Black. */
    private val duo = card("Test Pip Duo") {
        manaCost = "{B}{B}"
        typeLine = "Creature — Zombie"
        power = 1
        toughness = 1
    }

    /** Two hybrid black/green pips — black *and* green, and two black symbols (CR 107.4e). */
    private val hybrid = card("Test Pip Hybrid") {
        manaCost = "{B/G}{B/G}"
        typeLine = "Creature — Zombie"
        power = 1
        toughness = 1
    }

    /** Two Phyrexian black pips — still black mana symbols (CR 107.4f). */
    private val phyrexian = card("Test Pip Phyrexian") {
        manaCost = "{B/P}{B/P}"
        typeLine = "Creature — Zombie"
        power = 1
        toughness = 1
    }

    /**
     * Red, no black pip. Under the *unfiltered* measure it is a legal selection worth **0** — the
     * card that proves "enough cards" never implies "enough total". Under a black-card filter it is
     * not even offered.
     */
    private val ember = card("Test Pip Ember") {
        manaCost = "{2}{R}"
        typeLine = "Creature — Elemental"
        power = 1
        toughness = 1
    }

    /** Colorless, no pips at all: generic mana counts for nothing. */
    private val rock = card("Test Pip Rock") {
        manaCost = "{4}"
        typeLine = "Artifact"
    }

    /** An artifact creature: two card types on one card. */
    private val golem = card("Test Type Golem") {
        manaCost = "{3}"
        typeLine = "Artifact Creature — Golem"
        power = 2
        toughness = 2
    }

    /** A land: a third card type, and no mana cost at all. */
    private val waste = card("Test Type Waste") {
        typeLine = "Land"
    }

    // --- The two costs under test --------------------------------------------------------------

    /** The bare atom: black-pip measure, **no** filter, so every graveyard card is selectable. */
    private val vault = card("Test Pip Vault") {
        manaCost = "{2}"
        typeLine = "Artifact"
        activatedAbility {
            cost = Costs.ExileFromGraveyardForTotal(
                minTotal = 4,
                measure = CardMeasure.ColoredManaSymbols(listOf(Color.BLACK)),
            )
            effect = Effects.GainLife(3)
        }
    }

    /** The colour-filtered facade Baron Helmut Zemo uses: black cards, counted in black pips. */
    private val blackVault = card("Test Black Vault") {
        manaCost = "{2}"
        typeLine = "Artifact"
        activatedAbility {
            cost = Costs.ExileFromGraveyardForColoredSymbols(4, Color.BLACK)
            effect = Effects.GainLife(3)
        }
    }

    /** The union measure: "with three or more card types among them". */
    private val typeVault = card("Test Type Vault") {
        manaCost = "{2}"
        typeLine = "Artifact"
        activatedAbility {
            cost = Costs.ExileFromGraveyardForTotal(minTotal = 3, measure = CardMeasure.DistinctCardTypes)
            effect = Effects.GainLife(3)
        }
    }

    /** The union measure as a *spell's* mandatory additional cost, cast from hand. */
    private val typeRite = card("Test Type Rite") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        additionalCost(Costs.additional.ExileOtherCardsWithCardTypes(3))
        spell {
            effect = Effects.GainLife(3)
        }
    }

    // --- Harness -------------------------------------------------------------------------------

    private fun abilityIdOf(name: String) =
        cardRegistry.getCard(name)!!.script.activatedAbilities[0].id

    private fun TestGame.activate(name: String, exiled: List<EntityId> = emptyList()): ExecutionResult =
        execute(
            ActivateAbility(
                playerId = player1Id,
                sourceId = findPermanent(name)!!,
                abilityId = abilityIdOf(name),
                costPayment = AdditionalCostPayment(exiledCards = exiled),
            )
        )

    private fun TestGame.actionFor(name: String) =
        getLegalActions(1).firstOrNull {
            it.actionType == "ActivateAbility" &&
                (it.action as? ActivateAbility)?.sourceId == findPermanent(name)
        }

    private fun TestGame.nameOf(id: EntityId): String? =
        state.getEntity(id)?.get<CardComponent>()?.name

    private fun TestGame.exileIds(): List<EntityId> =
        state.getZone(ZoneKey(player1Id, Zone.EXILE))

    private fun TestGame.exileNames(): List<String> = exileIds().mapNotNull { nameOf(it) }

    private fun TestGame.graveyardNames(): List<String> =
        state.getZone(ZoneKey(player1Id, Zone.GRAVEYARD)).mapNotNull { nameOf(it) }

    private fun TestGame.graveyardCard(name: String): EntityId =
        findCardsInGraveyard(1, name).first()

    /** [sourceName] on the battlefield, [graveyard] in the controller's graveyard, main phase. */
    private fun setUp(sourceName: String, graveyard: List<String>): TestGame {
        var builder = scenario()
            .withPlayers("You", "Them")
            .withCardOnBattlefield(1, sourceName)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        graveyard.forEach { builder = builder.withCardInGraveyard(1, it) }
        return builder.build()
    }

    init {
        listOf(trio, duo, hybrid, phyrexian, ember, rock, vault, blackVault, golem, waste, typeVault, typeRite)
            .forEach { cardRegistry.register(it) }

        // -----------------------------------------------------------------------------------
        // Fails closed: an unreachable floor means no action at all
        // -----------------------------------------------------------------------------------

        test("a graveyard that can't reach the floor makes the ability absent, not unpayable") {
            val game = setUp("Test Pip Vault", listOf("Test Pip Ember", "Test Pip Rock", "Test Pip Duo"))

            withClue("2 black pips across three cards is short of 4 — the option is not offered") {
                game.actionFor("Test Pip Vault") shouldBe null
            }
            withClue("and a hand-built activation is refused rather than half-paid") {
                game.activate("Test Pip Vault").error shouldBe "Cannot pay ability cost"
            }
            withClue("nothing was exiled on the way to that refusal") {
                game.exileIds().isEmpty() shouldBe true
                game.graveyardNames().size shouldBe 3
            }
        }

        test("card count is never the question — four cards with no black pips still can't pay") {
            val game = setUp(
                "Test Pip Vault",
                listOf("Test Pip Rock", "Test Pip Rock", "Test Pip Rock", "Test Pip Ember"),
            )
            game.actionFor("Test Pip Vault") shouldBe null
        }

        // -----------------------------------------------------------------------------------
        // The offered payload: a sum gate the client can't compute for itself
        // -----------------------------------------------------------------------------------

        test("the cost payload carries the floor and a per-card weight for every offered card") {
            val game = setUp(
                "Test Pip Vault",
                listOf("Test Pip Trio", "Test Pip Duo", "Test Pip Ember"),
            )
            val info = game.actionFor("Test Pip Vault")!!.additionalCostInfo!!

            info.costType shouldBe "ExileForTotal"
            info.exileMinTotalWeight shouldBe 4
            withClue("unfiltered: every graveyard card is selectable, including the 0-weight one") {
                info.validExileTargets.mapNotNull { game.nameOf(it) }.toSet() shouldBe
                    setOf("Test Pip Trio", "Test Pip Duo", "Test Pip Ember")
            }
            withClue("the weights are black-pip counts, so the client only sums numbers it was given") {
                info.exileCardWeights[game.graveyardCard("Test Pip Trio")] shouldBe 3
                info.exileCardWeights[game.graveyardCard("Test Pip Duo")] shouldBe 2
                info.exileCardWeights[game.graveyardCard("Test Pip Ember")] shouldBe 0
            }
            withClue("the count bounds carry none of the real constraint: one to all of them") {
                info.exileMinCount shouldBe 1
                info.exileMaxCount shouldBe 3
            }
        }

        // -----------------------------------------------------------------------------------
        // Paying it
        // -----------------------------------------------------------------------------------

        test("a legal selection is honoured exactly, overpay and zero-weight passengers included") {
            val game = setUp(
                "Test Pip Vault",
                listOf("Test Pip Trio", "Test Pip Duo", "Test Pip Ember", "Test Pip Rock"),
            )
            val chosen = listOf(
                game.graveyardCard("Test Pip Trio"),
                game.graveyardCard("Test Pip Duo"),
                game.graveyardCard("Test Pip Ember"),
            )
            val life = game.getLifeTotal(1)

            game.activate("Test Pip Vault", chosen).error shouldBe null

            withClue("the payer's own three cards are exiled — 5 pips for a floor of 4 is their right") {
                game.exileIds().toSet() shouldBe chosen.toSet()
            }
            withClue("the card they didn't pick stays in the graveyard") {
                game.graveyardNames() shouldBe listOf("Test Pip Rock")
            }
            game.resolveStack()
            game.getLifeTotal(1) shouldBe life + 3
        }

        test("a submitted selection that doesn't reach the floor is rejected, not substituted") {
            val game = setUp("Test Pip Vault", listOf("Test Pip Trio", "Test Pip Duo"))
            val short = listOf(game.graveyardCard("Test Pip Duo"))

            withClue("2 of 4: refused outright") {
                (game.activate("Test Pip Vault", short).error ?: "")
                    .startsWith("Those cards don't pay this cost") shouldBe true
            }
            withClue("silently exiling the engine's own pick instead would exile cards nobody chose") {
                game.exileIds().isEmpty() shouldBe true
                game.graveyardNames().size shouldBe 2
            }
        }

        test("no selection at all takes the heaviest cards first, exiling as few as possible") {
            val game = setUp(
                "Test Pip Vault",
                listOf("Test Pip Trio", "Test Pip Duo", "Test Pip Ember", "Test Pip Rock"),
            )

            game.activate("Test Pip Vault").error shouldBe null

            withClue("3 + 2 clears the floor of 4, so the two 0-weight cards are never touched") {
                game.exileNames().toSet() shouldBe setOf("Test Pip Trio", "Test Pip Duo")
                game.graveyardNames().toSet() shouldBe setOf("Test Pip Ember", "Test Pip Rock")
            }
        }

        // -----------------------------------------------------------------------------------
        // Pip counting (CR 107.4e/f) — the same rule the per-card filter and amount use
        // -----------------------------------------------------------------------------------

        test("hybrid and Phyrexian symbols count as symbols of their colour") {
            val game = setUp("Test Pip Vault", listOf("Test Pip Hybrid", "Test Pip Phyrexian"))
            val info = game.actionFor("Test Pip Vault")!!.additionalCostInfo!!

            info.exileCardWeights[game.graveyardCard("Test Pip Hybrid")] shouldBe 2
            info.exileCardWeights[game.graveyardCard("Test Pip Phyrexian")] shouldBe 2
            withClue("2 + 2 reaches 4 on symbols no plain {B} ever printed") {
                game.activate("Test Pip Vault").error shouldBe null
                game.exileNames().toSet() shouldBe setOf("Test Pip Hybrid", "Test Pip Phyrexian")
            }
        }

        // -----------------------------------------------------------------------------------
        // The filter axis — what separates this from collect evidence
        // -----------------------------------------------------------------------------------

        test("the colour filter keeps non-matching cards out of the pool entirely") {
            val game = setUp(
                "Test Black Vault",
                listOf("Test Pip Trio", "Test Pip Duo", "Test Pip Ember", "Test Pip Rock"),
            )
            val info = game.actionFor("Test Black Vault")!!.additionalCostInfo!!

            withClue("only the black cards are offered — colour is a characteristic, pips are not") {
                info.validExileTargets.mapNotNull { game.nameOf(it) }.toSet() shouldBe
                    setOf("Test Pip Trio", "Test Pip Duo")
            }

            val smuggled = listOf(
                game.graveyardCard("Test Pip Trio"),
                game.graveyardCard("Test Pip Ember"),
            )
            withClue("a card outside the pool can't be submitted to top up a 3-pip payment") {
                (game.activate("Test Black Vault", smuggled).error ?: "")
                    .startsWith("Those cards don't pay this cost") shouldBe true
                game.exileIds().isEmpty() shouldBe true
            }
        }

        test("a filtered pool that can't reach the floor is unpayable even with cards to spare") {
            val game = setUp(
                "Test Black Vault",
                listOf("Test Pip Ember", "Test Pip Rock", "Test Pip Rock", "Test Pip Duo"),
            )
            withClue("the two black pips in the pool are all that count; the rest are invisible") {
                game.actionFor("Test Black Vault") shouldBe null
            }
        }

        // -----------------------------------------------------------------------------------
        // The union measure: card types *among* the chosen cards, not summed per card
        // -----------------------------------------------------------------------------------

        test("card types are a union — artifact creatures and creatures never show a third type") {
            val game = setUp(
                "Test Type Vault",
                listOf("Test Type Golem", "Test Pip Rock", "Test Pip Trio", "Test Pip Duo", "Test Type Golem"),
            )
            withClue("five cards, two per golem, but only artifact and creature among them") {
                game.actionFor("Test Type Vault") shouldBe null
            }
        }

        test("the union payload ships each card's types, and a selection is priced by their union") {
            val game = setUp(
                "Test Type Vault",
                listOf("Test Type Golem", "Test Pip Rock", "Test Pip Trio", "Test Type Waste"),
            )
            val info = game.actionFor("Test Type Vault")!!.additionalCostInfo!!
            info.exileMinTotalWeight shouldBe 3
            info.exileWeightUnit shouldBe "card types"
            info.exileCardTypes[game.graveyardCard("Test Type Golem")] shouldBe listOf("ARTIFACT", "CREATURE")
            info.exileCardTypes[game.graveyardCard("Test Type Waste")] shouldBe listOf("LAND")

            val twoTypesInThreeCards = listOf(
                game.graveyardCard("Test Type Golem"),
                game.graveyardCard("Test Pip Rock"),
                game.graveyardCard("Test Pip Trio"),
            )
            withClue("three cards whose weights would sum to 4 still show only two types") {
                (game.activate("Test Type Vault", twoTypesInThreeCards).error ?: "")
                    .startsWith("Those cards don't pay this cost") shouldBe true
                game.exileIds().isEmpty() shouldBe true
            }
        }

        test("as a spell's additional cost, the cast carries the union picker and checks the selection") {
            var builder = scenario()
                .withPlayers("You", "Them")
                .withCardInHand(1, "Test Type Rite")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            listOf("Test Type Golem", "Test Pip Trio", "Test Type Waste").forEach { builder = builder.withCardInGraveyard(1, it) }
            val game = builder.build()
            val rite = game.findCardsInHand(1, "Test Type Rite").first()
            val cast = game.getLegalActions(1).first { (it.action as? CastSpell)?.cardId == rite }
            val info = cast.additionalCostInfo!!
            info.costType shouldBe "ExileForTotal"
            info.exileMinTotalWeight shouldBe 3
            info.exileCardTypes[game.graveyardCard("Test Type Waste")] shouldBe listOf("LAND")

            fun castWith(exiled: List<EntityId>) = game.execute(
                CastSpell(game.player1Id, rite, additionalCostPayment = AdditionalCostPayment(exiledCards = exiled))
            )
            withClue("an artifact creature and a creature show two types, short of three") {
                castWith(listOf(game.graveyardCard("Test Type Golem"), game.graveyardCard("Test Pip Trio")))
                    .error.shouldNotBeNull()
                game.exileIds().isEmpty() shouldBe true
            }
            castWith(listOf(game.graveyardCard("Test Type Golem"), game.graveyardCard("Test Type Waste")))
                .error shouldBe null
            game.exileNames().toSet() shouldBe setOf("Test Type Golem", "Test Type Waste")
        }

        test("the engine's own pick covers the types with the fewest cards") {
            val game = setUp(
                "Test Type Vault",
                listOf("Test Pip Rock", "Test Pip Trio", "Test Type Golem", "Test Type Waste"),
            )
            game.activate("Test Type Vault").error shouldBe null
            withClue("the artifact creature shows two types at once, the land the third") {
                game.exileNames().toSet() shouldBe setOf("Test Type Golem", "Test Type Waste")
            }
        }
    }
}
