package com.wingedsheep.mtg.sets.definitions.mh1.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Urza, Lord High Artificer — Modern Horizons #75
 * {2}{U}{U} · Legendary Creature — Human Artificer 1/4
 *
 * When Urza enters, create a 0/0 colorless Construct artifact creature token with "This token gets
 * +1/+1 for each artifact you control."
 * Tap an untapped artifact you control: Add {U}.
 * {5}: Shuffle your library, then exile the top card. Until end of turn, you may play that card
 * without paying its mana cost.
 *
 * The Construct is Karn, Scion of Urza's token (a self-scoped dynamic-stats static that counts
 * itself). The mana ability taps an artifact as a cost rather than with {T}, so summoning sickness
 * doesn't matter (ruling). The last ability is Mind's Desire's shuffle-exile-play-free pattern.
 */
val UrzaLordHighArtificer = card("Urza, Lord High Artificer") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Human Artificer"
    power = 1
    toughness = 4
    oracleText = "When Urza enters, create a 0/0 colorless Construct artifact creature token with " +
        "\"This token gets +1/+1 for each artifact you control.\"\n" +
        "Tap an untapped artifact you control: Add {U}.\n" +
        "{5}: Shuffle your library, then exile the top card. Until end of turn, you may play that " +
        "card without paying its mana cost."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            power = 0,
            toughness = 0,
            colors = emptySet(),
            creatureTypes = setOf("Construct"),
            artifactToken = true,
            staticAbilities = listOf(
                GrantDynamicStats(
                    filter = GroupFilter.source(),
                    powerBonus = DynamicAmounts.battlefield(Player.You, GameObjectFilter.Artifact).count(),
                    toughnessBonus = DynamicAmounts.battlefield(Player.You, GameObjectFilter.Artifact).count()
                )
            ),
            imageUri = "https://cards.scryfall.io/normal/front/8/5/85f212cd-4fc6-42fe-b268-22d8e3b2b7eb.jpg?1783933219"
        )
        description = "When Urza enters, create a 0/0 colorless Construct artifact creature token with " +
            "\"This token gets +1/+1 for each artifact you control.\""
    }

    activatedAbility {
        cost = Costs.TapPermanents(1, GameObjectFilter.Artifact)
        effect = Effects.AddMana(Color.BLUE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Mana("{5}")
        effect = Patterns.Exile.shuffleAndExileTopPlayFree()
        description = "Shuffle your library, then exile the top card. Until end of turn, you may play " +
            "that card without paying its mana cost."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "75"
        artist = "Grzegorz Rutkowski"
        imageUri = "https://cards.scryfall.io/normal/front/9/e/9e7fb3c0-5159-4d1f-8490-ce4c9a60f567.jpg?1783933135"
        ruling("2022-12-08", "The token created by Urza's first ability will count itself, so it'll be at least 1/1.")
        ruling("2022-12-08", "You can tap any untapped artifact you control to pay the cost of the mana ability, including an artifact creature you haven't controlled continuously since the beginning of your most recent turn. Tapping an Equipment this way won't affect its abilities or the equipped creature.")
        ruling("2022-12-08", "If you don't play the card exiled with Urza's last ability, it remains in exile.")
        ruling("2022-12-08", "If a spell has {X} in its mana cost, you must choose 0 as the value of X when casting it without paying its mana cost.")
    }
}
