package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Rotten Reunion
 * {B}
 * Instant
 * Exile up to one target card from a graveyard. Create a 2/2 black Zombie creature token with decayed.
 * Flashback {1}{B}
 */
val RottenReunion = card("Rotten Reunion") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Exile up to one target card from a graveyard. Create a 2/2 black Zombie creature token with decayed. " +
        "(It can't block. When it attacks, sacrifice it at end of combat.)\n" +
        "Flashback {1}{B} (You may cast this card from your graveyard for its flashback cost. Then exile it.)"

    spell {
        // "Up to one": the exile target is optional; the Zombie is created regardless.
        val graveyardCard = target(TargetFilter.CardInGraveyard, optional = true)
        effect = Effects.Exile(graveyardCard) then Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Zombie"),
            keywords = setOf(Keyword.DECAYED),
            imageUri = "https://cards.scryfall.io/normal/front/6/a/6adb8607-1066-451d-a719-74ad32358278.jpg?1783925226",
        )
    }

    keywordAbility(KeywordAbility.flashback("{1}{B}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "119"
        artist = "Aaron Miller"
        flavorText = "Aunt and Uncle Greyfeld loved dinner guests."
        imageUri = "https://cards.scryfall.io/normal/front/9/7/97398ad2-675b-4a34-aab7-935dd6714f1c.jpg?1783925610"
    }
}
