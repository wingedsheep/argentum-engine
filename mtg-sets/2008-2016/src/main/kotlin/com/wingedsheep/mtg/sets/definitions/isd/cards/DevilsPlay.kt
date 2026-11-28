package com.wingedsheep.mtg.sets.definitions.isd.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Devil's Play
 * {X}{R}
 * Sorcery
 * Devil's Play deals X damage to any target.
 * Flashback {X}{R}{R}{R} (You may cast this card from your graveyard for its flashback cost. Then exile it.)
 */
val DevilsPlay = card("Devil's Play") {
    manaCost = "{X}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Devil's Play deals X damage to any target.\nFlashback {X}{R}{R}{R} (You may cast this card from your graveyard for its flashback cost. Then exile it.)"
    spell {
        val any = target(Targets.Any)
        effect = Effects.DealDamage(DynamicAmounts.xValue(), any)
    }
    keywordAbility(KeywordAbility.flashback("{X}{R}{R}{R}"))
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "140"
        artist = "Austin Hsu"
        flavorText = "A devil's hands are never idle."
        imageUri = "https://cards.scryfall.io/normal/front/c/8/c80596a4-b464-4b9e-8186-94a1c44838eb.jpg"
    }
}
