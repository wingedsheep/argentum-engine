package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttack
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PreventActivatedAbilities
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * One Thousand Lashes
 * {2}{W}{B}
 * Enchantment — Aura
 *
 * Enchant creature
 * Enchanted creature can't attack or block, and its activated abilities can't be activated.
 * At the beginning of the upkeep of enchanted creature's controller, that player loses 1 life.
 *
 * Arrest's three-static lock plus Wanderlust's attached-upkeep trigger, where the triggering
 * player is the enchanted creature's controller. The activation lock covers mana abilities too.
 */
val OneThousandLashes = card("One Thousand Lashes") {
    manaCost = "{2}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature can't attack or block, and its activated abilities can't be activated.\n" +
        "At the beginning of the upkeep of enchanted creature's controller, that player loses 1 life."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = CantAttack(filter = GroupFilter.attachedCreature())
    }

    staticAbility {
        ability = CantBlock(filter = GroupFilter.attachedCreature())
    }

    staticAbility {
        ability = PreventActivatedAbilities(
            filter = GameObjectFilter.Permanent.attachedToBySource(),
        )
    }

    triggeredAbility {
        trigger = Triggers.attached.beginningOf(Step.UPKEEP)
        effect = Effects.LoseLife(1, EffectTarget.PlayerRef(Player.TriggeringPlayer))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "183"
        artist = "Daarken"
        imageUri = "https://cards.scryfall.io/normal/front/e/e/eef2d548-477b-4be1-b946-6df6aac2ee6e.jpg?1783940103"
        ruling(
            "2016-06-08",
            "Activated abilities contain a colon. They're generally written " +
                "\"[Cost]: [Effect].\" Some keywords are activated abilities and will have " +
                "colons in their reminder text."
        )
    }
}
