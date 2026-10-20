package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.battlefield.CastChoicesComponent
import com.wingedsheep.engine.state.components.battlefield.ChoiceValue
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.chosenPlaneswalkerType
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.ChoiceSlot
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Deification — "As this enchantment enters, choose a planeswalker type. Planeswalkers you control
 * of the chosen type have hexproof. As long as you control a creature, if damage dealt to a
 * planeswalker you control of the chosen type would result in all loyalty counters on it being
 * removed, instead all but one of those counters are removed."
 */
class DeificationScenarioTest : ScenarioTestBase() {

    private fun TestGame.loyalty(name: String): Int =
        state.getEntity(findPermanent(name)!!)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

    /** Deification on player 1's battlefield; [choose] stamps its planeswalker type. */
    private fun ScenarioBuilder.withDeification(): ScenarioBuilder = withCardOnBattlefield(1, "Deification")

    private fun TestGame.choose(type: String) {
        val deification = findPermanent("Deification")!!
        state = state.updateEntity(deification) { c ->
            c.with(CastChoicesComponent(chosen = mapOf(ChoiceSlot.PLANESWALKER_TYPE to ChoiceValue.TextChoice(type))))
        }
    }

    init {
        test("casting it asks for a planeswalker type; only planeswalkers of that type get hexproof") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Deification")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withCardOnBattlefield(1, "Jace Beleren")
                .withCardOnBattlefield(1, "Liliana of the Veil")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Deification").error shouldBe null
            if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
            game.resolveStack()

            val choose = game.getPendingDecision() as ChooseOptionDecision
            withClue("the as-enters choice is a planeswalker-type prompt") {
                choose.prompt shouldBe "Choose a planeswalker type"
            }
            game.submitDecision(OptionChosenResponse(choose.id, choose.options.indexOf("Jace"))).error shouldBe null
            game.resolveStack()

            game.state.getEntity(game.findPermanent("Deification")!!)!!.chosenPlaneswalkerType() shouldBe "Jace"
            val projected = game.state.projectedState
            projected.hasKeyword(game.findPermanent("Jace Beleren")!!, Keyword.HEXPROOF) shouldBe true
            projected.hasKeyword(game.findPermanent("Liliana of the Veil")!!, Keyword.HEXPROOF) shouldBe false
        }

        test("put onto the battlefield without being cast, it still asks for a planeswalker type") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInGraveyard(1, "Deification")
                .withCardInHand(1, "Open the Vaults")
                .withLandsOnBattlefield(1, "Plains", 6)
                .withCardOnBattlefield(1, "Jace Beleren")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Open the Vaults").error shouldBe null
            if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
            game.resolveStack()

            val choose = game.getPendingDecision() as ChooseOptionDecision
            choose.prompt shouldBe "Choose a planeswalker type"
            game.submitDecision(OptionChosenResponse(choose.id, choose.options.indexOf("Jace"))).error shouldBe null
            game.resolveStack()

            game.state.getEntity(game.findPermanent("Deification")!!)!!.chosenPlaneswalkerType() shouldBe "Jace"
            game.state.projectedState.hasKeyword(game.findPermanent("Jace Beleren")!!, Keyword.HEXPROOF) shouldBe true
        }

        test("an opponent can't target a planeswalker of the chosen type, but can target another") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withDeification()
                .withCardOnBattlefield(1, "Jace Beleren")
                .withCardOnBattlefield(1, "Liliana of the Veil")
                .withCardInHand(2, "Lightning Bolt")
                .withLandsOnBattlefield(2, "Mountain", 1)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.choose("Jace")

            game.castSpell(2, "Lightning Bolt", game.findPermanent("Jace Beleren")).error shouldNotBe null
            game.castSpell(2, "Lightning Bolt", game.findPermanent("Liliana of the Veil")).error shouldBe null
        }

        test("with a creature, lethal damage to a chosen-type planeswalker leaves one loyalty counter") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withDeification()
                .withCardOnBattlefield(1, "Jace Beleren") // 3 loyalty
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.choose("Jace")

            game.castSpell(1, "Lightning Bolt", game.findPermanent("Jace Beleren")).error shouldBe null
            game.resolveStack()

            withClue("3 damage to a 3-loyalty Jace removes all but one counter") {
                game.isOnBattlefield("Jace Beleren") shouldBe true
                game.loyalty("Jace Beleren") shouldBe 1
            }
        }

        test("damage that leaves loyalty to spare is unchanged") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withDeification()
                .withCardOnBattlefield(1, "Chandra Nalaar") // 6 loyalty
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.choose("Chandra")

            game.castSpell(1, "Lightning Bolt", game.findPermanent("Chandra Nalaar")).error shouldBe null
            game.resolveStack()

            game.loyalty("Chandra Nalaar") shouldBe 3
        }

        test("without a creature, or for a planeswalker of another type, the damage is lethal") {
            val noCreature = scenario()
                .withPlayers("Player", "Opponent")
                .withDeification()
                .withCardOnBattlefield(1, "Jace Beleren")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            noCreature.choose("Jace")
            noCreature.castSpell(1, "Lightning Bolt", noCreature.findPermanent("Jace Beleren")).error shouldBe null
            noCreature.resolveStack()
            withClue("no creature: Deification's last ability doesn't apply") {
                noCreature.isInGraveyard(1, "Jace Beleren") shouldBe true
            }

            val otherType = scenario()
                .withPlayers("Player", "Opponent")
                .withDeification()
                .withCardOnBattlefield(1, "Liliana of the Veil") // 3 loyalty
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            otherType.choose("Jace")
            otherType.castSpell(1, "Lightning Bolt", otherType.findPermanent("Liliana of the Veil")).error shouldBe null
            otherType.resolveStack()
            withClue("Liliana isn't the chosen type") {
                otherType.isInGraveyard(1, "Liliana of the Veil") shouldBe true
            }
        }

        test("combat damage is still dealt in full — lifelink gains the whole amount, one counter stays") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withDeification()
                .withCardOnBattlefield(1, "Jace Beleren") // 3 loyalty
                .withCardOnBattlefield(1, "Grizzly Bears", tapped = true)
                .withCardOnBattlefield(2, "Baneslayer Angel", summoningSickness = false) // 5/5 lifelink
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.choose("Jace")

            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackersWithPermanentTargets(
                permanentAttackers = mapOf("Baneslayer Angel" to "Jace Beleren")
            ).error shouldBe null
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

            game.getLifeTotal(2) shouldBe 25
            game.loyalty("Jace Beleren") shouldBe 1
        }

        test("applies in the combat damage step that its controller's last creature dies") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withDeification()
                .withCardOnBattlefield(1, "Jace Beleren") // 3 loyalty
                .withCardOnBattlefield(1, "Llanowar Elves") // the only creature, about to trade
                .withCardOnBattlefield(2, "Hill Giant", summoningSickness = false)
                .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.choose("Jace")

            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackersWithPermanentTargets(
                playerAttackers = mapOf("Grizzly Bears" to 1),
                permanentAttackers = mapOf("Hill Giant" to "Jace Beleren")
            ).error shouldBe null
            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Llanowar Elves" to listOf("Grizzly Bears"))).error shouldBe null
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

            game.isInGraveyard(1, "Llanowar Elves") shouldBe true
            game.loyalty("Jace Beleren") shouldBe 1
        }
    }
}
