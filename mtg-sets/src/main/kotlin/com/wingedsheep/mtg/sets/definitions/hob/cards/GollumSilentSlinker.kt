package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Gollum, Silent Slinker // Meager Meal — The Hobbit #71
 * {3}{B} · Legendary Creature — Halfling Horror · Common
 * 4/3
 *
 * Menace
 *
 * Adventure: Meager Meal — {B}, Sorcery — Adventure
 * Put a +1/+1 counter on up to one target creature. Target player gains 2 life.
 *
 * The Adventure takes two targets: the creature is "up to one" ([Targets.UpToCreatures]) so the
 * spell is castable with no creatures on the battlefield, while the player is a required target —
 * declaring no creature never makes the life gain optional. Named targets keep the two apart, so a
 * creature that becomes illegal by resolution only skips the counter; the life gain still happens.
 *
 * The **required player target is declared first** and the optional creature second, which is the
 * reverse of the oracle sentence order. That is deliberate: cast-time targets are matched to
 * requirements by fixed-width positional slots, so a declined *leading* optional target would let
 * the player slide into the creature's slot and be rejected. A declined *trailing* optional target
 * is simply an absent slot. CR 601.2c fixes no order between separate instances of "target", and
 * the effects bind by name rather than by index, so nothing else moves.
 *
 * (CR 715: Adventure cards. Casting the Adventure exiles the card on resolution and lets the caster
 * cast it as the creature spell while it remains in exile.)
 */
val GollumSilentSlinker = card("Gollum, Silent Slinker") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Halfling Horror"
    power = 4
    toughness = 3
    oracleText = "Menace (This creature can't be blocked except by two or more creatures.)"

    keywords(Keyword.MENACE)

    adventure("Meager Meal") {
        manaCost = "{B}"
        typeLine = "Sorcery — Adventure"
        oracleText = "Put a +1/+1 counter on up to one target creature. Target player gains 2 life. " +
            "(Then exile this card. You may cast the creature later from exile.)"
        spell {
            val player = target("player", Targets.Player)
            val creature = target("creature", Targets.UpToCreatures(1))
            effect = Effects.Composite(
                Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, creature),
                Effects.GainLife(2, player)
            )
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "71"
        artist = "Miklós Ligeti"
        flavorText = "He rowed about quietly on the lake. Never a ripple did he make, not he."
        imageUri = "https://cards.scryfall.io/normal/front/6/c/6cfaa182-3fec-4907-8814-b4d29c33cec3.jpg?1785323234"
    }
}
