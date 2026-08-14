package com.wingedsheep.ai.engine.knowledge

import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.components.identity.RoomComponent
import com.wingedsheep.engine.state.components.identity.RoomFace
import com.wingedsheep.engine.state.components.identity.RoomFaceId
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.dsk.cards.UnholyAnnexRitualChamber
import com.wingedsheep.mtg.sets.definitions.woe.cards.VirtueOfLoyalty
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.shouldBe

/**
 * What the structural analyzer makes of real cards.
 *
 * These are the shapes the three Phase 6 consumers depend on, pinned against the actual card
 * definitions rather than hand-built scripts — a `cardDef` that changes shape (an effect renamed,
 * a static ability restructured) should fail here, which is the point.
 */
class CardIntentAnalyzerTest : ScenarioTestBase() {

    private fun intentOf(name: String) = CardIntentAnalyzer.analyze(cardRegistry.requireCard(name))

    private fun roomFace(name: String) = UnholyAnnexRitualChamber.cardFaces.first { it.name == name }

    /** An `Unholy Annex // Ritual Chamber` permanent with exactly [unlocked] doors open. */
    private fun roomPermanent(vararg unlocked: String) = ComponentContainer().withComponent(
        RoomComponent(
            faces = UnholyAnnexRitualChamber.cardFaces.map {
                RoomFace(RoomFaceId(it.name), it.name, it.manaCost)
            },
            unlocked = unlocked.map { RoomFaceId(it) }.toSet(),
        )
    )

