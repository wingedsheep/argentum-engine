package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.bng.cards.PhenaxGodOfDeception
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Phenax, God of Deception (BNG #152) — "Creatures you control have '{T}: Target player mills X
 * cards, where X is this creature's toughness.'" X is the tapped creature's own toughness, read as
 * the ability resolves (last-known if it has left the battlefield), and Phenax itself isn't a
 * creature below seven devotion to blue and black, so it doesn't get the ability.
 */
class PhenaxGodOfDeceptionScenarioTest : ScenarioTestBase() {

    private val millAbilityId = PhenaxGodOfDeception.staticAbilities
        .filterIsInstance<GrantActivatedAbility>()
        .single()
        .ability.id

    private fun setUp() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Phenax, God of Deception")
        .withCardOnBattlefield(1, "Hill Giant")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardInHand(1, "Lightning Bolt")
        .withLandsOnBattlefield(1, "Mountain", 1)
        .apply { repeat(6) { withCardInLibrary(2, "Island") } }
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.activate(creature: String) = execute(
        ActivateAbility(
            playerId = player1Id,
            sourceId = findPermanent(creature)!!,
            abilityId = millAbilityId,
            targets = listOf(ChosenTarget.Player(player2Id))
        )
    )

    init {
        test("each creature mills the target player by its own toughness") {
            val game = setUp()

            game.activate("Hill Giant").error shouldBe null
            game.resolveStack()
            withClue("Hill Giant is 3/3") { game.graveyardSize(2) shouldBe 3 }

            game.activate("Grizzly Bears").error shouldBe null
            game.resolveStack()
            withClue("Grizzly Bears is 2/2") { game.graveyardSize(2) shouldBe 5 }
            game.librarySize(2) shouldBe 1
        }

        test("a creature that leaves before resolution uses its last-known toughness") {
            val game = setUp()

            game.activate("Hill Giant").error shouldBe null
            game.castSpell(1, "Lightning Bolt", game.findPermanent("Hill Giant")!!).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Hill Giant") shouldBe true
            game.graveyardSize(2) shouldBe 3
        }

        test("Phenax isn't a creature below seven devotion, so it doesn't gain the ability") {
            val game = setUp()
            val phenax = game.findPermanent("Phenax, God of Deception")!!

            game.state.projectedState.isCreature(phenax) shouldBe false
            game.activate("Phenax, God of Deception").error shouldNotBe null
            game.graveyardSize(2) shouldBe 0
        }
    }
}
