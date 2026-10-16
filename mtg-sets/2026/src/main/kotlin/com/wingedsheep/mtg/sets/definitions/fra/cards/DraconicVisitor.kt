package com.wingedsheep.mtg.sets.definitions.fra.cards

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
 * Draconic Visitor — Reality Fracture #80
 * {3}{R}{R} · Creature — Dragon · 5/5
 *
 * Flying
 * If one or more artifact tokens would be created under your control, that many 5/5 red Dragon
 * creature tokens with flying are created instead.
 *
 * A [ReplaceTokenCreationWithToken] scoped to artifact tokens created under your control: Treasure,
 * Clue, Food, artifact creature tokens and token copies of artifacts are all swapped one-for-one
 * for 5/5 flying Dragons. The Dragons aren't artifacts, so the replacement can't apply to its own
 * output; the replaced effect's riders ("tapped", "sacrifice it at end of turn") still apply to
 * them.
 */
val DraconicVisitor = card("Draconic Visitor") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Dragon"
    power = 5
    toughness = 5
    oracleText = "Flying\n" +
        "If one or more artifact tokens would be created under your control, that many 5/5 red Dragon " +
        "creature tokens with flying are created instead."

    keywords(Keyword.FLYING)

    replacementEffect(
        ReplaceTokenCreationWithToken(
            tokens = listOf(Effects.CreateToken(
                power = 5,
                toughness = 5,
                colors = setOf(Color.RED),
                creatureTypes = setOf("Dragon"),
                keywords = setOf(Keyword.FLYING),
                imageUri = "https://cards.scryfall.io/normal/front/9/9/991a5840-adc7-45b5-8d5f-b24dae384bab.jpg?1789734473"
            )),
            appliesTo = EventPattern.TokenCreationEvent(
                controller = Player.You,
                tokenFilter = GameObjectFilter.Artifact
            )
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "80"
        artist = "Alexander Mokhov"
        flavorText = "\"What is that?! A firebreathing sphinx?\"\n—Derenk, Hexhaven cadet"
        imageUri = "https://cards.scryfall.io/normal/front/1/1/112f8478-bd89-4a14-9721-8ab750613129.jpg?1789470842"
        inBooster = false
    }
}
