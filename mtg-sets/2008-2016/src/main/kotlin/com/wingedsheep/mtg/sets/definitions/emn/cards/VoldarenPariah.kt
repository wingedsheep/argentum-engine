package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.madness
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Voldaren Pariah // Abolisher of Bloodlines (Eldritch Moon #111)
 *
 * Front: {3}{B}{B} Creature — Vampire Horror 3/3
 * Flying
 * Sacrifice three other creatures: Transform this creature.
 * Madness {B}{B}{B}
 *
 * Back: Creature — Eldrazi Vampire 6/5 (colorless)
 * Flying
 * When this creature transforms into Abolisher of Bloodlines, target opponent sacrifices three
 * creatures of their choice.
 *
 * - "Three other creatures" is [Costs.SacrificeMultiple] over a `notSourceItself()` creature
 *   filter, so the Pariah can't feed itself to its own cost.
 * - The back face's trigger is `Triggers.self.transforms(true)`: it lives on the face that ends up
 *   up, and fires only on a front-to-back transform.
 * - An opponent with fewer than three creatures sacrifices as many as they can (ruling) — the
 *   force-sacrifice takes what's there.
 */
private val VoldarenPariahFront = card("Voldaren Pariah") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire Horror"
    power = 3
    toughness = 3
    oracleText = "Flying\n" +
        "Sacrifice three other creatures: Transform this creature.\n" +
        "Madness {B}{B}{B} (If you discard this card, discard it into exile. When you do, cast it " +
        "for its madness cost or put it into your graveyard.)"

    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.SacrificeMultiple(3, GameObjectFilter.Creature.notSourceItself())
        effect = Effects.Transform(EffectTarget.Self)
        description = "Sacrifice three other creatures: Transform this creature."
    }

    madness("{B}{B}{B}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "111"
        artist = "James Ryman"
        imageUri = "https://cards.scryfall.io/normal/front/2/5/25baac6c-5bb4-4ecc-b1d5-fced52087bd9.jpg?1783937479"
    }
}

private val AbolisherOfBloodlines = card("Abolisher of Bloodlines") {
    manaCost = ""
    colorIdentity = "B"
    typeLine = "Creature — Eldrazi Vampire"
    power = 6
    toughness = 5
    oracleText = "Flying\n" +
        "When this creature transforms into Abolisher of Bloodlines, target opponent sacrifices " +
        "three creatures of their choice."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.transforms(true)
        val opponent = target(Targets.Opponent)
        effect = Effects.Sacrifice(GameObjectFilter.Creature, 3, opponent)
        description = "When this creature transforms into Abolisher of Bloodlines, target opponent " +
            "sacrifices three creatures of their choice."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "111"
        artist = "James Ryman"
        flavorText = "Even if the rain turned to blood, her thirst would not be slaked."
        imageUri = "https://cards.scryfall.io/normal/back/2/5/25baac6c-5bb4-4ecc-b1d5-fced52087bd9.jpg?1783937479"
        ruling(
            "2016-07-13",
            "If the targeted opponent doesn't control three creatures as the triggered ability of " +
                "Abolisher of Bloodlines resolves, that player sacrifices as many creatures as able."
        )
    }
}

val VoldarenPariah: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = VoldarenPariahFront,
    backFace = AbolisherOfBloodlines,
)
