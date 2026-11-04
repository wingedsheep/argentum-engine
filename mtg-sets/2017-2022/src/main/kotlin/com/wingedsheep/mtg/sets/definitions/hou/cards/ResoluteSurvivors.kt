package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ExertAsItAttacks
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Resolute Survivors
 * {1}{R}{W} Creature — Human Warrior 3/3
 * You may exert this creature as it attacks. (It won't untap during your next untap step.)
 * Whenever you exert a creature, this creature deals 1 damage to each opponent and you gain 1 life.
 */
val ResoluteSurvivors = card("Resolute Survivors") {
    manaCost = "{1}{R}{W}"
    colorIdentity = "RW"
    typeLine = "Creature — Human Warrior"
    oracleText = "You may exert this creature as it attacks. (It won't untap during your next untap step.)\n" +
        "Whenever you exert a creature, this creature deals 1 damage to each opponent and you gain 1 life."
    power = 3
    toughness = 3

    staticAbility {
        ability = ExertAsItAttacks()
    }

    triggeredAbility {
        trigger = Triggers.you.exerts(GameObjectFilter.Creature)
        effect = Effects.DealDamage(1, EffectTarget.PlayerRef(Player.EachOpponent)) then Effects.GainLife(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "142"
        artist = "David Palumbo"
        flavorText = "\"We are crops no longer. Reap elsewhere.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/1/713f469e-58c3-4db4-aa7a-77126bda13f2.jpg?1783936010"
        ruling("2017-04-18", "Some cards have abilities that trigger whenever you exert any creature. These abilities trigger when you exert that creature or any other creature you control.")
    }
}
