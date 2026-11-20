package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mogis's Marauder
 * {2}{B}
 * Creature — Human Berserker
 * 2/2
 *
 * When this creature enters, up to X target creatures each gain intimidate and haste until end
 * of turn, where X is your devotion to black.
 *
 * Devotion sets the *number of targets*, so it is read as the ability is put on the stack
 * (`dynamicMaxCount`), with the Marauder's own {B} counting; once chosen, the target count is
 * locked in. Each target gains both keywords on its own, so an illegal target at resolution
 * simply misses out while the rest still gain them.
 */
val MogissMarauder = card("Mogis's Marauder") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Berserker"
    power = 2
    toughness = 2
    oracleText = "When this creature enters, up to X target creatures each gain intimidate and haste " +
        "until end of turn, where X is your devotion to black. (A creature with intimidate can't be " +
        "blocked except by artifact creatures and/or creatures that share a color with it. Each {B} in " +
        "the mana costs of permanents you control counts toward your devotion to black.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        targets(
            TargetFilter.Creature,
            optional = true,
            dynamicMaxCount = DynamicAmounts.devotionTo(Color.BLACK),
        )
        effect = Effects.ForEachTarget(
            Effects.GrantKeyword(Keyword.INTIMIDATE, EffectTarget.ContextTarget(0)),
            Effects.GrantKeyword(Keyword.HASTE, EffectTarget.ContextTarget(0)),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "97"
        artist = "Chase Stone"
        imageUri = "https://cards.scryfall.io/normal/front/9/d/9dfd36c5-a484-4973-873c-b6a433da8e7a.jpg?1783939774"
        ruling(
            "2013-09-15",
            "Unlike most abilities that use devotion to determine the magnitude of an effect, Mogis's " +
                "Marauder uses devotion to determine the number of targets its ability has. Use your devotion " +
                "to black as you put the ability on the stack. Mogis's Marauder's mana cost will always count " +
                "toward your devotion to black. Once targets are chosen, it doesn't matter if your devotion to " +
                "black changes while the ability is on the stack. The number of targets is locked in."
        )
        ruling(
            "2013-09-15",
            "Hybrid mana symbols, monocolored hybrid mana symbols, and Phyrexian mana symbols do count " +
                "toward your devotion to their color(s)."
        )
    }
}
