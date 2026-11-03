package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.LibraryShuffledEvent
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.RedirectZoneChange
import com.wingedsheep.sdk.scripting.effects.AfterResolveDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * `spell { selfShuffleIntoLibrary() }` — the card-intrinsic "Shuffle <card name> into its owner's
 * library." clause printed on the Mirrodin Besieged Zenith cycle.
 *
 * The sibling of `selfExile()`: both replace the destination of **CR 608.2n** ("as the final part of
 * an instant or sorcery spell's resolution, the spell is put into its owner's graveyard"), read by
 * `StackResolver` at the same seam. What makes it worth its own test file is that the seam is
 * **duplicated**: `StackResolver` decides the destination once on the full-resolve path and again on
 * the paused-resolve path, and each then places the card at its own site. A card whose effect never
 * pauses exercises only the first pair — and Green Sun's Zenith, the cycle's headline card, pauses
 * for its library search and so takes the *other* one. Wiring one and not the other fails silently:
 * the card simply turns up in the graveyard.
 *
 * The negative cases matter as much as the positive ones. Unlike the cast-this-way rider
 * ([com.wingedsheep.engine.state.components.identity.AfterResolveDestinationComponent], covered by
 * [AfterResolveDestinationScenarioTest]), this clause is part of the spell's *effect*, so a spell
 * that never resolves never performs it:
 *
 * - **Countered** (CR 701.5a) — the spell is put into its owner's graveyard, and no part of its
 *   effect happens. Graveyard, not library.
 * - **Fizzled** (CR 608.2b) — a spell whose every target is illegal doesn't resolve. Same answer.
 *
 * That is the exact opposite of the rider's answer to the same two questions, which is why both
 * files exist.
 *
 * Two things about the seam are worth knowing before adding a card here:
 *
 * - **Flashback and harmonize still win.** They are the only replacements at this seam worded
 *   "exile this card instead of putting it *anywhere else* any time it would leave the stack"
 *   (CR 702.34a, CR 702.180a); every other clause — the cast-this-way rider, rebound, Adventure,
 *   Omen — names the *graveyard*, which is why the printed clause beats those and not these.
 *
 * - **The paused path moves the card early.** `StackResolver` puts the spell in the library and
 *   shuffles while the decision is still pending, so the remainder of that same resolution sees the
 *   card already in the library. Harmless for Green Sun's Zenith — a search's candidate list is
 *   built during effect execution, before the move, so the Zenith can never find itself — but it is
 *   a sharper edge than the same early move is for `selfExile()`, because a library card can be
 *   drawn or searched. A future pausing self-shuffling spell whose later steps draw cards would
 *   need this revisited (Blue Sun's Zenith's own ruling — "you won't be able to draw the same Blue
 *   Sun's Zenith that you cast" — depends on the ordering).
 */
class SelfShuffleIntoLibraryOnResolveScenarioTest : FunSpec({

    /** Targetless and non-pausing: the full-resolve path, with nothing else going on. */
    val plainZenith = card("Test Zenith Plain") {
        manaCost = "{1}"
        typeLine = "Sorcery"
        spell {
            effect = Effects.GainLife(2)
            selfShuffleIntoLibrary()
        }
    }

    /** The control — same card, same effect, no clause. Proves the assertions are the flag's doing. */
    val plainNoClause = card("Test Zenith No Clause") {
        manaCost = "{1}"
        typeLine = "Sorcery"
        spell { effect = Effects.GainLife(2) }
    }

    /**
     * Green Sun's Zenith's shape: a library search pauses resolution for a card-selection decision,
     * so the spell leaves the stack via the **paused-resolve** path rather than the full one.
     */
    val searchingZenith = card("Test Zenith Searching") {
        manaCost = "{1}"
        typeLine = "Sorcery"
        spell {
            effect = Patterns.Library.searchLibrary(
                filter = GameObjectFilter.Creature,
                count = 1,
                destination = SearchDestination.BATTLEFIELD,
            )
            selfShuffleIntoLibrary()
        }
    }

    /** Targets, so its target can be removed in response and the spell can fizzle (CR 608.2b). */
    val targetedZenith = card("Test Zenith Targeted") {
        manaCost = "{1}"
        typeLine = "Sorcery"
        spell {
            target(TargetFilter.Creature)
            effect = Effects.DealDamage(1, EffectTarget.ContextTarget(0))
            selfShuffleIntoLibrary()
        }
    }

    /** The clause plus flashback, the one replacement at this seam that still outranks it. */
    val flashbackZenith = card("Test Zenith Flashback") {
        manaCost = "{1}"
        typeLine = "Sorcery"
        spell {
            effect = Effects.GainLife(2)
            selfShuffleIntoLibrary()
        }
        keywordAbility(KeywordAbility.flashback("{1}"))
    }

    /** The same, but pausing mid-resolution, so the paused-resolve path is checked too. */
    val flashbackSearchingZenith = card("Test Zenith Flashback Searching") {
        manaCost = "{1}"
        typeLine = "Sorcery"
        spell {
            effect = Patterns.Library.searchLibrary(
                filter = GameObjectFilter.Creature,
                count = 1,
                destination = SearchDestination.BATTLEFIELD,
            )
            selfShuffleIntoLibrary()
        }
        keywordAbility(KeywordAbility.flashback("{1}"))
    }

    /**
     * Progenitus' shape, pointed at the library: a card-intrinsic self-replacement that catches the
     * spell on its way to the destination this clause chose. Proves the shuffle tail is gated on the
     * *final* zone rather than on the flag.
     */
    val redirectedZenith = card("Test Zenith Redirected") {
        manaCost = "{1}"
        typeLine = "Sorcery"
        spell {
            effect = Effects.GainLife(2)
            selfShuffleIntoLibrary()
        }
        replacementEffect(
            RedirectZoneChange(
                newDestination = Zone.EXILE,
                appliesTo = EventPattern.ZoneChangeEvent(to = Zone.LIBRARY),
                selfOnly = true,
            )
        )
    }

    /**
     * Kylox's Voltstrider's shape: casts a spell from a graveyard under the rider "if that spell
     * would be put into a graveyard, put it on the bottom of its owner's library instead."
     */
    val bottomGranter = card("Test Bottom Granter For Zenith") {
        manaCost = "{1}"
        typeLine = "Sorcery"
        spell {
            effect = GatherCardsEffect(
                source = CardSource.FromZone(
                    zone = Zone.GRAVEYARD,
                    filter = GameObjectFilter.InstantOrSorcery,
                ),
                storeAs = "pool",
            ) then
                SelectFromCollectionEffect(
                    from = "pool",
                    selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                    storeSelected = "pick",
                ) then
                Effects.CastFromCollectionWithoutPayingCost(
                    from = "pick",
                    insteadOfGraveyard = AfterResolveDestination.BOTTOM_OF_LIBRARY,
                )
        }
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCards(
            listOf(
                plainZenith, plainNoClause, searchingZenith, targetedZenith, bottomGranter,
                flashbackZenith, flashbackSearchingZenith, redirectedZenith,
            )
        )
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.settle() {
        var guard = 0
        while (guard++ < 30) {
            when {
                isPaused -> autoResolveDecision()
                state.stack.isNotEmpty() -> bothPass()
                else -> break
            }
        }
    }

    fun GameTestDriver.cast(
        you: EntityId,
        cardName: String,
        targets: List<ChosenTarget> = emptyList(),
    ): EntityId {
        val cardId = putCardInHand(you, cardName)
        giveMana(you, Color.BLUE, 4)
        submit(
            CastSpell(you, cardId, targets = targets, paymentStrategy = PaymentStrategy.FromPool)
        ).error shouldBe null
        return cardId
    }

    test("a resolved spell is shuffled into its owner's library — the full-resolve path") {
        val driver = newDriver()
        val you = driver.player1
        val librarySizeBefore = driver.state.getZone(ZoneKey(you, Zone.LIBRARY)).size
        val lifeBefore = driver.getLifeTotal(you)

        val cardId = driver.cast(you, "Test Zenith Plain")
        driver.settle()

        withClue("the spell resolved — its effect happened") {
            driver.getLifeTotal(you) shouldBe lifeBefore + 2
        }
        withClue("CR 608.2n's graveyard destination was replaced by the library") {
            driver.state.getZone(ZoneKey(you, Zone.LIBRARY)).contains(cardId) shouldBe true
            driver.state.getZone(ZoneKey(you, Zone.LIBRARY)).size shouldBe librarySizeBefore + 1
            driver.getGraveyardCardNames(you).contains("Test Zenith Plain") shouldBe false
            driver.getExile(you).contains(cardId) shouldBe false
        }
    }

    test("it is shuffled in, not put on the bottom") {
        val driver = newDriver()
        val you = driver.player1
        val shufflesBefore = driver.events.count { it is LibraryShuffledEvent && it.playerId == you }

        driver.cast(you, "Test Zenith Plain")
        driver.settle()

        withClue("the library is randomized and the shuffle announced, so a later effect that " +
            "looks at the top of the library can't be steered by casting this spell") {
            driver.events.count { it is LibraryShuffledEvent && it.playerId == you } shouldBe
                shufflesBefore + 1
        }
    }

    test("a spell that pauses mid-resolution is still shuffled in — the paused-resolve path") {
        val driver = newDriver()
        val you = driver.player1
        val creatureId = driver.putCardOnTopOfLibrary(you, "Grizzly Bears")
        val librarySizeBefore = driver.state.getZone(ZoneKey(you, Zone.LIBRARY)).size
        val shufflesBefore = driver.events.count { it is LibraryShuffledEvent && it.playerId == you }

        val cardId = driver.cast(you, "Test Zenith Searching")
        // The search pauses for a card-selection decision; take the creature.
        var guard = 0
        while (!driver.isPaused && driver.state.stack.isNotEmpty() && guard++ < 10) driver.bothPass()
        driver.submitCardSelection(you, listOf(creatureId))
        driver.settle()

        withClue("the search resolved — the creature is on the battlefield") {
            driver.state.getZone(ZoneKey(you, Zone.BATTLEFIELD)).contains(creatureId) shouldBe true
        }
        withClue("the paused-resolve path honours the clause too (Green Sun's Zenith's own shape)") {
            driver.state.getZone(ZoneKey(you, Zone.LIBRARY)).contains(cardId) shouldBe true
            driver.getGraveyardCardNames(you).contains("Test Zenith Searching") shouldBe false
        }
        withClue("net library change: the searched creature left, the spell came back") {
            driver.state.getZone(ZoneKey(you, Zone.LIBRARY)).size shouldBe librarySizeBefore
        }
        withClue("two separate shuffles, per the Green Sun's Zenith ruling: the search's own " +
            "shuffle and then the spell's. Shuffling once would be enough to randomize the " +
            "library, but effects that count shuffles (Psychogenic Probe) must see both") {
            driver.events.count { it is LibraryShuffledEvent && it.playerId == you } shouldBe
                shufflesBefore + 2
        }
    }

    test("a countered spell goes to the graveyard — CR 701.5a, the effect never happened") {
        val driver = newDriver()
        val you = driver.player1
        val opponent = driver.getOpponent(you)
        val counter = driver.putCardInHand(opponent, "Counterspell")
        driver.giveMana(opponent, Color.BLUE, 2)

        val cardId = driver.cast(you, "Test Zenith Plain")
        driver.passPriority(you)
        driver.submit(
            CastSpell(
                opponent, counter,
                targets = listOf(ChosenTarget.Spell(cardId)),
                paymentStrategy = PaymentStrategy.FromPool,
            )
        ).error shouldBe null
        driver.settle()

        withClue("it really was countered — no life gained") {
            driver.getLifeTotal(you) shouldBe 20
        }
        withClue("the shuffle is part of the effect, so a countered spell never performs it — " +
            "the opposite of the cast-this-way rider, which applies on the countered path too") {
            driver.getGraveyardCardNames(you).contains("Test Zenith Plain") shouldBe true
            driver.state.getZone(ZoneKey(you, Zone.LIBRARY)).contains(cardId) shouldBe false
        }
    }

    test("a fizzled spell goes to the graveyard — CR 608.2b") {
        val driver = newDriver()
        val you = driver.player1
        val bear = driver.putCreatureOnBattlefield(you, "Grizzly Bears")

        val cardId = driver.cast(
            you, "Test Zenith Targeted",
            targets = listOf(ChosenTarget.Permanent(bear)),
        )
        withClue("the spell waits on the stack with its target chosen") {
            driver.state.stack.contains(cardId) shouldBe true
        }

        // Break the only target: every target is now illegal, so the spell is removed from the
        // stack without resolving and none of its instructions are followed.
        driver.moveToGraveyard(bear)
        driver.settle()

        withClue("a spell that doesn't resolve never reaches CR 608.2n's replaced destination") {
            driver.getGraveyardCardNames(you).contains("Test Zenith Targeted") shouldBe true
            driver.state.getZone(ZoneKey(you, Zone.LIBRARY)).contains(cardId) shouldBe false
        }
    }

    test("the printed clause beats a conditional cast-this-way rider") {
        val driver = newDriver()
        val you = driver.player1
        val zenithId = driver.putCardInGraveyard(you, "Test Zenith Plain")
        val shufflesBefore = driver.events.count { it is LibraryShuffledEvent && it.playerId == you }

        val granterId = driver.putCardInHand(you, "Test Bottom Granter For Zenith")
        driver.giveMana(you, Color.BLUE, 4)
        driver.submit(
            CastSpell(you, granterId, paymentStrategy = PaymentStrategy.FromPool)
        ).error shouldBe null
        var guard = 0
        while (!driver.isPaused && driver.state.stack.isNotEmpty() && guard++ < 10) driver.bothPass()
        driver.submitCardSelection(you, listOf(zenithId))
        driver.settle()

        withClue("the rider replaces 'would be put into a graveyard', and this spell never would " +
            "be — so it is shuffled in, not laid on the bottom unshuffled") {
            driver.events.count { it is LibraryShuffledEvent && it.playerId == you } shouldBe
                shufflesBefore + 1
            driver.getGraveyardCardNames(you).contains("Test Zenith Plain") shouldBe false
        }
    }

    test("flashback outranks the printed clause — CR 702.34a's \"anywhere else\"") {
        val driver = newDriver()
        val you = driver.player1
        val librarySizeBefore = driver.state.getZone(ZoneKey(you, Zone.LIBRARY)).size
        val shufflesBefore = driver.events.count { it is LibraryShuffledEvent && it.playerId == you }
        val lifeBefore = driver.getLifeTotal(you)

        val cardId = driver.putCardInGraveyard(you, "Test Zenith Flashback")
        driver.giveMana(you, Color.BLUE, 4)
        driver.submit(
            CastSpell(
                you, cardId,
                paymentStrategy = PaymentStrategy.FromPool,
                useAlternativeCost = true,
            )
        ).error shouldBe null
        driver.settle()

        withClue("it resolved — the clause's own effect happened") {
            driver.getLifeTotal(you) shouldBe lifeBefore + 2
        }
        withClue("flashback replaces 'anywhere else any time it would leave the stack', not just " +
            "the graveyard, so it applies to the library move the printed clause asks for — " +
            "unlike the rider, rebound, Adventure and Omen, which all name the graveyard") {
            driver.getExile(you).contains(cardId) shouldBe true
            driver.state.getZone(ZoneKey(you, Zone.LIBRARY)).contains(cardId) shouldBe false
            driver.state.getZone(ZoneKey(you, Zone.LIBRARY)).size shouldBe librarySizeBefore
        }
        withClue("and nothing was shuffled — the tail is gated on the final destination") {
            driver.events.count { it is LibraryShuffledEvent && it.playerId == you } shouldBe
                shufflesBefore
        }
    }

    test("flashback outranks it on the paused-resolve path too") {
        val driver = newDriver()
        val you = driver.player1
        val creatureId = driver.putCardOnTopOfLibrary(you, "Grizzly Bears")

        val cardId = driver.putCardInGraveyard(you, "Test Zenith Flashback Searching")
        driver.giveMana(you, Color.BLUE, 4)
        driver.submit(
            CastSpell(
                you, cardId,
                paymentStrategy = PaymentStrategy.FromPool,
                useAlternativeCost = true,
            )
        ).error shouldBe null
        var guard = 0
        while (!driver.isPaused && driver.state.stack.isNotEmpty() && guard++ < 10) driver.bothPass()
        driver.submitCardSelection(you, listOf(creatureId))
        driver.settle()

        withClue("the search still resolved") {
            driver.state.getZone(ZoneKey(you, Zone.BATTLEFIELD)).contains(creatureId) shouldBe true
        }
        withClue("the duplicated seam has to agree with itself — wiring the precedence into only " +
            "one of the two paths is the silent half-fix this file exists to catch") {
            driver.getExile(you).contains(cardId) shouldBe true
            driver.state.getZone(ZoneKey(you, Zone.LIBRARY)).contains(cardId) shouldBe false
        }
    }

    test("a redirect away from the library suppresses the shuffle") {
        val driver = newDriver()
        val you = driver.player1
        val shufflesBefore = driver.events.count { it is LibraryShuffledEvent && it.playerId == you }

        val cardId = driver.cast(you, "Test Zenith Redirected")
        driver.settle()

        withClue("the self-replacement caught the card on its way to the library") {
            driver.getExile(you).contains(cardId) shouldBe true
            driver.state.getZone(ZoneKey(you, Zone.LIBRARY)).contains(cardId) shouldBe false
        }
        withClue("so no library was shuffled — the tail is gated on the destination the card " +
            "actually reached, not on the flag that asked for it") {
            driver.events.count { it is LibraryShuffledEvent && it.playerId == you } shouldBe
                shufflesBefore
        }
    }

    test("the printed clause beats the rider on the paused-resolve path too") {
        val driver = newDriver()
        val you = driver.player1
        driver.putCardOnTopOfLibrary(you, "Grizzly Bears")
        val zenithId = driver.putCardInGraveyard(you, "Test Zenith Searching")
        val shufflesBefore = driver.events.count { it is LibraryShuffledEvent && it.playerId == you }

        val granterId = driver.putCardInHand(you, "Test Bottom Granter For Zenith")
        driver.giveMana(you, Color.BLUE, 8)
        driver.submit(
            CastSpell(you, granterId, paymentStrategy = PaymentStrategy.FromPool)
        ).error shouldBe null
        var guard = 0
        while (!driver.isPaused && driver.state.stack.isNotEmpty() && guard++ < 10) driver.bothPass()
        driver.submitCardSelection(you, listOf(zenithId))
        driver.settle()

        withClue("the rider case was only ever checked on the full-resolve path; the paused one " +
            "has its own copy of the precedence and must answer the same way") {
            driver.state.getZone(ZoneKey(you, Zone.LIBRARY)).contains(zenithId) shouldBe true
            driver.getGraveyardCardNames(you).contains("Test Zenith Searching") shouldBe false
            driver.events.count { it is LibraryShuffledEvent && it.playerId == you } shouldBe
                (shufflesBefore + 2)
        }
    }

    test("a card can't print both destination clauses") {
        // Two spellings of one slot (the CR 608.2n destination). Without the guard this resolves
        // silently to whichever clause StackResolver checks first, which is not something a card
        // author should have to know.
        val ex = shouldThrow<IllegalArgumentException> {
            card("Test Zenith Contradictory") {
                manaCost = "{1}"
                typeLine = "Sorcery"
                spell {
                    effect = Effects.GainLife(2)
                    selfExile()
                    selfShuffleIntoLibrary()
                }
            }
        }
        ex.message shouldContain "Test Zenith Contradictory"
    }

    test("nor can a CardScript built directly set both") {
        // The DSL guard above never runs for a script assembled by hand — test fixtures and the
        // Assay compiler both do that — so the type itself has to refuse the pair.
        val ex = shouldThrow<IllegalArgumentException> {
            CardScript(selfExileOnResolve = true, selfLibraryPlacementOnResolve = ZonePlacement.Shuffled)
        }
        ex.message shouldContain "CR 608.2n destination"
    }

    test("without the clause the same spell goes to the graveyard — the control") {
        val driver = newDriver()
        val you = driver.player1

        val cardId = driver.cast(you, "Test Zenith No Clause")
        driver.settle()

        withClue("nothing replaced the destination, so CR 608.2n applies unchanged") {
            driver.getGraveyardCardNames(you).contains("Test Zenith No Clause") shouldBe true
            driver.state.getZone(ZoneKey(you, Zone.LIBRARY)).contains(cardId) shouldBe false
        }
    }
})
