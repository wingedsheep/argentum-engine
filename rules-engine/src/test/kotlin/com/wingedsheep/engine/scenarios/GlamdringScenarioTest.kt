package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Glamdring — {2} Legendary Artifact — Equipment:
 *   "Equipped creature has first strike and gets +1/+0 for each instant and sorcery card in your
 *    graveyard.
 *    Whenever equipped creature deals combat damage to a player, you may cast an instant or sorcery
 *    spell from your hand with mana value less than or equal to that damage without paying its mana
 *    cost.
 *    Equip {3}"
 *
 * Covers (a) the static grant: first strike + a dynamic +X/+0 that scales with the count of instant
 * and sorcery cards in your graveyard, and (b) the combat-damage free cast: dealing N combat damage
 * to a player lets you free-cast a hand instant/sorcery with MV ≤ N (and a higher-MV one is not
 * offered).
 */
class GlamdringScenarioTest : ScenarioTestBase() {

    private val equipAbilityId by lazy {
        cardRegistry.requireCard("Glamdring").activatedAbilities[0].id
    }

    init {
        // A cheap sorcery (MV 1) that draws a card — within a small combat-damage cap.
        cardRegistry.register(
            CardDefinition.sorcery(
                name = "Cheap Draw",
                manaCost = ManaCost.parse("{U}"),
                oracleText = "Draw a card.",
                script = CardScript(spellEffect = Effects.DrawCards(1))
            )
        )
        // An expensive sorcery (MV 5) — above a small combat-damage cap, must NOT be free-castable.
        cardRegistry.register(
            CardDefinition.sorcery(
                name = "Expensive Draw",
                manaCost = ManaCost.parse("{4}{U}"),
                oracleText = "Draw a card.",
                script = CardScript(spellEffect = Effects.DrawCards(1))
            )
        )

        test("equipped creature has first strike and gets +1/+0 per instant/sorcery in your graveyard") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Grizzly Bears")  // 2/2 base
                .withCardOnBattlefield(1, "Glamdring")
                .withLandsOnBattlefield(1, "Plains", 3)     // pay Equip {3}
                .withCardInGraveyard(1, "Cheap Draw")       // instant/sorcery #1
                .withCardInGraveyard(1, "Expensive Draw")   // instant/sorcery #2
                .withCardInGraveyard(1, "Grizzly Bears")    // a creature — must NOT count
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Island")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            val sword = game.findPermanent("Glamdring")!!

            game.state.projectedState.hasKeyword(bears, Keyword.FIRST_STRIKE) shouldBe false

            // Equip Glamdring onto the Bears.
            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = sword,
                    abilityId = equipAbilityId,
                    targets = listOf(ChosenTarget.Permanent(bears))
                )
            ).error shouldBe null
            game.resolveStack()

            withClue("Equipped creature has first strike") {
                game.state.projectedState.hasKeyword(bears, Keyword.FIRST_STRIKE) shouldBe true
            }
            withClue("+1/+0 for each of the two instant/sorcery cards (the creature card does not count)") {
                game.state.projectedState.getPower(bears) shouldBe 4   // 2 base + 2
                game.state.projectedState.getToughness(bears) shouldBe 2 // unchanged
            }
        }

        test("dealing N combat damage lets you free-cast a hand instant/sorcery with MV <= N") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Hill Giant")     // 3/3 — deals 3 combat damage
                .withCardOnBattlefield(1, "Glamdring")
                .withLandsOnBattlefield(1, "Plains", 3)     // pay Equip {3}
                .withCardInHand(1, "Cheap Draw")            // MV 1 <= 3 — free-castable
                .withCardInLibrary(1, "Plains")             // something to draw, proving the cast resolved
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val giant = game.findPermanent("Hill Giant")!!
            val sword = game.findPermanent("Glamdring")!!

            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = sword,
                    abilityId = equipAbilityId,
                    targets = listOf(ChosenTarget.Permanent(giant))
                )
            ).error shouldBe null
            game.resolveStack()

            // Attack the opponent; no blockers; combat damage fires the trigger.
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Hill Giant" to 2))
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareNoBlockers()
            // The equipped creature has first strike (granted by Glamdring), so it deals its combat
            // damage in the FIRST STRIKE combat damage step — that is where the trigger fires.
            game.passUntilPhase(Phase.COMBAT, Step.FIRST_STRIKE_COMBAT_DAMAGE)
            game.passPriority()
            game.resolveStack()

            withClue("Opponent took 3 combat damage") {
                game.getLifeTotal(2) shouldBe 17
            }

            // The free cast is optional — accept it, then resolve the targetless sorcery.
            game.answerYesNo(true)
            game.resolveStack()

            withClue("Cheap Draw (MV 1 <= 3) was free-cast and resolved (moved to graveyard)") {
                game.isInHand(1, "Cheap Draw") shouldBe false
                game.isInGraveyard(1, "Cheap Draw") shouldBe true
            }
        }

        test("a too-expensive instant/sorcery is NOT offered for the free cast") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Hill Giant")     // 3/3 — deals 3 combat damage
                .withCardOnBattlefield(1, "Glamdring")
                .withLandsOnBattlefield(1, "Plains", 3)     // pay Equip {3}
                .withCardInHand(1, "Expensive Draw")        // MV 5 > 3 — must not be offered
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val giant = game.findPermanent("Hill Giant")!!
            val sword = game.findPermanent("Glamdring")!!

            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = sword,
                    abilityId = equipAbilityId,
                    targets = listOf(ChosenTarget.Permanent(giant))
                )
            ).error shouldBe null
            game.resolveStack()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Hill Giant" to 2))
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareNoBlockers()
            // First strike: combat damage (and the Glamdring trigger) happen in the first strike step.
            game.passUntilPhase(Phase.COMBAT, Step.FIRST_STRIKE_COMBAT_DAMAGE)
            game.passPriority()
            game.resolveStack()

            withClue("No free-cast decision was offered (MV 5 > 3)") {
                game.state.pendingDecision shouldBe null
            }
            withClue("Expensive Draw stays in hand, uncast") {
                game.isInHand(1, "Expensive Draw") shouldBe true
            }
        }
    }
}
