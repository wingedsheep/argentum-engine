package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

val PuncturingBlow = card("Puncturing Blow") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Puncturing Blow deals 5 damage to target creature. If that creature would die this turn, exile it instead."

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.DealDamage(5, t) then Effects.MarkExileOnDeath(t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "106"
        artist = "Eric Deschamps"
        flavorText = "\"Neither the false God-Pharaoh nor his bugs will decide our fate.\"\n—Samut, former Tah-crop initiate"
        imageUri = "https://cards.scryfall.io/normal/front/d/7/d7d88da5-d9a3-49b3-a091-458b058c9114.jpg?1783936024"
        ruling("2017-07-14", "Puncturing Blow’s replacement effect will exile the target creature if it would die this turn for any reason, not just due to lethal damage. It applies to the target creature even if Puncturing Blow deals no damage to it (due to a prevention effect) or Puncturing Blow deals damage to a different creature (due to a redirection effect).")
    }
}
