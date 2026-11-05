package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Ruin Rat
 * {1}{B}
 * Creature — Rat
 * 1/1
 * Deathtouch
 * When this creature dies, exile target card from an opponent's graveyard.
 */
val RuinRat = card("Ruin Rat") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Rat"
    power = 1
    toughness = 1
    oracleText = "Deathtouch\nWhen this creature dies, exile target card from an opponent's graveyard."

    keywords(Keyword.DEATHTOUCH)

    triggeredAbility {
        trigger = Triggers.self.dies()
        val card = target(TargetFilter.CardInGraveyard.ownedByOpponent())
        effect = Effects.Exile(card)
        description = "When this creature dies, exile target card from an opponent's graveyard."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "75"
        artist = "Shreya Shetty"
        flavorText = "From every shadow came the dull scratch of teeth on bones."
        imageUri = "https://cards.scryfall.io/normal/front/9/1/91652fe0-2064-4996-b484-a1f88e0023f7.jpg?1783936036"
    }
}
