package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Invasion of Shandalar // Leyline Surge — March of the Machine #193.
 * {3}{G}{G} · Battle — Siege · defense 4 // Enchantment
 *
 * Front: an enters trigger with up to three optional graveyard targets (permanent cards you
 * own), returned to hand through the chosen-targets pipeline (Life from the Loam's shape).
 * Back: an upkeep trigger over [Patterns.Hand.putFromHand] — its `ChooseUpTo(1)` is the "may".
 */
private val InvasionOfShandalarFront = card("Invasion of Shandalar") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Battle — Siege"
    startingDefense = 4
    oracleText = "(As a Siege enters, choose an opponent to protect it. You and others can attack " +
        "it. When it's defeated, exile it, then cast it transformed.)\n" +
        "When this Siege enters, return up to three target permanent cards from your graveyard to your hand."

    triggeredAbility {
        trigger = Triggers.self.enters()
        targets(
            TargetFilter(GameObjectFilter.Permanent.ownedByYou(), zone = Zone.GRAVEYARD),
            count = 3,
            optional = true,
        )
        effect = Effects.Pipeline {
            val cards = gather(CardSource.ChosenTargets)
            toHand(cards)
        }
        description = "When this Siege enters, return up to three target permanent cards from your " +
            "graveyard to your hand."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "193"
        artist = "Adam Paquette"
        imageUri = "https://cards.scryfall.io/normal/front/5/f/5f80764e-8fa2-44e2-84c6-3b55f1c40ee7.jpg?1783916974"
        ruling("2023-04-14", "Sieges each have an intrinsic triggered ability. That ability is \"When the last defense counter is removed from this permanent, exile it, then you may cast it transformed without paying its mana cost.\"")
        ruling("2023-04-14", "As a Siege enters the battlefield, its controller chooses an opponent to be its protector.")
    }
}

private val LeylineSurge = card("Leyline Surge") {
    manaCost = ""
    colorIdentity = "G"
    colorIndicator = "G"
    typeLine = "Enchantment"
    oracleText = "At the beginning of your upkeep, you may put a permanent card from your hand onto the battlefield."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Patterns.Hand.putFromHand(filter = GameObjectFilter.Permanent)
        description = "At the beginning of your upkeep, you may put a permanent card from your hand onto the battlefield."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "193"
        artist = "Adam Paquette"
        flavorText = "Shandalar's wild magic stripped away the unnatural Phyrexian carapaces, " +
            "reclaiming the true forms hidden underneath."
        imageUri = "https://cards.scryfall.io/normal/back/5/f/5f80764e-8fa2-44e2-84c6-3b55f1c40ee7.jpg?1783916974"
    }
}

val InvasionOfShandalar: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = InvasionOfShandalarFront,
    backFace = LeylineSurge,
)
