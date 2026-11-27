package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DonorCards
import com.wingedsheep.sdk.scripting.HasAllActivatedAbilitiesOfCards
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Dark Impostor
 * {2}{B}
 * Creature — Vampire Assassin
 * 2/2
 *
 * {4}{B}{B}: Exile target creature and put a +1/+1 counter on this creature.
 * This creature has all activated abilities of all creature cards exiled with it.
 *
 * The Territory Forge / Patchwork Crawler shape: the activated ability files the exiled card in this
 * creature's linked-exile pile ([Effects.ExileLinkedToSource]), and [HasAllActivatedAbilitiesOfCards]
 * over `LINKED_EXILE` surfaces the pile's activated abilities on Dark Impostor. Unlike the Crawler,
 * the target is a creature *permanent*, so the exiled card need not be a creature card once in exile
 * (an animated land, say) — `cardFilter = Filters.Creature` keeps the grant to creature cards only.
 */
val DarkImpostor = card("Dark Impostor") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire Assassin"
    power = 2
    toughness = 2
    oracleText = "{4}{B}{B}: Exile target creature and put a +1/+1 counter on this creature.\n" +
        "This creature has all activated abilities of all creature cards exiled with it."

    activatedAbility {
        cost = Costs.Mana("{4}{B}{B}")
        val exiled = target(TargetFilter.Creature)
        effect = Effects.ExileLinkedToSource(exiled) then
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        description = "Exile target creature and put a +1/+1 counter on Dark Impostor."
    }

    staticAbility {
        ability = HasAllActivatedAbilitiesOfCards(
            donors = DonorCards.LINKED_EXILE,
            cardFilter = Filters.Creature,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "92"
        artist = "Johannes Voss"
        imageUri = "https://cards.scryfall.io/normal/front/8/f/8f5e8815-cda8-407d-847c-968b72c061e8.jpg?1783940704"
    }
}
