package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Kami of Terrible Secrets — Kamigawa: Neon Dynasty #105 (canonical printing)
 * {3}{B} · Creature — Spirit · 3/4
 *
 * When this creature enters, if you control an artifact and an enchantment, you draw a card and you
 * gain 1 life.
 *
 * The control check is an intervening-if made of two independent existence checks, so a single
 * artifact enchantment satisfies it (the Naomi, Pillar of Order shape).
 */
val KamiOfTerribleSecrets = card("Kami of Terrible Secrets") {
    manaCost = "{3}{B}"
    typeLine = "Creature — Spirit"
    power = 3
    toughness = 4
    oracleText = "When this creature enters, if you control an artifact and an enchantment, you draw a card " +
        "and you gain 1 life."

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.All(
            Conditions.YouControl(GameObjectFilter.Artifact),
            Conditions.YouControl(GameObjectFilter.Enchantment),
        )
        effect = Effects.DrawCards(1) then Effects.GainLife(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "105"
        artist = "Mark Zug"
        flavorText = "It feeds on things better left unknown, savoring the bitterness of lies and regrets."
        imageUri = "https://cards.scryfall.io/normal/front/5/8/58f15af6-eaf5-41b8-b93d-5a90ca219478.jpg?1783923883"
        ruling("2022-02-18", "The artifact and the enchantment may be the same permanent.")
        ruling(
            "2022-02-18",
            "If you don't control both an artifact and an enchantment at the moment Kami of Terrible Secrets " +
                "enters the battlefield, its ability won't trigger at all. If the ability triggers, it will " +
                "check again as it tries to resolve. If you don't control both at that time, the ability " +
                "won't do anything."
        )
    }
}
