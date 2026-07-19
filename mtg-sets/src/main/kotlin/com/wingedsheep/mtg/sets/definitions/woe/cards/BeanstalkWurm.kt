package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.PlayAdditionalLandsEffect

/**
 * Beanstalk Wurm // Plant Beans
 * {4}{G}
 * Creature — Plant Wurm
 * 5/4
 * Reach
 *
 * Adventure: Plant Beans — {1}{G}, Sorcery — Adventure
 * You may play an additional land this turn.
 *
 * (CR 715: Adventure cards. Casting the Adventure exiles the card on resolution and lets the
 * caster cast it as the creature spell while it remains in exile.)
 */
val BeanstalkWurm = card("Beanstalk Wurm") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Plant Wurm"
    oracleText = "Reach"
    power = 5
    toughness = 4
    keywords(Keyword.REACH)

    adventure("Plant Beans") {
        manaCost = "{1}{G}"
        typeLine = "Sorcery — Adventure"
        oracleText = "You may play an additional land this turn. " +
            "(Then exile this card. You may cast the creature later from exile.)"
        spell {
            effect = PlayAdditionalLandsEffect(count = 1)
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "161"
        artist = "Aldo Domínguez"
        flavorText = "When Beluna's pets escape Stormkeld, villages fall and new Everstalks rise."
        imageUri = "https://cards.scryfall.io/normal/front/1/9/19f20c0a-22be-4a9c-96ce-4047f7a2d424.jpg?1783915085"
        ruling(
            "2023-09-01",
            "The effect that allows you to play an additional land that turn is cumulative with " +
                "other effects that do so.",
        )
    }
}
