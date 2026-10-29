package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseColorDecision
import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dis.cards.UtopiaSprawl
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.basicLand
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Utopia Sprawl (DIS #99) — "Enchant Forest. As this Aura enters, choose a color. Whenever enchanted
 * Forest is tapped for mana, its controller adds an additional one mana of the chosen color."
 *
 * Two things ride on how the engine runs the script rather than on reading it: the color is chosen
 * as the Aura *enters* (not at cast) and the bonus reads that stored choice, and the Forest keeps
 * producing its own {G} — unlike Shimmerwilds Growth there is no recolour. The enchant restriction
 * is the first "Enchant Forest" in the corpus, so a Mountain must not be a legal target.
 */
class UtopiaSprawlScenarioTest : FunSpec({

    val testForest = basicLand("Forest") {}
    val forestManaAbility = testForest.activatedAbilities[0].id

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(UtopiaSprawl, testForest))
        d.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    test("enchanted Forest taps for its own green plus one mana of the color chosen as the Aura entered") {
        val d = driver()
        val me = d.activePlayer!!
        val forest = d.putPermanentOnBattlefield(me, "Forest")
        val sprawl = d.putCardInHand(me, "Utopia Sprawl")
        d.giveMana(me, Color.GREEN, 1)

        d.castSpell(me, sprawl, listOf(forest)).outcome shouldBe Outcome.Done
        d.bothPass()

        d.pendingDecision.shouldBeInstanceOf<ChooseColorDecision>()
        val decision = d.pendingDecision as ChooseColorDecision
        d.submitDecision(me, ColorChosenResponse(decision.id, Color.RED))

        val auraId = d.state.getBattlefield().first {
            d.state.getEntity(it)?.get<CardComponent>()?.name == "Utopia Sprawl"
        }
        d.state.getEntity(auraId)!!.get<AttachedToComponent>()!!.targetId shouldBe forest

        d.submit(ActivateAbility(playerId = me, sourceId = forest, abilityId = forestManaAbility))
            .outcome shouldBe Outcome.Done

        val pool = d.state.getEntity(me)!!.get<ManaPoolComponent>()!!
        pool.green shouldBe 1
        pool.red shouldBe 1
    }

    test("a Mountain is not a legal Aura target") {
        val d = driver()
        val me = d.activePlayer!!
        val mountain = d.putPermanentOnBattlefield(me, "Mountain")
        val sprawl = d.putCardInHand(me, "Utopia Sprawl")
        d.giveMana(me, Color.GREEN, 1)

        d.castSpell(me, sprawl, listOf(mountain)).outcome.shouldBeInstanceOf<Outcome.Rejected>()
    }
})
