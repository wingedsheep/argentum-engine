package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Cabal Initiate — Modern Horizons 2 #78
 * {1}{B} · Creature — Human Warlock · 2 / 1
 *
 * Discard a card: This creature gains lifelink until end of turn.
 * Threshold — This creature gets +1/+2 as long as there are seven or more cards in your graveyard.
 *
 * "Threshold" is an ability word, not a keyword — there is no `Keyword.THRESHOLD`, and nothing in
 * the engine reads the word. It lives only in [oracleText]; the ability it labels is a plain
 * [ConditionalStaticAbility] whose condition is re-evaluated during layer projection, so the bonus
 * appears and disappears the instant the graveyard crosses seven cards (CR 702.21a — a static
 * ability, never a trigger).
 *
 * [GroupFilter.source] scopes the [ModifyStats] to this permanent only. The two abilities feed each
 * other: paying [Costs.DiscardCard] both buys lifelink and pushes the graveyard toward threshold.
 */
val CabalInitiate = card("Cabal Initiate") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Warlock"
    power = 2
    toughness = 1
    oracleText = "Discard a card: This creature gains lifelink until end of turn.\n" +
        "Threshold — This creature gets +1/+2 as long as there are seven or more cards in your graveyard."

    activatedAbility {
        cost = Costs.DiscardCard
        effect = Effects.GrantKeyword(Keyword.LIFELINK, EffectTarget.Self)
    }

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(powerBonus = 1, toughnessBonus = 2, filter = GroupFilter.source()),
            condition = Conditions.CardsInGraveyardAtLeast(7),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "78"
        artist = "Bastien L. Deharme"
        flavorText = "\"Hear our prayers, oh Shadowed One, and bless us in bloody agony.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/2/b279a03f-85ab-43f2-b5ca-1bc10563e5ad.jpg?1783926864"
    }
}
