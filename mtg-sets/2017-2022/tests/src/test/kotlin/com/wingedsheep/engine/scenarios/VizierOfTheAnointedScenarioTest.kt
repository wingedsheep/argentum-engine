package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.mtg.sets.definitions.dft.cards.CurseclothWrappings
import com.wingedsheep.mtg.sets.definitions.hou.cards.VizierOfTheAnointed
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.embalm
import com.wingedsheep.sdk.dsl.embalmAbility
import com.wingedsheep.sdk.dsl.eternalize
import com.wingedsheep.sdk.dsl.eternalizeAbility
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Vizier of the Anointed (HOU #55) — {3}{U} 2/4.
 *
 *   When this creature enters, you may search your library for a creature card with eternalize or
 *   embalm, put that card into your graveyard, then shuffle.
 *   Whenever you activate an eternalize or embalm ability, draw a card.
 *
 * Pins the keyword identity of the two graveyard abilities: the search sees the printed keywords,
 * and the draw trigger fires on the eternalize / embalm *ability* — after its costs are paid, above
 * it on the stack (the 2017-07-14 ruling) — but not on another ability of the same card, nor on an
 * opponent's activation.
 */
class VizierOfTheAnointedScenarioTest : FunSpec({

    val eternalCat = card("Vizier Test Eternal Cat") {
        manaCost = "{1}{W}"
        typeLine = "Creature — Cat"
        power = 2
        toughness = 2
        activatedAbility {
            cost = Costs.Mana("{1}")
            effect = Effects.ModifyStats(1, 0, EffectTarget.Self)
        }
        eternalize("{2}")
    }

    val embalmedBird = card("Vizier Test Embalmed Bird") {
        manaCost = "{W}"
        typeLine = "Creature — Bird"
        power = 1
        toughness = 1
        embalm("{1}")
    }

    val plainBear = card("Vizier Test Plain Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }

    val eternalizeId = eternalizeAbility(ManaCost.parse("{2}")).id
    val embalmId = embalmAbility(ManaCost.parse("{1}")).id

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(VizierOfTheAnointed, CurseclothWrappings, eternalCat, embalmedBird, plainBear))
        initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.resolveAll(
        search: Boolean = true,
        onSearch: (SelectCardsDecision) -> List<EntityId> = { emptyList() },
    ) {
        var guard = 0
        while ((state.stack.isNotEmpty() || pendingDecision != null) && guard++ < 20) {
            when (val d = pendingDecision) {
                is YesNoDecision -> submitYesNo(d.playerId, search)
                is SelectCardsDecision -> submitCardSelection(d.playerId, onSearch(d))
                null -> bothPass()
                else -> autoResolveDecision()
            }
        }
    }

    fun GameTestDriver.activateFromGraveyard(player: EntityId, card: EntityId, abilityId: com.wingedsheep.sdk.scripting.AbilityId) {
        giveColorlessMana(player, 2)
        submit(ActivateAbility(playerId = player, sourceId = card, abilityId = abilityId)).outcome shouldBe Outcome.Done
    }

    test("enters: the search offers only creature cards with eternalize or embalm and puts the pick into the graveyard") {
        val d = driver()
        val me = d.player1
        val cat = d.putCardOnTopOfLibrary(me, "Vizier Test Eternal Cat")
        val bird = d.putCardOnTopOfLibrary(me, "Vizier Test Embalmed Bird")
        d.putCardOnTopOfLibrary(me, "Vizier Test Plain Bear")
        val vizier = d.putCardInHand(me, "Vizier of the Anointed")
        d.giveMana(me, Color.BLUE, 1)
        d.giveColorlessMana(me, 3)
        d.castSpell(me, vizier).outcome shouldBe Outcome.Done

        var offered: List<EntityId> = emptyList()
        d.resolveAll { decision ->
            offered = decision.options - decision.nonSelectableOptions.toSet()
            listOf(cat)
        }

        withClue("the plain Bear and the Islands are not offered") {
            offered shouldContainExactlyInAnyOrder listOf(cat, bird)
        }
        d.getGraveyard(me) shouldContain cat
    }

    test("enters: declining the search leaves library and graveyard alone") {
        val d = driver()
        val me = d.player1
        val cat = d.putCardOnTopOfLibrary(me, "Vizier Test Eternal Cat")
        val vizier = d.putCardInHand(me, "Vizier of the Anointed")
        d.giveMana(me, Color.BLUE, 1)
        d.giveColorlessMana(me, 3)
        d.castSpell(me, vizier).outcome shouldBe Outcome.Done

        var searched = false
        d.resolveAll(search = false) { searched = true; emptyList() }

        searched shouldBe false
        d.getGraveyard(me).contains(cat) shouldBe false
        d.state.getZone(me, Zone.LIBRARY) shouldContain cat
    }

    test("activating a granted embalm ability draws a card too") {
        val d = driver()
        val me = d.player1
        d.putCreatureOnBattlefield(me, "Vizier of the Anointed")
        val wrappings = d.putPermanentOnBattlefield(me, "Cursecloth Wrappings")
        val bear = d.putCardInGraveyard(me, "Vizier Test Plain Bear")
        val grant = d.cardRegistry.getCard("Cursecloth Wrappings")!!.activatedAbilities.first().id
        d.submit(
            ActivateAbility(
                playerId = me,
                sourceId = wrappings,
                abilityId = grant,
                targets = listOf(ChosenTarget.Card(bear, me, Zone.GRAVEYARD)),
            )
        ).outcome shouldBe Outcome.Done
        d.resolveAll()
        val handBefore = d.getHandSize(me)

        withClue("Cursecloth grants embalm at the Bear's mana cost, {1}{G}") {
            d.giveMana(me, Color.GREEN, 1)
            d.giveColorlessMana(me, 1)
            d.submit(
                ActivateAbility(playerId = me, sourceId = bear, abilityId = embalmAbility(ManaCost.parse("{1}{G}")).id)
            ).outcome shouldBe Outcome.Done
        }
        d.resolveAll()

        d.getHandSize(me) shouldBe handBefore + 1
        d.findPermanent(me, "Vizier Test Plain Bear").shouldNotBeNull()
    }

    test("activating an eternalize ability draws a card before the ability resolves") {
        val d = driver()
        val me = d.player1
        d.putCreatureOnBattlefield(me, "Vizier of the Anointed")
        val cat = d.putCardInGraveyard(me, "Vizier Test Eternal Cat")
        val handBefore = d.getHandSize(me)

        d.activateFromGraveyard(me, cat, eternalizeId)
        withClue("the card is exiled as a cost; the draw trigger is stacked above the ability") {
            d.state.getZone(me, Zone.EXILE) shouldContain cat
            d.state.stack.size shouldBe 2
        }
        d.bothPass()
        withClue("the trigger resolved first: one card drawn, no token yet") {
            d.getHandSize(me) shouldBe handBefore + 1
            d.findPermanent(me, "Vizier Test Eternal Cat") shouldBe null
        }
        d.resolveAll()
        val token = d.findPermanent(me, "Vizier Test Eternal Cat").shouldNotBeNull()
        d.state.projectedState.getPower(token) shouldBe 4
    }

    test("activating an embalm ability draws a card") {
        val d = driver()
        val me = d.player1
        d.putCreatureOnBattlefield(me, "Vizier of the Anointed")
        val bird = d.putCardInGraveyard(me, "Vizier Test Embalmed Bird")
        val handBefore = d.getHandSize(me)

        d.activateFromGraveyard(me, bird, embalmId)
        d.resolveAll()

        d.getHandSize(me) shouldBe handBefore + 1
        d.findPermanent(me, "Vizier Test Embalmed Bird").shouldNotBeNull()
    }

    test("another ability of an eternalize creature does not draw") {
        val d = driver()
        val me = d.player1
        d.putCreatureOnBattlefield(me, "Vizier of the Anointed")
        val cat = d.putCreatureOnBattlefield(me, "Vizier Test Eternal Cat")
        val pump = d.cardRegistry.getCard("Vizier Test Eternal Cat")!!.activatedAbilities
            .single { it.keyword == null }.id
        val handBefore = d.getHandSize(me)

        d.giveColorlessMana(me, 1)
        d.submit(ActivateAbility(playerId = me, sourceId = cat, abilityId = pump)).outcome shouldBe Outcome.Done
        d.state.stack.size shouldBe 1
        d.resolveAll()

        d.getHandSize(me) shouldBe handBefore
    }

    test("an opponent's embalm activation does not draw") {
        val d = driver()
        val me = d.player1
        val opponent = d.getOpponent(me)
        d.putCreatureOnBattlefield(me, "Vizier of the Anointed")
        val bird = d.putCardInGraveyard(opponent, "Vizier Test Embalmed Bird")
        d.passPriorityUntil(Step.PRECOMBAT_MAIN, activePlayer = opponent)
        d.state.activePlayerId shouldBe opponent
        val handBefore = d.getHandSize(me)

        d.activateFromGraveyard(opponent, bird, embalmId)
        d.resolveAll()

        d.getHandSize(me) shouldBe handBefore
        d.findPermanent(opponent, "Vizier Test Embalmed Bird").shouldNotBeNull()
    }

    test("the eternalize and embalm abilities carry their keyword, so the trigger can name them") {
        eternalizeAbility(ManaCost.parse("{2}")).keyword shouldBe Keyword.ETERNALIZE
        embalmAbility(ManaCost.parse("{1}")).keyword shouldBe Keyword.EMBALM
    }
})
