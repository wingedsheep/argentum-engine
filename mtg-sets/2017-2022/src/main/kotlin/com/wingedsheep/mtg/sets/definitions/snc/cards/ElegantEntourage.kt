package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Elegant Entourage
 * {3}{G}
 * Creature — Elf Druid
 * 4 / 4
 * Alliance — Whenever another creature you control enters, target creature other than this creature
 * gets +1/+1 and gains trample until end of turn.
 *
 * "Alliance" is a pure ability word, so the trigger is the plain
 * `Triggers.another(GameObjectFilter.Creature.youControl()).enters()`. The target is any creature
 * (either controller, including the creature that just entered) except this one —
 * `TargetFilter.OtherCreature`.
 */
val ElegantEntourage = card("Elegant Entourage") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Druid"
    oracleText = "Alliance — Whenever another creature you control enters, target creature other than this creature gets +1/+1 and gains trample until end of turn."
    power = 4
    toughness = 4

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.youControl()).enters()
        val t = target(TargetFilter.OtherCreature)
        effect = Effects.ModifyStats(1, 1, t) then Effects.GrantKeyword(Keyword.TRAMPLE, t)
        description = "Alliance — Whenever another creature you control enters, target creature other than this creature gets +1/+1 and gains trample until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "143"
        artist = "Jodie Muir"
        flavorText = "\"Terribly sorry, but this is a private party, and your name isn't on the list.\""
        imageUri = "https://cards.scryfall.io/normal/front/e/9/e9d3500e-09ba-48b6-933f-25b4625b79aa.jpg?1783923104"
    }
}
