package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedByMoreThan
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Kessig Prowler // Sinuous Predator (Eldritch Moon #163)
 * {G}
 * Creature — Werewolf Horror 2/1 // Creature — Eldrazi Werewolf 4/4
 *
 * Front — "{4}{G}: Transform this creature."
 * Back  — "This creature can't be blocked by more than one creature."
 *
 * The flip is an ordinary activated [Effects.Transform] (Shrill Howler's shape). The back is a
 * colorless Eldrazi Werewolf (no mana cost, no color indicator); `colorIdentity` stays "G" because
 * identity is a property of the whole card. The evasion is Norwood Riders' [CantBeBlockedByMoreThan].
 */

private val KessigProwlerFront = card("Kessig Prowler") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Creature — Werewolf Horror"
    power = 2
    toughness = 1
    oracleText = "{4}{G}: Transform this creature."

    activatedAbility {
        cost = Costs.Mana("{4}{G}")
        effect = Effects.Transform(EffectTarget.Self)
        description = "Transform this creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "163"
        artist = "Slawomir Maniak"
        flavorText = "In the wake of Emrakul, some werewolves' human forms were lost forever."
        imageUri = "https://cards.scryfall.io/normal/front/f/8/f89f116a-1e8e-4ae7-be39-552e4954f229.jpg?1783937448"
    }
}

private val SinuousPredator = card("Sinuous Predator") {
    manaCost = ""
    colorIdentity = "G"
    typeLine = "Creature — Eldrazi Werewolf"
    power = 4
    toughness = 4
    oracleText = "This creature can't be blocked by more than one creature."

    staticAbility {
        ability = CantBeBlockedByMoreThan(maxBlockers = 1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "163"
        artist = "Slawomir Maniak"
        flavorText = "A different kind of transformation has taken place."
        imageUri = "https://cards.scryfall.io/normal/back/f/8/f89f116a-1e8e-4ae7-be39-552e4954f229.jpg?1783937448"
        ruling(
            "2016-07-13",
            "If Sinuous Predator gains menace, it can't be blocked by only one creature and it can't be " +
                "blocked by more than one creature, so it simply can't be blocked.",
        )
    }
}

val KessigProwler: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = KessigProwlerFront,
    backFace = SinuousPredator,
)
