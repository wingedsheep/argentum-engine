package com.wingedsheep.mtg.sets.definitions.som.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Skithiryx, the Blight Dragon
 * {3}{B}{B}
 * Legendary Creature — Phyrexian Dragon Skeleton
 * 4/4
 *
 * Flying
 * Infect
 * {B}: Skithiryx gains haste until end of turn.
 * {B}{B}: Regenerate Skithiryx.
 */
val SkithiryxTheBlightDragon = card("Skithiryx, the Blight Dragon") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Phyrexian Dragon Skeleton"
    power = 4
    toughness = 4
    oracleText = "Flying\n" +
        "Infect (This creature deals damage to creatures in the form of -1/-1 counters and to players in the form of poison counters.)\n" +
        "{B}: Skithiryx gains haste until end of turn.\n" +
        "{B}{B}: Regenerate Skithiryx."

    keywords(Keyword.FLYING, Keyword.INFECT)

    activatedAbility {
        cost = Costs.Mana("{B}")
        effect = Effects.GrantKeyword(Keyword.HASTE, EffectTarget.Self)
    }

    activatedAbility {
        cost = Costs.Mana("{B}{B}")
        effect = Effects.Regenerate(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "79"
        artist = "Chippy"
        imageUri = "https://cards.scryfall.io/normal/front/c/9/c930c9cc-1b64-4f36-afe2-6bf120a74ce2.jpg?1783941728"
    }
}
