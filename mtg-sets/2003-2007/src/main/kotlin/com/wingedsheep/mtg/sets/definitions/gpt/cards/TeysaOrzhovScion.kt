package com.wingedsheep.mtg.sets.definitions.gpt.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Teysa, Orzhov Scion — Guildpact #134
 * {1}{W}{B} · Legendary Creature — Human Advisor · 2/3
 * Sacrifice three white creatures: Exile target creature.
 * Whenever another black creature you control dies, create a 1/1 white Spirit creature token with flying.
 *
 * Teysa may be one of the three sacrificed white creatures; any other white-and-black creature
 * sacrificed this way triggers the second ability, but Teysa herself never does ("another").
 */
val TeysaOrzhovScion = card("Teysa, Orzhov Scion") {
    manaCost = "{1}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Legendary Creature — Human Advisor"
    power = 2
    toughness = 3
    oracleText = "Sacrifice three white creatures: Exile target creature.\n" +
        "Whenever another black creature you control dies, create a 1/1 white Spirit creature token with flying."

    activatedAbility {
        cost = Costs.SacrificeMultiple(3, GameObjectFilter.Creature.withColor(Color.WHITE))
        val t = target(TargetFilter.Creature)
        effect = Effects.Exile(t)
    }

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.withColor(Color.BLACK).youControl()).dies()
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Spirit"),
            keywords = setOf(Keyword.FLYING),
            imageUri = "https://cards.scryfall.io/normal/front/0/6/06a96ac2-94a4-4b1c-a3c5-add1a3112cb2.jpg?1789737345",
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "134"
        artist = "Todd Lockwood"
        imageUri = "https://cards.scryfall.io/normal/front/9/e/9eb856e6-8f63-4048-9818-cc3e65748855.jpg?1783943468"
        ruling(
            "2024-01-12",
            "You may sacrifice Teysa itself to help pay for its first ability, but Teysa dying won't cause its " +
                "second ability to trigger. Any other white and black creatures you sacrifice to pay for the " +
                "first ability will cause the second ability to trigger."
        )
    }
}
