package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Garland, Knight of Cornelia // Chaos, the Endless — Final Fantasy #221
 * {B}{R} · Legendary Creature — Human Knight 3/2
 * // Legendary Creature — Demon 5/5
 *
 * Front — Garland, Knight of Cornelia:
 *   Whenever you cast a noncreature spell, surveil 1.
 *   {3}{B}{B}{R}{R}: Return this card from your graveyard to the battlefield transformed.
 *   Activate only as a sorcery.
 *
 * Back — Chaos, the Endless:
 *   Flying
 *   When Chaos dies, put it on the bottom of its owner's library.
 *
 * The graveyard ability is an `activateFromZone = GRAVEYARD` activated ability resolving to
 * [Effects.ReturnSelfFromGraveyardTransformed] — the card enters as a new object with the
 * Chaos face up (no transform triggers; a DFC in the graveyard is front-face-up by
 * definition). Chaos's death trigger is a plain bottom-of-library move: by the time it is in
 * the graveyard the card has reverted to its front face (Rule 712.8a), and the trigger uses
 * last-known information to move the card wherever it went.
 */
private val ChaosTheEndless = card("Chaos, the Endless") {
    manaCost = ""
    colorIdentity = "BR"
    typeLine = "Legendary Creature — Demon"
    oracleText = "Flying\n" +
        "When Chaos dies, put it on the bottom of its owner's library."
    power = 5
    toughness = 5

    keywords(Keyword.FLYING)

    // When Chaos dies, put it on the bottom of its owner's library.
    triggeredAbility {
        trigger = Triggers.Dies
        effect = Effects.PutOnBottomOfLibrary(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "221"
        artist = "Billy Christian"
        imageUri = "https://cards.scryfall.io/normal/back/d/d/dd463dbe-5f2c-4d4f-86f8-ad8ff407af62.jpg?1782686428"
    }
}

private val GarlandKnightOfCorneliaFront = card("Garland, Knight of Cornelia") {
    manaCost = "{B}{R}"
    colorIdentity = "BR"
    typeLine = "Legendary Creature — Human Knight"
    oracleText = "Whenever you cast a noncreature spell, surveil 1. (Look at the top card of " +
        "your library. You may put it into your graveyard.)\n" +
        "{3}{B}{B}{R}{R}: Return this card from your graveyard to the battlefield transformed. " +
        "Activate only as a sorcery."
    power = 3
    toughness = 2

    // Whenever you cast a noncreature spell, surveil 1.
    triggeredAbility {
        trigger = Triggers.YouCastNoncreature
        effect = Effects.Surveil(1)
    }

    // {3}{B}{B}{R}{R}: Return this card from your graveyard to the battlefield transformed.
    // Activate only as a sorcery.
    activatedAbility {
        cost = Costs.Mana("{3}{B}{B}{R}{R}")
        timing = TimingRule.SorcerySpeed
        activateFromZone = Zone.GRAVEYARD
        effect = Effects.ReturnSelfFromGraveyardTransformed()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "221"
        artist = "Billy Christian"
        imageUri = "https://cards.scryfall.io/normal/front/d/d/dd463dbe-5f2c-4d4f-86f8-ad8ff407af62.jpg?1782686428"
    }
}

val GarlandKnightOfCornelia: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = GarlandKnightOfCorneliaFront,
    backFace = ChaosTheEndless,
)
