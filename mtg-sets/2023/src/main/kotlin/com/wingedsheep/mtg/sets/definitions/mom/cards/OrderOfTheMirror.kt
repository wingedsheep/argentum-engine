package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Order of the Mirror // Order of the Alabaster Host (March of the Machine #72)
 * {1}{U} Creature — Human Knight 2/1 // Creature — Phyrexian Knight 3/3 (blue-white color indicator)
 *
 * Front — "{3}{W/P}: Transform this creature. Activate only as a sorcery."
 * Back  — "Whenever this creature becomes blocked by a creature, the blocking creature gets -1/-1
 *          until end of turn."
 */
private val OrderOfTheMirrorFront = card("Order of the Mirror") {
    manaCost = "{1}{U}"
    colorIdentity = "WU"
    typeLine = "Creature — Human Knight"
    power = 2
    toughness = 1
    oracleText = "{3}{W/P}: Transform this creature. Activate only as a sorcery. " +
        "({W/P} can be paid with either {W} or 2 life.)"

    activatedAbility {
        cost = Costs.Mana("{3}{W/P}")
        effect = Effects.Transform(EffectTarget.Self)
        timing = TimingRule.SorcerySpeed
        description = "Transform this creature."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "72"
        artist = "Andrew Mar"
        flavorText = "The Magic Mirror of Vantress showed Catrina a twisted vision of black oil and bloody machines."
        imageUri = "https://cards.scryfall.io/normal/front/1/0/103295ed-5ddc-4528-8848-4fd2cfec4b88.jpg?1783917036"
    }
}

private val OrderOfTheAlabasterHost = card("Order of the Alabaster Host") {
    manaCost = ""
    colorIndicator = "WU" // Transformed back face, no mana cost (CR 204).
    colorIdentity = "WU"
    typeLine = "Creature — Phyrexian Knight"
    power = 3
    toughness = 3
    oracleText = "Whenever this creature becomes blocked by a creature, the blocking creature gets -1/-1 until end of turn."

    triggeredAbility {
        trigger = Triggers.self.becomesBlocked(by = GameObjectFilter.Creature)
        effect = Effects.ModifyStats(-1, -1, EffectTarget.TriggeringEntity)
        description = "Whenever this creature becomes blocked by a creature, the blocking creature gets -1/-1 until end of turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "72"
        artist = "Andrew Mar"
        flavorText = "Soon she understood that it was not the vision that had been twisted, but her own mind."
        imageUri = "https://cards.scryfall.io/normal/back/1/0/103295ed-5ddc-4528-8848-4fd2cfec4b88.jpg?1783917036"
    }
}

val OrderOfTheMirror: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = OrderOfTheMirrorFront,
    backFace = OrderOfTheAlabasterHost,
)
