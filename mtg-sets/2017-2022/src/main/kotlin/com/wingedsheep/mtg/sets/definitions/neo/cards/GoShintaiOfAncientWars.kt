package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Go-Shintai of Ancient Wars — Kamigawa: Neon Dynasty #144 (canonical printing)
 * {2}{R} · Legendary Enchantment Creature — Shrine · 2/2
 *
 * First strike
 * At the beginning of your end step, you may pay {1}. When you do, Go-Shintai of Ancient Wars deals
 * X damage to target player or planeswalker, where X is the number of Shrines you control.
 *
 * "When you do" is a reflexive trigger (CR 603.12): the {1} is paid as the end-step trigger
 * resolves, and only then does the damage ability go on the stack and choose its target. X is read
 * when that reflexive ability resolves and includes the Go-Shintai itself.
 */
val GoShintaiOfAncientWars = card("Go-Shintai of Ancient Wars") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Enchantment Creature — Shrine"
    power = 2
    toughness = 2
    oracleText = "First strike\nAt the beginning of your end step, you may pay {1}. When you do, " +
        "Go-Shintai of Ancient Wars deals X damage to target player or planeswalker, where X is the " +
        "number of Shrines you control."

    keywords(Keyword.FIRST_STRIKE)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        effect = Effects.ReflexiveTrigger(
            action = Effects.PayMana("{1}"),
            descriptionOverride = "You may pay {1}. When you do, Go-Shintai of Ancient Wars deals X " +
                "damage to target player or planeswalker, where X is the number of Shrines you control."
        ) {
            val victim = target(Targets.PlayerOrPlaneswalker)
            effect = Effects.DealDamage(
                DynamicAmounts.battlefield(Player.You, GameObjectFilter.Any.withSubtype("Shrine")).count(),
                victim,
            )
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "144"
        artist = "Johannes Voss"
        flavorText = "It seeks those consumed with rage and offers burning blades."
        imageUri = "https://cards.scryfall.io/normal/front/5/b/5b915e8d-b972-4ae1-a1c3-74d6623963c8.jpg?1783923866"
    }
}
