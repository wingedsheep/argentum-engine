package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Kitsa, Otterball Elite.
 *
 * Kitsa, Otterball Elite: {1}{U}
 * Legendary Creature — Otter Wizard, 1/3
 * Vigilance, Prowess
 * {T}: Draw a card, then discard a card.
 * {2}, {T}: Copy target instant or sorcery spell you control. You may choose new targets
 * for the copy. Activate only if Kitsa's power is 3 or greater.
 */
class KitsaOtterballEliteScenarioTest : ScenarioTestBase() {

    init {
        context("Kitsa, Otterball Elite - copy spell power restriction") {
            test("cannot activate copy ability when power is less than 3") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Kitsa, Otterball Elite")
                    .withCardInHand(1, "Shock")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Shock targeting opponent — puts it on the stack
                // Prowess triggers once, making Kitsa 2/4
                game.castSpellTargetingPlayer(1, "Shock", 2)

                // Try to activate the copy ability — Kitsa has power 2 (base 1 + 1 prowess)
                val kitsaId = game.findPermanent("Kitsa, Otterball Elite")!!
                val cardDef = cardRegistry.getCard("Kitsa, Otterball Elite")!!
                val copyAbility = cardDef.script.activatedAbilities[1]

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = kitsaId,
                        abilityId = copyAbility.id
                    )
                )
                withClue("Copy ability should fail when power < 3") {
                    result.error shouldNotBe null
                }
            }
        }
    }
}
