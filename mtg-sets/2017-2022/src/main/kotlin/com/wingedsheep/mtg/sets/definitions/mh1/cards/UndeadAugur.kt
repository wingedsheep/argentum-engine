package com.wingedsheep.mtg.sets.definitions.mh1.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Undead Augur
 * {B}{B}
 * Creature — Zombie Wizard
 * 2/2
 * Whenever this creature or another Zombie you control dies, you draw a card and lose 1 life.
 *
 * Undead Augur is itself a Zombie you control, so one dies trigger over Zombies you control covers
 * both "this creature" and "another Zombie you control" (tokens included — there is no "nontoken").
 */
val UndeadAugur = card("Undead Augur") {
    manaCost = "{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie Wizard"
    power = 2
    toughness = 2
    oracleText = "Whenever this creature or another Zombie you control dies, you draw a card and lose 1 life."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Permanent.withSubtype("Zombie").youControl()).dies()
        effect = Effects.DrawCards(1) then Effects.LoseLife(1, EffectTarget.Controller)
        description = "Whenever this creature or another Zombie you control dies, you draw a card and lose 1 life."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "112"
        artist = "Sara Winters"
        flavorText = "\"To see past the veil of death, cross it.\"\n—The Red Book of Mezdrithalik"
        imageUri = "https://cards.scryfall.io/normal/front/c/6/c6e3049e-9397-4bef-914b-e9664661419c.jpg?1783933118"
    }
}
