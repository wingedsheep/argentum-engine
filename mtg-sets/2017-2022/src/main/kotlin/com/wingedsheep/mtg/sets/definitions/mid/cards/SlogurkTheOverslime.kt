package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Slogurk, the Overslime (Innistrad: Midnight Hunt #242)
 * {1}{G}{U}
 * Legendary Creature — Ooze
 * 3/3
 * Trample
 * Whenever a land card is put into your graveyard from anywhere, put a +1/+1 counter on Slogurk.
 * Remove three +1/+1 counters from Slogurk: Return it to its owner's hand.
 * When Slogurk leaves the battlefield, return up to three target land cards from your graveyard to
 * your hand.
 *
 * The counter trigger fires once per land card (not the batched "one or more" of The Gitrog
 * Monster), so it is a per-object zone-change trigger to the graveyard — nontoken, since a land
 * token is not a "land card", and owned by you, since it's "your graveyard".
 */
val SlogurkTheOverslime = card("Slogurk, the Overslime") {
    manaCost = "{1}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Legendary Creature — Ooze"
    oracleText = "Trample\n" +
        "Whenever a land card is put into your graveyard from anywhere, put a +1/+1 counter on Slogurk.\n" +
        "Remove three +1/+1 counters from Slogurk: Return it to its owner's hand.\n" +
        "When Slogurk leaves the battlefield, return up to three target land cards from your graveyard to your hand."
    power = 3
    toughness = 3

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land.ownedByYou().nontoken()).changesZone(to = Zone.GRAVEYARD)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    activatedAbility {
        cost = Costs.RemoveCounterFromSelf(CounterType.PLUS_ONE_PLUS_ONE, 3)
        effect = Effects.ReturnToHand(EffectTarget.Self)
    }

    triggeredAbility {
        trigger = Triggers.self.leaves()
        targets(
            TargetFilter(GameObjectFilter.Land.ownedByYou(), zone = Zone.GRAVEYARD),
            count = 3,
            optional = true,
        )
        effect = Effects.Pipeline {
            val lands = gather(CardSource.ChosenTargets)
            toHand(lands)
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "242"
        artist = "Nicholas Gregory"
        imageUri = "https://cards.scryfall.io/normal/front/f/9/f94e888b-6eeb-4ef3-ab21-5ed2bf0036a3.jpg?1783925554"
    }
}
