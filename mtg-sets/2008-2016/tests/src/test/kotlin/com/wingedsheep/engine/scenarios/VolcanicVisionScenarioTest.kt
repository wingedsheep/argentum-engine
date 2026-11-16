package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Volcanic Vision (DTK #167) — {5}{R}{R} Sorcery.
 *
 * "Return target instant or sorcery card from your graveyard to your hand. Volcanic Vision deals
 *  damage equal to that card's mana value to each creature your opponents control. Exile Volcanic Vision."
 *
 * Pins that the damage reads the returned card's mana value after it has moved to hand, that only
 * the opponents' creatures are hit, that {X} counts as 0 (ruling 2015-02-25), and that the spell
 * exiles itself.
 */
class VolcanicVisionScenarioTest : ScenarioTestBase() {

    private fun TestGame.damageOn(id: EntityId): Int =
        state.getEntity(id)?.get<DamageComponent>()?.amount ?: 0

    private fun board(graveyardCard: String) = scenario()
        .withPlayers("Alice", "Bob")
        .withCardInHand(1, "Volcanic Vision")
        .withLandsOnBattlefield(1, "Mountain", 7)
        .withCardInGraveyard(1, graveyardCard)
        .withCardOnBattlefield(1, "Hill Giant")
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withCardOnBattlefield(2, "Craw Wurm")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("returns the card and deals its mana value to each creature opponents control, then exiles itself") {
            val game = board("Divination")

            game.castSpellTargetingGraveyardCard(1, "Volcanic Vision", 1, "Divination").error shouldBe null
            game.resolveStack()

            game.isInHand(1, "Divination") shouldBe true
            withClue("Divination has mana value 3: Grizzly Bears dies, Craw Wurm survives with 3 damage") {
                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                game.damageOn(game.findPermanent("Craw Wurm").shouldNotBeNull()) shouldBe 3
            }
            withClue("Alice's own creature is not dealt damage") {
                game.damageOn(game.findPermanent("Hill Giant").shouldNotBeNull()) shouldBe 0
            }
            game.getLifeTotal(2) shouldBe 20
            game.isInExile(1, "Volcanic Vision") shouldBe true
            game.isInGraveyard(1, "Volcanic Vision") shouldBe false
        }

        test("X in the returned card's mana cost counts as 0") {
            val game = board("Fireball")

            game.castSpellTargetingGraveyardCard(1, "Volcanic Vision", 1, "Fireball").error shouldBe null
            game.resolveStack()

            game.isInHand(1, "Fireball") shouldBe true
            withClue("Fireball ({X}{R}) has mana value 1 in hand") {
                game.damageOn(game.findPermanent("Grizzly Bears").shouldNotBeNull()) shouldBe 1
                game.damageOn(game.findPermanent("Craw Wurm").shouldNotBeNull()) shouldBe 1
            }
            game.isInExile(1, "Volcanic Vision") shouldBe true
        }
    }
}
