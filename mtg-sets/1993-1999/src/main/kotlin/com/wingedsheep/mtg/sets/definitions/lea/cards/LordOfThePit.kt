package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Lord of the Pit — Limited Edition Alpha #114
 * {4}{B}{B}{B} · Creature — Demon · 7/7
 *
 * Flying, trample
 * At the beginning of your upkeep, sacrifice a creature other than this creature. If you can't,
 * this creature deals 7 damage to you.
 *
 * The "if you can't" fork is checked as the trigger resolves (the Rust Elemental shape): if you
 * control another creature you must sacrifice one — the damage is never an option you can choose —
 * and only with no other creature does Lord of the Pit deal 7 damage to you. Both `excludeSelf` and
 * `excludeSource` are load-bearing: the Lord is itself a creature and must not feed itself.
 */
val LordOfThePit = card("Lord of the Pit") {
    manaCost = "{4}{B}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Demon"
    power = 7
    toughness = 7
    oracleText = "Flying, trample\n" +
        "At the beginning of your upkeep, sacrifice a creature other than this creature. If you " +
        "can't, this creature deals 7 damage to you."

    keywords(Keyword.FLYING, Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.If(
            condition = Conditions.YouControl(GameObjectFilter.Creature, excludeSelf = true),
            then = Effects.SacrificeOwn(GameObjectFilter.Creature, excludeSource = true),
            otherwise = Effects.DealDamage(7, EffectTarget.Controller)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "114"
        artist = "Mark Tedin"
        imageUri = "https://cards.scryfall.io/normal/front/2/9/2926777a-4f6e-4965-ba83-22cf7df02602.jpg?1783948694"
        ruling("2017-11-17", "If you control any other creatures as Lord of the Pit's triggered ability resolves, you must sacrifice one. You can't choose to be dealt 7 damage instead.")
        ruling("2017-11-17", "If Lord of the Pit has gained lifelink, being dealt 7 damage by it doesn't cause your life total to change, even though you gain and lose life. If your life total is less than 7, you won't lose the game.")
        ruling("2017-11-17", "If you have two Lords of the Pit, you can sacrifice them to each other.")
    }
}
