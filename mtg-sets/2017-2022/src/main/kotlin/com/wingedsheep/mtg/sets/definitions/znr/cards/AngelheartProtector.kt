package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Angelheart Protector — Zendikar Rising #3
 * {2}{W} · Creature — Human Cleric · 3/2
 *
 * When this creature enters, target creature you control gains indestructible until end of turn.
 */
val AngelheartProtector = card("Angelheart Protector") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Cleric"
    oracleText = "When this creature enters, target creature you control gains indestructible until end of turn. " +
        "(Damage and effects that say \"destroy\" don't destroy it.)"
    power = 3
    toughness = 2

    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(TargetFilter.Creature.youControl())
        effect = Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "3"
        artist = "Cristi Balanescu"
        flavorText = "\"We shall persevere.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/0/c06cc03f-da73-4ba5-96c9-618b497a7ea3.jpg"
    }
}
