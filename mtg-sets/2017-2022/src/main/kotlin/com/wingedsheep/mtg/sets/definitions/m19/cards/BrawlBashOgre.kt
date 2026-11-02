package com.wingedsheep.mtg.sets.definitions.m19.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Brawl-Bash Ogre — Core Set 2019 #213
 * {2}{B}{R} · Creature — Ogre Warrior · 3 / 3
 *
 * Same shape as Vampire Gourmand / Bloodmist Infiltrator: an [Effects.MayPay] wrapping the
 * optional sacrifice of another creature, so "If you do" only grants +2/+2 (until end of turn)
 * when a creature was actually sacrificed. Only one creature can be sacrificed per trigger.
 */
val BrawlBashOgre = card("Brawl-Bash Ogre") {
    manaCost = "{2}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Creature — Ogre Warrior"
    power = 3
    toughness = 3
    oracleText = "Menace (This creature can't be blocked except by two or more creatures.)\n" +
        "Whenever this creature attacks, you may sacrifice another creature. If you do, " +
        "this creature gets +2/+2 until end of turn."

    keywords(Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.MayPay(
            Effects.SacrificeOwn(GameObjectFilter.Creature, excludeSource = true),
            then = Effects.ModifyStats(2, 2, EffectTarget.Self)
        )
        description = "Whenever this creature attacks, you may sacrifice another creature. If you do, " +
            "this creature gets +2/+2 until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "213"
        artist = "Even Amundsen"
        imageUri = "https://cards.scryfall.io/normal/front/3/8/38b9f9d7-a00d-4a90-9d7a-88ece10af328.jpg?1783934522"
        ruling(
            "2018-07-13",
            "While resolving the triggered ability of Brawl-Bash Ogre, you can't sacrifice multiple creatures to give it +2/+2 more than once."
        )
    }
}
