package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Release the Gremlins
 * {X}{X}{R}
 * Sorcery
 * Destroy X target artifacts. Create X 2/2 red Gremlin creature tokens.
 *
 * Exactly X targets (By Force's shape); the token count is X itself, not the number destroyed —
 * an indestructible or illegal-but-not-all target still yields X tokens (2017-02-09 rulings), and
 * the caster creates every token. If every target turns illegal the spell doesn't resolve.
 */
val ReleaseTheGremlins = card("Release the Gremlins") {
    manaCost = "{X}{X}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Destroy X target artifacts. Create X 2/2 red Gremlin creature tokens."

    spell {
        targets(TargetFilter.Artifact, exactly = DynamicAmounts.xValue())
        effect = Effects.ForEachTarget(Effects.Destroy(EffectTarget.ContextTarget(0))) then
            Effects.CreateToken(
                count = DynamicAmounts.xValue(),
                power = 2,
                toughness = 2,
                colors = setOf(Color.RED),
                creatureTypes = setOf("Gremlin"),
            )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "96"
        artist = "Izzy"
        flavorText = "\"Being an inventor means knowing how to use the tools at hand to make something great—or to bring it down.\"\n—Pia Nalaar"
        imageUri = "https://cards.scryfall.io/normal/front/d/4/d4ba75be-2428-45aa-bf02-75c379a5dfa2.jpg?1783936750"
        ruling("2017-02-09", "The value of X is limited by the number of artifacts that you can target. You can't target the same artifact more than once.")
        ruling("2017-02-09", "You can target an artifact with indestructible. It won't be destroyed, and you'll still create X Gremlin tokens.")
        ruling("2017-02-09", "If an artifact becomes an illegal target for Release the Gremlins, it won't change the number of tokens created. However, if every target artifact becomes an illegal target, Release the Gremlins doesn't resolve and no tokens are created.")
    }
}
