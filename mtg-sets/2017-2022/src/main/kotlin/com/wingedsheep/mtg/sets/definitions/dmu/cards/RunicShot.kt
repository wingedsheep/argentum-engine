package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

val RunicShot = card("Runic Shot") {
    manaCost = "{W}"
    colorIdentity = "WU"
    typeLine = "Sorcery"
    oracleText = "Kicker {U} (You may pay an additional {U} as you cast this spell.)\nDestroy target tapped creature. If this spell was kicked, scry 2."

    keywordAbility(KeywordAbility.kicker("{U}"))

    spell {
        val creature = target(TargetFilter.TappedCreature)
        effect = Effects.Destroy(creature) then
            Effects.If(Conditions.WasKicked, Patterns.Library.scry(2))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "30"
        artist = "Cristi Balanescu"
        flavorText = "\"Target acquired.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/5/f54413be-7d73-478d-a25a-8fe06f6491a5.jpg?1783921361"
    }
}
