package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.mom.cards.IngaAndEsika
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import io.kotest.matchers.shouldBe

/**
 * Inga and Esika — creatures you control have vigilance and "{T}: Add one mana of any color. Spend
 * this mana only to cast a creature spell." Whenever you cast a creature spell, if three or more
 * mana from creatures was spent to cast it, draw a card.
 */
class IngaAndEsikaScenarioTest : ScenarioTestBase() {

    private val grantedManaAbilityId = IngaAndEsika.staticAbilities
        .filterIsInstance<GrantActivatedAbility>().single().ability.id

    private fun TestGame.tapForMana(name: String) {
        for (id in findAllPermanents(name)) {
            execute(ActivateAbility(player1Id, id, grantedManaAbilityId, manaColorChoice = Color.GREEN))
                .error shouldBe null
        }
    }

    init {
        test("three mana from creatures spent on a creature spell draws a card") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Inga and Esika")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Llanowar Elves")
                .withCardInHand(1, "Hill Giant")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardInLibrary(1, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.tapForMana("Inga and Esika")
            game.tapForMana("Grizzly Bears")
            game.tapForMana("Llanowar Elves")
            val handBefore = game.handSize(1)

            game.castSpell(1, "Hill Giant").error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Hill Giant") shouldBe true
            game.handSize(1) shouldBe handBefore - 1 + 1
        }

        test("two mana from creatures is not enough") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Inga and Esika")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Hill Giant")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withCardInLibrary(1, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.tapForMana("Inga and Esika")
            game.tapForMana("Grizzly Bears")
            val handBefore = game.handSize(1)

            game.castSpell(1, "Hill Giant").error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Hill Giant") shouldBe true
            game.handSize(1) shouldBe handBefore - 1
        }

        test("creatures you control have vigilance") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Inga and Esika")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
            game.state.getEntity(bears)?.has<TappedComponent>() shouldBe false
        }
    }
}
