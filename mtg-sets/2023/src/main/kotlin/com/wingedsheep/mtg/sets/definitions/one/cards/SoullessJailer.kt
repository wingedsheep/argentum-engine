package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantEnterTheBattlefield
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PlayersCantCastSpells
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Soulless Jailer
 * {2}
 * Artifact Creature — Phyrexian Golem
 * 0/4
 * Permanent cards in graveyards can't enter the battlefield.
 * Players can't cast noncreature spells from graveyards or exile.
 *
 * Two zone-scoped locks: an entry prohibition watching graveyards (a permanent spell cast from a
 * graveyard still enters — from the stack), and the cast prohibition's "where" axis. Creature
 * spells stay castable from graveyards and exile, per the ruling.
 */
val SoullessJailer = card("Soulless Jailer") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Phyrexian Golem"
    oracleText = "Permanent cards in graveyards can't enter the battlefield.\n" +
        "Players can't cast noncreature spells from graveyards or exile."
    power = 0
    toughness = 4

    staticAbility {
        ability = CantEnterTheBattlefield(GameObjectFilter.Permanent, fromZones = setOf(Zone.GRAVEYARD))
    }
    staticAbility {
        ability = PlayersCantCastSpells(
            Player.Each,
            spellFilter = GameObjectFilter.Noncreature,
            fromZones = setOf(Zone.GRAVEYARD, Zone.EXILE)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "241"
        artist = "Donato Giancola"
        flavorText = "\"Lock the door and eat the key.\"\n—Vraan, Executioner Thane"
        imageUri = "https://cards.scryfall.io/normal/front/b/f/bf9991fd-ea6a-4ed7-b5f1-46a95f8d0634.jpg?1783917986"
        ruling(
            "2023-02-04",
            "Players may still cast creature spells from graveyards and from exile if an effect allows them to do so."
        )
        ruling(
            "2023-02-04",
            "Putting a permanent card onto the battlefield from a graveyard is an impossible action while Soulless Jailer is on the battlefield."
        )
    }
}
