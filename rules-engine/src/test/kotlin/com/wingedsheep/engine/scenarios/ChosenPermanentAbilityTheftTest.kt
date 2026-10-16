package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.chosenCreatureOf
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.GainActivatedAbilitiesOfPermanents
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PreventActivatedAbilities
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * "The chosen permanent" as a filter — `CardPredicate.ChosenBySource` — fed by an optional
 * `EntersWithChoice(CREATURE_ON_BATTLEFIELD, permanentFilter = …)`, the Scheming Fence shape:
 *  - the pool is the filter's (nonland permanents of either player), never the entering object;
 *  - `PreventActivatedAbilities(chosenBySource())` locks only the chosen permanent's abilities;
 *  - `GainActivatedAbilitiesOfPermanents(…, includeLoyaltyAbilities = false)` copies its
 *    activated abilities except loyalty abilities (CR 606), with the gainer as their source;
 *  - its `spendAsThoughAnyColor` lets any mana pay a copied colored pip, but a `{C}` pip still needs
 *    colorless mana (colorless is a type, not a color — CR 106.1a/b), and an ability another source
 *    grants the thief isn't relaxed;
 *  - once the chosen permanent leaves the battlefield it is a new object (CR 400.7): nothing stays
 *    locked or gained;
 *  - declining the "you may choose" records nothing, so nothing is locked or gained.
 */
