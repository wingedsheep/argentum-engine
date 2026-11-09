package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Vildin-Pack Outcast // Dronepack Kindred (Eldritch Moon #148)
 * {4}{R}
 * Creature — Werewolf Horror 4/4 // Creature — Eldrazi Werewolf 5/7
 *
 * Front — Trample. "{R}: This creature gets +1/-1 until end of turn."
 *         "{5}{R}{R}: Transform this creature."
 * Back  — Trample. "{1}: This creature gets +1/+0 until end of turn."
 *
 * The flip is an ordinary activated [Effects.Transform] (Shrill Howler's shape), not
 * Daybound/Nightbound. The back is a colorless Eldrazi Werewolf (no mana cost, no color indicator);
 * `colorIdentity` stays "R" because identity is a property of the whole card. Both pumps are plain
 * activated abilities (they don't produce mana), each one [Effects.ModifyStats] onto the source.
 */

private val VildinPackOutcastFront = card("Vildin-Pack Outcast") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Werewolf Horror"
    power = 4
    toughness = 4
    oracleText = "Trample\n" +
        "{R}: This creature gets +1/-1 until end of turn.\n" +
        "{5}{R}{R}: Transform this creature."

    keywords(Keyword.TRAMPLE)

    activatedAbility {
        cost = Costs.Mana("{R}")
        effect = Effects.ModifyStats(1, -1, EffectTarget.Self)
        description = "This creature gets +1/-1 until end of turn."
    }

    activatedAbility {
        cost = Costs.Mana("{5}{R}{R}")
        effect = Effects.Transform(EffectTarget.Self)
        description = "Transform this creature."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "148"
        artist = "Jose Cabrera"
        imageUri = "https://cards.scryfall.io/normal/front/4/6/460f7733-c0a6-4439-a313-7b26ae6ee15b.jpg?1783937455"
    }
}

private val DronepackKindred = card("Dronepack Kindred") {
    manaCost = ""
    colorIdentity = "R"
    typeLine = "Creature — Eldrazi Werewolf"
    power = 5
    toughness = 7
    oracleText = "Trample\n" +
        "{1}: This creature gets +1/+0 until end of turn."

    keywords(Keyword.TRAMPLE)

    activatedAbility {
        cost = Costs.Mana("{1}")
        effect = Effects.ModifyStats(1, 0, EffectTarget.Self)
        description = "This creature gets +1/+0 until end of turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "148"
        artist = "Jose Cabrera"
        flavorText = "These new terrors will not recede with the setting of the moon."
        imageUri = "https://cards.scryfall.io/normal/back/4/6/460f7733-c0a6-4439-a313-7b26ae6ee15b.jpg?1783937455"
    }
}

val VildinPackOutcast: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = VildinPackOutcastFront,
    backFace = DronepackKindred,
)
