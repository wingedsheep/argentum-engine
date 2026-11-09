package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CanBlockAdditionalForCreatureGroup
import com.wingedsheep.sdk.scripting.MustBeBlocked
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Tangleclaw Werewolf // Fibrous Entangler (Eldritch Moon #174)
 * {2}{G}{G}
 * Creature — Werewolf Horror 2/4 // Creature — Eldrazi Werewolf 4/6
 *
 * Front — "This creature can block an additional creature each combat.
 *          {6}{G}: Transform this creature."
 * Back  — "Vigilance
 *          This creature must be blocked if able.
 *          This creature can block an additional creature each combat."
 *
 * The flip is an ordinary activated [Effects.Transform] (Kessig Prowler's shape). The extra block is
 * [CanBlockAdditionalForCreatureGroup] scoped to [GroupFilter.source] (Lairwatch Giant), and "must be
 * blocked if able" is the at-least-one [MustBeBlocked] static (Canopy Stalker). The back is a colorless
 * Eldrazi Werewolf (no mana cost, no color indicator); `colorIdentity` stays "G".
 */

private val TangleclawWerewolfFront = card("Tangleclaw Werewolf") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Werewolf Horror"
    power = 2
    toughness = 4
    oracleText = "This creature can block an additional creature each combat.\n" +
        "{6}{G}: Transform this creature."

    staticAbility {
        ability = CanBlockAdditionalForCreatureGroup(count = 1, filter = GroupFilter.source())
    }

    activatedAbility {
        cost = Costs.Mana("{6}{G}")
        effect = Effects.Transform(EffectTarget.Self)
        description = "Transform this creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "174"
        artist = "Steven Belledin"
        flavorText = "\"What has long, flesh-rending claws and enormous, bone-crushing teeth?\"\n—Halana, Kessig ranger"
        imageUri = "https://cards.scryfall.io/normal/front/e/e/ee648500-a213-4aa4-a97c-b7223c11bebd.jpg?1783937443"
    }
}

private val FibrousEntangler = card("Fibrous Entangler") {
    manaCost = ""
    colorIdentity = "G"
    typeLine = "Creature — Eldrazi Werewolf"
    power = 4
    toughness = 6
    oracleText = "Vigilance\n" +
        "This creature must be blocked if able.\n" +
        "This creature can block an additional creature each combat."

    keywords(Keyword.VIGILANCE)

    staticAbility {
        ability = MustBeBlocked()
    }

    staticAbility {
        ability = CanBlockAdditionalForCreatureGroup(count = 1, filter = GroupFilter.source())
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "174"
        artist = "Steven Belledin"
        flavorText = "\". . . because to be honest, I couldn't tell you.\"\n—Halana, Kessig ranger"
        imageUri = "https://cards.scryfall.io/normal/back/e/e/ee648500-a213-4aa4-a97c-b7223c11bebd.jpg?1783937443"
        ruling(
            "2016-07-13",
            "Only one creature is required to block Fibrous Entangler. Other creatures may also block it, " +
                "and are free to block other creatures or not block at all.",
        )
        ruling(
            "2016-07-13",
            "If Tangleclaw Werewolf transforms into Fibrous Entangler while it's attacking, having " +
                "vigilance won't cause it to become untapped.",
        )
    }
}

val TangleclawWerewolf: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = TangleclawWerewolfFront,
    backFace = FibrousEntangler,
)
