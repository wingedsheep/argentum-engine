package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Guan Yu, Sainted Warrior
 * {3}{W}{W}
 * Legendary Creature — Human Soldier Warrior
 * 3/5
 * Horsemanship
 * When Guan Yu is put into your graveyard from the battlefield, you may shuffle Guan Yu into your library.
 */
val GuanYuSaintedWarrior = card("Guan Yu, Sainted Warrior") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Human Soldier Warrior"
    power = 3
    toughness = 5
    oracleText = "Horsemanship (This creature can't be blocked except by creatures with horsemanship.)\n" +
        "When Guan Yu is put into your graveyard from the battlefield, you may shuffle Guan Yu into your library."

    keywordAbility(KeywordAbility.Simple(Keyword.HORSEMANSHIP))

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.May(Effects.ShuffleIntoLibrary(EffectTarget.Self))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "6"
        artist = "Qiao Dafu"
        imageUri = "https://cards.scryfall.io/normal/front/1/5/1575fadf-cd5c-4b4f-8965-c006e571334b.jpg?1783946132"
    }
}
