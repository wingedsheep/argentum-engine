package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Hypnotic Sprite // Mesmeric Glare
 * {U}{U}
 * Creature — Faerie
 * 2/1
 *
 * Flying
 *
 * Adventure: Mesmeric Glare — {2}{U}, Instant — Adventure
 * Counter target spell with mana value 3 or less.
 */
val HypnoticSprite = card("Hypnotic Sprite") {
    manaCost = "{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Faerie"
    power = 2
    toughness = 1
    oracleText = "Flying"

    keywords(Keyword.FLYING)

    adventure("Mesmeric Glare") {
        manaCost = "{2}{U}"
        typeLine = "Instant — Adventure"
        oracleText = "Counter target spell with mana value 3 or less. " +
            "(Then exile this card. You may cast the creature later from exile.)"
        spell {
            target(TargetFilter.SpellOnStack.manaValueAtMost(3))
            effect = Effects.CounterSpell()
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "49"
        artist = "Irina Nordsol"
        imageUri = "https://cards.scryfall.io/normal/front/7/a/7acbd812-b994-4e68-8f95-04222796e994.jpg?1783932656"
    }
}
