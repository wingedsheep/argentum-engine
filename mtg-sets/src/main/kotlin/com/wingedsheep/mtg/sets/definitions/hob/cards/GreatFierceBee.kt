package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Great Fierce Bee
 * {2}{B}
 * Creature — Insect
 * 2/2
 * Flying
 * Whenever one or more other creatures die, scry 1.
 *
 * The trigger is the *batched* death shape — a board wipe scries once, not once per creature
 * (CR 603.3b). `anyController()` widens it past "you control" to every player's creatures, and
 * `excludeSelf = true` carries the "other" in the oracle text.
 */
val GreatFierceBee = card("Great Fierce Bee") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Insect"
    oracleText = "Flying\nWhenever one or more other creatures die, scry 1. " +
        "(Look at the top card of your library. You may put that card on the bottom.)"
    power = 2
    toughness = 2
    keywords(Keyword.FLYING)
    triggeredAbility {
        trigger = Triggers.OneOrMoreCreaturesYouControlDie(
            filter = GameObjectFilter.Creature.anyController(),
            excludeSelf = true,
        )
        effect = Effects.Scry(1)
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "73"
        artist = "Leesha Hannigan"
        flavorText = "\"If one were to sting me, I should swell up as big again as I am!\"\n—Bilbo"
        imageUri = "https://cards.scryfall.io/normal/front/9/d/9d9ef88f-d208-4788-9553-cd672b3be1fe.jpg?1785497086"
    }
}
