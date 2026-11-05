package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Chandra's Defeat
 * {R}
 * Instant
 * Chandra's Defeat deals 5 damage to target red creature or red planeswalker. If that permanent is
 * a Chandra planeswalker, you may discard a card. If you do, draw a card.
 *
 * The Chandra gate is read before the damage, like its Defeat siblings: lethal damage doesn't remove
 * the permanent until state-based actions after resolution, so checking first reads the same object.
 * The optional rummage is Immersturm Raider's spelling: [Effects.May] around [Effects.IfYouDo].
 */
val ChandrasDefeat = card("Chandra's Defeat") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Chandra's Defeat deals 5 damage to target red creature or red planeswalker. If that " +
        "permanent is a Chandra planeswalker, you may discard a card. If you do, draw a card."

    spell {
        val victim = target(TargetFilter(GameObjectFilter.CreatureOrPlaneswalker.withColor(Color.RED)))
        effect = Effects.If(
            condition = Conditions.EntityMatches(
                victim,
                GameObjectFilter.Planeswalker.withSubtype(Subtype.CHANDRA)
            ),
            then = Effects.DealDamage(5, victim) then
                Effects.May(Effects.IfYouDo(Patterns.Hand.discardCards(1), Effects.DrawCards(1))),
            otherwise = Effects.DealDamage(5, victim)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "86"
        artist = "Kieran Yanner"
        flavorText = "\"Fire? Is that your only trick, Chandra?\"\n—Nicol Bolas"
        imageUri = "https://cards.scryfall.io/normal/front/f/3/f38c6613-b1f7-4b38-b956-7860d041e593.jpg?1783936031"
    }
}
