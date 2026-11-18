package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * A library-search trigger (CR 701.23) binds the searcher as the triggering player, so
 * "whenever a player searches their library, that player …" resolves `Player.TriggeringPlayer`
 * to whoever searched — not to no one.
 */
class LibrarySearchTriggeringPlayerTest : FunSpec({
    val opponentPunisher = card("Search Punisher") {
        typeLine = "Enchantment"
        triggeredAbility {
            trigger = Triggers.anOpponent.searchesLibrary()
            effect = Effects.LoseLife(3, EffectTarget.PlayerRef(Player.TriggeringPlayer))
        }
    }
    val anySearchPunisher = card("Any Search Punisher") {
        typeLine = "Enchantment"
        triggeredAbility {
            trigger = Triggers.anyPlayer.searchesLibrary()
            effect = Effects.LoseLife(2, EffectTarget.PlayerRef(Player.TriggeringPlayer))
        }
    }
    val tutor = card("Plain Tutor") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell { effect = Patterns.Library.searchLibrary(GameObjectFilter.Any) }
    }
    val everyoneTutors = card("Everyone Tutors") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell { effect = Patterns.Library.eachPlayerSearchesLibrary(GameObjectFilter.Any, DynamicAmount.Fixed(1)) }
    }

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(opponentPunisher, anySearchPunisher, tutor, everyoneTutors))
        initMirrorMatch(Deck.of("Island" to 40), skipMulligans = true)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun GameTestDriver.castAndResolve(p: EntityId, name: String) {
        castSpell(p, putCardInHand(p, name)).error shouldBe null
        var guard = 0
        while ((pendingDecision != null || stackSize > 0) && guard++ < 40) {
            if (pendingDecision != null) autoResolveDecision() else bothPass()
        }
    }

    test("an opponent's search makes that opponent the triggering player") {
        val d = driver(); val searcher = d.activePlayer!!; val me = d.getOpponent(searcher)
        d.putPermanentOnBattlefield(me, "Search Punisher")
        d.castAndResolve(searcher, "Plain Tutor")
        withClue("the searcher loses the life") { d.getLifeTotal(searcher) shouldBe 17 }
        withClue("the trigger's controller doesn't") { d.getLifeTotal(me) shouldBe 20 }
    }

    test("each player searching binds each searcher in turn") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        d.putPermanentOnBattlefield(me, "Any Search Punisher")
        d.castAndResolve(me, "Everyone Tutors")
        d.getLifeTotal(me) shouldBe 18
        d.getLifeTotal(opp) shouldBe 18
    }
})
