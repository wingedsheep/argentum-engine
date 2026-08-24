package com.wingedsheep.mtg.sets.definitions.drk.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Necropolis
 * {5}
 * Artifact Creature — Wall
 * 0/1
 * Defender
 * Exile a creature card from your graveyard: Put X +0/+1 counters on this creature, where X is
 * the exiled card's mana value.
 *
 * The exile is a **cost** (CR 601.2h — paid on activation), so by the time the ability resolves the
 * card is already in exile and can't be read off the graveyard. `CardSource.ExiledAsCost` names
 * exactly the cards *this activation's* payment exiled; gathering them and summing with
 * `ManaValueSumOfCollection` gives X, and since the cost exiles exactly one card the sum *is* that
 * card's mana value.
 *
 * Modelled this way rather than with a graveyard *target*: the card says "Exile a creature card
 * from your graveyard:" before the colon, so there is nothing to target and nothing to fizzle.
 */
val Necropolis = card("Necropolis") {
    manaCost = "{5}"
    typeLine = "Artifact Creature — Wall"
    power = 0
    toughness = 1
    oracleText = "Defender (This creature can't attack.)\n" +
        "Exile a creature card from your graveyard: Put X +0/+1 counters on this creature, " +
        "where X is the exiled card's mana value."

    keywords(Keyword.DEFENDER)

    activatedAbility {
        cost = Costs.ExileFromGraveyard(1, GameObjectFilter.Creature)
        effect = Effects.Composite(
            GatherCardsEffect(source = CardSource.ExiledAsCost, storeAs = "necropolisFuel"),
            Effects.AddDynamicCounters(
                Counters.PLUS_ZERO_PLUS_ONE,
                DynamicAmount.ManaValueSumOfCollection("necropolisFuel"),
                EffectTarget.Self,
            ),
        )
        description = "Exile a creature card from your graveyard: Put X +0/+1 counters on this " +
            "creature, where X is the exiled card's mana value."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "105"
        artist = "NéNé Thomas"
        imageUri = "https://cards.scryfall.io/normal/front/8/9/893e8e9c-983e-4db1-8d93-10637025a559.jpg?1783947925"
    }
}
