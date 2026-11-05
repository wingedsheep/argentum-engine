package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Bontu's Last Reckoning
 * {1}{B}{B}
 * Sorcery
 * Destroy all creatures. Lands you control don't untap during your next untap step.
 *
 * The untap clause is not locked at resolution: per the 2017-07-14 ruling it holds back every land
 * you control at that untap step, including lands that entered after the spell resolved.
 */
val BontusLastReckoning = card("Bontu's Last Reckoning") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Destroy all creatures. Lands you control don't untap during your next untap step."

    spell {
        effect = Effects.DestroyAll(GameObjectFilter.Creature) then
            Effects.SkipUntap(EffectTarget.Controller, GameObjectFilter.Land)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "60"
        artist = "Victor Adame Minguez"
        flavorText = "To the last, Bontu wanted only to be first among the God-Pharaoh's subjects."
        imageUri = "https://cards.scryfall.io/normal/front/1/b/1b4d0102-c0d6-4d50-941a-dd1c3575a3a8.jpg?1783936042"
        ruling("2017-07-14", "No lands that you control will untap during your next untap step, even lands that aren't tapped as this spell resolves. This includes lands that enter the battlefield after this spell resolves.")
        ruling("2017-07-14", "If more than one spell says that lands you control don't untap during your next untap step, the effects will all wear off during that untap step. You'll untap lands you control during your untap step after that one.")
    }
}
