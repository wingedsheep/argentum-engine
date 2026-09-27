package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Cursed Ronin
 * {3}{B}
 * Creature — Human Samurai
 * 1/1
 * Bushido 1 (Whenever this creature blocks or becomes blocked, it gets +1/+1 until end of turn.)
 * {B}: This creature gets +1/+1 until end of turn.
 *
 * The firebreathing-shaped pump is a plain activated ability: a mana-only cost and
 * `ModifyStats(+1/+1)` on [EffectTarget.Self], matching Assay's compiled model exactly. It is
 * repeatable, so no once-per-turn restriction is declared.
 */
val CursedRonin = card("Cursed Ronin") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Samurai"
    power = 1
    toughness = 1
    oracleText = "Bushido 1 (Whenever this creature blocks or becomes blocked, it gets +1/+1 until end of turn.)\n" +
        "{B}: This creature gets +1/+1 until end of turn."

    keywordAbility(KeywordAbility.bushido(1))

    activatedAbility {
        cost = Costs.Mana("{B}")
        effect = Effects.ModifyStats(1, 1, EffectTarget.Self)
        description = "{B}: This creature gets +1/+1 until end of turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "107"
        artist = "Carl Critchlow"
        flavorText = "\"You are fortunate, my enemy. You have paid the price but once. I never stop paying.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/8/b8f24fe9-22c4-4e53-9d7a-3cbf5533ac9b.jpg?1783944316"
    }
}
