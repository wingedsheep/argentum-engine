package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.MustAttack
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Skin Invasion // Skin Shedder (Shadows over Innistrad #182)
 * {R} · Enchantment — Aura // Creature — Insect Horror 3/4
 *
 * Front — Skin Invasion
 *   Enchant creature
 *   Enchanted creature attacks each combat if able.
 *   When enchanted creature dies, return this card to the battlefield transformed under your control.
 *
 * Back — Skin Shedder (vanilla 3/4)
 *
 * The dies trigger is Journey to Eternity's self-return half: the Aura goes to its owner's
 * graveyard by state-based action right after the creature dies, and the trigger — controlled by
 * the Aura's controller, not the enchanted creature's (ruling) — returns it from there on its back
 * face under that player's control.
 */
private val SkinInvasionFront = card("Skin Invasion") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature attacks each combat if able.\n" +
        "When enchanted creature dies, return this card to the battlefield transformed under your control."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    // Enchanted creature attacks each combat if able.
    staticAbility {
        ability = MustAttack(Filters.EnchantedCreature)
    }

    // When enchanted creature dies, return this card to the battlefield transformed under your control.
    triggeredAbility {
        trigger = Triggers.attached.dies()
        effect = Effects.ReturnSelfFromGraveyardTransformed(underYourControl = true)
        description = "When enchanted creature dies, return Skin Invasion to the battlefield " +
            "transformed under your control."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "182"
        artist = "Nils Hamm"
        imageUri = "https://cards.scryfall.io/normal/front/c/0/c0243401-e3ae-41c6-a3a0-9cd4b330c200.jpg?1783937747"
        ruling(
            "2016-04-08",
            "The controller of Skin Invasion, not the controller of the enchanted creature, returns " +
                "Skin Invasion to the battlefield transformed."
        )
        ruling(
            "2016-04-08",
            "The enchanted creature's controller still chooses which player or planeswalker that " +
                "creature attacks."
        )
        ruling(
            "2016-04-08",
            "If, during your declare attackers step, the enchanted creature is tapped or is affected " +
                "by a spell or ability that says it can't attack, then it doesn't attack. If there's a " +
                "cost associated with having that creature attack, its controller isn't forced to pay " +
                "that cost, so it doesn't have to attack in that case either."
        )
    }
}

private val SkinShedder = card("Skin Shedder") {
    manaCost = ""
    colorIdentity = "R"
    colorIndicator = "R" // Transformed back face, no mana cost (CR 204).
    typeLine = "Creature — Insect Horror"
    power = 3
    toughness = 4
    oracleText = ""

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "182"
        artist = "Nils Hamm"
        flavorText = "\"Patient complained of burning sensation in upper back and at base of neck. " +
            "Bed rest recommended. In a couple days, he should feel like a new man.\"\n" +
            "—Lagra, hamlet physician"
        imageUri = "https://cards.scryfall.io/normal/back/c/0/c0243401-e3ae-41c6-a3a0-9cd4b330c200.jpg?1783937747"
    }
}

val SkinInvasion: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = SkinInvasionFront,
    backFace = SkinShedder,
)
