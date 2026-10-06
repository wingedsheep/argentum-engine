package com.wingedsheep.mtg.sets.definitions.m10.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Goblin Artillery — Magic 2010 #138
 * {1}{R}{R} · Creature — Goblin Warrior · 1 / 3
 *
 * {T}: This creature deals 2 damage to any target and 3 damage to you.
 *
 * The Brothers of Fire shape behind a tap cost. The damage to you is part of the targeted ability,
 * so if the target is illegal on resolution the whole ability fizzles and you take nothing.
 */
val GoblinArtillery = card("Goblin Artillery") {
    manaCost = "{1}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin Warrior"
    oracleText = "{T}: This creature deals 2 damage to any target and 3 damage to you."
    power = 1
    toughness = 3

    activatedAbility {
        cost = Costs.Tap
        val target = target(Targets.Any)
        effect = Effects.DealDamage(2, target) then Effects.DealDamage(3, EffectTarget.PlayerRef(Player.You))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "138"
        artist = "Alex Horley-Orlandelli"
        flavorText = "Most goblins get their turn firing the catapult, but few achieve the coveted title of Ammunition Holder."
        imageUri = "https://cards.scryfall.io/normal/front/6/7/674bd59d-9d6d-434d-974f-3aa1cbd770b5.jpg?1783942373"

        ruling(
            "2009-10-01",
            "If the targeted permanent or player is an illegal target by the time Goblin Artillery's " +
                "ability would resolve, the entire ability doesn't resolve. You won't be dealt any damage."
        )
    }
}
