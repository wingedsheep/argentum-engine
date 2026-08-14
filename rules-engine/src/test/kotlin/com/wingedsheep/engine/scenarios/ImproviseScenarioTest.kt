package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.engine.view.LegalActionInfo
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.AlternativePaymentChoice
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeywordToOwnSpells
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Engine-level scenario tests for **improvise** (CR 702.126) — the mechanic, not one card.
 *
 * CR 702.126a: *"For each generic mana in this spell's total cost, you may tap an untapped
 * artifact you control rather than pay that mana."* CR 702.126b adds that improvise is neither an
 * additional nor an alternative cost and applies only after the total cost is determined, and
 * CR 702.126c that multiple instances are redundant.
 *
 * Improvise is the artifacts-only case of the shared tap-for-generic payment rail: the chosen
 * artifacts travel in [AlternativePaymentChoice.tapForGenericPermanents], the same carrier a
 * waterbend cost uses, and the eligibility filter is what separates them.
 *
 * Rules pinned here:
 *  1. Each tapped artifact pays {1} generic, and the artifacts end up tapped.
 *  2. Improvise never pays a colored pip — the colored part still needs real mana.
 *  3. Taps beyond the generic in the cost are ignored (and those artifacts stay untapped).
 *  4. Only untapped artifacts the caster controls are eligible — not creatures, not the
 *     opponent's, not already-tapped ones.
 *  5. The cast is *enumerated* as affordable when the artifacts make up the shortfall, and the
 *     legal action carries the tap metadata and the "improvise" label the client renders.
 *  6. A spell without improvise ignores the taps entirely — nothing is tapped, nothing is
 *     discounted, and the cast fails for lack of mana.
 *  7. CR 702.126c — a second, *granted* instance of improvise on a spell that already prints it
 *     is redundant: it neither raises the tap cap nor doubles the discount.
 *  8. Improvise composes with another alternative payment on the same spell (delve, convoke).
 *     The grant is by card type, so this is the ordinary case rather than a corner one — and the
 *     no-taps configuration stays reachable, since a mana rock is worth more untapped.
 */
class ImproviseScenarioTest : ScenarioTestBase() {

