package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain

/**
 * Pipeline effects wire their steps together with named-collection keys (`storeAs` / `from`).
 * Those keys are internal plumbing and must never reach the player-facing `description` —
 * which, for instants/sorceries, is the text shown on the stack. Regression for issue #710,
 * where Wash Out rendered "Put the washOut_gathered cards a hand".
 */
class PipelineDescriptionLeakTest : DescribeSpec({

    describe("pipeline-collection keys never leak into effect descriptions") {

        it("Wash Out's gather→bounce pipeline reads naturally (issue #710)") {
            val effect = ChooseColorThenEffect(
                then = CompositeEffect(
                    listOf(
                        GatherCardsEffect(
                            source = CardSource.BattlefieldMatching(
                                filter = GameObjectFilter(
                                    cardPredicates = listOf(CardPredicate.HasChosenColor),
                                ),
                                player = Player.Each,
                            ),
                            storeAs = "washOut_gathered",
                        ),
                        MoveCollectionEffect(
                            from = "washOut_gathered",
                            destination = CardDestination.ToZone(Zone.HAND),
                        ),
                    ),
                ),
            )

            effect.description shouldNotContain "washOut_gathered"
            effect.description shouldBe
                "Choose a color. Look at all of the chosen color permanents on the battlefield. " +
                "Put those cards into a hand"
        }

        it("MoveCollection picks the right preposition per destination") {
            MoveCollectionEffect("x", CardDestination.ToZone(Zone.HAND)).description shouldBe
                "Put those cards into a hand"
            MoveCollectionEffect("x", CardDestination.ToZone(Zone.BATTLEFIELD)).description shouldBe
                "Put those cards onto the battlefield"
            MoveCollectionEffect("x", CardDestination.ToZone(Zone.EXILE)).description shouldBe
                "Put those cards into exile"
        }

        it("collection-referencing effects don't echo their key") {
            val key = "secret_internal_key"
            listOf(
                MoveCollectionEffect(key, CardDestination.ToZone(Zone.GRAVEYARD)).description,
                GatherSubtypesEffect(key, "out").description,
                CaptureControllersEffect(key, "out").description,
                FilterCollectionEffect(key, CollectionFilter.MatchesFilter(GameObjectFilter.Any), "out").description,
                StoreCardNameEffect(key).description,
            ).forEach { it shouldNotContain key }
        }
    }
})
