package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

val Hijack = card("Hijack") {
    manaCost = "{1}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Gain control of target artifact or creature until end of turn. Untap it. It gains haste until end of turn."

    spell {
        val permanent = target(TargetFilter.Artifact.or(TargetFilter.Creature))
        effect = Effects.GainControl(permanent, Duration.EndOfTurn) then
            Effects.Untap(permanent) then
            Effects.GrantKeyword(Keyword.HASTE, permanent)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "118"
        artist = "Victor Adame Minguez"
        flavorText = "\"Whoa there. Easy. We're just going to take a little detour.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/e/0e925954-faa3-4d16-8442-e34af1f85fb6.jpg?1783937193"
        ruling("2017-09-29", "You can target and gain control of an untapped artifact or creature this way. You can also untap an artifact or creature you already control and give it haste.")
    }
}
