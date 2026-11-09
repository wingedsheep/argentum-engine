package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Avacynian Missionaries // Lunarch Inquisitors (Shadows over Innistrad #6)
 * {3}{W}
 * Creature — Human Cleric 3/3 // Creature — Human Cleric 4/4
 *
 * Front — "At the beginning of your end step, if this creature is equipped, transform it."
 * Back  — "When this creature transforms into Lunarch Inquisitors, you may exile another target
 *         creature until this creature leaves the battlefield."
 *
 * "If this creature is equipped" is an intervening "if" (CR 603.4): checked as the end step begins
 * and again on resolution, so it rides on `interveningIf`.
 *
 * The exile is a one-shot "until" duration ([Effects.MoveUntilSourceLeaves]), not a leaves trigger:
 * nothing is exiled if the Inquisitors have left before the trigger resolves (first ruling), and the
 * creature returns the moment this permanent leaves — even if it has transformed back into Avacynian
 * Missionaries in between, since transforming doesn't end its battlefield visit (second ruling).
 * The target is mandatory once a creature is available; the "you may" is the resolution-time choice
 * to exile it.
 *
 * The back face has no mana cost, so its white comes from a color indicator (CR 204).
 */

private val AvacynianMissionariesFront = card("Avacynian Missionaries") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Cleric"
    power = 3
    toughness = 3
    oracleText = "At the beginning of your end step, if this creature is equipped, transform it."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        interveningIf = Conditions.SourceMatches(GameObjectFilter.Any.equipped())
        effect = Effects.Transform(EffectTarget.Self)
        description = "At the beginning of your end step, if this creature is equipped, transform it."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "6"
        artist = "Wayne Reynolds"
        flavorText = "\"We come bearing the word of Avacyn and the Lunarch Council.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/0/1001e708-e4f9-456a-8b9d-3421a2a3833d.jpg?1783937831"
        ruling(
            "2016-04-08",
            "The Equipment attached to Avacynian Missionaries remains attached after it transforms into " +
                "Lunarch Inquisitors."
        )
    }
}

private val LunarchInquisitors = card("Lunarch Inquisitors") {
    manaCost = ""
    colorIdentity = "W"
    colorIndicator = "W" // Transformed back face, no mana cost (CR 204).
    typeLine = "Creature — Human Cleric"
    power = 4
    toughness = 4
    oracleText = "When this creature transforms into Lunarch Inquisitors, you may exile another target " +
        "creature until this creature leaves the battlefield."

    triggeredAbility {
        trigger = Triggers.self.transforms(true)
        val creature = target(TargetFilter.OtherCreature)
        effect = Effects.May(effect = Effects.MoveUntilSourceLeaves(creature, Zone.EXILE))
        description = "When this creature transforms into Lunarch Inquisitors, you may exile another " +
            "target creature until this creature leaves the battlefield."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "6"
        artist = "Wayne Reynolds"
        flavorText = "\"We come bearing the tools of confession and absolution.\""
        imageUri = "https://cards.scryfall.io/normal/back/1/0/1001e708-e4f9-456a-8b9d-3421a2a3833d.jpg?1783937831"
        ruling(
            "2016-04-08",
            "If Lunarch Inquisitors leaves the battlefield before its triggered ability resolves, the " +
                "target creature won't be exiled."
        )
        ruling(
            "2016-04-08",
            "If Lunarch Inquisitors somehow transforms into Avacynian Missionaries, the creature will " +
                "remain exiled. If Avacynian Missionaries later leaves the battlefield, the creature will " +
                "return to the battlefield."
        )
        ruling("2016-04-08", "If a creature token is exiled, it ceases to exist. It won't be returned to the battlefield.")
    }
}

val AvacynianMissionaries: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = AvacynianMissionariesFront,
    backFace = LunarchInquisitors,
)