    init {
        // {4}{U} sorcery with printed improvise: one colored pip, four generic.
        val improviser = card("Improvising Blueprint") {
            manaCost = "{4}{U}"
            colorIdentity = "U"
            typeLine = "Sorcery"
            oracleText = "Improvise\nYou gain 5 life."
            keywords(Keyword.IMPROVISE)
            spell {
                effect = Effects.GainLife(5)
            }
        }
        cardRegistry.register(improviser)

        // Same cost, no improvise — the control for rule 6.
        val plain = card("Plain Blueprint") {
            manaCost = "{4}{U}"
            colorIdentity = "U"
            typeLine = "Sorcery"
            oracleText = "You gain 5 life."
            spell {
                effect = Effects.GainLife(5)
            }
        }
        cardRegistry.register(plain)

        // {6}{U} with printed delve, no printed improvise — the "improvise rides along on another
        // alternative payment" case, reached via the Beacon's grant.
        val delver = card("Delving Blueprint") {
            manaCost = "{6}{U}"
            colorIdentity = "U"
            typeLine = "Sorcery"
            oracleText = "Delve\nYou gain 5 life."
            keywords(Keyword.DELVE)
            spell {
                effect = Effects.GainLife(5)
            }
        }
        cardRegistry.register(delver)

        val trinket = card("Improvise Trinket") {
            manaCost = "{1}"
            colorIdentity = ""
            typeLine = "Artifact"
            oracleText = ""
        }
        cardRegistry.register(trinket)

        // Grants improvise to noncreature spells — the Ironheart, Clever Champion shape. Used to
        // put a *second* instance of improvise on a spell that already prints it (CR 702.126c).
        val beacon = card("Improvise Beacon") {
            manaCost = "{2}"
            colorIdentity = ""
            typeLine = "Artifact"
            oracleText = "Noncreature spells you cast have improvise."
            staticAbility {
                ability = GrantKeywordToOwnSpells(
                    keyword = Keyword.IMPROVISE,
                    spellFilter = GameObjectFilter.Noncreature
                )
            }
        }
        cardRegistry.register(beacon)

        fun castAction(game: TestGame, name: String): LegalActionInfo? =
            game.getLegalActions(1).firstOrNull {
                it.actionType == "CastSpell" && it.action is CastSpell && it.description.contains(name)
            }

        context("Paying with improvise") {

            test("each tapped artifact pays {1} of the generic, and the artifacts are tapped") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardInHand(1, "Improvising Blueprint")
                    .withLandsOnBattlefield(1, "Island", 1) // only the {U}
                    .withCardOnBattlefield(1, "Improvise Trinket")
                    .withCardOnBattlefield(1, "Improvise Trinket")
                    .withCardOnBattlefield(1, "Improvise Trinket")
                    .withCardOnBattlefield(1, "Improvise Trinket")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val before = game.getLifeTotal(1)
                val artifacts = game.findAllPermanents("Improvise Trinket")
                artifacts.size shouldBe 4

                val action = castAction(game, "Improvising Blueprint")
                withClue("one Island plus four artifacts should make {4}{U} affordable") {
                    action shouldNotBe null
                    action!!.isAffordable shouldBe true
                }

                val cast = (action!!.action as CastSpell).copy(
                    alternativePayment = AlternativePaymentChoice(tapForGenericPermanents = artifacts.toSet())
                )
                val result = game.execute(cast)
                withClue("four artifacts should cover the {4}: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()
                game.getLifeTotal(1) shouldBe before + 5
                withClue("every improvised artifact ends up tapped") {
                    artifacts.all { game.state.getEntity(it)!!.has<TappedComponent>() } shouldBe true
                }
            }

            test("improvise cannot pay the colored pip") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardInHand(1, "Improvising Blueprint")
                    .withCardOnBattlefield(1, "Improvise Trinket")
                    .withCardOnBattlefield(1, "Improvise Trinket")
                    .withCardOnBattlefield(1, "Improvise Trinket")
                    .withCardOnBattlefield(1, "Improvise Trinket")
                    .withCardOnBattlefield(1, "Improvise Trinket")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Five artifacts, no lands: CR 702.126a only lets them pay the four generic, so the
                // {U} is still owed and the cast must not be offered as affordable.
                val action = castAction(game, "Improvising Blueprint")
                withClue("with no blue source the improvise spell is not castable") {
                    (action == null || !action.isAffordable) shouldBe true
                }
            }

            test("taps beyond the generic in the cost are ignored and stay untapped") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardInHand(1, "Improvising Blueprint")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withCardOnBattlefield(1, "Improvise Trinket")
                    .withCardOnBattlefield(1, "Improvise Trinket")
                    .withCardOnBattlefield(1, "Improvise Trinket")
                    .withCardOnBattlefield(1, "Improvise Trinket")
                    .withCardOnBattlefield(1, "Improvise Trinket")
                    .withCardOnBattlefield(1, "Improvise Trinket")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val artifacts = game.findAllPermanents("Improvise Trinket")
                artifacts.size shouldBe 6
                val action = castAction(game, "Improvising Blueprint")!!
                val cast = (action.action as CastSpell).copy(
                    alternativePayment = AlternativePaymentChoice(tapForGenericPermanents = artifacts.toSet())
                )
                val result = game.execute(cast)
                result.error shouldBe null
                game.resolveStack()

                val tapped = artifacts.count { game.state.getEntity(it)!!.has<TappedComponent>() }
                withClue("only the {4} of generic can be improvised — the other two artifacts stay untapped") {
                    tapped shouldBe 4
                }
            }
        }

        context("Redundancy (CR 702.126c)") {

            test("a second, granted instance of improvise adds nothing") {
                // "Improvising Blueprint" prints improvise; the Beacon grants improvise to every
                // noncreature spell its controller casts. CR 702.126c: multiple instances on the
                // same spell are redundant — so the taps are still capped at the {4} of generic,
                // not doubled to {8}, and the spell still can't touch the {U}.
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardInHand(1, "Improvising Blueprint")
                    .withLandsOnBattlefield(1, "Island", 1) // only the {U}
                    .withCardOnBattlefield(1, "Improvise Beacon")
                    .withCardOnBattlefield(1, "Improvise Trinket")
                    .withCardOnBattlefield(1, "Improvise Trinket")
                    .withCardOnBattlefield(1, "Improvise Trinket")
                    .withCardOnBattlefield(1, "Improvise Trinket")
                    .withCardOnBattlefield(1, "Improvise Trinket")
                    .withCardOnBattlefield(1, "Improvise Trinket")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val before = game.getLifeTotal(1)
                val trinkets = game.findAllPermanents("Improvise Trinket")
                trinkets.size shouldBe 6
                val beaconId = game.findAllPermanents("Improvise Beacon").single()

                val action = castAction(game, "Improvising Blueprint")!!
                withClue("one instance or two, the payment is the same one rail") {
                    action.hasTapForGeneric shouldBe true
                    action.tapForGenericLabel shouldBe "improvise"
                    action.tapForGenericAmount shouldBe null
                }

                // Offer every artifact, the Beacon included — it is an untapped artifact too.
                val cast = (action.action as CastSpell).copy(
                    alternativePayment = AlternativePaymentChoice(
                        tapForGenericPermanents = (trinkets + beaconId).toSet()
                    )
                )
                val result = game.execute(cast)
                result.error shouldBe null
                game.resolveStack()
                game.getLifeTotal(1) shouldBe before + 5

                val tapped = (trinkets + beaconId).count { game.state.getEntity(it)!!.has<TappedComponent>() }
                withClue("redundant instances don't raise the cap: still only the {4} of generic") {
                    tapped shouldBe 4
                }
            }
        }

        context("Eligibility") {

            test("only the caster's untapped artifacts are offered") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardInHand(1, "Improvising Blueprint")
                    .withLandsOnBattlefield(1, "Island", 5)
                    .withCardOnBattlefield(1, "Improvise Trinket")                 // eligible
                    .withCardOnBattlefield(1, "Improvise Trinket", tapped = true)  // already tapped
                    .withCardOnBattlefield(1, "Glory Seeker")                      // creature, not an artifact
                    .withCardOnBattlefield(2, "Improvise Trinket")                 // opponent's
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val action = castAction(game, "Improvising Blueprint")
                withClue("the cast should carry the tap-for-generic metadata") {
                    action shouldNotBe null
                    action!!.hasTapForGeneric shouldBe true
                    action.tapForGenericLabel shouldBe "improvise"
                    withClue("no cap beyond the generic in the cost (CR 702.126a)") {
                        action.tapForGenericAmount shouldBe null
                    }
                }

                val mine = game.findAllPermanents("Improvise Trinket").filter { id ->
                    val entity = game.state.getEntity(id)!!
                    entity.get<ControllerComponent>()?.playerId == game.player1Id &&
                        !entity.has<TappedComponent>()
                }
                mine.size shouldBe 1
                val offered = action!!.validTapForGenericPermanents!!.map { it.entityId }
                withClue("creatures, tapped artifacts and the opponent's artifacts are all excluded") {
                    offered shouldContainExactlyInAnyOrder mine
                }
            }

            test("a spell without improvise ignores the taps entirely") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardInHand(1, "Plain Blueprint")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withCardOnBattlefield(1, "Improvise Trinket")
                    .withCardOnBattlefield(1, "Improvise Trinket")
                    .withCardOnBattlefield(1, "Improvise Trinket")
                    .withCardOnBattlefield(1, "Improvise Trinket")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val action = castAction(game, "Plain Blueprint")
                withClue("without improvise, one Island cannot pay {4}{U}") {
                    (action == null || !action.isAffordable) shouldBe true
                }

                // Force the action through anyway: a forged tap payment must not discount the cost
                // or tap anything.
                val cardId = game.state.getZone(ZoneKey(game.player1Id, Zone.HAND)).first { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Plain Blueprint"
                }
                val artifacts = game.findAllPermanents("Improvise Trinket")
                val result = game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = cardId,
                        alternativePayment = AlternativePaymentChoice(tapForGenericPermanents = artifacts.toSet())
                    )
                )
                withClue("the cast must fail — improvise isn't there to be used") {
                    result.error shouldNotBe null
                }
                withClue("and nothing was tapped on the way to failing") {
                    artifacts.none { result.state.getEntity(it)!!.has<TappedComponent>() } shouldBe true
                }
            }
        }

        context("Alongside another alternative payment") {

            // Improvise is granted by card *type* — "Noncreature spells you cast have improvise"
            // — so it lands on spells that print delve or convoke, which are noncreature almost to
            // a card (Treasure Cruise, Dig Through Time, Murderous Cut, Temporal Cleansing…).
            // The handler applies delve/convoke first and improvise second, so enumeration has to
            // consider them together: an unaffordable cast is dropped from the legal actions
            // entirely, not greyed out, so missing this makes a legal play impossible to reach.

            test("granted improvise stacks with delve on the same spell") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardInHand(1, "Delving Blueprint")
                    .withLandsOnBattlefield(1, "Island", 1) // the {U} only
                    .withCardInGraveyard(1, "Improvise Trinket")
                    .withCardInGraveyard(1, "Improvise Trinket")
                    .withCardOnBattlefield(1, "Improvise Beacon")
                    .withCardOnBattlefield(1, "Improvise Trinket")
                    .withCardOnBattlefield(1, "Improvise Trinket")
                    .withCardOnBattlefield(1, "Improvise Trinket")
                    .withCardOnBattlefield(1, "Improvise Trinket")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val before = game.getLifeTotal(1)
                val artifacts = game.findAllPermanents("Improvise Trinket")
                val graveyard = game.findCardsInGraveyard(1, "Improvise Trinket")
                artifacts.size shouldBe 4
                graveyard.size shouldBe 2

                // {6}{U}: delve two graveyard cards -> {4}{U}; four artifacts improvise the {4};
                // the lone Island pays the {U}. Neither payment covers it alone.
                val action = castAction(game, "Delving Blueprint")
                withClue("delve 2 + improvise 4 + one Island covers {6}{U}") {
                    action shouldNotBe null
                    action!!.isAffordable shouldBe true
                }

                val cast = (action!!.action as CastSpell).copy(
                    alternativePayment = AlternativePaymentChoice(
                        delvedCards = graveyard,
                        tapForGenericPermanents = artifacts.toSet()
                    )
                )
                val result = game.execute(cast)
                withClue("the engine accepts the combined payment: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()
                game.getLifeTotal(1) shouldBe before + 5
                withClue("every improvised artifact ends up tapped") {
                    artifacts.all { game.state.getEntity(it)!!.has<TappedComponent>() } shouldBe true
                }
            }

            test("a delve spell stays castable on mana alone when the artifacts are worth more untapped") {
                // The no-taps configuration has to remain reachable: a rock that taps for {3} is
                // worth more as a mana source than as a {1} improvise tap, so counting it as a tap
                // must never *remove* affordability the player already had.
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardInHand(1, "Delving Blueprint")
                    .withLandsOnBattlefield(1, "Island", 5)
                    .withCardInGraveyard(1, "Improvise Trinket")
                    .withCardInGraveyard(1, "Improvise Trinket")
                    .withCardOnBattlefield(1, "Improvise Beacon") // grants improvise; taps for nothing
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // {6}{U} minus delve 2 = {4}{U}; five Islands cover it with no taps at all.
                val action = castAction(game, "Delving Blueprint")
                withClue("delve plus five Islands is enough on its own") {
                    action shouldNotBe null
                    action!!.isAffordable shouldBe true
                }
            }
        }
    }
}
