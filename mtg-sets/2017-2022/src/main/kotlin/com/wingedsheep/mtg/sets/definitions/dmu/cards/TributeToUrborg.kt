package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.unaryMinus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Tribute to Urborg — Dominaria United #113
 * {1}{B} · Instant
 *
 * The kicked rider is a second, separate -X/-X with X counted as the spell resolves; Tribute to
 * Urborg is still on the stack then, so it never counts itself (2022-09-09 ruling).
 */
val TributeToUrborg = card("Tribute to Urborg") {
    manaCost = "{1}{B}"
    colorIdentity = "UB"
    typeLine = "Instant"
    oracleText = "Kicker {1}{U} (You may pay an additional {1}{U} as you cast this spell.)\n" +
        "Target creature gets -2/-2 until end of turn. If this spell was kicked, that creature " +
        "gets an additional -1/-1 until end of turn for each instant and sorcery card in your graveyard."

    keywordAbility(KeywordAbility.kicker("{1}{U}"))

    spell {
        val creature = target(TargetFilter.Creature)
        val spells = DynamicAmounts.count(Player.You, Zone.GRAVEYARD, GameObjectFilter.InstantOrSorcery)
        effect = Effects.ModifyStats(-2, -2, creature) then
            Effects.If(Conditions.WasKicked, Effects.ModifyStats(-spells, -spells, creature))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "113"
        artist = "Jodie Muir"
        imageUri = "https://cards.scryfall.io/normal/front/3/1/31274707-3e16-4f46-8450-6927d936064b.jpg?1783921323"
        ruling(
            "2022-09-09",
            "Tribute to Urborg is not in your graveyard yet as it is resolving, so it doesn't count itself."
        )
    }
}
