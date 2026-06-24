package com.wingedsheep.mtg.sets.definitions.dsk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Most Valuable Slayer
 * {3}{R}
 * Creature — Human Warrior
 * 2/4
 *
 * Whenever you attack, target attacking creature gets +1/+0 and gains first strike
 * until end of turn.
 *
 * Modeled on the standard "Whenever you attack, buff a target attacker" shape (Hunter's
 * Talent level 2): the once-per-combat [Triggers.YouAttack] trigger targets an attacking
 * creature ([Targets.AttackingCreature]) and composes a +1/+0 [Effects.ModifyStats] with a
 * first-strike [Effects.GrantKeyword], both lasting until end of turn (the facade default
 * duration for ModifyStats and the explicit duration for the keyword grant).
 */
val MostValuableSlayer = card("Most Valuable Slayer") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Warrior"
    power = 2
    toughness = 4
    oracleText = "Whenever you attack, target attacking creature gets +1/+0 and gains first " +
        "strike until end of turn."

    triggeredAbility {
        trigger = Triggers.YouAttack
        val attacker = target("attacking creature", Targets.AttackingCreature)
        effect = Effects.ModifyStats(1, 0, attacker)
            .then(Effects.GrantKeyword(Keyword.FIRST_STRIKE, attacker))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "144"
        artist = "Patrik Hell"
        flavorText = "Torain had never lost a match in his life, and he didn't plan to start now."
        imageUri = "https://cards.scryfall.io/normal/front/c/2/c2b7b635-4c72-4129-bf95-1eef05cce3d3.jpg?1726286383"
    }
}
