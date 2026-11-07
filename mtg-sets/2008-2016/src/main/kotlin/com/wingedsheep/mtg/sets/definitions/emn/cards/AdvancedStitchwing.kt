package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Advanced Stitchwing (Eldritch Moon #49)
 * {3}{U}{U}
 * Creature — Zombie Horror
 * 3/4
 *
 * Flying
 * {2}{U}, Discard two cards: Return this card from your graveyard to the battlefield tapped.
 *
 * The return is an activated ability that works only from the graveyard (`activateFromZone`).
 */
val AdvancedStitchwing = card("Advanced Stitchwing") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Zombie Horror"
    power = 3
    toughness = 4
    oracleText = "Flying\n" +
        "{2}{U}, Discard two cards: Return this card from your graveyard to the battlefield tapped."

    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{U}"), Costs.Discard(count = 2))
        effect = Effects.PutOntoBattlefieldFromGraveyard(EffectTarget.Self, tapped = true)
        activateFromZone = Zone.GRAVEYARD
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "49"
        artist = "Johann Bodin"
        flavorText = "\"Improvement is a consequence of practice.\"\n—Stitcher Geralf"
        imageUri = "https://cards.scryfall.io/normal/front/f/b/fbdb7f1c-6139-4e68-8706-01f373548a4f.jpg"
    }
}
