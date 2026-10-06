package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Drannith Stinger
 * {1}{R}
 * Creature — Human Wizard
 * 2/2
 * Whenever you cycle another card, this creature deals 1 damage to each opponent.
 * Cycling {1} ({1}, Discard this card: Draw a card.)
 *
 * "Another" needs no filter: the trigger only functions on the battlefield, and a card can only
 * be cycled from hand, so the Stinger cycling itself never reaches it.
 */
val DrannithStinger = card("Drannith Stinger") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Wizard"
    power = 2
    toughness = 2
    oracleText = "Whenever you cycle another card, this creature deals 1 damage to each opponent.\n" +
        "Cycling {1} ({1}, Discard this card: Draw a card.)"

    triggeredAbility {
        trigger = Triggers.you.cycles()
        effect = Effects.DealDamage(1, EffectTarget.PlayerRef(Player.EachOpponent))
        description = "Whenever you cycle another card, this creature deals 1 damage to each opponent."
    }

    keywordAbility(KeywordAbility.cycling("{1}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "113"
        artist = "Denman Rooke"
        flavorText = "The first line of defense, and the last person you want to anger."
        imageUri = "https://cards.scryfall.io/normal/front/6/1/612ee4be-e7a2-423c-a37c-7c6ca97f630e.jpg?1783931051"
    }
}
