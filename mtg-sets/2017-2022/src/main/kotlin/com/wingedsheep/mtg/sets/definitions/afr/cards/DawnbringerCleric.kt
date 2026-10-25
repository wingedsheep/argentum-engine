package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Dawnbringer Cleric
 * {1}{W}
 * Creature — Human Cleric
 * 1/3
 * When this creature enters, choose one —
 * • Cure Wounds — You gain 2 life.
 * • Dispel Magic — Destroy target enchantment.
 * • Gentle Repose — Exile target card from a graveyard.
 */
val DawnbringerCleric = card("Dawnbringer Cleric") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Cleric"
    oracleText = "When this creature enters, choose one —\n" +
        "• Cure Wounds — You gain 2 life.\n" +
        "• Dispel Magic — Destroy target enchantment.\n" +
        "• Gentle Repose — Exile target card from a graveyard."
    power = 1
    toughness = 3
    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = ModalEffect.chooseOne(
            Mode.noTarget(Effects.GainLife(2), "Cure Wounds — You gain 2 life."),
            mode("Dispel Magic — Destroy target enchantment.") {
                val enchantment = target(TargetFilter.Enchantment)
                effect = Effects.Destroy(enchantment)
            },
            mode("Gentle Repose — Exile target card from a graveyard.") {
                val card = target(TargetFilter.CardInGraveyard)
                effect = Effects.Move(card, Zone.EXILE)
            },
        )
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "9"
        artist = "Lie Setiawan"
        imageUri = "https://cards.scryfall.io/normal/front/c/0/c00856f7-fef5-4ba5-9079-59a81d452c82.jpg?1783926536"
    }
}
