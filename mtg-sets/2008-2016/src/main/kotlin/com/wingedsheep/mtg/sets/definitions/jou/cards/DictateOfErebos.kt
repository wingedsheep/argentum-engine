package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Dictate of Erebos
 * {3}{B}{B}
 * Enchantment
 * Flash
 * Whenever a creature you control dies, each opponent sacrifices a creature of their choice.
 *
 * No opponent is targeted (hexproof doesn't help); one trigger per creature that dies.
 */
val DictateOfErebos = card("Dictate of Erebos") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "Flash\nWhenever a creature you control dies, each opponent sacrifices a creature of their choice."

    keywords(Keyword.FLASH)

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).dies()
        effect = Effects.Sacrifice(GameObjectFilter.Creature, 1, EffectTarget.PlayerRef(Player.EachOpponent))
        description = "Whenever a creature you control dies, each opponent sacrifices a creature of their choice."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "65"
        artist = "Michael C. Hayes"
        flavorText = "\"I take no pleasure in your suffering, but it is necessary. Once you accept your " +
            "fate you'll find eternity more tolerable.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/f/9f06db70-95f9-41eb-8e5f-8bc56fd34c09.jpg?1783939437"
        ruling("2014-04-26", "When the triggered ability resolves, first the player whose turn it is (if that player is an opponent) chooses which creature they will sacrifice, then each other opponent in turn order does the same, then all chosen creatures are sacrificed at the same time.")
        ruling("2014-04-26", "None of the opponents are targeted. An opponent with hexproof would still sacrifice a creature.")
        ruling("2014-04-26", "If multiple creatures you control die at the same time, Dictate of Erebos's triggered ability will trigger that many times.")
    }
}
