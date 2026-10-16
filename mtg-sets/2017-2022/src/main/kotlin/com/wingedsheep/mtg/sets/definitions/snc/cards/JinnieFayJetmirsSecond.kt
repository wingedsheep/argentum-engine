package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.ReplaceTokenCreationWithToken
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Jinnie Fay, Jetmir's Second — Streets of New Capenna #195
 * {R/G}{G}{G/W} · Legendary Creature — Elf Druid · 3/3
 *
 * If you would create one or more tokens, you may instead create that many 2/2 green Cat creature
 * tokens with haste or that many 3/1 green Dog creature tokens with vigilance.
 *
 * An optional [ReplaceTokenCreationWithToken] with two substitutes: as any token-creation event
 * of yours happens, you pick Cats, Dogs or the original tokens, once for the whole event. The
 * creating effect's riders (tapped, attacking, "exile it at end of combat") still apply.
 */
val JinnieFayJetmirsSecond = card("Jinnie Fay, Jetmir's Second") {
    manaCost = "{R/G}{G}{G/W}"
    colorIdentity = "RGW"
    typeLine = "Legendary Creature — Elf Druid"
    power = 3
    toughness = 3
    oracleText = "If you would create one or more tokens, you may instead create that many 2/2 green Cat " +
        "creature tokens with haste or that many 3/1 green Dog creature tokens with vigilance."

    replacementEffect(
        ReplaceTokenCreationWithToken(
            tokens = listOf(
                Effects.CreateToken(
                    power = 2,
                    toughness = 2,
                    colors = setOf(Color.GREEN),
                    creatureTypes = setOf("Cat"),
                    keywords = setOf(Keyword.HASTE),
                    imageUri = "https://cards.scryfall.io/normal/front/5/7/57bd0b29-0860-4ee3-832a-f62ba7bf4aac.jpg?1783922959"
                ),
                Effects.CreateToken(
                    power = 3,
                    toughness = 1,
                    colors = setOf(Color.GREEN),
                    creatureTypes = setOf("Dog"),
                    keywords = setOf(Keyword.VIGILANCE),
                    imageUri = "https://cards.scryfall.io/normal/front/6/7/6736fe71-bd55-4602-b0aa-ece6193048a9.jpg?1783922959"
                )
            ),
            appliesTo = EventPattern.TokenCreationEvent(controller = Player.You),
            optional = true
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "195"
        artist = "David Gaillet"
        flavorText = "She watches Jetmir's back while her companions watch hers."
        imageUri = "https://cards.scryfall.io/normal/front/c/5/c5f9326a-2a41-45b3-97c3-548f0bdc0882.jpg?1783923082"
    }
}
