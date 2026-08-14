package com.wingedsheep.mtg.sets.definitions.blb.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.gift
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GiftKind
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Scrapshooter {1}{G}{G}
 * Creature — Raccoon Archer
 * 4/4
 *
 * Gift a card (You may promise an opponent a gift as you cast this spell.
 * If you do, when it enters, they draw a card.)
 * Reach
 * When this creature enters, if the gift was promised, destroy target artifact
 * or enchantment an opponent controls.
 *
 * The gift is promised as you cast (CR 702.174a) — `gift(...)` supplies both the additional cost
 * and the "they draw a card" enters ability. The printed enters ability is an intervening-if
 * trigger (CR 603.4) on the same promise, so a Scrapshooter cast without the gift never triggers
 * and never asks for a target — which is what CR 702.174m requires: targets belonging to a
 * gift-gated part of an ability are chosen only if the gift was promised.
 */
val Scrapshooter = card("Scrapshooter") {
    manaCost = "{1}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Raccoon Archer"
    power = 4
    toughness = 4
    oracleText = "Gift a card (You may promise an opponent a gift as you cast this spell. If you do, when it enters, they draw a card.)\nReach\nWhen this creature enters, if the gift was promised, destroy target artifact or enchantment an opponent controls."

    keywords(Keyword.REACH)

    gift(GiftKind.CARD)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        triggerCondition = Conditions.GiftWasPromised
        target = TargetObject(filter = TargetFilter.ArtifactOrEnchantment.opponentControls())
        effect = Effects.Destroy(EffectTarget.ContextTarget(0))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "191"
        artist = "Chris Rahn"
        imageUri = "https://cards.scryfall.io/normal/front/c/4/c42ab407-e72d-4c48-9a9e-2055b5e71c69.jpg?1721426916"

        ruling("2024-07-26", "For permanent spells with gift, an ability triggers when that permanent enters if the gift was promised. When that ability resolves, the gift is given to the appropriate opponent.")
        ruling("2024-07-26", "If a spell for which the gift was promised is countered, doesn't resolve, or is otherwise removed from the stack, the gift won't be given.")
    }
}
