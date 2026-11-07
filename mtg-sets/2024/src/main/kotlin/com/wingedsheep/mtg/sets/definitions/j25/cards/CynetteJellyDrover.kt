package com.wingedsheep.mtg.sets.definitions.j25.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Cynette, Jelly Drover — Foundations Jumpstart #35
 * {3}{U} · Legendary Creature — Human Wizard · 2/2
 *
 * When Cynette enters or dies, create a 1/1 blue Jellyfish creature token with flying.
 * Creatures you control with flying get +1/+1.
 *
 * "Enters or dies" is two triggered abilities (Crow of Dark Tidings' shape).
 */
val CynetteJellyDrover = card("Cynette, Jelly Drover") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Human Wizard"
    power = 2
    toughness = 2
    oracleText = "When Cynette enters or dies, create a 1/1 blue Jellyfish creature token with flying.\n" +
        "Creatures you control with flying get +1/+1."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = jellyfishToken()
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = jellyfishToken()
    }

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(GameObjectFilter.Creature.withKeyword(Keyword.FLYING).youControl())
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "35"
        artist = "Daisuke Tatsuma"
        flavorText = "\"Once you build up an immunity to their deadly venom, they're really quite cuddly!\""
        imageUri = "https://cards.scryfall.io/normal/front/8/8/8810b817-7349-468b-bcca-ba0843bbc852.jpg?1783908859"
    }
}

private fun jellyfishToken() = Effects.CreateToken(
    power = 1,
    toughness = 1,
    colors = setOf(Color.BLUE),
    creatureTypes = setOf("Jellyfish"),
    keywords = setOf(Keyword.FLYING)
)
