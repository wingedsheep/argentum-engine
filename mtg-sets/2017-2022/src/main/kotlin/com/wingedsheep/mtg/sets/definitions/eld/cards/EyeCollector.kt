package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Eye Collector
 * {B}
 * Creature — Faerie
 * 1/1
 *
 * Flying
 * Whenever this creature deals combat damage to a player, each player mills a card.
 */
val EyeCollector = card("Eye Collector") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Creature — Faerie"
    oracleText = "Flying\n" +
        "Whenever this creature deals combat damage to a player, each player mills a card. " +
        "(Each player puts the top card of their library into their graveyard.)"
    power = 1
    toughness = 1

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Patterns.Library.mill(1, EffectTarget.PlayerRef(Player.Each))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "86"
        artist = "Uriah Voth"
        flavorText = "\"Lord Rankle will see you now—all he asks is a small token of tribute.\""
        imageUri = "https://cards.scryfall.io/normal/front/2/b/2b9bb2f3-a7c7-4e8f-ba39-44e6acd9240b.jpg?1783932640"
    }
}
