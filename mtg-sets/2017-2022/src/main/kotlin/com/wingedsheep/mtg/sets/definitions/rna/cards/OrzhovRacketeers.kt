package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Orzhov Racketeers — Ravnica Allegiance #80
 * {4}{B} · Creature — Human Rogue · 3/2
 *
 * Whenever this creature deals combat damage to a player, that player discards a card.
 * Afterlife 2 (When this creature dies, create two 1/1 white and black Spirit creature tokens with flying.)
 *
 * The discard is the damaged player's own choice (Blazing Specter shape); afterlife is composed as its
 * reminder text, the same shape as Debtors' Transport.
 */
val OrzhovRacketeers = card("Orzhov Racketeers") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Rogue"
    power = 3
    toughness = 2
    oracleText = "Whenever this creature deals combat damage to a player, that player discards a card.\n" +
        "Afterlife 2 (When this creature dies, create two 1/1 white and black Spirit creature tokens with flying.)"

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Patterns.Hand.discardCards(1, EffectTarget.PlayerRef(Player.TriggeringPlayer))
        description = "Whenever this creature deals combat damage to a player, that player discards a card."
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.CreateToken(
            count = 2,
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE, Color.BLACK),
            creatureTypes = setOf("Spirit"),
            keywords = setOf(Keyword.FLYING),
            imageUri = "https://cards.scryfall.io/normal/front/4/5/45b3bdd7-b093-4bfd-8a9a-965e72bfefb3.jpg?1783933602",
        )
        description = "Afterlife 2 (When this creature dies, create two 1/1 white and black Spirit " +
            "creature tokens with flying.)"
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "80"
        artist = "David Palumbo"
        flavorText = "\"Looks like you need protection.\""
        imageUri = "https://cards.scryfall.io/normal/front/2/9/29b89f68-72ba-493d-95c4-eb37751fbd3d.jpg?1783933692"
        ruling(
            "2019-01-25",
            "Because blockers are chosen all at once, you can't block with a creature with afterlife, wait for " +
                "it to die, then block with the resulting Spirit tokens."
        )
    }
}
