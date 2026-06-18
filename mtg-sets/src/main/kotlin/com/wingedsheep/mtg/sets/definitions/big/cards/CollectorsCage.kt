package com.wingedsheep.mtg.sets.definitions.big.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.conditions.Compare
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.GrantMayPlayFromExileEffect
import com.wingedsheep.sdk.scripting.effects.GrantPlayWithoutPayingCostEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.CardNumericProperty
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Collector's Cage
 * {1}{W}
 * Artifact
 *
 * Hideaway 5 (When this artifact enters, look at the top five cards of your library, exile one
 * face down, then put the rest on the bottom in a random order.)
 * {1}, {T}: Put a +1/+1 counter on target creature you control. Then if you control three or more
 * creatures with different powers, you may play the exiled card without paying its mana cost.
 *
 * Hideaway 5 is composed (per the reminder text) as an ETB gather-top-5 → exile-one-face-down
 * (linked to this artifact) → bottom-randomize-the-rest pipeline, the same shape as
 * [com.wingedsheep.mtg.sets.definitions.lrw.cards.MosswortBridge]. Unlike Mosswort Bridge, the
 * play-exiled clause here is *mid-effect*, not an activation gate: the +1/+1 counter happens
 * unconditionally, then a [ConditionalEffect] tests "three or more creatures with different
 * powers" ([DynamicAmounts.distinctValues] over [CardNumericProperty.POWER] ≥ 3, two creatures
 * sharing a power counting once) and, if met, offers a [MayEffect] that grants may-play +
 * play-without-paying-cost over the hidden-away card.
 */
val CollectorsCage = card("Collector's Cage") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Artifact"
    oracleText = "Hideaway 5 (When this artifact enters, look at the top five cards of your " +
        "library, exile one face down, then put the rest on the bottom in a random order.)\n" +
        "{1}, {T}: Put a +1/+1 counter on target creature you control. Then if you control three " +
        "or more creatures with different powers, you may play the exiled card without paying its " +
        "mana cost."

    keywordAbility(KeywordAbility.hideaway(5))

    // Hideaway 5 — ETB look at top 5, exile one face down linked to this artifact, bottom-randomize rest.
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.Composite(
            listOf(
                GatherCardsEffect(
                    source = CardSource.TopOfLibrary(
                        count = DynamicAmount.Fixed(5),
                        player = Player.You
                    ),
                    storeAs = "hideawayTop"
                ),
                SelectFromCollectionEffect(
                    from = "hideawayTop",
                    selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),
                    storeSelected = "hideawayPicked",
                    storeRemainder = "hideawayRest",
                    prompt = "Choose a card to exile face down",
                    selectedLabel = "Exile face down",
                    remainderLabel = "Put on bottom of library"
                ),
                MoveCollectionEffect(
                    from = "hideawayPicked",
                    destination = CardDestination.ToZone(Zone.EXILE),
                    faceDown = true,
                    linkToSource = true
                ),
                MoveCollectionEffect(
                    from = "hideawayRest",
                    destination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Bottom),
                    order = CardOrder.Random
                )
            )
        )
    }

    // {1}, {T}: +1/+1 on target creature you control; then, if you control 3+ creatures with
    // different powers, you may play the exiled card for free.
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap)
        val target = target("target creature you control", Targets.CreatureYouControl)
        effect = Effects.Composite(
            listOf(
                Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, target),
                ConditionalEffect(
                    condition = Compare(
                        DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature)
                            .distinctValues(CardNumericProperty.POWER),
                        ComparisonOperator.GTE,
                        DynamicAmount.Fixed(3)
                    ),
                    effect = MayEffect(
                        Effects.Composite(
                            listOf(
                                GatherCardsEffect(
                                    source = CardSource.FromLinkedExile(),
                                    storeAs = "hideawayLinked"
                                ),
                                GrantMayPlayFromExileEffect("hideawayLinked"),
                                GrantPlayWithoutPayingCostEffect("hideawayLinked")
                            )
                        ),
                        descriptionOverride = "Play the exiled card without paying its mana cost"
                    )
                )
            )
        )
        description = "Put a +1/+1 counter on target creature you control. Then if you control " +
            "three or more creatures with different powers, you may play the exiled card without " +
            "paying its mana cost."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "1"
        artist = "Bartek Fedyczak"
        imageUri = "https://cards.scryfall.io/normal/front/a/3/a33703bb-51c0-4d57-9d06-1148507ddc4f.jpg?1739804143"
    }
}
