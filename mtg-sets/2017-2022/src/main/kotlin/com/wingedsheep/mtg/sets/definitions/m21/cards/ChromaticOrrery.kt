package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.SpendManaAsColor
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Chromatic Orrery
 * {7}
 * Legendary Artifact
 * You may spend mana as though it were mana of any color.
 * {T}: Add {C}{C}{C}{C}{C}.
 * {5}, {T}: Draw a card for each color among permanents you control.
 *
 * The spending static is Mycosynth Lattice's open-ended [SpendManaAsColor] (any mana, colorless
 * included, as any color) held by you alone. The draw counts Faeburrow Elder's
 * [DynamicAmounts.colorsAmongPermanents] — projected colors, capped at five (ruling).
 */
val ChromaticOrrery = card("Chromatic Orrery") {
    manaCost = "{7}"
    colorIdentity = ""
    typeLine = "Legendary Artifact"
    oracleText = "You may spend mana as though it were mana of any color.\n" +
        "{T}: Add {C}{C}{C}{C}{C}.\n" +
        "{5}, {T}: Draw a card for each color among permanents you control."

    staticAbility {
        ability = SpendManaAsColor(fromColor = null, toColor = null, players = Player.You)
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(5)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{5}"), Costs.Tap)
        effect = Effects.DrawCards(DynamicAmounts.colorsAmongPermanents())
        description = "{5}, {T}: Draw a card for each color among permanents you control."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "228"
        artist = "Volkan Baǵa"
        flavorText = "Visitors marvel at its design, unaware of the other world that inspired it."
        imageUri = "https://cards.scryfall.io/normal/front/3/a/3af78d76-ad5c-44ba-880d-b834bcde5398.jpg?1783930659"
        ruling("2020-06-23", "Chromatic Orrery's last ability can have you draw at most five cards. \"Colorless,\" \"artifact,\" and \"gold\" are not colors.")
    }
}
