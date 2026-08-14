package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CompositeStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.SetBasePowerToughnessStatic
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Archon of the Wild Rose
 * {2}{W}{W}
 * Creature — Archon
 * 4/4
 *
 * Flying
 * Other creatures you control that are enchanted by Auras you control have base power and
 * toughness 4/4 and have flying.
 *
 * The two adjectives bind to *different* objects: `youControl()` constrains the creature's
 * controller, `enchantedByAura(ControlledByYou)` constrains the controller of the Aura attached to
 * it. That is the whole reason this is not A Tale for the Ages' plain `enchanted()` — an opponent's
 * Aura (or Role) on your creature does not switch the Archon on, and your Aura on their creature
 * does not either. `excludeSelf` covers "other", and the Archon's own printed flying is unaffected.
 *
 * The base-P/T set (Layer 7b) and the flying grant (Layer 6) are one printed ability, so they go in
 * a single [CompositeStaticAbility] rather than two `staticAbility { }` blocks (CR 613.6): the
 * engine locks the affected set once and applies both layers to it, instead of each part
 * re-resolving its own set as the layers are walked. Per the printed rulings this only overwrites
 * *earlier* base-P/T-setting effects — a later one (or any +N/+N modifier or counter, whenever it
 * started) still applies on top, which the layer system gives us for free.
 */
val ArchonOfTheWildRose = card("Archon of the Wild Rose") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Archon"
    power = 4
    toughness = 4
    oracleText = "Flying\n" +
        "Other creatures you control that are enchanted by Auras you control have base power and " +
        "toughness 4/4 and have flying."

    keywords(Keyword.FLYING)

    val enchantedByYourAuras = GroupFilter(
        GameObjectFilter.Creature.youControl().enchantedByAura(),
        excludeSelf = true
    )

    staticAbility {
        ability = CompositeStaticAbility(
            listOf(
                SetBasePowerToughnessStatic(4, 4, enchantedByYourAuras),
                GrantKeyword(Keyword.FLYING, enchantedByYourAuras),
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "1"
        artist = "Chris Rahn"
        flavorText = "The curse of Redtooth Keep could only be broken by a mystical rose that " +
            "blooms in moonlight."
        imageUri = "https://cards.scryfall.io/normal/front/0/0/00174be7-0dc8-43b9-81b6-f25a8c3fb4eb.jpg?1783915137"

        ruling(
            "2023-09-01",
            "Archon of the Wild Rose's ability overwrites all previous effects that set the " +
                "affected creatures' power and/or toughness to specific values. Other effects that " +
                "set these characteristics to specific values that start to apply after Archon of " +
                "the Wild Rose enters the battlefield will overwrite this effect."
        )
        ruling(
            "2023-09-01",
            "Effects that modify a creature's power and/or toughness without setting it will apply " +
                "to the affected creatures no matter when they started to take effect. The same is " +
                "true for counters that change a creature's power and/or toughness."
        )
    }
}
