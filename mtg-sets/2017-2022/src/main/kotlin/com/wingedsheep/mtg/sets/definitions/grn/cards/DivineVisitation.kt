package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ReplaceTokenCreationWithToken
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Divine Visitation — Guilds of Ravnica #10
 * {3}{W}{W} · Enchantment
 *
 * If one or more creature tokens would be created under your control, that many 4/4 white Angel
 * creature tokens with flying and vigilance are created instead.
 *
 * A [ReplaceTokenCreationWithToken] scoped to creature tokens created under your control — the
 * same shape as Draconic Visitor's artifact-token swap. The replaced effect's riders (tapped,
 * attacking, "sacrifice it at end of turn") still apply to the Angels.
 */
val DivineVisitation = card("Divine Visitation") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "If one or more creature tokens would be created under your control, that many 4/4 " +
        "white Angel creature tokens with flying and vigilance are created instead."

    replacementEffect(
        ReplaceTokenCreationWithToken(
            tokens = listOf(Effects.CreateToken(
                power = 4,
                toughness = 4,
                colors = setOf(Color.WHITE),
                creatureTypes = setOf("Angel"),
                keywords = setOf(Keyword.FLYING, Keyword.VIGILANCE),
                imageUri = "https://cards.scryfall.io/normal/front/a/c/acb271a8-68bb-45e6-9f99-568479e92ea0.jpg?1783934064"
            )),
            appliesTo = EventPattern.TokenCreationEvent(
                controller = Player.You,
                tokenFilter = GameObjectFilter.Creature
            )
        )
    )

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "10"
        artist = "Victor Adame Minguez"
        flavorText = "The angels appreciated the offer but politely declined to eat any birdseed."
        imageUri = "https://cards.scryfall.io/normal/front/d/a/da3789d4-3621-44a1-bb72-8e3bbac8bf72.jpg?1783934201"
        ruling("2018-10-05", "The token's characteristics are entirely replaced by a 4/4 white Angel creature token with flying and vigilance. It doesn't have any abilities the token would have been created with. Anything else specified in the effect creating the token (such as tapped, attacking, \"That token gains haste,\" or \"Exile that token at end of combat\") still applies.")
        ruling("2018-10-05", "If you create a noncreature token that will be a creature as it enters the battlefield, perhaps because of an effect such as that of March of the Machines, Divine Visitation's effect doesn't apply to that token's creation.")
        ruling("2018-10-05", "If an effect changes under whose control a token would be created, that effect applies before Divine Visitation's effect applies. If an effect changes under whose control a token would enter the battlefield, that effect applies after Divine Visitation's effect is able to be applied.")
    }
}
