package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.PreventDamage
import com.wingedsheep.sdk.scripting.ProtectionScope
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Wall of Putrid Flesh — Legends #127
 * {2}{B} · Creature — Wall · 2/4
 *
 * Defender
 * Protection from white
 * Prevent all damage that would be dealt to this creature by enchanted creatures.
 *
 * Enchanted Being's [PreventDamage] replacement (source = creatures with an Aura attached,
 * `GameObjectFilter.Creature.enchanted()`) without the combat-only restriction: damage from an
 * enchanted creature's abilities is prevented too.
 */
val WallOfPutridFlesh = card("Wall of Putrid Flesh") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Wall"
    power = 2
    toughness = 4
    oracleText = "Defender (This creature can't attack.)\nProtection from white\n" +
        "Prevent all damage that would be dealt to this creature by enchanted creatures."

    keywords(Keyword.DEFENDER)
    keywordAbility(KeywordAbility.Protection(ProtectionScope.Color(Color.WHITE)))

    replacementEffect(
        PreventDamage(
            amount = null,
            appliesTo = EventPattern.DamageEvent(
                recipient = Recipient.Self,
                source = GameObjectFilter.Creature.enchanted()
            )
        )
    )

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "127"
        artist = "Richard Thomas"
        imageUri = "https://cards.scryfall.io/normal/front/0/7/07a17b74-a9c9-419a-8369-9ab4fec213f2.jpg?1783948061"
    }
}
