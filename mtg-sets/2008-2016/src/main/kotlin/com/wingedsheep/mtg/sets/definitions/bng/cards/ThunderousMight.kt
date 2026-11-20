package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Thunderous Might
 * {1}{R}
 * Enchantment — Aura
 * Enchant creature
 * Whenever enchanted creature attacks, it gets +X/+0 until end of turn, where X is your devotion
 * to red.
 *
 * The attachment trigger binds the attacking (enchanted) creature as the triggering entity, so
 * "it" keeps pointing at that creature even if the Aura leaves before the trigger resolves.
 * Devotion is counted as the trigger resolves, for the Aura's controller ("your").
 */
val ThunderousMight = card("Thunderous Might") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Whenever enchanted creature attacks, it gets +X/+0 until end of turn, where X is your " +
        "devotion to red. (Each {R} in the mana costs of permanents you control counts toward your " +
        "devotion to red.)"
    auraTarget = TargetObject(filter = TargetFilter.Creature)

    triggeredAbility {
        trigger = Triggers.attached.attacks()
        effect = Effects.ModifyStats(
            DynamicAmounts.devotionTo(Color.RED),
            DynamicAmounts.fixed(0),
            EffectTarget.TriggeringEntity,
        )
        description = "Whenever enchanted creature attacks, it gets +X/+0 until end of turn, where X " +
            "is your devotion to red."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "114"
        artist = "Jaime Jones"
        imageUri = "https://cards.scryfall.io/normal/front/d/3/d361851d-e2d4-4912-ba08-81913f6b6e08.jpg?1783939538"
        ruling(
            "2013-09-15",
            "Mana symbols in the text boxes of permanents you control don't count toward your devotion to any color."
        )
        ruling(
            "2013-09-15",
            "Numeric mana symbols ({0}, {1}, and so on) in mana costs of permanents you control don't count " +
                "toward your devotion to any color."
        )
        ruling(
            "2013-09-15",
            "Hybrid mana symbols, monocolored hybrid mana symbols, and Phyrexian mana symbols do count " +
                "toward your devotion to their color(s)."
        )
        ruling(
            "2013-09-15",
            "If an activated ability or triggered ability has an effect that depends on your devotion to a " +
                "color, you count the number of mana symbols of that color among the mana costs of " +
                "permanents you control as the ability resolves. The permanent with that ability will be " +
                "counted if it's still on the battlefield at that time."
        )
    }
}
