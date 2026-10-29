package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Obsessive Stitcher — Core Set 2021 #223
 * {1}{U}{B} · Creature — Human Wizard · 0/3
 *
 * {T}: Draw a card, then discard a card.
 * {2}{U}{B}, {T}, Sacrifice this creature: Return target creature card from your graveyard to the
 * battlefield.
 *
 * The looter half is one composite effect, so the draw and discard both happen while the ability
 * resolves. The reanimation targets a card already in the graveyard as the ability is put on the
 * stack — the Stitcher is still on the battlefield then, so it can't target itself (ruling).
 */
val ObsessiveStitcher = card("Obsessive Stitcher") {
    manaCost = "{1}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Creature — Human Wizard"
    power = 0
    toughness = 3
    oracleText = "{T}: Draw a card, then discard a card.\n" +
        "{2}{U}{B}, {T}, Sacrifice this creature: Return target creature card from your graveyard to the battlefield."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.DrawCards(1) then Patterns.Hand.discardCards(1)
        description = "{T}: Draw a card, then discard a card."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{U}{B}"), Costs.Tap, Costs.SacrificeSelf)
        val creatureCard = target(TargetFilter.CreatureInYourGraveyard)
        effect = Effects.PutOntoBattlefieldFromGraveyard(creatureCard)
        description = "{2}{U}{B}, {T}, Sacrifice this creature: Return target creature card from your graveyard to the battlefield."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "223"
        artist = "Joe Slucher"
        flavorText = "No one is as dedicated to perfection as a stitcher working on their latest masterpiece."
        imageUri = "https://cards.scryfall.io/normal/front/e/4/e449f985-d149-49eb-87d9-7a325215f82d.jpg?1783930661"
        ruling("2020-06-23", "Obsessive Stitcher can't be the target of its last ability.")
    }
}
