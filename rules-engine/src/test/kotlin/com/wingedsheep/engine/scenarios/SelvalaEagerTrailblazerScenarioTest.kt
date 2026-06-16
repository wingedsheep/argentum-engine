package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseColorDecision
import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.SelvalaEagerTrailblazer
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Selvala, Eager Trailblazer — {2}{G}{W} Legendary Creature — Elf Scout 4/5, vigilance.
 *
 * - "{T}: Choose a color. Add one mana of that color for each different power among creatures you
 *   control." → exercises [com.wingedsheep.sdk.scripting.values.Aggregation.DISTINCT_VALUES] over
 *   power, feeding [com.wingedsheep.sdk.dsl.Effects.AddManaOfChoice].
 * - "Whenever you cast a creature spell, create a 1/1 red Mercenary creature token …"
 */
class SelvalaEagerTrailblazerScenarioTest : FunSpec({

    val manaAbilityId = SelvalaEagerTrailblazer.activatedAbilities[0].id

    // Power-2 and power-3 vanilla creatures to vary the distinct-power count.
    val Bear2 = card("Selvala Test Bear 2") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }
    val Ogre3 = card("Selvala Test Ogre 3") {
        manaCost = "{2}{R}"
        typeLine = "Creature — Ogre"
        power = 3
        toughness = 3
    }

    fun createDriver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        d.registerCard(SelvalaEagerTrailblazer)
        d.registerCard(Bear2)
        d.registerCard(Ogre3)
        return d
    }

    test("mana ability adds one chosen-color mana per different power among creatures you control") {
        val d = createDriver()
        d.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val p = d.activePlayer!!

        // Selvala (power 4) + Bear (2) + Ogre (3) → distinct powers {4,2,3} = 3.
        val selvala = d.putCreatureOnBattlefield(p, "Selvala, Eager Trailblazer")
        d.removeSummoningSickness(selvala)
        d.putCreatureOnBattlefield(p, "Selvala Test Bear 2")
        d.putCreatureOnBattlefield(p, "Selvala Test Ogre 3")

        val result = d.submit(ActivateAbility(playerId = p, sourceId = selvala, abilityId = manaAbilityId))
        withClue("error=${result.error} isPaused=${d.isPaused}") {
            result.isPaused shouldBe true
        }
        // Pauses to choose a color.
        d.pendingDecision.shouldBeInstanceOf<ChooseColorDecision>()
        val decision = d.pendingDecision as ChooseColorDecision
        d.submitDecision(p, ColorChosenResponse(decision.id, Color.GREEN))

        val pool = d.state.getEntity(p)?.get<ManaPoolComponent>()
        pool?.green shouldBe 3
    }

    test("two creatures sharing a power count once") {
        val d = createDriver()
        d.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val p = d.activePlayer!!

        // Selvala (4) + Bear (2) + Bear (2) → distinct powers {4,2} = 2.
        val selvala = d.putCreatureOnBattlefield(p, "Selvala, Eager Trailblazer")
        d.removeSummoningSickness(selvala)
        d.putCreatureOnBattlefield(p, "Selvala Test Bear 2")
        d.putCreatureOnBattlefield(p, "Selvala Test Bear 2")

        d.submit(ActivateAbility(playerId = p, sourceId = selvala, abilityId = manaAbilityId))
        val decision = d.pendingDecision as ChooseColorDecision
        d.submitDecision(p, ColorChosenResponse(decision.id, Color.WHITE))

        d.state.getEntity(p)?.get<ManaPoolComponent>()?.white shouldBe 2
    }

    test("casting a creature spell creates a 1/1 red Mercenary token") {
        val d = createDriver()
        d.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val p = d.activePlayer!!

        d.putCreatureOnBattlefield(p, "Selvala, Eager Trailblazer")

        val mercenariesBefore = countMercenaries(d, p)

        val bear = d.putCardInHand(p, "Selvala Test Bear 2")
        d.giveMana(p, Color.GREEN, 2)
        val result = d.castSpell(p, bear)
        result.isSuccess shouldBe true
        // Resolve the cast trigger (token creation) and the spell.
        d.bothPass()

        countMercenaries(d, p) shouldBe (mercenariesBefore + 1)
    }
})

private fun countMercenaries(d: GameTestDriver, playerId: com.wingedsheep.sdk.model.EntityId): Int =
    d.state.getBattlefield(playerId).count { id ->
        val card = d.state.getEntity(id)?.get<CardComponent>() ?: return@count false
        card.typeLine.subtypes.any { it.value == "Mercenary" }
    }
