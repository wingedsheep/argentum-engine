package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.unaryMinus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AdditionalTokenScale
import com.wingedsheep.sdk.scripting.CreateAdditionalToken
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Chatterfang, Squirrel General — Modern Horizons 2 #151
 * {2}{G} · Legendary Creature — Squirrel Warrior · 3/3
 *
 * Forestwalk
 * If one or more tokens would be created under your control, those tokens plus that many 1/1 green
 * Squirrel creature tokens are created instead.
 * {B}, Sacrifice X Squirrels: Target creature gets +X/-X until end of turn.
 *
 * The replacement is [CreateAdditionalToken] scaled [AdditionalTokenScale.PER_MATCHING_TOKEN]: one
 * Squirrel for every token of the batch. Per the rulings it doesn't matter who creates the tokens,
 * only that they're created under your control (the default `appliesTo`), and the Squirrels share
 * the original creation's "tapped" rider but none of the original tokens' abilities.
 *
 * "Sacrifice X Squirrels" is a variable-count sacrifice whose count is the ability's X; X may be 0.
 */
val ChatterfangSquirrelGeneral = card("Chatterfang, Squirrel General") {
    manaCost = "{2}{G}"
    colorIdentity = "BG"
    typeLine = "Legendary Creature — Squirrel Warrior"
    power = 3
    toughness = 3
    oracleText = "Forestwalk (This creature can't be blocked as long as defending player controls a Forest.)\n" +
        "If one or more tokens would be created under your control, those tokens plus that many 1/1 green Squirrel creature tokens are created instead.\n" +
        "{B}, Sacrifice X Squirrels: Target creature gets +X/-X until end of turn."

    keywords(Keyword.FORESTWALK)

    replacementEffect(
        CreateAdditionalToken(
            additionalTokenType = "Squirrel",
            inheritTapped = true,
            scale = AdditionalTokenScale.PER_MATCHING_TOKEN
        )
    )

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{B}"),
            Costs.SacrificePermanents(GameObjectFilter.Creature.withSubtype("Squirrel"), minCount = 0)
        )
        val creature = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(DynamicAmounts.xValue(), -DynamicAmounts.xValue(), creature)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "151"
        artist = "Jason A. Engle"
        imageUri = "https://cards.scryfall.io/normal/front/1/7/1785cf85-1ac0-4246-9b89-1a8221a8e1b2.jpg?1783926835"
    }
}
