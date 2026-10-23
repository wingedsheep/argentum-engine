package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Introduction to Annihilation — Strixhaven: School of Mages #3 (canonical printing)
 * {5} · Sorcery — Lesson
 *
 * Exile target nonland permanent. Its controller draws a card.
 *
 * [Effects.Exile] then [Effects.DrawCards] for [EffectTarget.TargetController], which reads the
 * controller the exiled permanent last had on the battlefield (Buy Your Silence's shape). Lesson
 * is only a subtype.
 */
val IntroductionToAnnihilation = card("Introduction to Annihilation") {
    manaCost = "{5}"
    colorIdentity = ""
    typeLine = "Sorcery — Lesson"
    oracleText = "Exile target nonland permanent. Its controller draws a card."

    spell {
        val t = target(TargetFilter.NonlandPermanent)
        effect = Effects.Exile(t) then Effects.DrawCards(1, EffectTarget.TargetController)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "3"
        artist = "Nicholas Gregory"
        flavorText = "\"I've never seen a student fail quite so spectacularly. There's potential in " +
            "that.\"\n—Hostram, professor of arcanochemistry"
        imageUri = "https://cards.scryfall.io/normal/front/b/0/b0bc4682-bcaf-4f51-be0b-9f2851a16e3b.jpg?1783927394"
    }
}
