package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Iron Lance
 * {2}
 * Artifact
 */
val IronLance = card("Iron Lance") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{3}, {T}: Target creature gains first strike until end of turn."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}"), Costs.Tap)
        val t = target("target", TargetCreature())
        effect = Effects.GrantKeyword(Keyword.FIRST_STRIKE, t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "300"
        artist = "Scott M. Fischer"
        flavorText = "\"The only way to get Mercadians to fight on the front lines is to give them really long weapons.\"\n" +
            "—Gerrard"
        imageUri = "https://cards.scryfall.io/normal/front/4/1/41f7d212-faf2-4a6f-a338-d9e5014b56d5.jpg"
    }
}
