package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Bishop of Wings — Core Set 2020 #8
 * {W}{W} · Creature — Human Cleric · 1/4
 * Whenever an Angel you control enters, you gain 4 life.
 * Whenever an Angel you control dies, create a 1/1 white Spirit creature token with flying.
 *
 * Both triggers watch any Angel permanent you control (Assay's reading), so Bishop itself counts if
 * an effect makes it an Angel (ruling), and the dies trigger looks back in time, so it fires for each
 * Angel that dies alongside the Bishop.
 */
val BishopOfWings = card("Bishop of Wings") {
    manaCost = "{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Cleric"
    power = 1
    toughness = 4
    oracleText = "Whenever an Angel you control enters, you gain 4 life.\n" +
        "Whenever an Angel you control dies, create a 1/1 white Spirit creature token with flying."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Permanent.withSubtype("Angel").youControl()).enters()
        effect = Effects.GainLife(4)
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Permanent.withSubtype("Angel").youControl()).dies()
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Spirit"),
            keywords = setOf(Keyword.FLYING),
            imageUri = "https://cards.scryfall.io/normal/front/7/9/79b17be6-2ee7-4f3b-88df-f298bb058f46.jpg?1783932857",
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "8"
        artist = "Matt Stewart"
        flavorText = "\"Within every angel is a spirit of light that will fight as long as there is darkness.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/a/ca42da05-330d-4050-a580-dc00f6faff24.jpg?1783933032"
        ruling("2019-07-12", "If Bishop of Wings dies at the same time as one or more Angels you control, its last ability triggers for each of those Angels.")
        ruling("2019-07-12", "If an effect (such as that of Arcane Adaptation) causes Bishop of Wings to be an Angel, its first ability triggers as it enters the battlefield and its last ability triggers as it dies.")
    }
}
