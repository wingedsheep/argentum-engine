package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.CollectionContainsMatch
import com.wingedsheep.sdk.scripting.effects.CaptureControllersEffect
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.ConditionalOnCollectionEffect
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.ForEachCapturedControllerEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.effects.RevealCollectionEffect
import com.wingedsheep.sdk.scripting.effects.RevealHandEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.ShuffleLibraryEffect
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.serialization.CardSerialization
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * The `pipeline { }` builder must compile to the *exact* `CompositeEffect` tree the
 * raw step constructors produce by hand — same types, same keys, same JSON — so the
 * engine, the linter, and the snapshot goldens can't tell the difference between a
 * hand-built pipeline and a builder-built one (`backlog/inline-pipeline-dsl.md` §2).
 */
class PipelineBuilderTest : DescribeSpec({

    fun jsonOf(effect: Effect): String =
        CardSerialization.compactJson.encodeToString(Effect.serializer(), effect)

    describe("byte-identical serialization against hand-built trees") {

        it("reproduces Lobotomy's hand-built pipeline exactly via name= overrides") {
            // The hand-built tree from mtg-sets inv/cards/Lobotomy.kt, verbatim.
            val handBuilt = CompositeEffect(
                listOf(
                    RevealHandEffect(EffectTarget.ContextTarget(0)),
                    GatherCardsEffect(
                        source = CardSource.FromZone(Zone.HAND, Player.ContextPlayer(0)),
                        storeAs = "hand"
                    ),
                    SelectFromCollectionEffect(
                        from = "hand",
                        selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),
                        chooser = Chooser.Controller,
                        filter = GameObjectFilter(cardPredicates = listOf(CardPredicate.Not(CardPredicate.IsBasicLand))),
                        storeSelected = "chosen",
                        prompt = "Choose a card other than a basic land card",
                        alwaysPrompt = true,
                        showAllCards = true
                    ),
                    Effects.StoreCardName(from = "chosen", storeAs = "chosenName"),
                    GatherCardsEffect(
                        source = CardSource.FromMultipleZones(
                            zones = listOf(Zone.GRAVEYARD, Zone.HAND, Zone.LIBRARY),
                            player = Player.ContextPlayer(0),
                            filter = GameObjectFilter.Any.namedFromVariable("chosenName")
                        ),
                        storeAs = "toExile"
                    ),
                    MoveCollectionEffect(
                        from = "toExile",
                        destination = CardDestination.ToZone(Zone.EXILE, Player.ContextPlayer(0))
                    ),
                    ShuffleLibraryEffect(target = EffectTarget.ContextTarget(0))
                )
            )

            val built = Effects.Pipeline {
                run(RevealHandEffect(EffectTarget.ContextTarget(0)))
                val hand = gather(CardSource.FromZone(Zone.HAND, Player.ContextPlayer(0)), name = "hand")
                val chosen = chooseExactly(
                    1, from = hand,
                    filter = GameObjectFilter(cardPredicates = listOf(CardPredicate.Not(CardPredicate.IsBasicLand))),
                    prompt = "Choose a card other than a basic land card",
                    alwaysPrompt = true,
                    showAllCards = true,
                    name = "chosen"
                )
                val chosenName = storeCardName(chosen, name = "chosenName")
                val toExile = gather(
                    CardSource.FromMultipleZones(
                        zones = listOf(Zone.GRAVEYARD, Zone.HAND, Zone.LIBRARY),
                        player = Player.ContextPlayer(0),
                        filter = GameObjectFilter.Any.namedFromVariable(chosenName)
                    ),
                    name = "toExile"
                )
                exile(toExile, owner = Player.ContextPlayer(0))
                run(ShuffleLibraryEffect(target = EffectTarget.ContextTarget(0)))
            }

            built shouldBe handBuilt
            jsonOf(built) shouldBe jsonOf(handBuilt)
        }

        it("reproduces the Ancestral Memories shape (look at 7, keep 2, rest to graveyard)") {
            val handBuilt = CompositeEffect(
                listOf(
                    GatherCardsEffect(
                        source = CardSource.TopOfLibrary(DynamicAmount.Fixed(7)),
                        storeAs = "looked"
                    ),
                    SelectFromCollectionEffect(
                        from = "looked",
                        selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(2)),
                        storeSelected = "kept",
                        storeRemainder = "remainder"
                    ),
                    MoveCollectionEffect(from = "kept", destination = CardDestination.ToZone(Zone.HAND)),
                    MoveCollectionEffect(from = "remainder", destination = CardDestination.ToZone(Zone.GRAVEYARD))
                )
            )

            val built = Effects.Pipeline {
                val looked = gather(CardSource.TopOfLibrary(DynamicAmount.Fixed(7)), name = "looked")
                val (kept, rest) = chooseExactlySplit(2, from = looked, name = "kept", remainderName = "remainder")
                toHand(kept)
                toGraveyard(rest)
            }

            built shouldBe handBuilt
            jsonOf(built) shouldBe jsonOf(handBuilt)
        }

        it("reproduces the branch-on-gathered idiom (draw-reveal, creature to hand, else graveyard)") {
            val handBuilt = CompositeEffect(
                listOf(
                    GatherCardsEffect(source = CardSource.TopOfLibrary(DynamicAmount.Fixed(1)), storeAs = "drawn"),
                    RevealCollectionEffect(from = "drawn"),
                    ConditionalOnCollectionEffect(
                        collection = "drawn",
                        filter = GameObjectFilter.Creature,
                        ifNotEmpty = MoveCollectionEffect(from = "drawn", destination = CardDestination.ToZone(Zone.HAND)),
                        ifEmpty = MoveCollectionEffect(from = "drawn", destination = CardDestination.ToZone(Zone.GRAVEYARD))
                    )
                )
            )

            val built = Effects.Pipeline {
                val drawn = gather(CardSource.TopOfLibrary(DynamicAmount.Fixed(1)), name = "drawn")
                reveal(drawn)
                ifNotEmpty(drawn, filter = GameObjectFilter.Creature) {
                    toHand(drawn)
                } orElse {
                    toGraveyard(drawn)
                }
            }

            built shouldBe handBuilt
            jsonOf(built) shouldBe jsonOf(handBuilt)
        }
    }

    describe("deterministic auto-generated keys") {

        it("keys are verb-prefixed and positional per builder instance") {
            val built = Effects.Pipeline {
                val tied = gather(GameObjectFilter.Creature)
                val pick = chooseExactly(1, from = tied, useTargetingUI = true)
                destroy(pick, noRegenerate = true)
            } as CompositeEffect

            val gatherStep = built.effects[0].shouldBeInstanceOf<GatherCardsEffect>()
            gatherStep.storeAs shouldBe "gathered0"
            gatherStep.source shouldBe CardSource.BattlefieldMatching(filter = GameObjectFilter.Creature)

            val selectStep = built.effects[1].shouldBeInstanceOf<SelectFromCollectionEffect>()
            selectStep.from shouldBe "gathered0"
            selectStep.storeSelected shouldBe "selected1"
            selectStep.storeRemainder shouldBe null
            selectStep.useTargetingUI shouldBe true

            val moveStep = built.effects[2].shouldBeInstanceOf<MoveCollectionEffect>()
            moveStep.from shouldBe "selected1"
            moveStep.moveType shouldBe MoveType.Destroy
            moveStep.noRegenerate shouldBe true
            moveStep.destination shouldBe CardDestination.ToZone(Zone.GRAVEYARD)
        }

        it("renaming the Kotlin val does not change the serialized tree") {
            fun build(useTargetingUI: Boolean) = Effects.Pipeline {
                val someName = gather(GameObjectFilter.Creature)
                val otherName = chooseExactly(1, from = someName, useTargetingUI = useTargetingUI)
                destroy(otherName)
            }
            // Same steps, differently named vals → identical JSON.
            jsonOf(build(true)) shouldBe jsonOf(Effects.Pipeline {
                val a = gather(GameObjectFilter.Creature)
                val b = chooseExactly(1, from = a, useTargetingUI = true)
                destroy(b)
            })
        }

        it("nested branch scopes share the key counter, so keys never collide") {
            val built = Effects.Pipeline {
                val drawn = gather(CardSource.TopOfLibrary(DynamicAmount.Fixed(3)))   // step 0
                ifNotEmpty(drawn) {                                                   // step 1
                    val inner = chooseExactly(1, from = drawn)                        // step 2
                    toHand(inner)                                                     // step 3
                }
                val after = gather(CardSource.TopOfLibrary(DynamicAmount.Fixed(1)))   // step 4
                toGraveyard(after)
            } as CompositeEffect

            val conditional = built.effects[1].shouldBeInstanceOf<ConditionalOnCollectionEffect>()
            val innerSelect = (conditional.ifNotEmpty as CompositeEffect)
                .effects[0].shouldBeInstanceOf<SelectFromCollectionEffect>()
            innerSelect.storeSelected shouldBe "selected2"
            (built.effects[2] as GatherCardsEffect).storeAs shouldBe "gathered4"
        }
    }

    describe("authoring-error prevention") {

        it("rejects duplicate explicit slot names") {
            shouldThrow<IllegalArgumentException> {
                Effects.Pipeline {
                    gather(GameObjectFilter.Creature, name = "pile")
                    gather(GameObjectFilter.Land, name = "pile")
                }
            }
        }

        it("rejects an empty pipeline") {
            shouldThrow<IllegalArgumentException> { Effects.Pipeline { } }
        }

        it("rejects an empty branch block") {
            shouldThrow<IllegalArgumentException> {
                Effects.Pipeline {
                    val drawn = gather(CardSource.TopOfLibrary(DynamicAmount.Fixed(1)))
                    ifNotEmpty(drawn) { }
                }
            }
        }

        it("rejects chaining orElse twice") {
            shouldThrow<IllegalArgumentException> {
                Effects.Pipeline {
                    val drawn = gather(CardSource.TopOfLibrary(DynamicAmount.Fixed(1)))
                    val handle = ifNotEmpty(drawn) { toHand(drawn) }
                    handle orElse { toGraveyard(drawn) }
                    handle orElse { toGraveyard(drawn) }
                }
            }
        }
    }

    describe("secondary outputs are only serialized when requested") {

        it("chooseExactly leaves storeRemainder null; chooseExactlySplit sets it") {
            val withoutRemainder = Effects.Pipeline {
                val pile = gather(CardSource.TopOfLibrary(DynamicAmount.Fixed(3)))
                toHand(chooseExactly(1, from = pile))
            } as CompositeEffect
            (withoutRemainder.effects[1] as SelectFromCollectionEffect).storeRemainder shouldBe null

            val withRemainder = Effects.Pipeline {
                val pile = gather(CardSource.TopOfLibrary(DynamicAmount.Fixed(3)))
                val (kept, rest) = chooseExactlySplit(1, from = pile)
                toHand(kept)
                toGraveyard(rest)
            } as CompositeEffect
            val select = withRemainder.effects[1] as SelectFromCollectionEffect
            select.storeSelected shouldBe "selected1"
            select.storeRemainder shouldBe "remainder1"
        }
    }

    describe("cross-namespace handles and conditions") {

        it("NumberSlot.amount reads back as a VariableReference") {
            val built = Effects.Pipeline {
                val count = storeNumber(DynamicAmount.Fixed(3), name = "tally")
                run(DealDamageEffect(count.amount, EffectTarget.ContextTarget(0)))
            } as CompositeEffect
            val damage = built.effects[1].shouldBeInstanceOf<DealDamageEffect>()
            damage.amount shouldBe DynamicAmount.VariableReference("tally")
        }

        it("whenMatches builds a CollectionContainsMatch over the slot's key") {
            var condition: CollectionContainsMatch? = null
            Effects.Pipeline {
                val drawn = gather(CardSource.TopOfLibrary(DynamicAmount.Fixed(1)))
                condition = whenMatches(drawn, GameObjectFilter.Creature) as CollectionContainsMatch
                toHand(drawn)
            }
            condition shouldBe CollectionContainsMatch("gathered0", GameObjectFilter.Creature)
        }

        it("forEachCaptured wires the three collections and exposes the per-controller tally") {
            val built = Effects.Pipeline {
                val targets = gather(CardSource.ChosenTargets, name = "targets")
                val controllers = captureControllers(targets, name = "owners")
                val dead = moveTracked(
                    targets, CardDestination.ToZone(Zone.GRAVEYARD),
                    moveType = MoveType.Destroy, name = "destroyed"
                )
                forEachCaptured(dead, original = targets, controllers = controllers, countName = "perPlayer") { count ->
                    run(DealDamageEffect(count.amount, EffectTarget.Controller))
                }
            } as CompositeEffect

            (built.effects[1] as CaptureControllersEffect).storeAs shouldBe "owners"
            val forEach = built.effects[3].shouldBeInstanceOf<ForEachCapturedControllerEffect>()
            forEach.collection shouldBe "destroyed"
            forEach.originalCollection shouldBe "targets"
            forEach.controllerSnapshot shouldBe "owners"
            forEach.countVariable shouldBe "perPlayer"
            val inner = forEach.effects.single().shouldBeInstanceOf<DealDamageEffect>()
            inner.amount shouldBe DynamicAmount.VariableReference("perPlayer")
        }
    }
})
