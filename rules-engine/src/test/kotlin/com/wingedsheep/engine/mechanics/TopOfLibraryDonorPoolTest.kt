package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.DonorCards
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.HasAllActivatedAbilitiesOfCards
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * `DonorCards.TOP_OF_YOUR_LIBRARY` — the "top card of your library" pool of
 * `HasAllActivatedAbilitiesOfCards` (Conspicuous Snoop). Only the single top card of the
 * *controller's* library donates, narrowed by the card filter, and the pool is re-read live: when
 * the top card changes, the granted abilities change with it.
 */
class TopOfLibraryDonorPoolTest : FunSpec({

    val goblinMedic = card("Top Pool Test Goblin") {
        manaCost = "{1}"
        typeLine = "Creature — Goblin"
        power = 1
        toughness = 1
        oracleText = "{T}: You gain 2 life."
        activatedAbility {
            cost = Costs.Tap
            effect = Effects.GainLife(2)
        }
    }
    val humanMedic = card("Top Pool Test Human") {
        manaCost = "{1}"
        typeLine = "Creature — Human"
        power = 1
        toughness = 1
        oracleText = "{T}: You gain 3 life."
        activatedAbility {
            cost = Costs.Tap
            effect = Effects.GainLife(3)
        }
    }
    val snoop = card("Top Pool Test Snoop") {
        manaCost = "{2}"
        typeLine = "Creature — Goblin"
        power = 2
        toughness = 2
        oracleText = "As long as the top card of your library is a Goblin card, this creature has all activated abilities of that card."
        staticAbility {
            ability = HasAllActivatedAbilitiesOfCards(
                donors = DonorCards.TOP_OF_YOUR_LIBRARY,
                cardFilter = GameObjectFilter.Any.withSubtype("Goblin"),
            )
        }
    }
    val goblinAbility = goblinMedic.activatedAbilities[0].id
    val humanAbility = humanMedic.activatedAbilities[0].id

    fun driver(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(goblinMedic, humanMedic, snoop))
        it.initMirrorMatch(Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("a Goblin card on top of your library lends its activated ability") {
        val d = driver()
        val source = d.putPermanentOnBattlefield(d.player1, "Top Pool Test Snoop")
        d.putCardOnTopOfLibrary(d.player1, "Top Pool Test Goblin")

        val life = d.getLifeTotal(d.player1)
        d.submit(ActivateAbility(d.player1, source, goblinAbility)).error shouldBe null
        d.bothPass()
        d.getLifeTotal(d.player1) shouldBe life + 2
        d.isTapped(source) shouldBe true
    }

    test("a top card that fails the card filter lends nothing") {
        val d = driver()
        val source = d.putPermanentOnBattlefield(d.player1, "Top Pool Test Snoop")
        d.putCardOnTopOfLibrary(d.player1, "Top Pool Test Human")

        d.submit(ActivateAbility(d.player1, source, humanAbility)).error shouldNotBe null
    }

    test("only the top card donates — a Goblin second from the top lends nothing") {
        val d = driver()
        val source = d.putPermanentOnBattlefield(d.player1, "Top Pool Test Snoop")
        d.putCardOnTopOfLibrary(d.player1, "Top Pool Test Goblin")
        d.putCardOnTopOfLibrary(d.player1, "Island")

        d.submit(ActivateAbility(d.player1, source, goblinAbility)).error shouldNotBe null
    }

    test("the pool re-reads live: drawing the Goblin away removes the ability") {
        val d = driver()
        val source = d.putPermanentOnBattlefield(d.player1, "Top Pool Test Snoop")
        d.putCardOnTopOfLibrary(d.player1, "Top Pool Test Goblin")
        val top = d.state.getLibrary(d.player1).first()
        d.replaceState(
            d.state.removeFromZone(ZoneKey(d.player1, Zone.LIBRARY), top).addToZone(ZoneKey(d.player1, Zone.HAND), top)
        )

        d.submit(ActivateAbility(d.player1, source, goblinAbility)).error shouldNotBe null
    }

    test("the opponent's library is not your library") {
        val d = driver()
        val source = d.putPermanentOnBattlefield(d.player1, "Top Pool Test Snoop")
        d.putCardOnTopOfLibrary(d.player2, "Top Pool Test Goblin")

        d.submit(ActivateAbility(d.player1, source, goblinAbility)).error shouldNotBe null
    }
})
