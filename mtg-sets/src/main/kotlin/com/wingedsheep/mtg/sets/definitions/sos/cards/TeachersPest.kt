package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Teacher's Pest
 * {B}{G}
 * Creature — Skeleton Pest
 * 1/1
 *
 * Menace (This creature can't be blocked except by two or more creatures.)
 * Whenever this creature attacks, you gain 1 life.
 * {B}{G}: Return this card from your graveyard to the battlefield tapped.
 *
 * The recursion ability is activated from the graveyard (`activateFromZone = GRAVEYARD`)
 * and returns the card to the battlefield tapped via `PutOntoBattlefield(Self, tapped)`.
 */
val TeachersPest = card("Teacher's Pest") {
    manaCost = "{B}{G}"
    colorIdentity = "BG"
    typeLine = "Creature — Skeleton Pest"
    power = 1
    toughness = 1
    oracleText = "Menace (This creature can't be blocked except by two or more creatures.)\n" +
        "Whenever this creature attacks, you gain 1 life.\n" +
        "{B}{G}: Return this card from your graveyard to the battlefield tapped."

    keywords(Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.Attacks
        effect = Effects.GainLife(1)
        description = "Whenever this creature attacks, you gain 1 life."
    }

    activatedAbility {
        cost = Costs.Mana("{B}{G}")
        effect = Effects.PutOntoBattlefield(EffectTarget.Self, tapped = true)
        activateFromZone = Zone.GRAVEYARD
        description = "Return this card from your graveyard to the battlefield tapped."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "238"
        artist = "Stephanie Cheung"
        flavorText = "\"Stop playing dead, Rinata. No one's believed you for a decade.\"\n—Moseo, dean of the vein"
        imageUri = "https://cards.scryfall.io/normal/front/e/a/eaa358ac-761d-4507-aa15-3d4684027207.jpg?1775938661"
    }
}
