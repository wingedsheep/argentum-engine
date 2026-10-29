package com.wingedsheep.mtg.sets.definitions.tdc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CastSpellTypesFromTopOfLibrary
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.LookAtTopOfLibrary

/**
 * Thundermane Dragon — Tarkir: Dragonstorm Commander #38 (canonical printing)
 * {3}{R} · Creature — Dragon · 4/4
 *
 * Flying
 * You may look at the top card of your library any time.
 * You may cast creature spells with power 4 or greater from the top of your library. If you cast
 * a creature spell this way, it gains haste until end of turn.
 *
 * The haste is the permission's cast-this-way rider (`castThisWayKeyword`), frozen onto the spell
 * as it is cast, so the creature keeps it even if the Dragon leaves before the spell resolves.
 */
val ThundermaneDragon = card("Thundermane Dragon") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Dragon"
    power = 4
    toughness = 4
    oracleText = "Flying\n" +
        "You may look at the top card of your library any time.\n" +
        "You may cast creature spells with power 4 or greater from the top of your library. If you cast a creature spell this way, it gains haste until end of turn."

    keywords(Keyword.FLYING)

    staticAbility {
        ability = LookAtTopOfLibrary()
    }

    staticAbility {
        ability = CastSpellTypesFromTopOfLibrary(
            filter = GameObjectFilter.Creature.powerAtLeast(4),
            castThisWayKeyword = Keyword.HASTE,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "38"
        artist = "Slawomir Maniak"
        imageUri = "https://cards.scryfall.io/normal/front/7/c/7ca93b31-d6ec-4265-b2ff-d46090086f07.jpg?1783907172"
        ruling("2025-04-04", "Thundermane Dragon lets you look at the top card of your library whenever you want (with one restriction—see below), even if you don't have priority. This action doesn't use the stack. Knowing what that card is becomes part of the information you have access to, just like you can look at the cards in your hand.")
        ruling("2025-04-04", "If the top card of your library changes while you're casting a spell, playing a land, activating an ability, or taking a special action, you can't look at the new top card until you finish doing so. This means that if you cast the top card of your library, you can't look at the next one until you're done paying for that spell.")
        ruling("2025-04-04", "Thundermane Dragon's last ability allows you to cast the top card of your library if the resulting spell is a creature spell with power 4 or greater, it's your main phase, and the stack is empty. If that creature spell has flash, you'll be able to cast it any time you could cast an instant, even on an opponent's turn.")
    }
}
