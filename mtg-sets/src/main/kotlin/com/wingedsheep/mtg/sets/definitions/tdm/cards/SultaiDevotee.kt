package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.values.ManaColorSet

/**
 * Sultai Devotee
 * {1}{G}
 * Creature — Zombie Snake Druid
 * 2/1
 *
 * Deathtouch
 * {1}: Add {B}, {G}, or {U}. Activate only once each turn.
 */
val SultaiDevotee = card("Sultai Devotee") {
    manaCost = "{1}{G}"
    colorIdentity = "BGU"
    typeLine = "Creature — Zombie Snake Druid"
    power = 2
    toughness = 1
    oracleText = "Deathtouch\n{1}: Add {B}, {G}, or {U}. Activate only once each turn."

    keywords(Keyword.DEATHTOUCH)

    activatedAbility {
        cost = Costs.Mana("{1}")
        manaAbility = true
        restrictions = listOf(ActivationRestriction.OncePerTurn)
        effect = Effects.AddManaOfChoice(
            ManaColorSet.Specific(setOf(Color.BLACK, Color.GREEN, Color.BLUE))
        )
        description = "{1}: Add {B}, {G}, or {U}. Activate only once each turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "160"
        artist = "Bastien L. Deharme"
        flavorText = "Many Sultai villages are protected by naga who weave magics to bless or curse as needed."
        imageUri = "https://cards.scryfall.io/normal/front/c/3/c32487e9-f3ac-472e-b6ea-81bd9254770c.jpg?1743204608"
    }
}
