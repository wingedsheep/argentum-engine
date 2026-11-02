package com.wingedsheep.mtg.sets.definitions.m19.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Lena, Selfless Champion — Core Set 2019 #21
 * {4}{W}{W} · Legendary Creature — Human Knight · 3/3
 *
 * When Lena enters, create a 1/1 white Soldier creature token for each nontoken creature you control.
 * Sacrifice Lena: Creatures you control with power less than Lena's power gain indestructible until
 * end of turn.
 *
 * Modeling notes:
 *  - The token count is read as the trigger resolves, so Lena counts herself if she is still there
 *    (ruling 2018-07-13).
 *  - The sacrifice ability is Hajar, Loyal Bodyguard's group shape: the affected set is snapshotted
 *    as the ability resolves. "Lena's power" is [GameObjectFilter.powerLessThanEntity] against
 *    [EffectTarget.Self], which reads the sacrificed source's last-known power (ruling 2018-07-13) —
 *    counters and pumps she had included, not her printed 3.
 */
val LenaSelflessChampion = card("Lena, Selfless Champion") {
    manaCost = "{4}{W}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Human Knight"
    power = 3
    toughness = 3
    oracleText = "When Lena enters, create a 1/1 white Soldier creature token for each nontoken " +
        "creature you control.\nSacrifice Lena: Creatures you control with power less than Lena's " +
        "power gain indestructible until end of turn."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            count = DynamicAmounts.count(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Creature.nontoken()),
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Soldier"),
            imageUri = "https://cards.scryfall.io/normal/front/0/7/07929ced-5c81-4b05-82e1-522fec1cf5c0.jpg?1783934469",
        )
        description = "When Lena enters, create a 1/1 white Soldier creature token for each nontoken " +
            "creature you control."
    }

    activatedAbility {
        cost = Costs.SacrificeSelf
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.youControl().powerLessThanEntity(EffectTarget.Self)),
            Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, EffectTarget.IterationEntity),
        )
        description = "Sacrifice Lena: Creatures you control with power less than Lena's power gain " +
            "indestructible until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "21"
        artist = "Lucas Graciano"
        flavorText = "\"Those who do evil and those who do nothing are equally worthy of contempt.\""
        imageUri = "https://cards.scryfall.io/normal/front/2/f/2ffcbcda-2ba3-45e7-80c0-85ea3b7eea0c.jpg?1783934603"
        ruling(
            "2018-07-13",
            "The number of nontoken creatures you control is counted only as Lena's first ability " +
                "resolves. If Lena is still on the battlefield (and not somehow a token), it will count itself."
        )
        ruling(
            "2018-07-13",
            "To determine which creatures gain indestructible while resolving Lena's last ability, use " +
                "Lena's power as it last existed on the battlefield."
        )
        ruling(
            "2018-07-13",
            "Lena's last ability affects only creatures you control with the appropriate power at the " +
                "time it resolves. Creatures you begin to control later in the turn won't gain " +
                "indestructible, and a creature you control whose power increases later in the turn " +
                "won't lose indestructible."
        )
    }
}
