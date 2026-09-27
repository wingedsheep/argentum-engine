package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Horobi, Death's Wail
 * {2}{B}{B}
 * Legendary Creature — Spirit
 * 4/4
 *
 * Flying
 * Whenever a creature becomes the target of a spell or ability, destroy that creature.
 *
 * "A creature" is any battlefield creature — either player's, Horobi included — targeted by any
 * spell or ability, so the trigger subject is the unrestricted `Creature` filter and the destroy
 * reads the targeted object back as the triggering entity.
 */
val HorobiDeathsWail = card("Horobi, Death's Wail") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Spirit"
    power = 4
    toughness = 4
    oracleText = "Flying\nWhenever a creature becomes the target of a spell or ability, destroy that creature."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature).becomesTarget()
        effect = Effects.Destroy(EffectTarget.TriggeringEntity)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "117"
        artist = "John Bolton"
        flavorText = "\"From the ashes of Reito rose a new kami. And thereafter at every battle came Horobi, Death's Wail.\"\n—The History of Kamigawa"
        imageUri = "https://cards.scryfall.io/normal/front/b/4/b41983cb-c4e4-4384-bd69-df3fc6e74cd0.jpg?1783944314"

        ruling(
            "2023-04-14",
            "If the creature is the only target of the spell or ability, that spell or ability won't resolve."
        )
    }
}
