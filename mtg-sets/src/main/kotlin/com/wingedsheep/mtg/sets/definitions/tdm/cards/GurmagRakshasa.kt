package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Gurmag Rakshasa
 * {4}{B}{B}
 * Creature — Demon
 * 5/5
 *
 * Menace
 * When this creature enters, target creature an opponent controls gets -2/-2 until end of turn
 * and target creature you control gets +2/+2 until end of turn.
 */
val GurmagRakshasa = card("Gurmag Rakshasa") {
    manaCost = "{4}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Demon"
    power = 5
    toughness = 5
    oracleText = "Menace (This creature can't be blocked except by two or more creatures.)\n" +
        "When this creature enters, target creature an opponent controls gets -2/-2 until end of turn " +
        "and target creature you control gets +2/+2 until end of turn."

    keywords(Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val opponentCreature = target("creature an opponent controls", Targets.CreatureOpponentControls)
        val yourCreature = target("creature you control", Targets.CreatureYouControl)
        effect = Effects.ModifyStats(-2, -2, opponentCreature)
            .then(Effects.ModifyStats(2, 2, yourCreature))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "81"
        artist = "Johan Grenier"
        flavorText = "\"It's a pity to hoard life essence in a form so weak. Give it to me.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/0/f05ad909-8860-473b-9a30-a322f7670b32.jpg?1743204286"
    }
}
