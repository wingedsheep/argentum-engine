package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.pz2.cards.GoblinGoliath
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Goblin Goliath (PZ2).
 *
 * "When this creature enters, create a number of 1/1 red Goblin creature tokens equal to the number
 * of opponents you have.
 * {3}{R}, {T}: If a source you control would deal damage to an opponent this turn, it deals double
 * that damage to that player instead."
 *
 * The doubling is scoped to damage dealt to an opponent *player*: a Bolt to the opponent's face deals
 * 6, but a Bolt to the opponent's creature still deals 3, and other sources you control (a Goblin
 * token attacking) double too.
 */
class GoblinGoliathScenarioTest : ScenarioTestBase() {

    private val doubleAbilityId = GoblinGoliath.activatedAbilities.first().id

    init {
        test("entering creates one Goblin token per opponent") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Goblin Goliath")
                .withLandsOnBattlefield(1, "Mountain", 6)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Goblin Goliath").error shouldBe null
            game.resolveStack()

            game.findPermanent("Goblin Goliath").shouldNotBeNull()
            game.findPermanents("Goblin Token").size shouldBe 1
        }

        test("doubles damage your sources deal to an opponent, but not to the opponent's creatures") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardOnBattlefield(1, "Goblin Goliath")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Lightning Bolt")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 6)
                .withCardOnBattlefield(2, "Craw Wurm")
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val goliath = game.findPermanent("Goblin Goliath").shouldNotBeNull()
            game.execute(
                ActivateAbility(playerId = game.player1Id, sourceId = goliath, abilityId = doubleAbilityId)
            ).error shouldBe null
            game.resolveStack()

            game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
            game.resolveStack()
            withClue("Bolt's 3 damage to the opponent is doubled to 6") {
                game.getLifeTotal(2) shouldBe 14
            }

            val wurm = game.findPermanent("Craw Wurm").shouldNotBeNull()
            game.castSpell(1, "Lightning Bolt", wurm).error shouldBe null
            game.resolveStack()
            withClue("damage to a creature the opponent controls is not doubled — the 6/4 Wurm survives with 3") {
                game.findPermanent("Craw Wurm").shouldNotBeNull()
                game.state.getEntity(wurm)?.get<DamageComponent>()?.amount shouldBe 3
            }

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareNoBlockers().error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)
            withClue("another source you control doubles too: the Bears' 2 combat damage becomes 4") {
                game.getLifeTotal(2) shouldBe 10
            }
        }
    }
}
