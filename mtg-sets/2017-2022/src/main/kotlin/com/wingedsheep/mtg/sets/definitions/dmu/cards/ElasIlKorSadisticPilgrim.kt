package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

val ElasIlKorSadisticPilgrim = card("Elas il-Kor, Sadistic Pilgrim") {
    manaCost = "{W}{B}"
    typeLine = "Legendary Creature — Phyrexian Kor Cleric"
    power = 2
    toughness = 2
    oracleText = "Deathtouch\nWhenever another creature you control enters, you gain 1 life.\nWhenever another creature you control dies, each opponent loses 1 life."

    keywords(Keyword.DEATHTOUCH)
    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.youControl()).enters()
        effect = Effects.GainLife(1)
    }
    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.youControl()).dies()
        effect = Effects.LoseLife(1, EffectTarget.PlayerRef(Player.EachOpponent))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "198"
        artist = "G-host Lee"
        flavorText = "\"All will bow to the Whispering One.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/c/1c3f0ffe-cf51-4c30-8cd5-9e3c7e92c019.jpg?1783921287"
        ruling("2022-09-09", "If one or more other creatures enter the battlefield under your control at the same time that Elas il-Kor enters the battlefield, its second ability will trigger for each of those other creatures.")
        ruling("2022-09-09", "If one or more other creatures you control die at the same time that Elas il-Kor dies, its last ability triggers for each of those other creatures.")
    }
}
