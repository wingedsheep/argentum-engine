package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.effects.MayPayManaEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.Effects

/**
 * Forsaken Miner
 * {B}
 * Creature — Skeleton Rogue
 * 2/2
 * This creature can't block.
 * Whenever you commit a crime, you may pay {B}. If you do, return this card from your
 * graveyard to the battlefield.
 */
val ForsakenMiner = card("Forsaken Miner") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Creature — Skeleton Rogue"
    power = 2
    toughness = 2
    oracleText = "This creature can't block.\nWhenever you commit a crime, you may pay {B}. If you do, return this card from your graveyard to the battlefield."

    staticAbility {
        ability = CantBlock(GroupFilter.source())
    }

    triggeredAbility {
        trigger = Triggers.YouCommitCrime
        triggerZone = Zone.GRAVEYARD
        effect = MayPayManaEffect(
            cost = ManaCost.parse("{B}"),
            effect = Effects.Move(
                target = EffectTarget.Self,
                destination = Zone.BATTLEFIELD
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "88"
        artist = "Andrey Kuzinskiy"
        imageUri = "https://cards.scryfall.io/normal/front/1/6/1679f74d-00f8-436c-9f8c-aa3f843a546c.jpg?1712355591"

        ruling("2024-04-12", "A player commits a crime as they cast a spell, activate an ability, or put a triggered ability on the stack that targets at least one opponent, at least one permanent, spell, or ability an opponent controls, and/or at least one card in an opponent's graveyard.")
        ruling("2024-04-12", "A player can commit only one crime per spell or ability they control. Targeting multiple opponents, permanents, spells, abilities, and/or cards with the same spell or ability doesn't constitute committing multiple crimes.")
        ruling("2024-04-12", "Forsaken Miner's last ability triggers only if Forsaken Miner is in your graveyard at the moment you cast a spell, activate an ability, or put a triggered ability on the stack with one or more targets that constitute a crime.")
        ruling("2024-04-12", "You choose whether to pay {B} as the triggered ability is resolving. Once the ability starts resolving, it's too late for anyone to respond by removing Forsaken Miner from your graveyard to stop you from bringing it back.")
        ruling("2024-04-12", "Changing the target or targets of a spell or ability won't affect whether or not the controller of that spell or ability has committed a crime. Only the initial targets chosen for that spell or ability are used to determine whether or not its controller committed a crime.")
    }
}
