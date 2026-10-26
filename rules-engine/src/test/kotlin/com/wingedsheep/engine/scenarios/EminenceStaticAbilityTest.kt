package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.PhasedOutComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.CommanderComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.ZoneScopedStaticAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Static abilities that function from the command zone — *eminence* statics (CR 113.6b: an ability
 * that states which zones it functions in functions only from those zones). The Ur-Dragon's
 * "As long as The Ur-Dragon is in the command zone or on the battlefield, other Dragon spells you
 * cast cost {1} less to cast" is `staticAbility { activeZones = setOf(BATTLEFIELD, COMMAND) }`,
 * which builds the bare battlefield static plus a [ZoneScopedStaticAbility] over `{COMMAND}`.
 *
 * Pinned down here:
 *  - the cost reduction functions from the command zone and from the battlefield, once each and
 *    never both (a card is in one zone at a time);
 *  - it is inert in the zones it doesn't name — hand, graveyard, library;
 *  - "you" in the command zone is the card's owner, so an opponent's Dragon is not discounted;
 *  - the filter still applies (a non-Dragon is not discounted);
 *  - "other": the card doesn't discount its own cast from the command zone — it is on the stack
 *    as its cost is determined (CR 601.2a, 601.2f) — while another commander still is discounted;
 *  - the same zone readers serve layer statics (a keyword grant from the command zone) and cost
 *    modifiers from the graveyard, the other supported zone.
 */
class EminenceStaticAbilityTest : ScenarioTestBase() {

    private val eminenceDragon = card("Test Eminence Wyrm") {
        manaCost = "{4}{R}"
        typeLine = "Legendary Creature — Dragon"
        power = 4
        toughness = 4
        oracleText = "Eminence — As long as Test Eminence Wyrm is in the command zone or on the " +
            "battlefield, other Dragon spells you cast cost {1} less to cast."
        staticAbility {
            ability = ModifySpellCost(
                target = SpellCostTarget.YouCast(GameObjectFilter.Any.withSubtype("Dragon")),
                modification = CostModification.ReduceGeneric(1),
            )
            activeZones = setOf(Zone.BATTLEFIELD, Zone.COMMAND)
        }
    }

    private val whelp = card("Test Dragon Whelp") {
        manaCost = "{2}{R}"
        typeLine = "Creature — Dragon"
        power = 2
        toughness = 2
    }

    private val commandBanner = card("Test Command Banner") {
        manaCost = "{2}"
        typeLine = "Legendary Artifact"
        oracleText = "As long as Test Command Banner is in the command zone, creatures you control have flying."
        staticAbility {
            ability = GrantKeyword(Keyword.FLYING, GroupFilter(GameObjectFilter.Creature.youControl()))
            activeZones = setOf(Zone.COMMAND)
        }
    }

