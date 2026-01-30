package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.LookAtTargetHandEffect
import com.wingedsheep.sdk.scripting.OnEnterBattlefield
import com.wingedsheep.sdk.targeting.TargetOpponent

/**
 * Ingenious Thief
 * {1}{U}
 * Creature - Human Rogue
 * 1/1
 * Flying
 * When Ingenious Thief enters, look at target player's hand.
 */
val IngeniousThief = card("Ingenious Thief") {
    manaCost = "{1}{U}"
    typeLine = "Creature — Human Rogue"
    power = 1
    toughness = 1

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = OnEnterBattlefield()
        target = TargetOpponent()
        effect = LookAtTargetHandEffect(EffectTarget.ContextTarget(0))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "58"
        artist = "Dan Frazier"
        imageUri = "https://cards.scryfall.io/normal/front/b/e/be341805-b4de-456e-8b46-4ee5fdbca7e0.jpg"
    }
}
