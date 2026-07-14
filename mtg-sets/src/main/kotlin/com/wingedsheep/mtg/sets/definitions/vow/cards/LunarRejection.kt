package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Lunar Rejection
 * {1}{U}
 * Instant
 * Cleave {3}{U} (You may cast this spell for its cleave cost. If you do, remove the words in
 * square brackets.)
 * Return target [Wolf or Werewolf] creature to its owner's hand.
 * Draw a card.
 *
 * Cleave (CR 702.148) removes the bracketed words when its alternative cost is paid. The printed
 * (cheaper) cast is a tribal bounce — it can only return a Wolf or Werewolf creature; paying the
 * cleave cost broadens the target to any creature.
 *
 * Target-only difference: the base [target] carries the "Wolf or Werewolf" subtype restriction and
 * [cleaveTarget] drops it. The bounce + draw are identical in both modes, so only the target
 * requirement is swapped and the effect (return the chosen target, then draw a card) is shared.
 */
val LunarRejection = card("Lunar Rejection") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Cleave {3}{U} (You may cast this spell for its cleave cost. If you do, remove " +
        "the words in square brackets.)\nReturn target [Wolf or Werewolf] creature to its owner's " +
        "hand.\nDraw a card."

    keywordAbility(KeywordAbility.cleave("{3}{U}"))

    spell {
        // Printed (brackets present): return target Wolf or Werewolf creature, then draw a card.
        val wolfOrWerewolf = target(
            "Wolf or Werewolf creature",
            TargetCreature(
                filter = TargetFilter(
                    GameObjectFilter.Creature.withAnyOfSubtypes(listOf(Subtype.WOLF, Subtype.WEREWOLF)),
                ),
            ),
        )
        effect = Effects.ReturnToHand(wolfOrWerewolf).then(Effects.DrawCards(1))

        // Cleaved (brackets removed): return target creature, then draw a card.
        val anyCreature = cleaveTarget("creature", Targets.Creature)
        cleaveEffect = Effects.ReturnToHand(anyCreature).then(Effects.DrawCards(1))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "67"
        artist = "Donato Giancola"
        imageUri = "https://cards.scryfall.io/normal/front/0/f/0f66511c-355f-4e8a-96fc-3afc7a315231.jpg?1783924891"
    }
}
