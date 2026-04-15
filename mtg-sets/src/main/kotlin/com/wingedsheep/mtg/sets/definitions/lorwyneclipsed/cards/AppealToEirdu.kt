package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ForEachTargetEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Appeal to Eirdu
 * {3}{W}
 * Instant
 *
 * Convoke (Your creatures can help cast this spell. Each creature you tap while
 * casting this spell pays for {1} or one mana of that creature's color.)
 * One or two target creatures each get +2/+1 until end of turn.
 */
val AppealToEirdu = card("Appeal to Eirdu") {
    manaCost = "{3}{W}"
    typeLine = "Instant"
    oracleText = "Convoke (Your creatures can help cast this spell. Each creature you tap while casting this spell pays for {1} or one mana of that creature's color.)\n" +
        "One or two target creatures each get +2/+1 until end of turn."

    keywords(Keyword.CONVOKE)

    spell {
        target = TargetCreature(count = 2, minCount = 1, filter = TargetFilter.Creature)
        effect = ForEachTargetEffect(
            listOf(Effects.ModifyStats(2, 1, EffectTarget.ContextTarget(0)))
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "5"
        artist = "Milivoj Ćeran"
        flavorText = "Adherents of the sun implore others to bask in Eirdu's light and expel darkness from the plane."
        imageUri = "https://cards.scryfall.io/normal/front/6/8/68ce9752-21f9-48e9-bf48-7f76f1cecbc5.jpg?1767692118"
    }
}
