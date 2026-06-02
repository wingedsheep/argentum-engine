package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Ainok Wayfarer — Tarkir: Dragonstorm #134
 * {1}{G} · Creature — Dog Scout · 1/1
 *
 * When this creature enters, mill three cards. You may put a land card from among
 * them into your hand. If you don't, put a +1/+1 counter on this creature.
 *
 * Modeled as the atomic gather → mill → (IfYouDo select-land-into-hand else
 * +1/+1 counter) pipeline:
 *   1. Gather the top three cards as "milled".
 *   2. Move all three into the graveyard — this is the actual mill (emits the mill /
 *      put-into-graveyard events the Mill keyword and graveyard triggers care about).
 *   3. IfYouDo: optionally select a land from among the milled three and put it into
 *      hand. Success is gated on the "chosen" collection becoming non-empty
 *      ([SuccessCriterion.CollectionNonEmpty]); if no land is taken (declined or none
 *      present), the "if you don't" branch adds a +1/+1 counter to this creature.
 */
val AinokWayfarer = card("Ainok Wayfarer") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Dog Scout"
    power = 1
    toughness = 1
    oracleText = "When this creature enters, mill three cards. You may put a land card from among them into your hand. If you don't, put a +1/+1 counter on this creature."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.Composite(
            listOf(
                // Mill three: gather the top three, then move them to the graveyard.
                GatherCardsEffect(
                    source = CardSource.TopOfLibrary(DynamicAmount.Fixed(3)),
                    storeAs = "milled"
                ),
                MoveCollectionEffect(
                    from = "milled",
                    destination = CardDestination.ToZone(Zone.GRAVEYARD)
                ),
                // You may put a land card from among them into your hand.
                // If you don't, put a +1/+1 counter on this creature.
                // Success is gated on whether a land actually reached your hand:
                // SuccessCriterion.Auto probes the terminal hand move's destination
                // zone, so the "if you don't" counter only fires when the player
                // declines or no land is among the milled cards. There is no "if you do"
                // payoff, so that branch is an empty composite.
                Effects.IfYouDo(
                    action = Effects.Composite(
                        listOf(
                            SelectFromCollectionEffect(
                                from = "milled",
                                selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                                filter = GameObjectFilter.Land,
                                storeSelected = "chosen",
                                storeRemainder = "leftInGraveyard",
                                selectedLabel = "Put in hand"
                            ),
                            MoveCollectionEffect(
                                from = "chosen",
                                destination = CardDestination.ToZone(Zone.HAND),
                                revealed = true
                            )
                        )
                    ),
                    ifYouDo = Effects.Composite(),
                    ifYouDont = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
                )
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "134"
        artist = "Filipe Pagliuso"
        flavorText = "\"There has to be a faster route through Sagu Jungle. Give me a week.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/7/57695a9b-8f72-4ccc-a946-5d5037b09b8f.jpg?1743204503"
    }
}