    private val graveCostSpirit = card("Test Grave Discounter") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Spirit"
        power = 1
        toughness = 1
        oracleText = "As long as Test Grave Discounter is in your graveyard, creature spells you cast cost {1} less to cast."
        staticAbility {
            ability = ModifySpellCost(
                target = SpellCostTarget.YouCast(GameObjectFilter.Creature),
                modification = CostModification.ReduceGeneric(1),
            )
            activeZones = setOf(Zone.GRAVEYARD)
        }
    }

    init {
        cardRegistry.register(eminenceDragon)
        cardRegistry.register(whelp)
        cardRegistry.register(commandBanner)
        cardRegistry.register(graveCostSpirit)
    }

    /** The mana cost the engine offers [playerNumber] for casting [name]. */
    private fun TestGame.offeredCost(playerNumber: Int, name: String): String? =
        getLegalActions(playerNumber)
            .firstOrNull { it.actionType == "CastSpell" && it.description == "Cast $name" }
            ?.manaCostString

    private fun TestGame.commandZoneCard(playerNumber: Int, name: String): EntityId {
        val playerId = if (playerNumber == 1) player1Id else player2Id
        return state.getZone(ZoneKey(playerId, Zone.COMMAND)).first {
            state.getEntity(it)?.get<CardComponent>()?.name == name
        }
    }

    /** Tag a command-zone card as its owner's commander, so it can be cast from there. */
    private fun TestGame.makeCommander(playerNumber: Int, name: String): EntityId {
        val playerId = if (playerNumber == 1) player1Id else player2Id
        val id = commandZoneCard(playerNumber, name)
        state = state.updateEntity(id) { it.with(CommanderComponent(ownerId = playerId)) }
        return id
    }

    private fun TestGame.tappedLands(): Int =
        state.getBattlefield().count { state.getEntity(it)?.has<TappedComponent>() == true }

    init {
        test("the DSL builds the bare battlefield static plus a command-zone wrapper") {
            val statics = eminenceDragon.script.staticAbilities
            statics.size shouldBe 2
            statics[0].shouldBeInstanceOf<ModifySpellCost>()
            statics[1].shouldBeInstanceOf<ZoneScopedStaticAbility>().activeZones shouldBe setOf(Zone.COMMAND)
        }

        test("from the command zone, the owner's Dragon spells cost {1} less") {
            val game = scenario()
                .withPlayers()
                .withCardInCommandZone(1, "Test Eminence Wyrm")
                .withCardInHand(1, "Test Dragon Whelp")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.offeredCost(1, "Test Dragon Whelp") shouldBe "{1}{R}"
            withClue("two Mountains pay the discounted cost") {
                game.castSpell(1, "Test Dragon Whelp").error shouldBe null
            }
            game.resolveStack()
            game.findPermanent("Test Dragon Whelp") shouldNotBe null
        }

        test("from the battlefield, the reduction applies exactly once") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Test Eminence Wyrm")
                .withCardInHand(1, "Test Dragon Whelp")
                .withLandsOnBattlefield(1, "Mountain", 3)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.offeredCost(1, "Test Dragon Whelp") shouldBe "{1}{R}"
            game.castSpell(1, "Test Dragon Whelp").error shouldBe null
            withClue("only two of the three Mountains were needed") { game.tappedLands() shouldBe 2 }
        }

        test("a phased-out source on the battlefield doesn't discount — it doesn't fall back to its owner") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Test Eminence Wyrm")
                .withCardInHand(1, "Test Dragon Whelp")
                .withLandsOnBattlefield(1, "Mountain", 3)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val wyrm = game.findPermanent("Test Eminence Wyrm")!!
            game.state = game.state.updateEntity(wyrm) { it.with(PhasedOutComponent(game.player1Id)) }

            game.offeredCost(1, "Test Dragon Whelp") shouldBe "{2}{R}"
        }

        test("a non-Dragon spell is not discounted") {
            val game = scenario()
                .withPlayers()
                .withCardInCommandZone(1, "Test Eminence Wyrm")
                .withCardInHand(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Forest", 2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.offeredCost(1, "Grizzly Bears") shouldBe "{1}{G}"
        }

        test("an opponent's Dragon spell is not discounted — in the command zone, \"you\" is the owner") {
            val game = scenario()
                .withPlayers()
                .withCardInCommandZone(1, "Test Eminence Wyrm")
                .withCardInHand(2, "Test Dragon Whelp")
                .withLandsOnBattlefield(2, "Mountain", 3)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.offeredCost(2, "Test Dragon Whelp") shouldBe "{2}{R}"
        }

        test("inert in the graveyard, the hand, and the library") {
            for (place in listOf("graveyard", "hand", "library")) {
                val builder = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Test Dragon Whelp")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                when (place) {
                    "graveyard" -> builder.withCardInGraveyard(1, "Test Eminence Wyrm")
                    "hand" -> builder.withCardInHand(1, "Test Eminence Wyrm")
                    else -> builder.withCardInLibrary(1, "Test Eminence Wyrm")
                }
                val game = builder.build()
                withClue("Test Eminence Wyrm in the $place") {
                    game.offeredCost(1, "Test Dragon Whelp") shouldBe "{2}{R}"
                }
            }
        }

        test("\"other\": casting the card from the command zone doesn't discount itself") {
            val game = scenario()
                .withPlayers()
                .withCardInCommandZone(1, "Test Eminence Wyrm")
                .withLandsOnBattlefield(1, "Mountain", 5)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val wyrm = game.makeCommander(1, "Test Eminence Wyrm")

            game.offeredCost(1, "Test Eminence Wyrm") shouldBe "{4}{R}"
            game.execute(CastSpell(game.player1Id, wyrm)).error shouldBe null
            withClue("the cast paid the full {4}{R}") { game.tappedLands() shouldBe 5 }
        }

        test("another Dragon commander cast from the command zone is still discounted") {
            val game = scenario()
                .withPlayers()
                .withCardInCommandZone(1, "Test Eminence Wyrm")
                .withCardInCommandZone(1, "Test Dragon Whelp")
                .withLandsOnBattlefield(1, "Mountain", 3)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val whelpId = game.makeCommander(1, "Test Dragon Whelp")

            game.offeredCost(1, "Test Dragon Whelp") shouldBe "{1}{R}"
            game.execute(CastSpell(game.player1Id, whelpId)).error shouldBe null
            game.tappedLands() shouldBe 2
        }

        test("a layer static functions from the command zone for its owner only") {
            val game = scenario()
                .withPlayers()
                .withCardInCommandZone(1, "Test Command Banner")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val projected = game.state.projectedState
            projected.hasKeyword(game.findPermanent("Grizzly Bears")!!, Keyword.FLYING) shouldBe true
            projected.hasKeyword(game.findPermanent("Hill Giant")!!, Keyword.FLYING) shouldBe false
        }

        test("a command-zone layer static is off on the battlefield") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Test Command Banner")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.state.projectedState.hasKeyword(game.findPermanent("Grizzly Bears")!!, Keyword.FLYING) shouldBe false
        }

        test("a cost modifier scoped to the graveyard functions there and not on the battlefield") {
            val fromGraveyard = scenario()
                .withPlayers()
                .withCardInGraveyard(1, "Test Grave Discounter")
                .withCardInHand(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Forest", 2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            fromGraveyard.offeredCost(1, "Grizzly Bears") shouldBe "{G}"

            val fromBattlefield = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Test Grave Discounter")
                .withCardInHand(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Forest", 2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            fromBattlefield.offeredCost(1, "Grizzly Bears") shouldBe "{1}{G}"
        }
    }
}
