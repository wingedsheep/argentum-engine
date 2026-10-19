package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Twisted Embrace — Kamigawa: Neon Dynasty #125
 * {2}{B}{B} · Enchantment — Aura
 *
 * Enchant artifact or creature you control
 * When this Aura enters, destroy target creature or planeswalker an opponent controls.
 * As long as enchanted permanent is a creature, it gets +1/+1.
 *
 * Gift of Wrath's creature-gated bonus ([Conditions.EnchantedPermanentMatches] on the host's
 * projected type, so an uncrewed Vehicle gets nothing), with the enchant restriction narrowed to
 * permanents you control and Master's Rebuke's opponent-controlled creature-or-planeswalker target.
 */
val TwistedEmbrace = card("Twisted Embrace") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant artifact or creature you control\n" +
        "When this Aura enters, destroy target creature or planeswalker an opponent controls.\n" +
        "As long as enchanted permanent is a creature, it gets +1/+1."

    auraTarget = TargetObject(filter = TargetFilter.CreatureOrArtifact.youControl())

    triggeredAbility {
        trigger = Triggers.self.enters()
        val victim = target(TargetFilter(GameObjectFilter.CreatureOrPlaneswalker.opponentControls()))
        effect = Effects.Destroy(victim)
    }

    staticAbility {
        condition = Conditions.EnchantedPermanentMatches(GameObjectFilter.Creature)
        ability = ModifyStats(1, 1, GroupFilter.attachedCreature())
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "125"
        artist = "Betty Jiang"
        flavorText = "Evil kami are drawn to mortals who share their grim fascinations."
        imageUri = "https://cards.scryfall.io/normal/front/e/4/e4c7919a-de62-46df-a937-593647473ef5.jpg?1783923875"
    }
}
