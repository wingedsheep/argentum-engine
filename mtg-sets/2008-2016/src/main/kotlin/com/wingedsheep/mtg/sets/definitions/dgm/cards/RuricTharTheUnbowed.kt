package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.MustAttack
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ruric Thar, the Unbowed
 * {4}{R}{G}
 * Legendary Creature — Ogre Warrior
 * 6/6
 * Reach, vigilance
 * Ruric Thar attacks each combat if able.
 * Whenever a player casts a noncreature spell, Ruric Thar deals 6 damage to that player.
 */
val RuricTharTheUnbowed = card("Ruric Thar, the Unbowed") {
    manaCost = "{4}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Legendary Creature — Ogre Warrior"
    power = 6
    toughness = 6
    oracleText = "Reach, vigilance\n" +
        "Ruric Thar attacks each combat if able.\n" +
        "Whenever a player casts a noncreature spell, Ruric Thar deals 6 damage to that player."

    keywords(Keyword.REACH, Keyword.VIGILANCE)

    staticAbility {
        ability = MustAttack()
    }

    triggeredAbility {
        trigger = Triggers.anyPlayer.casts(GameObjectFilter.Noncreature)
        effect = Effects.DealDamage(6, EffectTarget.PlayerRef(Player.TriggeringPlayer))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "99"
        artist = "Tyler Jacobson"
        imageUri = "https://cards.scryfall.io/normal/front/8/4/84dd3586-7c3b-4f9c-a1eb-7745b75339b0.jpg?1783940022"
    }
}
