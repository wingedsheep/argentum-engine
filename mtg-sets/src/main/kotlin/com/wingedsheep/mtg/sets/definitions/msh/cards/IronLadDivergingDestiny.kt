package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.LookAtTopOfLibrary
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Iron Lad, Diverging Destiny — Marvel Super Heroes #59
 * {2}{U} · Legendary Artifact Creature — Human Hero · 2/2
 *
 * Flying, vigilance
 * You may look at the top card of your library any time.
 * {T}: Reveal the top card of your library. If it's an artifact card, draw a card.
 *
 * "You may look at the top card of your library any time" is the filterless player-permission
 * static [LookAtTopOfLibrary] (Glarb, Calamity's Augur / Glowcap Lantern shape).
 *
 * The tap ability is a Gather → branch composition: [GatherCardsEffect] with `revealed = true`
 * shows the top card to every player *without moving it* (the pipeline gather is
 * non-destructive — nothing is put anywhere unless a later Move step says so), then
 * [Conditions.CollectionContainsMatch] on [GameObjectFilter.Artifact] gates the draw. Because the
 * card stays on top, a successful reveal draws that same card. An empty library reveals nothing
 * and the condition is false, so no card is drawn.
 */
val IronLadDivergingDestiny = card("Iron Lad, Diverging Destiny") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Artifact Creature — Human Hero"
    power = 2
    toughness = 2
    oracleText = "Flying, vigilance\n" +
        "You may look at the top card of your library any time.\n" +
        "{T}: Reveal the top card of your library. If it's an artifact card, draw a card."

    keywords(Keyword.FLYING, Keyword.VIGILANCE)

    staticAbility {
        ability = LookAtTopOfLibrary
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.Composite(
            listOf(
                GatherCardsEffect(
                    source = CardSource.TopOfLibrary(DynamicAmount.Fixed(1)),
                    storeAs = "ironLadRevealed",
                    revealed = true,
                ),
                ConditionalEffect(
                    condition = Conditions.CollectionContainsMatch(
                        "ironLadRevealed",
                        GameObjectFilter.Artifact,
                    ),
                    effect = Effects.DrawCards(1),
                ),
            )
        )
        description = "{T}: Reveal the top card of your library. If it's an artifact card, draw a card."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "59"
        artist = "Erikas Perl"
        flavorText = "\"If you had a chance to change your fate, would you take it?\""
        imageUri = "https://cards.scryfall.io/normal/front/3/5/355e7197-2f20-43b6-9305-73c4e1fd4a3c.jpg?1783902957"
    }
}
