package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator

/**
 * Blot Out the Sky — Strixhaven: School of Mages #167 (canonical printing)
 * {X}{W}{B} · Sorcery
 *
 * Create X tapped 2/1 white and black Inkling creature tokens with flying. If X is 6 or more,
 * destroy all noncreature, nonland permanents.
 *
 * X tapped Inklings via the dynamic-count [Effects.CreateToken], then an [Effects.If] on
 * `X >= 6` gating [Effects.DestroyAll] over nonland noncreature permanents (same X gate as
 * Zero Point Ballad). Token art resolves through STX's synced token printings.
 */
val BlotOutTheSky = card("Blot Out the Sky") {
    manaCost = "{X}{W}{B}"
    colorIdentity = "BW"
    typeLine = "Sorcery"
    oracleText = "Create X tapped 2/1 white and black Inkling creature tokens with flying. " +
        "If X is 6 or more, destroy all noncreature, nonland permanents."

    spell {
        effect = Effects.CreateToken(
            count = DynamicAmounts.xValue(),
            power = 2,
            toughness = 1,
            colors = setOf(Color.WHITE, Color.BLACK),
            creatureTypes = setOf("Inkling"),
            keywords = setOf(Keyword.FLYING),
            tapped = true
        ) then Effects.If(
            condition = Conditions.CompareAmounts(
                DynamicAmounts.xValue(),
                ComparisonOperator.GTE,
                6
            ),
            then = Effects.DestroyAll(GameObjectFilter.NonlandPermanent.notCreature())
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "167"
        artist = "Olena Richards"
        flavorText = "A drop of ink is a nuisance. A torrent of inklings is a menace."
        imageUri = "https://cards.scryfall.io/normal/front/0/1/0141312f-4b68-4c56-b1dc-5b7e6afbb96c.jpg?1783927322"
    }
}
