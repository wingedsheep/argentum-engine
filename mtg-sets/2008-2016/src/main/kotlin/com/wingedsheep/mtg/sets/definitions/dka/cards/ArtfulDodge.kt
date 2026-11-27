package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Artful Dodge
 * {U}
 * Sorcery
 * Target creature can't be blocked this turn.
 * Flashback {U}
 */
val ArtfulDodge = card("Artful Dodge") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Target creature can't be blocked this turn.\n" +
        "Flashback {U} (You may cast this card from your graveyard for its flashback cost. Then exile it.)"

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.GrantKeyword(AbilityFlag.CANT_BE_BLOCKED, creature)
    }
    keywordAbility(KeywordAbility.flashback("{U}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "27"
        artist = "Tomasz Jedruszek"
        flavorText = "Those who know the alleys and sewers of the Erdwal can disappear like smoke."
        imageUri = "https://cards.scryfall.io/normal/front/d/e/de6ce6aa-e19f-4299-9807-e68920e63c73.jpg"
    }
}
