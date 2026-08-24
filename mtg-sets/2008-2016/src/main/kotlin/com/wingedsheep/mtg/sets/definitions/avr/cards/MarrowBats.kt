package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.RegenerateEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Marrow Bats
 * {4}{B}
 * Creature — Bat Skeleton
 * 4/1
 * Flying
 * Pay 4 life: Regenerate this creature.
 *
 * Flying is a printed [Keyword]. The regeneration ability is the Deepwood Ghoul shape with a
 * bigger price: a bare [Costs.PayLife] with no tap or mana beside it, and
 * [RegenerateEffect] on [EffectTarget.Self]. `RegenerateEffect` has no `Effects` facade entry,
 * so it is imported directly.
 */
val MarrowBats = card("Marrow Bats") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Bat Skeleton"
    power = 4
    toughness = 1
    oracleText = "Flying\n" +
        "Pay 4 life: Regenerate this creature."

    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.PayLife(4)
        effect = RegenerateEffect(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "113"
        artist = "Jason A. Engle"
        flavorText = "\"No matter how far we push into Stensia, undeath will always remain in these lands.\"\n—Terhold, archmage of Drunau"
        imageUri = "https://cards.scryfall.io/normal/front/3/8/38dcbad0-267e-411f-8e99-5d90b537bf9b.jpg"
    }
}
