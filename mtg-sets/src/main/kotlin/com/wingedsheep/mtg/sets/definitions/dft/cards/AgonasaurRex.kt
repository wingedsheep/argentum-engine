package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Agonasaur Rex — Aetherdrift #151
 * {3}{G}{G} · Creature — Dinosaur · 8/8
 *
 * Trample
 * Cycling {2}{G}
 * When you cycle this card, put two +1/+1 counters on up to one target creature or Vehicle. It
 * gains trample and indestructible until end of turn.
 *
 * The cycle payoff is a separate ability from cycling itself (CR 702.29 rulings): it's legal to
 * cycle with no creature or Vehicle on the battlefield, because the target is "up to one"
 * (`optional = true`) and countering one ability leaves the other to resolve. The trample and
 * indestructible grants ride the same chosen target and take [Effects.GrantKeyword]'s default
 * end-of-turn duration; the counters, being counters, stay.
 */
val AgonasaurRex = card("Agonasaur Rex") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Dinosaur"
    power = 8
    toughness = 8
    oracleText = "Trample\n" +
        "Cycling {2}{G} ({2}{G}, Discard this card: Draw a card.)\n" +
        "When you cycle this card, put two +1/+1 counters on up to one target creature or Vehicle. " +
        "It gains trample and indestructible until end of turn."

    keywords(Keyword.TRAMPLE)

    keywordAbility(KeywordAbility.cycling("{2}{G}"))

    triggeredAbility {
        trigger = Triggers.YouCycleThis
        val t = target(
            "up to one target creature or Vehicle",
            TargetPermanent(optional = true, filter = TargetFilter(GameObjectFilter.CreatureOrVehicle))
        )
        effect = Effects.Composite(
            Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 2, t),
            Effects.GrantKeyword(Keyword.TRAMPLE, t),
            Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, t),
        )
        description = "When you cycle this card, put two +1/+1 counters on up to one target " +
            "creature or Vehicle. It gains trample and indestructible until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "151"
        artist = "Lucas Graciano"
        imageUri = "https://cards.scryfall.io/normal/front/5/b/5b11ec26-9e07-45e1-bcaa-5d44d1231586.jpg?1783907875"
    }
}
