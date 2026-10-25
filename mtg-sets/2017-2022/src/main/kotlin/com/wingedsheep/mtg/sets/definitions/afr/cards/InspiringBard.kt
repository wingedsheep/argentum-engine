package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Inspiring Bard
 * {3}{G}
 * Creature — Elf Bard
 * 3/3
 * When this creature enters, choose one —
 * • Bardic Inspiration — Target creature gets +2/+2 until end of turn.
 * • Song of Rest — You gain 3 life.
 */
val InspiringBard = card("Inspiring Bard") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Bard"
    oracleText = "When this creature enters, choose one —\n" +
        "• Bardic Inspiration — Target creature gets +2/+2 until end of turn.\n" +
        "• Song of Rest — You gain 3 life."
    power = 3
    toughness = 3
    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = ModalEffect.chooseOne(
            mode("Bardic Inspiration — Target creature gets +2/+2 until end of turn.") {
                val creature = target(TargetFilter.Creature)
                effect = Effects.ModifyStats(2, 2, creature)
            },
            Mode.noTarget(Effects.GainLife(3), "Song of Rest — You gain 3 life."),
        )
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "189"
        artist = "Eelis Kyttanen"
        flavorText = "\"Wake up, my friends! There are deeds to be done and treasures to be won!\""
        imageUri = "https://cards.scryfall.io/normal/front/0/b/0b33db66-d205-4164-b168-6084df562ebb.jpg?1783926461"
    }
}
