package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Kindly Stranger // Demon-Possessed Witch (Shadows over Innistrad #119)
 *
 * Front: {2}{B} Creature — Human 2/3
 * Delirium — {2}{B}: Transform this creature. Activate only if there are four or more card types
 * among cards in your graveyard.
 *
 * Back: Creature — Human Shaman 4/3 (black color indicator)
 * When this creature transforms into Demon-Possessed Witch, you may destroy target creature.
 *
 * - Delirium is checked only on activation, not on resolution (ruling) — an
 *   [ActivationRestriction.OnlyIfCondition], like Stallion of Ashmouth.
 * - Two stacked activations transform it only once (CR 701.27g, enforced by the transform
 *   executor), so the Witch never flips back.
 * - The target is chosen as the trigger goes on the stack; "you may" is the resolution-time choice
 *   ([Effects.May]), mirroring Lunarch Inquisitors.
 */
private val KindlyStrangerFront = card("Kindly Stranger") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human"
    power = 2
    toughness = 3
    oracleText = "Delirium — {2}{B}: Transform this creature. Activate only if there are four or more " +
        "card types among cards in your graveyard."

    activatedAbility {
        cost = Costs.Mana("{2}{B}")
        effect = Effects.Transform(EffectTarget.Self)
        restrictions = listOf(ActivationRestriction.OnlyIfCondition(Conditions.Delirium()))
        description = "Delirium — {2}{B}: Transform this creature. Activate only if there are four or " +
            "more card types among cards in your graveyard."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "119"
        artist = "Ryan Pancoast"
        flavorText = "\"I'm not afraid out here, for I know that a higher power protects me.\""
        imageUri = "https://cards.scryfall.io/normal/front/e/3/e326d5f4-63e1-45fa-8c48-6fdc05709c52.jpg?1783937777"
        ruling(
            "2016-04-08",
            "Activating Kindly Stranger's ability twice won't cause it to transform back into Kindly " +
                "Stranger once it has already transformed into Demon-Possessed Witch."
        )
        ruling(
            "2016-04-08",
            "Some delirium abilities are activated abilities of permanents. To activate such an ability, " +
                "there must be four or more card types among cards in your graveyard. The number of card " +
                "types is not rechecked as the ability resolves."
        )
    }
}

private val DemonPossessedWitch = card("Demon-Possessed Witch") {
    manaCost = ""
    colorIdentity = "B"
    colorIndicator = "B" // Transformed back face, no mana cost (CR 204).
    typeLine = "Creature — Human Shaman"
    power = 4
    toughness = 3
    oracleText = "When this creature transforms into Demon-Possessed Witch, you may destroy target creature."

    triggeredAbility {
        trigger = Triggers.self.transforms(true)
        val creature = target(TargetFilter.Creature)
        effect = Effects.May(effect = Effects.Destroy(creature))
        description = "When this creature transforms into Demon-Possessed Witch, you may destroy target " +
            "creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "119"
        artist = "Ryan Pancoast"
        flavorText = "\"It looks like you could use some protection of your own.\""
        imageUri = "https://cards.scryfall.io/normal/back/e/3/e326d5f4-63e1-45fa-8c48-6fdc05709c52.jpg?1783937777"
    }
}

val KindlyStranger: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = KindlyStrangerFront,
    backFace = DemonPossessedWitch,
)
