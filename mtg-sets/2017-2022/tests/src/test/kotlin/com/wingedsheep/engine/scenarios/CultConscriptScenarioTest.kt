package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.player.CreatureSubtypesDiedThisTurnComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dmu.cards.CultConscript
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Cult Conscript (DMU #88) — {B} 2/1 Creature — Skeleton Warrior.
 *
 * "This creature enters tapped.
 *  {1}{B}: Return this card from your graveyard to the battlefield. Activate only if a non-Skeleton
 *  creature died under your control this turn."
 *
 * The restriction is `NonSubtypeCreatureDiedThisTurn(SKELETON, player = You)`: a single death must
 * be both non-Skeleton *and* under your control. A Skeleton of yours dying alongside an opponent's
 * non-Skeleton must not satisfy it.
 */
class CultConscriptScenarioTest : FunSpec({

    val returnAbility = CultConscript.activatedAbilities.single().id

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + CultConscript)
        d.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.recordDeaths(player: EntityId, vararg subtypes: Set<String>) =
        addComponent(player, CreatureSubtypesDiedThisTurnComponent(subtypes.toList()))

    fun GameTestDriver.activateReturn(conscript: EntityId) =
        submit(ActivateAbility(playerId = player1, sourceId = conscript, abilityId = returnAbility))

    test("enters tapped") {
        val d = driver()
        val conscript = d.putCardInHand(d.player1, "Cult Conscript")
        d.giveMana(d.player1, Color.BLACK, 1)
        d.castSpell(d.player1, conscript).error shouldBe null
        d.bothPass()
        d.isTapped(d.findPermanent(d.player1, "Cult Conscript")!!) shouldBe true
    }

    test("your non-Skeleton creature died: returns from graveyard to the battlefield tapped") {
        val d = driver()
        val conscript = d.putCardInGraveyard(d.player1, "Cult Conscript")
        val lions = d.putPermanentOnBattlefield(d.player1, "Savannah Lions")
        val doomBlade = d.putCardInHand(d.player1, "Doom Blade")
        d.giveMana(d.player1, Color.BLACK, 2)
        d.castSpell(d.player1, doomBlade, targets = listOf(lions)).error shouldBe null
        d.bothPass()

        d.giveMana(d.player1, Color.BLACK, 2)
        d.activateReturn(conscript).error shouldBe null
        d.bothPass()

        val returned = d.findPermanent(d.player1, "Cult Conscript")
        returned shouldNotBe null
        d.isTapped(returned!!) shouldBe true
    }

    test("nothing died: can't activate") {
        val d = driver()
        val conscript = d.putCardInGraveyard(d.player1, "Cult Conscript")
        d.giveMana(d.player1, Color.BLACK, 2)
        d.activateReturn(conscript).error shouldNotBe null
    }

    test("only an opponent's non-Skeleton creature died: can't activate") {
        val d = driver()
        val conscript = d.putCardInGraveyard(d.player1, "Cult Conscript")
        val lions = d.putPermanentOnBattlefield(d.player2, "Savannah Lions")
        val doomBlade = d.putCardInHand(d.player1, "Doom Blade")
        d.giveMana(d.player1, Color.BLACK, 2)
        d.castSpell(d.player1, doomBlade, targets = listOf(lions)).error shouldBe null
        d.bothPass()

        d.giveMana(d.player1, Color.BLACK, 2)
        d.activateReturn(conscript).error shouldNotBe null
    }

    test("only your Skeleton died: can't activate") {
        val d = driver()
        val conscript = d.putCardInGraveyard(d.player1, "Cult Conscript")
        d.recordDeaths(d.player1, setOf("Skeleton", "Warrior"))
        d.giveMana(d.player1, Color.BLACK, 2)
        d.activateReturn(conscript).error shouldNotBe null
    }

    test("your Skeleton and an opponent's non-Skeleton died: still can't activate") {
        val d = driver()
        val conscript = d.putCardInGraveyard(d.player1, "Cult Conscript")
        d.recordDeaths(d.player1, setOf("Skeleton", "Warrior"))
        d.recordDeaths(d.player2, setOf("Human"))
        d.giveMana(d.player1, Color.BLACK, 2)
        d.activateReturn(conscript).error shouldNotBe null
    }
})