    init {
        test("a repeatable tapper reads as a repeatable tapper") {
            val intent = intentOf("Icy Manipulator")
            intent.tags shouldContain IntentTag.TAPPER
            intent.repeatable shouldBe true
            intent.speed shouldBe Speed.ACTIVATED
            intent.affectsOpponent shouldBe true
            withClue("must clear the ~2.7 a Disenchant costs in card advantage") {
                intent.staticPriorValue shouldBeGreaterThan 2.7
            }
        }

        test("a mana rock is ramp, and is worth about a land") {
            val intent = intentOf("Mind Stone")
            intent.tags shouldContain IntentTag.RAMP
            withClue("the draw ability sacrifices the rock, so it is not a repeatable engine") {
                intent.repeatable shouldBe false
            }
            intent.staticPriorValue shouldBeLessThan 1.0
        }

        test("an anthem carries its per-creature bonus separately from its prior") {
            val intent = intentOf("Glorious Anthem")
            intent.tags shouldContain IntentTag.ANTHEM
            intent.speed shouldBe Speed.STATIC
            withClue("+1/+1 to each creature = 2 points of stats") { intent.anthemBonus shouldBe 2 }
            intent.staticPriorValue shouldBeGreaterThan 2.7
        }

        test("an ETB exile-until-leaves enchantment is exile removal, but not a repeatable one") {
            val intent = intentOf("Banishing Light")
            intent.tags shouldContain IntentTag.REMOVAL
            intent.tags shouldContain IntentTag.EXILE_REMOVAL
            withClue("its own ETB trigger fires once per object, not once per turn") {
                intent.repeatable shouldBe false
            }
            intent.staticPriorValue shouldBeGreaterThan 2.7
        }

        test("an instant that destroys is instant-speed removal, and not a combat trick") {
            val intent = intentOf("Disenchant")
            intent.tags shouldContain IntentTag.REMOVAL
            intent.speed shouldBe Speed.INSTANT
            intent.tags shouldNotContain IntentTag.COMBAT_TRICK
            withClue("a non-permanent is never on a battlefield, so it has no board prior") {
                intent.staticPriorValue shouldBe 0.0
            }
        }

        test("an instant pump is a combat trick, and says how much toughness it buys") {
            val intent = intentOf("Giant Growth")
            intent.tags shouldContain IntentTag.PUMP
            intent.tags shouldContain IntentTag.COMBAT_TRICK
            intent.speed shouldBe Speed.INSTANT
            withClue("`HoldPolicy` compares this against the damage already on the stack") {
                intent.pumpToughness shouldBe 3
            }
        }

        test("a permanent pump is not a combat trick and buys no trick toughness") {
            val intent = intentOf("Unholy Strength")
            intent.tags shouldContain IntentTag.PUMP
            intent.tags shouldNotContain IntentTag.COMBAT_TRICK
            withClue("an Aura's bonus does not expire, so it is not a response to a deadline") {
                intent.pumpToughness shouldBe 0
            }
        }

        test("a fight is removal whose reach is not on the card") {
            // `removalReach == null` otherwise means "destruction — toughness is no defence", which
            // is the opposite of what a fight is. `HoldPolicy` reads the tag to tell them apart.
            val intent = intentOf("Malamet Battle Glyph")
            intent.tags shouldContain IntentTag.REMOVAL
            intent.tags shouldContain IntentTag.FIGHT
            intent.removalReach shouldBe null
        }

        test("a sorcery pump is not a combat trick") {
            val intent = intentOf("Titanic Growth")
            intent.speed shouldBe Speed.INSTANT
        }

        test("a sweeper is tagged as one, and reads as opponent-facing") {
            val intent = intentOf("Wrath of God")
            intent.tags shouldContain IntentTag.REMOVAL
            intent.tags shouldContain IntentTag.SWEEPER
            intent.affectsOpponent shouldBe true
        }

        test("burn carries its reach") {
            intentOf("Lightning Bolt").removalReach shouldBe 3
        }

        test("a vanilla creature is uninterpretable, and keeps the historical flat prior") {
            val intent = intentOf("Grizzly Bears")
            intent.tags shouldBe emptySet()
            intent.staticPriorValue shouldBe CardIntent.UNKNOWN.staticPriorValue
        }

        test("a Room's rules text lives on its faces, and the analyzer reads it") {
            // Top-level script is empty for a split card (CR 709) — reading only that priced this
            // repeatable draw engine as a vanilla enchantment, and the AI never cast it.
            val intent = CardIntentAnalyzer.analyze(UnholyAnnexRitualChamber)
            intent.tags shouldContain IntentTag.DRAW
            intent.tags shouldContain IntentTag.TOKEN_MAKER
            intent.repeatable shouldBe true
            intent.staticPriorValue shouldBeGreaterThan 2.7
        }

        test("each Room half is valued on its own, so unlocking a door is worth something") {
            val annex = CardIntentAnalyzer.analyzeFace(UnholyAnnexRitualChamber, roomFace("Unholy Annex"))
            withClue("an end-step draw trigger that fires every turn") {
                annex.tags shouldContain IntentTag.DRAW
                annex.repeatable shouldBe true
                annex.staticPriorValue shouldBeGreaterThan 2.7
            }

            val chamber = CardIntentAnalyzer.analyzeFace(UnholyAnnexRitualChamber, roomFace("Ritual Chamber"))
            chamber.tags shouldContain IntentTag.TOKEN_MAKER
            withClue("'when you unlock this door' fires once for that door, like an ETB") {
                chamber.repeatable shouldBe false
                chamber.staticPriorValue shouldBe CardIntent.UNKNOWN.staticPriorValue
            }
        }

        test("the catalog resolves a face by name, and shrugs at a face that isn't there") {
            val catalog = IntentCatalog.of(cardRegistry.apply { register(UnholyAnnexRitualChamber) })
            catalog.forFace(UnholyAnnexRitualChamber.name, "Unholy Annex")!!.tags shouldContain IntentTag.DRAW
            catalog.forFace(UnholyAnnexRitualChamber.name, "Broom Closet") shouldBe null
            IntentCatalog.NONE.forFace(UnholyAnnexRitualChamber.name, "Unholy Annex") shouldBe null
        }

        test("a spent Adventure half does not inflate the permanent it left behind") {
            // Virtue of Loyalty is an enchantment whose Adventure (CR 715) makes a 2/2 Knight. The
            // Adventure resolves to exile — it is never text on the battlefield — so the
            // enchantment standing there must not be priced as a token engine.
            val wholeCard = CardIntentAnalyzer.analyze(VirtueOfLoyalty)
            withClue("casting the Adventure is a real option, so the card as a whole still shows it") {
                wholeCard.tags shouldContain IntentTag.TOKEN_MAKER
            }

            val asPermanent = CardIntentAnalyzer.analyzeSelf(VirtueOfLoyalty)
            asPermanent.tags shouldNotContain IntentTag.TOKEN_MAKER
            withClue("a repeatable token maker prices at 3.0; this end-step trigger is worth 1.5") {
                asPermanent.staticPriorValue shouldBeLessThan wholeCard.staticPriorValue
            }
        }

        test("a single-face card reads the same whether asked as a card or as a permanent") {
            val card = cardRegistry.requireCard("Icy Manipulator")
            CardIntentAnalyzer.analyzeSelf(card) shouldBe CardIntentAnalyzer.analyze(card)
        }

        test("a permanent is read from the faces in force on it, and a Room's are its open doors") {
            val catalog = IntentCatalog.of(
                cardRegistry.apply { register(UnholyAnnexRitualChamber); register(VirtueOfLoyalty) }
            )

            withClue("an ordinary permanent is one reading — its own, minus the Adventure") {
                val plain = catalog.forPermanent(ComponentContainer(), VirtueOfLoyalty.name)
                plain.map { it.tags } shouldBe listOf(CardIntentAnalyzer.analyzeSelf(VirtueOfLoyalty).tags)
            }

            withClue("a Room contributes one reading per unlocked door, and none for a locked one") {
                val oneDoor = catalog.forPermanent(
                    roomPermanent("Unholy Annex"), UnholyAnnexRitualChamber.name
                )
                oneDoor.single().tags shouldContain IntentTag.DRAW
                oneDoor.single().tags shouldNotContain IntentTag.TOKEN_MAKER

                catalog.forPermanent(
                    roomPermanent("Unholy Annex", "Ritual Chamber"), UnholyAnnexRitualChamber.name
                ) shouldHaveSize 2
            }

            withClue("both doors locked (CR 709.5d) is no text at all, not the whole card") {
                catalog.forPermanent(roomPermanent(), UnholyAnnexRitualChamber.name).shouldBeEmpty()
            }

            withClue("the off position answers nothing, so every caller keeps its old behaviour") {
                IntentCatalog.NONE.forPermanent(
                    roomPermanent("Unholy Annex"), UnholyAnnexRitualChamber.name
                ).shouldBeEmpty()
            }
        }

        test("the catalog is off by default and answers nothing") {
            IntentCatalog.NONE.isEnabled shouldBe false
            IntentCatalog.NONE.forName("Icy Manipulator") shouldBe null
        }

        test("an enabled catalog resolves by name and shrugs at a name it does not know") {
            val catalog = IntentCatalog.of(cardRegistry)
            catalog.isEnabled shouldBe true
            catalog.forName("Icy Manipulator")?.tags shouldBe intentOf("Icy Manipulator").tags
            catalog.forName("Definitely Not A Card") shouldBe null
        }
    }
}
