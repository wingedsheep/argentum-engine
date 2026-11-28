package com.wingedsheep.mtg.sets.definitions.isd.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Nightbird's Clutches
 * {1}{R}
 * Sorcery
 * Up to two target creatures can't block this turn.
 * Flashback {3}{R} (You may cast this card from your graveyard for its flashback cost. Then exile it.)
 */
val NightbirdsClutches = card("Nightbird's Clutches") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Up to two target creatures can't block this turn.\nFlashback {3}{R} (You may cast this card from your graveyard for its flashback cost. Then exile it.)"
    spell {
        target = TargetObject(filter = TargetFilter.Creature, count = 2, optional = true)
        effect = Effects.ForEachTarget(Effects.CantBlock(EffectTarget.ContextTarget(0)))
    }
    keywordAbility(KeywordAbility.flashback("{3}{R}"))
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "154"
        artist = "Jason A. Engle"
        flavorText = "Shiny polished silver can ward off many creatures, but it attracts others."
        imageUri = "https://cards.scryfall.io/normal/front/b/5/b5c7410d-b69b-41a3-b469-e12c6ffc7578.jpg"
    }
}
