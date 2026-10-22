package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.daybound
import com.wingedsheep.sdk.dsl.nightbound
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Reckless Stormseeker // Storm-Charged Slasher (Innistrad: Midnight Hunt)
 * {2}{R}
 * Creature — Human Werewolf // Creature — Werewolf
 *
 * Front — Reckless Stormseeker (2/3): "At the beginning of combat on your turn, target creature you
 *          control gets +1/+0 and gains haste until end of turn." Daybound.
 * Back  — Storm-Charged Slasher (3/4): the same trigger at +2/+0, granting trample and haste.
 *          Nightbound.
 */

private const val FRONT_TRIGGER_TEXT = "At the beginning of combat on your turn, target creature you control " +
    "gets +1/+0 and gains haste until end of turn."

private const val BACK_TRIGGER_TEXT = "At the beginning of combat on your turn, target creature you control " +
    "gets +2/+0 and gains trample and haste until end of turn."

private val RecklessStormseekerFront = card("Reckless Stormseeker") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Werewolf"
    power = 2
    toughness = 3
    oracleText = "$FRONT_TRIGGER_TEXT\n" +
        "Daybound (If a player casts no spells during their own turn, it becomes night next turn.)"

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.ModifyStats(1, 0, creature) then
            Effects.GrantKeyword(Keyword.HASTE, creature)
        description = FRONT_TRIGGER_TEXT
    }
    daybound()

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "157"
        artist = "Manuel Castañón"
        imageUri = "https://cards.scryfall.io/normal/front/a/3/a33af331-0746-4adf-935a-bf61ff9d8d4b.jpg?1783925597"
    }
}

private val StormChargedSlasher = card("Storm-Charged Slasher") {
    manaCost = ""
    colorIdentity = "R"
    colorIndicator = "R" // Transformed back face, no mana cost (CR 204).
    typeLine = "Creature — Werewolf"
    power = 3
    toughness = 4
    oracleText = "$BACK_TRIGGER_TEXT\n" +
        "Nightbound (If a player casts at least two spells during their own turn, it becomes day next turn.)"

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.ModifyStats(2, 0, creature) then
            Effects.GrantKeyword(Keyword.TRAMPLE, creature) then
            Effects.GrantKeyword(Keyword.HASTE, creature)
        description = BACK_TRIGGER_TEXT
    }
    nightbound()

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "157"
        artist = "Manuel Castañón"
        imageUri = "https://cards.scryfall.io/normal/back/a/3/a33af331-0746-4adf-935a-bf61ff9d8d4b.jpg?1783925597"
    }
}

val RecklessStormseeker: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = RecklessStormseekerFront,
    backFace = StormChargedSlasher,
)
