package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Wight
 * {1}{B}
 * Creature — Zombie Soldier
 * 3/2
 * This creature enters tapped.
 * Life Drain — Whenever a creature dealt damage by this creature this turn dies, create a tapped
 * 2/2 black Zombie creature token and exile that card.
 *
 * The token is always created; the dead card is exiled only if it is still in the graveyard when
 * the ability resolves (`fromZone = GRAVEYARD` skips the move otherwise — and a token that died
 * has already ceased to exist).
 */
val Wight = card("Wight") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie Soldier"
    power = 3
    toughness = 2
    oracleText = "This creature enters tapped.\n" +
        "Life Drain — Whenever a creature dealt damage by this creature this turn dies, create a tapped " +
        "2/2 black Zombie creature token and exile that card."

    replacementEffect(EntersTapped())

    triggeredAbility {
        trigger = Triggers.self.damagedCreatureDies()
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Zombie"),
            tapped = true,
            imageUri = "https://cards.scryfall.io/normal/front/0/b/0b08d210-01cb-46c5-9150-4dfb47f50ae7.jpg?1783926346"
        ) then Effects.Exile(EffectTarget.TriggeringEntity, fromZone = Zone.GRAVEYARD)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "127"
        artist = "Slawomir Maniak"
        flavorText = "Wights want the world to resound with echoes of their own cruel hatred."
        imageUri = "https://cards.scryfall.io/normal/front/f/6/f62abe9c-7599-433b-a964-10d6641f3586.jpg?1783926485"
    }
}
