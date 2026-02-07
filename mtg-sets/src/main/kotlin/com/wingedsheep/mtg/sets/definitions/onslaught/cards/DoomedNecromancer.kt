package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.MoveToZoneEffect

/**
 * Doomed Necromancer
 * {2}{B}
 * Creature — Human Cleric Mercenary
 * 2/2
 * {B}, {T}, Sacrifice Doomed Necromancer: Return target creature card from your graveyard to the battlefield.
 *
 * Oracle errata: Type line updated from "Human Cleric Wizard" to "Human Cleric Mercenary"
 */
val DoomedNecromancer = card("Doomed Necromancer") {
    manaCost = "{2}{B}"
    typeLine = "Creature — Human Cleric Mercenary"
    power = 2
    toughness = 2

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{B}"), Costs.Tap, Costs.SacrificeSelf)
        target = Targets.CreatureCardInYourGraveyard
        effect = MoveToZoneEffect(EffectTarget.ContextTarget(0), Zone.BATTLEFIELD)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "140"
        artist = "Mark Brill"
        flavorText = "\"His sacrifice shall not be forgotten. Now toss his body over there with the others.\"\n—Phage the Untouchable"
        imageUri = "https://cards.scryfall.io/normal/front/3/c/3ca3e348-47cc-41d6-999a-60d1206aaf06.jpg?1562909264"
    }
}
