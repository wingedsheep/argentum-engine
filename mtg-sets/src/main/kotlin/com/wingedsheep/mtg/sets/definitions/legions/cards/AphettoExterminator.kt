package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Aphetto Exterminator
 * {2}{B}
 * Creature — Human Wizard
 * 3/1
 * Morph {3}{B}
 * When this creature is turned face up, target creature gets -3/-3 until end of turn.
 */
val AphettoExterminator = card("Aphetto Exterminator") {
    manaCost = "{2}{B}"
    typeLine = "Creature — Human Wizard"
    power = 3
    toughness = 1
    oracleText = "Morph {3}{B} (You may cast this card face down as a 2/2 creature for {3}. Turn it face up any time for its morph cost.)\nWhen this creature is turned face up, target creature gets -3/-3 until end of turn."

    triggeredAbility {
        trigger = Triggers.TurnedFaceUp
        val t = target("target creature", TargetCreature())
        effect = Effects.ModifyStats(-3, -3, t)
    }

    morph = "{3}{B}"

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "59"
        artist = "Scott M. Fischer"
        imageUri = "https://cards.scryfall.io/normal/front/0/6/06be8f63-daf2-4dbe-bb07-2b246145cdab.jpg?1562896334"
    }
}
