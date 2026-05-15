package com.wingedsheep.mtg.sets.definitions.blc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Esika's Chariot
 * {3}{G}
 * Legendary Artifact — Vehicle
 * 4/4
 *
 * When Esika's Chariot enters, create two 2/2 green Cat creature tokens.
 * Whenever Esika's Chariot attacks, create a token that's a copy of target token you control.
 * Crew 4
 */
val EsikasChariot = card("Esika's Chariot") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Artifact — Vehicle"
    power = 4
    toughness = 4
    oracleText = "When Esika's Chariot enters, create two 2/2 green Cat creature tokens.\n" +
        "Whenever Esika's Chariot attacks, create a token that's a copy of target token you control.\n" +
        "Crew 4"

    // When this enters, create two 2/2 green Cat creature tokens.
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Cat"),
            count = 2,
            imageUri = "https://cards.scryfall.io/normal/front/2/1/21450a75-26ec-4344-bebc-d55c2cf3fc1c.jpg?1721427725"
        )
    }

    // Whenever this attacks, create a token that's a copy of target token you control.
    triggeredAbility {
        trigger = Triggers.Attacks
        val token = target(
            "target token you control",
            TargetObject(filter = TargetFilter(GameObjectFilter.Token.youControl()))
        )
        effect = Effects.CreateTokenCopyOfTarget(target = token)
    }

    keywordAbility(KeywordAbility.Numeric(Keyword.CREW, 4))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "215"
        artist = "Raoul Vitale"
        imageUri = "https://cards.scryfall.io/normal/front/1/f/1f2514c2-c0c7-44f3-ab9d-48b227f039db.jpg?1721429257"
    }
}
