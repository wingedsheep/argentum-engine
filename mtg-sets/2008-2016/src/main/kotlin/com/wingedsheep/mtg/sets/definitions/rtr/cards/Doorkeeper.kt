package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Doorkeeper — Return to Ravnica #37 (canonical printing)
 * {1}{U} · Creature — Homunculus · 0/4
 *
 * Defender
 * {2}{U}, {T}: Target player mills X cards, where X is the number of creatures you control with
 * defender.
 *
 * Millstone's targeted mill over Axebane Guardian's count of your defender creatures; the count
 * is a [DynamicAmounts.battlefield] amount, read on projected state as the ability resolves, and
 * includes Doorkeeper itself.
 */
val Doorkeeper = card("Doorkeeper") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Homunculus"
    power = 0
    toughness = 4
    oracleText = "Defender\n{2}{U}, {T}: Target player mills X cards, where X is the number of " +
        "creatures you control with defender."

    keywords(Keyword.DEFENDER)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{U}"), Costs.Tap)
        val player = target(Targets.Player)
        effect = Patterns.Library.mill(
            DynamicAmounts.battlefield(
                Player.You,
                GameObjectFilter.Creature.withKeyword(Keyword.DEFENDER),
            ).count(),
            player,
        )
        description = "{2}{U}, {T}: Target player mills X cards, where X is the number of creatures " +
            "you control with defender."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "37"
        artist = "Kev Walker"
        flavorText = "One piercing stare usually convinces thieves to go elsewhere."
        imageUri = "https://cards.scryfall.io/normal/front/5/c/5c31221f-3753-4d5c-905a-6b558ab648ae.jpg?1783940369"
        ruling(
            "2012-10-01",
            "The number of creatures with defender you control is counted when the ability resolves.",
        )
    }
}