class ChosenPermanentAbilityTheftTest : FunSpec({

    val thief = card("Ability Thief") {
        manaCost = "{W}{U}"
        typeLine = "Creature — Human Citizen"
        power = 2
        toughness = 3
        oracleText = "As this creature enters, you may choose a nonland permanent.\n" +
            "Activated abilities of the chosen permanent can't be activated.\n" +
            "This creature has all activated abilities of the chosen permanent except for loyalty abilities. " +
            "You may spend mana as though it were mana of any color to activate those abilities."
        replacementEffect(
            EntersWithChoice(
                ChoiceType.CREATURE_ON_BATTLEFIELD,
                permanentFilter = GameObjectFilter.NonlandPermanent,
                mayChooseNothing = true
            )
        )
        staticAbility { ability = PreventActivatedAbilities(filter = GameObjectFilter.ChosenBySource) }
        staticAbility {
            ability = GainActivatedAbilitiesOfPermanents(
                grantedTo = GroupFilter.source(),
                sourceFilter = GameObjectFilter.ChosenBySource,
                includeManaAbilities = true,
                includeLoyaltyAbilities = false,
                spendAsThoughAnyColor = true
            )
        }
    }

    // "{B}: +1/+1" and "{C}: +2/+0" — one colored pip, one colorless pip.
    val pumper = card("Two-Pip Pumper") {
        manaCost = "{2}"
        typeLine = "Artifact Creature — Construct"
        power = 0
        toughness = 1
        activatedAbility {
            cost = Costs.Mana("{B}")
            effect = Effects.ModifyStats(1, 1, EffectTarget.Self)
        }
        activatedAbility {
            cost = Costs.Mana("{C}")
            effect = Effects.ModifyStats(2, 0, EffectTarget.Self)
        }
    }

    val walker = card("Test Walker") {
        manaCost = "{2}{W}"
        typeLine = "Legendary Planeswalker — Test"
        startingLoyalty = 3
        oracleText = "+1: You gain 1 life."
        loyaltyAbility(+1) {
            effect = Effects.GainLife(1)
        }
    }

    // Grants every creature you control "{R}: +1/+0" — an ability the thief has from another source.
    val banner = card("Red Banner") {
        manaCost = "{2}"
        typeLine = "Artifact"
        staticAbility {
            ability = GrantActivatedAbility(
                ability = ActivatedAbility(
                    id = AbilityId.next(),
                    cost = Costs.Mana("{R}"),
                    effect = Effects.ModifyStats(1, 0, EffectTarget.Self)
                ),
                filter = GroupFilter.AllCreaturesYouControl
            )
        }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(thief, pumper, walker, banner))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 20), skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    /** Cast the thief and stop at its "you may choose" decision. */
    fun GameTestDriver.castThief(player: EntityId): SelectCardsDecision {
        giveMana(player, Color.WHITE, 1)
        giveMana(player, Color.BLUE, 1)
        castSpell(player, putCardInHand(player, "Ability Thief")).error shouldBe null
        bothPass()
        return pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
    }

    fun GameTestDriver.putWalker(player: EntityId): EntityId {
        val id = putPermanentOnBattlefield(player, "Test Walker")
        replaceState(state.updateEntity(id) { it.with(CountersComponent().withAdded(CounterType.LOYALTY, 3)) })
        return id
    }

    fun GameTestDriver.thiefId(player: EntityId) = findPermanent(player, "Ability Thief")!!

    fun GameTestDriver.offered(player: EntityId, sourceId: EntityId) = legalActions(player)
        .mapNotNull { it.action as? ActivateAbility }
        .filter { it.sourceId == sourceId }
        .map { it.abilityId }

    val pumpB by lazy { pumper.script.activatedAbilities[0].id }
    val pumpC by lazy { pumper.script.activatedAbilities[1].id }

    test("the pool is every nonland permanent of either player, optional, never the entering object") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        val mine = driver.putCreatureOnBattlefield(me, "Two-Pip Pumper")
        val theirs = driver.putCreatureOnBattlefield(opp, "Two-Pip Pumper")
        val theirWalker = driver.putWalker(opp)
        val land = driver.putLandOnBattlefield(opp, "Forest")

        val decision = driver.castThief(me)
        decision.options shouldContainAll listOf(mine, theirs, theirWalker)
        decision.options shouldNotContain land
        decision.minSelections shouldBe 0
    }

    test("the chosen permanent's abilities are locked, and the chooser has them") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        // The lock is who-blind: choosing your own permanent locks it for you too.
        val chosen = driver.putCreatureOnBattlefield(me, "Two-Pip Pumper")
        val bystander = driver.putCreatureOnBattlefield(me, "Two-Pip Pumper")

        driver.castThief(me)
        driver.submitCardSelection(me, listOf(chosen)).error shouldBe null
        val thiefId = driver.thiefId(me)
        driver.state.chosenCreatureOf(thiefId) shouldBe chosen

        withClue("the chosen Pumper is locked; an unchosen one isn't") {
            driver.offered(me, chosen) shouldBe emptyList()
            driver.giveMana(me, Color.BLACK, 1)
            driver.submit(ActivateAbility(me, chosen, pumpB, paymentStrategy = PaymentStrategy.FromPool)).error shouldNotBe null
            driver.submit(ActivateAbility(me, bystander, pumpB, paymentStrategy = PaymentStrategy.FromPool)).error shouldBe null
        }
        withClue("the thief offers both copied abilities") {
            driver.offered(me, thiefId).containsAll(listOf(pumpB, pumpC)) shouldBe true
        }
    }

    test("any mana pays a gained colored pip, but a {C} pip still needs colorless mana") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        val theirs = driver.putCreatureOnBattlefield(opp, "Two-Pip Pumper")
        driver.castThief(me)
        driver.submitCardSelection(me, listOf(theirs)).error shouldBe null
        val thiefId = driver.thiefId(me)

        driver.giveMana(me, Color.WHITE, 1)
        driver.submit(ActivateAbility(me, thiefId, pumpB, paymentStrategy = PaymentStrategy.FromPool)).error shouldBe null
        driver.bothPass()
        driver.state.projectedState.getPower(thiefId) shouldBe 3
        driver.state.projectedState.getPower(theirs) shouldBe 0

        driver.giveMana(me, Color.WHITE, 1)
        withClue("white mana can't be spent as colorless") {
            driver.submit(ActivateAbility(me, thiefId, pumpC, paymentStrategy = PaymentStrategy.FromPool)).error shouldNotBe null
        }
        driver.giveColorlessMana(me, 1)
        driver.submit(ActivateAbility(me, thiefId, pumpC, paymentStrategy = PaymentStrategy.FromPool)).error shouldBe null
        driver.bothPass()
        driver.state.projectedState.getPower(thiefId) shouldBe 5
    }

    test("a chosen planeswalker's loyalty abilities are locked but not gained") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val myWalker = driver.putWalker(me)
        val loyalty = walker.script.activatedAbilities.single().id

        driver.castThief(me)
        driver.submitCardSelection(me, listOf(myWalker)).error shouldBe null

        driver.offered(me, myWalker) shouldNotContain loyalty
        driver.submit(ActivateAbility(me, myWalker, loyalty)).error shouldNotBe null
        driver.offered(me, driver.thiefId(me)) shouldBe emptyList()
    }

    test("declining the choice records nothing: nothing is locked or gained") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val mine = driver.putCreatureOnBattlefield(me, "Two-Pip Pumper")

        driver.castThief(me)
        driver.submitCardSelection(me, emptyList()).error shouldBe null
        val thiefId = driver.thiefId(me)

        driver.state.chosenCreatureOf(thiefId) shouldBe null
        driver.offered(me, thiefId) shouldBe emptyList()
        driver.giveMana(me, Color.BLACK, 1)
        driver.submit(ActivateAbility(me, mine, pumpB, paymentStrategy = PaymentStrategy.FromPool)).error shouldBe null
    }

    test("an ability another source grants the thief isn't relaxed — only the copies are") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val chosen = driver.putCreatureOnBattlefield(me, "Two-Pip Pumper")
        driver.putPermanentOnBattlefield(me, "Red Banner")
        val bannerPump = banner.script.staticAbilities.filterIsInstance<GrantActivatedAbility>().single().ability.id

        driver.castThief(me)
        driver.submitCardSelection(me, listOf(chosen)).error shouldBe null
        val thiefId = driver.thiefId(me)

        driver.giveMana(me, Color.WHITE, 1)
        withClue("white mana can't pay the banner's {R}") {
            driver.submit(ActivateAbility(me, thiefId, bannerPump, paymentStrategy = PaymentStrategy.FromPool)).error shouldNotBe null
        }
        driver.submit(ActivateAbility(me, thiefId, pumpB, paymentStrategy = PaymentStrategy.FromPool)).error shouldBe null
    }

    test("a chosen mana creature's mana ability is locked; the thief's copy taps the thief") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val elves = driver.putCreatureOnBattlefield(me, "Llanowar Elves")
        driver.removeSummoningSickness(elves)
        val elvesMana = TestCards.LlanowarElves.script.activatedAbilities.single().id

        driver.castThief(me)
        driver.submitCardSelection(me, listOf(elves)).error shouldBe null
        val thiefId = driver.thiefId(me)
        driver.removeSummoningSickness(thiefId)

        driver.offered(me, elves) shouldNotContain elvesMana
        driver.offered(me, thiefId) shouldContain elvesMana
        driver.submit(ActivateAbility(me, thiefId, elvesMana)).error shouldBe null
        driver.isTapped(thiefId) shouldBe true
        driver.isTapped(elves) shouldBe false
    }

    test("once the chosen permanent leaves the battlefield nothing is gained — the returned card is a new object") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val chosen = driver.putCreatureOnBattlefield(me, "Two-Pip Pumper")

        driver.castThief(me)
        driver.submitCardSelection(me, listOf(chosen)).error shouldBe null
        val thiefId = driver.thiefId(me)
        driver.offered(me, thiefId).isNotEmpty() shouldBe true

        driver.moveToGraveyard(chosen)
        driver.state.chosenCreatureOf(thiefId) shouldBe null
        driver.offered(me, thiefId) shouldBe emptyList()

        val returned = driver.putCreatureOnBattlefield(me, "Two-Pip Pumper")
        driver.offered(me, thiefId) shouldBe emptyList()
        driver.giveMana(me, Color.BLACK, 1)
        driver.submit(ActivateAbility(me, returned, pumpB, paymentStrategy = PaymentStrategy.FromPool)).error shouldBe null
    }
})
