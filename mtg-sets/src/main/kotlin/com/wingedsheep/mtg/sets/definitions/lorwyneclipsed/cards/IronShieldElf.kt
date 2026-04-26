package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Iron-Shield Elf
 * {1}{B}
 * Creature — Elf Warrior
 * 3/1
 *
 * Discard a card: This creature gains indestructible until end of turn. Tap it.
 */
val IronShieldElf = card("Iron-Shield Elf") {
    manaCost = "{1}{B}"
    typeLine = "Creature — Elf Warrior"
    power = 3
    toughness = 1
    oracleText = "Discard a card: This creature gains indestructible until end of turn. Tap it. " +
        "(Damage and effects that say \"destroy\" don't destroy it. If its toughness is 0 or less, it still dies.)"

    activatedAbility {
        cost = Costs.DiscardCard
        effect = Effects.Composite(
            Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, EffectTarget.Self),
            Effects.Tap(EffectTarget.Self),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "108"
        artist = "Adrián Rodríguez Pérez"
        flavorText = "Flamekin metalcraft saves many an elvish life but mortally wounds their pride."
        imageUri = "https://cards.scryfall.io/normal/front/9/e/9e0140b2-0185-4adb-b365-2611ce89a0e2.jpg?1767658146"
        ruling("2025-11-17", "You can activate Iron-Shield Elf's ability even if Iron-Shield Elf is already tapped.")
    }
}
