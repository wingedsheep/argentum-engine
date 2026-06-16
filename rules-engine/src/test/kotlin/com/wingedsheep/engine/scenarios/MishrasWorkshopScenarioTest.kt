package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Integration scenario test for Mishra's Workshop (ATQ #81).
 *
 * Land: "{T}: Add {C}{C}{C}. Spend this mana only to cast artifact spells."
 *
 * The pure ManaPool restriction logic is unit-tested in ManaSpendRestrictionArtifactTest; this
 * test exercises the integration: activating the mana ability adds three restricted colorless
 * mana to the pool, and that restricted mana actually pays for an artifact spell (Ashnod's Altar,
 * {3}) which then resolves onto the battlefield.
 */
class MishrasWorkshopScenarioTest : ScenarioTestBase() {

    init {
        context("Mishra's Workshop") {

            test("activating adds three artifact-restricted colorless mana that casts an artifact spell") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Mishra's Workshop")
                    .withCardInHand(1, "Ashnod's Altar") // {3} artifact
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val workshopId = game.findPermanent("Mishra's Workshop")!!
                val abilityId = cardRegistry.getCard("Mishra's Workshop")!!
                    .script.activatedAbilities[0].id

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = workshopId,
                        abilityId = abilityId
                    )
                )
                withClue("Activating Mishra's Workshop should succeed: ${result.error}") {
                    result.error shouldBe null
                }

                val pool = game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()!!
                withClue("Three restricted colorless mana entries should be in the pool") {
                    pool.restrictedMana.size shouldBe 3
                }
                withClue("Each restricted entry is colorless with an artifact-spell-only restriction") {
                    pool.restrictedMana.all { it.color == null } shouldBe true
                    pool.restrictedMana.all {
                        it.restriction == ManaRestriction.CardTypeSpellsOrAbilitiesOnly(
                            cardType = CardType.ARTIFACT,
                            allowSpells = true,
                            allowAbilities = false
                        )
                    } shouldBe true
                }
                withClue("Unrestricted colorless should still be 0 — all three carry the restriction") {
                    pool.colorless shouldBe 0
                }

                // Cast the {3} artifact paying strictly from the (restricted) pool mana.
                val altarId = game.state.getHand(game.player1Id).first { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Ashnod's Altar"
                }
                val castResult = game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = altarId,
                        paymentStrategy = PaymentStrategy.FromPool
                    )
                )
                withClue("Restricted artifact mana should pay for the artifact spell: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                withClue("The artifact spell should resolve onto the battlefield") {
                    game.isOnBattlefield("Ashnod's Altar") shouldBe true
                }
                withClue("The three restricted mana were consumed paying the {3} cost") {
                    val poolAfter = game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()!!
                    poolAfter.restrictedMana.size shouldBe 0
                }
            }
        }
    }
}
