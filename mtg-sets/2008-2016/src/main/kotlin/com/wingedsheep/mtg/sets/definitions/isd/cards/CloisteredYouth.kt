package com.wingedsheep.mtg.sets.definitions.isd.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Cloistered Youth // Unholy Fiend (Innistrad #8)
 * {1}{W}
 * Creature — Human 1/1 // Creature — Horror 3/3
 *
 * Front — "At the beginning of your upkeep, you may transform this creature."
 * Back  — "At the beginning of your end step, you lose 1 life."
 *
 * The back face has no mana cost, so its black comes from a color indicator (CR 204).
 */
private val CloisteredYouthFront = card("Cloistered Youth") {
    manaCost = "{1}{W}"
    colorIdentity = "WB"
    typeLine = "Creature — Human"
    power = 1
    toughness = 1
    oracleText = "At the beginning of your upkeep, you may transform this creature."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.May(Effects.Transform(EffectTarget.Self))
        description = "At the beginning of your upkeep, you may transform this creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "8"
        artist = "Igor Kieryluk"
        flavorText = "\"I heard her talking in her sleep—pleading, shrieking, snarling. It was not my daughter's voice. That is not my daughter.\"\n—Ekatrin, elder of Hanweir"
        imageUri = "https://cards.scryfall.io/normal/front/f/8/f8b8f0b4-71e1-4822-99a1-b1b3c2f10cb2.jpg"
    }
}

private val UnholyFiend = card("Unholy Fiend") {
    manaCost = ""
    colorIdentity = "WB"
    colorIndicator = "B" // Transformed back face, no mana cost (CR 204).
    typeLine = "Creature — Horror"
    power = 3
    toughness = 3
    oracleText = "At the beginning of your end step, you lose 1 life."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        effect = Effects.LoseLife(1, EffectTarget.Controller)
        description = "At the beginning of your end step, you lose 1 life."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "8"
        artist = "Igor Kieryluk"
        flavorText = "The fiend tormented them by recounting the girl's memories, as if some part of her remained inside that twisted shell."
        imageUri = "https://cards.scryfall.io/normal/back/f/8/f8b8f0b4-71e1-4822-99a1-b1b3c2f10cb2.jpg"
    }
}

val CloisteredYouth: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = CloisteredYouthFront,
    backFace = UnholyFiend,
)
