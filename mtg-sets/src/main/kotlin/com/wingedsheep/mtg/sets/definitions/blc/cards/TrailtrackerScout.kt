package com.wingedsheep.mtg.sets.definitions.blc.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

val TrailtrackerScout = card("Trailtracker Scout") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Raccoon Scout"
    power = 1
    toughness = 3
    oracleText = "{T}: Add one mana of any color.\n" +
        "Whenever you expend 8, return up to one target permanent card from your graveyard to your hand. " +
        "(You expend 8 as you spend your eighth total mana to cast spells during a turn.)"

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddAnyColorMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    triggeredAbility {
        trigger = Triggers.Expend(8)
        val card = target(
            "permanent card from your graveyard",
            TargetObject(
                count = 1,
                optional = true,
                filter = TargetFilter(GameObjectFilter.Permanent.ownedByYou(), zone = Zone.GRAVEYARD)
            )
        )
        effect = Effects.ReturnToHand(card)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "35"
        artist = "Henry Peters"
        flavorText = "It takes great courage—and great curiosity—to tail calamity."
        imageUri = "https://cards.scryfall.io/normal/front/3/6/36ee967a-3cac-4fff-b616-ec2557c676f2.jpg?1721431413"
        ruling("2024-07-26", "Abilities that trigger whenever you \"expend N\" only trigger when you reach that specific amount of mana spent on casting spells that turn. This can only happen once per turn.")
        ruling("2024-07-26", "If the cost to cast a spell is increased, decreased, or changed because of additional or alternative costs, expend counts only the mana you actually spent.")
        ruling("2024-07-26", "A permanent with an ability that triggers whenever you \"expend N\" will see mana you spent to cast spells the turn it enters, including mana you spent before it entered.")
    }
}
