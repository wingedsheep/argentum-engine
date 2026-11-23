package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Boros Reckoner — Gatecrash #215
 * {R/W}{R/W}{R/W} · Creature — Minotaur Wizard · 3/3
 *
 * Whenever this creature is dealt damage, it deals that much damage to any target.
 * {R/W}: This creature gains first strike until end of turn.
 *
 * Brash Taunter's dealt-damage shape aimed at any target. Simultaneous combat damage from
 * several sources folds into one trigger for the total (TriggerDetector.foldSimultaneousDamage),
 * matching the ruling.
 */
val BorosReckoner = card("Boros Reckoner") {
    manaCost = "{R/W}{R/W}{R/W}"
    colorIdentity = "RW"
    typeLine = "Creature — Minotaur Wizard"
    power = 3
    toughness = 3
    oracleText = "Whenever this creature is dealt damage, it deals that much damage to any target.\n" +
        "{R/W}: This creature gains first strike until end of turn."

    triggeredAbility {
        trigger = Triggers.self.isDealtDamage()
        val any = target(Targets.Any)
        effect = Effects.DealDamage(DynamicAmounts.triggerDamageAmount(), any)
        description = "Whenever this creature is dealt damage, it deals that much damage to any target."
    }

    activatedAbility {
        cost = Costs.Mana("{R/W}")
        effect = Effects.GrantKeyword(Keyword.FIRST_STRIKE, EffectTarget.Self)
        description = "{R/W}: This creature gains first strike until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "215"
        artist = "Howard Lyon"
        flavorText = "\"Imagine a thunderstorm that's also a tactical genius. That's him.\"\n" +
            "—Dars Gostok, Firefist captain"
        imageUri = "https://cards.scryfall.io/normal/front/8/2/82a18b07-38b8-4854-9735-3cfe83b11bf1.jpg?1783940095"
        ruling(
            "2017-03-14",
            "Boros Reckoner's first ability will trigger even if it is dealt lethal damage. For example, if it " +
                "blocks a 7/7 creature, its ability will trigger and Boros Reckoner will deal 7 damage to the target."
        )
        ruling(
            "2017-03-14",
            "Damage dealt by Boros Reckoner due to its first ability isn't combat damage, even if it was combat " +
                "damage that caused that ability to trigger."
        )
        ruling(
            "2017-03-14",
            "If Boros Reckoner is dealt damage by multiple sources at once, such as by two creatures blocking it, " +
                "its ability triggers once and one target is dealt that much damage."
        )
    }
}
