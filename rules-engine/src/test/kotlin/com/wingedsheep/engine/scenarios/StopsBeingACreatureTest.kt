package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.combat.BlockedComponent
import com.wingedsheep.engine.state.components.combat.BlockingComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * A permanent that stops being a creature, exercised through Nylea, God of the Hunt — a Theros god
 * that "isn't a creature" while its controller's devotion to green is below five.
 *
 * The rules pinned here:
 *  - CR 700.5: devotion counts the {G} symbols among the mana costs of permanents you control,
 *    the god's own included, and the type removal follows it in both directions.
 *  - CR 205.1a: when the creature card type is removed, the creature subtypes go with it — a god
 *    below its threshold is a legendary enchantment without the creature type God (ruling).
 *  - CR 506.4: an attacking or blocking creature that stops being a creature is removed from
 *    combat, deals and receives no combat damage, and doesn't rejoin combat if it becomes a
 *    creature again. A creature it was blocking stays blocked (CR 509.1h).
 */
class StopsBeingACreatureTest : ScenarioTestBase() {

    /** Four green pips: with Nylea's own {G}, devotion to green is exactly five. */
    private val devotee = card("Test Green Devotee") {
        manaCost = "{G}{G}{G}{G}"
        typeLine = "Creature — Elf"
        power = 1
        toughness = 1
    }

    /** A vanilla attacker with no green pips. */
    private val raider = card("Test Colorless Raider") {
        manaCost = "{3}"
        typeLine = "Artifact Creature — Construct"
        power = 3
        toughness = 3
    }

    private fun TestGame.nylea() = findPermanent("Nylea, God of the Hunt")!!

    init {
        cardRegistry.register(devotee)
        cardRegistry.register(raider)

        test("below five devotion Nylea is a legendary enchantment without the creature type God") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Nylea, God of the Hunt")
                .withCardOnBattlefield(1, "Test Colorless Raider")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val projected = game.state.projectedState
            projected.isCreature(game.nylea()) shouldBe false
            projected.hasSubtype(game.nylea(), "God") shouldBe false
            projected.hasType(game.nylea(), "ENCHANTMENT") shouldBe true
            projected.isLegendary(game.nylea()) shouldBe true
            // Her other abilities work while she isn't a creature.
            projected.hasKeyword(game.findPermanent("Test Colorless Raider")!!, Keyword.TRAMPLE) shouldBe true
            projected.hasKeyword(game.nylea(), Keyword.INDESTRUCTIBLE) shouldBe true
        }

        test("at five devotion Nylea is a 6/6 God creature, and falls back when devotion drops") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Nylea, God of the Hunt")
                .withCardOnBattlefield(1, "Test Green Devotee")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val projected = game.state.projectedState
            projected.isCreature(game.nylea()) shouldBe true
            projected.hasSubtype(game.nylea(), "God") shouldBe true
            projected.getPower(game.nylea()) shouldBe 6
            projected.getToughness(game.nylea()) shouldBe 6
            // "Other creatures": she doesn't grant herself trample.
            projected.hasKeyword(game.nylea(), Keyword.TRAMPLE) shouldBe false

            game.castSpell(1, "Lightning Bolt", game.findPermanent("Test Green Devotee")!!).error shouldBe null
            game.resolveStack()

            game.state.projectedState.isCreature(game.nylea()) shouldBe false
            game.state.projectedState.hasSubtype(game.nylea(), "God") shouldBe false
        }

        test("an attacking Nylea whose devotion drops is removed from combat and deals no damage") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Nylea, God of the Hunt")
                .withCardOnBattlefield(1, "Test Green Devotee")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Nylea, God of the Hunt" to 2)).error shouldBe null
            game.state.getEntity(game.nylea())!!.get<AttackingComponent>() shouldNotBe null

            game.castSpell(1, "Lightning Bolt", game.findPermanent("Test Green Devotee")!!).error shouldBe null
            game.resolveStack()

            game.state.getEntity(game.nylea())!!.get<AttackingComponent>() shouldBe null

            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
            game.getLifeTotal(2) shouldBe 20
        }

        test("a blocking Nylea that stops being a creature leaves combat; the attacker stays blocked") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Test Colorless Raider")
                .withCardOnBattlefield(2, "Nylea, God of the Hunt")
                .withCardOnBattlefield(2, "Test Green Devotee")
                .withCardInHand(2, "Lightning Bolt")
                .withLandsOnBattlefield(2, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Test Colorless Raider" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Nylea, God of the Hunt" to listOf("Test Colorless Raider"))).error shouldBe null
            game.state.getEntity(game.nylea())!!.get<BlockingComponent>() shouldNotBe null

            // Once the active player has passed, the defender bolts their own devotee.
            if (game.state.priorityPlayerId != game.player2Id) game.passPriority()
            game.state.priorityPlayerId shouldBe game.player2Id
            game.castSpell(2, "Lightning Bolt", game.findPermanent("Test Green Devotee")!!).error shouldBe null
            game.resolveStack()

            game.state.getEntity(game.nylea())!!.get<BlockingComponent>() shouldBe null
            val raiderId = game.findPermanent("Test Colorless Raider")!!
            game.state.getEntity(raiderId)!!.get<BlockedComponent>() shouldNotBe null

            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
            // Blocked with no blockers left and no trample: no damage to the defender.
            game.getLifeTotal(2) shouldBe 20
        }
    }
}
