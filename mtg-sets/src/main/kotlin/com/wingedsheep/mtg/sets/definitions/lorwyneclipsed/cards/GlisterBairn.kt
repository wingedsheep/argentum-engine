package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Glister Bairn
 * {2}{G/U}{G/U}{G/U}
 * Creature — Ouphe
 * 1/4
 *
 * Vivid — At the beginning of combat on your turn, another target creature you control gets
 * +X/+X until end of turn, where X is the number of colors among permanents you control.
 */
val GlisterBairn = card("Glister Bairn") {
    manaCost = "{2}{G/U}{G/U}{G/U}"
    typeLine = "Creature — Ouphe"
    oracleText = "Vivid — At the beginning of combat on your turn, another target creature you control " +
        "gets +X/+X until end of turn, where X is the number of colors among permanents you control."
    power = 1
    toughness = 4

    keywords(Keyword.VIVID)

    triggeredAbility {
        trigger = Triggers.BeginCombat
        val ally = target("creature", TargetCreature(filter = TargetFilter.OtherCreatureYouControl))
        effect = Effects.ModifyStats(
            power = DynamicAmounts.colorsAmongPermanents(),
            toughness = DynamicAmounts.colorsAmongPermanents(),
            target = ally
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "227"
        artist = "Nils Hamm"
        flavorText = "\"We'll leave this one here and that one there. Then wait and see who comes to share.\""
        imageUri = "https://cards.scryfall.io/normal/front/d/c/dcccdd98-a1ba-41f4-a9ae-84d263da0af9.jpg?1767862652"
        ruling("2025-11-17", "The value of X is calculated only once, as Glister Bairn's ability resolves.")
    }
}
