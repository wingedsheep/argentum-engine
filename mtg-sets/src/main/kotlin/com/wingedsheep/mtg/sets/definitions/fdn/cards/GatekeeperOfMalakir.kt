package com.wingedsheep.mtg.sets.definitions.fdn.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.WasKicked
import com.wingedsheep.sdk.scripting.effects.ForceSacrificeEffect

/**
 * Gatekeeper of Malakir
 * {B}{B}
 * Creature — Vampire Warrior
 * 2/2
 *
 * Kicker {B} (You may pay an additional {B} as you cast this spell.)
 * When this creature enters, if it was kicked, target player sacrifices a creature of their choice.
 */
val GatekeeperOfMalakir = card("Gatekeeper of Malakir") {
    manaCost = "{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire Warrior"
    power = 2
    toughness = 2
    oracleText = "Kicker {B} (You may pay an additional {B} as you cast this spell.)\nWhen this creature enters, if it was kicked, target player sacrifices a creature of their choice."

    keywordAbility(KeywordAbility.Kicker(ManaCost.parse("{B}")))

    // When this creature enters, if it was kicked, target player sacrifices a creature of their choice.
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        triggerCondition = WasKicked
        val player = target("target player", Targets.Player)
        effect = ForceSacrificeEffect(GameObjectFilter.Creature, 1, player)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "752"
        artist = "Karl Kopinski"
        imageUri = "https://cards.scryfall.io/normal/front/5/8/58c45158-7858-409e-ab37-8a9a66ad8714.jpg?1775599837"
        flavorText = "\"You may enter the city—once the toll is paid.\""
    }
}
