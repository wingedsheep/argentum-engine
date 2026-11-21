package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Blaze Commando (DGM #56)
 * {3}{R}{W} Creature — Minotaur Soldier, 5/3
 *
 * A per-instruction damage observer: it fires once each time an instant or sorcery spell you
 * control *deals damage* — once per "deals" in the spell's text, however many players or
 * permanents that instruction hits — not once per recipient (2013-04-15 ruling).
 */
val BlazeCommando = card("Blaze Commando") {
    manaCost = "{3}{R}{W}"
    colorIdentity = "RW"
    typeLine = "Creature — Minotaur Soldier"
    oracleText = "Whenever an instant or sorcery spell you control deals damage, create two 1/1 red and white Soldier creature tokens with haste."
    power = 5
    toughness = 3

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.InstantOrSorcery.youControl()).dealsDamage(perInstruction = true)
        effect = Effects.CreateToken(
            count = 2,
            power = 1,
            toughness = 1,
            colors = setOf(Color.RED, Color.WHITE),
            creatureTypes = setOf("Soldier"),
            keywords = setOf(Keyword.HASTE),
            imageUri = "https://cards.scryfall.io/normal/front/a/8/a8a5a5a5-a8ae-41ed-9af5-b99d80ce0b35.jpg?1783940147",
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "56"
        artist = "James Ryman"
        imageUri = "https://cards.scryfall.io/normal/front/5/e/5e179f0d-2965-44e4-8483-67b330a8608c.jpg?1783940033"
        ruling("2013-04-15", "Blaze Commando's ability triggers each time an instant or sorcery spell you control deals damage (or, put another way, the number of times the word \"deals\" appears in its instructions), no matter how much damage is dealt or how many players or permanents are dealt damage. For example, if you cast Punish the Enemy and it \"deals 3 damage to target player or planeswalker and 3 damage to target creature,\" Blaze Commando's ability will trigger once and you'll get two Soldier tokens.")
        ruling("2013-04-15", "Some instant or sorcery spells cause other objects to deal damage, rather than deal damage themselves. Such spells don't cause Blaze Commando to trigger.")
    }
}
