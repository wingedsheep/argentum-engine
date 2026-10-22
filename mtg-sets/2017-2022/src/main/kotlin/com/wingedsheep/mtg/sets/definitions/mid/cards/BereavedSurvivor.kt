package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Bereaved Survivor // Dauntless Avenger (Innistrad: Midnight Hunt #4)
 *
 * Front — Bereaved Survivor ({2}{W}, Creature — Human Peasant, 2/1)
 *   When another creature you control dies, transform this creature.
 *
 * Back — Dauntless Avenger (Creature — Human Soldier, 3/2, white color indicator)
 *   Whenever this creature attacks, return target creature card with mana value 2 or less from
 *   your graveyard to the battlefield tapped and attacking.
 *
 * Several creatures dying at once trigger the front face several times, but it transforms only
 * once: `TransformEffectExecutor` applies CR 701.28f, so a trigger whose source has already
 * transformed since it was put on the stack does nothing.
 */
private val BereavedSurvivorFront = card("Bereaved Survivor") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Peasant"
    power = 2
    toughness = 1
    oracleText = "When another creature you control dies, transform this creature."

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.youControl()).dies()
        effect = Effects.Transform(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "4"
        artist = "Zara Alfonso"
        flavorText = "\"With Avacyn gone, I no longer knew who to pray to. Alone, I wept for a sister who would never return.\""
        imageUri = "https://cards.scryfall.io/normal/front/4/a/4adee830-62fd-4ab4-b1c6-a8bbe15331d1.jpg?1783925673"
        ruling("2021-09-24", "Multiple creatures dying will cause Bereaved Survivor to transform only once.")
    }
}

private val DauntlessAvenger = card("Dauntless Avenger") {
    manaCost = ""
    colorIdentity = "W"
    colorIndicator = "W"
    typeLine = "Creature — Human Soldier"
    power = 3
    toughness = 2
    oracleText = "Whenever this creature attacks, return target creature card with mana value 2 or " +
        "less from your graveyard to the battlefield tapped and attacking."

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val creature = target(TargetFilter.CreatureInYourGraveyard.manaValueAtMost(2))
        effect = Effects.Move(
            target = creature,
            destination = Zone.BATTLEFIELD,
            placement = ZonePlacement.TappedAndAttacking,
            fromZone = Zone.GRAVEYARD,
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "4"
        artist = "Zara Alfonso"
        flavorText = "\"It was Sigarda who lifted me from despair and gave me the strength to fight on.\""
        imageUri = "https://cards.scryfall.io/normal/back/4/a/4adee830-62fd-4ab4-b1c6-a8bbe15331d1.jpg?1783925673"
    }
}

val BereavedSurvivor: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = BereavedSurvivorFront,
    backFace = DauntlessAvenger,
)
