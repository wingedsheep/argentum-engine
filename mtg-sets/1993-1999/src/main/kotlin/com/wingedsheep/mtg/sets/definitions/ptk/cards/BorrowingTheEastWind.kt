package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Borrowing the East Wind
 * {X}{G}{G}
 * Sorcery
 * Borrowing the East Wind deals X damage to each creature with horsemanship and each player.
 *
 * The Famine shape: one printed sentence, two halves joined by [Effects.Composite]. The board half
 * is [Effects.ForEachInGroup] over the horsemanship creatures with the damage aimed at
 * [EffectTarget.Self] — the current iteration entity — and the player half is the corpus' symmetric
 * player sweep, [Effects.ForEachPlayer] over [Player.Each], each iteration rebinding the controller
 * so [EffectTarget.Controller] is the player being processed.
 */
val BorrowingTheEastWind = card("Borrowing the East Wind") {
    manaCost = "{X}{G}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Borrowing the East Wind deals X damage to each creature with horsemanship and each player."

    spell {
        effect = Effects.Composite(
            Effects.ForEachInGroup(
                GroupFilter(GameObjectFilter.Creature.withKeyword(Keyword.HORSEMANSHIP)),
                Effects.DealDamage(DynamicAmount.XValue, EffectTarget.Self)
            ),
            Effects.ForEachPlayer(
                Player.Each,
                listOf(Effects.DealDamage(DynamicAmount.XValue, EffectTarget.Controller))
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "133"
        artist = "Gao Yan"
        imageUri = "https://cards.scryfall.io/normal/front/9/6/96ba9014-d750-4924-aa6f-8b9f421807f9.jpg"
    }
}
