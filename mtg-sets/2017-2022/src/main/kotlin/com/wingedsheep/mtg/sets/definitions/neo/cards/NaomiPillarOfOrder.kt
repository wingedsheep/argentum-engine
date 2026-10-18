package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Naomi, Pillar of Order — Kamigawa: Neon Dynasty #229 (canonical printing)
 * {3}{W}{B} · Legendary Creature — Human Advisor · 4/4
 *
 * Whenever Naomi enters or attacks, if you control an artifact and an enchantment, create a 2/2 white
 * Samurai creature token with vigilance.
 *
 * "Enters or attacks" is two trigger events sharing one effect (the Inferno Titan shape). The control
 * check is an intervening-if (checked on trigger and again on resolution), made of two independent
 * existence checks so a single artifact enchantment satisfies it.
 */
private val naomiCondition = Conditions.All(
    Conditions.YouControl(GameObjectFilter.Artifact),
    Conditions.YouControl(GameObjectFilter.Enchantment),
)

private val naomiToken = Effects.CreateToken(
    power = 2,
    toughness = 2,
    colors = setOf(Color.WHITE),
    creatureTypes = setOf("Samurai"),
    keywords = setOf(Keyword.VIGILANCE),
)

private const val NAOMI_TEXT = "Whenever Naomi enters or attacks, if you control an artifact and an " +
    "enchantment, create a 2/2 white Samurai creature token with vigilance."

val NaomiPillarOfOrder = card("Naomi, Pillar of Order") {
    manaCost = "{3}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Legendary Creature — Human Advisor"
    power = 4
    toughness = 4
    oracleText = NAOMI_TEXT

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = naomiCondition
        effect = naomiToken
        description = NAOMI_TEXT
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        interveningIf = naomiCondition
        effect = naomiToken
        description = NAOMI_TEXT
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "229"
        artist = "Joshua Raphael"
        flavorText = "\"Progress comes only when we rise above our petty quarrels.\""
        imageUri = "https://cards.scryfall.io/normal/front/4/4/4400fd66-1a24-47fb-bc94-37e0aae0e610.jpg?1783923834"
        ruling("2022-02-18", "The artifact and the enchantment may be the same permanent.")
        ruling(
            "2022-02-18",
            "If you don't control both an artifact and an enchantment at the moment Naomi, Pillar of Order " +
                "enters the battlefield or attacks, its ability won't trigger at all. If the ability triggers, " +
                "it will check again as it tries to resolve. If you don't control both at that time, the " +
                "ability won't do anything."
        )
    }
}
