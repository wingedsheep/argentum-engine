package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.daybound
import com.wingedsheep.sdk.dsl.nightbound
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Outland Liberator // Frenzied Trapbreaker (Innistrad: Midnight Hunt)
 * {1}{G}
 * Creature — Human Werewolf // Creature — Werewolf
 *
 * Front — Outland Liberator (2/2): {1}, Sacrifice this creature: Destroy target artifact or
 *         enchantment. Daybound.
 * Back  — Frenzied Trapbreaker (3/3): {1}, Sacrifice this creature: Destroy target artifact or
 *         enchantment. Whenever this creature attacks, destroy target artifact or enchantment
 *         defending player controls. Nightbound.
 */
private val OutlandLiberatorFront = card("Outland Liberator") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Werewolf"
    power = 2
    toughness = 2
    oracleText = "{1}, Sacrifice this creature: Destroy target artifact or enchantment.\n" +
        "Daybound (If a player casts no spells during their own turn, it becomes night next turn.)"

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.SacrificeSelf)
        val t = target(TargetFilter.ArtifactOrEnchantment)
        effect = Effects.Destroy(t)
    }

    daybound()

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "190"
        artist = "Randy Vargas"
        flavorText = "\"Just hold still. I'll help you.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/0/60e53d61-fcc3-4def-8206-052b46f62deb.jpg?1783925581"
    }
}

private val FrenziedTrapbreaker = card("Frenzied Trapbreaker") {
    manaCost = ""
    colorIdentity = "G"
    colorIndicator = "G" // Transformed back face, no mana cost (CR 204).
    typeLine = "Creature — Werewolf"
    power = 3
    toughness = 3
    oracleText = "{1}, Sacrifice this creature: Destroy target artifact or enchantment.\n" +
        "Whenever this creature attacks, destroy target artifact or enchantment defending player controls.\n" +
        "Nightbound (If a player casts at least two spells during their own turn, it becomes day next turn.)"

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.SacrificeSelf)
        val t = target(TargetFilter.ArtifactOrEnchantment)
        effect = Effects.Destroy(t)
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val t = target(TargetFilter.ArtifactOrEnchantment.defendingPlayerControls())
        effect = Effects.Destroy(t)
    }

    nightbound()

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "190"
        artist = "Randy Vargas"
        imageUri = "https://cards.scryfall.io/normal/back/6/0/60e53d61-fcc3-4def-8206-052b46f62deb.jpg?1783925581"
    }
}

val OutlandLiberator: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = OutlandLiberatorFront,
    backFace = FrenziedTrapbreaker,
)
