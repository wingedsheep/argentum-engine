package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Skemfar Avenger
 * {1}{B}
 * Creature — Elf Berserker
 * 3/1
 *
 * Whenever another nontoken Elf or Berserker you control dies, you draw a card and you lose 1 life.
 *
 * (The printed card also read "or is put into exile"; the current Oracle text is "dies" only.)
 */
val SkemfarAvenger = card("Skemfar Avenger") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Elf Berserker"
    power = 3
    toughness = 1
    oracleText = "Whenever another nontoken Elf or Berserker you control dies, you draw a card and you lose 1 life."

    triggeredAbility {
        trigger = Triggers.another(
            GameObjectFilter.Creature
                .withAnyOfSubtypes(listOf(Subtype.ELF, Subtype.BERSERKER))
                .nontoken()
                .youControl()
        ).dies()
        effect = Effects.DrawCards(1) then Effects.LoseLife(1, EffectTarget.Controller)
        description = "Whenever another nontoken Elf or Berserker you control dies, you draw a card and you lose 1 life."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "109"
        artist = "Randy Vargas"
        flavorText = "Stripped of their divinity, the elves of Skemfar channeled their rage and sorrow into bloody brutality."
        imageUri = "https://cards.scryfall.io/normal/front/f/a/fad2c6d4-03dd-4dab-861c-77c55bda0db7.jpg?1783928240"
    }
}
